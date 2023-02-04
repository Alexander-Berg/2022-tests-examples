from decimal import Decimal

import hamcrest as hm
from billing.contract_iface import JSONContract

from billing.library.python.calculator.models.tax import TaxPolicyModel
from billing.library.python.calculator.models.trust import PaymentRowModel
from billing.library.python.calculator.test_utils.builder import gen_trust_service_product
from billing.library.python.calculator.test_utils.builder.diod_builder import gen_diod_key
from billing.library.python.calculator.values import PaymentMethodID

from billing.hot.calculators.trust.calculator.core.adapters import EventAdapter, EventRowAdapter
from billing.hot.calculators.trust.calculator.core.configurable_blocks.adapters import AdapterType
from billing.hot.calculators.trust.calculator.core.configurable_blocks.getters import GetterType
from billing.hot.calculators.trust.calculator.core.const import TransactionState
from billing.hot.calculators.trust.calculator.core.models.method import to_payment_method
from billing.hot.calculators.trust.calculator.core.parse_settings import _parse_settings

from .test_data.manifests import gen_manifest_settings
from .test_data.payment.builder import (
    ORDER,
    PRODUCT,
    gen_payment,
    gen_references,
    gen_refund,
    gen_trust_order,
    gen_trust_payment_row,
)
from .test_data.payment.const import CLIENT_ID, NOW_WITH_TZ, PRODUCT_ID, SERVICE_PRODUCT_ID
from .test_data.payment.generated_data import FIRM, GENERAL_CONTRACT_FOR_USD, MIGRATION_INFO


ORDER_2 = gen_trust_order(
    service_product_id=SERVICE_PRODUCT_ID + 1, service_order_id="2", service_product_external_id="2"
)
ORDER_3 = gen_trust_order(
    service_product_id=SERVICE_PRODUCT_ID + 2, service_order_id="3", service_product_external_id="3"
)

PRODUCT_2 = gen_trust_service_product(partner_id=CLIENT_ID, id=PRODUCT_ID + 1, external_id="2")
PRODUCT_3 = gen_trust_service_product(partner_id=CLIENT_ID, id=PRODUCT_ID + 2, external_id="3")

# describe only essential fields that effect adapters logic, others are default
EVENT_DATA = gen_payment(
    service_id=1,
    amount=Decimal(60),
    postauth_amount=Decimal(60),
    payment_method="card-x3375",
    payment_method_id=PaymentMethodID.CARD,
    postauth_dt=NOW_WITH_TZ,
    tariffer_payload={"key1": "value"},
    products=[PRODUCT, PRODUCT_2, PRODUCT_3],
    rows=[
        gen_trust_payment_row(id=1, amount=Decimal(10), order=ORDER),
        gen_trust_payment_row(id=2, amount=Decimal(10), order=ORDER),
        gen_trust_payment_row(id=3, amount=Decimal(10), order=ORDER),
        gen_trust_payment_row(id=4, amount=Decimal(10), order=ORDER),
        gen_trust_payment_row(id=5, amount=Decimal(10), order=ORDER_2),  # partial reversal
        gen_trust_payment_row(id=6, amount=Decimal(10), order=ORDER_3),  # full reversal
    ],
    refunds=[
        gen_refund(
            amount=Decimal(15),
            is_reversal=True,
            rows=[
                gen_trust_payment_row(id=7, amount=Decimal(5), order=ORDER_2),
                gen_trust_payment_row(id=8, amount=Decimal(10), order=ORDER_3),
            ],
        ),
        gen_refund(amount=Decimal(10), rows=[gen_trust_payment_row(id=9, amount=Decimal(10), order=ORDER)]),
    ],
    composite_components=[
        gen_payment(
            service_id=1,
            amount=Decimal(20),
            postauth_amount=Decimal(20),
            payment_method="not_card-x3375",
            payment_method_id=PaymentMethodID.YAMONEY_WALLET,
            products=[PRODUCT_2],
            rows=[
                gen_trust_payment_row(id=10, amount=Decimal(10), order=ORDER_2),
            ],
        ),
    ],
)

