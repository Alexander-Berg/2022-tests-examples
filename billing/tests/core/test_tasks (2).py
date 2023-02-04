import pytest
from django.db.utils import DatabaseError

from mdh.core.models import Record, Schema, STATUS_PUBLISHED, STATUS_ON_REVIEW, STATUS_NOMINATED, STATUS_DRAFT, Batch
from mdh.core.tasks import send_messages, cleanup_messages


def test_startrek_send_fail(run_task, init_user, init_resource, response_mock, monkeypatch):
    user = init_user()
    resource = init_resource(user=user)

    record = Record.objects.create(creator=user, **{
        'schema': resource.schema,
        'resource': resource,
        'attrs': {'integer1': 2},
        'status': STATUS_NOMINATED,
    })

    counter = [0]

    save_base = Record.save_base

    def flaky_save(self, **kwargs):
        counter[0] += 1

        # проверим обработку исключений на разных уровнях: при сохранении в StartrekIssue.create()
        # и в WithBatchHandling._batch_wrapper().
        if counter[0] in (1, 3):
            raise DatabaseError('db flapping')

        return save_base(self, **kwargs)

    with response_mock('POST https://st-api.test.yandex-team.ru/v2/issues -> 200:{"key": "TESTMDH-77"}'):
        monkeypatch.setattr(Record, 'save_base', flaky_save)
        run_task('startrek_send_record')

    record.refresh_from_db()
    # проверим, что возвращённый номер задачи не потерялся а прописан, и статус переведён.
    assert record.issue == 'TESTMDH-77'
    assert record.is_on_review


@pytest.mark.parametrize('model_cls', [Record, Schema, Batch])
def test_startrek_send(model_cls, run_task, init_user, init_resource, response_mock):

    user = init_user()

    task_map = {
        Record: 'startrek_send_record',
        Schema: 'startrek_send_schema',
        Batch: 'startrek_send_batch',
    }

    kwargs = {}

    is_record = model_cls is Record

    if is_record:
        resource = init_resource(user=user)

        kwargs = {
            'schema': resource.schema,
            'resource': resource,
            'attrs': {'integer1': 2},
        }

    idx_tracker_bogus = 3
    idx_draft = 2

    def spawn_obj(idx):
        if model_cls is Schema:
            kwargs['alias'] = f'schema{idx}'

        kwargs['status'] = STATUS_DRAFT if idx == idx_draft else STATUS_NOMINATED

        return model_cls.objects.create(creator=user, **kwargs)

    objects = {idx: spawn_obj(idx).id for idx in range(1, 5)}
    responses = []

    for idx, key in objects.items():
        if idx == idx_draft:
            continue

        body = f'{{"key": "TESTMDH-{key}"}}'
        status = 200
        if idx == idx_tracker_bogus:
            body = ''
            status = 504

        responses.append(f'POST https://st-api.test.yandex-team.ru/v2/issues -> {status} :{body}')

    bypass = False

    task_name = task_map[model_cls]

    with response_mock(responses, bypass=bypass):
        run_task(task_name)

    for idx, obj in enumerate(model_cls.objects.order_by('id').all(), 1):

        if idx == idx_draft:
            # не было попытки отправить в трекер
            assert obj.issue == ''
            assert obj.is_draft

        elif idx == idx_tracker_bogus:
            # при отправке трекер ответил ошибкой
            assert obj.issue == ''
            assert obj.is_nominated

        else:
            # отправка в трекер прошла успешно
            assert obj.issue == f'TESTMDH-{obj.id}'
            assert obj.is_on_review


@pytest.mark.parametrize('model_cls', [Record, Schema, Batch])
def test_startrek_sync(model_cls, run_task, init_user, init_resource, response_mock):

    user = init_user()

    task_map = {
        Record: 'startrek_sync_record',
        Schema: 'startrek_sync_schema',
        Batch: 'startrek_sync_batch',
    }

    kwargs = {}

    is_record = model_cls is Record

    if is_record:
        resource = init_resource(user=user)

        kwargs = {
            'schema': resource.schema,
            'resource': resource,
            'attrs': {'integer1': 2},
        }

    idx_published = 2

    for idx in range(1, 7):

        if model_cls is Schema:
            kwargs['alias'] = f'schema{idx}'

        kwargs['status'] = STATUS_PUBLISHED if idx == idx_published else STATUS_ON_REVIEW

        model_cls.objects.create(issue=f'TESTMDH-{idx}', creator=user, **kwargs)

    responses = [
        f'GET https://st-api.test.yandex-team.ru/v2/issues/{key} -> {data[0]} :{data[1]}'
        for key, data in
        {
            'TESTMDH-1': (200, '{"status": {"key": "approved"}}'),
            'TESTMDH-3': (200, '{"status": {"key": "new"}}'),
            'TESTMDH-4': (200, '{"status": {"key": "approved"}}'),
            'TESTMDH-5': (504, ''),  # сбойная не должна мешать обновлению следующей
            'TESTMDH-6': (200, '{"status": {"key": "approved"}}'),

        }.items()
    ]

    bypass = False

    with response_mock(responses, bypass=bypass):
        run_task(task_map[model_cls])

    items = {obj.issue: obj for obj in model_cls.objects.all()}
    assert items['TESTMDH-1'].is_approved
    assert items['TESTMDH-2'].is_published
    assert items['TESTMDH-3'].is_on_review
    assert items['TESTMDH-4'].is_approved
    assert items['TESTMDH-5'].is_on_review
    assert items['TESTMDH-6'].is_approved


def test_send_messages(run_task):
    send_messages(0)


def test_cleanup_messages(run_task):
    cleanup_messages(0)


def test_stats(run_task):
    run_task('send_stats')


def test_publish(run_task):
    run_task('publish_record')
    run_task('publish_schema')
    run_task('publish_batch')


def test_queue(run_task):
    assert run_task('process_queue')


def test_cleanup(run_task):
    assert run_task('cleanup')
