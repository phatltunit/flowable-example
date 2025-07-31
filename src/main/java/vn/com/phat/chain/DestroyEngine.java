package vn.com.phat.chain;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.ProcessEngines;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
public class DestroyEngine extends ChainExecutor{

    @Override
    public void execute(ConfigurableApplicationContext applicationContext, String... args) {
        log.info("We'll destroy the Flowable Process Engines after the application has started.");
        ProcessEngines.destroy();
        checkNext(applicationContext, args);
    }
}
