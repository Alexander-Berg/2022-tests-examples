package ru.yandex.partner.core.entity.queue.service;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.queue.repository.TaskRepository;
import ru.yandex.partner.core.entity.tasks.EmailToProcessingTask;
import ru.yandex.partner.core.entity.tasks.EmailToProcessingTaskFactory;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.core.queue.TaskData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CoreTest
@ExtendWith(MySqlRefresher.class)
public class EmailToProcessingTaskTest {

    @Autowired
    private TaskQueueService taskQueueService;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private EmailToProcessingTaskFactory factory;

    @Test
    void test() {
        String serializedParams = "{\"uid\":123}";

        EmailToProcessingTask.Payload payload = EmailToProcessingTask.Payload.of(123L);
        assertEquals(serializedParams, payload.serializeParams());

        taskQueueService.enqueue(payload);

        Optional<TaskData> optionalTaskData = taskRepository.pickFreeTask(EmailToProcessingTask.class);
        assertTrue(optionalTaskData.isPresent());

        TaskData taskData = optionalTaskData.get();
        assertEquals(serializedParams, taskData.getParams());

        EmailToProcessingTask emailToProcessingTask = factory.fromTaskData(taskData);
        TaskData recoveredTaskData = emailToProcessingTask.getTaskData();
        assertEquals(serializedParams, recoveredTaskData.getParams());
    }
}
