package com.tweetConversations.tweetConversations;

import com.tweetConversations.tweetConversations.model.Tweet;
import com.tweetConversations.tweetConversations.repo.TrendRepository;
import com.tweetConversations.tweetConversations.repo.TweetRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Trend;
import twitter4j.Trends;
import twitter4j.Twitter;
import twitter4j.TwitterException;

@Service
public class TweetService {

	private static final Integer GERMANY_LOCATION_WOEID = 23424829;
	public static final long HOUR = 3600*1000; // in milli-seconds.

	@Autowired
	private Twitter twitter;

	@Autowired
	private TweetRepository tweetRepository;

	@Autowired
	private TrendRepository trendRepository;

	public void startDiscussion() throws TwitterException, InterruptedException {


		Trends trends = twitter.getPlaceTrends(GERMANY_LOCATION_WOEID);

		try {

			Trend trendQuery = getTrendQuery(trends);

			Status mostFollowedUsersTweetT = getMostInteractedTweetFromTrend(trendQuery);

			if (tweetRepository.findTweetsByTweetId(mostFollowedUsersTweetT.getId()).isEmpty()) {
				System.out.println("Zwischen status: " + mostFollowedUsersTweetT);

				Tweet baseTweet = null;

				Tweet quatedTweet = getQuoted(mostFollowedUsersTweetT, trendQuery);
				if (quatedTweet != null) {
					baseTweet = quatedTweet;
				}

				Tweet retweetedTweet = getRetweeted(mostFollowedUsersTweetT, trendQuery);
				if (retweetedTweet != null) {
					baseTweet = retweetedTweet;
				}

				Tweet otherTweet = getOtherwise(mostFollowedUsersTweetT, trendQuery);
				if (otherTweet != null) {
					baseTweet = otherTweet;
				}


				if (baseTweet != null) {
					calcConversationDepth(baseTweet);

					calcConversationSize(baseTweet);
				}
			}


		} catch (TwitterException twitterExceeption) {
			if (twitterExceeption.getRateLimitStatus() != null && twitterExceeption.getRateLimitStatus().getRemaining() == 0) {
				System.out.println("SLEEPING IN START DISCUSSION.....");
				Thread.sleep(900000);
			}
		}

		extendConversation();

	}

	private Trend getTrendQuery(Trends trends) {
		//random fillen
		int min = 0;
		int max = 10;

		Random random = new Random();

		int value = random.nextInt(max + min) + min;
		Trend trendQuery = trends.getTrends()[value];
		String trendTitle = trendQuery.getName();

		Optional<com.tweetConversations.tweetConversations.model.Trend> trendInDB = trendRepository.findByTitle(trendTitle);
		if (trendInDB.isEmpty()) {
			trendRepository.save(com.tweetConversations.tweetConversations.model.Trend.builder().title(trendTitle).build());
		}
		return trendQuery;
	}

	private void calcConversationSize(Tweet baseTweet) {
		AtomicInteger sum = new AtomicInteger();
		calcNodesSum(baseTweet, sum);
		if (sum.get() != 0) {
			//Tweet tweet = tweetRepository.findByTweetId(baseTweet.getTweetId());
			baseTweet.setNodes(sum.get());
			tweetRepository.save(baseTweet);
		}
	}

	private void calcConversationDepth(Tweet baseTweet) {
		//System.out.println("Thats the Tweet to count: " + mostFollowedUsersTweetT.getId());
		ArrayList<Integer> depths = new ArrayList<>();
		ArrayList<Integer> depthsOfDepths = new ArrayList<>();

		if (baseTweet != null) {
			if (baseTweet.getTweetId() != 0) {
				//Tweet tweet = tweetRepository.findByTweetId(baseTweet.getTweetId());
				List<Tweet> replies = tweetRepository.findAllByReplyId(baseTweet.getId());
				replies.forEach(reply -> {
					List<Integer> newDeeps = countDepth(reply, 1, depths);
					int highestDepthOfReplies = Collections.max(newDeeps);
					depthsOfDepths.add(highestDepthOfReplies);
				});

				if (!CollectionUtils.isEmpty(depthsOfDepths)) {
					//System.out.println("All Depth: " + depthsOfDepths);
					int highest = Collections.max(depthsOfDepths);
					baseTweet.setDepth(highest);
					//tweet.setAllDepths(depthsOfDepths);
					if (replies.size() > depthsOfDepths.size()) {
						baseTweet.setJustDirectReply(replies.size() - depthsOfDepths.size());
					}
					tweetRepository.save(baseTweet);
				}

			}
		}
	}

