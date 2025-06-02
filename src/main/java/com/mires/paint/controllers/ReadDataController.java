package com.mires.paint.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mires.paint.common.Common;
import com.mires.paint.common.GsonFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/readData")
public class ReadDataController {
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getReadData() {
        final Gson gson = GsonFactory.getCompactGson();

        final WebClient webClient = WebClient.create();

        final Mono<String> response = webClient.get()
                .uri("https://api.thingspeak.com/channels/" + Common.CHANNEL_ID + "/feed.json?api_key=" + Common.READ_KEY)
                .retrieve()
                .bodyToMono(String.class);

        final Map<String, Object> jsonMap = gson.fromJson(response.block(), new TypeToken<Map<String, Object>>(){}.getType());

        //TODO finish reading data in JSON format and create the whole process of dividing the data per day and put it into database

        return jsonMap;
    }
}
