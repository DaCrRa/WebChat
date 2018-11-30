package es.sidelab.webchat;

import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import es.codeurjc.webchat.Chat;
import es.codeurjc.webchat.ChatManager;

public class ChatManagerAndChatConcurrencyTest {

	private ChatManager manager = new ChatManager(50);

	@Test
	public void givenNUsersOperatingConcurrently_whenEachOneRegistersInMChats_thenNoExceptionsThrown() throws Throwable {
		int numberOfUsers = 10;
		int numberOfChats = 5;
		ExecutorService executor =
				Executors.newFixedThreadPool(10);

		CompletionService<Void> completionService =
				new ExecutorCompletionService<>(executor);

		for (int i = 0; i < numberOfUsers; i++) {
			int userId = i;
			completionService.submit(()->registerUserinMChats("user id " + userId, numberOfChats));
		}

		for (int i = 0; i < numberOfUsers; i++) {
			try {
				completionService.take().get();
			} catch (ExecutionException e) {
				throw e.getCause();
			}
		}
	}

	private Void registerUserinMChats(String userName, int m) throws InterruptedException, TimeoutException {
		TestUser user = new TestUser(userName) {
			@Override
			public void newChat(Chat chat) {
				System.out.println("User: " + userName + "  New chat " + chat.getName());
			}
		};
		manager.newUser(user);
		for (int i = 0; i < m; i++) {
			Chat chat = manager.newChat("chat" + i, 5, TimeUnit.SECONDS);
			chat.addUser(user);
		}
		return null;
	}
}
