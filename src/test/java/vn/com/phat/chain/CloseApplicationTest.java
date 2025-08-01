package vn.com.phat.chain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for CloseApplication class
 * Testing framework: JUnit 5 with Mockito (via spring-boot-starter-test)
 * 
 * This test class covers:
 * - Happy path scenarios for normal execution
 * - Edge cases with different argument combinations  
 * - Exception handling scenarios when ApplicationContext.close() fails
 * - Chain execution verification using the ChainExecutor pattern
 * - Integration with Spring Application Context lifecycle
 * - Concurrency and thread safety scenarios
 * - Boundary value testing and error recovery
 */
@ExtendWith(MockitoExtension.class)
class CloseApplicationTest {

    @Mock
    private ConfigurableApplicationContext mockApplicationContext;

    private CloseApplication closeApplication;

    @BeforeEach
    void setUp() {
        closeApplication = new CloseApplication();
    }

    // Happy Path Tests
    @Test
    void execute_ShouldCloseApplicationContextSuccessfully() {
        // Given
        String[] args = {"arg1", "arg2"};

        // When
        closeApplication.execute(mockApplicationContext, args);

        // Then
        verify(mockApplicationContext, times(1)).close();
    }

    @Test
    void execute_ShouldCallCheckNextAfterClosingContext() {
        // Given
        String[] args = {"test-arg"};
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(ConfigurableApplicationContext.class), any(String[].class));

        // When
        spyCloseApplication.execute(mockApplicationContext, args);

