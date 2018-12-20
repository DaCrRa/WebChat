package es.sidelab.webchat;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import es.codeurjc.webchat.Chat;
import es.codeurjc.webchat.ChatManager;

public class CreateChatTimeoutTest {

	@Test(expected = TimeoutException.class)
	public void givenManagerWithMaxChatsRegistered_whenNewChatAndNoChatsDeleted_thenTimeoutException()
			throws Throwable {

		// Given
		ChatManager manager = new ChatManager(1);
		manager.newChat("pre-registered chat", 1, TimeUnit.SECONDS);

		// When
		manager.newChat("new chat to register", 1, TimeUnit.SECONDS);

		// Then
		// Exception thrown
	}

	@Test
	public void givenManagerWithMaxChatsRegistered_whenNewChatAndChatDeletedWithinTime_thenChatRegistered()
			throws Throwable {

		ExecutorService executor = Executors.newSingleThreadExecutor();

		// Given
		ChatManager manager = new ChatManager(1);
		Chat preRegisteredChat = manager.newChat("pre-registered chat", 1, TimeUnit.SECONDS);

		// When
		Future<Chat> futureChat = executor.submit(() -> manager.newChat("new chat to register", 5, TimeUnit.SECONDS));

		Thread.sleep(1 * 1000 /* millis per second */);
		manager.closeChat(preRegisteredChat);

		// Then
		Chat newRegisteredChat = futureChat.get(2, TimeUnit.SECONDS);

		assertThat(newRegisteredChat.getName(), is(equalTo("new chat to register")));

	}
}
