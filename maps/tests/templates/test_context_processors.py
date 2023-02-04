from datetime import timedelta
from decimal import Decimal

import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.telegraphist.server.lib.templates import (
    TEMPLATES_BY_FIELDS_TO_FORMAT,
    email_context_processor,
)

TEMPLATES = [
    "order_created",
    "order_changed",
    "order_cancelled",
    "order_reminder",
    "certificate_connect_payment",
    "certificate_rejected",
    "subsequent_certificate_approved",
    "first_certificate_approved",
    "certificate_expired",
    "certificate_expiring",
    "order_created_for_business",
    "order_changed_for_business",
    "order_cancelled_for_business",
    "certificate_purchased",
    "certificate_created",
    "request_created_for_business",
]


def template_context(**updates):
    ctx = {
        "org": {
            "name": "Кафе",
            "phone": "+7 (495) 739-70-00",
            "url": "http://cafe.ru",
            "categories": ["Общепит", "Ресторан", "Посольство РФ"],
            "tz_offset": timedelta(seconds=10800),
        },
        "order": {
            "booking_code": "booking_code_1",
            "items": [
                {
                    "name": "Охлаждение жепы",
                    "booking_timestamp": dt("2020-10-01 13:00:00"),
                    "employee_name": "Веселый Эдик",
                    "cost": {"final_cost": Decimal("6550.50")},
                },
                {
                    "name": "Стрижка бровей",
                    "booking_timestamp": dt("2020-10-02 22:00:00"),
                    "employee_name": "Грустный Саня",
                    "cost": {
                        "final_cost": Decimal("50.50"),
                        "cost_before_discounts": Decimal("100"),
                        "discount": {
                            "percent": Decimal("12.2"),
                            "value": Decimal("50.1"),
                        },
                    },
                },
            ],
            "total_cost": {
                "final_cost": Decimal("100.50"),
                "cost_before_discounts": Decimal("20000"),
                "discount": {"percent": Decimal("14.4"), "value": Decimal("100.1")},
            },
        },
        "client": {
            "client_id": 160,
            "name": "Уточка говорит мяу",
            "phone": 88002000600,
        },
        "certificate": {
            "name": "Скидочка на стрижечку жепы",
            "link": "http://certificate.link",
            "sales": Decimal("1200.50"),
            "price": Decimal("12800.3"),
            "validity_period": {
                "valid_from": dt("2020-11-25 18:00:00"),
                "valid_to": dt("2021-01-25 18:00:00"),
            },
            "discount": Decimal("15.2"),
        },
    }
    ctx.update(updates)

    return ctx


@pytest.mark.parametrize(
    "template_name",
    [t for t in TEMPLATES if t in TEMPLATES_BY_FIELDS_TO_FORMAT["order"]],
)
@pytest.mark.parametrize("cost_type", ["final_cost", "minimal_cost"])
@pytest.mark.parametrize(
    "cost, expected_cost",
    [(Decimal("0"), "0&nbsp;₽"), (Decimal("6550.50"), "6&nbsp;550.50&nbsp;₽")],
)
def test_processes_order_total_cost_for_respective_templates(
    template_name, cost, expected_cost, cost_type
):
    result = email_context_processor(
        template_name,
        template_context(
            order={
                "booking_code": "booking_code_1",
                "items": [
                    {
                        "name": "Охлаждение жепы",
                        "booking_timestamp": dt("2020-10-01 13:00:00"),
                        "employee_name": "Веселый Эдик",
                        "cost": {"final_cost": Decimal("6550.50")},
                    },
                ],
                "total_cost": {
                    cost_type: cost,
                    "cost_before_discounts": Decimal("20000"),
                    "discount": {"percent": Decimal("14.4"), "value": Decimal("100.1")},
                },
            }
        ),
    )

    assert result["order"]["total_cost"] == {
        cost_type: expected_cost,
        "cost_before_discounts": "20&nbsp;000&nbsp;₽",
        "discount": {"percent": "14.4%", "value": "100.10&nbsp;₽"},
    }


