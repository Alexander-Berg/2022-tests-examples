from datetime import datetime

import pytest
from mdh.core.exceptions import HttpIntegrationError
from mdh.core.integration.startrek import StartrekClient


def test_basic(response_mock, read_fixture):

    client = StartrekClient()

    bypass = False

    # Создаём задачу.
    with response_mock(
            'POST https://st-api.test.yandex-team.ru/v2/issues -> 200:'
            f"{read_fixture('st_create.json').decode()}",
            bypass=bypass
    ):

        issue = client.issue_create(
            queue='TESTMDH',
            summary=f'Title test {datetime.utcnow()}',
            description='**nice**',

            author='robot-refs-test',
            followers=['idlesign'],
            tags=['one', 'two'],
        )

        issue_key = issue['key']
        assert issue_key
        assert issue_key == str(issue)

    # Получаем данные существующей задачи.

    with response_mock(
        f'GET https://st-api.test.yandex-team.ru/v2/issues/{issue_key} -> 200 :'
        f"{read_fixture('st_get.json').decode()}",
        bypass=bypass
    ):
        issue = client.issue_get(issue_key)

        assert issue_key == issue['key']

    # Обновляем параметры задачи.

    with response_mock(
        f'PATCH https://st-api.test.yandex-team.ru/v2/issues/{issue_key} -> 200 :'
        f"{read_fixture('st_update.json').decode()}",
        bypass=bypass
    ):
        issue = client.issue_update(issue, description='changed', tags={'remove': ['two']})

        assert issue_key == issue['key']
        assert issue['tags'] == ['one']
        assert issue['description'] == 'changed'

    # Добавляем комментарий.

    with response_mock(
        f'POST https://st-api.test.yandex-team.ru/v2/issues/{issue_key}/comments -> 200 :'
        f"{read_fixture('st_comment.json').decode()}",
        bypass=bypass
    ):
        comment = client.issue_comment(issue, 'new comment')
        assert str(comment['id']) == str(comment)
        assert comment['id']
        assert comment['text'] == 'new comment'

    # Переход в другое состояние.

    with response_mock(
        f'POST https://st-api.test.yandex-team.ru/v2/issues/{issue_key}/transitions/dummy/_execute -> 404:'
        '{"errors":{},"errorMessages":["no transition"],"statusCode":404}',
        bypass=bypass
    ):
        with pytest.raises(HttpIntegrationError) as e:
            client.issue_transition(issue, state='dummy')

        assert e.value.message == 'no transition'

    with response_mock(
        f'POST https://st-api.test.yandex-team.ru/v2/issues/{issue_key}/transitions/need_info/_execute -> 200:'
        f"{read_fixture('st_transitions.json').decode()}",
        bypass=bypass
    ):
        transitions = client.issue_transition(issue, state='need_info', comment='for needinfo')
        assert len(transitions) > 0
        assert str(transitions[0])

    # проверка порождения исключения при 500+
    with response_mock(
        f'POST https://st-api.test.yandex-team.ru/v2/issues/{issue_key}/transitions/dummy/_execute -> 504:'
    ):
        with pytest.raises(HttpIntegrationError) as e:
            client.issue_transition(issue, state='dummy')

        assert e.value.message == 'status: 504'
