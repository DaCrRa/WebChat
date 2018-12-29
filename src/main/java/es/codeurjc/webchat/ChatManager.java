package es.codeurjc.webchat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class ChatManager {

	private ConcurrentMap<String, Chat> chats = new ConcurrentHashMap<>();
	private ConcurrentMap<String, UserCallbackHandler> users = new ConcurrentHashMap<>();
	private BlockingQueue<Object> chatTokens;

	public ChatManager(int maxChats) {
		Object[] tokens = new Object[maxChats];
		Arrays.parallelSetAll(tokens, i -> new Object());
		chatTokens = new ArrayBlockingQueue<>(maxChats, false, Arrays.asList(tokens));
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

		ensureChatCapacity(timeout, unit);

		return chats.computeIfAbsent(name, n -> {
			Chat newChat = new Chat(this, name);
			for (UserCallbackHandler handler : users.values()) {
				handler.newChat(newChat);
			}
			return newChat;
		});
	}

	private void ensureChatCapacity(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		if (chatTokens.poll(timeout, unit) == null) {
			throw new TimeoutException("Timed out waiting for capacity to create new chat");
		}
	}

	public void closeChat(Chat chat) {
		chats.computeIfPresent(chat.getName(), (chatName, chatToRemove) -> {
			for (UserCallbackHandler handler : users.values()) {
				handler.chatClosed(chatToRemove);
			}
			increaseChatCapacity();
			return null;
		});
	}

	private void increaseChatCapacity() {
		if (!chatTokens.add(new Object())) {
			throw new RuntimeException("Failed to add chat capacity");
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
