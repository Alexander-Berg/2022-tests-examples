import logging
import os

import pytest
from smb.common.testing_utils import dt

from maps_adv.common.email_sender import EmailSenderError, MailingListSource

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.mock_dm,
    pytest.mark.freeze_time("2020-02-02 12:00:00"),
]


@pytest.fixture(autouse=True)
def caplog_set_level_error(caplog):
    caplog.set_level(logging.ERROR)


@pytest.fixture(autouse=True)
def common_mocks(dm, email_client):
    dm.list_unprocessed_email_messages.coro.return_value = []
    dm.mark_messages_processed.coro.return_value = None

    email_client.schedule_promo_campaign.coro.return_value = {
        "id": 111,
        "slug": "GUG2RFM2-MV51",
        "campaign_is_scheduled": True,
    }


@pytest.fixture(autouse=True)
def mail_templates():
    templates = {
        "template_1.tpl.html": "Html template 1",
        "template_2.tpl.html": "Html template 2",
        "template_3.tpl.html": "Html template 3",
    }

    os.makedirs("templates", exist_ok=True)
    for name, content in templates.items():
        with open(os.path.join("templates", name), "wt") as f:
            f.write(content)

    yield templates

    for name in templates.keys():
        os.unlink(os.path.join("templates", name))

    try:
        os.rmdir("templates")
    except OSError:
        pass


async def test_fetches_messages(domain, dm):
    await domain.process_unsent_emails()

    dm.list_unprocessed_email_messages.assert_called_with()


@pytest.mark.parametrize(
    "group_fields_overrides",
    [
        {"subject": None},
        {"template_name": None},
        {"template_name": "no_such_template.txt"},
    ],
)
async def test_sends_only_valid_messages(
    domain, dm, email_client, group_fields_overrides
):
    dm.list_unprocessed_email_messages.coro.return_value = [
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": "Тема письма",
            "template_name": "template_1",
            "messages": [
                {
                    "id": 11,
                    "message_anchor": "message_1",
                    "recipient": "example1@yandex.ru",
                    "template_vars": {},
                }
            ],
        },
        {
            "time_to_send": dt("2020-02-02 13:40:00"),
            "subject": "Тема другого письма",
            "template_name": "template_2",
            "messages": [
                {
                    "id": 12,
                    "message_anchor": "message_2",
                    "recipient": "example2@yandex.ru",
                    "template_vars": {"first_name": "Not", "last_name": "Sure"},
                }
            ],
        },
        {
            "time_to_send": dt("2020-02-03 14:50:00"),
            "subject": "Тоже тема",
            "template_name": "template_3",
            "messages": [
                {
                    "id": 13,
                    "message_anchor": "message_3",
                    "recipient": "example3@yandex.ru",
                    "template_vars": {"first_name": "Not", "last_name": "Sure"},
                }
            ],
        },
    ]
    dm.list_unprocessed_email_messages.coro.return_value[1].update(
        group_fields_overrides
    )

    await domain.process_unsent_emails()

    assert email_client.schedule_promo_campaign.call_args_list == [
        (
            (),
            {
                "body": "Html template 1",
                "from_email": "from@yandex.ru",
                "from_name": "Предложения от партнёров",
                "mailing_list_params": [{"email": "example1@yandex.ru", "params": {}}],
                "mailing_list_source": MailingListSource.IN_PLACE,
                "schedule_dt": dt("2020-02-02 12:30:00"),
                "subject": "Тема письма",
                "tags": ("message_1",),
                "title": "Автокампания",
                "unsubscribe_list_slug": "yandex_clients",
                "allowed_stat_domains": ["yandex.ru", "ya.ru"],
            },
        ),
        (
            (),
            {
                "body": "Html template 3",
                "from_email": "from@yandex.ru",
                "from_name": "Предложения от партнёров",
                "mailing_list_params": [
                    {
                        "email": "example3@yandex.ru",
                        "params": {"first_name": "Not", "last_name": "Sure"},
                    }
                ],
                "mailing_list_source": MailingListSource.IN_PLACE,
                "schedule_dt": dt("2020-02-03 14:50:00"),
                "subject": "Тоже тема",
                "tags": ("message_3",),
                "title": "Автокампания",
                "unsubscribe_list_slug": "yandex_clients",
                "allowed_stat_domains": ["yandex.ru", "ya.ru"],
            },
        ),
    ]


