package com.mires.paint.services.user;

import com.mires.paint.entities.responses.error.ErrorResponse;
import com.mires.paint.entities.responses.user.UserResponse;
import com.mires.paint.entities.user.User;
import com.mires.paint.entities.user.UserOTD;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserService {
    private final MongoDatabase database;

    public UserService(final MongoClient mongoClient) {
        this.database = mongoClient.getDatabase("paint");
    }

    public Mono<UserResponse> findByLogin(final String login) {
        return Mono.from(database.getCollection("users", User.class)
                        .find(new Document("login", login))
                        .first())
                .mapNotNull(user -> {
                    if (user == null) {
                        return new UserResponse(null, new ErrorResponse("User not found", "No user found with the provided login."));
                    }
                    return new UserResponse(new User(user.get_id(), user.getLogin(), user.getPassword(),
                            user.getEmail(), user.getName(), user.getSurname(), user.getRole()), null);
                })
                .switchIfEmpty(Mono.just(new UserResponse(null, new ErrorResponse("User not found", "No user found with the provided login."))));
    }

    public Mono<User> findById(final int id) {
        return Mono.from(database.getCollection("users", User.class)
                        .find(new Document("_id", id))
                        .first())
                .mapNotNull(user -> {
                    if (user == null) {
                        return null;
                    }
                    return new User(user.get_id(), user.getLogin(), user.getPassword(),
                            user.getEmail(), user.getName(), user.getSurname(), user.getRole());
                });
    }

    public Mono<User> createUser(final UserOTD userOTD) {
        final User user = new User(
                0,
                userOTD.getLogin(),
                userOTD.getPassword(),
                userOTD.getEmail(),
                userOTD.getName(),
                userOTD.getSurname(),
                userOTD.getRole()
        );
        return generateUniqueUserId()
                .flatMap(id -> {
                    user.set_id(id);
                    return Mono.from(database.getCollection("users", User.class)
                                    .insertOne(user))
                            .thenReturn(user);
                });
    }

    public Mono<Integer> getNextUserId() {
        MongoCollection<Document> counters = database.getCollection("counters");
        return Mono.from(
                counters.findOneAndUpdate(
                        Filters.eq("_id", "users"),
                        Updates.inc("seq", 1),
                        new FindOneAndUpdateOptions()
                                .upsert(true)
                                .returnDocument(ReturnDocument.AFTER)
                )
        ).map(doc -> doc.getInteger("seq"));
    }

    private Mono<Integer> generateUniqueUserId() {
        return getNextUserId()
                .flatMap(id ->
                        findById(id)
                                .flatMap(existingUser -> {
                                    // If a user with this ID exists, try again recursively
                                    if (existingUser != null) {
                                        return generateUniqueUserId();
                                    } else {
                                        return Mono.just(id);
                                    }
                                })
                                .switchIfEmpty(Mono.just(id)) // if findById returns empty, ID is safe
                );
    }

    public Mono<UserResponse> deleteUser(final int id) {
        MongoCollection<User> collection = database.getCollection("users", User.class);

        return findById(id)
                .flatMap(existingUser -> {
                    if (existingUser == null) {
                        return Mono.just(new UserResponse(null,
                                new ErrorResponse("User not found", "No user with ID " + id + " exists.")));
                    }

                    return Mono.from(collection.deleteOne(new Document("_id", id)))
                            .thenReturn(new UserResponse(existingUser, null));
                })
                .switchIfEmpty(Mono.just(new UserResponse(null,
                        new ErrorResponse("User not found", "No user with ID " + id + " exists."))));
    }

    public Mono<UserResponse> updateUser(final User user) {
        return findById(user.get_id())
                .flatMap(existingUser -> {
                    if (existingUser == null) {
                        return Mono.just(new UserResponse(null,
                                new ErrorResponse("User not found", "No user with ID " + user.get_id() + " exists.")));
                    }

                    return Mono.from(database.getCollection("users", User.class)
                                    .replaceOne(new Document("_id", user.get_id()), user))
                            .thenReturn(new UserResponse(user, null));
                })
                .switchIfEmpty(Mono.just(new UserResponse(null,
                        new ErrorResponse("User not found", "No user with ID " + user.get_id() + " exists."))));
    }

    public Mono<UserResponse> login(final String login, final String password) {
        return findByLogin(login)
                .flatMap(response -> {
                    if (response.getUser() == null)
                        return Mono.just(response);
                    if (response.getUser().getPassword().equals(password))
                        return Mono.just(response);
                    else
                        return Mono.just(new UserResponse(null, new ErrorResponse("Invalid credentials", "The provided login or password is incorrect.")));
                });
    }
}
