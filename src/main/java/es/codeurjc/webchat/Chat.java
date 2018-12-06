package es.codeurjc.webchat;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Chat {

	private static final int NUM_THREADS = 10; 

	private String name;

	private ConcurrentMap<String, User> users = 
			new ConcurrentHashMap<String, User>();
	
	ExecutorService executor =
			Executors.newFixedThreadPool(NUM_THREADS);

	CompletionService<Void> completionService =
			new ExecutorCompletionService<>(executor);

	private ChatManager chatManager;

	public Chat(ChatManager chatManager, String name) {
		this.chatManager = chatManager;
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void addUser(User user) {
		
		this.users.put(user.getName(), user);
		for(User u : this.users.values()){
			if (u != user) {
				u.newUserInChat(this, user);
			}
		}
	}

	public void removeUser(User user) {
		this.users.remove(user.getName());
		for(User u : this.users.values()){
			u.userExitedFromChat(this, user);
		}
	}

	public Collection<User> getUsers() {
		return Collections.unmodifiableCollection(users.values());
	}

	public User getUser(String name) {
		return this.users.get(name);
	}

	public void sendMessage(User user, String message) throws Throwable {
		
		for (User u : this.users.values()) {
		    
		    if (!u.equals(user)) {
		        completionService.submit(() -> {
				    u.newMessage(this, user, message);
				    return null;
			    });
		    }
		}

		for (int i=0; i<this.users.size()-1; i++) {
			try {
				this.completionService.take().get();
			} catch (ExecutionException e) {
				throw e.getCause();
			}
		}
	}

	public void close() {
		this.chatManager.closeChat(this);
	}
}