REFERENCES_DATA = gen_references(
    contracts=[GENERAL_CONTRACT_FOR_USD],
    firms=[FIRM],
    migration_info=[MIGRATION_INFO],
    diod_keys=[
        gen_diod_key("1", "trust", "not_migrated"),
        gen_diod_key("2", "trust", "skipped"),
        gen_diod_key("3", "trust", "processed"),
        gen_diod_key("4", "trust", "processed_before"),
    ],
    currency_conversion={"to": "USD", "rate": Decimal("60.5")},
    lock=None,
)

EVENT_METHOD_DATA = {
    "event": EVENT_DATA,
    "references": REFERENCES_DATA,
}

EVENT_METHOD = to_payment_method(**EVENT_METHOD_DATA)

SETTINGS = _parse_settings(
    gen_manifest_settings(
        namespace="trust",
        endpoint="payment",
        adapter_getters={
            "type": AdapterType.DefaultAdapter,
            "getters": {"some_getter_field": {"type": GetterType.ConstGetter, "arguments": {"const": 1}}},
        },
        currency_conversion={"RUB": "USD"},
    )
)

EVENT_ADAPTER = EventAdapter(EVENT_METHOD.event, SETTINGS, EVENT_METHOD.references)
COMPOSITE_ADAPTER = EventAdapter(EVENT_METHOD.event.composite_components[0], SETTINGS, header_adapter=EVENT_ADAPTER)


def test_event_adapter() -> None:
    event_method, settings, adapter = EVENT_METHOD, SETTINGS, EVENT_ADAPTER

    assert adapter.header_adapter == adapter
    assert adapter.event == event_method.event
    assert adapter.references == event_method.references

    assert len(adapter.payments) == 5
    hm.assert_that(adapter.payments, hm.contains_inanyorder(*[hm.instance_of(EventRowAdapter)] * 5))  # type: ignore

    row_with_reversal = adapter.payments[4]
    corresponding_reversal_row = adapter._find_reversals_for_payment_row(row_with_reversal.row)[0]
    hm.assert_that(corresponding_reversal_row, hm.instance_of(PaymentRowModel))
    assert corresponding_reversal_row.amount == Decimal(5)

    assert len(adapter.refunds) == 1
    hm.assert_that(adapter.refunds[0], hm.instance_of(EventRowAdapter))
    assert adapter.refunds[0].refund

    assert adapter.service_id == 1
    assert adapter.filters == settings.row_filters
    assert adapter.tariffer_payload == event_method.event.tariffer_payload
    assert adapter.cutoff_dt is None

    assert len(adapter.diod_keys) == 4
    hm.assert_that(adapter.diod_keys, hm.instance_of(dict))


def test_composite_component() -> None:
    event_method, settings, header_adapter = EVENT_METHOD, SETTINGS, EVENT_ADAPTER
    event_component, composite_adapter = EVENT_METHOD.event.composite_components[0], COMPOSITE_ADAPTER

    assert composite_adapter.header_adapter == header_adapter
    assert composite_adapter.event == event_component
    assert composite_adapter.references == event_method.references

    assert len(composite_adapter.payments) == 1
    hm.assert_that(composite_adapter.payments[0], hm.instance_of(EventRowAdapter))

    assert len(composite_adapter.refunds) == 0

    assert composite_adapter.service_id == 1
    assert composite_adapter.filters == settings.row_filters
    assert composite_adapter.tariffer_payload == event_method.event.tariffer_payload
    assert composite_adapter.cutoff_dt is None
    assert composite_adapter.postauth_dt == header_adapter.postauth_dt

    assert len(composite_adapter.diod_keys) == 4
    hm.assert_that(composite_adapter.diod_keys, hm.instance_of(dict))


