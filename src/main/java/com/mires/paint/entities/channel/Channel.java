package com.mires.paint.entities.channel;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class Channel implements Serializable {
    private final double id;
    private final String name;
    private final String latitude;
    private final String longitude;
    private final String field1;
    private final String field2;
    private final String field3;
    private final String created_at;
    private final String updated_at;
    private final double last_entry_id;
}