@pytest.mark.parametrize(
    "message_fields_overrides",
    [{"recipient": None}, {"recipient": "not_an_email_is_it"}, {"template_vars": []}],
)
async def test_sends_only_valid_messages_within_group(
    domain, dm, email_client, message_fields_overrides
):
    dm.list_unprocessed_email_messages.coro.return_value = [
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": "Тема письма",
            "template_name": "template_1",
            "messages": [
                {
                    "id": 11,
                    "message_anchor": "message_1",
                    "recipient": "example1@yandex.ru",
                    "template_vars": {},
                },
                {
                    "id": 12,
                    "message_anchor": "message_2",
                    "recipient": "example2@yandex.ru",
                    "template_vars": {},
                },
                {
                    "id": 13,
                    "message_anchor": "message_3",
                    "recipient": "example3@yandex.ru",
                    "template_vars": {"first_name": "Not", "last_name": "Sure"},
                },
            ],
        }
    ]

    dm.list_unprocessed_email_messages.coro.return_value[0]["messages"][1].update(
        message_fields_overrides
    )

    await domain.process_unsent_emails()

    assert email_client.schedule_promo_campaign.call_args_list == [
        (
            (),
            {
                "body": "Html template 1",
                "from_email": "from@yandex.ru",
                "from_name": "Предложения от партнёров",
                "mailing_list_params": [
                    {"email": "example1@yandex.ru", "params": {}},
                    {
                        "email": "example3@yandex.ru",
                        "params": {"first_name": "Not", "last_name": "Sure"},
                    },
                ],
                "mailing_list_source": MailingListSource.IN_PLACE,
                "schedule_dt": dt("2020-02-02 12:30:00"),
                "subject": "Тема письма",
                "tags": ("message_1", "message_3"),
                "title": "Автокампания",
                "unsubscribe_list_slug": "yandex_clients",
                "allowed_stat_domains": ["yandex.ru", "ya.ru"],
            },
        )
    ]


@pytest.mark.parametrize("exc_class", [EmailSenderError, Exception])
async def test_not_raises_if_email_client_raises(domain, dm, email_client, exc_class):
    dm.list_unprocessed_email_messages.coro.return_value = [
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": "Тема письма",
            "template_name": "template_1",
            "messages": [
                {
                    "id": 11,
                    "recipient": "example1@yandex.ru",
                    "message_anchor": "message_1",
                    "template_vars": {},
                }
            ],
        }
    ]

    email_client.schedule_promo_campaign.coro.side_effect = exc_class

    try:
        await domain.process_unsent_emails()
    except Exception:
        pytest.fail("Should not raise")


async def test_marks_message_as_processed_without_error_if_message_sent(domain, dm):
    dm.list_unprocessed_email_messages.coro.return_value = [
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": "Тема письма",
            "template_name": "template_1",
            "messages": [
                {
                    "id": 11,
                    "message_anchor": "message_1",
                    "recipient": "example1@yandex.ru",
                    "template_vars": {},
                }
            ],
        }
    ]

    await domain.process_unsent_emails()

    dm.mark_messages_processed.assert_called_with(
        {11: None},
        dt("2020-02-02 12:00:00"),
        {"id": 111, "slug": "GUG2RFM2-MV51", "campaign_is_scheduled": True},
    )


