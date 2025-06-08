package com.mires.paint.entities.requests.login;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class LoginRequest {
    private final String login;
    private final String password;
}