	private void calcNodesSum(Tweet baseTweet, AtomicInteger sum) {
		//System.out.println("Thats the Tweet to count: " + mostFollowedUsersTweetT.getId());
		//AtomicInteger sum = new AtomicInteger();
		//sum.set(newValue);

		if (baseTweet != null) {
			if (baseTweet.getTweetId() != 0) {
				//Tweet tweet = tweetRepository.findByTweetId(baseTweet.getTweetId());
				System.out.println("Thats the Tweet tweet: " + baseTweet);
				List<Tweet> replies = tweetRepository.findAllByReplyId(baseTweet.getId());
				replies.forEach(reply -> {

					///int nodeSum = recursiveNodeSum(reply, sum.get());
					calcNodesSum(reply, sum);
				});
				if(replies.size() > 0) {
					sum.addAndGet(replies.size());
				}
			}
		}
	}

	private Tweet getOtherwise(Status mostFollowedUsersTweetT, Trend trend) {
		AtomicReference<Tweet> tweet = new AtomicReference<>();
		if (mostFollowedUsersTweetT.getRetweetedStatus() == null && mostFollowedUsersTweetT.getQuotedStatus() == null) {

			if (tweetRepository.findTweetsByTweetId(mostFollowedUsersTweetT.getId()).isEmpty()) {
				Tweet rootTweet = tweetRepository.save(Tweet.builder().tweetId(mostFollowedUsersTweetT.getId()).title(mostFollowedUsersTweetT.getText()).rootTweet(true).createdAt(mostFollowedUsersTweetT.getCreatedAt()).trend(trendRepository.findByTitle(trend.getName()).get()).build());
				tweet.set(rootTweet);
				System.out.println(" TWEET MIT grosser Follower Zahl 2 : " + mostFollowedUsersTweetT);
				System.out.println("ROOT TWEET TIME 2 : " + mostFollowedUsersTweetT.getCreatedAt());
				long diff = new Date().getTime() - mostFollowedUsersTweetT.getCreatedAt().getTime();
				long diffHours = diff / (60 * 60 * 1000);
				System.out.println("DIFF: " + diffHours);
				List<Status> discussionOnRootTweet = null;
				tweetsDiscussion(mostFollowedUsersTweetT, rootTweet, discussionOnRootTweet);
			}

			return tweet.get();
		}
		return null;
	}

	private Tweet getRetweeted(Status mostFollowedUsersTweetT, Trend trend) throws TwitterException {
		AtomicReference<Tweet> tweet = new AtomicReference<>();
		if (mostFollowedUsersTweetT.getRetweetedStatus() != null) {

			Optional<Status> mostFollowedUsersTweetOpt = twitter.getUserTimeline(mostFollowedUsersTweetT.getRetweetedStatus().getUser().getScreenName()).stream().filter(a -> a.getId() == mostFollowedUsersTweetT.getRetweetedStatus().getId()).findFirst();

			mostFollowedUsersTweetOpt.ifPresent(mostFollowedUsersTweet -> {

				if (tweetRepository.findTweetsByTweetId(mostFollowedUsersTweet.getId()).isEmpty()) {
					Tweet rootTweet = tweetRepository.save(Tweet.builder().tweetId(mostFollowedUsersTweet.getId()).title(mostFollowedUsersTweet.getText()).rootTweet(true).createdAt(mostFollowedUsersTweet.getCreatedAt()).trend(trendRepository.findByTitle(trend.getName()).get()).build());
					tweet.set(rootTweet);
					List<Status> discussionOnRootTweet = null;
					tweetsDiscussion(mostFollowedUsersTweet, rootTweet, discussionOnRootTweet);
				}

			});
			return tweet.get();
		}
		return null;
	}

