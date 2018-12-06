package es.sidelab.webchat;

import static org.junit.Assert.assertEquals;
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
    public void givenChatManagerWithNUsersRegistered_whenOneUserHasConnectionIssue_thenAllUsersAreNotified() 
            throws InterruptedException, TimeoutException {
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
                    try {
                        super.newMessage(chat, user, message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
        
        Stream.of(spyUsers).skip(1).forEach(spy -> {
            try {
                verify(spy, times(1)).newMessage(any(), any(), any());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    
    @Test
    public void givenChatManagerWithNUsersRegistered_whenOneUserHasConnectionIssue_thenAllUsersAreNotifiedInSortOrder() 
            throws InterruptedException, TimeoutException {
        
        User producer = new TestUser("producer"){
            @Override
            public void newMessage(Chat chat, User user, String message) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    super.newMessage(chat, user, message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        
        User consumer = new TestUser("consumer"){
            int messageId = 0;
            
            @Override
            public void newMessage(Chat chat, User user, String message) {
                if ((Integer.parseInt(message))== messageId) {
                    messageId++;
                } else {
                    this.setIsSorted(false);
                }
                super.newMessage(chat, user, message);
            }
        };
        
        Chat chat = new Chat(this.manager, "New chat");
        chat.addUser(producer);
        chat.addUser(consumer);
                
        for (int i=0; i<5; i++) {
            try {
                chat.sendMessage(producer, String.valueOf(i));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        
        assertEquals(((TestUser) consumer).getIsSorted(), true);
    }
}
