package vn.com.phat.chain;

import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashSet;
import java.util.Set;

public abstract class ChainExecutor {
    public abstract void execute(ConfigurableApplicationContext applicationContext, String... args);
    private ChainExecutor next;

    public static ChainExecutor link(ChainExecutor first, ChainExecutor... executors) {
        ChainExecutor current = first;
        if(first == null) return null;
        if (executors == null)
            return first;

        Set<ChainExecutor> uniqueExecutors = new HashSet<>();
        uniqueExecutors.add(first);
        for (ChainExecutor executor : executors) {
            if(executor == null || !uniqueExecutors.add(executor)) continue;
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
