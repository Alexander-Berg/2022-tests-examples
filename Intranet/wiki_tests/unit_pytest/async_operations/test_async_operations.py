import mock
import pytest

from intranet.wiki.tests.wiki_tests.common.utils import no_celery, celery_eager
from wiki.async_operations.consts import OperationType, Status, OperationOwner, OperationIdentity
from wiki.async_operations.exceptions import PreconditionsFailed, AlreadyRunning
from wiki.async_operations.models import TestTaskParams
from wiki.async_operations.operation_executors.debug_operation.debug_operation import TestAsyncOperation
from wiki.async_operations.progress_storage import ASYNC_OP_PROGRESS_STORAGE
from wiki.async_operations.tasks.execute_async_operation import drf_view_schedule_execution, execute_async_operation


@pytest.mark.django_db
def test_preconditions(client, wiki_users, test_org_id):
    ASYNC_OP_PROGRESS_STORAGE.clear()

    params = TestTaskParams(number=-1, sleep=False)
    owner = OperationOwner(org_inner_id=test_org_id, user_id=wiki_users.thasonic.id)
    my_async_op = TestAsyncOperation(params, owner)

    with pytest.raises(PreconditionsFailed):
        drf_view_schedule_execution(my_async_op)

    params.number = 1000
    my_async_op = TestAsyncOperation(params, owner)
    drf_view_schedule_execution(my_async_op)

    with pytest.raises(AlreadyRunning):
        drf_view_schedule_execution(my_async_op)


@pytest.mark.django_db
def test_task(client, wiki_users, test_org_id):
    ASYNC_OP_PROGRESS_STORAGE.clear()

    idx = OperationIdentity(id='zazaz', type=OperationType.TEST)
    count = 1000
    owner = OperationOwner(org_inner_id=test_org_id, user_id=wiki_users.thasonic.id)

    params = TestTaskParams(number=count, sleep=False)
    ASYNC_OP_PROGRESS_STORAGE.report_scheduled(idx, owner)

    with mock.patch('wiki.async_operations.progress_storage.ASYNC_OP_PROGRESS_STORAGE.report_progress') as m:
        execute_async_operation.apply(args=(OperationType.TEST.value, idx.id, params.dict(), owner.dict())).get()
        assert m.call_count == count + 1
        data = ASYNC_OP_PROGRESS_STORAGE.load(idx)
        assert data.status == Status.SUCCESS


@pytest.mark.django_db
def test_async_op_view(client, wiki_users, test_org_id):
    ASYNC_OP_PROGRESS_STORAGE.clear()
    client.login(wiki_users.thasonic)
    owner = OperationOwner(org_inner_id=test_org_id, user_id=wiki_users.thasonic.id)

    idx = OperationIdentity(id='zazaz', type=OperationType.TEST)
    request_url = f'/_api/frontend/.async_operations?id={idx.id}'
    response = client.get(request_url)
    assert response.status_code == 404

    ASYNC_OP_PROGRESS_STORAGE.report_scheduled(idx, owner)
    request_url = f'/_api/frontend/.async_operations?id={idx.id}'
    assert client.get(request_url).status_code == 200

    params = TestTaskParams(number=1000, sleep=False)
    execute_async_operation.apply(args=(OperationType.TEST.value, idx.id, params.dict(), owner.dict())).get()

    request_url = f'/_api/frontend/.async_operations?id={idx.id}'
    response = client.get(request_url)
    assert response.status_code == 200
    assert response.json()['data']['status'] == Status.SUCCESS


@pytest.mark.django_db
@no_celery
def test_async_op_in_progress(client, wiki_users):
    ASYNC_OP_PROGRESS_STORAGE.clear()

    client.login(wiki_users.thasonic)

    request_url = '/_api/frontend/.async_operations/test'
    response = client.post(request_url, json={'number': 1000})
    assert response.status_code == 200
    task_id = response.json()['data']['task_id']

    request_url = f'/_api/frontend/.async_operations?id={task_id}'
    response = client.get(request_url)
    assert response.status_code == 200
    assert response.json()['data']['status'] == Status.SCHEDULED

    # если поставить повторно
    request_url = '/_api/frontend/.async_operations/test'
    response = client.post(request_url, json={'number': 1000})
    assert response.status_code == 400
    data = response.json()
    assert data['error']['error_code'] == 'ALREADY_RUNNING'
    assert data['error']['details']['task_id'] == task_id


@pytest.mark.django_db
@no_celery
def test_async_op_api_v2(client, wiki_users):
    ASYNC_OP_PROGRESS_STORAGE.clear()

    client.login(wiki_users.thasonic)
    request_url = '/api/v2/public/operations/counter'

    response = client.post(request_url, data={'number': 1000})
    assert response.status_code == 200

    status_url = response.json()['status_url']

    response = client.get(status_url)
    assert response.json()['status'] == Status.SCHEDULED

    # пользователь не читает чужой прогресс
    client.login(wiki_users.asm)
    response = client.get(status_url)
    assert response.status_code == 403

    client.login(wiki_users.thasonic)
    response = client.post(request_url, data={'number': 1000})
    assert response.status_code == 400
    assert response.json()['error_code'] == 'ALREADY_RUNNING'


@pytest.mark.django_db
@celery_eager
def test_async_op_api_v2_run_task(client, wiki_users):
    ASYNC_OP_PROGRESS_STORAGE.clear()

    client.login(wiki_users.thasonic)
    request_url = '/api/v2/public/operations/counter'

    response = client.post(request_url, data={'number': 1000})
    assert response.status_code == 200

    status_url = response.json()['status_url']

    response = client.get(status_url)
    assert response.json()['status'] == Status.SUCCESS
    assert response.json()['result'] == {'number': 1000}
