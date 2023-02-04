import pytest

from intranet.vconf.src.call.call_template import Template
from intranet.vconf.src.call.models import Event

from intranet.vconf.tests.call.mock import get_next_event_mock
from intranet.vconf.tests.call.factories import CallTemplateFactory, EventFactory

pytestmark = pytest.mark.django_db


def test_update_not_existing_event():
    template = CallTemplateFactory()
    manager = Template(obj=template)
    data = get_next_event_mock()

    manager.update_event(data)
    event = Event.objects.get(id=data['master_id'])

    assert event.description == data['description']


def test_update_existing_event():
    template = CallTemplateFactory()
    manager = Template(obj=template)
    event = EventFactory()
    old_description = event.description
    template.next_event = event
    template.save()

    data = get_next_event_mock()
    data['master_id'] = event.id
    data['description'] = 'new text'
    manager.update_event(data)
    event.refresh_from_db()

    assert event.description != old_description
    assert event.description == data['description']
