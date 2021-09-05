package com.tweetConversations.tweetConversations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import twitter4j.TwitterException;

@SpringBootApplication
public class TweetConversationsApplication {

	public static void main(String[] args) throws TwitterException, InterruptedException {
		ApplicationContext applicationContext = SpringApplication.run(TweetConversationsApplication.class, args);

		TweetService service = applicationContext.getBean(TweetService.class);
		service.startDiscussion();

	}

}
