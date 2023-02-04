import asyncio
from enum import Enum
from typing import List, Optional
from unittest import mock

import pytest
from infra.deploy_notifications_controller.lib import infra_client, qnotifier_client, paste_client, jns_client
from infra.deploy_notifications_controller.lib import yp_client, looper, http

from infra.deploy_notifications_controller.lib.async_queue_with_statistic import AsyncQueueWithStatistic
from infra.deploy_notifications_controller.lib.models.action import DummyQnotifierMessage, Notification, \
    QnotifierMessage, InfraChange
from infra.deploy_notifications_controller.lib.models.stage import Stage
from infra.deploy_notifications_controller.lib.models.url_formatter import UrlFormatter
from infra.deploy_notifications_controller.lib.yp_client import YpClient

pytestmark = pytest.mark.asyncio


STAGES = 10

INFRA_SERVICE_ID = 1
INFRA_ENVIRONMENT_ID = 2


def make_project(client) -> str:
    return client.create_object(object_type="project", attributes={
        'spec': {
            'account_id': 'tmp'
        }
    })


def make_stage(client, project_id) -> str:
    du_revision = 1

    return client.create_object(object_type="stage", attributes={
        'labels': {
            'infra_environment': INFRA_ENVIRONMENT_ID,
            'infra_service': INFRA_SERVICE_ID,
        },
        "meta": {
            'project_id': project_id,
        },
        'spec': {
            'account_id': 'tmp',
            'revision': 1,
            'deploy_units': {
                'du1': {
                    'revision': du_revision,
                    'replica_set': {
                        'replica_set_template': {
                            'pod_template_spec': {
                                'spec': {
                                    'pod_agent_payload': {
                                        'spec': {
                                            'resources': {
                                                'static_resources': [
                                                    {
                                                        'id': 'ref1',
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                },
                            },
                        },
                    },
                },
            },
        },
        'status': {
            'deploy_units': {
                'du1': {
                    'replica_set': {
                        'revision_id': 1,
                    },
                    'latest_deployed_revision': du_revision,
                }
            }
        }
    })


def make_notification_policy(client, stage_id):
    return client.create_object(object_type="notification_policy", attributes={
        'meta': {
            'id': stage_id,
            'stage_id': stage_id,
        },
        'spec': {
            'deploy_unit_actions': {
                'du1': {
                    'start_actions': [
                        {
                            'jns_message': {
                                'project': 'test',
                                'channel': 'test',
                            }
                        }
                    ],
                    'finish_actions': [
                        {
                            'jns_message': {
                                'project': 'test',
                                'channel': 'test',
                            }
                        }
                    ],
                }
            }
        }
    })


def set_deploying(client, stage_id, log):
    old_du_revision = client.get_object(
        object_type='stage',
        object_identity=stage_id,
        selectors=['/spec/deploy_units/du1/revision'],
    )[0]

    new_du_revision = old_du_revision + 1

    log.debug("Progress du %s and %s", old_du_revision, new_du_revision)

    client.update_object(
        object_type="stage",
        object_identity=stage_id,
        set_updates=[
            {
                'path': '/spec/deploy_units/du1/revision',
                'value': new_du_revision,
            },
        ],
    )


def set_deployed(client, stage_id, log):
    du_revision = client.get_object(
        object_type='stage',
        object_identity=stage_id,
        selectors=['/spec/deploy_units/du1/revision'],
    )[0]

    log.debug("Updated du %s", du_revision)

    client.update_object(
        object_type="stage",
        object_identity=stage_id,
        set_updates=[
            {
                'path': '/status/deploy_units/du1/latest_deployed_revision',
                'value': du_revision,
            },
        ],
    )


def update_spec(client, stage_id, log):
    path = '/meta/project_id'

    old_res = client.get_object(
        object_type='stage',
        object_identity=stage_id,
        selectors=[path],
    )[0]

    new_res = make_project(client)

    log.debug("Project project ids %s and %s", old_res, new_res)

    client.update_object(
        object_type='stage',
        object_identity=stage_id,
        set_updates=[{
            'path': path,
            'value': new_res,
        }],
    )


@pytest.fixture(scope='function')
def with_stages(yp_env):
    project_id = make_project(yp_env)
    return [
        make_stage(yp_env, project_id)
        for _ in range(STAGES)
    ]


@pytest.fixture(scope='function')
def with_stages_and_npolicies(yp_env):
    project_id = make_project(yp_env)
    stage_ids = []
    for _ in range(STAGES):
        stage_id = make_stage(yp_env, project_id)
        stage_ids.append(stage_id)
        make_notification_policy(yp_env, stage_id)
    return stage_ids


@pytest.fixture(scope='function')
def statistics():
    return http.StatisticsServer(port=0)


@pytest.fixture(scope='function')
def controller(yp_env, statistics):
    client = yp_client.YpClient(yp_env.create_grpc_object_stub())

    qclient = mock.MagicMock(qnotifier_client.QnotifierClient)

    async def post_event_coro(*args, **kwargs):
        pass

    qclient.post_event.side_effect = post_event_coro

    async def paste(text, *args, **kwargs):
        return text

    iclient = mock.MagicMock(infra_client.InfraClient)

    async def iclient_get_current_events_coro(*args, **kwargs):
        return [(0, 'Event title', {'event_meta': 'value'})]

    iclient.get_current_events.side_effect = iclient_get_current_events_coro

    async def iclient_event_coro(*args, **kwargs):
        pass

    iclient.create_event.side_effect = iclient_event_coro
    iclient.update_event.side_effect = iclient_event_coro

    pclient = mock.MagicMock(paste_client.PasteClient)
    pclient.paste.side_effect = paste
    jclient = mock.MagicMock(jns_client.JNSClient)

    controller = looper.Looper(
        yp_client=client,
        infra_client=iclient,
        infra_attempts_delay=0.1,
        qnotifier_client=qclient,
        paste_client=pclient,
        batch_size=1000,
        stage_read_period=30,
        timestamp_generate_period=1,
        qnotifier_send_attempts=2,
        qnotifier_aggregation_period=60,
        history_workers=1,
        history_read_delay=0,
        statistics=statistics,
        url_formatter=UrlFormatter(stage_format='{controller} id={id}', user_format='{name}'),
        jns_client=jclient,
        jns_attempts_delay=0,
    )
    return controller


class UseMode(Enum):
    ASSERT = 'check',
    PROCESS = 'process',


class LooperTestContext:
    __slots__ = [
        'yp_env',
        'controller',
        'queue',
        'log',
        'with_stages',
        'stage_id',
        'stage',
        'last_timestamp',
        'last_queue_size',
    ]

    yp_env: YpClient
    controller: looper.Looper
    queue: AsyncQueueWithStatistic
    with_stages: List[str]

    stage_id: Optional[str]
    stage: Optional[Stage]

    last_timestamp: int
    last_queue_size: int

    def __init__(self,
                 yp_env: YpClient,
                 controller: looper.Looper,
                 queue: AsyncQueueWithStatistic,
                 with_stages: List[str]):
        self.yp_env = yp_env
        self.controller = controller
        self.queue = queue
        self.log = controller.log
        self.with_stages = with_stages

        self.stage_id = None
        self.stage = None

        self.last_timestamp = 0
        self.last_queue_size = 0

    def debug_to_log(
        self,
        log_message: str,
    ):
        self.log.debug(log_message)

    def _update_stage(
        self,
        updater,
    ):
        updater(self.yp_env, self.stage.id, self.log)

    async def init_stage_and_assert_no_events_at_start(self):
        self.stage_id = self.with_stages[0]
        self.controller.yp_client.stage_filter = f'[/meta/id] = {self.stage_id!r}'

        await asyncio.sleep(2.)  # we sleep for the initial poll to not catch STAGE_CREATED event

        await self._assert_no_events_at_start(
            "no events should be generated at start",
        )

    async def _assert_no_events_at_start(
        self,
        log_message: str,
    ):
        self.debug_to_log(log_message)

        await self._assert_generate_timestamp_poll_stages(first_poll=True)

        self._assert_timestamp_changed()

        for _ in (0, 1):
            await self._assert_get_history(
                expected_queue_size_delta=0,
            )

    async def _assert_generate_timestamp_poll_stages(
        self,
        first_poll: Optional[bool] = False
    ):
        ts = await self.controller.yp_client.generate_timestamp()
        await self._assert_poll_stages(
            ts,
            first_poll=first_poll,
        )

    async def _assert_poll_stages(
        self,
        ts: int,
        first_poll: Optional[bool] = False
    ):
        await self.controller._poll_stages(self.log, ts)

        assert len(self.controller.stages) == 1
        assert len(self.controller.stages_by_id) == 1

        if first_poll:
            assert self.stage_id in self.controller.stages_by_id
            self.stage = self.controller.stages_by_id[self.stage_id]

        assert self.stage.id in self.controller.stages_by_id
        assert self.stage.uuid in self.controller.stages

        assert self.controller.stages_by_id[self.stage.id] is self.stage
        assert self.controller.stages[self.stage.uuid] is self.stage

    async def _assert_get_history(
        self,
        expected_queue_size_delta: int,
    ):
        await self.controller._get_history_events(self.log, self.stage)

        self.assert_queue_size(
            expected_queue_size_delta=expected_queue_size_delta
        )

    async def _assert_poll_stages_get_history(
        self,
        expected_queue_size_delta: int,
    ):
        await self._assert_generate_timestamp_poll_stages()

        assert self.stage == self.controller.stages_queue.get_nowait()

        await self._assert_get_history(
            expected_queue_size_delta=expected_queue_size_delta,
        )

    async def assert_no_new_events(
        self,
        log_message: str
    ):
        self.debug_to_log(log_message)

        await self._assert_poll_stages_get_history(
            expected_queue_size_delta=0,
        )

        assert self.stage.last_timestamp == self.last_timestamp

    def _assert_timestamp_changed(self):
        assert self.last_timestamp < self.stage.last_timestamp
        self.last_timestamp = self.stage.last_timestamp

    def assert_queue_size(self,
                          expected_queue_size_delta: int,
                          ):
        assert self.queue.qsize() == self.last_queue_size + expected_queue_size_delta
        self.last_queue_size = self.queue.qsize()

    async def _put_item(self,
                        item,
                        ):
        await self.queue.put(item)
        self.assert_queue_size(
            expected_queue_size_delta=1,
        )

    async def _poll_item(self):
        item = await self.queue.get()
        self.assert_queue_size(
            expected_queue_size_delta=-1,
        )

        return item

    async def assert_stage_updated(self,
                                   log_message: str,
                                   stage_updater,
                                   expected_queue_size_delta: int = 1,
                                   ):
        self.debug_to_log(log_message)
        self._update_stage(updater=stage_updater)

        await self._assert_poll_stages_get_history(
            expected_queue_size_delta=expected_queue_size_delta,
        )

        self._assert_timestamp_changed()

    async def use_item(self,
                       log_message: str,
                       item_use,
                       use_mode: UseMode,
                       poll_item: bool,
                       ):
        self.debug_to_log(log_message)

        item = await self._poll_item()

        if UseMode.ASSERT == use_mode:
            item_use(item)

        if UseMode.PROCESS == use_mode:
            await item_use(self, item)

        if not poll_item:
            await self._put_item(item)


async def test_qnotifier(yp_env, controller, with_stages):
    context = LooperTestContext(
        yp_env=yp_env,
        controller=controller,
        with_stages=with_stages,
        queue=controller.qnotifier_queue,
    )

    await context.init_stage_and_assert_no_events_at_start()

    await context.assert_stage_updated(
        "now stage is started deploying, one new message is expected",
        stage_updater=set_deploying,
    )

    await context.use_item(
        "after stage was started deploying, we expect to post event about it",
        item_use=send_qnotifier_message,
        use_mode=UseMode.PROCESS,
        poll_item=True,
    )

    await context.assert_no_new_events(
        "now all events are sent, no new events are expected",
    )

    await context.assert_stage_updated(
        "now stage is deployed, one new message is expected",
        stage_updater=set_deployed,
    )

    await context.use_item(
        "after stage was deployed, we expect nothing from qnotifier but dummy message (statuses monitoring is disabled)",
        item_use=assert_qnotifier_message_dummy,
        use_mode=UseMode.ASSERT,
        poll_item=True,
    )

    await context.assert_stage_updated(
        "now stage spec is updated, one new message is expected",
        stage_updater=update_spec,
    )

    await context.use_item(
        "after stage spec was updated, we expect message with 0 post attempts",
        item_use=create_message_attempts_assertion(0),
        use_mode=UseMode.ASSERT,
        poll_item=False,
    )

    await context.use_item(
        "after stage spec was updated, we expect qnotifier to fail and event not posted",
        item_use=fail_to_send_qnotifier_message,
        use_mode=UseMode.PROCESS,
        poll_item=True,
    )

    await context.use_item(
        "after message was not posted, we expect one message with 1 post attempts",
        item_use=create_message_attempts_assertion(1),
        use_mode=UseMode.ASSERT,
        poll_item=True,
    )

    await context.assert_no_new_events(
        "while we have not sent our last notification, we expect to not get it again from YP",
    )


def create_message_attempts_assertion(expected_attempts: int):
    def assert_message_attempts(message: QnotifierMessage):
        assert message.attempts == expected_attempts

    return assert_message_attempts


async def fail_to_send_qnotifier_message(context: LooperTestContext,
                                         message: QnotifierMessage,
                                         ):
    qclient_post_event_default_coro = context.controller.qnotifier_client.post_event.side_effect

    context.controller.qnotifier_client.post_event.side_effect = Exception("Kaboom!")
    await send_qnotifier_message(
        context=context,
        message=message,
    )

    context.assert_queue_size(
        expected_queue_size_delta=1,
    )

    context.controller.qnotifier_client.post_event.side_effect = qclient_post_event_default_coro


async def send_qnotifier_message(context: LooperTestContext,
                                 message: QnotifierMessage,
                                 ):
    await context.controller._send_qnotifier_message(context.log, message)
    context.controller.qnotifier_client.post_event.assert_called_once()
    context.controller.qnotifier_client.post_event.reset_mock()


async def assert_qnotifier_message_dummy(message: QnotifierMessage):
    assert isinstance(message, DummyQnotifierMessage)


async def test_loop_with_notification_policy(yp_env, controller, with_stages_and_npolicies):
    context = LooperTestContext(
        yp_env=yp_env,
        controller=controller,
        with_stages=with_stages_and_npolicies,
        queue=controller.notify_queue,
    )

    await context.init_stage_and_assert_no_events_at_start()

    await context.assert_stage_updated(
        "now stage is started deploying, one notification action is expected",
        stage_updater=set_deploying,
    )

    await context.use_item(
        "after stage was started deploying, notification action 'DEPLOY_UNIT_STARTED' is expected",
        item_use=create_notification_event_kind_assertion(Notification.EventKind.DEPLOY_UNIT_STARTED),
        use_mode=UseMode.ASSERT,
        poll_item=True,
    )

    await context.assert_stage_updated(
        "now stage is deployed, one notification action is expected",
        stage_updater=set_deployed,
    )

    await context.use_item(
        "after stage was deployed, notification action 'DEPLOY_UNIT_FINISHED' is expected",
        item_use=create_notification_event_kind_assertion(Notification.EventKind.DEPLOY_UNIT_FINISHED),
        use_mode=UseMode.ASSERT,
        poll_item=True,
    )


def create_notification_event_kind_assertion(expected_event_kind: Notification.EventKind):
    def assert_notification_event_kind(notification: Notification):
        return notification.event_kind == expected_event_kind

    return assert_notification_event_kind


async def test_infra(yp_env, controller, with_stages):
    context = LooperTestContext(
        yp_env=yp_env,
        controller=controller,
        with_stages=with_stages,
        queue=controller.infra_queue,
    )

    await context.init_stage_and_assert_no_events_at_start()

    await context.assert_stage_updated(
        "now stage is started deploying, two new infra changes are expected",
        stage_updater=set_deploying,
        expected_queue_size_delta=2,
    )

    await context.use_item(
        "after stage was started deploying, we expect infra change 'STARTED'",
        item_use=create_infra_change_event_kind_assertion(InfraChange.EventKind.STARTED),
        use_mode=UseMode.ASSERT,
        poll_item=False,
    )

    await try_send_infra_change_started(
        "after stage was started deploying, we expect to post event about it",
        context,
    )

    await context.use_item(
        "after stage was started deploying, we expect infra change 'CANCELLED' after 'STARTED'",
        item_use=create_infra_change_event_kind_assertion(InfraChange.EventKind.CANCELLED),
        use_mode=UseMode.ASSERT,
        poll_item=False,
    )

    await try_send_infra_change_finished(
        "after stage was started deploying, we expect to post event about it",
        context,
    )

    await context.assert_no_new_events(
        "now all events are sent, no new events are expected",
    )

    await context.assert_stage_updated(
        "now stage is deployed, one new infra change is expected",
        stage_updater=set_deployed,
    )

    await context.use_item(
        "after stage was deployed, we expect infra change 'FINISHED'",
        item_use=create_infra_change_event_kind_assertion(InfraChange.EventKind.FINISHED),
        use_mode=UseMode.ASSERT,
        poll_item=False,
    )

    await fail_to_send_infra_change_finished(
        "after stage was deployed, we expect infra client to fail and finished event not posted",
        context,
    )

    await try_send_infra_change_finished(
        "after stage was started deploying, we expect to post event about it",
        context,
    )

    await context.assert_stage_updated(
        "now stage spec is updated, no messages is expected",
        stage_updater=update_spec,
        expected_queue_size_delta=0,
    )


def create_infra_change_event_kind_assertion(expected_event_kind: InfraChange.EventKind):
    def assert_infra_change_event_kind(infra_change: InfraChange):
        return infra_change.event_kind == expected_event_kind

    return assert_infra_change_event_kind


async def try_send_infra_change_started(log_message: str,
                                        context: LooperTestContext,
                                        ):
    context.debug_to_log(log_message)

    await context.controller._process_infra_queue_item(context.log, context.controller.infra_client)

    context.controller.infra_client.create_event.assert_called_once()
    context.controller.infra_client.create_event.reset_mock()

    context.assert_queue_size(
        expected_queue_size_delta=-1,
    )


async def fail_to_send_infra_change_finished(log_message: str,
                                             context: LooperTestContext,
                                             ):
    iclient_update_event_default_coro = context.controller.infra_client.update_event.side_effect

    context.controller.infra_client.update_event.side_effect = Exception("Kaboom!")

    await try_send_infra_change_finished(
        log_message=log_message,
        context=context,
        expected_queue_size_delta=0,
    )

    context.controller.infra_client.update_event.side_effect = iclient_update_event_default_coro


async def try_send_infra_change_finished(log_message: str,
                                         context: LooperTestContext,
                                         expected_queue_size_delta: int = -1,
                                         ):
    context.debug_to_log(log_message)

    await context.controller._process_infra_queue_item(context.log, context.controller.infra_client)

    context.controller.infra_client.get_current_events.assert_called_once()
    context.controller.infra_client.get_current_events.reset_mock()

    context.controller.infra_client.update_event.assert_called_once()
    context.controller.infra_client.update_event.reset_mock()

    context.assert_queue_size(
        expected_queue_size_delta=expected_queue_size_delta,
    )
