package ru.yandex.partner.core.entity.queue.doaction;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionPerformer;
import ru.yandex.partner.core.action.ActionUserIdContext;
import ru.yandex.partner.core.action.factories.AllCustomPayloadActionsFactory;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.block.actions.rtb.external.RtbBlockStopFactory;
import ru.yandex.partner.core.entity.block.filter.BlockFilters;
import ru.yandex.partner.core.entity.block.model.BaseBlock;
import ru.yandex.partner.core.entity.block.model.BlockWithMultistate;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.service.BlockService;
import ru.yandex.partner.core.entity.queue.exceptions.TaskExecutionException;
import ru.yandex.partner.core.entity.queue.repository.TaskRepository;
import ru.yandex.partner.core.entity.queue.service.TaskQueueExecutor;
import ru.yandex.partner.core.entity.queue.service.TaskQueueService;
import ru.yandex.partner.core.entity.tasks.doaction.DoActionTask;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.core.multistate.Multistate;
import ru.yandex.partner.core.multistate.block.BlockStateFlag;
import ru.yandex.partner.core.multistate.queue.TaskMultistate;
import ru.yandex.partner.core.queue.TaskData;
import ru.yandex.partner.core.queue.TaskExecutionResult;
import ru.yandex.partner.core.queue.TaskType;
import ru.yandex.partner.core.service.entitymanager.EntityManager;
import ru.yandex.partner.libs.multistate.graph.MultistateGraph;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.partner.core.CoreConstants.SYSTEM_CRON_USER_ID;
import static ru.yandex.partner.libs.multistate.MultistatePredicates.has;

@CoreTest
@ExtendWith(MySqlRefresher.class)
class DoActionTaskTest {
    private static final Long PAGE_ID = 41443L;
    private static final Long BLOCK_UNIQ_ID = 347649081345L;
    private static final TaskExecutionException TASK_EXECUTION_EXCEPTION = new TaskExecutionException(
            "Couldn't apply the action \"stop\" to some entities"
    ) {
        @Override
        public Object getErrorData() {
            return List.of(BLOCK_UNIQ_ID);
        }
    };
    @Autowired
    TaskRepository taskRepository;
    @Autowired
    TaskQueueExecutor executor;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    BlockService blockService;
    @Autowired
    MultistateGraph<RtbBlock, BlockStateFlag> multistateGraph;
    @Autowired
    RtbBlockStopFactory rtbBlockStopFactory;
    @Autowired
    ActionPerformer actionPerformer;
    @Autowired
    AllCustomPayloadActionsFactory factory;
    @Autowired
    EntityManager entityManager;
    @Autowired
    TaskQueueService taskQueueService;
    @Autowired
    ActionUserIdContext actionUserIdContext;
    String params;

