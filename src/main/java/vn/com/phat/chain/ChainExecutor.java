package vn.com.phat.chain;

import org.springframework.context.ConfigurableApplicationContext;

public abstract class ChainExecutor {
    public abstract void execute(ConfigurableApplicationContext applicationContext, String... args);
    private ChainExecutor next;

    public static ChainExecutor link(ChainExecutor first, ChainExecutor... executors) {
        ChainExecutor current = first;
        for (ChainExecutor executor : executors) {
            current.next = executor;
            current = executor;
        }
        return first;
    }

    protected void checkNext(ConfigurableApplicationContext applicationContext, String... args) {
        if (next != null) {
            next.execute(applicationContext, args);
        }
    }

}
