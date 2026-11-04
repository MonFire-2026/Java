package school.sptech;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.spi.SystemInfoFactory;
import oshi.spi.SystemInfoProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class Main {
    private static final String REDE_RECEBIDA = "REDE_RECEBIDA";
    private static final String REDE_ENVIADA = "REDE_ENVIADA";

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

    private static Map<String, Integer> buscarConfiguracoes(
            JdbcTemplate con,
            int maquinaId
    ) {
        Map<String, Integer> configuracoes = new HashMap<>();
        String sql = """
                SELECT cm.id, tc.codigo
                FROM configuracao_maquina cm
                JOIN componente comp ON comp.id = cm.fk_componente
                JOIN tipo_componente tc ON tc.id = comp.fk_tipo_componente
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
                            resultSet.getInt("id")
                    );
                }
        );

        return configuracoes;
    }

    private static void salvar(JdbcTemplate con, double valor, int configuracao) {
        try {
            con.update(
                    "INSERT INTO captura (valor, fk_configuracao_maquina) VALUES (?, ?)",
                    valor, configuracao
            );
        } catch (Exception e) {
            System.err.println("Erro ao salvar no MySQL: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SystemInfoProvider si = SystemInfoFactory.create();
        HardwareAbstractionLayer hal = si.getHardware();

        JdbcTemplate con = new ConexaoBanco().getJdbcTemplate();
        int maquinaId = lerMaquinaId();
        Map<String, Integer> configuracoes = buscarConfiguracoes(con, maquinaId);
        Integer configuracaoRecebida = configuracoes.get(REDE_RECEBIDA);
        Integer configuracaoEnviada = configuracoes.get(REDE_ENVIADA);

        System.out.printf("Máquina %d | Métricas Java: %s%n", maquinaId, configuracoes.keySet());
        if (configuracaoRecebida == null) {
            System.out.println("Rede recebida não está configurada para esta máquina.");
        }
        if (configuracaoEnviada == null) {
            System.out.println("Rede enviada não está configurada para esta máquina.");
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
                    salvar(con, recebidosMb, configuracaoRecebida);
                }
                if (configuracaoEnviada != null) {
                    salvar(con, enviadosMb, configuracaoEnviada);
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
