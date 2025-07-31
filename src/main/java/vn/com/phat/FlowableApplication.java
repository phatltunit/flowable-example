package vn.com.phat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import vn.com.phat.chain.ChainExecutor;
import vn.com.phat.chain.CloseApplication;
import vn.com.phat.chain.DestroyEngine;

@Slf4j
@SpringBootApplication(proxyBeanMethods = false)
public class FlowableApplication {

    public static void main(String[] args) {
        ChainExecutor.link(new DestroyEngine(), new CloseApplication())
                .execute(SpringApplication.run(FlowableApplication.class, args), args);
        log.info("Application has been closed successfully.");
    }
}
