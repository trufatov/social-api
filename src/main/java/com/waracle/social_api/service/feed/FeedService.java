package com.waracle.social_api.service.feed;

import com.waracle.social_api.dto.response.FeedResponse;

public interface FeedService {

    FeedResponse getFeed(Long cursor, int limit);
}
