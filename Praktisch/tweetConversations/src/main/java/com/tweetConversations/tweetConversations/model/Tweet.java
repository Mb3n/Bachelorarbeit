package com.tweetConversations.tweetConversations.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node
@Data
@Builder
public class Tweet {
	@Id
	@GeneratedValue
	private Long id;

	private String title;

	private Integer tweetId;

	@Relationship(type="REPLYTO", direction=Relationship.Direction.OUTGOING)
	private Tweet reply;
}