def test_header_event_row_adapter() -> None:
    event_method, header_adapter = EVENT_METHOD, EVENT_ADAPTER
    row_adapter = header_adapter.payments[0]
    refund_row_adapter = header_adapter.refunds[0]

    # check dynamic_getters
    assert row_adapter.some_getter_field == 1

    assert header_adapter.ready_for_processing

    assert header_adapter.payments[0].state == TransactionState.NOT_MIGRATED
    assert header_adapter.payments[1].state == TransactionState.PROCESSED
    assert header_adapter.payments[2].state == TransactionState.PROCESSED_BEFORE
    assert header_adapter.payments[3].state == TransactionState.PROCESSED_BEFORE
    assert header_adapter.payments[4].state == TransactionState.PROCESSED

    assert row_adapter.on_dt == NOW_WITH_TZ
    hm.assert_that(row_adapter.current_signed, hm.instance_of(JSONContract.CurrentState))
    assert row_adapter.firm == event_method.references.firms[0]
    assert row_adapter.migrated is True
    assert row_adapter.dry_run is False
    assert row_adapter.tariffer_payload == {}
    hm.assert_that(row_adapter.tax_policy, hm.instance_of(TaxPolicyModel))
    assert Decimal(row_adapter.amount) == Decimal(10) * Decimal("60.5")  # 10 * conversion_rate
    assert row_adapter.amount_wo_vat == Decimal("504.166667")
    assert row_adapter.service_product == event_method.event.products[0]
    assert row_adapter.client_id == 1
    assert row_adapter.converted_currency == "USD"
    assert row_adapter.currency == "USD"
    assert row_adapter.contract_id == 1

    assert row_adapter.transaction_dt == NOW_WITH_TZ
    assert row_adapter.terminal_id == 1
    assert row_adapter.payment_type == "card"
    assert row_adapter.payment_method_id == PaymentMethodID.CARD
    assert row_adapter.service_id == 1
    assert row_adapter.product_id == "1"
    assert row_adapter.row == event_method.event.rows[0]

    assert row_adapter.refund is None
    assert refund_row_adapter.refund == event_method.event.refunds[1]

    assert row_adapter.parent_adapter == header_adapter
    assert row_adapter.header_adapter == header_adapter


def test_composite_event_row_adapter() -> None:
    event_method, header_adapter, parent_adapter = EVENT_METHOD, EVENT_ADAPTER, COMPOSITE_ADAPTER
    row_adapter = parent_adapter.payments[0]

    # check dynamic_getters
    assert row_adapter.some_getter_field == 1

    assert row_adapter.state == TransactionState.PROCESSED
    assert row_adapter.on_dt == NOW_WITH_TZ
    hm.assert_that(row_adapter.current_signed, hm.instance_of(JSONContract.CurrentState))
    assert row_adapter.firm == event_method.references.firms[0]
    assert row_adapter.migrated is True
    assert row_adapter.dry_run is False
    assert row_adapter.tariffer_payload == {}
    hm.assert_that(row_adapter.tax_policy, hm.instance_of(TaxPolicyModel))
    assert Decimal(row_adapter.amount) == Decimal(10) * Decimal("60.5")  # 10 * conversion_rate
    assert row_adapter.amount_wo_vat == Decimal("504.166667")
    assert row_adapter.service_product == event_method.event.composite_components[0].products[0]
    assert row_adapter.client_id == 1
    assert row_adapter.converted_currency == "USD"
    assert row_adapter.currency == "USD"
    assert row_adapter.contract_id == 1

    assert row_adapter.transaction_dt == NOW_WITH_TZ
    assert row_adapter.terminal_id == 1
    assert row_adapter.payment_type == "not_card"
    assert row_adapter.payment_method_id == PaymentMethodID.YAMONEY_WALLET
    assert row_adapter.service_id == 1
    assert row_adapter.product_id == "2"
    assert row_adapter.row == event_method.event.composite_components[0].rows[0]

    assert row_adapter.refund is None

    assert row_adapter.parent_adapter == parent_adapter
    assert row_adapter.header_adapter == header_adapter