	private Tweet getQuoted(Status mostFollowedUsersTweetT, Trend trend) throws TwitterException {
		AtomicReference<Tweet> tweet = new AtomicReference<>();
		if (mostFollowedUsersTweetT.getQuotedStatus() != null) {
			System.out.println("QUATED STATUS: " + mostFollowedUsersTweetT.getQuotedStatus());

			Optional<Status> mostFollowedUsersTweetOpt = twitter.getUserTimeline(mostFollowedUsersTweetT.getQuotedStatus().getUser().getScreenName()).stream().filter(a -> a.getId() == mostFollowedUsersTweetT.getQuotedStatus().getId()).findFirst();

			mostFollowedUsersTweetOpt.ifPresent(mostFollowedUsersTweet -> {

				if (tweetRepository.findTweetsByTweetId(mostFollowedUsersTweet.getId()).isEmpty()) {

					Tweet rootTweet = tweetRepository.save(Tweet.builder().tweetId(mostFollowedUsersTweet.getId()).title(mostFollowedUsersTweet.getText()).rootTweet(true).createdAt(mostFollowedUsersTweet.getCreatedAt()).trend(trendRepository.findByTitle(trend.getName()).get()).build());
					tweet.set(rootTweet);
					System.out.println(" TWEET MIT grosser Follower Zahl: " + mostFollowedUsersTweet);
					System.out.println("ROOT TWEET TIME: " + mostFollowedUsersTweet.getCreatedAt());
					List<Status> discussionOnRootTweet = null;
					tweetsDiscussion(mostFollowedUsersTweet, rootTweet, discussionOnRootTweet);
				}
			});

			return tweet.get();

		}
		return null;
	}

	private List<Integer> countDepth(Tweet tweet, int deep, List<Integer> depths) {

		depths.add(deep);

		AtomicInteger depth = new AtomicInteger(deep);

		List<Tweet> replies = tweetRepository.findAllByReplyId(tweet.getId());
		if (replies.size() > 0) {
			depth.incrementAndGet();
		}
		replies.forEach(reply -> {
			countDepth(reply, depth.get(), depths);
		});

		return depths;
	}

	private void tweetsDiscussion(Status mostFollowedUsersTweet, Tweet rootTweet, List<Status> discussionOnRootTweet) {
		System.out.println("TWEETDISCUSSION");
		try {
			discussionOnRootTweet = getDiscussion(mostFollowedUsersTweet);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		List<Status> finalDiscussionOnRootTweet = discussionOnRootTweet;
		discussionOnRootTweet.forEach(reply -> {

			Tweet replyTweet = Tweet.builder().tweetId(reply.getId()).title(reply.getText()).reply(rootTweet).rootTweet(false).createdAt(reply.getCreatedAt()).build();
			tweetRepository.save(replyTweet);
			System.out.println("TWEETDISCUSSION for loop");
			tweetsDiscussion(reply, replyTweet, finalDiscussionOnRootTweet);
		});
	}

	private Status getMostInteractedTweetFromTrend(Trend trendQuery) throws TwitterException {

		System.out.println("Trend: " + trendQuery.getName());
		Query query = new Query(trendQuery.getQuery());
		query.setCount(100);
		QueryResult result = twitter.search(query);
		System.out.println("RESULT: " + result);

		Status interactionTweet = getHighestInteractionTweet(result.getTweets());
		long mostFollowedUsersTweetId;
		if (interactionTweet.getRetweetedStatus() != null) {
			mostFollowedUsersTweetId = getHighestInteractionTweet(result.getTweets()).getRetweetedStatus().getId();
		} else {
			mostFollowedUsersTweetId = getHighestInteractionTweet(result.getTweets()).getId();
		}

		Query queryq = new Query(""+mostFollowedUsersTweetId);
		QueryResult resultq = twitter.search(queryq);
		if (resultq.getTweets().isEmpty()) {
			throw new TwitterException("No Tweets found");
		}

		return resultq.getTweets().get(0);
	}

	private Status getHighestInteractionTweet(List<Status> tweets) {
		int highestInteractionNumber = 0;
		Status highestInteractionNumberTweet = null;

		for (Status tweet: tweets) {
			List<Tweet> foundTweet = tweetRepository.findTweetsByTweetId(tweet.getId());

			if (foundTweet.isEmpty()) {
				int currentInteractionNumber = tweet.getFavoriteCount() + tweet.getRetweetCount();
				if (currentInteractionNumber > highestInteractionNumber) {
					highestInteractionNumber = currentInteractionNumber;
					highestInteractionNumberTweet = tweet;
				}
			}
		}

		System.out.println("INTERACTION NUMBER: " + highestInteractionNumber);
		return highestInteractionNumberTweet;
	}

	public ArrayList<Status> getDiscussion(Status status) throws InterruptedException {
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
				System.out.println("hier?");

				if (query != null)
					result = twitter.search(query);

			} while (query != null);
			System.out.println("hiuer");
			return replies;
		} catch (TwitterException twitterException) {
			if (twitterException.getRateLimitStatus().getRemaining() == 0) {
				System.out.println("SLEEPING.....");
				Thread.sleep(900000);
				return replies;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		}
		return replies;
	}

