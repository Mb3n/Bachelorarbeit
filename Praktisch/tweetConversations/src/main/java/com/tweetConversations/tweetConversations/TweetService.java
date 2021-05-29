package com.tweetConversations.tweetConversations;

import com.tweetConversations.tweetConversations.model.Tweet;
import com.tweetConversations.tweetConversations.repo.TweetRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Trends;
import twitter4j.Twitter;
import twitter4j.TwitterException;

@Service
public class TweetService {

	@Autowired
	private Twitter twitter;

	@Autowired
	private TweetRepository tweetRepository;

	public List<String> getLatestTweets() throws TwitterException {

		List<String> tweets = new ArrayList<>();
		try {
			ResponseList<Status> homeTimeline = twitter.getHomeTimeline();
			for (Status status : homeTimeline) {
				tweets.add(status.getText());
				System.out.println("DISCUSSION: " + getDiscussion(status, twitter));

			}
		} catch (TwitterException e) {
			throw new RuntimeException(e);
		}
		return tweets;
	}

	public void startDiscussion() throws TwitterException {
		ResponseList<Status> homeTimeline = twitter.getHomeTimeline();
		List<Status> status = getDiscussion(homeTimeline.get(5), twitter);

		Tweet rootTweet = Tweet.builder().tweetId(homeTimeline.get(5).getId()).title(homeTimeline.get(5).getText()).build();
		tweetRepository.save(rootTweet);

		status.forEach(reply -> {
			Tweet tweet = Tweet.builder().tweetId(reply.getId()).title(reply.getText()).reply(rootTweet).build();
			tweetRepository.save(tweet);
		});

	}

	public ArrayList<Status> getDiscussion(Status status, Twitter twitter) {
		ArrayList<Status> replies = new ArrayList<>();

		ArrayList<Status> all = null;

		try {
			long id = status.getId();
			String screenname = status.getUser().getScreenName();

			Query query = new Query("@" + screenname + " since_id:" + id);

			System.out.println("query string: " + query.getQuery());

			try {
				query.setCount(100);
			} catch (Throwable e) {
				// enlarge buffer error?
				query.setCount(30);
			}

			QueryResult result = twitter.search(query);
			System.out.println("result: " + result.getTweets().size());

			all = new ArrayList<Status>();

			do {
				System.out.println("do loop repetition");

				List<Status> tweets = result.getTweets();

				for (Status tweet : tweets)
					if (tweet.getInReplyToStatusId() == id)
						all.add(tweet);

				if (all.size() > 0) {
					for (int i = all.size() - 1; i >= 0; i--)
						replies.add(all.get(i));
					all.clear();
				}

				query = result.nextQuery();

				if (query != null)
					result = twitter.search(query);

			} while (query != null);

		} catch (Exception e) {
			e.printStackTrace();
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		}
		return replies;
	}
}
