package com.giulio.meteoapp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;

public class MeteoApp extends TelegramLongPollingBot {

    private static final HttpClient client = HttpClient.newHttpClient();

    @Override
    public String getBotUsername() {
        return "MeteoBot_Giulio";
    }

    @Override
    public String getBotToken() {
        return "8160309659:AAGzyLy6jNL89pKkOXNiKh9Kpv-wcpXE_1I";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String cityName = update.getMessage().getText().trim();

            String risposta = "Cerco meteo per: " + cityName + "...";
            sendMsg(update, risposta);

            try {
                String meteo = getMeteoPerCitta(cityName);
                sendMsg(update, meteo);
            } catch (Exception e) {
                sendMsg(update, "Errore nel recuperare i dati meteo.");
                e.printStackTrace();
            }
        }
    }

    private void sendMsg(Update update, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(update.getMessage().getChatId().toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String getMeteoPerCitta(String cityName) throws IOException, InterruptedException {
        // 1. Trovo coordinate con Open-Meteo Geocoding API (prendo solo 1 risultato)
        String geoUrl = String.format(
                "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=en&format=json",
                cityName.replace(" ", "%20")
        );

        HttpRequest geoRequest = HttpRequest.newBuilder()
                .uri(URI.create(geoUrl))
                .build();
        HttpResponse<String> geoResponse = client.send(geoRequest, HttpResponse.BodyHandlers.ofString());

        JsonObject geoJson = JsonParser.parseString(geoResponse.body()).getAsJsonObject();
        JsonArray results = geoJson.getAsJsonArray("results");

        if (results == null || results.size() == 0) {
            return "Città non trovata, scrivi meglio.";
        }

        JsonObject location = results.get(0).getAsJsonObject();
        double latitude = location.get("latitude").getAsDouble();
        double longitude = location.get("longitude").getAsDouble();
        String name = location.get("name").getAsString();

        // 2. Chiamata meteo con coordinate, prendo temperatura, umidità e vento correnti
        String weatherUrl = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,wind_speed_10m",
                latitude, longitude
        );

        HttpRequest weatherRequest = HttpRequest.newBuilder()
                .uri(URI.create(weatherUrl))
                .build();
        HttpResponse<String> weatherResponse = client.send(weatherRequest, HttpResponse.BodyHandlers.ofString());

        JsonObject weatherJson = JsonParser.parseString(weatherResponse.body()).getAsJsonObject();
        JsonObject current = weatherJson.getAsJsonObject("current");

        if (current == null) {
            return "Dati meteo non disponibili.";
        }

        double temp = current.get("temperature_2m").getAsDouble();
        double humidity = current.get("relative_humidity_2m").getAsDouble();
        double windSpeed = current.get("wind_speed_10m").getAsDouble();

        return String.format("Meteo per %s:\nTemperatura: %.1f °C\nUmidità: %.1f %%\nVento: %.1f km/h",
                name, temp, humidity, windSpeed);
    }


    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new MeteoApp());
            System.out.println("Bot avviato...");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
