package es.sidelab.webchat;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import es.codeurjc.webchat.Chat;
import es.codeurjc.webchat.ChatManager;
import es.codeurjc.webchat.User;

public class ChatManagerAndChatConcurrencyTest {

	private ChatManager manager;
	private User spyUser;

	@Before
	public void createChatManagerWithASpyUser() {
		manager = new ChatManager(50);
		spyUser = mock(User.class);
		when(spyUser.getName()).thenReturn("Spy");
		manager.newUser(spyUser);
	}

	@Test
	public void givenOneChatRegisteredInManager_whenRemovingItConcurrently_thenNoExceptionsThrown() throws Throwable {
		int numberOfConcurrentRemovals = 50;

		String chatName = "chat to remove";
		manager.newChat(chatName, 5, TimeUnit.SECONDS);

		ExecutorService executor =
				Executors.newFixedThreadPool(10);

		CompletionService<Void> completionService =
				new ExecutorCompletionService<>(executor);

		for (int i = 0; i < numberOfConcurrentRemovals; i++) {
			completionService.submit(()-> {
				manager.closeChat(new Chat(manager, chatName));
				return null;
			});
		}
		for (int i = 0; i < numberOfConcurrentRemovals; i++) {
			try {
				completionService.take().get();
			} catch (ExecutionException e) {
				throw e.getCause();
			}
		}
		verify(spyUser, times(1)).chatClosed(any());
	}

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

		verify(spyUser, times(numberOfChats)).newChat(any());
	}

	private Void registerUserinMChats(String userName, int m) throws InterruptedException, TimeoutException {
		TestUser user = new TestUser(userName);
		manager.newUser(user);
		for (int i = 0; i < m; i++) {
			Chat chat = manager.newChat("chat " + i, 5, TimeUnit.SECONDS);
			chat.addUser(user);
		}
		return null;
	}
}
