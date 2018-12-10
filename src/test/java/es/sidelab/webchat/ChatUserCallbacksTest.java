package es.sidelab.webchat;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import es.codeurjc.webchat.Chat;
import es.codeurjc.webchat.ChatManager;
import es.codeurjc.webchat.User;

public class ChatUserCallbacksTest {

	ChatManager manager;
	Chat aChat;
	User actor;

	@Before
	public void createChatManagerChatAndActor() throws InterruptedException, TimeoutException {
		manager = new ChatManager(50);
		aChat = manager.newChat("new chat", 5, TimeUnit.SECONDS);
		actor = spy(new TestUser("actor"));
		manager.newUser(actor);
		aChat.addUser(actor);
	}

	private void failIfCountDownLatchDoesntGetToZeroWithin(long timeout, TimeUnit unit, CountDownLatch latch)
			throws InterruptedException {
		assertTrue("Timed out waiting for threads to do their job", latch.await(timeout, unit));
	}

	@Test
	public void givenChatManagerWithNUsersRegistered_whenNewChatCreated_thenAllUsersAreNotified()
			throws InterruptedException, TimeoutException {
		int numberOfUsers = 2;

		CountDownLatch countDownLatch = new CountDownLatch(numberOfUsers);

		// Given
		User[] spyUsers = new User[numberOfUsers];
		Arrays.parallelSetAll(spyUsers, i -> {
			return spy(new TestUser("User " + i) {
				@Override
				public void newChat(Chat chat) {
					countDownLatch.countDown();
					super.newChat(chat);
				}
			});
		});

		Stream.of(spyUsers).forEach(user -> manager.newUser(user));

		// When
		Chat createdChat = manager.newChat("chat created", 5, TimeUnit.SECONDS);

		// Then
		failIfCountDownLatchDoesntGetToZeroWithin(10, TimeUnit.SECONDS, countDownLatch);

		Stream.of(spyUsers).forEach(spy -> {
			verify(spy, times(1)).newChat(createdChat);
			verify(spy, times(1)).newChat(any());
		});
	}

	@Test
	public void givenChatManagerWithNUsersRegistered_whenOneUserHasConnectionIssue_thenAllUsersAreNotified()
			throws Throwable {
		int numberOfUsers = 4;

		CountDownLatch countDownLatch = new CountDownLatch(numberOfUsers);

		User[] spyUsers = new User[numberOfUsers];

		Arrays.parallelSetAll(spyUsers, i -> {
			return spy(new TestUser("User " + i) {
				@Override
				public void newMessage(Chat chat, User user, String message) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					try {
						super.newMessage(chat, user, message);
					} catch (Exception e) {
						e.printStackTrace();
					}
					countDownLatch.countDown();
				}
			});
		});

		Chat chat = new Chat(this.manager, "New chat");
		Stream.of(spyUsers).forEach(user -> {
			manager.newUser(user);
			chat.addUser(user);
		});

		chat.sendMessage(actor, "test");

		failIfCountDownLatchDoesntGetToZeroWithin(5, TimeUnit.SECONDS, countDownLatch);

