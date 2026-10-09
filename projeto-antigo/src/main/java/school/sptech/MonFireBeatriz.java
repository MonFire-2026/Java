package school.sptech;

import java.util.Scanner;

public class MonFireBeatriz {

    static class Maquina {

        private Integer id;
        private String nome;
        private Boolean capturaCpu;
        private Boolean capturaRam;
        private Boolean capturaDisco;
        private Boolean capturaRede;

        public Maquina(Integer id, String nome) {
            this.id = id;
            this.nome = nome;
            this.capturaCpu = true;
            this.capturaRam = false;
            this.capturaDisco = false;
            this.capturaRede = false;
        }

//      get e set:
        public Integer getId() {
            return id;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public Boolean getCapturaCpu() {
            return capturaCpu;
        }

        public Boolean getCapturaRam() {
            return capturaRam;
        }

        public void setCapturaRam(Boolean capturaRam) {
            this.capturaRam = capturaRam;
        }

        public Boolean getCapturaDisco() {
            return capturaDisco;
        }

        public void setCapturaDisco(Boolean capturaDisco) {
            this.capturaDisco = capturaDisco;
        }

        public Boolean getCapturaRede() {
            return capturaRede;
        }

        public void setCapturaRede(Boolean capturaRede) {
            this.capturaRede = capturaRede;
        }

        // qtd capturas ativas no total:
        public Integer contarCapturasAtivas() {
            Integer total = 0;
            if (capturaCpu) total++;
            if (capturaRam) total++;
            if (capturaDisco) total++;
            if (capturaRede) total++;
            return total;
        }

        public String descreverCapturas() {
            String descricao = "CPU"; // sempre presente
            if (capturaRam) descricao += ", RAM";
            if (capturaDisco) descricao += ", DISCO";
            if (capturaRede) descricao += ", REDE";
            return descricao;
        }

        public void exibirDados() {
            System.out.printf("ID: %d | Nome: %s | Capturas (%d): %s%n",
                    id, nome, contarCapturasAtivas(), descreverCapturas());
        }
    }


    private Maquina[] maquinas;
    private Integer totalMaquinas;
    private Integer proximoId;

    public MonFireBeatriz(Integer capacidade) {
        this.maquinas = new Maquina[capacidade];
        this.totalMaquinas = 0;
        this.proximoId = 1;
    }


    public void cadastrarMaquina(String nome) {
        if (totalMaquinas >= maquinas.length) {
            System.out.println("Limite de maquinas cadastradas atingido!");
            return;
        }
        Maquina novaMaquina = new Maquina(proximoId, nome);
        maquinas[totalMaquinas] = novaMaquina;
        totalMaquinas++;
        proximoId++;
        System.out.printf("Maquina \"%s\" cadastrada com sucesso (ID: %d), capturando CPU.%n",
                nome, novaMaquina.getId());
    }

    public Maquina buscarPorId(Integer id) {
        for (int i = 0; i < totalMaquinas; i++) {
            if (maquinas[i].getId().equals(id)) {
                return maquinas[i];
            }
        }
        return null;
    }

    public void atualizarNome(Integer id, String novoNome) {
        Maquina maquina = buscarPorId(id);
        if (maquina == null) {
            System.out.println("Maquina nao encontrada!");
            return;
        }
        maquina.setNome(novoNome);
        System.out.println("Nome atualizado com sucesso!");
    }


    public void atualizarCapturaRam(Integer id, Boolean ativar) {
        Maquina maquina = buscarPorId(id);
        if (maquina == null) {
            System.out.println("Maquina nao encontrada!");
            return;
        }
        maquina.setCapturaRam(ativar);
        System.out.printf("Captura de RAM %s para a maquina \"%s\".%n",
                ativar ? "ativada" : "desativada", maquina.getNome());
    }


