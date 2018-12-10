package es.codeurjc.webchat;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class ChatManager {

	private ConcurrentMap<String, Chat> chats = new ConcurrentHashMap<>();
	private ConcurrentMap<String, UserCallbackHandler> users = new ConcurrentHashMap<>();
	private int maxChats;

	public ChatManager(int maxChats) {
		this.maxChats = maxChats;
	}

	public void newUser(User user) {

		users.compute(user.getName(), (name, mappedHandler) -> {
			if (mappedHandler != null) {
				throw new IllegalArgumentException("There is already a user with name \'" + user.getName() + "\'");
			}
			return new UserCallbackHandler(user);
		});
	}

	public Chat newChat(String name, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {

		if (chats.size() == maxChats) {
			throw new TimeoutException("There is no enought capacity to create a new chat");
		}

		return chats.computeIfAbsent(name, n -> {
			Chat newChat = new Chat(this, name);
			for (UserCallbackHandler handler : users.values()) {
				handler.newChat(newChat);
			}
			return newChat;
		});
	}

	public void closeChat(Chat chat) {
		Chat removedChat = chats.remove(chat.getName());
		if (removedChat != null) {
			for (UserCallbackHandler handler : users.values()) {
				handler.chatClosed(removedChat);
			}
		}
	}

	public Collection<Chat> getChats() {
		return Collections.unmodifiableCollection(chats.values());
	}

	public Chat getChat(String chatName) {
		return chats.get(chatName);
	}

	public Collection<User> getUsers() {
		return Collections.unmodifiableCollection(users.values().stream().map(userHandler -> {
			return userHandler.getHandledUser();
		}).collect(Collectors.toList()));
	}

	public User getUser(String userName) {
		return users.get(userName).getHandledUser();
	}

	public void close() {
	}

	public UserCallbackHandler getUserCallbackHandlerForUser(User user) {
		return users.get(user.getName());
	}
}