@pytest.mark.parametrize(
    "template_name",
    [t for t in TEMPLATES if t in TEMPLATES_BY_FIELDS_TO_FORMAT["order"]],
)
@pytest.mark.parametrize("cost_type", ["final_cost", "minimal_cost"])
def test_processes_order_items_cost_for_respective_templates(template_name, cost_type):
    result = email_context_processor(
        template_name,
        template_context(
            order={
                "booking_code": "booking_code_1",
                "items": [
                    {
                        "name": "Охлаждение жепы",
                        "booking_timestamp": dt("2020-10-01 13:00:00"),
                        "employee_name": "Веселый Эдик",
                        "cost": {cost_type: Decimal("6550.50")},
                    },
                    {
                        "name": "Стрижка бровей",
                        "booking_timestamp": dt("2020-10-02 22:00:00"),
                        "employee_name": "Грустный Саня",
                        "cost": {
                            cost_type: Decimal("0"),
                            "cost_before_discounts": Decimal("100"),
                            "discount": {
                                "percent": Decimal("12.2"),
                                "value": Decimal("50.1"),
                            },
                        },
                    },
                ],
                "total_cost": {
                    "final_cost": Decimal("6550.50"),
                    "cost_before_discounts": Decimal("20000"),
                    "discount": {"percent": Decimal("14.4"), "value": Decimal("100.1")},
                },
            }
        ),
    )

    assert result["order"]["items"][0]["cost"] == {cost_type: "6&nbsp;550.50&nbsp;₽"}
    assert result["order"]["items"][1]["cost"] == {
        cost_type: "0&nbsp;₽",
        "cost_before_discounts": "100&nbsp;₽",
        "discount": {"percent": "12.2%", "value": "50.10&nbsp;₽"},
    }


@pytest.mark.parametrize(
    "template_name",
    [t for t in TEMPLATES if t in TEMPLATES_BY_FIELDS_TO_FORMAT["order"]],
)
@pytest.mark.parametrize(
    ("org_tz_offset", "expected_value"),
    [
        (timedelta(seconds=0), "13:00, чт, 1 октября"),
        (timedelta(seconds=10800), "16:00, чт, 1 октября"),
        (None, "13:00, чт, 1 октября"),
    ],
)
def test_adds_earliest_order_booking_for_respective_templates(
    template_name, org_tz_offset, expected_value
):
    context = template_context()
    context["org"]["tz_offset"] = org_tz_offset

    result = email_context_processor(template_name, context)

    assert result["order"]["earliest_order_booking"] == expected_value


@pytest.mark.parametrize(
    "template_name",
    [t for t in TEMPLATES if t in TEMPLATES_BY_FIELDS_TO_FORMAT["order"]],
)
def test_processes_order_items_booking_timestamp_for_respective_templates(
    template_name,
):
    result = email_context_processor(template_name, template_context())

    assert (
        result["order"]["items"][0]["booking_timestamp"]
        == "16:00, четверг, 1&nbsp;октября 2020&nbsp;г."
    )
    assert (
        result["order"]["items"][1]["booking_timestamp"]
        == "01:00, суббота, 3&nbsp;октября 2020&nbsp;г."
    )


@pytest.mark.parametrize(
    "template_name",
    [t for t in TEMPLATES if t not in TEMPLATES_BY_FIELDS_TO_FORMAT["order"]],
)
def test_does_not_process_order_if_not_intended_by_template(template_name):
    result = email_context_processor(template_name, template_context())

    assert result["order"] == {
        "booking_code": "booking_code_1",
        "items": [
            {
                "name": "Охлаждение жепы",
                "booking_timestamp": dt("2020-10-01 13:00:00"),
                "employee_name": "Веселый Эдик",
                "cost": {"final_cost": Decimal("6550.50")},
            },
            {
                "name": "Стрижка бровей",
                "booking_timestamp": dt("2020-10-02 22:00:00"),
                "employee_name": "Грустный Саня",
                "cost": {
                    "final_cost": Decimal("50.50"),
                    "cost_before_discounts": Decimal("100"),
                    "discount": {
                        "percent": Decimal("12.2"),
                        "value": Decimal("50.1"),
                    },
                },
            },
        ],
        "total_cost": {
            "final_cost": Decimal("100.50"),
            "cost_before_discounts": Decimal("20000"),
            "discount": {"percent": Decimal("14.4"), "value": Decimal("100.1")},
        },
    }


@pytest.mark.parametrize(
    "template_name",
    [t for t in TEMPLATES if t in TEMPLATES_BY_FIELDS_TO_FORMAT["certificate"]],
)
def test_processes_certificate_sales_for_respective_templates(template_name):
    result = email_context_processor(template_name, template_context())

    assert result["certificate"] == {
        "name": "Скидочка на стрижечку жепы",
        "link": "http://certificate.link",
        "sales": "1&nbsp;200.50&nbsp;₽",
        "price": "12&nbsp;800.30&nbsp;₽",
        "validity_period": {
            "valid_from": "25 ноября 2020г.",
            "valid_to": "25 января 2021г.",
        },
        "discount": "15.2%",
    }


@pytest.mark.parametrize(
    "template_name",
    [t for t in TEMPLATES if t not in TEMPLATES_BY_FIELDS_TO_FORMAT["certificate"]],
)
def test_does_not_process_certificate_if_not_intended_by_template(template_name):
    result = email_context_processor(template_name, template_context())

    assert result["certificate"] == {
        "name": "Скидочка на стрижечку жепы",
        "link": "http://certificate.link",
        "sales": Decimal("1200.50"),
        "price": Decimal("12800.3"),
        "validity_period": {
            "valid_from": dt("2020-11-25 18:00:00"),
            "valid_to": dt("2021-01-25 18:00:00"),
        },
        "discount": Decimal("15.2"),
    }


