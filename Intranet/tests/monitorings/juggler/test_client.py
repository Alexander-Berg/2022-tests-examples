import contextlib
import os
from unittest import mock

from idm.monitorings.juggler import JugglerEvent, JugglerStatus, JugglerClient
from idm.tests.utils import random_slug


def generate_event(status: JugglerStatus = None, **kwargs) -> JugglerEvent:
    defaults = {
        'service': random_slug(),
        'status': status or JugglerStatus.OK,
        'description': random_slug(),
    }
    defaults.update(kwargs)
    return JugglerEvent(**defaults)


@contextlib.contextmanager
def override_env(name: str, value: str):
    origin = os.getenv(name)
    os.environ[name] = value
    yield
    if origin is None:
        os.unsetenv(name)
    else:
        os.environ[name] = origin


def test_push_events():
    juggler_url = random_slug()
    app_name = random_slug()
    hostname = random_slug()
    deploy_unit = random_slug()

    client = JugglerClient(juggler_url, app_name)
    with mock.patch('requests.post') as post_mock, \
            override_env('PORTO_HOST', hostname), \
            override_env('DEPLOY_UNIT_ID', deploy_unit):
        events = [generate_event(status=status) for status in JugglerStatus]
        client.push_events(*events)

    post_mock.assert_called_once()
    assert post_mock.call_args.args == (juggler_url, )
    assert post_mock.call_args.kwargs['json']['source'] == app_name
    assert len(post_mock.call_args.kwargs['json']['events']) == len(events)
    for event_data, event in zip(post_mock.call_args.kwargs['json']['events'], events):
        assert event_data['service'] == event.service
        assert event_data['status'] == str(event.status)
        assert event_data['description'] == event.description
        assert event_data['host'] == hostname
        assert event_data['instance'] == deploy_unit

