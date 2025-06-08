package com.mires.paint.entities.feed;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class Feed {
    @BsonProperty("created_at")
    private final String created_at;
    @BsonId
    private final double entry_id;
    @BsonProperty("field1")
    private String field1;
    @BsonProperty("field2")
    private String field2;
    @BsonProperty("field3")
    private String field3;
}