		Stream.of(spyUsers).forEach(spy -> {
			try {
				verify(spy, times(1)).newMessage(chat, actor, "test");
				verify(spy, times(1)).newMessage(any(), any(), any());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	@Test
	public void givenChatManagerWithNUsersRegistered_whenOneUserHasConnectionIssue_thenAllUsersAreNotifiedInSortOrder()
			throws InterruptedException, TimeoutException {

		User producer = new TestUser("producer") {
			@Override
			public void newMessage(Chat chat, User user, String message) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				try {
					super.newMessage(chat, user, message);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		int numberOfMessages = 5;
		CountDownLatch countDownLatch = new CountDownLatch(numberOfMessages);

		User consumer = spy(new TestUser("consumer") {
			int messageId = 0;

			@Override
			public void newMessage(Chat chat, User user, String message) {
				if ((Integer.parseInt(message)) == messageId) {
					messageId++;
				} else {
					this.setIsSorted(false);
				}
				super.newMessage(chat, user, message);
				countDownLatch.countDown();
			}
		});

		Chat chat = new Chat(this.manager, "New chat");
		manager.newUser(producer);
		manager.newUser(consumer);
		chat.addUser(producer);
		chat.addUser(consumer);

		for (int i = 0; i < numberOfMessages; i++) {
			try {
				chat.sendMessage(producer, String.valueOf(i));
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

		failIfCountDownLatchDoesntGetToZeroWithin(10, TimeUnit.SECONDS, countDownLatch);

		assertTrue(((TestUser) consumer).getIsSorted());
		verify(consumer, times(numberOfMessages)).newMessage(eq(chat), eq(producer), any());
	}

	@Test
	public void givenChatManagerWithNUsersRegistered_whenNewChatCreatedWithSameNameAsPreexistent_thenUsersAreNotNotified()
			throws InterruptedException, TimeoutException {
		int numberOfUsers = 2;

		// Given
		User[] spyUsers = new User[numberOfUsers];
		Arrays.parallelSetAll(spyUsers, i -> {
			return spy(new TestUser("User " + i));
		});

		Stream.of(spyUsers).forEach(user -> manager.newUser(user));

		// When
		Chat createdChat = manager.newChat("new chat", 5, TimeUnit.SECONDS);

		// Then
		Stream.of(spyUsers).forEach(spy -> {
			verify(spy, never()).newChat(createdChat);
		});
	}

	@Test
	public void givenChatManagerWithNUsersRegisteredAndOneChat_whenChatRemoved_thenAllUsersAreNotified()
			throws InterruptedException, TimeoutException {
		int numberOfUsers = 2;

		CountDownLatch countDownLatch = new CountDownLatch(numberOfUsers);

		// Given
		User[] spyUsers = new User[numberOfUsers];
		Arrays.parallelSetAll(spyUsers, i -> {
			return spy(new TestUser("User " + i) {
				@Override
				public void chatClosed(Chat chat) {
					countDownLatch.countDown();
					super.chatClosed(chat);
				}
			});
		});

		Stream.of(spyUsers).forEach(user -> manager.newUser(user));

		// When
		manager.closeChat(aChat);

		// Then
		failIfCountDownLatchDoesntGetToZeroWithin(10, TimeUnit.SECONDS, countDownLatch);

		Stream.of(spyUsers).forEach(spy -> {
			verify(spy, times(1)).chatClosed(aChat);
			verify(spy, times(1)).chatClosed(any());
		});
	}

	@Test
	public void givenChatWithNUsersRegistered_whenNewUserInChat_thenAllUsersAreNotified()
			throws InterruptedException, TimeoutException {
		int numberOfUsers = 2;

		CountDownLatch countDownLatch = new CountDownLatch(numberOfUsers);

		// Given
		User[] spyUsers = new User[numberOfUsers];
		Arrays.parallelSetAll(spyUsers, i -> {
			return spy(new TestUser("User " + i) {
				@Override
				public void newUserInChat(Chat chat, User user) {
					countDownLatch.countDown();
					super.newUserInChat(chat, user);
				}
			});
		});

		Stream.of(spyUsers).forEach(user -> {
			manager.newUser(user);
			aChat.addUser(user);
		});

		// When
		User newUser = spy(new TestUser("the new user"));
		manager.newUser(newUser);
		aChat.addUser(newUser);

		// Then
		failIfCountDownLatchDoesntGetToZeroWithin(10, TimeUnit.SECONDS, countDownLatch);

		Stream.of(spyUsers).forEach(spy -> {
			verify(spy, times(1)).newUserInChat(aChat, newUser);
		});

		verify(newUser, never()).newUserInChat(any(), any());
	}

	@Test
	public void givenChatWithNUsersRegistered_whenUserLeavesChat_thenAllUsersAreNotified()
			throws InterruptedException, TimeoutException {
		int numberOfUsers = 2;

		CountDownLatch countDownLatch = new CountDownLatch(numberOfUsers);

		// Given
		User[] spyUsers = new User[numberOfUsers];
		Arrays.parallelSetAll(spyUsers, i -> {
			return spy(new TestUser("User " + i) {
				@Override
				public void userExitedFromChat(Chat chat, User user) {
					countDownLatch.countDown();
					super.userExitedFromChat(chat, user);
				}
			});
		});

		Stream.of(spyUsers).forEach(user -> {
			manager.newUser(user);
			aChat.addUser(user);
		});

		// When
		aChat.removeUser(actor);

		// Then
		failIfCountDownLatchDoesntGetToZeroWithin(10, TimeUnit.SECONDS, countDownLatch);

		Stream.of(spyUsers).forEach(spy -> {
			verify(spy, times(1)).userExitedFromChat(aChat, actor);
		});

		verify(actor, never()).userExitedFromChat(any(), any());
	}

	@Test
	public void givenChatWithNUsersRegistered_whenMessageOnTheChat_thenAllUsersGetMessage()
			throws InterruptedException, TimeoutException {
		int numberOfUsers = 2;

		CountDownLatch countDownLatch = new CountDownLatch(numberOfUsers);

		// Given
		User[] spyUsers = new User[numberOfUsers];
		Arrays.parallelSetAll(spyUsers, i -> {
			return spy(new TestUser("User " + i) {
				@Override
				public void newMessage(Chat chat, User user, String message) {
					countDownLatch.countDown();
					super.newMessage(chat, user, message);
				}
			});
		});

		Stream.of(spyUsers).forEach(user -> {
			manager.newUser(user);
			aChat.addUser(user);
		});

		// When
		try {
			aChat.sendMessage(actor, "message sent by the actor");
		} catch (Throwable e) {
			e.printStackTrace();
		}

		// Then
		failIfCountDownLatchDoesntGetToZeroWithin(10, TimeUnit.SECONDS, countDownLatch);

		Stream.of(spyUsers).forEach(spy -> {
			verify(spy, times(1)).newMessage(aChat, actor, "message sent by the actor");
			verify(spy, times(1)).newMessage(any(), any(), any());
		});
	}

	@Test
	public void givenTwoChatsWithUsersRegisteredOnBoth_whenMessagesOnEachChat_thenTheyGetMessages()
			throws InterruptedException, TimeoutException {
		int numberOfUsers = 2;

		CountDownLatch countDownLatch = new CountDownLatch(numberOfUsers * 2);

		// Given
		User[] spyUsers = new User[numberOfUsers];
		Arrays.parallelSetAll(spyUsers, i -> {
			return spy(new TestUser("User " + i) {
				@Override
				public void newMessage(Chat chat, User user, String message) {
					countDownLatch.countDown();
					super.newMessage(chat, user, message);
				}
			});
		});

		Stream.of(spyUsers).forEach(user -> {
			manager.newUser(user);
			aChat.addUser(user);
		});

		Chat otherChat = manager.newChat("new chat", 5, TimeUnit.SECONDS);
		Stream.of(spyUsers).forEach(user -> otherChat.addUser(user));
		otherChat.addUser(actor);

		// When
		try {
			aChat.sendMessage(actor, "message sent to a chat");
		} catch (Throwable e) {
			e.printStackTrace();
		}

		try {
			otherChat.sendMessage(actor, "message sent to the other chat");
		} catch (Throwable e) {
			e.printStackTrace();
		}

		// Then
		failIfCountDownLatchDoesntGetToZeroWithin(10, TimeUnit.SECONDS, countDownLatch);

		Stream.of(spyUsers).forEach(spy -> {
			verify(spy, times(1)).newMessage(aChat, actor, "message sent to a chat");
			verify(spy, times(1)).newMessage(otherChat, actor, "message sent to the other chat");
		});
	}

	@Test
	public void givenChatWithUsersRegistered_whenMessageSentWhileUserLeaves_thenNoExceptionThrown() throws Throwable {
		int numberOfUsers = 4;

		CountDownLatch countDownLatch = new CountDownLatch(numberOfUsers);

		// Given
		User[] spyUsers = new User[numberOfUsers];
		Arrays.parallelSetAll(spyUsers, i -> {
			return spy(new TestUser("User " + i) {
				@Override
				public void newMessage(Chat chat, User user, String message) {
					try {
						Thread.sleep(1 * 1000 /* millis per second */);
					} catch (InterruptedException e) {
					}
					super.newMessage(chat, user, message);
					countDownLatch.countDown();
				}
			});
		});

		Stream.of(spyUsers).forEach(user -> {
			manager.newUser(user);
			aChat.addUser(user);
		});

		// When
		aChat.sendMessage(actor, "message sent to a chat");
		aChat.removeUser(actor);

		// Then
		failIfCountDownLatchDoesntGetToZeroWithin(10, TimeUnit.SECONDS, countDownLatch);

		Stream.of(spyUsers).forEach(spy -> {
			verify(spy, times(1)).newMessage(aChat, actor, "message sent to a chat");
		});
	}

}
