package es.codeurjc.webchat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserCallbackHandler {

	private User handledUser;

	private ExecutorService executor;

	public UserCallbackHandler(User user) {
		this.handledUser = user;
		executor = Executors.newSingleThreadExecutor();
	}

	public void newUserInChat(Chat chat, User user) {
		if (user != handledUser) {
			executor.submit(() -> handledUser.newUserInChat(chat, user));
		}
	}

	public void userExitedFromChat(Chat chat, User user) {
		if (user != handledUser) {
			executor.submit(() -> handledUser.userExitedFromChat(chat, user));
		}
	}

	public User getHandledUser() {
		return handledUser;
	}

	public void newMessage(Chat chat, User user, String message) {
		executor.submit(() -> handledUser.newMessage(chat, user, message));
	}

	public void newChat(Chat newChat) {
		executor.submit(() -> handledUser.newChat(newChat));
	}

	public void chatClosed(Chat removedChat) {
		executor.submit(() -> handledUser.chatClosed(removedChat));
	}

}
