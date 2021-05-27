package com.tweetConversations.tweetConversations.repo;

import com.tweetConversations.tweetConversations.model.Tweet;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TweetRepository extends Neo4jRepository<Tweet, Long> {

}
