package com.mires.paint.entities.responses.feed;

import com.mires.paint.entities.channel.Channel;
import com.mires.paint.entities.feed.Feed;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class FeedResponse implements Serializable {
    private final Channel channel;
    private final List<Feed> feeds;
}
