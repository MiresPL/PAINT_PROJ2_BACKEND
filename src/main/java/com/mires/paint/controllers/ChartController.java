package com.mires.paint.controllers;

import com.mires.paint.common.Common;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/chart")
public class ChartController {

    private static final String BASE_CHART_URL = "https://api.thingspeak.com/channels/" + Common.CHANNEL_ID + "/charts/1";

    @GetMapping(value = "", produces = MediaType.TEXT_HTML_VALUE)
    @CrossOrigin
    public String getChartHtml(
            @RequestParam Optional<String> title,
            @RequestParam Optional<Integer> width,
            @RequestParam Optional<Integer> height,
            @RequestParam Optional<String> start,
            @RequestParam Optional<String> end
    ) {
        String url = buildChartUrl(title, width, height, start, end);


        final WebClient webClient = WebClient.create();

        final Mono<String> response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class);

        return response.block()
                .replaceAll("src=\"/", "src=\"https://api.thingspeak.com/")
                .replaceAll("href=\"/", "href=\"https://api.thingspeak.com/");
    }

    private String buildChartUrl(
            Optional<String> title,
            Optional<Integer> width,
            Optional<Integer> height,
            Optional<String> start,
            Optional<String> end
    ) {
        StringBuilder urlBuilder = new StringBuilder(BASE_CHART_URL)
                .append("?api_key=").append(Common.READ_KEY)
                .append("&color=000000");

        title.ifPresent(t -> urlBuilder.append("&title=").append(encode(t)));
        urlBuilder.append("&width=").append(width.orElse(480));
        urlBuilder.append("&height=").append(height.orElse(640));
        start.ifPresent(s -> urlBuilder.append("&start=").append(encode(s)));
        end.ifPresent(e -> urlBuilder.append("&end=").append(encode(e)));

        return urlBuilder.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
