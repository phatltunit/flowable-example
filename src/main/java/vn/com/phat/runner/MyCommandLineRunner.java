package vn.com.phat.runner;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.Execution;
import org.flowable.task.api.Task;
import org.springframework.boot.CommandLineRunner;

import java.util.function.Consumer;

@AllArgsConstructor
@Slf4j
public class MyCommandLineRunner implements CommandLineRunner {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    @Override
    public void run(String... args) throws Exception {

        log.info("Cleaning up previous deployments and process instances...");
        repositoryService.createDeploymentQuery().list().forEach(deployment -> {
            repositoryService.deleteDeployment(deployment.getId(), true);
        });

       var runningProcessInstance = runtimeService.createExecutionQuery().list().stream().map(Execution::getProcessInstanceId).toList();
        runtimeService.bulkDeleteProcessInstances(runningProcessInstance, "cleanup");
        log.info("Previous deployments and process instances cleaned up.");

        log.info("Starting new deployment and process instance...");
        var deployment = repositoryService.createDeployment().addClasspathResource("example.bpmn")
                .deploy();
        var deploymentId = deployment.getId();

        var deploymentQuery = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).singleResult();
        log.info("Deployment completed with ID: {}", deploymentQuery.getDeploymentId());


        log.info("Starting process instance for process definition key: {}", deploymentQuery.getKey());
        var processInstance = runtimeService.startProcessInstanceByKey("holidayRequest");
        log.info("Process instance started with ID: {}", processInstance.getProcessInstanceId());


        var tasks = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).list();
        Consumer<Task> completeTask = task -> {
            log.info("Completing task: {} for process instance ID: {}", task.getName(), processInstance.getProcessInstanceId());
            var variables = task.getProcessVariables();
            variables.put("approved", Math.random() < 0.5);
            log.info("Task variables: {}", variables);
            taskService.complete(task.getId(), variables);
            log.info("Task {} completed successfully.", task.getName());
        };
        while(!tasks.isEmpty()){
            log.info("Found {} tasks for process instance ID: {}", tasks.size(), processInstance.getProcessInstanceId());
            tasks.forEach(completeTask);

            // Re-fetch tasks after completing the current ones
            tasks = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).list();
            log.info("Re-fetched tasks, found {} tasks remaining for process instance ID: {}", tasks.size(), processInstance.getProcessInstanceId());
        }
        log.info("There are no more tasks for process instance ID: {}", processInstance.getProcessInstanceId());
    }
}
