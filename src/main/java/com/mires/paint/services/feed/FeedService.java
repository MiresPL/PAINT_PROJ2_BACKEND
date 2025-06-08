package com.mires.paint.services.feed;

import com.mires.paint.entities.responses.error.ErrorResponse;
import com.mires.paint.entities.feed.Feed;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.Getter;
import org.bson.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class FeedService {
    private final MongoDatabase database;
    @Getter
    private final Set<Feed> editedFeeds = Collections.synchronizedSet(new HashSet<>());

    public FeedService(final MongoClient mongoClient) {
        this.database = mongoClient.getDatabase("paint");
    }

    public Mono<Void> saveFeeds(List<Feed> feeds) {
        return Flux.fromIterable(feeds)
                .flatMap(this::upsertFeed)
                .then();
    }

    public Mono<Feed> getFeedFromDbByID(final double id, final LocalDateTime createdAt) {
        final String date = createdAt.toLocalDate().toString().replace("-", "_");
        final String collectionName = "feeds_" + date;

        final MongoCollection<Document> collection = database.getCollection(collectionName, Document.class);

        return Mono.from(collection.find(Filters.eq("_id", id)).first())
                .mapNotNull(doc -> {
                    if (doc == null) {
                        return null;
                    }
                    return new Feed(
                            doc.getString("created_at"),
                            doc.getDouble("_id"),
                            doc.getString("field1"),
                            doc.getString("field2"),
                            doc.getString("field3")
                    );
                });
    }

    public Mono<ResponseEntity<?>> upsertFeed(Feed feed) {
        final String date = feed.getCreated_at().substring(0, 10);
        final String collectionName = "feeds_" + date.replace("-", "_");

        final MongoCollection<Document> collection = database.getCollection(collectionName, Document.class);

        System.out.println(feed.getCreated_at());
        System.out.println(Instant.parse(feed.getCreated_at()).atZone(ZoneId.of("Europe/Warsaw")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        final Document doc = new Document("_id", feed.getEntry_id())
                .append("created_at", Instant.parse(feed.getCreated_at()).atZone(ZoneId.of("Europe/Warsaw")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .append("field1", feed.getField1())
                .append("field2", feed.getField2())
                .append("field3", feed.getField3());

        final ReplaceOptions options = new ReplaceOptions().upsert(true);

        return Mono.from(collection.replaceOne(Filters.eq("_id", feed.getEntry_id()), doc, options))
                .map(result -> {
                    if (result.getModifiedCount() > 0 || result.getUpsertedId() != null) {
                        editedFeeds.add(feed);
                        return ResponseEntity.ok("Feed upserted successfully");
                    }
                    return ResponseEntity.ok(new ErrorResponse("", "Successfully inserted new feed"));
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().body(new ErrorResponse("Error upserting feed with id: " + feed.getEntry_id(), e.getMessage())))
                );
    }

    public Mono<List<Document>> getFeedsFromDb(LocalDateTime start, LocalDateTime end) {
        final List<String> dates = getDatesInRange(start.toLocalDate(), end.toLocalDate());

        final List<Mono<List<Document>>> feedFetchMonos = dates.stream()
                .map(date -> {
                    String collectionName = "feeds_" + date.replace("-", "_");
                    MongoCollection<Document> collection = database.getCollection(collectionName);
                    return Flux.from(collection.find(Filters.and(
                                    Filters.gte("created_at", start.toString()),
                                    Filters.lte("created_at", end.toString())
                            )))
                            .collectList()
                            .onErrorResume(e -> Mono.just(List.of()));
                })
                .toList();

        return Flux.merge(feedFetchMonos)
                .flatMap(Flux::fromIterable)
                .collectList();
    }

    private List<String> getDatesInRange(LocalDate start, LocalDate end) {
        final List<String> dates = new ArrayList<>();
        while (!start.isAfter(end)) {
            dates.add(start.toString());
            start = start.plusDays(1);
        }
        return dates;
    }
}
