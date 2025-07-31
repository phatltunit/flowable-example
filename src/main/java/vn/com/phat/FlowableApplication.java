package vn.com.phat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import vn.com.phat.chain.ChainExecutor;
import vn.com.phat.chain.CloseApplication;
import vn.com.phat.chain.DestroyEngine;

import java.util.Optional;

@Slf4j
@SpringBootApplication(proxyBeanMethods = false)
public class FlowableApplication {

    public static void main(String[] args) {
        var applicationContext = SpringApplication.run(FlowableApplication.class, args);
        Optional.ofNullable(ChainExecutor.link(new DestroyEngine(), new CloseApplication()))
                .ifPresent(ex -> ex.execute(applicationContext, args));
        log.info("Application has been closed successfully.");
    }
}
