
package school.sptech;

import com.slack.api.Slack;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
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

    private static final Map<String, ConfiguracaoMetrica> configuracoes =
            new HashMap<>();

    private static class ConfiguracaoMetrica {

        int id;
        String codigo;
        double limiteAtencao;
        double limiteCritico;
        String nomeMaquina;
        String nomeEmpresa;
        String nomeComponente;
        String unidade;

        ConfiguracaoMetrica(int id, String codigo) {
            this.id = id;
            this.codigo = codigo;
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

    // Busca as oito configurações, sem exigir limites de alerta.
    private static void buscarConfiguracoes(
            JdbcTemplate con,
            int maquinaId
    ) {

        String sql = """
                SELECT
                    cm.id,
                    tc.codigo
                FROM configuracao_maquina cm
                JOIN componente comp
                    ON comp.id = cm.fk_componente
                JOIN tipo_componente tc
                    ON tc.id = comp.fk_tipo_componente
                WHERE cm.fk_maquina = ?
                  AND cm.ativa = 1
                  AND comp.status_atividade = 1
                  AND tc.codigo IN (
                    'REDE_RECEBIDA',
                    'REDE_ENVIADA',
                    'REDE_PACOTES_RECEBIDOS',
                    'REDE_PACOTES_ENVIADOS',
                    'REDE_ERROS_RECEBIDOS',
                    'REDE_ERROS_ENVIADOS',
                    'REDE_DESCARTES_RECEBIDOS',
                    'REDE_COLISOES'
                  )
                """;

        con.query(
                sql,
                preparedStatement -> preparedStatement.setInt(1, maquinaId),
                resultSet -> {

                    String codigo = resultSet.getString("codigo");

                    ConfiguracaoMetrica configuracao =
                            new ConfiguracaoMetrica(
                                    resultSet.getInt("id"),
                                    codigo
                            );

                    configuracoes.put(codigo, configuracao);

                    System.out.println(
                            codigo + " -> configuração " + configuracao.id
                    );
                }
        );

        // Busca os limites somente para Download e Upload.
        buscarLimitesAlertas(con, maquinaId);
    }

    private static void buscarLimitesAlertas(
            JdbcTemplate con,
            int maquinaId
    ) {

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
                JOIN maquina m
                    ON m.id = cm.fk_maquina
                JOIN empresa e
                    ON e.id = m.fk_empresa
                JOIN componente comp
                    ON comp.id = cm.fk_componente
                JOIN tipo_componente tc
                    ON tc.id = comp.fk_tipo_componente
                JOIN metrica_componente mc
                    ON mc.id = tc.fk_metrica_componente
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

                    String codigo = resultSet.getString("codigo");

                    ConfiguracaoMetrica configuracao =
                            configuracoes.get(codigo);

                    if (configuracao != null) {

                        configuracao.limiteAtencao =
                                resultSet.getDouble("limite_atencao");

                        configuracao.limiteCritico =
                                resultSet.getDouble("limite_critico");

                        configuracao.nomeMaquina =
                                resultSet.getString("nome_maquina");

                        configuracao.nomeEmpresa =
                                resultSet.getString("nome_empresa");

                        configuracao.nomeComponente =
                                resultSet.getString("nome_componente");

                        configuracao.unidade =
                                resultSet.getString("unidade_medida");
                    }
                }
        );
    }

    private static boolean salvar(
            JdbcTemplate con,
            double valor,
            String codigo
    ) {

        ConfiguracaoMetrica configuracao = configuracoes.get(codigo);

        if (configuracao == null) {
            System.err.println(
                    "Configuração não encontrada: " + codigo
            );
            return false;
        }

        try {

            con.update(
                    "INSERT INTO captura (valor, fk_configuracao_maquina) VALUES (?, ?)",
                    valor,
                    configuracao.id
            );

            return true;

        } catch (Exception e) {

            System.err.println(
                    "Erro ao salvar " + codigo + ": " + e.getMessage()
            );

            return false;
        }
    }

    private static String classificarRede(
            double valor,
            ConfiguracaoMetrica configuracao
    ) {

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

            ChatPostMessageResponse resposta =
                    SLACK_CLIENT.methods(SLACK_BOT_TOKEN)
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

    private static void verificarAlerta(
            double valor,
            ConfiguracaoMetrica configuracao
    ) {

        if (SLACK_BOT_TOKEN == null || SLACK_BOT_TOKEN.isBlank()
                || SLACK_CHANNEL_ID == null || SLACK_CHANNEL_ID.isBlank()) {
            return;
        }

        String nivelAtual = classificarRede(valor, configuracao);

        String nivelAnterior = ESTADOS_ALERTA.getOrDefault(
                configuracao.codigo,
                "Normal"
        );

        boolean piorou =
                pesoNivel(nivelAtual) > pesoNivel(nivelAnterior);

        if (piorou && !nivelAtual.equals("Normal")) {

            String mensagem = montarMensagem(
                    valor,
                    configuracao,
                    nivelAtual
            );

            if (!enviarAoSlack(mensagem)) {
                return;
            }

            System.out.printf(
                    "Alerta %s enviado ao Slack.%n",
                    nivelAtual
            );
        }

        ESTADOS_ALERTA.put(configuracao.codigo, nivelAtual);
    }

    public static void main(String[] args) {

        // Inicialização do OSHI.
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();

        // Conexão com o banco.
        JdbcTemplate con = new ConexaoBanco().getJdbcTemplate();

        int maquinaId = lerMaquinaId();

        try {

            buscarConfiguracoes(con, maquinaId);

            System.out.println(
                    "Máquina " + maquinaId
                            + " | Métricas encontradas: "
                            + configuracoes.keySet()
            );

        } catch (Exception e) {

            System.err.println(
                    "Erro ao carregar configurações: " + e.getMessage()
            );

            e.printStackTrace();
            return;
        }

        if (SLACK_BOT_TOKEN == null || SLACK_BOT_TOKEN.isBlank()
                || SLACK_CHANNEL_ID == null || SLACK_CHANNEL_ID.isBlank()) {

            System.out.println(
                    "Slack não configurado. Captura continuará normalmente, "
                            + "mas os alertas estarão desativados."
            );
        }

        // Contadores anteriores para calcular o tráfego do intervalo.
        long bytesRecebidosAnterior = 0;
        long bytesEnviadosAnterior = 0;

        for (NetworkIF net : hal.getNetworkIFs()) {

            if (net.getIfOperStatus() != NetworkIF.IfOperStatus.DOWN) {

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

                try {

                    long bytesRecebidosAtual = 0;
                    long bytesEnviadosAtual = 0;

                    long pacotesRecebidos = 0;
                    long pacotesEnviados = 0;

                    long errosRecebidos = 0;
                    long errosEnviados = 0;

                    long descartesRecebidos = 0;
                    long colisoes = 0;

                    // Percorre as interfaces de rede.
                    for (NetworkIF net : hal.getNetworkIFs()) {

                        if ("lo".equalsIgnoreCase(net.getName())) {
                            continue;
                        }

                        boolean atualizou = net.updateAttributes();

                        if (!atualizou) {
                            continue;
                        }

                        if (net.getIfOperStatus()
                                == NetworkIF.IfOperStatus.DOWN) {
                            continue;
                        }

                        bytesRecebidosAtual += net.getBytesRecv();
                        bytesEnviadosAtual += net.getBytesSent();

                        pacotesRecebidos += net.getPacketsRecv();
                        pacotesEnviados += net.getPacketsSent();

                        errosRecebidos += net.getInErrors();
                        errosEnviados += net.getOutErrors();

                        descartesRecebidos += net.getInDrops();
                        colisoes += net.getCollisions();
                    }

                    // Calcula o tráfego desde a última coleta.
                    long diferencaRecebidos = Math.max(
                            0,
                            bytesRecebidosAtual - recebidosAnterior[0]
                    );

                    long diferencaEnviados = Math.max(
                            0,
                            bytesEnviadosAtual - enviadosAnterior[0]
                    );

                    double recebidosMb =
                            diferencaRecebidos / 1_000_000.0;

                    double enviadosMb =
                            diferencaEnviados / 1_000_000.0;

                    // Salva Download e Upload.
                    if (salvar(con, recebidosMb, REDE_RECEBIDA)) {

                        ConfiguracaoMetrica config =
                                configuracoes.get(REDE_RECEBIDA);

                        if (config != null
                                && config.nomeMaquina != null) {
                            verificarAlerta(recebidosMb, config);
                        }
                    }

                    if (salvar(con, enviadosMb, REDE_ENVIADA)) {

                        ConfiguracaoMetrica config =
                                configuracoes.get(REDE_ENVIADA);

                        if (config != null
                                && config.nomeMaquina != null) {
                            verificarAlerta(enviadosMb, config);
                        }
                    }

                    // Salva as outras seis métricas sem exigir alertas.
                    salvar(
                            con,
                            pacotesRecebidos,
                            "REDE_PACOTES_RECEBIDOS"
                    );

                    salvar(
                            con,
                            pacotesEnviados,
                            "REDE_PACOTES_ENVIADOS"
                    );

                    salvar(
                            con,
                            errosRecebidos,
                            "REDE_ERROS_RECEBIDOS"
                    );

                    salvar(
                            con,
                            errosEnviados,
                            "REDE_ERROS_ENVIADOS"
                    );

                    salvar(
                            con,
                            descartesRecebidos,
                            "REDE_DESCARTES_RECEBIDOS"
                    );

                    salvar(
                            con,
                            colisoes,
                            "REDE_COLISOES"
                    );

                    // Calcula velocidade média em Mbps.
                    long tempoAtual = System.nanoTime();

                    double segundos =
                            (tempoAtual - tempoAnterior[0])
                                    / 1_000_000_000.0;

                    double recebidosMbps = 0;
                    double enviadosMbps = 0;

                    if (segundos > 0) {

                        recebidosMbps =
                                (diferencaRecebidos * 8.0)
                                        / segundos / 1_000_000.0;

                        enviadosMbps =
                                (diferencaEnviados * 8.0)
                                        / segundos / 1_000_000.0;
                    }

                    System.out.println(
                            "\n===== MONITORAMENTO DE REDE ====="
                    );

                    System.out.printf(
                            "Download: %.4f MB (%.2f Mbps)%n",
                            recebidosMb,
                            recebidosMbps
                    );

                    System.out.printf(
                            "Upload: %.4f MB (%.2f Mbps)%n",
                            enviadosMb,
                            enviadosMbps
                    );

                    System.out.println(
                            "Pacotes recebidos: " + pacotesRecebidos
                    );

                    System.out.println(
                            "Pacotes enviados: " + pacotesEnviados
                    );

                    System.out.println(
                            "Erros recebidos: " + errosRecebidos
                    );

                    System.out.println(
                            "Erros enviados: " + errosEnviados
                    );

                    System.out.println(
                            "Descartes recebidos: " + descartesRecebidos
                    );

                    System.out.println(
                            "Colisões: " + colisoes
                    );

                    System.out.println(
                            "=================================\n"
                    );

                    // Atualiza os valores para a próxima coleta.
                    recebidosAnterior[0] = bytesRecebidosAtual;
                    enviadosAnterior[0] = bytesEnviadosAtual;
                    tempoAnterior[0] = tempoAtual;

                } catch (Exception e) {

                    System.err.println(
                            "Erro durante a coleta de rede: "
                                    + e.getMessage()
                    );

                    e.printStackTrace();
                }
            }
        };

        // Executa imediatamente e repete a cada 5 segundos.
        new Timer().scheduleAtFixedRate(tarefa, 0, 5000);
    }
}
