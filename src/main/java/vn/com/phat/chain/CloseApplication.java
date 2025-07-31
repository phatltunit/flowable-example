package vn.com.phat.chain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
public class CloseApplication extends ChainExecutor{

    @Override
    public void execute(ConfigurableApplicationContext applicationContext, String... args) {
        log.info("We'll close the Spring Application Context after the Process Engines has destroyed.");
        applicationContext.close();
        checkNext(applicationContext, args);
    }
}
