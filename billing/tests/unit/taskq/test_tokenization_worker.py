from datetime import datetime, timedelta, timezone
from unittest.mock import AsyncMock
from uuid import UUID

import pytest

from sendr_core.exceptions import BaseCoreError
from sendr_utils import alist, utcnow

from hamcrest import assert_that, equal_to, has_properties, has_property

from billing.yandex_pay.yandex_pay.core.actions.tokenization.mastercard import MastercardTokenizationAction
from billing.yandex_pay.yandex_pay.core.actions.tokenization.task import (
    TokenizationStatusDeletedError, TokenizationStatusExpiredError, TokenizationStatusInactiveError,
    TokenizationStatusSuspendedError
)
from billing.yandex_pay.yandex_pay.core.actions.tokenization.visa import VisaTokenizationAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enums import (
    MasterCardTokenStatus, TokenizationQueueState, TSPTokenStatus, TSPType, VisaTokenStatus
)
from billing.yandex_pay.yandex_pay.core.entities.mastercard import (
    DigitalCardData, MaskedCard, MaskedConsumer, MaskedConsumerIdentity, MaskedMobileNumber
)
from billing.yandex_pay.yandex_pay.core.entities.tokenization_queue import TokenizationQueue
from billing.yandex_pay.yandex_pay.interactions.cardproxy import MastercardEnrollment
from billing.yandex_pay.yandex_pay.interactions.cardproxy.entities import (
    ExpirationDate, PaymentInstrument, TokenInfo, VisaEnrollment
)
from billing.yandex_pay.yandex_pay.interactions.trust_paysys import TrustPaysysCardInfo
from billing.yandex_pay.yandex_pay.taskq.tokenization import MastercardTokenizationWorker, VisaTokenizationWorker
from billing.yandex_pay.yandex_pay.tests.matchers import close_to_datetime


@pytest.fixture
def past_time():
    return datetime(2000, 1, 1, 0, 0, 0, tzinfo=timezone.utc)


OWNER_UID = 5555


@pytest.fixture
def tsp_type(request):
    return getattr(request, 'param', TSPType.MASTERCARD)


@pytest.fixture
def card_to_tokenize_entity(past_time, tsp_type):
    return Card(
        trust_card_id='trust-card-id',
        owner_uid=OWNER_UID,
        tsp=tsp_type,
        expire=utcnow() + timedelta(days=365),
        last4='1234',
    )


@pytest.fixture
async def card_to_tokenize(storage, card_to_tokenize_entity):
    return await storage.card.create(card_to_tokenize_entity)


@pytest.fixture
async def task(storage, card_to_tokenize, past_time):
    return await storage.tokenization_queue.create(
        TokenizationQueue(
            card_id=card_to_tokenize.card_id,
            state=TokenizationQueueState.PENDING,
            run_at=past_time
        )
    )


@pytest.fixture
async def worker(tsp_type, dummy_logger, worker_app):
    return await worker_builder(tsp_type, dummy_logger, worker_app)


async def worker_builder(tsp_type, dummy_logger, worker_app):
    if tsp_type == TSPType.MASTERCARD:
        worker = MastercardTokenizationWorker(logger=dummy_logger)
    elif tsp_type == TSPType.VISA:
        worker = VisaTokenizationWorker(logger=dummy_logger)
    else:
        raise Exception(f'unknown tsp type {tsp_type}')
    worker.app = worker_app
    await worker.register_worker(worker_app)
    return worker