    public void atualizarCapturaDisco(Integer id, Boolean ativar) {
        Maquina maquina = buscarPorId(id);
        if (maquina == null) {
            System.out.println("Maquina nao encontrada!");
            return;
        }
        maquina.setCapturaDisco(ativar);
        System.out.printf("Captura de DISCO %s para a maquina \"%s\".%n",
                ativar ? "ativada" : "desativada", maquina.getNome());
    }

    public void atualizarCapturaRede(Integer id, Boolean ativar) {
        Maquina maquina = buscarPorId(id);
        if (maquina == null) {
            System.out.println("Maquina nao encontrada!");
            return;
        }
        maquina.setCapturaRede(ativar);
        System.out.printf("Captura de REDE %s para a maquina \"%s\".%n",
                ativar ? "ativada" : "desativada", maquina.getNome());
    }


    public void listarMaquinas() {
        System.out.println("=================================================");
        System.out.println("   MAQUINAS CADASTRADAS");
        System.out.println("=================================================");
        if (totalMaquinas == 0) {
            System.out.println("Nenhuma maquina cadastrada ainda.");
            return;
        }
        for (int i = 0; i < totalMaquinas; i++) {
            maquinas[i].exibirDados();
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        MonFireBeatriz sistema = new MonFireBeatriz(50);
        Integer opcao = -1;

        while (opcao != 0) {
            System.out.println("\n=================================================");
            System.out.println(" SISTEMA DE CADASTRO DE MAQUINAS");
            System.out.println("=================================================");
            System.out.println("1 - Cadastrar nova maquina (inicia com CPU)");
            System.out.println("2 - Atualizar nome da maquina");
            System.out.println("3 - Ativar/Desativar captura de RAM");
            System.out.println("4 - Ativar/Desativar captura de DISCO");
            System.out.println("5 - Ativar/Desativar captura de REDE");
            System.out.println("6 - Listar maquinas cadastradas");
            System.out.println("0 - Sair");
            System.out.print("Escolha uma opcao: ");
            opcao = Integer.valueOf(scanner.nextInt());
            scanner.nextLine(); // limpa o buffer

            if (opcao == 1) {
                System.out.print("Nome da nova maquina: ");
                String nome = scanner.nextLine();
                sistema.cadastrarMaquina(nome);

            } else if (opcao == 2) {
                System.out.print("ID da maquina: ");
                Integer idNome = Integer.valueOf(scanner.nextInt());
                scanner.nextLine();
                System.out.print("Novo nome: ");
                String novoNome = scanner.nextLine();
                sistema.atualizarNome(idNome, novoNome);

            } else if (opcao == 3) {
                System.out.print("ID da maquina: ");
                Integer idRam = Integer.valueOf(scanner.nextInt());
                System.out.print("Ativar captura de RAM? (1-Sim / 0-Nao): ");
                Boolean ativarRam = scanner.nextInt() == 1;
                sistema.atualizarCapturaRam(idRam, ativarRam);

            } else if (opcao == 4) {
                System.out.print("ID da maquina: ");
                Integer idDisco = Integer.valueOf(scanner.nextInt());
                System.out.print("Ativar captura de DISCO? (1-Sim / 0-Nao): ");
                Boolean ativarDisco = scanner.nextInt() == 1;
                sistema.atualizarCapturaDisco(idDisco, ativarDisco);

            } else if (opcao == 5) {
                System.out.print("ID da maquina: ");
                Integer idRede = Integer.valueOf(scanner.nextInt());
                System.out.print("Ativar captura de REDE? (1-Sim / 0-Nao): ");
                Boolean ativarRede = scanner.nextInt() == 1;
                sistema.atualizarCapturaRede(idRede, ativarRede);

            } else if (opcao == 6) {
                sistema.listarMaquinas();

            } else if (opcao == 0) {
                System.out.println("Encerrando o sistema...");

            } else {
                System.out.println("Opcao invalida!");
            }
        }

        scanner.close();
    }
}