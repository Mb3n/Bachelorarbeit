package com.tweetConversations.tweetConversations.api;

import com.tweetConversations.tweetConversations.TweetService;
import com.tweetConversations.tweetConversations.model.Tweet;
import com.tweetConversations.tweetConversations.repo.TweetRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import twitter4j.TwitterException;

@RestController
@RequestMapping("/rest/neo4j")
public class TweetController {

	@Autowired
	TweetRepository tweetRepository;

	@Autowired
	TweetService tweetService;

	@GetMapping
	public List<Tweet> getAll() throws TwitterException {

		//System.out.println("TWEETS: " + tweetService.getLatestTweets());

		//Tweet rootTweet = tweetRepository.findAll().get(0);

		//tweetRepository.save(Tweet.builder().tweetId(2).title("ausSpring").reply(rootTweet).build());

		tweetService.startDiscussion();
		//tweetService.getLatestTweets();

		return tweetRepository.findAll();
	}
}