@pytest.mark.parametrize(
    ("group_fields_overrides", "expected_error"),
    [
        ({"subject": None}, "subject: Field may not be null."),
        ({"template_name": None}, "Failed to get template content"),
        ({"template_name": "no_such_template.txt"}, "Failed to get template content"),
    ],
)
async def test_marks_all_messages_in_group_as_failed_if_group_validation_failed(
    domain, dm, group_fields_overrides, expected_error
):
    dm.list_unprocessed_email_messages.coro.return_value = [
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": "Тема письма",
            "template_name": "template_1",
            "messages": [
                {
                    "id": 11,
                    "message_anchor": "message_1",
                    "recipient": "example1@yandex.ru",
                    "template_vars": {},
                },
                {
                    "id": 12,
                    "message_anchor": "message_2",
                    "recipient": "example2@yandex.ru",
                    "template_vars": {"first_name": "Not", "last_name": "Sure"},
                },
            ],
        }
    ]
    dm.list_unprocessed_email_messages.coro.return_value[0].update(
        group_fields_overrides
    )

    await domain.process_unsent_emails()

    dm.mark_messages_processed.assert_called_with(
        {11: expected_error, 12: expected_error}, dt("2020-02-02 12:00:00"), None
    )


@pytest.mark.parametrize(
    ("message_fields_overrides", "expected_error"),
    [
        ({"recipient": None}, "recipient: Field may not be null."),
        ({"recipient": "not_a_email_is_it"}, "recipient: Invalid email"),
        ({"template_vars": []}, "template_vars: Not a valid mapping type."),
    ],
)
async def test_marks_message_as_processed_with_error_if_message_validation_failed(
    domain, dm, message_fields_overrides, expected_error
):
    dm.list_unprocessed_email_messages.coro.return_value = [
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": "Тема письма",
            "template_name": "template_1",
            "messages": [
                {
                    "id": 11,
                    "message_anchor": "message_1",
                    "recipient": "example1@yandex.ru",
                    "template_vars": {},
                }
            ],
        }
    ]
    dm.list_unprocessed_email_messages.coro.return_value[0]["messages"][0].update(
        message_fields_overrides
    )

    await domain.process_unsent_emails()

    dm.mark_messages_processed.assert_called_with(
        {11: expected_error}, dt("2020-02-02 12:00:00"), None
    )


async def test_marks_message_as_processed_with_error_if_email_client_raised_known_error(
    domain, dm, email_client
):
    dm.list_unprocessed_email_messages.coro.return_value = [
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": "Тема письма",
            "template_name": "template_1",
            "messages": [
                {
                    "id": 11,
                    "message_anchor": "message_1",
                    "recipient": "example1@yandex.ru",
                    "template_vars": {},
                }
            ],
        }
    ]
    email_client.schedule_promo_campaign.coro.side_effect = EmailSenderError(
        "I will not"
    )

    await domain.process_unsent_emails()

    dm.mark_messages_processed.assert_called_with(
        {11: "EmailSender error: I will not"}, dt("2020-02-02 12:00:00"), None
    )


async def test_does_not_mark_as_processed_if_email_client_raised_unknown_error(
    domain, dm, email_client
):
    dm.list_unprocessed_email_messages.coro.return_value = [
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": "Тема письма",
            "template_name": "template_1",
            "messages": [
                {
                    "id": 11,
                    "message_anchor": "message_1",
                    "recipient": "example1@yandex.ru",
                    "template_vars": {},
                }
            ],
        }
    ]
    email_client.schedule_promo_campaign.coro.side_effect = Exception

    await domain.process_unsent_emails()

    dm.mark_messages_processed.assert_not_called()


