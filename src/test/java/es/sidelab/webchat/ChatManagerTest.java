package es.sidelab.webchat;

import static org.junit.Assert.assertTrue;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import es.codeurjc.webchat.Chat;
import es.codeurjc.webchat.ChatManager;
import es.codeurjc.webchat.User;

public class ChatManagerTest {

	private void failIfCountDownLatchDoesntGetToZeroWithin(long timeout, TimeUnit unit, CountDownLatch latch)
			throws InterruptedException {
		assertTrue("Timed out waiting for threads to do their job", latch.await(timeout, unit));
	}

	@Test
	public void newChat() throws InterruptedException, TimeoutException {

		// Crear el chat Manager
		ChatManager chatManager = new ChatManager(5);

		// Crear un usuario que guarda en chatName el nombre del nuevo chat
		CountDownLatch countdownLatch = new CountDownLatch(1);
		final String[] chatName = new String[1];

		chatManager.newUser(new TestUser("user") {
			@Override
			public void newChat(Chat chat) {
				chatName[0] = chat.getName();
				countdownLatch.countDown();
			}
		});

		// Crear un nuevo chat en el chatManager
		chatManager.newChat("Chat", 5, TimeUnit.SECONDS);

		failIfCountDownLatchDoesntGetToZeroWithin(1, TimeUnit.SECONDS, countdownLatch);

		// Comprobar que el chat recibido en el método 'newChat' se llama 'Chat'
		assertTrue("The method 'newChat' should be invoked with 'Chat', but the value is " + chatName[0],
				Objects.equals(chatName[0], "Chat"));
	}

	@Test
	public void newUserInChat() throws InterruptedException, TimeoutException {

		ChatManager chatManager = new ChatManager(5);

		CountDownLatch countdownLatch = new CountDownLatch(1);
		final String[] newUser = new String[1];

		TestUser user1 = new TestUser("user1") {
			@Override
			public void newUserInChat(Chat chat, User user) {
				newUser[0] = user.getName();
				countdownLatch.countDown();
			}
		};

		TestUser user2 = new TestUser("user2");

		chatManager.newUser(user1);
		chatManager.newUser(user2);

		Chat chat = chatManager.newChat("Chat", 5, TimeUnit.SECONDS);

		chat.addUser(user1);
		chat.addUser(user2);

		failIfCountDownLatchDoesntGetToZeroWithin(1, TimeUnit.SECONDS, countdownLatch);

		assertTrue("Notified new user '" + newUser[0] + "' is not equal than user name 'user2'",
				"user2".equals(newUser[0]));

	}
}
