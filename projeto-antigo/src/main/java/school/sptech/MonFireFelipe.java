package school.sptech;

import java.util.Scanner;

public class MonFireFelipe {

    static void main() {

        String[] nomesCadastrados = new String[1000];
        String[] respectivoCargo = new String[1000];
        String[] respectivaSenha = new String[1000];
        String[] respectivoCPF = new String[1000];
        Integer contas = 0;
        Scanner contaScanner = new Scanner(System.in);
        Scanner nomeScanner = new Scanner(System.in);
        Scanner cargoScanner = new Scanner(System.in);
        Scanner senhaScanner = new Scanner(System.in);
        Scanner cpfScanner = new Scanner(System.in);
        Scanner loginCPFScanner = new Scanner(System.in);
        Scanner loginSenhaScanner = new Scanner(System.in);

        Integer acao = 0;

        while (acao != 4) {

            System.out.println("==============================");
            System.out.println("Olá, bem vindo a Monfire!");
            System.out.println("O que deseja realizar no sistema?");
            System.out.println("1 - Consultar usuários cadastrados");
            System.out.println("2 - Cadastrar um usuário");
            System.out.println("3 - Realizar login");
            System.out.println("4 - Encerrar programa");
            System.out.println("------------------------------");
            acao = contaScanner.nextInt();
            if (acao == 1) {
                if (nomesCadastrados[0] == null) {
                    System.out.println("Ainda não há usuários cadastrados no sistema");
                } else {
                    for (int i = 0; i < nomesCadastrados.length; i++) {
                        if (nomesCadastrados[i] != null) {
                            System.out.println(nomesCadastrados[i] + " - " + respectivoCargo[i]);
                        }
                    }
                }
            } else if (acao == 2) {
                System.out.println("==============================");
                System.out.println("Digite seu nome completo:");
                System.out.println("------------------------------");
                String nomeCadastro = nomeScanner.nextLine();
                System.out.println("==============================");
                System.out.println("Digite seu CPF:");
                System.out.println("------------------------------");
                String cpfCadastro = cpfScanner.nextLine();
                System.out.println("==============================");
                System.out.println("Digite seu cargo:");
                System.out.println("------------------------------");
                String cargoCadastro = cargoScanner.nextLine();
                System.out.println("==============================");
                System.out.println("Digite sua senha:");
                System.out.println("------------------------------");
                String senhaCadastro = senhaScanner.nextLine();
                nomesCadastrados[contas] = nomeCadastro;
                respectivoCargo[contas] = cargoCadastro;
                respectivaSenha[contas] = senhaCadastro;
                respectivoCPF[contas] = cpfCadastro;
                contas++;
                System.out.println("Cadastro realizado com sucesso!");
            } else if (acao == 4) {
                System.out.println("==============================");
                System.out.println("Encerrando programa...");
                System.out.println("------------------------------");
            } else if (acao == 3) {
                if (nomesCadastrados[0] == null){
                    System.out.println("Ainda não há usuários cadastrados no sistema");
                } else {
                    System.out.println("==============================");
                    System.out.println("Insira seu CPF para login");
                    System.out.println("------------------------------");
                    String cpfLogin = loginCPFScanner.nextLine();
                    for (int i = 0; i < respectivoCPF.length; i++) {
                        if (respectivoCPF[i].equals(cpfLogin)){
                            System.out.println("==============================");
                            System.out.println("Insira sua senha para login");
                            System.out.println("------------------------------");
                            String senhaLogin = loginSenhaScanner.nextLine();
                            if (respectivaSenha[i].equals(senhaLogin)){
                                System.out.println("Olá " + nomesCadastrados[i] + ", bem vindo a MonFire!");
                            } else {
                                System.out.println("Você digitou algo errado, tente novamente");
                            }
                        }
                    }
                }
            } else {
                System.out.println("Você digitou algo errado, tente novamente!");
            }

        }
    }
}
