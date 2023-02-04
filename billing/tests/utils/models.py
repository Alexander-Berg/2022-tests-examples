import contextlib

from django.conf import settings
from django.urls import reverse

from rest_framework import status as http_statuses

from billing.dcsaap.backend.core.models import Check, Diff, Run, Webhook

__all__ = ['create_check', 'create_diff', 'create_run', 'create_webhook', 'mock_webhook_choices', 'create_run_via_http']


def create_check(
    title,
    cluster,
    table1,
    table2,
    keys,
    columns,
    result,
    status=Check.STATUS_ENABLED,
    login=settings.YAUTH_TEST_USER,
    aa_workflow_id=None,
    aa_instance_id=None,
    code=None,
    is_sox=False,
    diffs_count_limit=1_000_000,
) -> Check:
    return Check.objects.create(
        code=code,
        title=title,
        cluster=cluster,
        table1=table1,
        table2=table2,
        keys=keys,
        columns=columns,
        result=result,
        status=status,
        created_login=login,
        changed_login=login,
        workflow_id='yyyy',
        instance_id='xxxx',
        aa_workflow_id=aa_workflow_id,
        aa_instance_id=aa_instance_id,
        is_sox=is_sox,
        diffs_count_limit=diffs_count_limit,
    )


def create_run(check, result=None, status=None, type_=Run.TYPE_PROD) -> Run:
    kwargs = {'check_model': check, 'type': type_}
    if result is not None:
        kwargs['result'] = result
    if status is not None:
        kwargs['status'] = status
    return Run.objects.create(**kwargs)


def create_webhook(event_name, url=None, tvm_id=None) -> Webhook:
    if tvm_id is None:
        tvm_id = int(settings.YAUTH_TVM2_CLIENT_ID)
    if url is None:
        url = 'https://a.b.c.d.yandex.ru/'
    return Webhook.objects.create(event_name=event_name, url=url, tvm_id=tvm_id)


def create_diff(
    run, k1_name, k1_value, v_name, v1, v2, status=Diff.STATUS_NEW, diff_type=Diff.TYPE_DIFF, issue_key=None
) -> Diff:
    return Diff.objects.create(
        run=run,
        key1_name=k1_name,
        key1_value=k1_value,
        column_name=v_name,
        column_value1=v1,
        column_value2=v2,
        status=status,
        type=diff_type,
        issue_key=issue_key,
    )


def create_run_via_http(client, check, result=None, type=Run.TYPE_TEST, status=Run.STATUS_STARTED):
    """
    Запускает сверку через HTTP API (имитация нажатия кнопки Run в UI)
    """
    values = (
        ('check_model', check.id),
        ('type', type),
        ('status', status),
    )

    new_run = {}
    for k, v in values:
        new_run[k] = v

    if result is not None:
        new_run['result'] = result

    response = client.post(reverse('run-list'), new_run, format='json')
    assert response.status_code == http_statuses.HTTP_201_CREATED, response.content
    return response.data


@contextlib.contextmanager
def mock_webhook_choices(choices):
    """
    Небольшой контекст-менеджер для переопределения списка возможных значений
      поля `event_name` модели `Webhook`.

    Не удалось сделать:
      - через mock: потому что нужно патчить атрибут уже созданного поля
      - через переопределение `settings`: потому что choices кешируются при создании поля
      - через дополнительное поле в сериализаторе: потому что сериализатор не умеет
          в кастомную валидацию полей типа `ChoiceField` и так же кеширует
          возможные значения `choices`
    """
    field = getattr(Webhook, '_meta').get_field('event_name')
    original_choices = field.choices
    field.choices = list(zip(choices, choices))
    try:
        yield
    finally:
        field.choices = original_choices
