package es.sidelab.webchat;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

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

public class ChatOperationsTest {

	ChatManager manager;

	@Before
	public void createChatManager() {
		manager = new ChatManager(50);
	}

	@Test
	public void givenChatManagerWithNUsersRegistered_whenNewChatCreated_thenAllUsersAreNotified() throws InterruptedException, TimeoutException {
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
		Chat aChat = manager.newChat("new chat", 5, TimeUnit.SECONDS);

		countDownLatch.await(10, TimeUnit.SECONDS);

		// Then
		Stream.of(spyUsers).forEach(spy -> {
			verify(spy, times(1)).newChat(aChat);
			verify(spy, times(1)).newChat(any());
		});
	}

	@Test
	public void givenChatManagerWithNUsersRegisteredAndOneChat_whenChatRemoved_thenAllUsersAreNotified() throws InterruptedException, TimeoutException {
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
		Chat aChat = manager.newChat("new chat", 5, TimeUnit.SECONDS);

		// When
		manager.closeChat(aChat);

		countDownLatch.await(10, TimeUnit.SECONDS);

		// Then
		Stream.of(spyUsers).forEach(spy -> {
			verify(spy, times(1)).chatClosed(aChat);
			verify(spy, times(1)).chatClosed(any());
		});
	}

	@Test
	public void givenChatWithNUsersRegistered_whenNewUserInChat_thenAllUsersAreNotified() throws InterruptedException, TimeoutException {
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

		Chat aChat = manager.newChat("new chat", 5, TimeUnit.SECONDS);
		Stream.of(spyUsers).forEach(user -> aChat.addUser(user));

		// When
		User newUser = spy(new TestUser("the new user"));
		aChat.addUser(newUser);

		countDownLatch.await(10, TimeUnit.SECONDS);

		// Then
		Stream.of(spyUsers).forEach(spy -> {
			verify(spy, times(1)).newUserInChat(aChat, newUser);
		});

		verify(newUser, never()).newUserInChat(any(), any());
	}

	@Test
	public void givenChatWithNUsersRegistered_whenUserLeavesChat_thenAllUsersAreNotified() throws InterruptedException, TimeoutException {
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

		Chat aChat = manager.newChat("new chat", 5, TimeUnit.SECONDS);
		Stream.of(spyUsers).forEach(user -> aChat.addUser(user));

		User newUser = spy(new TestUser("the new user"));
		aChat.addUser(newUser);

		// When
		aChat.removeUser(newUser);

		countDownLatch.await(10, TimeUnit.SECONDS);

		// Then
		Stream.of(spyUsers).forEach(spy -> {
			verify(spy, times(1)).userExitedFromChat(aChat, newUser);
		});

		verify(newUser, never()).userExitedFromChat(any(), any());
	}

	@Test
	public void givenChatWithNUsersRegistered_whenMessageOnTheChat_thenAllUsersGetMessage() throws InterruptedException, TimeoutException {
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

		Chat aChat = manager.newChat("new chat", 5, TimeUnit.SECONDS);
		Stream.of(spyUsers).forEach(user -> aChat.addUser(user));

		User newUser = spy(new TestUser("the new user"));
		aChat.addUser(newUser);

		// When
		aChat.sendMessage(newUser, "message sent by newUser");

		countDownLatch.await(10, TimeUnit.SECONDS);

		// Then
		Stream.of(spyUsers).forEach(spy -> {
			verify(spy, times(1)).newMessage(aChat, newUser, "message sent by newUser");
		});
	}

	@Test
	public void givenTwoChatsWithUsersRegisteredOnBoth_whenMessagesOnEachChat_thenTheyGetMessages() throws InterruptedException, TimeoutException {
		int numberOfUsers= 2;

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

		Chat aChat = manager.newChat("new chat", 5, TimeUnit.SECONDS);
		Stream.of(spyUsers).forEach(user -> aChat.addUser(user));

		Chat otherChat = manager.newChat("new chat", 5, TimeUnit.SECONDS);
		Stream.of(spyUsers).forEach(user -> otherChat.addUser(user));

		User newUser = spy(new TestUser("the new user"));
		aChat.addUser(newUser);
		otherChat.addUser(newUser);

		// When
		aChat.sendMessage(newUser, "message sent to a chat");
		otherChat.sendMessage(newUser, "message sent to the other chat");

		countDownLatch.await(10, TimeUnit.SECONDS);

		// Then
		Stream.of(spyUsers).forEach(spy -> {
			verify(spy, times(1)).newMessage(aChat, newUser, "message sent to a chat");
			verify(spy, times(1)).newMessage(otherChat, newUser, "message sent to the other chat");
		});
	}

}
