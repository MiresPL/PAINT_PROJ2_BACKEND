package com.mires.paint.controllers.feeds;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mires.paint.common.Common;
import com.mires.paint.common.GsonFactory;
import com.mires.paint.entities.responses.error.ErrorResponse;
import com.mires.paint.entities.feed.Feed;
import com.mires.paint.entities.responses.feed.FeedResponse;
import com.mires.paint.services.feed.FeedService;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/feeds")
public class FeedController {

    private final FeedService feedService;

    public FeedController(final FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<Feed>> getFeeds() {
        final Gson gson = GsonFactory.getCompactGson();
        final WebClient webClient = WebClient.create();

        final Mono<String> response = webClient.get()
                .uri("https://api.thingspeak.com/channels/" + Common.CHANNEL_ID + "/feed.json?api_key=" + Common.READ_KEY + "&timezone=Europe%2FWarsaw&results=8000")
                .retrieve()
                .bodyToMono(String.class);

        final String jsonResponse = response.block();
        final FeedResponse feedResponse = gson.fromJson(jsonResponse, FeedResponse.class);


        return response.map(jsonString -> gson.fromJson(jsonString, FeedResponse.class))
                .flatMap(response1 -> feedService.saveFeeds(response1.getFeeds()).thenReturn(feedResponse.getFeeds()));
    }

    @GetMapping(value = "/range", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> getFeedsInRange(
            @RequestParam(value = "start", required = false) String startStr,
            @RequestParam(value = "end", required = false) String endStr) {

        if (startStr == null || endStr == null) {
            return Mono.just(ResponseEntity
                    .badRequest()
                    .body(new ErrorResponse(
                            HttpStatus.BAD_REQUEST.value() + " " + HttpStatus.BAD_REQUEST.getReasonPhrase(),
                            "Missing required parameters: 'start' and/or 'end'."
                    )));
        }

        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;

        try {
            startDateTime = LocalDateTime.parse(URLDecoder.decode(startStr, StandardCharsets.UTF_8), formatter);
            endDateTime = LocalDateTime.parse(URLDecoder.decode(endStr, StandardCharsets.UTF_8), formatter);
        } catch (Exception e) {
            return Mono.just(ResponseEntity
                    .badRequest()
                    .body(new ErrorResponse(
                            HttpStatus.BAD_REQUEST.value() + " " + HttpStatus.BAD_REQUEST.getReasonPhrase(),
                            "Invalid date format. Expected: yyyy-MM-dd HH:mm:ss"
                    )));
        }

        if (endDateTime.isBefore(startDateTime)) {
            return Mono.just(ResponseEntity
                    .badRequest()
                    .body(new ErrorResponse(
                            HttpStatus.BAD_REQUEST.value() + " " + HttpStatus.BAD_REQUEST.getReasonPhrase(),
                            "End date must be after Start date."
                    )));
        }

        final WebClient webClient = WebClient.create();
        final Mono<List<Document>> feedsMono = feedService.getFeedsFromDb(startDateTime, endDateTime);


        return feedsMono.flatMap(feeds -> {
            if (!feeds.isEmpty()) {
                return Mono.just(ResponseEntity.ok(feeds));
            } else {
                // Try to fetch/update
                return webClient.get().uri("http://localhost:8080/api/feeds/").retrieve().bodyToMono(Void.class)
                        .then(feedService.getFeedsFromDb(startDateTime, endDateTime))
                        .flatMap(updatedFeeds -> {
                            if (!updatedFeeds.isEmpty()) {
                                return Mono.just(ResponseEntity.ok(updatedFeeds));
                            } else {
                                return Mono.just(ResponseEntity
                                        .badRequest()
                                        .body(new ErrorResponse(
                                                HttpStatus.NOT_FOUND.value() + " " + HttpStatus.NOT_FOUND.getReasonPhrase(),
                                                "No feeds found in the given range even after update."
                                        )));
                            }
                        });
            }
        });
    }


    @PostMapping("/edit")
    public Mono<ResponseEntity<?>> editFeed(@RequestBody Feed feed) {
        return feedService.upsertFeed(feed);
    }

    @GetMapping(value = "/push-edits", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> pushEdits() {
        if (feedService.getEditedFeeds().isEmpty())
            return Mono.just(ResponseEntity.ok(new ErrorResponse("", "There was no edited feeds to be pushed.")));


        return Flux.merge(feedService.getEditedFeeds().stream().map(Mono::just).toList())
                .collectList()
                .publishOn(Schedulers.boundedElastic())
                .publishOn(Schedulers.boundedElastic())
                .flatMap(feeds -> {
                    final JsonObject obj = new JsonObject();
                    obj.addProperty("write_api_key", Common.WRITE_KEY);
                    final JsonArray array = new JsonArray();
                    feeds.forEach(feed -> {
                        final JsonObject feedObj = new JsonObject();
                        feedObj.addProperty("created_at", feed.getCreated_at());
                        feedObj.addProperty("field1", feed.getField1());
                        feedObj.addProperty("field2", feed.getField2());
                        feedObj.addProperty("field3", feed.getField3());

                        array.add(feedObj);
                    });
                    obj.add("updates", array);

                    final WebClient client = WebClient.create();
                    final Mono<String> response = client.post()
                            .uri("https://api.thingspeak.com/channels/" + Common.CHANNEL_ID + "/bulk_update.json")
                            .bodyValue(obj)
                            .header("Content-Type", "application/json")
                            .retrieve()
                            .bodyToMono(String.class);

                    final String jsonResponse = response.block();

                    assert jsonResponse != null;
                    if (jsonResponse.contains("success"))
                        return Mono.just(ResponseEntity.ok("Successfully pushed edited feeds to external site."));

                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
                });
    }
}
