import pytest
from sentry_sdk import Hub

from bcl.core.models import Lock
from bcl.exceptions import UserHandledException, DailyTaskPostponed, FileFormatError
from bcl.toolbox.tasks import task
from bcl.toolbox.utils import capture_exception

sentry_last_sent = [None]


@pytest.fixture(autouse=True)
def mock_sentry_send():
    sentry_last_sent[0] = None

    class MockTransport:

        def capture_event(self, event: dict):
            sentry_last_sent[0] = {
                'message': event['exception']['values'][0]['value'],
            }

        def flush(self, *args, **kwargs):
            pass

        def kill(self):
            pass

    mock_transport = MockTransport()
    Hub.current.client.transport = mock_transport


def test_exception_text():

    try:
        raise ValueError('some text')

    except Exception:
        assert 'присвоили номер' in capture_exception()

    try:
        raise FileFormatError()

    except Exception:
        assert capture_exception() == 'Неподдерживаемый формат файла'


def test_sentry_send():
    try:
        raise Exception('MyException')

    except Exception:
        message = capture_exception(realm='my_realm')

    assert message.startswith('Возникла непредвиденная ошибка. Мы её зафиксировали и присвоили номер:')

    data = sentry_last_sent[0]

    assert data['message'] == 'MyException'


@pytest.mark.parametrize('exception_class', (UserHandledException, DailyTaskPostponed))
def test_filter_exceptions(exception_class):

    try:
        raise exception_class('MyException')

    except exception_class:
        message = capture_exception(realm='my_realm')

    assert message == 'MyException'


def test_task_exception():

    Lock(name='testtask').save()

    @task()
    def testtask(**task_kwargs):
        raise Exception('TaskException')

    testtask()

    data = sentry_last_sent[0]
    assert data['message'] == 'TaskException'
