package es.sidelab.webchat;

import es.codeurjc.webchat.Chat;
import es.codeurjc.webchat.User;

public class TestUser implements User {

	public String name;

	private boolean isSorted;

	public TestUser(String name) {
		this.name = name;
		this.isSorted = true;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public boolean getIsSorted() {
		return this.isSorted;
	}

	public void setIsSorted(boolean isOrdered) {
		this.isSorted = isOrdered;
	}

	@Override
	public String getColor() {
		return "007AFF";
	}

	@Override
	public void newChat(Chat chat) {
		System.out.println(this + ": New chat " + chat.getName());
	}

	@Override
	public void chatClosed(Chat chat) {
		System.out.println(this + ": Chat " + chat.getName() + " closed ");
	}

	@Override
	public void newUserInChat(Chat chat, User user) {
		System.out.println(this + ": New user " + user.getName() + " in chat " + chat.getName());
	}

	@Override
	public void userExitedFromChat(Chat chat, User user) {
		System.out.println(this + ": User " + user.getName() + " exited from chat " + chat.getName());
	}

	@Override
	public void newMessage(Chat chat, User user, String message) {
		System.out.println(
				this + ": New message '" + message + "' from user " + user.getName() + " in chat " + chat.getName());
	}

	@Override
	public String toString() {
		return "User[" + name + "]";
	}
}
