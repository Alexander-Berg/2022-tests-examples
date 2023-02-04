from datetime import timedelta

import pytest

from maps_adv.geosmb.telegraphist.server.lib.templates import env
from maps_adv.geosmb.telegraphist.server.tests.helpers import (
    make_order,
    make_order_item,
)


@pytest.mark.parametrize(
    "overrides, expected",
    (
        [dict(), "Вы записались в «Кафе с едой» на 13:00, чт, 1 окт."],  # many items
        [  # without details link
            dict(order=make_order(items=[make_order_item()])),
            "Вы записались в «Кафе с едой» на 13:00, чт, 1 окт.",
        ],
        [  # with details link
            dict(details_link="https://butt-freezer.com"),
            "Вы записались в «Кафе с едой» на 13:00, чт, 1 окт. "
            "Отменить или перенести: https://butt-freezer.com",
        ],
        [  # with org phone
            dict(org=dict(name="Кафе с едой", phone="322223")),
            "Вы записались в «Кафе с едой» на 13:00, чт, 1 окт. "
            "Отменить или перенести: 322223",
        ],
        [  # with details link and org phone
            dict(
                details_link="https://butt-freezer.com",
                org=dict(name="Кафе с едой", phone="322223"),
            ),
            "Вы записались в «Кафе с едой» на 13:00, чт, 1 окт. "
            "Отменить или перенести: https://butt-freezer.com",
        ],
        [  # with org tz
            dict(org=dict(name="Кафе с едой", tz_offset=timedelta(hours=3))),
            "Вы записались в «Кафе с едой» на 16:00, чт, 1 окт.",
        ],
    ),
)
def test_order_created(overrides, expected):
    kwargs = {"org": {"name": "Кафе с едой"}, "order": make_order()}
    kwargs.update(overrides)

    result = env.get_template("order_created_sms.jinja").render(**kwargs)

    assert result == expected


@pytest.mark.parametrize(
    "overrides, expected",
    (
        [  # many items
            dict(),
            "Напоминаем, что вы записаны в «Кафе с едой» на 13:00, чт, 1 окт.",
        ],
        [  # without details link
            dict(order=make_order(items=[make_order_item()])),
            "Напоминаем, что вы записаны в «Кафе с едой» на 13:00, чт, 1 окт.",
        ],
        [  # with details link
            dict(details_link="https://butt-freezer.com"),
            "Напоминаем, что вы записаны в «Кафе с едой» на 13:00, чт, 1 окт. "
            "Отменить или перенести: https://butt-freezer.com",
        ],
        [  # with org phone
            dict(org=dict(name="Кафе с едой", phone="322223")),
            "Напоминаем, что вы записаны в «Кафе с едой» на 13:00, чт, 1 окт. "
            "Отменить или перенести: 322223",
        ],
        [  # with details link and org phone
            dict(
                order=make_order(items=[make_order_item()]),
                details_link="https://butt-freezer.com",
                org=dict(name="Кафе с едой", phone="322223"),
            ),
            "Напоминаем, что вы записаны в «Кафе с едой» на 13:00, чт, 1 окт. "
            "Отменить или перенести: https://butt-freezer.com",
        ],
        [  # with org tz
            dict(org=dict(name="Кафе с едой", tz_offset=timedelta(hours=3))),
            "Напоминаем, что вы записаны в «Кафе с едой» на 16:00, чт, 1 окт.",
        ],
    ),
)
def test_order_reminder(overrides, expected):
    kwargs = {"org": {"name": "Кафе с едой"}, "order": make_order()}
    kwargs.update(overrides)

    result = env.get_template("order_reminder_sms.jinja").render(**kwargs)

    assert result == expected


@pytest.mark.parametrize(
    "overrides, expected",
    (
        [  # many items
            dict(),
            "Время вашей записи в «Кафе с едой» было изменено. "
            "Ждем вас в 13:00, чт, 1 окт.",
        ],
        [  # without details link
            dict(order=make_order(items=[make_order_item()])),
            "Время вашей записи в «Кафе с едой» было изменено. "
            "Ждем вас в 13:00, чт, 1 окт.",
        ],
        [  # with details link
            dict(
                order=dict(items=[make_order_item()]),
                details_link="https://butt-freezer.com",
            ),
            "Время вашей записи в «Кафе с едой» было изменено. "
            "Ждем вас в 13:00, чт, 1 окт. "
            "Отменить или перенести: https://butt-freezer.com",
        ],
        [  # with org phone
            dict(org=dict(name="Кафе с едой", phone="322223")),
            "Время вашей записи в «Кафе с едой» было изменено. "
            "Ждем вас в 13:00, чт, 1 окт. "
            "Отменить или перенести: 322223",
        ],
        [  # with details link and org phone
            dict(
                details_link="https://butt-freezer.com",
                org=dict(name="Кафе с едой", phone="322223"),
            ),
            "Время вашей записи в «Кафе с едой» было изменено. "
            "Ждем вас в 13:00, чт, 1 окт. "
            "Отменить или перенести: https://butt-freezer.com",
        ],
        [  # with org tz
            dict(org=dict(name="Кафе с едой", tz_offset=timedelta(hours=3))),
            "Время вашей записи в «Кафе с едой» было изменено. "
            "Ждем вас в 16:00, чт, 1 окт.",
        ],
    ),
)
def test_order_changed(overrides, expected):
    kwargs = {"org": {"name": "Кафе с едой"}, "order": make_order()}
    kwargs.update(overrides)

    result = env.get_template("order_changed_sms.jinja").render(**kwargs)

    assert result == expected


@pytest.mark.parametrize(
    "overrides, expected",
    (
        [  # many items
            dict(),
            "Ваша запись в «Кафе с едой» на 13:00, чт, 1 окт. отменена",
        ],
        [  # with org tz
            dict(org=dict(name="Кафе с едой", tz_offset=timedelta(hours=3))),
            "Ваша запись в «Кафе с едой» на 16:00, чт, 1 окт. отменена",
        ],
    ),
)
def test_order_cancelled(overrides, expected):
    kwargs = {"org": {"name": "Кафе с едой"}, "order": make_order()}
    kwargs.update(overrides)

    result = env.get_template("order_cancelled_sms.jinja").render(**kwargs)

    assert result == expected


def test_request_created_for_business():
    result = env.get_template("request_created_for_business_sms.jinja").render(
        details_link="http://details.link",
    )

    assert result == "Новая заявка в Яндекс.Бизнесе http://details.link"