@pytest.mark.parametrize(
    ("exc_class", "expected_exc_str"),
    [(ValueError, "ValueError"), (KeyError, "KeyError"), (Exception, "Exception")],
)
async def test_logs_error_if_email_client_raised_unknown_error(
    caplog, domain, dm, email_client, exc_class, expected_exc_str
):
    dm.list_unprocessed_email_messages.coro.return_value = [
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": "Тема письма",
            "template_name": "template_1",
            "messages": [
                {
                    "id": 11,
                    "message_anchor": "message_1",
                    "recipient": "example1@yandex.ru",
                    "template_vars": {},
                },
                {
                    "id": 12,
                    "message_anchor": "message_2",
                    "recipient": "example2@yandex.ru",
                    "template_vars": {},
                },
            ],
        }
    ]
    email_client.schedule_promo_campaign.coro.side_effect = exc_class

    await domain.process_unsent_emails()

    assert len(caplog.records) == 1
    record = caplog.records[0]
    assert record.levelno == logging.ERROR
    assert (
        record.msg == f"Failed to send email messages %s "
        f"because email_client raised {expected_exc_str}"
    )
    assert record.args == ("11,12",)


async def test_keeps_on_sending_if_some_messages_fail(domain, dm, email_client):
    dm.list_unprocessed_email_messages.coro.return_value = [
        # Failed by validation
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": "Тема письма",
            "template_name": "no_such_template",
            "messages": [
                {
                    "id": 11,
                    "message_anchor": "message_1",
                    "recipient": "example1@yandex.ru",
                    "template_vars": {},
                }
            ],
        },
        # Failed by email_client
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": "Тема письма",
            "template_name": "template_1",
            "messages": [
                {
                    "id": 12,
                    "message_anchor": "message_2",
                    "recipient": "example2@yandex.ru",
                    "template_vars": {},
                }
            ],
        },
        # Also failed by email_client
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": "Тема письма",
            "template_name": "template_2",
            "messages": [
                {
                    "id": 13,
                    "message_anchor": "message_3",
                    "recipient": "example3@yandex.ru",
                    "template_vars": {},
                }
            ],
        },
        # Should be sent
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": "Тема письма",
            "template_name": "template_3",
            "messages": [
                {
                    "id": 14,
                    "message_anchor": "message_4",
                    "recipient": "example4@yandex.ru",
                    "template_vars": {},
                }
            ],
        },
    ]
    email_client.schedule_promo_campaign.coro.side_effect = [
        EmailSenderError,
        Exception,
        {"id": 1, "slug": "GUG2RFM2-MV51", "campaign_is_scheduled": True},
    ]

    await domain.process_unsent_emails()

    email_client.schedule_promo_campaign.assert_called_with(
        body="Html template 3",
        from_email="from@yandex.ru",
        from_name="Предложения от партнёров",
        mailing_list_params=[{"email": "example4@yandex.ru", "params": {}}],
        mailing_list_source=MailingListSource.IN_PLACE,
        schedule_dt=dt("2020-02-02 12:30:00"),
        subject="Тема письма",
        tags=("message_4",),
        title="Автокампания",
        unsubscribe_list_slug="yandex_clients",
        allowed_stat_domains=["yandex.ru", "ya.ru"],
    )


async def test_processed_grouped_messages_with_one_call(domain, dm, email_client):
    dm.list_unprocessed_email_messages.coro.return_value = [
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": "Тема письма",
            "template_name": "template_1",
            "messages": [
                {
                    "id": 11,
                    "message_anchor": "message_1",
                    "recipient": "example1@yandex.ru",
                    "template_vars": {},
                },
                {
                    "id": 12,
                    "message_anchor": "message_2",
                    "recipient": "example2@yandex.ru",
                    "template_vars": {"first_name": "Not", "last_name": "Sure"},
                },
            ],
        }
    ]

    await domain.process_unsent_emails()

    email_client.schedule_promo_campaign.assert_called_with(
        body="Html template 1",
        from_email="from@yandex.ru",
        from_name="Предложения от партнёров",
        mailing_list_params=[
            {"email": "example1@yandex.ru", "params": {}},
            {
                "email": "example2@yandex.ru",
                "params": {"first_name": "Not", "last_name": "Sure"},
            },
        ],
        mailing_list_source=MailingListSource.IN_PLACE,
        schedule_dt=dt("2020-02-02 12:30:00"),
        subject="Тема письма",
        tags=("message_1", "message_2"),
        title="Автокампания",
        unsubscribe_list_slug="yandex_clients",
        allowed_stat_domains=["yandex.ru", "ya.ru"],
    )
    dm.mark_messages_processed.assert_called_with(
        {11: None, 12: None},
        dt("2020-02-02 12:00:00"),
        {"id": 111, "slug": "GUG2RFM2-MV51", "campaign_is_scheduled": True},
    )