        // Then
        verify(mockApplicationContext, times(1)).close();
        verify(spyCloseApplication, times(1)).checkNext(mockApplicationContext, args);
    }

    @Test
    void execute_ShouldMaintainCorrectExecutionOrder() {
        // Given
        String[] args = {"order-test"};
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(), any());
        
        var inOrder = inOrder(mockApplicationContext, spyCloseApplication);

        // When
        spyCloseApplication.execute(mockApplicationContext, args);

        // Then - verify the order: close first, then checkNext
        inOrder.verify(mockApplicationContext).close();
        inOrder.verify(spyCloseApplication).checkNext(mockApplicationContext, args);
    }

    // Edge Cases with Arguments
    @Test
    void execute_ShouldHandleEmptyArgsArray() {
        // Given
        String[] emptyArgs = {};
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(), any());

        // When
        spyCloseApplication.execute(mockApplicationContext, emptyArgs);

        // Then
        verify(mockApplicationContext, times(1)).close();
        verify(spyCloseApplication, times(1)).checkNext(mockApplicationContext, emptyArgs);
    }

    @Test
    void execute_ShouldHandleNullArgsArray() {
        // Given
        String[] nullArgs = null;
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(), any());

        // When
        spyCloseApplication.execute(mockApplicationContext, nullArgs);

        // Then
        verify(mockApplicationContext, times(1)).close();
        verify(spyCloseApplication, times(1)).checkNext(mockApplicationContext, nullArgs);
    }

    @Test
    void execute_ShouldPassExactSameArgumentsToCheckNext() {
        // Given
        String[] originalArgs = {"arg1", "arg2", "arg3"};
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(), any());

        // When
        spyCloseApplication.execute(mockApplicationContext, originalArgs);

        // Then
        verify(spyCloseApplication).checkNext(eq(mockApplicationContext), eq(originalArgs));
    }

    @Test
    void execute_ShouldHandleLargeArgumentsArray() {
        // Given
        String[] largeArgs = new String[1000];
        for (int i = 0; i < 1000; i++) {
            largeArgs[i] = "argument" + i;
        }
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(), any());

        // When
        assertDoesNotThrow(() -> spyCloseApplication.execute(mockApplicationContext, largeArgs));

        // Then
        verify(mockApplicationContext, times(1)).close();
        verify(spyCloseApplication, times(1)).checkNext(mockApplicationContext, largeArgs);
    }

    @Test
    void execute_ShouldHandleArgsWithSpecialCharacters() {
        // Given
        String[] specialArgs = {
            "arg with spaces", 
            "arg-with-dashes", 
            "arg_with_underscores", 
            "arg@with#symbols",
            "arg/with/slashes",
            "arg\\with\\backslashes",
            "arg\"with\"quotes",
            "arg'with'apostrophes"
        };
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(), any());

        // When
        spyCloseApplication.execute(mockApplicationContext, specialArgs);

        // Then
        verify(mockApplicationContext, times(1)).close();
        verify(spyCloseApplication, times(1)).checkNext(mockApplicationContext, specialArgs);
    }

    // Exception Handling Tests
    @Test
    void execute_ShouldPropagateExceptionWhenApplicationContextCloseThrows() {
        // Given
        String[] args = {"test"};
        RuntimeException expectedException = new RuntimeException("Application context close failed");
        doThrow(expectedException).when(mockApplicationContext).close();

        // When & Then
        RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
            closeApplication.execute(mockApplicationContext, args);
        });

        assertEquals("Application context close failed", thrownException.getMessage());
        verify(mockApplicationContext, times(1)).close();
    }

    @Test
    void execute_ShouldNotCallCheckNextWhenCloseThrowsException() {
        // Given
        String[] args = {"test"};
        CloseApplication spyCloseApplication = spy(closeApplication);
        doThrow(new RuntimeException("Close failed")).when(mockApplicationContext).close();

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            spyCloseApplication.execute(mockApplicationContext, args);
        });

        // Verify checkNext is never called when close fails
        verify(spyCloseApplication, never()).checkNext(any(), any());
    }

    @Test
    void execute_ShouldHandleIllegalStateExceptionFromClose() {
        // Given
        String[] args = {"state-test"};
        doThrow(new IllegalStateException("Context already closed")).when(mockApplicationContext).close();

        // When & Then
        IllegalStateException thrownException = assertThrows(IllegalStateException.class, () -> {
            closeApplication.execute(mockApplicationContext, args);
        });

        assertEquals("Context already closed", thrownException.getMessage());
        verify(mockApplicationContext, times(1)).close();
    }

    @Test
    void execute_ShouldHandleGenericExceptionFromClose() {
        // Given
        String[] args = {"generic-exception-test"};
        Exception expectedException = new Exception("Generic close failure");
        doThrow(expectedException).when(mockApplicationContext).close();

        // When & Then
        Exception thrownException = assertThrows(Exception.class, () -> {
            closeApplication.execute(mockApplicationContext, args);
        });

        assertEquals("Generic close failure", thrownException.getMessage());
        verify(mockApplicationContext, times(1)).close();
    }

    // Multiple Invocation Tests
    @Test
    void execute_ShouldWorkWithMultipleInvocations() {
        // Given
        String[] args1 = {"first-invocation"};
        String[] args2 = {"second-invocation"};
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(), any());

        // When
        spyCloseApplication.execute(mockApplicationContext, args1);
        spyCloseApplication.execute(mockApplicationContext, args2);

        // Then
        verify(mockApplicationContext, times(2)).close();
        verify(spyCloseApplication, times(1)).checkNext(mockApplicationContext, args1);
        verify(spyCloseApplication, times(1)).checkNext(mockApplicationContext, args2);
    }

    @Test
    void execute_ShouldWorkWithDifferentApplicationContexts() {
        // Given
        ConfigurableApplicationContext secondContext = mock(ConfigurableApplicationContext.class);
        String[] args = {"multi-context-test"};
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(), any());

        // When
        spyCloseApplication.execute(mockApplicationContext, args);
        spyCloseApplication.execute(secondContext, args);

        // Then
        verify(mockApplicationContext, times(1)).close();
        verify(secondContext, times(1)).close();
        verify(spyCloseApplication, times(1)).checkNext(mockApplicationContext, args);
        verify(spyCloseApplication, times(1)).checkNext(secondContext, args);
    }

    // State and Behavior Tests
    @Test
    void execute_ShouldNotModifyInputArguments() {
        // Given
        String[] originalArgs = {"original1", "original2"};
        String[] argsCopy = originalArgs.clone();
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(), any());

        // When
        spyCloseApplication.execute(mockApplicationContext, originalArgs);

        // Then
        assertArrayEquals(argsCopy, originalArgs, "Input arguments should not be modified");
    }

    @Test
    void execute_ShouldHandleNullApplicationContext() {
        // Given
        String[] args = {"null-context-test"};

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            closeApplication.execute(null, args);
        });
    }

    // Inheritance and Type Tests
    @Test
    void closeApplication_ShouldExtendChainExecutor() {
        // When & Then
        assertTrue(closeApplication instanceof ChainExecutor, 
                  "CloseApplication should extend ChainExecutor");
    }

    @Test
    void closeApplication_ShouldBeInstantiableWithoutParameters() {
        // When
        CloseApplication newInstance = new CloseApplication();

        // Then
        assertNotNull(newInstance, "CloseApplication should be instantiable");
        assertTrue(newInstance instanceof ChainExecutor, 
                  "New instance should be a ChainExecutor");
    }

    @Test
    void execute_ShouldCallApplicationContextCloseExactlyOnce() {
        // Given
        String[] args = {"single-call-test"};
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(), any());

        // When
        spyCloseApplication.execute(mockApplicationContext, args);

        // Then
        verify(mockApplicationContext, times(1)).close();
        verify(mockApplicationContext, never()).start();
        verify(mockApplicationContext, never()).stop();
    }

    // Chain Executor Integration Tests
    @Test
    void execute_ShouldIntegrateWithChainExecutorPattern() {
        // Given
        CloseApplication firstExecutor = new CloseApplication();
        CloseApplication secondExecutor = new CloseApplication();
        String[] args = {"chain-test"};
        
        // Link the executors using ChainExecutor.link method
        ChainExecutor chain = ChainExecutor.link(firstExecutor, secondExecutor);
        
        // When
        assertDoesNotThrow(() -> chain.execute(mockApplicationContext, args));

        // Then
        verify(mockApplicationContext, atLeastOnce()).close();
    }

    @Test
    void execute_ShouldWorkInChainWithNullNext() {
        // Given
        String[] args = {"null-next-test"};
        CloseApplication spyCloseApplication = spy(closeApplication);

        // When - execute without linking to another executor
        assertDoesNotThrow(() -> spyCloseApplication.execute(mockApplicationContext, args));

        // Then
        verify(mockApplicationContext, times(1)).close();
        verify(spyCloseApplication, times(1)).checkNext(mockApplicationContext, args);
    }

    // Concurrent Execution Test
    @Test
    void execute_ShouldHandleConcurrentExecution() throws InterruptedException {
        // Given
        String[] args = {"concurrent-test"};
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(), any());
        
        Thread[] threads = new Thread[10];
        
        // When
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                spyCloseApplication.execute(mockApplicationContext, args);
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        verify(mockApplicationContext, times(10)).close();
        verify(spyCloseApplication, times(10)).checkNext(mockApplicationContext, args);
    }

    // Integration-style Test
    @Test
    void execute_ShouldLogAndCloseAsExpectedInRealScenario() {
        // Given
        String[] realArgs = {"--spring.profiles.active=test", "--server.port=8080"};
        
        // When
        assertDoesNotThrow(() -> closeApplication.execute(mockApplicationContext, realArgs));

        // Then
        verify(mockApplicationContext, times(1)).close();
    }

    // Boundary Value Tests
    @Test
    void execute_ShouldHandleMaximumArgumentSize() {
        // Given
        StringBuilder largeArg = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeArg.append("a");
        }
        String[] args = {largeArg.toString()};
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(), any());

        // When
        assertDoesNotThrow(() -> spyCloseApplication.execute(mockApplicationContext, args));

        // Then
        verify(mockApplicationContext, times(1)).close();
        verify(spyCloseApplication, times(1)).checkNext(mockApplicationContext, args);
    }

    @Test
    void execute_ShouldHandleUnicodeCharactersInArgs() {
        // Given
        String[] unicodeArgs = {"测试参数", "🚀emoji", "Ñoël", "العربية"};
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(), any());

        // When
        spyCloseApplication.execute(mockApplicationContext, unicodeArgs);

        // Then
        verify(mockApplicationContext, times(1)).close();
        verify(spyCloseApplication, times(1)).checkNext(mockApplicationContext, unicodeArgs);
    }

    // Error Recovery Tests
    @Test
    void execute_ShouldPropagateOriginalExceptionWhenCloseFailsMultipleTimes() {
        // Given
        String[] args = {"multiple-failures-test"};
        RuntimeException firstException = new RuntimeException("First failure");
        doThrow(firstException).when(mockApplicationContext).close();

        // When & Then
        RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
            closeApplication.execute(mockApplicationContext, args);
        });

        assertEquals("First failure", thrownException.getMessage());
        verify(mockApplicationContext, times(1)).close();
    }

    // Flowable-specific Integration Test
    @Test
    void execute_ShouldWorkInFlowableApplicationShutdownSequence() {
        // Given - simulating Flowable application shutdown scenario
        String[] flowableArgs = {"--flowable.async-executor.activate=false"};
        CloseApplication spyCloseApplication = spy(closeApplication);
        doNothing().when(spyCloseApplication).checkNext(any(), any());

        // When
        assertDoesNotThrow(() -> spyCloseApplication.execute(mockApplicationContext, flowableArgs));

        // Then
        verify(mockApplicationContext, times(1)).close();
        verify(spyCloseApplication, times(1)).checkNext(mockApplicationContext, flowableArgs);
    }

    // Logging Verification Test (testing the log message behavior)
    @Test
    void execute_ShouldExecuteWithoutThrowingExceptionsForLogging() {
        // Given
        String[] args = {"logging-test"};

        // When & Then - this verifies that the logging doesn't cause issues
        assertDoesNotThrow(() -> closeApplication.execute(mockApplicationContext, args));
        
        verify(mockApplicationContext, times(1)).close();
    }

    // Performance Test
    @Test
    void execute_ShouldCompleteWithinReasonableTime() {
        // Given
        String[] args = {"performance-test"};
        long startTime = System.currentTimeMillis();

        // When
        closeApplication.execute(mockApplicationContext, args);
        long endTime = System.currentTimeMillis();

        // Then
        long executionTime = endTime - startTime;
        assertTrue(executionTime < 1000, "Execution should complete within 1 second");
        verify(mockApplicationContext, times(1)).close();
    }
}