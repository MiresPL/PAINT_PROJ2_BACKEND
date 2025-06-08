package com.mires.paint.controllers.charts;

import com.mires.paint.common.Common;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api")
public class ChartController {

    @GetMapping(value = "/chart", produces = MediaType.TEXT_HTML_VALUE)
    @CrossOrigin
    public String getChartHtml(
            @RequestParam Optional<String> field,
            @RequestParam Optional<String> title,
            @RequestParam Optional<Integer> width,
            @RequestParam Optional<Integer> height,
            @RequestParam Optional<String> start,
            @RequestParam Optional<String> end,
            @RequestParam Optional<String> color
    ) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String defaultStart = LocalDateTime.now().minusHours(2).format(fmt);
        String defaultEnd = LocalDateTime.now().format(fmt);

        WebClient webClient = WebClient.create();

        final Mono<String> response = webClient.get()
                .uri(uriBuilder -> {
                    UriBuilder builder = uriBuilder
                            .scheme("https")
                            .host("api.thingspeak.com")
                            .path("/channels/" + Common.CHANNEL_ID + "/charts/" + field.orElse("1"))
                            .queryParam("api_key", Common.READ_KEY)
                            .queryParam("color", color.orElse("000000"))
                            .queryParam("width", width.orElse(480))
                            .queryParam("height", height.orElse(640))
                            .queryParam("start", start.orElse(defaultStart))
                            .queryParam("end", end.orElse(defaultEnd))
                            .queryParam("timezone", "Europe%2FWarsaw");
                    title.ifPresent(t -> builder.queryParam("title", t));

                    return builder.build();
                })
                .retrieve()
                .bodyToMono(String.class);

        return Objects.requireNonNull(response.block())
                .replaceAll("src=\"/", "src=\"https://api.thingspeak.com/")
                .replaceAll("href=\"/", "href=\"https://api.thingspeak.com/");
    }
}
