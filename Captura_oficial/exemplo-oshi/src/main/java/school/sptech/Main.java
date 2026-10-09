package school.sptech;

import oshi.hardware.CentralProcessor;
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
        CentralProcessor cpu = hal.getProcessor();

        JdbcTemplate con = new ConexaoBanco().getJdbcTemplate();

        long[][] prevTicks = { cpu.getSystemCpuLoadTicks() };

        TimerTask tarefa = new TimerTask() {
            @Override
            public void run() {

                // Rede em MB (soma das interfaces ativas, valor acumulado)
                double recebidosMb = 0;
                double enviadosMb = 0;
                for (NetworkIF net : hal.getNetworkIFs()) {
                    if (net.getIfOperStatus() == NetworkIF.IfOperStatus.UP) {
                        net.updateAttributes();
                        recebidosMb += net.getBytesRecv() / 1024.0 / 1024.0;
                        enviadosMb += net.getBytesSent() / 1024.0 / 1024.0;
                    }
                }

                salvar(con, recebidosMb, CONFIG_REDE_RECEBIDOS);
                salvar(con, enviadosMb, CONFIG_REDE_ENVIADOS);

                System.out.printf("Salvo ->  Rede: %.2f MB recebidos, %.2f MB enviados%n",recebidosMb, enviadosMb);
            }
        };

        new Timer().scheduleAtFixedRate(tarefa, 0, 5000);
    }
}