class TestGetTask:
    @pytest.mark.asyncio
    @pytest.mark.parametrize('tsp_type', [t for t in TSPType if t != TSPType.UNKNOWN], indirect=True)
    async def test_get_task(self, worker, storage, task):
        fetched_task = await worker.get_task(storage)

        expected_properties = {
            'tokenization_queue_id': task.tokenization_queue_id,
            'state': TokenizationQueueState.PROCESSING,
            'worker_id': UUID(worker.worker_id),
        }
        assert_that(
            fetched_task,
            has_properties(expected_properties),
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('tsp_type', [TSPType.VISA], indirect=True)
    async def test_visa_task_not_fetched(self, storage, task, dummy_logger, worker_app):
        w = await worker_builder(TSPType.MASTERCARD, dummy_logger, worker_app)
        # no Mastercard tasks are available
        with pytest.raises(TokenizationQueue.DoesNotExist):
            await w.get_task(storage)

        # but Visa task exists
        assert_that(
            await alist(storage.tokenization_queue.find()),
            equal_to([task]),
        )
        card = await storage.card.get(task.card_id)
        assert_that(card, has_property('tsp', TSPType.VISA))


@pytest.fixture
def mocked_storage_context(storage, worker, mocker):
    async_mock_context = AsyncMock()
    async_mock_context.__aenter__.return_value = storage
    return mocker.patch.object(worker, 'storage_context', return_value=async_mock_context)


@pytest.fixture
def action_class_by_tsp(tsp_type: TSPType):
    if tsp_type == TSPType.VISA:
        return VisaTokenizationAction
    elif tsp_type == TSPType.MASTERCARD:
        return MastercardTokenizationAction
    raise Exception(f'unexpected tsp type {tsp_type}')


@pytest.fixture
def get_tsp_status(tsp_type: TSPType):
    if tsp_type == TSPType.VISA:
        return TSPTokenStatus.from_visa_status
    elif tsp_type == TSPType.MASTERCARD:
        return TSPTokenStatus.from_mastercard_status
    raise Exception(f'unexpected tsp type {tsp_type}')


@pytest.mark.usefixtures('mocked_storage_context')
class TestProcessTask:
    @pytest.fixture
    def mocked_process_action(self, worker, mocker):
        return mocker.patch.object(worker, 'process_action', AsyncMock())

    @pytest.mark.asyncio
    @pytest.mark.parametrize('tsp_type', [t for t in TSPType if t != TSPType.UNKNOWN], indirect=True)
    async def test_process_task(
        self,
        storage,
        worker,
        task,
        action_class_by_tsp,
        mocked_process_action
    ):
        result = await worker.process_task()

        assert_that(result, equal_to(worker.PROCESS_TASK_WITH_NO_PAUSE))
        expected_params = {
            'card_id': task.card_id,
            'tokenization_action_cls': action_class_by_tsp,
        }
        mocked_process_action.assert_awaited_once_with(
            worker.action_cls, expected_params
        )

        task = await storage.tokenization_queue.get(task.task_id)
        expected_properties = {
            'state': TokenizationQueueState.FINISHED,
            'retries': 0,
        }
        assert_that(task, has_properties(expected_properties))

    @pytest.mark.asyncio
    @pytest.mark.parametrize('tsp_type', [t for t in TSPType if t != TSPType.UNKNOWN], indirect=True)
    async def test_no_tasks_available(self, storage, worker, mocked_process_action):
        result = await worker.process_task()
        assert_that(result, equal_to(False))

        mocked_process_action.assert_not_awaited()

        task = await alist(storage.tokenization_queue.find(filters={}))
        assert_that(len(task), equal_to(0))

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'exception,expected_task_state,expected_retries',
        [
            (TokenizationStatusDeletedError, TokenizationQueueState.FAILED, 0),
            (TokenizationStatusSuspendedError, TokenizationQueueState.FAILED, 0),
            (TokenizationStatusExpiredError, TokenizationQueueState.FAILED, 0),
            (TokenizationStatusInactiveError, TokenizationQueueState.PENDING, 1),
            (ValueError, TokenizationQueueState.PENDING, 1),
            (Exception, TokenizationQueueState.PENDING, 1),
        ]
    )
    @pytest.mark.parametrize('tsp_type', [t for t in TSPType if t != TSPType.UNKNOWN], indirect=True)
    async def test_process_task_exception(
        self,
        storage,
        worker,
        task,
        mocked_process_action,
        exception,
        expected_task_state,
        expected_retries,
    ):
        mocked_process_action.side_effect = exception
        result = await worker.process_task()

        assert_that(result, equal_to(worker.PROCESS_TASK_WITH_NO_PAUSE))
        task = await storage.tokenization_queue.get(task.task_id)
        expected_properties = {
            'state': expected_task_state,
            'retries': expected_retries,
        }
        if expected_task_state == TokenizationQueueState.FAILED:
            expected_properties['details'] = {
                'reason': f'Action failed because of exception {exception.__name__}'
            }
        elif expected_task_state == TokenizationQueueState.PENDING:
            expected_properties['details'] = {'retry_reason': exception.__name__}

        assert_that(task, has_properties(expected_properties))

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'tsp_token_status,expected_task_state,expected_retries,run_at_delta',
        [
            (TSPTokenStatus.ACTIVE, TokenizationQueueState.FINISHED, 0, None),
            (
                TSPTokenStatus.INACTIVE,
                TokenizationQueueState.PENDING,
                1,
                'TASKQ_TOKENIZATION_STATUS_INACTIVE_COOLDOWN_SECONDS',
            ),
            (TSPTokenStatus.DELETED, TokenizationQueueState.FAILED, 0, None),
            (TSPTokenStatus.SUSPENDED, TokenizationQueueState.FAILED, 0, None),
            (TSPTokenStatus.EXPIRED, TokenizationQueueState.FAILED, 0, None),
        ]
    )
    @pytest.mark.parametrize('tsp_type', [t for t in TSPType if t != TSPType.UNKNOWN], indirect=True)
    async def test_process_task_result_depending_on_tsp_token_status(
        self,
        storage,
        worker,
        task,
        tsp_token_status,
        expected_task_state,
        expected_retries,
        run_at_delta,
        past_time,
        yandex_pay_settings,
        action_class_by_tsp,
        mocker,
    ):
        mock_enrollment = mocker.patch.object(action_class_by_tsp, 'run', AsyncMock())
        mock_enrollment.return_value.tsp_token_status = tsp_token_status

        run_at_delta = 0 if run_at_delta is None else getattr(yandex_pay_settings, run_at_delta)
        run_at_delta = timedelta(seconds=run_at_delta)
        expected_run_at = utcnow() if expected_retries > 0 else past_time
        expected_run_at += run_at_delta

        result = await worker.process_task()
        assert_that(result, equal_to(worker.PROCESS_TASK_WITH_NO_PAUSE))
        task = await storage.tokenization_queue.get(task.task_id)
        expected_properties = {
            'state': expected_task_state,
            'retries': expected_retries,
            'run_at': close_to_datetime(expected_run_at, timedelta(minutes=1))
        }
        assert_that(task, has_properties(expected_properties))

    @pytest.mark.asyncio
    @pytest.mark.parametrize('tsp_type', [t for t in TSPType if t != TSPType.UNKNOWN], indirect=True)
    async def test_retry_previous_tasks(self, storage, worker, task):
        # pretend this task was previously fetched by the worker
        task.worker_id = worker.worker_id
        task.state = TokenizationQueueState.PROCESSING
        await storage.tokenization_queue.save(task)

        # the worker should now retry all its previous tasks
        result = await worker.process_task()
        assert_that(result, equal_to(worker.PROCESS_TASK_WITH_PAUSE))
        task = await storage.tokenization_queue.get(task.task_id)

        # check that the task is back into the PENDING state
        expected_properties = {
            'state': TokenizationQueueState.PENDING,
            'retries': 1,
            'run_at': close_to_datetime(utcnow(), timedelta(minutes=1)),
        }
        assert_that(task, has_properties(expected_properties))


