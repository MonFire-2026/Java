package school.sptech;

import java.util.Scanner;

public class MonFireRaphael {

    public static void main() {

        Scanner leitor = new Scanner(System.in);
        Scanner leitorLinha = new Scanner(System.in);

        System.out.println("Insira o Número da Ocorrência");
        Integer numeroOcorrencia = leitor.nextInt();

        leitorLinha.nextLine();

        System.out.println("Insira o tipo da ocorrência");
        String tipo = leitorLinha.nextLine();


        System.out.println("Insira o local da ocorrência");
        String local = leitorLinha.nextLine();


        System.out.println("Insira o nível de gravidade da ocorrência (0 a 5)");
        Integer nivelGravidade = leitor.nextInt();

        if (nivelGravidade < 0 || nivelGravidade > 5) {
            System.out.println("Nível de gravidade inválido: " + nivelGravidade + "o valor nivel deve estar entre 0 e 5");
        }

        leitorLinha.nextLine();

        System.out.println("Insira a quantidade de envolvidos na ocorrência");
        Integer numeroEnvolvidos = leitor.nextInt();

        if (numeroEnvolvidos < 0) {
            System.out.println("Quantidade de envolvidos inválida: " + numeroEnvolvidos + ". A quantidade de envolvidos deve ser acima ou igual a 0.");
        }

        leitorLinha.nextLine();

        System.out.println("Insira o resumo da ocorrência");
        String resumoOcorrencia = leitorLinha.nextLine();

        System.out.println("Número da ocorrência: " + numeroOcorrencia);
        System.out.println("Tipo da ocorrência: " + tipo);
        System.out.println("Local da ocorrência: " + local);
        System.out.println("Gravidade da ocorrência: " + nivelGravidade);
        System.out.println("Quantidade de envolvidos na ocorrência: " + numeroEnvolvidos);
        System.out.println("Resumo da ocorrência: " + resumoOcorrencia);
    }
}
