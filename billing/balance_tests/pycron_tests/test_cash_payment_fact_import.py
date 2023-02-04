# coding: utf-8
import json
from base64 import b64encode
from functools import partial

import pytest
from mock.mock import MagicMock, call
from sqlalchemy.exc import DatabaseError

from balance import mapper
from balance.constants import OebsOperationType
from cluster_tools.cash_payment_fact_import import InvalidMessage


@pytest.fixture()
def mock_importer(mock_logbroker_app_base):
    return partial(
        mock_logbroker_app_base,
        "cluster_tools.cash_payment_fact_import",
        "CashPaymentFactImport",
    )


@pytest.fixture()
def default_config():
    return {
        "retry_ora_codes": [
            12543,
        ],
        "dev": {
            "consumer": "balance/test/balance",
            "topic": "/billing-payout/test/cpf",
            "error_topic": "/billing-payout/test/cpf_errors",
            "batch_size": 5000,
        },
    }


@pytest.mark.parametrize("attr_to_delete", ["topic", "consumer"])
def test_cash_payment_fact_import_cfg_invalid(
    attr_to_delete, default_config, mock_importer
):
    del default_config["dev"][attr_to_delete]
    with mock_importer(default_config) as importer:
        importer.main()
        importer.lb.get_consumer.assert_not_called()


@pytest.mark.parametrize(
    "message,result,retry",
    [
        (None, "Message is not a dict", []),
        ({}, "Message violates the schema", []),
        ([], "Message is not a dict", []),
        ({"a": 1}, "Message violates the schema", []),
        (
            {
                "id": 135143,
                "amount": "12x34",  # not a decimal
                "operation_type": OebsOperationType.ONLINE,
                "receipt_date": "2015-04-27 12:32:17",
                "receipt_number": "TEST_RECEIPT_12345",
            },
            "Message violates the schema",
            [],
        ),
        (
            {
                "id": 135143,
                "amount": "12.34",
                "operation_type": OebsOperationType.ONLINE,
                "receipt_date": "2015-04-27-12:32:17",  # invalid date format
                "receipt_number": "TEST_RECEIPT_12345",
            },
            "Message violates the schema",
            [],
        ),
        (
            {
                "id": 135143,
                "amount": "12.34",
                "operation_type": "K92" * 20,  # too long for the column
                "receipt_date": "2015-04-27 12:32:17",
                "receipt_number": "TEST_RECEIPT_12345",
            },
            "Database error while handling message",
            [],
        ),
        (
            {
                "id": 135143,
                "amount": "12.34",
                "operation_type": "K92" * 20,  # too long for the column
                "receipt_date": "2015-04-27 12:32:17",
                "receipt_number": "TEST_RECEIPT_12345",
            },
            DatabaseError,
            [6502],
        ),
        (
            {
                "id": 135143,
                "amount": "12.34",
                "operation_type": OebsOperationType.ONLINE,
                "receipt_date": "2015-04-27 12:32:17",
                "receipt_number": "TEST_RECEIPT_12345",
            },
            None,
            [],
        ),
    ],
)
def test_cash_payment_fact_import_handle_message(
    message, result, session, retry, mock_importer
):
    with mock_importer() as importer:
        if isinstance(result, type) and issubclass(result, Exception):
            with pytest.raises(result):
                importer.handle_message(message, session, retry)
        elif isinstance(result, (str, unicode)):
            existing_facts = session.query(mapper.OebsCashPaymentFact).count()
            error = importer.handle_message(message, session, retry)
            assert isinstance(error, (str, unicode)) and error.startswith(result)
            session.flush()
            assert session.query(mapper.OebsCashPaymentFact).count() == existing_facts
        else:
            existing_facts = session.query(mapper.OebsCashPaymentFact).count()
            assert importer.handle_message(message, session, retry) is None
            session.flush()
            new_fact = session.query(mapper.OebsCashPaymentFact).get(message["id"])
            assert new_fact is not None
            existing_facts += 1
            assert session.query(mapper.OebsCashPaymentFact).count() == existing_facts
            assert importer.handle_message(message, session, retry) is None
            session.flush()
            assert session.query(mapper.OebsCashPaymentFact).count() == existing_facts
            message["amount"] = "10.15"
            error = importer.handle_message(message, session, retry)
            assert isinstance(error, (str, unicode)) and error.startswith(
                "Message differs from the existing record"
            )
            session.flush()
            assert session.query(mapper.OebsCashPaymentFact).count() == existing_facts