@pytest.mark.usefixtures('mocked_storage_context')
class TestIntegrationWithTokenizationAction:
    INVALID_TOKEN_STATUS = 'Invalid_token_status'

    @pytest.fixture
    def token_status(self, request):
        return getattr(request, 'param', MasterCardTokenStatus.ACTIVE)

    @pytest.fixture
    def mock_interaction_clients(self, mocker, action_class_by_tsp):
        return mocker.patch.object(action_class_by_tsp, 'clients_cls')

    @pytest.fixture
    def fake_card_info(self):
        return TrustPaysysCardInfo(
            card_id='fake_card_id',
            card_token='fake_card_token',
            holder='Fake Holder',
            expiration_year=utcnow().year,
            expiration_month=12,
        )

    @pytest.fixture
    def mock_trust_get_card(self, mock_interaction_clients, fake_card_info):
        mock_get_card = AsyncMock(return_value=fake_card_info)
        mock_interaction_clients.return_value.trust_paysys.get_card = mock_get_card
        return mock_get_card

    @pytest.fixture
    def mock_cardproxy_enrollment(self, mock_interaction_clients, token_status, tsp_type):
        if tsp_type == TSPType.MASTERCARD:
            fake_enrollment = MastercardEnrollment(
                masked_card=MaskedCard(
                    src_digital_card_id='fake_src_digital_card_id',
                    pan_bin='fake_bin',
                    pan_last_four='0000',
                    date_of_card_created=utcnow(),
                    digital_card_data=DigitalCardData(
                        status=token_status,
                        descriptor_name='fake_descriptor_name',
                        art_uri='fake_art_uri',
                    )
                ),
                masked_consumer=MaskedConsumer(
                    masked_consumer_identity=MaskedConsumerIdentity(
                        identity_type='EXTERNAL_ACCOUNT_ID',
                        masked_identity_value='4053525715'
                    ),
                    status='ACTIVE',
                    date_consumer_added=utcnow(),
                    masked_mobile_number=MaskedMobileNumber()
                )
            )
            mock_enrollment = AsyncMock(return_value=fake_enrollment)
            mock_interaction_clients.return_value.cardproxy.mastercard_enrollment = mock_enrollment
        elif tsp_type == TSPType.VISA:
            fake_enrollment = VisaEnrollment(
                provisioned_token_id='fake-provisioned-token',
                pan_enrollment_id='fake-pan-enrollment',
                payment_instrument=PaymentInstrument(
                    last4='0000',
                    expiration_date=ExpirationDate(month=12, year=2030),
                    payment_account_reference='FAKE-PAR-1111'
                ),
                token_info=TokenInfo(
                    ExpirationDate(month=12, year=2030),
                    status=token_status,
                    last4='0000'
                )
            )
            mock_enrollment = AsyncMock(return_value=fake_enrollment)
            mock_interaction_clients.return_value.cardproxy.visa_enrollment = mock_enrollment
        else:
            raise Exception(f'unknown tsp type {tsp_type}')

        return mock_enrollment

    @pytest.fixture
    def expected_enrollment_args(self, tsp_type, card_to_tokenize, fake_card_info):
        args = {
            'uid': card_to_tokenize.owner_uid,
            'pci_card_token': fake_card_info.card_token,
            'expiration_month': fake_card_info.expiration_month,
            'expiration_year': fake_card_info.expiration_year,
        }
        if tsp_type == TSPType.MASTERCARD:
            args['holder_name'] = fake_card_info.holder
        return args

    @pytest.mark.asyncio
    @pytest.mark.parametrize('tsp_type,token_status',
                             [(TSPType.MASTERCARD, each) for each in MasterCardTokenStatus]
                             + [(TSPType.VISA, each) for each in VisaTokenStatus],
                             indirect=True)
    async def test_integration_handle(
        self,
        storage,
        worker,
        task,
        card_to_tokenize,
        token_status,
        fake_card_info,
        get_tsp_status,
        expected_enrollment_args,
        mock_trust_get_card,
        mock_cardproxy_enrollment,
    ):
        enrollments_before = await alist(storage.enrollment.find())
        assert_that(enrollments_before, equal_to([]))

        await worker.process_task()
        enrollment = await storage.enrollment.get_by_card_id_and_merchant_id(
            card_id=card_to_tokenize.card_id,
            merchant_id=None,
        )
        assert_that(
            enrollment,
            has_property(
                'tsp_token_status',
                get_tsp_status(token_status)
            ),
        )

        mock_trust_get_card.assert_awaited_once_with(
            trust_card_id=card_to_tokenize.trust_card_id
        )
        mock_cardproxy_enrollment.assert_awaited_once_with(
            **expected_enrollment_args
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('token_status', [INVALID_TOKEN_STATUS], indirect=True)
    @pytest.mark.parametrize('tsp_type', [t for t in TSPType if t != TSPType.UNKNOWN], indirect=True)
    async def test_cardproxy_client_returns_invalid_token_status(
        self,
        storage,
        worker,
        task,
        tsp_type,
        mock_trust_get_card,
        mock_cardproxy_enrollment,
        card_to_tokenize,
        fake_card_info,
        expected_enrollment_args,
    ):
        enrollments_before = await alist(storage.enrollment.find())
        assert_that(enrollments_before, equal_to([]))

        await worker.process_task()

        mock_trust_get_card.assert_awaited_once_with(
            trust_card_id=card_to_tokenize.trust_card_id
        )
        mock_cardproxy_enrollment.assert_awaited_once_with(
            **expected_enrollment_args
        )

        error = f'CardProxy {"Visa" if tsp_type == TSPType.VISA else "MasterCard"}' \
                f' returned invalid token status: {self.INVALID_TOKEN_STATUS}'
        task = await storage.tokenization_queue.get(task.task_id)
        expected_properties = {
            'details': {'retry_reason': f'InvalidTokenStatusError: {error}.'},
            'retries': 1,
            'state': TokenizationQueueState.PENDING,
        }
        assert_that(task, has_properties(expected_properties))

    @pytest.mark.asyncio
    @pytest.mark.parametrize('tsp_type', [t for t in TSPType if t != TSPType.UNKNOWN], indirect=True)
    async def test_trust_client_failure(self, storage, worker, task, mock_interaction_clients):
        mock_get_card_failure = AsyncMock(side_effect=BaseCoreError('Boom!'))
        mock_interaction_clients.return_value.trust_paysys.get_card = mock_get_card_failure

        enrollments_before = await alist(storage.enrollment.find())
        assert_that(enrollments_before, equal_to([]))

        await worker.process_task()

        enrollments_after = await alist(storage.enrollment.find())
        assert_that(enrollments_after, equal_to([]))

        task = await storage.tokenization_queue.get(task.task_id)
        expected_properties = {
            'state': TokenizationQueueState.PENDING,
            'retries': 1,
            'details': {'retry_reason': 'BaseCoreError: Boom!'},
        }
        assert_that(task, has_properties(expected_properties))

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('mock_trust_get_card')
    @pytest.mark.parametrize('tsp_type', [t for t in TSPType if t != TSPType.UNKNOWN], indirect=True)
    async def test_cardproxy_client_failure(self, storage, worker, task, mock_interaction_clients):
        mock_cardproxy_failure = AsyncMock(side_effect=BaseCoreError('Bang!'))
        mock_interaction_clients.return_value.cardproxy.mastercard_enrollment = mock_cardproxy_failure
        mock_interaction_clients.return_value.cardproxy.visa_enrollment = mock_cardproxy_failure

        enrollments_before = await alist(storage.enrollment.find())
        assert_that(enrollments_before, equal_to([]))

        await worker.process_task()

        enrollments_after = await alist(storage.enrollment.find())
        assert_that(enrollments_after, equal_to([]))

        task = await storage.tokenization_queue.get(task.task_id)
        expected_properties = {
            'state': TokenizationQueueState.PENDING,
            'retries': 1,
            'details': {'retry_reason': 'BaseCoreError: Bang!'},
        }
        assert_that(task, has_properties(expected_properties))
