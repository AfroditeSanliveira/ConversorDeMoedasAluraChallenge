package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

public class Main {
    private static final String API_KEY = "SUA-CHAVE-AQUI";
    private static final String API_BASE_URL = "LINK";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean continuar = true; // Variável de controle do loop

        System.out.println("--- Bem-vindo ao Conversor de Moedas (Real para outras moedas - Tempo Real) ---");

        while (continuar) { // O loop principal começa aqui
            System.out.println("\n------------------------------------------------------------------");
            System.out.println("Moeda de origem: BRL (Real)");
            System.out.println("Moedas de destino disponíveis: USD (Dólar), EUR (Euro), GBP (Libra)");
            System.out.println("------------------------------------------------------------------");

            System.out.print("Digite o valor em BRL a ser convertido: ");
            double valor = -1; // Inicializa com valor inválido
            try {
                valor = scanner.nextDouble();
            } catch (java.util.InputMismatchException e) {
                System.err.println("Entrada inválida. Por favor, digite um número para o valor.");
                scanner.next(); // Consome a entrada inválida para evitar loop infinito
                continue; // Volta para o início do loop
            }

            String moedaOrigem = "BRL"; // Moeda de origem FIXA em Real

            System.out.print("Digite o código da moeda de destino (USD, EUR, GBP): ");
            String moedaDestino = scanner.next().toUpperCase();

            // Validação específica para as moedas de destino permitidas
            if (!moedaDestino.equals("USD") && !moedaDestino.equals("EUR") && !moedaDestino.equals("GBP")) {
                System.err.println("Erro: Moeda de destino inválida. Escolha entre USD, EUR ou GBP.");
                continue; // Volta para o início do loop
            }

            try {
                double valorConvertido = converter(valor, moedaOrigem, moedaDestino);
                System.out.printf("%.2f %s equivale a %.2f %s%n", valor, moedaOrigem, valorConvertido, moedaDestino);
            } catch (IOException | InterruptedException e) {
                System.err.println("-------------------------- ERRO --------------------------");
                System.err.println("Ocorreu um erro ao conectar à API ou processar a requisição:");
                System.err.println("Detalhes: " + e.getMessage());
                System.err.println("Verifique:");
                System.err.println("  1. Sua conexão com a internet.");
                System.err.println("  2. Se a sua API_KEY está correta no código (e se você a substituiu de 'SUA_CHAVE_DE_API_AQUI').");
                System.err.println("  3. Se você não excedeu o limite de requisições da API.");
                System.err.println("----------------------------------------------------------");
            } catch (IllegalArgumentException e) {
                System.err.println("Erro de argumento: " + e.getMessage());
            }

            // Pergunta ao usuário se deseja continuar
            System.out.print("\nDeseja fazer outra conversão? (sim/não): ");
            String resposta = scanner.next().toLowerCase();
            if (!resposta.equals("sim")) {
                continuar = false; // Define a condição para sair do loop
            }
        } // O loop principal termina aqui

        System.out.println("Obrigado por usar o conversor de moedas! Até a próxima.");
        scanner.close(); // Garante que o scanner seja sempre fechado ao final do programa
    }


    public static double converter(double valor, String moedaOrigem, String moedaDestino) throws IOException, InterruptedException {
        if (moedaOrigem.equals(moedaDestino)) {
            return valor;
        }

        HttpClient client = HttpClient.newHttpClient();
        String url = API_BASE_URL + API_KEY + "/pair/" + moedaOrigem + "/" + moedaDestino + "/" + valor;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Erro na requisição à API: Código " + response.statusCode() + " - " + response.body());
        }

        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);

        String result = jsonResponse.get("result").getAsString();
        if ("success".equals(result)) {
            return jsonResponse.get("conversion_result").getAsDouble();
        } else {
            String errorType = jsonResponse.get("error-type").getAsString();
            throw new IOException("Erro da API: " + errorType + " (Verifique sua API Key ou limites de uso)");
        }
    }

    private static boolean isValidCurrency(String currencyCode) {
        return currencyCode.equals("USD") || currencyCode.equals("EUR") ||
                currencyCode.equals("BRL") || currencyCode.equals("GBP");
    }
}