    @Test
    void doActionStopTaskByModelIds() throws JsonProcessingException {
        String taskParams = objectMapper.writeValueAsString(Map.of("modelIds", List.of(BLOCK_UNIQ_ID),
                "actionName", "stop",
                "modelName", "context_on_site_rtb",
                "userId", SYSTEM_CRON_USER_ID));

        taskRepository.insertTask(TaskData.newBuilder()
                .withMultistate(new TaskMultistate())
                .withTypeId(TaskType.DO_ACTION.getTypeId())
                .withParams(taskParams)
                .withUserId(SYSTEM_CRON_USER_ID)
                .withTries(0)
                .build());

        var result = executor.doOneTask(DoActionTask.class);
        var blockAfter = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, List.of(BLOCK_UNIQ_ID)))
                .withProps(Set.of(BlockWithMultistate.MULTISTATE))
        ).get(0);
        assertThat(blockAfter.getMultistate().hasFlag(BlockStateFlag.WORKING)).isFalse();
        assertThat(result).isTrue();

    }

    @Test
    void doActionStopTaskByPageId() {
        Set<Long> allowedStates =
                multistateGraph.getMultistatesForPredicate(has(BlockStateFlag.WORKING))
                        .stream().map(Multistate::toMultistateValue).collect(Collectors.toSet());

        List<RtbBlock> rtbBlocks = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(BlockFilters.PAGE_ID.eq(PAGE_ID).and(
                        BlockFilters.MULTISTATE.asLongFilter().in(allowedStates)))
                .withProps(Set.of(BlockWithMultistate.MULTISTATE, BaseBlock.PAGE_ID))
        );

        assertThat(rtbBlocks.isEmpty()).isFalse();

        taskRepository.insertTask(TaskData.newBuilder()
                .withMultistate(new TaskMultistate())
                .withTypeId(TaskType.DO_ACTION.getTypeId())
                .withParams(getJsonParams())
                .withUserId(SYSTEM_CRON_USER_ID)
                .withTries(0)
                .build());

        var result = executor.doOneTask(DoActionTask.class);
        assertThat(result).isTrue();

        List<RtbBlock> rtbBlocksAfter = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(BlockFilters.PAGE_ID.eq(PAGE_ID).and(
                        BlockFilters.MULTISTATE.asLongFilter().in(allowedStates)))
                .withProps(Set.of(BlockWithMultistate.MULTISTATE, BaseBlock.PAGE_ID))
        );

        assertThat(rtbBlocksAfter.isEmpty()).isTrue();
    }

    @Test
    void doActionStopTaskByPageIdWithAlreadyStoppedBlocks() {
        var action = rtbBlockStopFactory.createAction(List.of(BLOCK_UNIQ_ID));
        actionUserIdContext.setUserId(0L);
        actionPerformer.doActions(action);

        Set<Long> allowedStates =
                multistateGraph.getMultistatesForPredicate(has(BlockStateFlag.WORKING))
                        .stream().map(Multistate::toMultistateValue).collect(Collectors.toSet());

        List<RtbBlock> rtbBlocks = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(BlockFilters.PAGE_ID.eq(PAGE_ID).and(
                        BlockFilters.MULTISTATE.asLongFilter().in(allowedStates)))
                .withProps(Set.of(BlockWithMultistate.MULTISTATE, BaseBlock.PAGE_ID))
        );

        assertThat(rtbBlocks.isEmpty()).isFalse();

        taskRepository.insertTask(TaskData.newBuilder()
                .withMultistate(new TaskMultistate())
                .withTypeId(TaskType.DO_ACTION.getTypeId())
                .withParams(getJsonParams())
                .withUserId(SYSTEM_CRON_USER_ID)
                .withTries(0)
                .build());

        var result = executor.doOneTask(DoActionTask.class);
        assertThat(result).isTrue();

        List<RtbBlock> rtbBlocksAfter = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(BlockFilters.PAGE_ID.eq(PAGE_ID).and(
                        BlockFilters.MULTISTATE.asLongFilter().in(allowedStates)))
                .withProps(Set.of(BlockWithMultistate.MULTISTATE, BaseBlock.PAGE_ID))
        );

        assertThat(rtbBlocksAfter.isEmpty()).isTrue();
    }

    @Test
    void checkCanNotDoAction() throws JsonProcessingException {
        //stop block BLOCK_UNIQ_ID
        var action = rtbBlockStopFactory.createAction(List.of(BLOCK_UNIQ_ID));
        actionUserIdContext.setUserId(0L);
        actionPerformer.doActions(action);

        Set<Long> allowedStates =
                multistateGraph.getMultistatesForPredicate(has(BlockStateFlag.WORKING))
                        .stream().map(Multistate::toMultistateValue).collect(Collectors.toSet());

        List<RtbBlock> rtbBlocks = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(BlockFilters.PAGE_ID.eq(PAGE_ID).and(
                        BlockFilters.MULTISTATE.asLongFilter().in(allowedStates)))
                .withProps(Set.of(BlockWithMultistate.MULTISTATE, BaseBlock.PAGE_ID))
        );
        List<Long> toStopBlockIds = rtbBlocks.stream()
                .map(RtbBlock::getId).toList();

        //check working blocks before
        assertThat(rtbBlocks.isEmpty()).isFalse();

        // start mocks //
        params = getJsonParams();

        DoActionTask.DoActionTaskPayload payload = objectMapper.readValue(params,
                DoActionTask.DoActionTaskPayload.class);

        TaskData taskData = TaskData.newBuilder()
                .withParams(params)
                .withUserId(SYSTEM_CRON_USER_ID)
                .withTries(0)
                .build();

        DoActionTask doActionTask = new DoActionTask(actionPerformer, factory, objectMapper, taskData, entityManager,
                actionUserIdContext);
        doActionTask = Mockito.spy(doActionTask);

        Mockito.when(doActionTask.getPayload(params)).thenReturn(payload);
        Mockito.when(doActionTask.getIdsByPayload(payload)).thenReturn(toStopBlockIds);
        // end mocks //

        var result = doActionTask.execute();

        assertThat(result.isCommitted()).isTrue();

        //check that all blocks have been stopped
        List<RtbBlock> rtbBlocksAfter = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(BlockFilters.PAGE_ID.eq(PAGE_ID).and(
                        BlockFilters.MULTISTATE.asLongFilter().in(allowedStates)))
                .withProps(Set.of(BlockWithMultistate.MULTISTATE, BaseBlock.PAGE_ID))
        );

        assertThat(rtbBlocksAfter.isEmpty()).isTrue();
    }

    @Test
    void checkCorrectUpdateTaskWithError() {
        TaskData taskData = TaskData.newBuilder()
                .withMultistate(new TaskMultistate())
                .withTypeId(TaskType.DO_ACTION.getTypeId())
                .withParams(params)
                .withUserId(SYSTEM_CRON_USER_ID)
                .withTries(0)
                .build();

        taskData = taskRepository.insertTask(taskData);

        DoActionTask doActionTask = new DoActionTask(actionPerformer, factory, objectMapper, taskData, entityManager,
                actionUserIdContext);

        //save result to db
        taskQueueService.finishTask(doActionTask, TaskExecutionResult.failure(TASK_EXECUTION_EXCEPTION.getErrorData(),
                TASK_EXECUTION_EXCEPTION.getMessage()));

        //check task after error
        taskData = taskRepository.get(taskData.getId());

        assertThat(taskData.getErrorData()).isEqualTo("[" + BLOCK_UNIQ_ID + "]");
        assertThat(taskData.getLog()).isEqualTo(TASK_EXECUTION_EXCEPTION.getMessage());
    }

    private String getJsonParams() {
        String taskParams;
        try {
            taskParams = objectMapper.writeValueAsString(Map.of("pageId", PAGE_ID,
                    "actionName", "stop",
                    "modelName", "context_on_site_rtb",
                    "userId", SYSTEM_CRON_USER_ID));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return taskParams;
    }
}
