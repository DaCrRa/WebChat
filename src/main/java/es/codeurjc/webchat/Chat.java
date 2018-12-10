package es.codeurjc.webchat;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class Chat {

	private String name;

	private ConcurrentMap<String, UserCallbackHandler> users = new ConcurrentHashMap<String, UserCallbackHandler>();

	private ChatManager chatManager;

	public Chat(ChatManager chatManager, String name) {
		this.chatManager = chatManager;
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void addUser(User user) {

		this.users.put(user.getName(), new UserCallbackHandler(user));
		for (UserCallbackHandler handler : this.users.values()) {
			handler.newUserInChat(this, user);
		}
	}

	public void removeUser(User user) {
		this.users.remove(user.getName());
		for (UserCallbackHandler handler : this.users.values()) {
			handler.userExitedFromChat(this, user);
		}
	}

	public Collection<User> getUsers() {
		return Collections.unmodifiableCollection(users.values().stream().map(userHandler -> {
			return userHandler.getHandledUser();
		}).collect(Collectors.toList()));
	}

	public User getUser(String name) {
		return this.users.get(name).getHandledUser();
	}

	public void sendMessage(User user, String message) throws Throwable {

		for (UserCallbackHandler handler : this.users.values()) {
			handler.newMessage(this, user, message);
		}
	}

	public void close() {
		this.chatManager.closeChat(this);
	}
}
