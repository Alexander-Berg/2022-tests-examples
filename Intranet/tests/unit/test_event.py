import pytest

from intranet.domenator.src.db.event.models import Event as EventModel
from intranet.domenator.src.logic.event import Event, save_event
from intranet.domenator.src.settings import config

pytestmark = pytest.mark.asyncio


async def test_save_event(client):
    config.events_saving_enabled = True

    event = Event(name='event1', data={
        'key1': 'value1',
        'key2': 'value2',
    })
    await save_event(event)

    model: EventModel = await EventModel.query.where(EventModel.name == event.name).gino.first_or_404()
    assert model.data == event.data


async def test_event_not_saved_when_saving_disabled(client):
    config.events_saving_enabled = False

    event = Event(name='event1', data={})
    await save_event(event)

    model: EventModel = await EventModel.query.where(EventModel.name == event.name).gino.first()
    assert model is None