async def test_message_not_sent_if_time_to_send_is_in_past(domain, dm, email_client):
    dm.list_unprocessed_email_messages.coro.return_value = [
        {
            "time_to_send": dt("2020-02-02 09:00:00"),
            "subject": "Тема письма",
            "template_name": "template_1",
            "messages": [
                {
                    "id": 11,
                    "message_anchor": "message_1",
                    "recipient": "example1@yandex.ru",
                    "template_vars": {},
                },
                {
                    "id": 12,
                    "message_anchor": "message_2",
                    "recipient": "example2@yandex.ru",
                    "template_vars": {"first_name": "Not", "last_name": "Sure"},
                },
            ],
        }
    ]

    await domain.process_unsent_emails()

    email_client.schedule_promo_campaign.assert_not_called()
    dm.mark_messages_processed.assert_called_with(
        {11: "Too late to send", 12: "Too late to send"},
        dt("2020-02-02 12:00:00"),
        None,
    )


async def test_error_is_logged_if_time_to_send_is_in_past(caplog, domain, dm):
    dm.list_unprocessed_email_messages.coro.return_value = [
        {
            "time_to_send": dt("2020-02-02 09:00:00"),
            "subject": "Тема письма",
            "template_name": "template_1",
            "messages": [
                {
                    "id": 11,
                    "message_anchor": "message_1",
                    "recipient": "example1@yandex.ru",
                    "template_vars": {},
                },
                {
                    "id": 12,
                    "message_anchor": "message_2",
                    "recipient": "example2@yandex.ru",
                    "template_vars": {"first_name": "Not", "last_name": "Sure"},
                },
            ],
        }
    ]

    await domain.process_unsent_emails()

    assert len(caplog.records) == 1
    record = caplog.records[0]
    assert record.levelno == logging.ERROR
    assert record.msg == "Too late to send email messages %s"
    assert record.args == ("11,12",)


async def test_message_without_button_url_template_var_is_sent(
    domain, dm, email_client
):
    dm.list_unprocessed_email_messages.coro.return_value = [
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": "Тема письма",
            "template_name": "template_1",
            "messages": [
                {
                    "id": 11,
                    "message_anchor": "message_1",
                    "recipient": "example1@yandex.ru",
                    "template_vars": {"first_name": "Not", "last_name": "Sure"},
                },
            ],
        }
    ]

    await domain.process_unsent_emails()

    email_client.schedule_promo_campaign.assert_called_with(
        body="Html template 1",
        from_email="from@yandex.ru",
        from_name="Предложения от партнёров",
        mailing_list_params=[
            {
                "email": "example1@yandex.ru",
                "params": {"first_name": "Not", "last_name": "Sure"},
            }
        ],
        mailing_list_source=MailingListSource.IN_PLACE,
        schedule_dt=dt("2020-02-02 12:30:00"),
        subject="Тема письма",
        tags=("message_1",),
        title="Автокампания",
        unsubscribe_list_slug="yandex_clients",
        allowed_stat_domains=["yandex.ru", "ya.ru"],
    )
    dm.mark_messages_processed.assert_called_with(
        {
            11: None,
        },
        dt("2020-02-02 12:00:00"),
        {"id": 111, "slug": "GUG2RFM2-MV51", "campaign_is_scheduled": True},
    )


