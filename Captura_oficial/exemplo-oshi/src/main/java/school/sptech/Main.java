package school.sptech;

import com.slack.api.Slack;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.spi.SystemInfoFactory;
import oshi.spi.SystemInfoProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class Main {
    private static final String REDE_RECEBIDA = "REDE_RECEBIDA";
    private static final String REDE_ENVIADA = "REDE_ENVIADA";
    private static final String SLACK_BOT_TOKEN = System.getenv("SLACK_BOT_TOKEN");
    private static final String SLACK_CHANNEL_ID = System.getenv("SLACK_CHANNEL_ID");
    private static final Map<String, String> ESTADOS_ALERTA = new HashMap<>();
    private static final Slack SLACK_CLIENT = Slack.getInstance();

    private static class ConfiguracaoMetrica {
        int id;
        String codigo;
        double limiteAtencao;
        double limiteCritico;
        String nomeMaquina;
        String nomeEmpresa;
        String nomeComponente;
        String unidade;

        ConfiguracaoMetrica(
                int id,
                String codigo,
                double limiteAtencao,
                double limiteCritico,
                String nomeMaquina,
                String nomeEmpresa,
                String nomeComponente,
                String unidade
        ) {
            this.id = id;
            this.codigo = codigo;
            this.limiteAtencao = limiteAtencao;
            this.limiteCritico = limiteCritico;
            this.nomeMaquina = nomeMaquina;
            this.nomeEmpresa = nomeEmpresa;
            this.nomeComponente = nomeComponente;
            this.unidade = unidade;
        }
    }

    private static int lerMaquinaId() {
        String valor = System.getenv("MAQUINA_ID");

        try {
            int maquinaId = Integer.parseInt(valor);
            if (maquinaId <= 0) {
                throw new NumberFormatException();
            }
            return maquinaId;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "A variável MAQUINA_ID deve ser um inteiro positivo."
            );
        }
    }

    private static Map<String, ConfiguracaoMetrica> buscarConfiguracoes(
            JdbcTemplate con,
            int maquinaId
    ) {
        Map<String, ConfiguracaoMetrica> configuracoes = new HashMap<>();
        String sql = """
                SELECT
                    cm.id,
                    tc.codigo,
                    atencao.limite AS limite_atencao,
                    critico.limite AS limite_critico,
                    m.nome AS nome_maquina,
                    e.unidade AS nome_empresa,
                    tc.nome AS nome_componente,
                    mc.unidade_medida
                FROM configuracao_maquina cm
                JOIN maquina m ON m.id = cm.fk_maquina
                JOIN empresa e ON e.id = m.fk_empresa
                JOIN componente comp ON comp.id = cm.fk_componente
                JOIN tipo_componente tc ON tc.id = comp.fk_tipo_componente
                JOIN metrica_componente mc ON mc.id = tc.fk_metrica_componente
                JOIN nivel_alerta nivel_atencao
                    ON nivel_atencao.estado = 'Atenção'
                JOIN metrica_alerta atencao
                    ON atencao.fk_configuracao_maquina = cm.id
                    AND atencao.fk_nivel_alerta = nivel_atencao.id
                JOIN nivel_alerta nivel_critico
                    ON nivel_critico.estado = 'Crítico'
                JOIN metrica_alerta critico
                    ON critico.fk_configuracao_maquina = cm.id
                    AND critico.fk_nivel_alerta = nivel_critico.id
                WHERE cm.fk_maquina = ?
                  AND cm.ativa = 1
                  AND comp.status_atividade = 1
                  AND tc.codigo IN ('REDE_RECEBIDA', 'REDE_ENVIADA')
                """;

        con.query(
                sql,
                preparedStatement -> preparedStatement.setInt(1, maquinaId),
                resultSet -> {
                    configuracoes.put(
                            resultSet.getString("codigo"),
                            new ConfiguracaoMetrica(
                                    resultSet.getInt("id"),
                                    resultSet.getString("codigo"),
                                    resultSet.getDouble("limite_atencao"),
                                    resultSet.getDouble("limite_critico"),
                                    resultSet.getString("nome_maquina"),
                                    resultSet.getString("nome_empresa"),
                                    resultSet.getString("nome_componente"),
                                    resultSet.getString("unidade_medida")
                            )
                    );
                }
        );

        return configuracoes;
    }

    private static boolean salvar(
            JdbcTemplate con,
            double valor,
            ConfiguracaoMetrica configuracao
    ) {
        try {
            con.update(
                    "INSERT INTO captura (valor, fk_configuracao_maquina) VALUES (?, ?)",
                    valor, configuracao.id
            );
            return true;
        } catch (Exception e) {
            System.err.println("Erro ao salvar no MySQL: " + e.getMessage());
            return false;
        }
    }

    private static String classificarRede(double valor, ConfiguracaoMetrica configuracao) {
        double limiteCritico = Math.min(
                configuracao.limiteAtencao,
                configuracao.limiteCritico
        );
        double limiteAtencao = Math.max(
                configuracao.limiteAtencao,
                configuracao.limiteCritico
        );

        if (valor <= limiteCritico) return "Crítico";
        if (valor <= limiteAtencao) return "Atenção";
        return "Normal";
    }

    private static int pesoNivel(String nivel) {
        return switch (nivel) {
            case "Crítico" -> 2;
            case "Atenção" -> 1;
            default -> 0;
        };
    }

    private static String montarMensagem(
            double valor,
            ConfiguracaoMetrica configuracao,
            String nivel
    ) {
        String simbolo = nivel.equals("Crítico") ? "🔴" : "🟡";
        double limite = nivel.equals("Crítico")
                ? configuracao.limiteCritico
                : configuracao.limiteAtencao;

        return String.format(
                Locale.forLanguageTag("pt-BR"),
                """
                %s *Alerta %s - MonFire*
                *Empresa:* %s
                *Máquina:* %s
                *Componente:* %s (%s)
                *Valor capturado:* %.2f %s
                *Limite atingido:* %.2f %s
                *Data e hora:* %s""",
                simbolo,
                nivel,
                configuracao.nomeEmpresa,
                configuracao.nomeMaquina,
                configuracao.nomeComponente,
                configuracao.codigo,
                valor,
                configuracao.unidade,
                limite,
                configuracao.unidade,
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                )
        );
    }

    private static boolean enviarAoSlack(String mensagem) {
        try {
            ChatPostMessageResponse resposta = SLACK_CLIENT
                    .methods(SLACK_BOT_TOKEN)
                    .chatPostMessage(requisicao -> requisicao
                            .channel(SLACK_CHANNEL_ID)
                            .text(mensagem)
                    );

            if (!resposta.isOk()) {
                System.err.println(
                        "Slack recusou a mensagem: " + resposta.getError()
                );
                return false;
            }

            return true;
        } catch (Exception e) {
            System.err.println(
                    "Erro ao enviar alerta ao Slack: " + e.getMessage()
            );
            return false;
        }
    }

    private static void verificarAlerta(double valor, ConfiguracaoMetrica configuracao) {
        if (
                SLACK_BOT_TOKEN == null || SLACK_BOT_TOKEN.isBlank()
                || SLACK_CHANNEL_ID == null || SLACK_CHANNEL_ID.isBlank()
        ) return;

        String nivelAtual = classificarRede(valor, configuracao);
        String nivelAnterior = ESTADOS_ALERTA.getOrDefault(
                configuracao.codigo,
                "Normal"
        );
        boolean piorou = pesoNivel(nivelAtual) > pesoNivel(nivelAnterior);

        if (piorou && !nivelAtual.equals("Normal")) {
            String mensagem = montarMensagem(valor, configuracao, nivelAtual);
            if (!enviarAoSlack(mensagem)) return;

            System.out.printf("Alerta %s enviado ao Slack.%n", nivelAtual);
        }

        ESTADOS_ALERTA.put(configuracao.codigo, nivelAtual);
    }

    public static void main(String[] args) {
        SystemInfoProvider si = SystemInfoFactory.create();
        HardwareAbstractionLayer hal = si.getHardware();

        JdbcTemplate con = new ConexaoBanco().getJdbcTemplate();
        int maquinaId = lerMaquinaId();
        Map<String, ConfiguracaoMetrica> configuracoes = buscarConfiguracoes(con, maquinaId);
        ConfiguracaoMetrica configuracaoRecebida = configuracoes.get(REDE_RECEBIDA);
        ConfiguracaoMetrica configuracaoEnviada = configuracoes.get(REDE_ENVIADA);

        System.out.printf("Máquina %d | Métricas Java: %s%n", maquinaId, configuracoes.keySet());
        if (configuracaoRecebida == null) {
            System.out.println("Rede recebida não está configurada para esta máquina.");
        }
        if (configuracaoEnviada == null) {
            System.out.println("Rede enviada não está configurada para esta máquina.");
        }
        if (
                SLACK_BOT_TOKEN == null || SLACK_BOT_TOKEN.isBlank()
                || SLACK_CHANNEL_ID == null || SLACK_CHANNEL_ID.isBlank()
        ) {
            System.out.println(
                    "SLACK_BOT_TOKEN ou SLACK_CHANNEL_ID não configurado. "
                    + "Alertas desativados."
            );
        }

        // Guarda os contadores iniciais da rede
        long bytesRecebidosAnterior = 0;
        long bytesEnviadosAnterior = 0;

        for (NetworkIF net : hal.getNetworkIFs()) {
            if (net.getIfOperStatus() == NetworkIF.IfOperStatus.UP) {
                net.updateAttributes();

                bytesRecebidosAnterior += net.getBytesRecv();
                bytesEnviadosAnterior += net.getBytesSent();
            }
        }

        final long[] recebidosAnterior = {bytesRecebidosAnterior};
        final long[] enviadosAnterior = {bytesEnviadosAnterior};
        final long[] tempoAnterior = {System.nanoTime()};

        TimerTask tarefa = new TimerTask() {
            @Override
            public void run() {
                long bytesRecebidosAtual = 0;
                long bytesEnviadosAtual = 0;

                for (NetworkIF net : hal.getNetworkIFs()) {
                    if (net.getIfOperStatus() == NetworkIF.IfOperStatus.UP) {
                        net.updateAttributes();

                        bytesRecebidosAtual += net.getBytesRecv();
                        bytesEnviadosAtual += net.getBytesSent();
                    }
                }

                long tempoAtual = System.nanoTime();

                double segundos = (tempoAtual - tempoAnterior[0])
                        / 1_000_000_000.0;

                long diferencaRecebidos = Math.max(
                        0, bytesRecebidosAtual - recebidosAnterior[0]
                );

                long diferencaEnviados = Math.max(
                        0, bytesEnviadosAtual - enviadosAnterior[0]
                );

                // Converte o intervalo para MB
                double recebidosMb = diferencaRecebidos / 1_000_000.0;
                double enviadosMb = diferencaEnviados / 1_000_000.0;

                if (configuracaoRecebida != null) {
                    if (salvar(con, recebidosMb, configuracaoRecebida)) {
                        verificarAlerta(recebidosMb, configuracaoRecebida);
                    }
                }
                if (configuracaoEnviada != null) {
                    if (salvar(con, enviadosMb, configuracaoEnviada)) {
                        verificarAlerta(enviadosMb, configuracaoEnviada);
                    }
                }

                // Calcula a velocidade média em Mbps
                double recebidosMbps = (diferencaRecebidos * 8.0)
                        / segundos / 1_000_000.0;

                double enviadosMbps = (diferencaEnviados * 8.0)
                        / segundos / 1_000_000.0;

                if (configuracaoRecebida != null || configuracaoEnviada != null) {
                    System.out.printf(
                            "Salvo -> Rede: %.4f MB recebidos (%.2f Mbps), %.4f MB enviados (%.2f Mbps)%n",
                            recebidosMb, recebidosMbps, enviadosMb, enviadosMbps
                    );
                }

                // Atualiza os valores para a próxima medição
                recebidosAnterior[0] = bytesRecebidosAtual;
                enviadosAnterior[0] = bytesEnviadosAtual;
                tempoAnterior[0] = tempoAtual;
            }
        };

        // Ccaptura a cada 5 segundos
        new Timer().scheduleAtFixedRate(tarefa, 5000, 5000);
    }
}