	public void extendConversation() {

		//alle RootTweets aus der DB holen
		List<Tweet> rootTweets = tweetRepository.findAllByRootTweetIsTrue();

		rootTweets.forEach(rootTweet -> {
			long diff = new Date().getTime() - rootTweet.getCreatedAt().getTime();
			long diffHours = diff / (60 * 60 * 1000);
			System.out.println("DIFF ON ROOT TWEETS: " + diffHours);

			if (diffHours <= 12) {
				try {
					System.out.println("TWEET ERWEITERN: " + rootTweet.getTweetId());
					Status status = twitter.showStatus(rootTweet.getTweetId());
					List<Status> dissOnRootTweet = getDiscussion(status);
					dissOnRootTweet.forEach(reply -> {
						List<Tweet> givenTweet = tweetRepository.findTweetsByTweetId(reply.getId());

						if (givenTweet.isEmpty()) {

							System.out.println("TWEET ERWEITERN UM: " + reply.getId());

							tweetRepository.save(Tweet.builder().tweetId(reply.getId()).createdAt(reply.getCreatedAt())
														 .title(reply.getText()).reply(rootTweet).build());
						}
					});
					getReplyTreeOfReplies(rootTweet);
				} catch (TwitterException twitterException ) {
					twitterException.printStackTrace();
					if (twitterException.getRateLimitStatus().getRemaining() == 0) {
						System.out.println("SLEEPING.....");
						try {
							Thread.sleep(900000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} catch (InterruptedException interruptedException ) {

				}
			}

			calcConversationDepth(rootTweet);
			calcConversationSize(rootTweet);

		});

		try {
			startDiscussion();
		} catch (TwitterException twitterException) {
			twitterException.printStackTrace();
		} catch (InterruptedException interruptedException) {
			interruptedException.printStackTrace();
		}
	}

	private void getReplyTreeOfReplies(Tweet rootTweet) {

		List<Tweet> replies = tweetRepository.findAllByReplyId(rootTweet.getId());
		replies.forEach(pReplies -> {
			try {
				Status statusOfReply = twitter.showStatus(pReplies.getTweetId());
				List<Status> dissOnReplyTweet = getDiscussion(statusOfReply);
				dissOnReplyTweet.forEach(replyOnReply -> {
					List<Tweet> givenTweet = tweetRepository.findTweetsByTweetId(replyOnReply.getId());

					if (givenTweet.isEmpty()) {
						tweetRepository.save(Tweet.builder().tweetId(replyOnReply.getId()).createdAt(replyOnReply.getCreatedAt())
													 .title(replyOnReply.getText()).reply(pReplies).build());
						System.out.println("REPLY ERWEITERN UM: " + replyOnReply.getId());
					}
				});

			} catch (TwitterException twitterException) {
				if (twitterException.getRateLimitStatus().getRemaining() == 0) {
					System.out.println("SLEEPING.....");
					try {
						Thread.sleep(900000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} catch (InterruptedException interruptedException) {
			}


			getReplyTreeOfReplies(pReplies);
		});
	}
}