async def test_message_with_button_url_template_var_with_allowed_domain_is_sent(
    domain, dm, email_client
):
    dm.list_unprocessed_email_messages.coro.return_value = [
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": "Тема письма",
            "template_name": "template_1",
            "messages": [
                {
                    "id": 11,
                    "message_anchor": "message_1",
                    "recipient": "example1@yandex.ru",
                    "template_vars": {
                        "button_url": "https://yandex.ru",
                        "first_name": "Not",
                        "last_name": "Sure",
                    },
                },
                {
                    "id": 12,
                    "message_anchor": "message_2",
                    "recipient": "example2@yandex.ru",
                    "template_vars": {
                        "button_url": "http://ya.ru/smth?utm-no#goto-anchor"
                    },
                },
            ],
        }
    ]

    await domain.process_unsent_emails()

    email_client.schedule_promo_campaign.assert_called_with(
        body="Html template 1",
        from_email="from@yandex.ru",
        from_name="Предложения от партнёров",
        mailing_list_params=[
            {
                "email": "example1@yandex.ru",
                "params": {
                    "button_url": "https://yandex.ru",
                    "first_name": "Not",
                    "last_name": "Sure",
                },
            },
            {
                "email": "example2@yandex.ru",
                "params": {"button_url": "http://ya.ru/smth?utm-no#goto-anchor"},
            },
        ],
        mailing_list_source=MailingListSource.IN_PLACE,
        schedule_dt=dt("2020-02-02 12:30:00"),
        subject="Тема письма",
        tags=("message_1", "message_2"),
        title="Автокампания",
        unsubscribe_list_slug="yandex_clients",
        allowed_stat_domains=["yandex.ru", "ya.ru"],
    )
    dm.mark_messages_processed.assert_called_with(
        {
            11: None,
            12: None,
        },
        dt("2020-02-02 12:00:00"),
        {"id": 111, "slug": "GUG2RFM2-MV51", "campaign_is_scheduled": True},
    )


async def test_message_with_button_url_template_var_with_not_allowed_domain_is_not_sent(
    domain, dm, email_client
):
    dm.list_unprocessed_email_messages.coro.return_value = [
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": "Тема письма",
            "template_name": "template_1",
            "messages": [
                {
                    "id": 11,
                    "message_anchor": "message_1",
                    "recipient": "example1@yandex.ru",
                    "template_vars": {
                        "button_url": "https://maps.yandex.ru",
                        "first_name": "Not",
                        "last_name": "Sure",
                    },
                },
                {
                    "id": 12,
                    "message_anchor": "message_2",
                    "recipient": "example2@yandex.ru",
                    "template_vars": {
                        "button_url": "http://google.com:80/smth?utm-no#yandex.ru"
                    },
                },
            ],
        }
    ]

    await domain.process_unsent_emails()

    email_client.schedule_promo_campaign.assert_not_called()
    dm.mark_messages_processed.assert_called_with(
        {
            11: "template_vars: domain not allowed for button_url",
            12: "template_vars: domain not allowed for button_url",
        },
        dt("2020-02-02 12:00:00"),
        None,
    )


async def test_message_with_invalid_button_url_template_var_with_not_allowed_domain_is_not_sent(  # noqa E501
    domain, dm, email_client
):
    dm.list_unprocessed_email_messages.coro.return_value = [
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": "Тема письма",
            "template_name": "template_1",
            "messages": [
                {
                    "id": 11,
                    "message_anchor": "message_1",
                    "recipient": "example1@yandex.ru",
                    "template_vars": {
                        "button_url": "https://[ff::/path",
                        "first_name": "Not",
                        "last_name": "Sure",
                    },
                },
                {
                    "id": 12,
                    "message_anchor": "message_2",
                    "recipient": "example2@yandex.ru",
                    "template_vars": {"button_url": "http://google\u2100.con/path"},
                },
            ],
        }
    ]

    await domain.process_unsent_emails()

    email_client.schedule_promo_campaign.assert_not_called()
    dm.mark_messages_processed.assert_called_with(
        {
            11: "template_vars: button_url is invalid",
            12: "template_vars: button_url is invalid",
        },
        dt("2020-02-02 12:00:00"),
        None,
    )
