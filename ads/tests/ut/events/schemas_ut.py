import collections
import pytest

from ads.watchman.timeline.api.lib.modules.events import schemas, models
from ads.watchman.timeline.api.tests import helpers


NOT_VALID_EVENT_PATCHES = collections.OrderedDict([
    (u"start_time_does_not_exist", lambda d: {k: v for k, v in d.iteritems() if k != u'start_time'}),
    (u"end_time_does_not_exist", lambda d: {k: v for k, v in d.iteritems() if k != u'end_time'}),
    (u"start_time_is_not_datetime", lambda d: helpers.update_attr(d, u'start_time', 1)),
    (u"end_time_is_not_datetime", lambda d: helpers.update_attr(d, u'end_time', 1)),
    (u"end_time_less_then_start_time", lambda d: helpers.update_attr(helpers.update_attr(d, u'end_time', u"2017-10-13T18:00:04"), u'start_time', u"2017-10-13T18:00:05"))
])


EVENT_SCHEMAS = [
    (schemas.FiascoEventSchema(strict=True), models.EventType.fiasco),
    (schemas.FiascoEventWithIdSchema(strict=True), models.EventType.fiasco),
    (schemas.HolidayEventSchema(strict=True), models.EventType.holiday),
    (schemas.HolidayEventWithIdSchema(strict=True), models.EventType.holiday)
]

EVENT_SCHEMAS_IDS = [s.__class__.__name__ for s, _ in EVENT_SCHEMAS]


@pytest.mark.parametrize(u"schema,event_type", EVENT_SCHEMAS, ids=EVENT_SCHEMAS_IDS)
def test_event_schemas(schema, event_type):
    event = helpers.model_generators.TestModelGenerator.create_event(event_type=event_type)
    event_serialized, errors_serialized = schema.dump(event)
    event_deserealized, errors_deserealized = schema.load(event_serialized)

    assert event == event_deserealized
    assert errors_deserealized == {}
    assert errors_serialized == {}


@pytest.mark.parametrize(u"patch", NOT_VALID_EVENT_PATCHES.values(), ids=NOT_VALID_EVENT_PATCHES.keys())
def test_FiascoEventSchema_returns_errors_if_not_valid_event(patch):
    event_serialized = schemas.FiascoEventSchema(strict=True).dump(helpers.model_generators.TestModelGenerator.create_event(event_type=models.EventType.fiasco)).data
    event_serialized = patch(event_serialized)
    _, errors = schemas.FiascoEventSchema().load(event_serialized)
    assert errors != {}


# TODO: add tests for ResponseTimelineSchema and unify with other Schema testing
