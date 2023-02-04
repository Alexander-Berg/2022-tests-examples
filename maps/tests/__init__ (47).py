from maps_adv.common.helpers import AsyncIterator, dt

__all__ = [
    "make_import_events_generator",
    "make_import_event_data",
    "make_single_import_event_generator",
]


def make_import_events_generator(import_events) -> AsyncIterator:
    return AsyncIterator([import_events])()


def make_import_event_data(**overrided) -> list:
    data = dict(
        biz_id=123,
        event_type="REVIEW",
        event_timestamp=dt("2020-01-01 00:00:00"),
        data_source="MOBILE",
        user_nickname="ivan45",
        passport_uid="11",
        device_id="111",
        yandex_uid="1111",
        event_value="5",
        event_amount=2,
        source="EXTERNAL_ADVERT",
    )
    data.update(**overrided)

    return list(data.values())


def make_single_import_event_generator(**event_data) -> AsyncIterator:
    events_batch = [make_import_event_data(**event_data)]
    return AsyncIterator([events_batch])()
