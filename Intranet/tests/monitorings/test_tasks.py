import random
from unittest import mock

import pytest

from idm.monitorings.juggler import JugglerStatus
from idm.monitorings.juggler.checks import JugglerCheck, JugglerEvent
from idm.monitorings.tasks import PushJugglerEvents
from idm.tests.utils import random_slug


def test_push_juggler_events():
    with mock.patch('idm.monitorings.juggler.checks.ACTIVE_CHECKS', new=[mock.MagicMock(spec=JugglerCheck)]) as \
            active_checks_mock, \
            mock.patch('idm.monitorings.juggler.client.JugglerClient.push_events') as juggler_client_push_mock:
        fake_events = []
        for check in active_checks_mock:
            check.name = random_slug()
            event = JugglerEvent(service=random_slug(), status=random.choice(list(JugglerStatus)))
            check.get_event.return_value = event
            fake_events.append(event)

        PushJugglerEvents.run()

    juggler_client_push_mock.assert_called_once_with(*fake_events)


def test_push_juggler_events__no_check():
    with mock.patch('idm.monitorings.juggler.checks.ACTIVE_CHECKS', new=[]), \
            mock.patch('idm.monitorings.juggler.client.JugglerClient.push_events') as juggler_client_push_mock, \
            pytest.raises(RuntimeError):

        PushJugglerEvents.init()

    juggler_client_push_mock.assert_not_called()
