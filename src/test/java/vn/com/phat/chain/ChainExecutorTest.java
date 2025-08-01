package vn.com.phat.chain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ChainExecutor Tests")
class ChainExecutorTest {

    @Mock
    private ConfigurableApplicationContext mockApplicationContext;

    private TestChainExecutor executor1;
    private TestChainExecutor executor2;
    private TestChainExecutor executor3;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        executor1 = new TestChainExecutor("executor1");
        executor2 = new TestChainExecutor("executor2");
        executor3 = new TestChainExecutor("executor3");
        TestChainExecutor.resetOrderCounter(); // Reset static counter for each test
    }

    @Nested
    @DisplayName("link() method tests")
    class LinkMethodTests {

        @Test
        @DisplayName("Should return null when first executor is null")
        void shouldReturnNullWhenFirstExecutorIsNull() {
            // Given
            ChainExecutor first = null;
            
            // When
            ChainExecutor result = ChainExecutor.link(first, executor2, executor3);
            
            // Then
            assertNull(result, "Chain should return null when first executor is null");
        }

        @Test
        @DisplayName("Should return first executor when no additional executors provided")
        void shouldReturnFirstExecutorWhenNoAdditionalExecutors() {
            // Given & When
            ChainExecutor result = ChainExecutor.link(executor1);
            
            // Then
            assertSame(executor1, result, "Should return the same first executor instance");
        }

        @Test
        @DisplayName("Should return first executor when executors array is null")
        void shouldReturnFirstExecutorWhenExecutorsArrayIsNull() {
            // Given
            ChainExecutor[] executors = null;
            
            // When
            ChainExecutor result = ChainExecutor.link(executor1, executors);
            
            // Then
            assertSame(executor1, result, "Should return first executor when array is null");
        }

        @Test
        @DisplayName("Should link two executors correctly")
        void shouldLinkTwoExecutorsCorrectly() {
            // Given & When
            ChainExecutor result = ChainExecutor.link(executor1, executor2);
            
            // Then
            assertSame(executor1, result, "Should return first executor as chain head");
            
            // Verify chain execution
            result.execute(mockApplicationContext, "test");
            assertTrue(executor1.wasExecuted(), "First executor should be executed");
            assertTrue(executor2.wasExecuted(), "Second executor should be executed");
        }

        @Test
        @DisplayName("Should link multiple executors correctly")
        void shouldLinkMultipleExecutorsCorrectly() {
            // Given & When
            ChainExecutor result = ChainExecutor.link(executor1, executor2, executor3);
            
            // Then
            assertSame(executor1, result, "Should return first executor as chain head");
            
            // Verify chain execution order
            result.execute(mockApplicationContext, "test");
            assertTrue(executor1.wasExecuted(), "First executor should be executed");
            assertTrue(executor2.wasExecuted(), "Second executor should be executed");
            assertTrue(executor3.wasExecuted(), "Third executor should be executed");
            
            // Verify execution order
            assertTrue(executor1.getExecutionOrder() < executor2.getExecutionOrder(),
                "Executor1 should execute before executor2");
            assertTrue(executor2.getExecutionOrder() < executor3.getExecutionOrder(),
                "Executor2 should execute before executor3");
        }

        @Test
        @DisplayName("Should skip null executors in chain")
        void shouldSkipNullExecutorsInChain() {
            // Given & When
            ChainExecutor result = ChainExecutor.link(executor1, null, executor2, null, executor3);
            
            // Then
            assertSame(executor1, result, "Should return first executor as chain head");
            result.execute(mockApplicationContext, "test");
            assertTrue(executor1.wasExecuted(), "First executor should be executed");
            assertTrue(executor2.wasExecuted(), "Second executor should be executed");
            assertTrue(executor3.wasExecuted(), "Third executor should be executed");
        }

        @Test
        @DisplayName("Should handle duplicate executors by maintaining uniqueness")
        void shouldHandleDuplicateExecutorsByMaintainingUniqueness() {
            // Given & When
            ChainExecutor result = ChainExecutor.link(executor1, executor2, executor1, executor3, executor2);
            
            // Then
            assertSame(executor1, result, "Should return first executor as chain head");
            result.execute(mockApplicationContext, "test");
            
            // Each executor should be executed only once despite duplicates
            assertEquals(1, executor1.getExecutionCount(), "Executor1 should execute only once");
            assertEquals(1, executor2.getExecutionCount(), "Executor2 should execute only once");
            assertEquals(1, executor3.getExecutionCount(), "Executor3 should execute only once");
        }

        @Test
        @DisplayName("Should handle empty executors array")
        void shouldHandleEmptyExecutorsArray() {
            // Given
            ChainExecutor[] emptyExecutors = new ChainExecutor[0];
            
            // When
            ChainExecutor result = ChainExecutor.link(executor1, emptyExecutors);
            
            // Then
            assertSame(executor1, result, "Should return first executor when array is empty");
            result.execute(mockApplicationContext, "test");
            assertTrue(executor1.wasExecuted(), "First executor should be executed");
        }

        @Test
        @DisplayName("Should handle array with all null executors")
        void shouldHandleArrayWithAllNullExecutors() {
            // Given
            ChainExecutor[] nullExecutors = {null, null, null};
            
            // When
            ChainExecutor result = ChainExecutor.link(executor1, nullExecutors);
            
            // Then
            assertSame(executor1, result, "Should return first executor when all others are null");
            result.execute(mockApplicationContext, "test");
            assertTrue(executor1.wasExecuted(), "First executor should be executed");
        }

        @Test
        @DisplayName("Should handle single executor with null varargs")
        void shouldHandleSingleExecutorWithNullVarargs() {
            // Given & When
            ChainExecutor result = ChainExecutor.link(executor1, (ChainExecutor[]) null);
            
            // Then
            assertSame(executor1, result, "Should return first executor when varargs is null");
            result.execute(mockApplicationContext, "test");
            assertTrue(executor1.wasExecuted(), "First executor should be executed");
        }
    }

    @Nested
    @DisplayName("checkNext() method tests")
    class CheckNextMethodTests {

        @Test
        @DisplayName("Should not execute next when next is null")
        void shouldNotExecuteNextWhenNextIsNull() {
            // Given
            TestChainExecutor singleExecutor = new TestChainExecutor("single");
            
            // When
            singleExecutor.execute(mockApplicationContext, "test");
            
            // Then
            assertTrue(singleExecutor.wasExecuted(), "Single executor should be executed");
        }

        @Test
        @DisplayName("Should execute next executor when next is not null")
        void shouldExecuteNextExecutorWhenNextIsNotNull() {
            // Given
            ChainExecutor chain = ChainExecutor.link(executor1, executor2);
            
            // When
            chain.execute(mockApplicationContext, "test");
            
            // Then
            assertTrue(executor1.wasExecuted(), "First executor should be executed");
            assertTrue(executor2.wasExecuted(), "Second executor should be executed");
        }

        @Test
        @DisplayName("Should pass applicationContext to next executor")
        void shouldPassApplicationContextToNextExecutor() {
            // Given
            ChainExecutor chain = ChainExecutor.link(executor1, executor2);
            
            // When
            chain.execute(mockApplicationContext, "test");
            
            // Then
            assertSame(mockApplicationContext, executor1.getReceivedContext(),
                "First executor should receive correct context");
            assertSame(mockApplicationContext, executor2.getReceivedContext(),
                "Second executor should receive correct context");
        }

        @Test
        @DisplayName("Should pass arguments to next executor")
        void shouldPassArgumentsToNextExecutor() {
            // Given
            ChainExecutor chain = ChainExecutor.link(executor1, executor2);
            String[] args = {"arg1", "arg2", "arg3"};
            
            // When
            chain.execute(mockApplicationContext, args);
            
            // Then
            assertArrayEquals(args, executor1.getReceivedArgs(),
                "First executor should receive correct arguments");
            assertArrayEquals(args, executor2.getReceivedArgs(),
                "Second executor should receive correct arguments");
        }

        @Test
        @DisplayName("Should handle null arguments")
        void shouldHandleNullArguments() {
            // Given
            ChainExecutor chain = ChainExecutor.link(executor1, executor2);
            
            // When
            chain.execute(mockApplicationContext, (String[]) null);
            
            // Then
            assertTrue(executor1.wasExecuted(), "First executor should be executed");
            assertTrue(executor2.wasExecuted(), "Second executor should be executed");
            assertNull(executor1.getReceivedArgs(), "First executor should receive null args");
            assertNull(executor2.getReceivedArgs(), "Second executor should receive null args");
        }

        @Test
        @DisplayName("Should handle empty arguments array")
        void shouldHandleEmptyArgumentsArray() {
            // Given
            ChainExecutor chain = ChainExecutor.link(executor1, executor2);
            String[] emptyArgs = new String[0];
            
            // When
            chain.execute(mockApplicationContext, emptyArgs);
            
            // Then
            assertTrue(executor1.wasExecuted(), "First executor should be executed");
            assertTrue(executor2.wasExecuted(), "Second executor should be executed");
            assertArrayEquals(emptyArgs, executor1.getReceivedArgs(),
                "First executor should receive empty args");
            assertArrayEquals(emptyArgs, executor2.getReceivedArgs(),
                "Second executor should receive empty args");
        }

        @Test
        @DisplayName("Should handle null application context")
        void shouldHandleNullApplicationContext() {
            // Given
            ChainExecutor chain = ChainExecutor.link(executor1, executor2);
            
            // When
            chain.execute(null, "test");
            
            // Then
            assertTrue(executor1.wasExecuted(), "First executor should be executed");
            assertTrue(executor2.wasExecuted(), "Second executor should be executed");
            assertNull(executor1.getReceivedContext(), "First executor should receive null context");
            assertNull(executor2.getReceivedContext(), "Second executor should receive null context");
        }
    }

    @Nested
    @DisplayName("Integration and Edge Case Tests")
    class IntegrationAndEdgeCaseTests {

        @Test
        @DisplayName("Should handle long chain execution")
        void shouldHandleLongChainExecution() {
            // Given
            TestChainExecutor[] executors = new TestChainExecutor[50];
            for (int i = 0; i < executors.length; i++) {
                executors[i] = new TestChainExecutor("executor" + i);
            }
            
            // When
            ChainExecutor chain = ChainExecutor.link(executors[0], 
                Arrays.copyOfRange(executors, 1, executors.length));
            chain.execute(mockApplicationContext, "test");
            
            // Then
            for (int i = 0; i < executors.length; i++) {
                assertTrue(executors[i].wasExecuted(), 
                    "Executor " + i + " should have been executed");
            }
            
            // Verify execution order
            for (int i = 1; i < executors.length; i++) {
                assertTrue(executors[i-1].getExecutionOrder() < executors[i].getExecutionOrder(),
                    "Executor " + (i-1) + " should execute before executor " + i);
            }
        }

        @Test
        @DisplayName("Should handle exception in executor gracefully")
        void shouldHandleExceptionInExecutorGracefully() {
            // Given
            TestChainExecutor throwingExecutor = new TestChainExecutor("throwing") {
                @Override
                public void execute(ConfigurableApplicationContext applicationContext, String... args) {
                    super.execute(applicationContext, args);
                    throw new RuntimeException("Test exception");
                }
            };
            
            // When & Then
            ChainExecutor chain = ChainExecutor.link(throwingExecutor, executor2);
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                chain.execute(mockApplicationContext, "test");
            });
            
            assertEquals("Test exception", exception.getMessage(), "Should propagate exception message");
            assertTrue(throwingExecutor.wasExecuted(), "Throwing executor should be executed");
            assertFalse(executor2.wasExecuted(), "Next executor should not be executed due to exception");
        }

        @Test
        @DisplayName("Should handle circular reference prevention")
        void shouldHandleCircularReferencePrevention() {
            // Given - trying to create a circular reference
            ChainExecutor chain1 = ChainExecutor.link(executor1, executor2);
            ChainExecutor chain2 = ChainExecutor.link(executor2, executor1);
            
            // When
            chain1.execute(mockApplicationContext, "test");
            
            // Then
            assertTrue(executor1.wasExecuted(), "Executor1 should be executed");
            assertTrue(executor2.wasExecuted(), "Executor2 should be executed");
            assertEquals(1, executor1.getExecutionCount(), "Executor1 should execute only once");
            assertEquals(1, executor2.getExecutionCount(), "Executor2 should execute only once");
        }

        @Test
        @DisplayName("Should maintain thread safety for execution")
        void shouldMaintainThreadSafetyForExecution() throws InterruptedException {
            // Given
            TestChainExecutor threadSafeExecutor = new TestChainExecutor("threadSafe");
            ChainExecutor chain = ChainExecutor.link(threadSafeExecutor);
            
            // When - execute from multiple threads
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            for (int i = 0; i < 10; i++) {
                executorService.submit(() -> chain.execute(mockApplicationContext, "test"));
            }
            
            executorService.shutdown();
            assertTrue(executorService.awaitTermination(5, TimeUnit.SECONDS),
                "All threads should complete within timeout");
            
            // Then
            assertEquals(10, threadSafeExecutor.getExecutionCount(),
                "Thread safe executor should be executed 10 times");
        }

        @Test
        @DisplayName("Should handle mixed valid and invalid executors")
        void shouldHandleMixedValidAndInvalidExecutors() {
            // Given
            TestChainExecutor validExecutor1 = new TestChainExecutor("valid1");
            TestChainExecutor validExecutor2 = new TestChainExecutor("valid2");
            
            // When
            ChainExecutor chain = ChainExecutor.link(validExecutor1, null, validExecutor2, null);
            chain.execute(mockApplicationContext, "test");
            
            // Then
            assertTrue(validExecutor1.wasExecuted(), "Valid executor 1 should be executed");
            assertTrue(validExecutor2.wasExecuted(), "Valid executor 2 should be executed");
        }

        @Test
        @DisplayName("Should handle complex argument scenarios")
        void shouldHandleComplexArgumentScenarios() {
            // Given
            ChainExecutor chain = ChainExecutor.link(executor1, executor2);
            String[] complexArgs = {"--config", "prod", "--verbose", "", null, "final-arg"};
            
            // When
            chain.execute(mockApplicationContext, complexArgs);
            
            // Then
            assertTrue(executor1.wasExecuted(), "First executor should be executed");
            assertTrue(executor2.wasExecuted(), "Second executor should be executed");
            assertArrayEquals(complexArgs, executor1.getReceivedArgs(),
                "First executor should receive complex args correctly");
            assertArrayEquals(complexArgs, executor2.getReceivedArgs(),
                "Second executor should receive complex args correctly");
        }
    }

    @Nested
    @DisplayName("Boundary Value Tests")
    class BoundaryValueTests {

        @Test
        @DisplayName("Should handle maximum realistic chain length")
        void shouldHandleMaximumRealisticChainLength() {
            // Given
            List<TestChainExecutor> executors = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                executors.add(new TestChainExecutor("executor" + i));
            }
            
            // When
            ChainExecutor chain = ChainExecutor.link(executors.get(0), 
                executors.subList(1, executors.size()).toArray(new ChainExecutor[0]));
            
            // Then
            assertNotNull(chain, "Chain should be created successfully");
            chain.execute(mockApplicationContext, "test");
            
            // Verify all executors were executed
            for (TestChainExecutor executor : executors) {
                assertTrue(executor.wasExecuted(), 
                    "Executor " + executor.getName() + " should be executed");
            }
        }

        @Test
        @DisplayName("Should handle single executor chain")
        void shouldHandleSingleExecutorChain() {
            // Given & When
            ChainExecutor chain = ChainExecutor.link(executor1);
            
            // Then
            assertSame(executor1, chain, "Should return the single executor");
            chain.execute(mockApplicationContext, "test");
            assertTrue(executor1.wasExecuted(), "Single executor should be executed");
        }
    }

    @Nested
    @DisplayName("State Management Tests") 
    class StateManagementTests {

        @Test
        @DisplayName("Should maintain proper state after multiple executions")
        void shouldMaintainProperStateAfterMultipleExecutions() {
            // Given
            ChainExecutor chain = ChainExecutor.link(executor1, executor2);
            
            // When
            chain.execute(mockApplicationContext, "first");
            chain.execute(mockApplicationContext, "second");
            chain.execute(mockApplicationContext, "third");
            
            // Then
            assertEquals(3, executor1.getExecutionCount(), "First executor should be called 3 times");
            assertEquals(3, executor2.getExecutionCount(), "Second executor should be called 3 times");
            assertArrayEquals(new String[]{"third"}, executor1.getReceivedArgs(),
                "Should retain last execution args");
            assertArrayEquals(new String[]{"third"}, executor2.getReceivedArgs(),
                "Should retain last execution args");
        }

        @Test
        @DisplayName("Should handle state consistency across chain modifications")
        void shouldHandleStateConsistencyAcrossChainModifications() {
            // Given
            TestChainExecutor additionalExecutor = new TestChainExecutor("additional");
            ChainExecutor chain1 = ChainExecutor.link(executor1, executor2);
            ChainExecutor chain2 = ChainExecutor.link(executor1, additionalExecutor, executor2);
            
            // When
            chain1.execute(mockApplicationContext, "chain1");
            chain2.execute(mockApplicationContext, "chain2");
            
            // Then
            assertEquals(2, executor1.getExecutionCount(), "Executor1 should be called twice");
            assertEquals(2, executor2.getExecutionCount(), "Executor2 should be called twice");
            assertEquals(1, additionalExecutor.getExecutionCount(), "Additional executor should be called once");
        }
    }

    @Nested
    @DisplayName("Performance and Resource Tests")
    class PerformanceAndResourceTests {

        @Test
        @DisplayName("Should handle rapid successive executions")
        void shouldHandleRapidSuccessiveExecutions() {
            // Given
            ChainExecutor chain = ChainExecutor.link(executor1, executor2);
            
            // When
            for (int i = 0; i < 1000; i++) {
                chain.execute(mockApplicationContext, "iteration" + i);
            }
            
            // Then
            assertEquals(1000, executor1.getExecutionCount(), "First executor should be called 1000 times");
            assertEquals(1000, executor2.getExecutionCount(), "Second executor should be called 1000 times");
        }

        @Test
        @DisplayName("Should handle memory efficient chain creation")
        void shouldHandleMemoryEfficientChainCreation() {
            // Given
            List<TestChainExecutor> largeExecutorList = new ArrayList<>();
            for (int i = 0; i < 10000; i++) {
                largeExecutorList.add(new TestChainExecutor("exec" + i));
            }
            
            // When
            ChainExecutor chain = ChainExecutor.link(largeExecutorList.get(0),
                largeExecutorList.subList(1, 100).toArray(new ChainExecutor[0])); // Test first 100
            
            // Then
            assertNotNull(chain, "Chain should be created without memory issues");
            chain.execute(mockApplicationContext, "test");
            
            // Verify first few executors
            for (int i = 0; i < 100; i++) {
                assertTrue(largeExecutorList.get(i).wasExecuted(),
                    "Executor " + i + " should be executed");
            }
        }
    }

    // Test implementation of abstract ChainExecutor
    private static class TestChainExecutor extends ChainExecutor {
        private final String name;
        private volatile boolean executed = false;
        private ConfigurableApplicationContext receivedContext;
        private String[] receivedArgs;
        private long executionOrder;
        private static volatile long orderCounter = 0;
        private volatile int executionCount = 0;

        public TestChainExecutor(String name) {
            this.name = name;
        }

        @Override
        public synchronized void execute(ConfigurableApplicationContext applicationContext, String... args) {
            this.executed = true;
            this.receivedContext = applicationContext;
            this.receivedArgs = args;
            this.executionOrder = ++orderCounter;
            this.executionCount++;
            checkNext(applicationContext, args);
        }

        public boolean wasExecuted() {
            return executed;
        }

        public ConfigurableApplicationContext getReceivedContext() {
            return receivedContext;
        }

        public String[] getReceivedArgs() {
            return receivedArgs;
        }

        public long getExecutionOrder() {
            return executionOrder;
        }

        public String getName() {
            return name;
        }

        public int getExecutionCount() {
            return executionCount;
        }

        public static void resetOrderCounter() {
            orderCounter = 0;
        }
    }
}