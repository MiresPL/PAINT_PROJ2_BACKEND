package com.mires.paint.entities.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class User implements Serializable {
    @BsonId
    public Integer _id;
    public String login, password, email, name, surname, role;
}