@pytest.mark.parametrize(
    "template_name",
    [t for t in TEMPLATES if t in TEMPLATES_BY_FIELDS_TO_FORMAT["client"]],
)
@pytest.mark.parametrize(
    "client_data, expected_client_data",
    [
        (
            dict(name="Иванов", phone=78002000600),
            dict(name="Иванов", phone="+7 (800) 200-06-00"),
        ),
        (
            dict(name="Васятка", phone=88002000600),
            dict(name="Васятка", phone="8 (800) 200-06-00"),
        ),
        (
            dict(name="Васятка Иванов", phone=None),
            dict(name="Васятка Иванов", phone="-"),
        ),
    ],
)
def test_processes_client_phone_as_expected_for_respective_templates(
    template_name, client_data, expected_client_data
):
    result = email_context_processor(
        template_name, template_context(client=client_data)
    )

    assert result["client"] == expected_client_data


@pytest.mark.parametrize(
    "template_name",
    [t for t in TEMPLATES if t not in TEMPLATES_BY_FIELDS_TO_FORMAT["client"]],
)
def test_does_not_process_client_data_if_not_intended_by_template(template_name):
    result = email_context_processor(template_name, template_context())

    assert result["client"] == {
        "client_id": 160,
        "name": "Уточка говорит мяу",
        "phone": 88002000600,
    }


@pytest.mark.parametrize("template_name", TEMPLATES)
def test_processes_org_tz_offset_for_all_templates(template_name):
    result = email_context_processor(template_name, template_context())

    assert "tz_offset" not in result["org"]


@pytest.mark.parametrize(
    "template_name",
    [t for t in TEMPLATES if t in TEMPLATES_BY_FIELDS_TO_FORMAT["org"]],
)
def test_processes_org_links_for_respective_templates(template_name):
    result = email_context_processor(template_name, template_context())

    assert result["org"]["url_stripped"] == "cafe.ru"


@pytest.mark.parametrize(
    "template_name",
    [t for t in TEMPLATES if t in TEMPLATES_BY_FIELDS_TO_FORMAT["org"]],
)
def test_processed_org_categories_for_respective_templates(template_name):
    result = email_context_processor(template_name, template_context())

    assert result["org"]["categories"] == "Общепит, ресторан, посольство РФ"


@pytest.mark.parametrize(
    "template_name",
    [t for t in TEMPLATES if t not in TEMPLATES_BY_FIELDS_TO_FORMAT["org"]],
)
def test_does_not_processes_org_data_if_not_intended_by_template(template_name):
    result = email_context_processor(template_name, template_context())

    assert result["org"] == {
        "name": "Кафе",
        "phone": "+7 (495) 739-70-00",
        "url": "http://cafe.ru",
        "categories": ["Общепит", "Ресторан", "Посольство РФ"],
    }


@pytest.mark.parametrize("template_name", TEMPLATES)
def test_not_modifies_input_data_for_all_templates(template_name):
    context = template_context()
    email_context_processor(template_name, template_context())

    assert context["order"]["total_cost"] == {
        "final_cost": Decimal("100.50"),
        "cost_before_discounts": Decimal("20000"),
        "discount": {"percent": Decimal("14.4"), "value": Decimal("100.1")},
    }
    assert context["order"]["items"][1]["cost"] == {
        "final_cost": Decimal("50.50"),
        "cost_before_discounts": Decimal("100"),
        "discount": {"percent": Decimal("12.2"), "value": Decimal("50.1")},
    }
    assert context["order"]["items"][0]["booking_timestamp"] == dt(
        "2020-10-01 13:00:00"
    )
    assert "tz_offset" in context["org"]
    assert "url_stripped" not in context["org"]
    assert context["org"]["categories"] == [
        "Общепит",
        "Ресторан",
        "Посольство РФ",
    ]
    assert "earliest_order_booking" not in context["order"]
    assert context["client"] == {
        "client_id": 160,
        "name": "Уточка говорит мяу",
        "phone": 88002000600,
    }
    assert context["certificate"] == {
        "name": "Скидочка на стрижечку жепы",
        "link": "http://certificate.link",
        "sales": Decimal("1200.50"),
        "price": Decimal("12800.3"),
        "validity_period": {
            "valid_from": dt("2020-11-25 18:00:00"),
            "valid_to": dt("2021-01-25 18:00:00"),
        },
        "discount": Decimal("15.2"),
    }
