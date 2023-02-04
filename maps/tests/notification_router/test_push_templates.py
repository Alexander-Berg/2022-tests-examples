from datetime import timedelta

import pytest

from maps_adv.geosmb.telegraphist.server.lib.templates import push_env
from maps_adv.geosmb.telegraphist.server.tests.helpers import make_order


@pytest.mark.parametrize(
    "overrides, expected",
    (
        [  # without org tz
            dict(org=dict(name="Кафе с едой", tz_offset=None)),
            "Ваша запись в «Кафе с едой» на 13:00, чт, 1 окт. отменена",
        ],
        [  # with org tz
            dict(org=dict(name="Кафе с едой", tz_offset=timedelta(hours=3))),
            "Ваша запись в «Кафе с едой» на 16:00, чт, 1 окт. отменена",
        ],
        [
            dict(),
            "Ваша запись в «Кафе с едой» на 13:00, чт, 1 окт. отменена",
        ],  # many items
    ),
)
def test_order_cancelled(overrides, expected):
    kwargs = {"org": {"name": "Кафе с едой"}, "order": make_order()}
    kwargs.update(overrides)

    result = push_env.get_template("order_cancelled_push.jinja").render(**kwargs)

    assert result == expected


def test_order_cnanged():
    kwargs = {"org": {"name": "Кафе с едой"}, "order": make_order()}

    result = push_env.get_template("order_changed_push.jinja").render(**kwargs)

    assert result == "Время вашей записи в «Кафе с едой» было изменено"


@pytest.mark.parametrize(
    "overrides, expected",
    (
        [  # without org tz
            dict(org=dict(name="Кафе с едой", tz_offset=None)),
            "«Кафе с едой» ждёт вас в 13:00, чт, 1 окт.",
        ],
        [  # with org tz
            dict(org=dict(name="Кафе с едой", tz_offset=timedelta(hours=3))),
            "«Кафе с едой» ждёт вас в 16:00, чт, 1 окт.",
        ],
        [dict(), "«Кафе с едой» ждёт вас в 13:00, чт, 1 окт."],  # many items
    ),
)
def test_order_created(overrides, expected):
    kwargs = {"org": {"name": "Кафе с едой"}, "order": make_order()}
    kwargs.update(overrides)

    result = push_env.get_template("order_created_push.jinja").render(**kwargs)

    assert result == expected


@pytest.mark.parametrize(
    "overrides, expected",
    (
        [  # without org tz
            dict(org=dict(name="Кафе с едой", tz_offset=None)),
            "Вы записаны в «Кафе с едой» на 13:00, чт, 1 окт.",
        ],
        [  # with org tz
            dict(org=dict(name="Кафе с едой", tz_offset=timedelta(hours=3))),
            "Вы записаны в «Кафе с едой» на 16:00, чт, 1 окт.",
        ],
        [dict(), "Вы записаны в «Кафе с едой» на 13:00, чт, 1 окт."],  # many items
    ),
)
def test_order_reminder(overrides, expected):
    kwargs = {"org": {"name": "Кафе с едой"}, "order": make_order()}
    kwargs.update(overrides)

    result = push_env.get_template("order_reminder_push.jinja").render(**kwargs)

    assert result == expected
