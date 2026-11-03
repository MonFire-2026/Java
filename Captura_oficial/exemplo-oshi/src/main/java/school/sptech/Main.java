package school.sptech;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.spi.SystemInfoFactory;
import oshi.spi.SystemInfoProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Timer;
import java.util.TimerTask;

public class Main {
    private static final int CONFIG_REDE_RECEBIDOS = 11;
    private static final int CONFIG_REDE_ENVIADOS = 12;

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

                // Salva  no banco.
                salvar(con, recebidosMb, CONFIG_REDE_RECEBIDOS);
                salvar(con, enviadosMb, CONFIG_REDE_ENVIADOS);

                // Calcula a velocidade média em Mbps
                double recebidosMbps = (diferencaRecebidos * 8.0)
                        / segundos / 1_000_000.0;

                double enviadosMbps = (diferencaEnviados * 8.0)
                        / segundos / 1_000_000.0;

                System.out.printf( "Salvo -> Rede: %.4f MB recebidos (%.2f Mbps), %.4f MB enviados (%.2f Mbps)%n", recebidosMb, recebidosMbps, enviadosMb, enviadosMbps
                );

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