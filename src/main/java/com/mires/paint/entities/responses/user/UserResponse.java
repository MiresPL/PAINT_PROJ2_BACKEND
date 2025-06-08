package com.mires.paint.entities.responses.user;

import com.mires.paint.entities.responses.error.ErrorResponse;
import com.mires.paint.entities.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {
    private final User user;
    private final ErrorResponse errorResponse;
}
