package vn.com.phat.delegation;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

@Slf4j
public class SomeThing implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Executing SomeThing delegate for process instance ID: {}", execution.getProcessInstanceId());
        execution.setVariable("someVariable", "someValue");
    }
}
