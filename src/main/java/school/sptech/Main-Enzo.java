package school.sptech;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static class Usuario {
        public String login;
        public String senha;

        public Usuario(String login, String senha) {
            this.login = login;
            this.senha = senha;
        }
    }

    static List<Usuario> usuarios = new ArrayList<>();
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        int opcao;

        do {
            System.out.println("\n===== MENU =====");
            System.out.println("1 - Cadastrar");
            System.out.println("2 - Login");
            System.out.println("3 - Sair");
            System.out.print("Escolha uma opção: ");
            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {
                case 1:
                    cadastrar();
                    break;
                case 2:
                    login();
                    break;
                case 3:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida!");
            }
        } while (opcao != 3);
    }

    static void cadastrar() {
        System.out.print("Digite um login: ");
        String login = scanner.nextLine();

        for (Usuario u : usuarios) {
            if (u.login.equals(login)) {
                System.out.println("Esse login já existe!");
                return;
            }
        }

        System.out.print("Digite uma senha: ");
        String senha = scanner.nextLine();

        usuarios.add(new Usuario(login, senha));
        System.out.println("Usuário cadastrado com sucesso!");
    }

    static void login() {
        System.out.print("Login: ");
        String login = scanner.nextLine();
        System.out.print("Senha: ");
        String senha = scanner.nextLine();

        for (Usuario u : usuarios) {
            if (u.login.equals(login) && u.senha.equals(senha)) {
                System.out.println("Login realizado com sucesso! Bem-vindo, " + login + "!");
                return;
            }
        }

        System.out.println("Login ou senha incorretos!");
    }
}