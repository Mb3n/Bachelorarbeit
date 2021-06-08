package com.tweetConversations.tweetConversations.repo;

import com.tweetConversations.tweetConversations.model.Tweet;
import java.util.Optional;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;
import twitter4j.Status;

@Repository
public interface TweetRepository extends Neo4jRepository<Tweet, Long> {
	Tweet findTweetByTweetId(long tweetId);
}
