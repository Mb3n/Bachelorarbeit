package com.tweetConversations.tweetConversations;

import com.tweetConversations.tweetConversations.model.Tweet;
import com.tweetConversations.tweetConversations.repo.TweetRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import twitter4j.Location;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Trends;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.URLEntity;

@Service
public class TweetService {

	private static final Integer GERMANY_LOCATION_WOEID = 23424829;

	@Autowired
	private Twitter twitter;

	@Autowired
	private TweetRepository tweetRepository;

	/*
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

	 */

	public void startDiscussion() throws TwitterException, InterruptedException {
		/*
		ResponseList<Location> locations;
		locations = twitter.getAvailableTrends();
		System.out.println("Showing available trends");
		for (Location location : locations) {
			System.out.println(location.getName() + " (woeid:" + location.getWoeid() + ")");
		}

		 */
		Trends trends = twitter.getPlaceTrends(GERMANY_LOCATION_WOEID);
		//for (int i = 0; i < trends.getTrends().length; i++) {
		//	if (trends.getTrends()[i].getTweetVolume() > 10000) {
				try {
					System.out.println("GERMAN TRENDS: " + trends.getTrends()[0]);

					Query query = new Query(trends.getTrends()[0].getQuery());
					query.setCount(100);
					QueryResult result = twitter.search(query);
					long mostFollowedUsersTweetId = getHighestInteractionTweet(result.getTweets()).getRetweetedStatus().getId();

					Query queryq = new Query(""+mostFollowedUsersTweetId);
					QueryResult resultq = twitter.search(queryq);
					Status mostFollowedUsersTweetT =resultq.getTweets().get(0);

					if (mostFollowedUsersTweetT.getQuotedStatus() != null) {
						System.out.println("QUATED STATUS: " + mostFollowedUsersTweetT.getQuotedStatus());

						//Query queryq1 = new Query(""+mostFollowedUsersTweetT.getQuotedStatus().getId());
						//QueryResult resultq1 = twitter.search(queryq1);
						Optional<Status> mostFollowedUsersTweetOpt = twitter.getUserTimeline(mostFollowedUsersTweetT.getQuotedStatus().getUser().getScreenName()).stream().filter(a -> a.getId() == mostFollowedUsersTweetT.getQuotedStatus().getId()).findFirst();

						mostFollowedUsersTweetOpt.ifPresent(mostFollowedUsersTweet -> {
							Tweet rootTweet = tweetRepository.save(Tweet.builder().tweetId(mostFollowedUsersTweet.getId()).title(mostFollowedUsersTweet.getText()).build());
							System.out.println(" TWEET MIT grosser Follower Zahl: " + mostFollowedUsersTweet);
							List<Status> discussionOnRootTweet = null;
							try {
								discussionOnRootTweet = getDiscussion(mostFollowedUsersTweet, twitter);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							discussionOnRootTweet.forEach(reply -> {

								Tweet replyTweet = Tweet.builder().tweetId(reply.getId()).title(reply.getText()).reply(rootTweet).build();
								tweetRepository.save(replyTweet);

								List<Status> discussionOnReply = null;
								try {
									discussionOnReply = getDiscussion(reply, twitter);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								discussionOnReply.forEach(discussionReply -> {

									tweetRepository.save(Tweet.builder().tweetId(discussionReply.getId()).title(discussionReply.getText().substring(discussionReply.getUser().getScreenName().length())).reply(replyTweet).build());
								});
							});
						});
					}

					if (mostFollowedUsersTweetT.getRetweetedStatus() != null) {
						System.out.println("RETQEET STATUS: " + mostFollowedUsersTweetT.getRetweetedStatus());


						//Query queryq1 = new Query(""+mostFollowedUsersTweetT.getRetweetedStatus().getId());
						//QueryResult resultq1 = twitter.search(queryq1);
						//System.out.println("TWEETS: " + resultq1.getTweets());


						String url = "https://twitter.com/" + mostFollowedUsersTweetT.getRetweetedStatus().getUser().getScreenName() + "/status/" + mostFollowedUsersTweetT.getRetweetedStatus().getId();
						System.out.println("Twitter URL:"+url);

						//System.out.println("URL OF TWEET" + url);
						//twitter.getUserTimeline(mostFollowedUsersTweetT.getRetweetedStatus().getUser().getScreenName());
						//System.out.println("URL TWEET : " + twitter.getUserTimeline(mostFollowedUsersTweetT.getRetweetedStatus().getUser().getScreenName()).stream().filter(a -> a.getId() == mostFollowedUsersTweetT.getRetweetedStatus().getId()).findFirst());


						Optional<Status> mostFollowedUsersTweetOpt = twitter.getUserTimeline(mostFollowedUsersTweetT.getRetweetedStatus().getUser().getScreenName()).stream().filter(a -> a.getId() == mostFollowedUsersTweetT.getRetweetedStatus().getId()).findFirst();

						mostFollowedUsersTweetOpt.ifPresent(mostFollowedUsersTweet -> {
							Tweet rootTweet = tweetRepository.save(Tweet.builder().tweetId(mostFollowedUsersTweet.getId()).title(mostFollowedUsersTweet.getText()).build());
							System.out.println(" TWEET MIT grosser Follower Zahl: " + mostFollowedUsersTweet);
							List<Status> discussionOnRootTweet = null;
							try {
								discussionOnRootTweet = getDiscussion(mostFollowedUsersTweet, twitter);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							discussionOnRootTweet.forEach(reply -> {
								tweetRepository.save(Tweet.builder().tweetId(reply.getId()).title(reply.getText().substring(reply.getUser().getScreenName().length())).reply(rootTweet).build());
							});
						});


					}
				} catch (TwitterException twitterExceeption) {
					if (twitterExceeption.getRateLimitStatus().getRemaining() == 0) {
						System.out.println("SLEEPING IN START DISCUSSION.....");
						Thread.sleep(900000);
						startDiscussion();
					}
				}



		//	}

		//}

		/*
		ResponseList<Status> homeTimeline = twitter.getHomeTimeline();
		Status rootStatus = homeTimeline.get(15);
		System.out.println("ROOT_TWEET: " + rootStatus);
		List<Status> status = getDiscussion(rootStatus, twitter);
		System.out.println("DISS: " + status);

		 */


		/*
		Tweet rootTweet = Tweet.builder().tweetId(rootStatus.getId()).title(rootStatus.getText()).build();
		tweetRepository.save(rootTweet);

		status.forEach(reply -> {
			Tweet tweet = Tweet.builder().tweetId(reply.getId()).title(reply.getText()).reply(rootTweet).build();
			tweetRepository.save(tweet);
		});

		 */

		/*
		Query query = new Query("1399057365287247872");
		QueryResult result = twitter.search(query);
		System.out.println("ID: " + result.getTweets().get(0).getId());
		System.out.println(tweetRepository.findTweetByTweetId(result.getTweets().get(0).getId()));
		Status rootStatus =result.getTweets().get(0);


		List<Status> status = getDiscussion(rootStatus.getQuotedStatus(), twitter);
		System.out.println("DISS; " + status);


		Tweet rootTweet = Tweet.builder().tweetId(rootStatus.getId()).title(rootStatus.getText()).build();
		tweetRepository.save(rootTweet);

		status.forEach(reply -> {
			Tweet tweet = Tweet.builder().tweetId(reply.getId()).title(reply.getText()).reply(rootTweet).build();
			tweetRepository.save(tweet);
		});

		 */


	}

	private Status getHighestInteractionTweet(List<Status> tweets) {
		int highestInteractionNumber = 0;
		Status highestInteractionNumberTweet = null;

		for (Status tweet: tweets) {
			int currentInteractionNumber = tweet.getFavoriteCount() + tweet.getRetweetCount();
			if (currentInteractionNumber > highestInteractionNumber) {
				highestInteractionNumber = currentInteractionNumber;
				highestInteractionNumberTweet = tweet;
			}
		}

		System.out.println("INTERACTION NUMBER: " + highestInteractionNumber);
		return highestInteractionNumberTweet;
	}

	private Status getHighestFollowerCountUsersTweet(List<Status> tweets) {
		int highestFollowerNumber = 0;
		Status highestFollowerNumberTweet = null;

		for (Status tweet: tweets) {
			if (tweet.getUser().getFollowersCount() > highestFollowerNumber) {
				highestFollowerNumber = tweet.getUser().getFollowersCount();
				highestFollowerNumberTweet = tweet;
			}
		}

		System.out.println("FOLLOWER NUMBER: " + highestFollowerNumber);
		return highestFollowerNumberTweet;
	}

	public ArrayList<Status> getDiscussion(Status status, Twitter twitter) throws InterruptedException {
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
		} catch (TwitterException twitterException) {
			if (twitterException.getRateLimitStatus().getRemaining() == 0) {
				System.out.println("SLEEPING.....");
				Thread.sleep(900000);
				getDiscussion(status, twitter);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		}
		return replies;
	}
}
