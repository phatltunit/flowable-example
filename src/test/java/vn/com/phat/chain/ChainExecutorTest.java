package vn.com.phat.chain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ChainExecutor class.
 * Testing Framework: JUnit 5 with Mockito for mocking
 * 
 * Tests cover:
 * - Chain linking functionality with various scenarios
 * - Chain execution flow and order
 * - Edge cases and error conditions
 * - Null handling and parameter passing
 * - Duplicate executor handling
 * - Protected method checkNext() behavior
 */
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
    }

    @Nested
    @DisplayName("Link Method Tests")
    class LinkMethodTests {

        @Test
        @DisplayName("Should return null when first executor is null")
        void shouldReturnNullWhenFirstExecutorIsNull() {
            ChainExecutor result = ChainExecutor.link(null, executor1, executor2);
            
            assertNull(result, "Link should return null when first executor is null");
        }

        @Test
        @DisplayName("Should return first executor when no additional executors provided")
        void shouldReturnFirstExecutorWhenNoAdditionalExecutors() {
            ChainExecutor result = ChainExecutor.link(executor1);
            
            assertSame(executor1, result, "Should return the first executor");
            assertNull(executor1.getNext(), "First executor should have no next executor");
        }

        @Test
        @DisplayName("Should return first executor when executors array is null")
        void shouldReturnFirstExecutorWhenExecutorsArrayIsNull() {
            ChainExecutor result = ChainExecutor.link(executor1, (ChainExecutor[]) null);
            
            assertSame(executor1, result, "Should return the first executor");
            assertNull(executor1.getNext(), "First executor should have no next executor when array is null");
        }

        @Test
        @DisplayName("Should link two executors correctly")
        void shouldLinkTwoExecutorsCorrectly() {
            ChainExecutor result = ChainExecutor.link(executor1, executor2);
            
            assertSame(executor1, result, "Should return the first executor");
            assertSame(executor2, executor1.getNext(), "First executor should link to second");
            assertNull(executor2.getNext(), "Second executor should have no next");
        }

        @Test
        @DisplayName("Should link multiple executors in correct order")
        void shouldLinkMultipleExecutorsInCorrectOrder() {
            ChainExecutor result = ChainExecutor.link(executor1, executor2, executor3);
            
            assertSame(executor1, result, "Should return the first executor");
            assertSame(executor2, executor1.getNext(), "First should link to second");
            assertSame(executor3, executor2.getNext(), "Second should link to third");
            assertNull(executor3.getNext(), "Third should have no next");
        }

        @Test
        @DisplayName("Should skip null executors in chain")
        void shouldSkipNullExecutorsInChain() {
            ChainExecutor result = ChainExecutor.link(executor1, null, executor2, null, executor3);
            
            assertSame(executor1, result, "Should return the first executor");
            assertSame(executor2, executor1.getNext(), "Should skip null and link to executor2");
            assertSame(executor3, executor2.getNext(), "Should skip null and link to executor3");
            assertNull(executor3.getNext(), "Last executor should have no next");
        }

        @Test
        @DisplayName("Should handle duplicate executors by ignoring duplicates using HashSet")
        void shouldHandleDuplicateExecutorsByIgnoringDuplicates() {
            ChainExecutor result = ChainExecutor.link(executor1, executor2, executor1, executor3, executor2);
            
            assertSame(executor1, result, "Should return the first executor");
            assertSame(executor2, executor1.getNext(), "Should link to executor2");
            assertSame(executor3, executor2.getNext(), "Should link to executor3, skipping duplicates");
            assertNull(executor3.getNext(), "Last unique executor should have no next");
        }

        @Test
        @DisplayName("Should handle chain with only null executors after first")
        void shouldHandleChainWithOnlyNullExecutorsAfterFirst() {
            ChainExecutor result = ChainExecutor.link(executor1, null, null, null);
            
            assertSame(executor1, result, "Should return the first executor");
            assertNull(executor1.getNext(), "Should have no next when all others are null");
        }

        @Test
        @DisplayName("Should handle empty executors array")
        void shouldHandleEmptyExecutorsArray() {
            ChainExecutor result = ChainExecutor.link(executor1, new ChainExecutor[0]);
            
            assertSame(executor1, result, "Should return the first executor");
            assertNull(executor1.getNext(), "Should have no next when array is empty");
        }

        @Test
        @DisplayName("Should handle single duplicate executor")
        void shouldHandleSingleDuplicateExecutor() {
            ChainExecutor result = ChainExecutor.link(executor1, executor1);
            
            assertSame(executor1, result, "Should return the first executor");
            assertNull(executor1.getNext(), "Should have no next when duplicate is ignored");
        }

        @Test
        @DisplayName("Should maintain referential integrity in complex scenarios")
        void shouldMaintainReferentialIntegrityInComplexScenarios() {
            TestChainExecutor executor4 = new TestChainExecutor("executor4");
            TestChainExecutor executor5 = new TestChainExecutor("executor5");
            
            ChainExecutor result = ChainExecutor.link(executor1, null, executor2, executor1, 
                                                     executor3, null, executor4, executor2, executor5);
            
            // Verify the chain: executor1 -> executor2 -> executor3 -> executor4 -> executor5
            assertSame(executor1, result);
            assertSame(executor2, executor1.getNext());
            assertSame(executor3, executor2.getNext());
            assertSame(executor4, executor3.getNext());
            assertSame(executor5, executor4.getNext());
            assertNull(executor5.getNext());
        }

        @Test
        @DisplayName("Should correctly use HashSet uniqueExecutors to track duplicates")
        void shouldCorrectlyUseHashSetToTrackDuplicates() {
            // Test the exact logic in the link method - using HashSet.add() return value
            TestChainExecutor duplicateExecutor = new TestChainExecutor("duplicate");
            
            ChainExecutor result = ChainExecutor.link(executor1, executor2, duplicateExecutor, 
                                                     executor3, duplicateExecutor, executor2);
            
            // Should be: executor1 -> executor2 -> duplicateExecutor -> executor3
            assertSame(executor1, result);
            assertSame(executor2, executor1.getNext());
            assertSame(duplicateExecutor, executor2.getNext());
            assertSame(executor3, duplicateExecutor.getNext());
            assertNull(executor3.getNext());
        }
    }

    @Nested
    @DisplayName("CheckNext Method Tests")
    class CheckNextMethodTests {

        @Test
        @DisplayName("Should not execute when next is null")
        void shouldNotExecuteWhenNextIsNull() {
            executor1.testCheckNext(mockApplicationContext, "arg1", "arg2");
            
            // Since next is null, no execution should occur
            assertEquals(0, executor1.getExecutionCount(), "Executor should not execute itself in checkNext");
        }

        @Test
        @DisplayName("Should execute next executor when next is not null")
        void shouldExecuteNextExecutorWhenNextIsNotNull() {
            ChainExecutor.link(executor1, executor2);
            
            executor1.testCheckNext(mockApplicationContext, "arg1", "arg2");
            
            assertEquals(1, executor2.getExecutionCount(), "Next executor should be executed once");
            assertArrayEquals(new String[]{"arg1", "arg2"}, executor2.getLastArgs(), 
                             "Arguments should be passed correctly");
            assertSame(mockApplicationContext, executor2.getLastApplicationContext(), 
                      "Application context should be passed correctly");
        }

        @Test
        @DisplayName("Should pass null application context to next executor")
        void shouldPassNullApplicationContextToNextExecutor() {
            ChainExecutor.link(executor1, executor2);
            
            executor1.testCheckNext(null, "arg1");
            
            assertEquals(1, executor2.getExecutionCount(), "Next executor should be executed");
            assertNull(executor2.getLastApplicationContext(), "Null context should be passed through");
            assertArrayEquals(new String[]{"arg1"}, executor2.getLastArgs(), "Arguments should still be passed");
        }

        @Test
        @DisplayName("Should pass empty args to next executor")
        void shouldPassEmptyArgsToNextExecutor() {
            ChainExecutor.link(executor1, executor2);
            
            executor1.testCheckNext(mockApplicationContext);
            
            assertEquals(1, executor2.getExecutionCount(), "Next executor should be executed");
            assertEquals(0, executor2.getLastArgs().length, "Empty args should be passed");
            assertSame(mockApplicationContext, executor2.getLastApplicationContext(), 
                      "Context should still be passed");
        }

        @Test
        @DisplayName("Should handle varargs correctly")
        void shouldHandleVarargsCorrectly() {
            ChainExecutor.link(executor1, executor2);
            String[] args = {"arg1", "arg2", "arg3", "arg4"};
            
            executor1.testCheckNext(mockApplicationContext, args);
            
            assertEquals(1, executor2.getExecutionCount(), "Next executor should be executed");
            assertArrayEquals(args, executor2.getLastArgs(), "All varargs should be passed correctly");
        }

        @Test
        @DisplayName("Should handle null args array gracefully")
        void shouldHandleNullArgsArrayGracefully() {
            ChainExecutor.link(executor1, executor2);
            
            executor1.testCheckNext(mockApplicationContext, (String[]) null);
            
            assertEquals(1, executor2.getExecutionCount(), "Next executor should be executed");
            assertNotNull(executor2.getLastArgs(), "Args array should not be null");
            assertEquals(0, executor2.getLastArgs().length, "Args array should be empty when null passed");
        }

        @Test
        @DisplayName("Should propagate calls through entire chain via checkNext")
        void shouldPropagatCallsThroughEntireChainViaCheckNext() {
            ChainExecutor.link(executor1, executor2, executor3);
            
            executor1.testCheckNext(mockApplicationContext, "propagate");
            
            assertEquals(0, executor1.getExecutionCount(), "First executor should not execute itself");
            assertEquals(1, executor2.getExecutionCount(), "Second executor should execute");
            assertEquals(1, executor3.getExecutionCount(), "Third executor should execute");
            
            assertArrayEquals(new String[]{"propagate"}, executor2.getLastArgs());
            assertArrayEquals(new String[]{"propagate"}, executor3.getLastArgs());
        }
    }

    @Nested
    @DisplayName("Chain Execution Integration Tests")
    class ChainExecutionIntegrationTests {

        @Test
        @DisplayName("Should execute entire chain in correct order")
        void shouldExecuteEntireChainInCorrectOrder() {
            ChainExecutor chain = ChainExecutor.link(executor1, executor2, executor3);
            
            chain.execute(mockApplicationContext, "test");
            
            assertEquals(1, executor1.getExecutionCount(), "First executor should execute once");
            assertEquals(1, executor2.getExecutionCount(), "Second executor should execute once");
            assertEquals(1, executor3.getExecutionCount(), "Third executor should execute once");
            
            // Verify execution order by checking timestamps
            assertTrue(executor1.getLastExecutionTime() <= executor2.getLastExecutionTime(),
                      "First executor should execute before or at same time as second");
            assertTrue(executor2.getLastExecutionTime() <= executor3.getLastExecutionTime(),
                      "Second executor should execute before or at same time as third");
        }

        @Test
        @DisplayName("Should handle chain execution with null context")
        void shouldHandleChainExecutionWithNullContext() {
            ChainExecutor chain = ChainExecutor.link(executor1, executor2);
            
            assertDoesNotThrow(() -> chain.execute(null, "test"), 
                              "Chain should handle null context gracefully");
            
            assertEquals(1, executor1.getExecutionCount(), "First executor should execute");
            assertEquals(1, executor2.getExecutionCount(), "Second executor should execute");
            assertNull(executor1.getLastApplicationContext(), "Context should be null for first executor");
            assertNull(executor2.getLastApplicationContext(), "Context should be null for second executor");
        }

        @Test
        @DisplayName("Should handle chain execution with no arguments")
        void shouldHandleChainExecutionWithNoArguments() {
            ChainExecutor chain = ChainExecutor.link(executor1, executor2);
            
            assertDoesNotThrow(() -> chain.execute(mockApplicationContext), 
                              "Chain should handle no arguments gracefully");
            
            assertEquals(1, executor1.getExecutionCount(), "First executor should execute");
            assertEquals(1, executor2.getExecutionCount(), "Second executor should execute");
            assertEquals(0, executor1.getLastArgs().length, "First executor should receive empty args");
            assertEquals(0, executor2.getLastArgs().length, "Second executor should receive empty args");
        }

        @Test
        @DisplayName("Should handle single executor chain")
        void shouldHandleSingleExecutorChain() {
            ChainExecutor chain = ChainExecutor.link(executor1);
            
            chain.execute(mockApplicationContext, "solo");
            
            assertEquals(1, executor1.getExecutionCount(), "Single executor should execute once");
            assertArrayEquals(new String[]{"solo"}, executor1.getLastArgs(), "Arguments should be passed correctly");
            assertSame(mockApplicationContext, executor1.getLastApplicationContext(), "Context should be passed correctly");
        }

        @Test
        @DisplayName("Should preserve arguments throughout chain")
        void shouldPreserveArgumentsThroughoutChain() {
            ChainExecutor chain = ChainExecutor.link(executor1, executor2, executor3);
            String[] originalArgs = {"arg1", "arg2", "arg3"};
            
            chain.execute(mockApplicationContext, originalArgs);
            
            assertArrayEquals(originalArgs, executor1.getLastArgs(), "First executor should receive original args");
            assertArrayEquals(originalArgs, executor2.getLastArgs(), "Second executor should receive original args");
            assertArrayEquals(originalArgs, executor3.getLastArgs(), "Third executor should receive original args");
        }

        @Test
        @DisplayName("Should preserve application context throughout chain")
        void shouldPreserveApplicationContextThroughoutChain() {
            ChainExecutor chain = ChainExecutor.link(executor1, executor2, executor3);
            
            chain.execute(mockApplicationContext, "test");
            
            assertSame(mockApplicationContext, executor1.getLastApplicationContext(), 
                      "First executor should receive original context");
            assertSame(mockApplicationContext, executor2.getLastApplicationContext(), 
                      "Second executor should receive original context");
            assertSame(mockApplicationContext, executor3.getLastApplicationContext(), 
                      "Third executor should receive original context");
        }

        @Test
        @DisplayName("Should handle multiple executions of same chain")
        void shouldHandleMultipleExecutionsOfSameChain() {
            ChainExecutor chain = ChainExecutor.link(executor1, executor2);
            
            chain.execute(mockApplicationContext, "first");
            chain.execute(mockApplicationContext, "second");
            chain.execute(mockApplicationContext, "third");
            
            assertEquals(3, executor1.getExecutionCount(), "First executor should execute 3 times");
            assertEquals(3, executor2.getExecutionCount(), "Second executor should execute 3 times");
            assertArrayEquals(new String[]{"third"}, executor1.getLastArgs(), "Should preserve last execution args");
            assertArrayEquals(new String[]{"third"}, executor2.getLastArgs(), "Should preserve last execution args");
        }

        @Test
        @DisplayName("Should handle execution with both null context and null args")
        void shouldHandleExecutionWithBothNullContextAndNullArgs() {
            ChainExecutor chain = ChainExecutor.link(executor1, executor2);
            
            assertDoesNotThrow(() -> chain.execute(null, (String[]) null), 
                              "Chain should handle both null context and args");
            
            assertEquals(1, executor1.getExecutionCount(), "First executor should execute");
            assertEquals(1, executor2.getExecutionCount(), "Second executor should execute");
            assertNull(executor1.getLastApplicationContext(), "Context should be null");
            assertNull(executor2.getLastApplicationContext(), "Context should be null");
            assertEquals(0, executor1.getLastArgs().length, "Args should be empty array, not null");
            assertEquals(0, executor2.getLastArgs().length, "Args should be empty array, not null");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("Should handle large chain without stack overflow")
        void shouldHandleLargeChainWithoutStackOverflow() {
            // Create a chain of 50 executors (reasonable size for testing)
            TestChainExecutor[] executors = new TestChainExecutor[50];
            for (int i = 0; i < 50; i++) {
                executors[i] = new TestChainExecutor("executor" + i);
            }
            
            ChainExecutor chain = ChainExecutor.link(executors[0], 
                java.util.Arrays.copyOfRange(executors, 1, executors.length));
            
            assertDoesNotThrow(() -> chain.execute(mockApplicationContext, "stress-test"), 
                              "Large chain should not cause stack overflow");
            
            // Verify all executors were called
            for (int i = 0; i < executors.length; i++) {
                assertEquals(1, executors[i].getExecutionCount(), 
                           "Executor " + i + " should be executed exactly once");
            }
        }

        @Test
        @DisplayName("Should handle executor that throws exception")
        void shouldHandleExecutorThatThrowsException() {
            TestChainExecutor throwingExecutor = new TestChainExecutor("throwing") {
                @Override
                public void execute(ConfigurableApplicationContext applicationContext, String... args) {
                    super.execute(applicationContext, args);
                    throw new RuntimeException("Test exception");
                }
            };
            
            ChainExecutor chain = ChainExecutor.link(executor1, throwingExecutor, executor2);
            
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> chain.execute(mockApplicationContext, "test"), 
                "Chain should propagate exceptions");
            
            assertEquals("Test exception", exception.getMessage(), "Exception message should be preserved");
            assertEquals(1, executor1.getExecutionCount(), "First executor should execute");
            assertEquals(1, throwingExecutor.getExecutionCount(), "Throwing executor should execute");
            assertEquals(0, executor2.getExecutionCount(), "Subsequent executors should not execute after exception");
        }

        @Test
        @DisplayName("Should handle mixed null and duplicate executors correctly")
        void shouldHandleMixedNullAndDuplicateExecutorsCorrectly() {
            ChainExecutor result = ChainExecutor.link(executor1, null, executor2, executor1, null, executor3, executor2);
            
            assertSame(executor1, result, "Should return first executor");
            assertSame(executor2, executor1.getNext(), "Should link to second unique executor");
            assertSame(executor3, executor2.getNext(), "Should link to third unique executor");
            assertNull(executor3.getNext(), "Last executor should have no next");
        }

        @Test
        @DisplayName("Should handle extremely long argument arrays")
        void shouldHandleExtremelyLongArgumentArrays() {
            ChainExecutor chain = ChainExecutor.link(executor1, executor2);
            String[] longArgs = new String[100]; // Reduced size for test performance
            for (int i = 0; i < 100; i++) {
                longArgs[i] = "arg" + i;
            }
            
            assertDoesNotThrow(() -> chain.execute(mockApplicationContext, longArgs), 
                              "Should handle large argument arrays");
            
            assertEquals(100, executor1.getLastArgs().length, "Should preserve all arguments for first executor");
            assertEquals(100, executor2.getLastArgs().length, "Should preserve all arguments for second executor");
            assertEquals("arg0", executor1.getLastArgs()[0], "Should preserve first argument");
            assertEquals("arg99", executor1.getLastArgs()[99], "Should preserve last argument");
        }

        @Test
        @DisplayName("Should handle chain with all null executors except first")
        void shouldHandleChainWithAllNullExecutorsExceptFirst() {
            ChainExecutor result = ChainExecutor.link(executor1, null, null, null, null);
            
            assertSame(executor1, result, "Should return first executor");
            assertNull(executor1.getNext(), "Should have no next executors");
            
            // Test execution still works
            result.execute(mockApplicationContext, "test");
            assertEquals(1, executor1.getExecutionCount(), "First executor should still execute");
        }

        @Test
        @DisplayName("Should maintain HashSet uniqueness contract throughout linking")
        void shouldMaintainHashSetUniquenessContractThroughoutLinking() {
            // Create multiple references to same executors to thoroughly test HashSet behavior
            TestChainExecutor exec1 = new TestChainExecutor("exec1");
            TestChainExecutor exec2 = new TestChainExecutor("exec2");
            TestChainExecutor exec3 = new TestChainExecutor("exec3");
            
            ChainExecutor result = ChainExecutor.link(exec1, exec2, exec3, exec1, exec2, exec3, exec1);
            
            // Should only have exec1 -> exec2 -> exec3
            assertSame(exec1, result);
            assertSame(exec2, exec1.getNext());
            assertSame(exec3, exec2.getNext());
            assertNull(exec3.getNext());
            
            // Verify execution works correctly
            result.execute(mockApplicationContext, "uniqueness-test");
            assertEquals(1, exec1.getExecutionCount());
            assertEquals(1, exec2.getExecutionCount());
            assertEquals(1, exec3.getExecutionCount());
        }
    }

    /**
     * Test implementation of ChainExecutor for testing purposes.
     * Provides instrumentation to track execution state and verify behavior.
     * Exposes protected checkNext method for direct testing.
     */
    private static class TestChainExecutor extends ChainExecutor {
        private final String name;
        private int executionCount = 0;
        private ConfigurableApplicationContext lastApplicationContext;
        private String[] lastArgs;
        private long lastExecutionTime;

        public TestChainExecutor(String name) {
            this.name = name;
        }

        @Override
        public void execute(ConfigurableApplicationContext applicationContext, String... args) {
            this.executionCount++;
            this.lastApplicationContext = applicationContext;
            this.lastArgs = args != null ? args.clone() : new String[0];
            this.lastExecutionTime = System.nanoTime();
            checkNext(applicationContext, args);
        }

        /**
         * Exposes the protected checkNext method for direct testing
         */
        public void testCheckNext(ConfigurableApplicationContext applicationContext, String... args) {
            checkNext(applicationContext, args);
        }

        /**
         * Exposes the private next field for testing chain structure
         */
        public ChainExecutor getNext() {
            return next;
        }

        public int getExecutionCount() {
            return executionCount;
        }

        public ConfigurableApplicationContext getLastApplicationContext() {
            return lastApplicationContext;
        }

        public String[] getLastArgs() {
            return lastArgs;
        }

        public long getLastExecutionTime() {
            return lastExecutionTime;
        }

        @Override
        public String toString() {
            return "TestChainExecutor{" + name + "}";
        }
    }
}