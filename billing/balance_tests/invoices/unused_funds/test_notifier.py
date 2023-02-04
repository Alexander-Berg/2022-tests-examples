# -*- coding: utf-8 -*-
from abc import abstractmethod, ABCMeta

import pytest

from balance.constants import (
    ServiceId,
    NotifyOpcode,
)
from balance.mapper import ClientUnusedFunds
from notifier import data_objects as notifier_objects
from tests.balance_tests.invoices.unused_funds.common import (
    create_invoice,
)


@pytest.fixture(scope="session")
def service_id():
    return ServiceId.DIRECT


def get_notification_id(client, invoice, service_id):
    return ClientUnusedFunds.calculate_notification_id(
        client_id=client.id,
        invoice_id=invoice.id,
        service_id=service_id,
    )


class TestCaseSetup(object):
    __metaclass__ = ABCMeta

    @classmethod
    def prefill_cache(
        cls,
        session,
        client,
        invoice,
        other_invoice,
    ):
        cache = [
            ClientUnusedFunds(client=client, **obj_desc)
            for obj_desc in cls.cache_content_descr(
                invoice,
                other_invoice,
            )
        ]
        session.add_all(cache)
        session.flush()

    @classmethod
    @abstractmethod
    def cache_content_descr(
        cls,
        invoice,
        other_invoice,
    ):  # type: (...) -> list[dict[str, Any]]
        pass

    @classmethod
    @abstractmethod
    def expected_result(cls):  # type: (...) -> dict[str, Any]
        pass


class NoUnusedFunds(TestCaseSetup):
    # Cache is empty if there are no unused funds
    @classmethod
    def cache_content_descr(
        cls,
        invoice,
        other_invoice,
    ):  # type: (...) -> list[dict[str, Any]]
        return []

    @classmethod
    def expected_result(cls):  # type: (...) -> dict[str, Any]
        return {}


class AllUsedSameCurrency(TestCaseSetup):
    @classmethod
    def cache_content_descr(
        cls,
        invoice,
        other_invoice,
    ):  # type: (...) -> list[dict[str, Any]]
        currency = "RUR"
        unused_funds = 0
        return [
            {"invoice": inv, "currency": currency, "unused_funds": unused_funds}
            for inv in [invoice, other_invoice]
        ]

    @classmethod
    def expected_result(cls):  # type: (...) -> dict[str, Any]
        return {}


class AllUsedDifferentCurrency(TestCaseSetup):
    @classmethod
    def cache_content_descr(
        cls,
        invoice,
        other_invoice,
    ):  # type: (...) -> list[dict[str, Any]]
        unused_funds = 0
        return [
            {"invoice": inv, "currency": currency, "unused_funds": unused_funds}
            for inv, currency in [
                (invoice, "RUR"),
                (other_invoice, "USD"),
            ]
        ]

    @classmethod
    def expected_result(cls):  # type: (...) -> dict[str, Any]
        return {}


class UnusedFundsSameCurrency(TestCaseSetup):

    funds = (12, 21)
    currency = "RUR"

    @classmethod
    def cache_content_descr(
        cls,
        invoice,
        other_invoice,
    ):  # type: (...) -> list[dict[str, Any]]
        return [
            {
                "invoice": inv,
                "currency": cls.currency,
                "unused_funds": unused_funds,
            }
            for inv, unused_funds in zip(
                (invoice, other_invoice),
                cls.funds,
            )
        ]

    @classmethod
    def expected_result(cls):  # type: (...) -> dict[str, Any]
        return {cls.currency: sum(cls.funds)}


class UnusedFundsDifferentCurrency(TestCaseSetup):

    unused_description = {"RUR": 666, "USD": 123}

    @classmethod
    def cache_content_descr(
        cls,
        invoice,
        other_invoice,
    ):  # type: (...) -> list[dict[str, Any]]
        return [
            {
                "currency": currency,
                "unused_funds": funds,
                "invoice": inv,
            }
            for (currency, funds), inv in zip(
                cls.unused_description.items(), (invoice, other_invoice)
            )
        ]

    @classmethod
    def expected_result(cls):  # type: (...) -> dict[str, Any]
        return cls.unused_description


class MixedUnusedFunds(TestCaseSetup):

    unused_funds = 666
    unused_currency = "RUR"

    used_currency = "USD"

    @classmethod
    def cache_content_descr(
        cls,
        invoice,
        other_invoice,
    ):  # type: (...) -> list[dict[str, Any]]
        return [
            {
                "invoice": invoice,
                "currency": cls.unused_currency,
                "unused_funds": cls.unused_funds,
            },
            {
                "invoice": other_invoice,
                "currency": cls.used_currency,
                "unused_funds": 0,
            },
        ]

    @classmethod
    def expected_result(cls):  # type: (...) -> dict[str, Any]
        return {cls.unused_currency: cls.unused_funds}


@pytest.mark.parametrize("swap_test_invoices", [True, False])
@pytest.mark.parametrize(
    "test_case",
    [
        NoUnusedFunds,
        AllUsedSameCurrency,
        AllUsedDifferentCurrency,
        UnusedFundsSameCurrency,
        UnusedFundsDifferentCurrency,
        MixedUnusedFunds,
    ],
)
def test_unused_funds_notifier(
    session,
    client,
    invoice,
    test_case,
    service_id,
    swap_test_invoices,
):
    other_invoice = create_invoice(client=client)
    if swap_test_invoices:
        test_case.prefill_cache(session, client, other_invoice, invoice)
    else:
        test_case.prefill_cache(session, client, invoice, other_invoice)
    session.expire_all()

    info = notifier_objects.BaseInfo.get_notification_info(
        session,
        NotifyOpcode.INVOICE_UNUSED_FUNDS,
        get_notification_id(client, invoice, service_id),
    )

    args = info[1]["args"][0]
    assert args["ClientID"] == str(client.id)
    assert args["ServiceID"] == str(service_id)
    assert {
        unused_funds_descr["Currency"]: unused_funds_descr["UnusedFunds"]
        for unused_funds_descr in args["UnusedFunds"]
    } == test_case.expected_result()
