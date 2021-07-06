package com.tweetConversations.tweetConversations.repo;

import com.tweetConversations.tweetConversations.model.Trend;
import java.util.Optional;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface TrendRepository extends Neo4jRepository<Trend, Long> {
	Optional<Trend> findByTitle(String title);
}
