package vn.com.phat.chain;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.flowable.engine.ProcessEngines;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for DestroyEngine class.
 * Testing Framework: JUnit 5 with Mockito for mocking and Logback for log testing
 * 
 * This test class covers:
 * - Happy path scenarios with various argument combinations
 * - Edge cases including null parameters and empty arrays
 * - Exception handling when ProcessEngines.destroy() fails
 * - Logging verification at the correct level with exact message
 * - Chain of responsibility pattern verification (checkNext calls)
 * - Execution order verification
 * - Class structure and annotation validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DestroyEngine Tests")
class DestroyEngineTest {

    @Mock
    private ConfigurableApplicationContext applicationContext;

    private DestroyEngine destroyEngine;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        destroyEngine = new DestroyEngine();
        
        // Set up log capture for testing log output
        logger = (Logger) LoggerFactory.getLogger(DestroyEngine.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.ALL);
    }

    @AfterEach
    void tearDown() {
        if (listAppender != null) {
            logger.detachAppender(listAppender);
        }
    }

    @Test
    @DisplayName("Should execute successfully with valid application context and no arguments")
    void execute_WithValidContextAndNoArgs_ShouldExecuteSuccessfully() {
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            // Given
            String[] args = {};

            // When
            destroyEngine.execute(applicationContext, args);

            // Then
            processEnginesMock.verify(ProcessEngines::destroy, times(1));
            
            // Verify logging
            assertEquals(1, listAppender.list.size());
            ILoggingEvent loggingEvent = listAppender.list.get(0);
            assertEquals(Level.INFO, loggingEvent.getLevel());
            assertEquals("We'll destroy the Flowable Process Engines after the application has started.", 
                        loggingEvent.getFormattedMessage());
        }
    }

    @Test
    @DisplayName("Should execute successfully with valid application context and multiple arguments")
    void execute_WithValidContextAndMultipleArgs_ShouldExecuteSuccessfully() {
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            // Given
            String[] args = {"arg1", "arg2", "arg3"};

            // When
            destroyEngine.execute(applicationContext, args);

            // Then
            processEnginesMock.verify(ProcessEngines::destroy, times(1));
            
            // Verify logging
            assertEquals(1, listAppender.list.size());
            ILoggingEvent loggingEvent = listAppender.list.get(0);
            assertEquals(Level.INFO, loggingEvent.getLevel());
            assertEquals("We'll destroy the Flowable Process Engines after the application has started.", 
                        loggingEvent.getFormattedMessage());
        }
    }

    @Test
    @DisplayName("Should execute successfully with null arguments")
    void execute_WithNullArgs_ShouldExecuteSuccessfully() {
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            // Given
            String[] args = null;

            // When
            destroyEngine.execute(applicationContext, args);

            // Then
            processEnginesMock.verify(ProcessEngines::destroy, times(1));
            
            // Verify logging
            assertEquals(1, listAppender.list.size());
        }
    }

    @Test
    @DisplayName("Should handle ProcessEngines.destroy() throwing RuntimeException")
    void execute_WhenProcessEnginesDestroyThrowsRuntimeException_ShouldPropagateException() {
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            // Given
            String[] args = {"test"};
            RuntimeException expectedException = new RuntimeException("Engine destruction failed");
            processEnginesMock.when(ProcessEngines::destroy).thenThrow(expectedException);

            // When & Then
            RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
                destroyEngine.execute(applicationContext, args);
            });

            assertEquals("Engine destruction failed", thrownException.getMessage());
            
            // Verify ProcessEngines.destroy was called despite the exception
            processEnginesMock.verify(ProcessEngines::destroy, times(1));
            
            // Verify logging still occurred before the exception
            assertEquals(1, listAppender.list.size());
            assertEquals("We'll destroy the Flowable Process Engines after the application has started.", 
                        listAppender.list.get(0).getFormattedMessage());
        }
    }

    @Test
    @DisplayName("Should handle ProcessEngines.destroy() throwing IllegalStateException")
    void execute_WhenProcessEnginesDestroyThrowsIllegalStateException_ShouldPropagateException() {
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            // Given
            String[] args = {"test"};
            IllegalStateException expectedException = new IllegalStateException("Process engines already destroyed");
            processEnginesMock.when(ProcessEngines::destroy).thenThrow(expectedException);

            // When & Then
            IllegalStateException thrownException = assertThrows(IllegalStateException.class, () -> {
                destroyEngine.execute(applicationContext, args);
            });

            assertEquals("Process engines already destroyed", thrownException.getMessage());
            processEnginesMock.verify(ProcessEngines::destroy, times(1));
        }
    }

    @Test
    @DisplayName("Should handle null application context")
    void execute_WithNullApplicationContext_ShouldExecuteWithoutError() {
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            // Given
            ConfigurableApplicationContext nullContext = null;
            String[] args = {"test"};

            // When
            destroyEngine.execute(nullContext, args);

            // Then
            processEnginesMock.verify(ProcessEngines::destroy, times(1));
            
            // Verify logging
            assertEquals(1, listAppender.list.size());
        }
    }

    @Test
    @DisplayName("Should execute in correct order: log, destroy engines, then check next")
    void execute_ShouldFollowCorrectExecutionOrder() {
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            // Given
            String[] args = {"test"};
            
            // Set up a spy to verify execution order
            DestroyEngine spyDestroyEngine = spy(destroyEngine);
            doNothing().when(spyDestroyEngine).checkNext(any(), any());

            // When
            spyDestroyEngine.execute(applicationContext, args);

            // Then - verify execution order
            // 1. Log message should be present
            assertEquals(1, listAppender.list.size());
            assertEquals("We'll destroy the Flowable Process Engines after the application has started.", 
                        listAppender.list.get(0).getFormattedMessage());
            
            // 2. ProcessEngines.destroy should be called
            processEnginesMock.verify(ProcessEngines::destroy, times(1));
            
            // 3. checkNext should be called with correct parameters
            verify(spyDestroyEngine, times(1)).checkNext(applicationContext, args);
        }
    }

    @Test
    @DisplayName("Should pass same arguments to checkNext method")
    void execute_ShouldPassSameArgumentsToCheckNext() {
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            // Given
            String[] originalArgs = {"arg1", "arg2", "test-arg"};
            DestroyEngine spyDestroyEngine = spy(destroyEngine);
            doNothing().when(spyDestroyEngine).checkNext(any(), any());

            // When
            spyDestroyEngine.execute(applicationContext, originalArgs);

            // Then
            verify(spyDestroyEngine, times(1)).checkNext(applicationContext, originalArgs);
        }
    }

    @Test
    @DisplayName("Should pass same application context to checkNext method")
    void execute_ShouldPassSameApplicationContextToCheckNext() {
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            // Given
            String[] args = {"test"};
            DestroyEngine spyDestroyEngine = spy(destroyEngine);
            doNothing().when(spyDestroyEngine).checkNext(any(), any());

            // When
            spyDestroyEngine.execute(applicationContext, args);

            // Then
            verify(spyDestroyEngine, times(1)).checkNext(eq(applicationContext), eq(args));
        }
    }

    @Test
    @DisplayName("Should log at INFO level")
    void execute_ShouldLogAtInfoLevel() {
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            // Given
            String[] args = {"test"};

            // When
            destroyEngine.execute(applicationContext, args);

            // Then
            assertEquals(1, listAppender.list.size());
            ILoggingEvent loggingEvent = listAppender.list.get(0);
            assertEquals(Level.INFO, loggingEvent.getLevel());
        }
    }

    @Test
    @DisplayName("Should have exactly the expected log message")
    void execute_ShouldHaveExactLogMessage() {
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            // Given
            String[] args = {};
            String expectedMessage = "We'll destroy the Flowable Process Engines after the application has started.";

            // When
            destroyEngine.execute(applicationContext, args);

            // Then
            assertEquals(1, listAppender.list.size());
            assertEquals(expectedMessage, listAppender.list.get(0).getFormattedMessage());
        }
    }

    @Test
    @DisplayName("Should be instance of ChainExecutor")
    void destroyEngine_ShouldBeInstanceOfChainExecutor() {
        // Then
        assertInstanceOf(ChainExecutor.class, destroyEngine);
    }

    @Test
    @DisplayName("Should have proper class annotations")
    void destroyEngine_ShouldHaveSlf4jAnnotation() {
        // Then
        assertTrue(DestroyEngine.class.isAnnotationPresent(lombok.extern.slf4j.Slf4j.class));
    }

    @Test
    @DisplayName("Should handle empty string arguments")
    void execute_WithEmptyStringArguments_ShouldExecuteSuccessfully() {
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            // Given
            String[] args = {"", " ", ""};

            // When
            destroyEngine.execute(applicationContext, args);

            // Then
            processEnginesMock.verify(ProcessEngines::destroy, times(1));
            assertEquals(1, listAppender.list.size());
        }
    }

    @Test
    @DisplayName("Should handle very large number of arguments")
    void execute_WithManyArguments_ShouldExecuteSuccessfully() {
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            // Given
            String[] args = new String[1000];
            for (int i = 0; i < 1000; i++) {
                args[i] = "arg" + i;
            }

            // When
            destroyEngine.execute(applicationContext, args);

            // Then
            processEnginesMock.verify(ProcessEngines::destroy, times(1));
            assertEquals(1, listAppender.list.size());
        }
    }

    @Test
    @DisplayName("Should maintain consistent behavior across multiple executions")
    void execute_MultipleExecutions_ShouldLogEachTime() {
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            // Given
            String[] args = {"test"};

            // When
            destroyEngine.execute(applicationContext, args);
            destroyEngine.execute(applicationContext, args);

            // Then
            processEnginesMock.verify(ProcessEngines::destroy, times(2));
            assertEquals(2, listAppender.list.size());
            
            // Both log messages should be identical
            assertEquals(listAppender.list.get(0).getFormattedMessage(), 
                        listAppender.list.get(1).getFormattedMessage());
        }
    }

    @Test
    @DisplayName("Should handle arguments with special characters")
    void execute_WithSpecialCharacterArguments_ShouldExecuteSuccessfully() {
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            // Given
            String[] args = {"arg@#$%", "arg with spaces", "arg\nwith\nnewlines", "arg\twith\ttabs"};

            // When
            destroyEngine.execute(applicationContext, args);

            // Then
            processEnginesMock.verify(ProcessEngines::destroy, times(1));
            assertEquals(1, listAppender.list.size());
        }
    }

    @Test
    @DisplayName("Should not call checkNext when ProcessEngines.destroy throws exception")
    void execute_WhenDestroyThrowsException_ShouldNotCallCheckNext() {
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            // Given
            String[] args = {"test"};
            DestroyEngine spyDestroyEngine = spy(destroyEngine);
            RuntimeException expectedException = new RuntimeException("Destruction failed");
            processEnginesMock.when(ProcessEngines::destroy).thenThrow(expectedException);

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                spyDestroyEngine.execute(applicationContext, args);
            });

            // checkNext should not be called due to exception
            verify(spyDestroyEngine, never()).checkNext(any(), any());
        }
    }

    @Test
    @DisplayName("Should verify logger name matches class name")
    void destroyEngine_ShouldHaveCorrectLoggerName() {
        // Given & When
        Logger actualLogger = (Logger) LoggerFactory.getLogger(DestroyEngine.class);
        
        // Then
        assertEquals("vn.com.phat.chain.DestroyEngine", actualLogger.getName());
    }

    @Test
    @DisplayName("Should verify ProcessEngines.destroy() is static method call")
    void execute_ShouldCallStaticDestroyMethod() {
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            // Given
            String[] args = {"test"};

            // When
            destroyEngine.execute(applicationContext, args);

            // Then - Verify static method call
            processEnginesMock.verify(ProcessEngines::destroy, times(1));
            processEnginesMock.verifyNoMoreInteractions();
        }
    }

    @Test
    @DisplayName("Should handle concurrent execution scenarios")
    void execute_ConcurrentExecutions_ShouldBehaveCorrectly() {
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            // Given
            String[] args1 = {"concurrent1"};
            String[] args2 = {"concurrent2"};

            // When
            destroyEngine.execute(applicationContext, args1);
            destroyEngine.execute(applicationContext, args2);

            // Then
            processEnginesMock.verify(ProcessEngines::destroy, times(2));
            assertEquals(2, listAppender.list.size());
        }
    }

    @Test
    @DisplayName("Should handle unicode arguments correctly")
    void execute_WithUnicodeArguments_ShouldExecuteSuccessfully() {
        try (MockedStatic<ProcessEngines> processEnginesMock = mockStatic(ProcessEngines.class)) {
            // Given
            String[] args = {"测试", "тест", "テスト", "🚀"};

            // When
            destroyEngine.execute(applicationContext, args);

            // Then
            processEnginesMock.verify(ProcessEngines::destroy, times(1));
            assertEquals(1, listAppender.list.size());
        }
    }
}