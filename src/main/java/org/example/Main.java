package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;

public class Main {
    //IMPORTANTE: SUBSTITUA "SUA_CHAVE_DE_API_AQUI" PELA CHAVE QUE VOCÊ OBTEVE NO SITE DA EXCHANGERATE-API
    private static final String API_KEY = "SUA-CHAVE-API"; // <<<<<<<<<<< COLOQUE SUA CHAVE AQUI >>>>>>>>>>>
    private static final String API_BASE_URL = "https://www.exchangerate-api.com/";

    // Conjunto de moedas válidas para facilitar a validação
    private static final Set<String> MOEDAS_VALIDAS = new HashSet<>();

    // Bloco estático para popular a lista de moedas válidas uma única vez
    static {
        MOEDAS_VALIDAS.add("ARS"); // Peso argentino
        MOEDAS_VALIDAS.add("BOB"); // Boliviano boliviano
        MOEDAS_VALIDAS.add("BRL"); // Real brasileiro (origem fixa)
        MOEDAS_VALIDAS.add("CLP"); // Peso chileno
        MOEDAS_VALIDAS.add("COP"); // Peso colombiano
        MOEDAS_VALIDAS.add("USD"); // Dólar americano
        MOEDAS_VALIDAS.add("EUR"); // Euro
        MOEDAS_VALIDAS.add("GBP"); // Libra Esterlina
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean continuar = true; // Variável de controle do loop

        System.out.println("--- Bem-vindo ao Conversor de Moedas (Real Brasileiro para outras moedas - Tempo Real) ---");

        while (continuar) { // O loop principal começa aqui
            System.out.println("\n------------------------------------------------------------------");
            System.out.println("Moeda de origem: BRL (Real Brasileiro)");
            System.out.println("Moedas de destino disponíveis:");
            // Lista todas as moedas válidas, exceto BRL, que é a origem fixa.
            MOEDAS_VALIDAS.stream()
                    .filter(c -> !c.equals("BRL"))
                    .forEach(c -> System.out.println("  - " + c + " (" + obterNomeMoeda(c) + ")"));
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

            System.out.print("Digite o código da moeda de destino: ");
            String moedaDestino = scanner.next().toUpperCase();

            // Validação da moeda de destino
            if (!MOEDAS_VALIDAS.contains(moedaDestino) || moedaDestino.equals("BRL")) {
                System.err.println("Erro: Moeda de destino inválida ou igual à moeda de origem (BRL). Escolha uma das moedas listadas e diferente de BRL.");
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
                System.err.println("  2. Se a sua API_KEY está correta no código."); // Mensagem ajustada
                System.err.println("  3. Se você não excedeu o limite de requisições da API.");
                System.err.println("  4. Se a moeda de destino que você digitou é suportada pela API (a lista exibida é um guia).");
                System.err.println("----------------------------------------------------------");
            } catch (IllegalArgumentException e) {
                System.err.println("Erro de argumento: " + e.getMessage());
            }

            // Pergunta ao usuário se deseja fazer outra conversão
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
        // Se as moedas de origem e destino forem as mesmas, retorna o valor original.
        if (moedaOrigem.equals(moedaDestino)) {
            return valor;
        }

        // Cria uma nova instância de HttpClient para cada requisição.
        HttpClient client = HttpClient.newHttpClient();
        String url = API_BASE_URL + API_KEY + "/pair/" + moedaOrigem + "/" + moedaDestino + "/" + valor;

        // Constrói a requisição HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        // Envia a requisição e recebe a resposta
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Verifica o código de status da resposta HTTP (200 OK é sucesso)
        if (response.statusCode() != 200) {
            throw new IOException("Erro na requisição à API: Código " + response.statusCode() + " - " + response.body());
        }

        // Usa Gson para parsear a string JSON da resposta em um objeto JsonObject
        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);

        // Verifica o resultado da operação da API
        String result = jsonResponse.get("result").getAsString();
        if ("success".equals(result)) {
            // Retorna o valor convertido obtido da resposta JSON
            return jsonResponse.get("conversion_result").getAsDouble();
        } else {
            // Lida com erros reportados pela própria API (ex: chave inválida, moeda não suportada pela API)
            String errorType = jsonResponse.get("error-type").getAsString();
            throw new IOException("Erro da API: " + errorType + " (Verifique sua API Key ou limites de uso)");
        }
    }

    /**
     * Retorna o nome completo da moeda a partir do seu código.
     * @param codigoMoeda O código da moeda (ex: "BRL").
     * @return O nome completo da moeda.
     */
    private static String obterNomeMoeda(String codigoMoeda) {
        return switch (codigoMoeda) {
            case "ARS" -> "Peso Argentino";
            case "BOB" -> "Boliviano Boliviano";
            case "BRL" -> "Real Brasileiro";
            case "CLP" -> "Peso Chileno";
            case "COP" -> "Peso Colombiano";
            case "USD" -> "Dólar Americano";
            case "EUR" -> "Euro";
            case "GBP" -> "Libra Esterlina";
            default -> "Moeda Desconhecida";
        };
    }
}
