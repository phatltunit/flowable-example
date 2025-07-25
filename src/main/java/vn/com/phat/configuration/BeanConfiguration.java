package vn.com.phat.configuration;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.com.phat.runner.MyCommandLineRunner;

@Configuration
public class BeanConfiguration {

    @Bean
    MyCommandLineRunner myCommandLineRunner(final RepositoryService repositoryService, final RuntimeService runtimeService,
            final TaskService taskService) {
        return new MyCommandLineRunner(repositoryService, runtimeService, taskService);
    }

}
