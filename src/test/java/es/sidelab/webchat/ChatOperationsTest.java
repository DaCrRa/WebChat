package es.sidelab.webchat;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.spy;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import es.codeurjc.webchat.Chat;
import es.codeurjc.webchat.ChatManager;
import es.codeurjc.webchat.User;

public class ChatOperationsTest {

	ChatManager manager;

	@Before
	public void createChatManager() {
		manager = new ChatManager(50);
	}


	@Test
	public void givenChatManagerWithNUsersRegistered_whenNewChatCreated_thenAllUsersAreNotified() throws InterruptedException, TimeoutException {
		int numberOfUsers = 2;

		CountDownLatch countDownLatch = new CountDownLatch(numberOfUsers);

		User[] spyUsers = new User[numberOfUsers];
		Arrays.parallelSetAll(spyUsers, i -> {
			return spy(new TestUser("User " + i) {
				@Override
				public void newChat(Chat chat) {
					countDownLatch.countDown();
					super.newChat(chat);
				}
			});
		});

		Stream.of(spyUsers).forEach(user -> manager.newUser(user));

		Chat aChat = manager.newChat("new chat", 5, TimeUnit.SECONDS);

		countDownLatch.await(10, TimeUnit.SECONDS);

		Stream.of(spyUsers).forEach(spy -> {
			verify(spy, times(1)).newChat(aChat);
			verify(spy, times(1)).newChat(any());
		});

	}
	
	
    @Test
    public void givenChatManagerWithNUsersRegistered_whenOneUserHasConnectionIssue_thenAllUsersAreNotified() throws InterruptedException, TimeoutException {
        int numberOfUsers = 4;

        CountDownLatch countDownLatch = new CountDownLatch(numberOfUsers);

        User[] spyUsers = new User[numberOfUsers];
        
        Arrays.parallelSetAll(spyUsers, i -> {
            return spy(new TestUser("User " + i) {
                @Override
                public void newMessage(Chat chat, User user, String message) {
                    countDownLatch.countDown();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    super.newMessage(chat, user, message);
                }
            });
        });
            
        Chat chat = new Chat(this.manager, "New chat");
        Stream.of(spyUsers).forEach(user -> chat.addUser(user));
        
        try {
            if (Stream.of(spyUsers).findFirst() != null) {
                User user = Stream.of(spyUsers).findFirst().get();
                chat.sendMessage(user,  "test");
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
                
        countDownLatch.await(1, TimeUnit.SECONDS);
        
        Stream.of(spyUsers).forEach(spy -> {
            verify(spy, times(1)).newMessage(any(), any(), any());
        });
    }
}
