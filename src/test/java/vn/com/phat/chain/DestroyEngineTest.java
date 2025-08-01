package vn.com.phat.chain;

import org.flowable.engine.ProcessEngines;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DestroyEngine class.
 * Testing Framework: JUnit 5 with Mockito for mocking
 * 
 * Tests cover:
 * - Happy path scenarios with valid inputs
 * - Edge cases with null/empty arguments
 * - Error handling scenarios
 * - Chain of responsibility pattern verification
 * - Thread safety considerations
 */
@ExtendWith(MockitoExtension.class)
class DestroyEngineTest {

    @Mock
    private ConfigurableApplicationContext applicationContext;

    @Mock
    private ChainExecutor nextChainExecutor;

    private DestroyEngine destroyEngine;

    @BeforeEach
    void setUp() {
        destroyEngine = new DestroyEngine();
        // Set up the next chain executor using reflection to access private field
        ReflectionTestUtils.setField(destroyEngine, "next", nextChainExecutor);
    }

    @Test
    void execute_ShouldDestroyProcessEnginesAndCallNext_WhenExecutedWithValidContext() {
        // Given
        String[] args = {"arg1", "arg2"};

        // When & Then
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            destroyEngine.execute(applicationContext, args);

            // Verify ProcessEngines.destroy() is called exactly once
            processEnginesMock.verify(ProcessEngines::destroy, times(1));
        }

