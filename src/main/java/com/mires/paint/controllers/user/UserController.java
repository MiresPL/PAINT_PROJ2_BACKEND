package com.mires.paint.controllers.user;

import com.mires.paint.entities.requests.delete.DeleteRequest;
import com.mires.paint.entities.requests.login.LoginRequest;
import com.mires.paint.entities.responses.error.ErrorResponse;
import com.mires.paint.entities.responses.user.UserResponse;
import com.mires.paint.entities.user.User;
import com.mires.paint.entities.user.UserOTD;
import com.mires.paint.services.user.UserService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(final UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/create")
    public Mono<UserResponse> createUser(@RequestBody UserOTD user) {
        //add check for login existing but not as a return

        return userService.findByLogin(user.getLogin())
                .flatMap(existingUser -> {
                    if (existingUser.getUser() != null) {
                        return Mono.just(new UserResponse(null, new ErrorResponse("Login already exists", "")));
                    }
                    return userService.createUser(user)
                            .map(createdUser -> new UserResponse(createdUser, null))
                            .onErrorResume(e -> Mono.just(new UserResponse(null, new ErrorResponse("","Error creating user: " + e.getMessage()))));
                })
                .switchIfEmpty(userService.createUser(user)
                        .map(createdUser -> new UserResponse(createdUser, null))
                        .onErrorResume(e -> Mono.just(new UserResponse(null, new ErrorResponse("","Error creating user: " + e.getMessage())))));
    }

    @PostMapping("/login")
    public Mono<UserResponse> loginUser(@RequestBody LoginRequest loginRequest) {
        return userService.login(loginRequest.getLogin(), loginRequest.getPassword());
    }

    @PostMapping("/edit")
    public Mono<UserResponse> editUser(@RequestBody User user) {
        return userService.updateUser(user)
                .onErrorResume(e -> Mono.just(new UserResponse(null,
                        new ErrorResponse("Update Error", e.getMessage()))));
    }

    @PostMapping("/delete")
    public Mono<UserResponse> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        return userService.deleteUser(deleteRequest.getId())
                .onErrorResume(e -> Mono.just(new UserResponse(null,
                        new ErrorResponse("Delete Error", e.getMessage()))));
    }

    @GetMapping("/list")
    public Mono<List<User>> listUsers() {
        return userService.listUsers();
    }
}

