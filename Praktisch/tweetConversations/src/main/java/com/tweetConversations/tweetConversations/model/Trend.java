package com.tweetConversations.tweetConversations.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node
@Data
@Builder
public class Trend {
	@Id
	@GeneratedValue
	private Long id;

	private String title;
}
