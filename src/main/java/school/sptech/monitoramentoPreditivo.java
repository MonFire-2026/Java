package school.sptech;

import java.sql.SQLOutput;
import java.util.Scanner;

public class monitoramentoPreditivo {
    static void main() {

        Scanner leitorValores = new Scanner(System.in);

        System.out.println("==================================================================");
        System.out.println("Defina os limites críticos para cada area");
        System.out.println("==================================================================\n");

        System.out.println("Digite o limite inicial Critico para a porcentagem da cpu: ");
        Float limiteCpu = leitorValores.nextFloat();
        leitorValores.nextLine();
        while (limiteCpu<0||limiteCpu>100){
            System.out.println("Valor Inválido, digite um valor de 0 a 100");
            limiteCpu = leitorValores.nextFloat();
            leitorValores.nextLine();
        }

        System.out.println("Digite o limite inicial Critico para a porcentagem de RAM: ");
        Float limiteRAM = leitorValores.nextFloat();
        leitorValores.nextLine();
        while (limiteRAM<0||limiteRAM>100){
            System.out.println("Valor Inválido, digite um valor de 0 a 100");
            limiteRAM = leitorValores.nextFloat();
            leitorValores.nextLine();
        }

        System.out.println("Digite o limite inicial Critico para a porcentagem do Disco: ");
        Float limiteDisco = leitorValores.nextFloat();
        leitorValores.nextLine();
        while (limiteDisco<0||limiteDisco>100){
            System.out.println("Valor Inválido, digite um valor de 0 a 100");
            limiteDisco = leitorValores.nextFloat();
            leitorValores.nextLine();
        }

        System.out.println("\n----------------------------------------------------------------------\n");

        System.out.println("\n=========================Valores definidos=====================");
        System.out.printf("Limite inicial de Criticidade da Cpu: %.2f%n" +
                "        \nLimite inicial de Criticidade da Ram: %.2f%n" +
                "        \nLimite inicial de Criticidade do Disco: %.2f%n" +
                "         ", limiteCpu, limiteDisco, limiteRAM);
        System.out.println("\n--------------------------------------------------------------------------\n");

        System.out.println("\n========Sistema de Simulção de Alertas==========\n");
        System.out.println("Digite o valor da Captura Cpu: ");
        Float valorCapturaCpu = leitorValores.nextFloat();
        while (valorCapturaCpu<0||valorCapturaCpu>100){
            System.out.println("Valor Inválido, digite um valor de 0 a 100");
            valorCapturaCpu = leitorValores.nextFloat();
            leitorValores.nextLine();
        }

        System.out.println("Digite o valor da Captura RAM: ");
        Float valorCapturaRAM = leitorValores.nextFloat();
        while (valorCapturaRAM<0||valorCapturaRAM>100){
            System.out.println("Valor Inválido, digite um valor de 0 a 100");
            valorCapturaRAM = leitorValores.nextFloat();
            leitorValores.nextLine();
        }

        System.out.println("Digite o valor da Captura Disco: ");
        Float valorCapturaDisco = leitorValores.nextFloat();
        while (valorCapturaDisco<0||valorCapturaDisco>100){
            System.out.println("Valor Inválido, digite um valor de 0 a 100");
            valorCapturaDisco = leitorValores.nextFloat();
            leitorValores.nextLine();
        }
        System.out.println("\n-----------------------------------------------------------------------\n");

        System.out.println("\n===================================Status das Leituras==================================================\n");

        if(limiteCpu<valorCapturaCpu){

            System.out.printf("Alerta: Nivel Critico Ultrapassado, o valor da captura da CPU foi de %.2f%n sendo que o limite inicial é de %.2f%n. Verifique o sistema antes que ele congele", valorCapturaCpu,limiteCpu);
        }else{

            System.out.println("Alerta: Tudo certo por aqui, a cpu está com o valor ideal para uso");
        }
        if(limiteRAM<valorCapturaRAM){
            System.out.printf("Alerta Nivel Critico Ultrapassado, o valor da captura da Ram foi de %.2f%n sendo que o limite inicial é de %.2f%n. Verifique o sistema antes que ele congele", valorCapturaRAM,limiteRAM);
        }else{

            System.out.println("Alerta: Tudo certo por aqui, a RAM está com o valor ideal para uso");
        }
        if(limiteDisco<valorCapturaDisco){
            System.out.printf("Alerta Nivel Critico Ultrapassado, o valor da captura do Disco foi de %.2f%n sendo que o limite inicial é de %.2f%n. Verifique o sistema antes que ele congele", valorCapturaDisco,limiteDisco);
        }else{

            System.out.println("Alerta: Tudo certo por aqui, o Disco está com o valor ideal para uso");
        }














    }






}

