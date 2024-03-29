package com.tweetConversations.tweetConversations.model;

import java.util.Date;
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
public class Tweet {
	@Id
	@GeneratedValue
	private Long id;

	private String title;

	private long tweetId;

	@Relationship(type="REPLYTO", direction=Relationship.Direction.OUTGOING)
	private Tweet reply;

	@Relationship(type="PARTOF", direction=Relationship.Direction.OUTGOING)
	private Trend trend;

	private Date createdAt;

	private Boolean rootTweet;

	private Integer depth;

	private Integer nodes;

	private Integer justDirectReply;

	private List<Integer> allDepths;
}