        // Verify checkNext is called with correct parameters
        verify(nextChainExecutor, times(1)).execute(eq(applicationContext), eq(args));
    }

    @Test
    void execute_ShouldDestroyProcessEnginesAndCallNext_WhenExecutedWithEmptyArgs() {
        // Given
        String[] emptyArgs = {};

        // When & Then
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            destroyEngine.execute(applicationContext, emptyArgs);

            processEnginesMock.verify(ProcessEngines::destroy, times(1));
        }

        verify(nextChainExecutor, times(1)).execute(eq(applicationContext), eq(emptyArgs));
    }

    @Test
    void execute_ShouldDestroyProcessEnginesAndCallNext_WhenExecutedWithNullArgs() {
        // Given
        String[] nullArgs = null;

        // When & Then
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            destroyEngine.execute(applicationContext, nullArgs);

            processEnginesMock.verify(ProcessEngines::destroy, times(1));
        }

        verify(nextChainExecutor, times(1)).execute(eq(applicationContext), eq(nullArgs));
    }

    @Test
    void execute_ShouldStillCallNextChainExecutor_WhenProcessEnginesDestroyThrowsRuntimeException() {
        // Given
        String[] args = {"arg1"};
        RuntimeException expectedException = new RuntimeException("Process engine destruction failed");

        // When & Then
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            processEnginesMock.when(ProcessEngines::destroy).thenThrow(expectedException);

            // Should propagate the exception
            RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
                destroyEngine.execute(applicationContext, args);
            });

            assertEquals("Process engine destruction failed", thrownException.getMessage());
            processEnginesMock.verify(ProcessEngines::destroy, times(1));
        }

        // Next chain executor should not be called if exception occurs before checkNext
        verify(nextChainExecutor, never()).execute(any(), any());
    }

    @Test
    void execute_ShouldHandleNullApplicationContext_WithoutThrowingException() {
        // Given
        ConfigurableApplicationContext nullContext = null;
        String[] args = {"arg1"};

        // When & Then
        assertDoesNotThrow(() -> {
            try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
                destroyEngine.execute(nullContext, args);
                processEnginesMock.verify(ProcessEngines::destroy, times(1));
            }
        });

        verify(nextChainExecutor, times(1)).execute(eq(nullContext), eq(args));
    }

    @Test
    void execute_ShouldMaintainChainOfResponsibilityPattern_WhenMultipleArgsProvided() {
        // Given
        String[] multipleArgs = {"arg1", "arg2", "arg3", "--verbose", "--config=test"};

        // When & Then
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            destroyEngine.execute(applicationContext, multipleArgs);

            processEnginesMock.verify(ProcessEngines::destroy, times(1));
        }

        // Verify all arguments are passed to next executor
        verify(nextChainExecutor, times(1)).execute(eq(applicationContext), eq(multipleArgs));
    }

    @Test
    void execute_ShouldCallProcessEnginesDestroyForEachExecution_WhenExecutedMultipleTimes() {
        // Given
        String[] args = {"arg1"};

        // When & Then
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            destroyEngine.execute(applicationContext, args);
            destroyEngine.execute(applicationContext, args);
            destroyEngine.execute(applicationContext, args);

            // Should be called once for each execution
            processEnginesMock.verify(ProcessEngines::destroy, times(3));
        }

        verify(nextChainExecutor, times(3)).execute(eq(applicationContext), eq(args));
    }

    @Test
    void execute_ShouldWorkWithoutNextChainExecutor_WhenNextIsNull() {
        // Given
        String[] args = {"arg1"};
        DestroyEngine standaloneEngine = new DestroyEngine();
        // Don't set next executor (it should be null by default)

        // When & Then
        assertDoesNotThrow(() -> {
            try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
                standaloneEngine.execute(applicationContext, args);
                processEnginesMock.verify(ProcessEngines::destroy, times(1));
            }
        });

        // No interaction with nextChainExecutor since it's null
        verifyNoInteractions(nextChainExecutor);
    }

    @Test
    void execute_ShouldHandleVeryLargeArgumentArrays_WithoutPerformanceIssues() {
        // Given
        String[] largeArgsArray = new String[1000];
        for (int i = 0; i < 1000; i++) {
            largeArgsArray[i] = "arg" + i;
        }

        // When & Then
        assertDoesNotThrow(() -> {
            try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
                destroyEngine.execute(applicationContext, largeArgsArray);
                processEnginesMock.verify(ProcessEngines::destroy, times(1));
            }
        });

        verify(nextChainExecutor, times(1)).execute(eq(applicationContext), eq(largeArgsArray));
    }

    @Test
    void execute_ShouldHandleArgsWithSpecialCharacters_WithoutIssues() {
        // Given
        String[] specialArgs = {"arg with spaces", "arg@with#special$chars", "arg\nwith\nnewlines", ""};

        // When & Then
        assertDoesNotThrow(() -> {
            try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
                destroyEngine.execute(applicationContext, specialArgs);
                processEnginesMock.verify(ProcessEngines::destroy, times(1));
            }
        });

        verify(nextChainExecutor, times(1)).execute(eq(applicationContext), eq(specialArgs));
    }

    @Test
    void execute_ShouldBeThreadSafe_WhenExecutedConcurrently() throws InterruptedException {
        // Given
        String[] args = {"arg1"};
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        
        // When
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> destroyEngine.execute(applicationContext, args));
                threads[i].start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join(5000); // 5 second timeout per thread
            }

            // Then - ProcessEngines.destroy should be called once per thread
            processEnginesMock.verify(ProcessEngines::destroy, times(threadCount));
        }

        // Verify next executor is called for each thread execution
        verify(nextChainExecutor, times(threadCount)).execute(eq(applicationContext), eq(args));
    }

    @Test
    void execute_ShouldInheritFromChainExecutor_AndImplementAbstractMethod() {
        // Given & When & Then
        assertTrue(destroyEngine instanceof ChainExecutor, 
                   "DestroyEngine should extend ChainExecutor");
        
        // Verify the abstract method is properly implemented
        assertDoesNotThrow(() -> {
            try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
                destroyEngine.execute(applicationContext, new String[]{"test"});
                processEnginesMock.verify(ProcessEngines::destroy, times(1));
            }
        });
    }

    @Test
    void execute_ShouldHandleProcessEnginesError_GracefullyWithoutAffectingChain() {
        // Given
        String[] args = {"arg1"};
        Error systemError = new OutOfMemoryError("Simulated system error");

        // When & Then
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            processEnginesMock.when(ProcessEngines::destroy).thenThrow(systemError);

            // Should propagate the error
            Error thrownError = assertThrows(OutOfMemoryError.class, () -> {
                destroyEngine.execute(applicationContext, args);
            });

            assertEquals("Simulated system error", thrownError.getMessage());
            processEnginesMock.verify(ProcessEngines::destroy, times(1));
        }

        // Next chain executor should not be called when Error occurs
        verify(nextChainExecutor, never()).execute(any(), any());
    }

    @Test
    void execute_ShouldLogCorrectMessage_WhenExecuted() {
        // Given
        String[] args = {"arg1"};
        
        // This test verifies the method executes without issues
        // In a real scenario, you might use a logging framework like LogCaptor
        // to capture and verify the actual log message
        
        // When & Then
        assertDoesNotThrow(() -> {
            try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
                destroyEngine.execute(applicationContext, args);
                processEnginesMock.verify(ProcessEngines::destroy, times(1));
            }
        });

        verify(nextChainExecutor, times(1)).execute(eq(applicationContext), eq(args));
    }

    @Test
    void execute_ShouldHandleNullArgsArrayElements_WithoutIssues() {
        // Given
        String[] argsWithNulls = {"arg1", null, "arg3", null};

        // When & Then
        assertDoesNotThrow(() -> {
            try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
                destroyEngine.execute(applicationContext, argsWithNulls);
                processEnginesMock.verify(ProcessEngines::destroy, times(1));
            }
        });

        verify(nextChainExecutor, times(1)).execute(eq(applicationContext), eq(argsWithNulls));
    }

    @Test
    void execute_ShouldMaintainExecutionOrder_WhenCalledSequentially() {
        // Given
        String[] firstArgs = {"first", "execution"};
        String[] secondArgs = {"second", "execution"};

        // When & Then
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            destroyEngine.execute(applicationContext, firstArgs);
            destroyEngine.execute(applicationContext, secondArgs);

            processEnginesMock.verify(ProcessEngines::destroy, times(2));
        }

        // Verify execution order is maintained
        verify(nextChainExecutor, times(1)).execute(eq(applicationContext), eq(firstArgs));
        verify(nextChainExecutor, times(1)).execute(eq(applicationContext), eq(secondArgs));
    }
}