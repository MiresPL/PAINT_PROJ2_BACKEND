package com.mires.paint.entities.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserOTD implements Serializable {
    public String login, password, email, name, surname, role;
}
