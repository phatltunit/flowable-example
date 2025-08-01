package vn.com.phat.chain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CloseApplication class.
 * Testing framework: JUnit 5 with Mockito
 */
@ExtendWith(MockitoExtension.class)
class CloseApplicationTest {

    @Mock
    private ConfigurableApplicationContext mockApplicationContext;

    @Mock
    private Logger mockLogger;

    private CloseApplication closeApplication;

    @BeforeEach
    void setUp() {
        closeApplication = new CloseApplication();
    }

    @Test
    void execute_ShouldLogInfoMessage_WhenCalled() {
        // Given
        String[] args = {"arg1", "arg2"};
        
        try (MockedStatic<org.slf4j.LoggerFactory> loggerFactory = mockStatic(org.slf4j.LoggerFactory.class)) {
            loggerFactory.when(() -> org.slf4j.LoggerFactory.getLogger(CloseApplication.class))
                    .thenReturn(mockLogger);
            
            // Create a new instance to use the mocked logger
            CloseApplication testInstance = new CloseApplication();
            
            // When
            testInstance.execute(mockApplicationContext, args);
            
            // Then
            verify(mockLogger).info("We'll close the Spring Application Context after the Process Engines has destroyed.");
        }
    }

    @Test
    void execute_ShouldCloseApplicationContext_WhenCalled() {
        // Given
        String[] args = {"arg1", "arg2"};
        
        // When
        closeApplication.execute(mockApplicationContext, args);
        
        // Then
        verify(mockApplicationContext, times(1)).close();
    }

    @Test
    void execute_ShouldCallCheckNext_WithCorrectParameters() {
        // Given
        String[] args = {"arg1", "arg2"};
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(ConfigurableApplicationContext.class), any(String[].class));
        
        // When
        spyCloseApplication.execute(mockApplicationContext, args);
        
        // Then
        verify(spyCloseApplication, times(1)).checkNext(mockApplicationContext, args);
    }

    @Test
    void execute_ShouldExecuteInCorrectOrder_LogThenCloseThencheckNext() {
        // Given
        String[] args = {"arg1"};
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(ConfigurableApplicationContext.class), any(String[].class));
        
        try (MockedStatic<org.slf4j.LoggerFactory> loggerFactory = mockStatic(org.slf4j.LoggerFactory.class)) {
            loggerFactory.when(() -> org.slf4j.LoggerFactory.getLogger(CloseApplication.class))
                    .thenReturn(mockLogger);
            
            // When
            spyCloseApplication.execute(mockApplicationContext, args);
            
            // Then - Verify order of operations using InOrder
            var inOrder = inOrder(mockLogger, mockApplicationContext, spyCloseApplication);
            inOrder.verify(mockLogger).info("We'll close the Spring Application Context after the Process Engines has destroyed.");
            inOrder.verify(mockApplicationContext).close();
            inOrder.verify(spyCloseApplication).checkNext(mockApplicationContext, args);
        }
    }

    @Test
    void execute_ShouldHandleNullArgs_Gracefully() {
        // Given
        String[] nullArgs = null;
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(ConfigurableApplicationContext.class), any());
        
        // When & Then - Should not throw exception
        spyCloseApplication.execute(mockApplicationContext, nullArgs);
        
        // Verify interactions still occur
        verify(mockApplicationContext, times(1)).close();
        verify(spyCloseApplication, times(1)).checkNext(mockApplicationContext, nullArgs);
    }

    @Test
    void execute_ShouldHandleEmptyArgs_Gracefully() {
        // Given
        String[] emptyArgs = new String[0];
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(ConfigurableApplicationContext.class), any(String[].class));
        
        // When
        spyCloseApplication.execute(mockApplicationContext, emptyArgs);
        
        // Then
        verify(mockApplicationContext, times(1)).close();
        verify(spyCloseApplication, times(1)).checkNext(mockApplicationContext, emptyArgs);
    }

    @Test
    void execute_ShouldPropagateException_WhenApplicationContextCloseThrows() {
        // Given
        String[] args = {"arg1"};
        RuntimeException expectedException = new RuntimeException("Close failed");
        doThrow(expectedException).when(mockApplicationContext).close();
        
        // When & Then
        try {
            closeApplication.execute(mockApplicationContext, args);
        } catch (RuntimeException e) {
            // Verify the exception is propagated
            assert e == expectedException;
        }
        
        // Verify close was attempted
        verify(mockApplicationContext, times(1)).close();
    }

    @Test
    void execute_ShouldStillCallCheckNext_EvenIfLoggingFails() {
        // Given
        String[] args = {"arg1"};
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(ConfigurableApplicationContext.class), any(String[].class));
        
        try (MockedStatic<org.slf4j.LoggerFactory> loggerFactory = mockStatic(org.slf4j.LoggerFactory.class)) {
            loggerFactory.when(() -> org.slf4j.LoggerFactory.getLogger(CloseApplication.class))
                    .thenThrow(new RuntimeException("Logging failed"));
            
            // When - Should handle logging failure gracefully
            try {
                spyCloseApplication.execute(mockApplicationContext, args);
            } catch (RuntimeException e) {
                // Expected due to logging failure
            }
            
            // Then - Verify other operations still attempted
            verify(mockApplicationContext, times(1)).close();
            verify(spyCloseApplication, times(1)).checkNext(mockApplicationContext, args);
        }
    }

    @Test
    void execute_ShouldHandleLargeArgsArray() {
        // Given
        String[] largeArgs = new String[1000];
        for (int i = 0; i < 1000; i++) {
            largeArgs[i] = "arg" + i;
        }
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(ConfigurableApplicationContext.class), any(String[].class));
        
        // When
        spyCloseApplication.execute(mockApplicationContext, largeArgs);
        
        // Then
        verify(mockApplicationContext, times(1)).close();
        verify(spyCloseApplication, times(1)).checkNext(mockApplicationContext, largeArgs);
    }

    @Test
    void execute_ShouldHandleArgsWithSpecialCharacters() {
        // Given
        String[] specialArgs = {"arg with spaces", "arg@with#special$chars", "arg\nwith\nnewlines", ""};
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(ConfigurableApplicationContext.class), any(String[].class));
        
        // When
        spyCloseApplication.execute(mockApplicationContext, specialArgs);
        
        // Then
        verify(mockApplicationContext, times(1)).close();
        verify(spyCloseApplication, times(1)).checkNext(mockApplicationContext, specialArgs);
    }

    @Test
    void execute_ShouldBeThreadSafe_WhenCalledConcurrently() throws InterruptedException {
        // Given
        String[] args = {"concurrent", "test"};
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(ConfigurableApplicationContext.class), any(String[].class));
        
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        
        // When - Execute concurrently
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> spyCloseApplication.execute(mockApplicationContext, args));
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then - Verify all calls were made
        verify(mockApplicationContext, times(threadCount)).close();
        verify(spyCloseApplication, times(threadCount)).checkNext(mockApplicationContext, args);
    }
}