@pytest.mark.parametrize(
    "invalid_messages",
    [
        None,
        [],
        [
            InvalidMessage("error1", "message1", "message1_orig"),
            InvalidMessage("error2", None, "message2_orig"),
        ],
    ],
)
@pytest.mark.parametrize("error_topic", [None, "test_error_topic"])
def test_cash_payment_fact_import_handle_invalid_messages(
    invalid_messages, error_topic, mock_importer
):
    config = {"dev": {"error_topic": error_topic}} if error_topic else None
    with mock_importer(config) as importer:
        importer.write_messages = MagicMock()
        importer.handle_invalid_messages(invalid_messages)
        if not invalid_messages or not error_topic:
            importer.write_messages.assert_not_called()
        else:
            importer.write_messages.assert_called_once_with(
                [
                    {
                        "error": m.error,
                        "message": m.message,
                        "base64_original_data": b64encode(m.original_data),
                    }
                    for m in invalid_messages
                ],
                error_topic,
            )


def _mock_handle_message(m, _s, _r):
    if "exception" in m:
        raise ValueError(u"Test exception Ю")
    if "invalid" in m:
        return "invalid"
    return None


@pytest.mark.parametrize(
    "messages",
    [
        None,
        [],
        ["not a json", u"не json"],
        [{"a": u"й"}, {u"ю": 2}],
        [{"a": u"й", "invalid": 1}, {u"ю": 4, "invalid": 1}],
        [{"a": u"й"}, {u"ю": 2}, {"a": u"й", "invalid": 1}, {"b": u"ю", "invalid": 1}],
        [
            {"a": u"й"},
            {u"ю": 2, "exception": 1},
            {"a": u"й", "invalid": 1},
            {u"ю": 4, "invalid": 1, "exception": 1},
        ],
    ],
)
def test_cash_payment_fact_import_main(messages, default_config, mock_importer):
    with mock_importer(default_config, messages) as importer:
        importer.handle_message = MagicMock(wraps=_mock_handle_message)
        importer.handle_invalid_messages = MagicMock()
        importer.main()
        importer.lb.get_consumer.assert_called_once()
        if not messages:
            importer.handle_message.assert_not_called()
            importer.handle_invalid_messages.assert_not_called()
            assert importer.lb.get_consumer.mock_calls[-1] == call().read(
                auto_commit=False
            )
        else:
            exceptions = False
            handled_messages = []
            invalid_messages = []
            for m in messages:
                if not isinstance(m, dict):
                    invalid_messages.append(("Unable to parse message", None, m))
                    continue
                handled_messages.append(m)
                if "exception" in m:
                    exceptions = True
                    break
                if "invalid" in m:
                    invalid_messages.append(
                        (
                            "invalid",
                            m,
                            json.dumps(m, ensure_ascii=False, encoding="utf8"),
                        )
                    )
            handle_message_args = [
                c.args[0] for c in importer.handle_message.call_args_list
            ]
            assert handle_message_args == handled_messages
            if not exceptions:
                importer.handle_invalid_messages.assert_called_once_with(
                    invalid_messages
                )
                assert importer.lb.get_consumer.mock_calls[
                    -1
                ] == call().consumer.commit(default_config["dev"]["topic"])
            else:
                assert importer.lb.get_consumer.mock_calls[-1] == call().read(
                    auto_commit=False
                )
