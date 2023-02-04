from maps.bizdir.sps.signal_processor.proto_helpers import (
    get_protocol_field,
    optional_field,
    optional_fields,
    empty_or_equals,
    clear_field_if_empty_str
)
from yandex.maps.proto.bizdir.sps.signal_pb2 import Signal

from yandex.maps.proto.bizdir.sps.business_pb2 import Business

from yandex.maps.proto.bizdir.common.business_pb2 import (
    CLOSED,
    OPEN,
    CompanyName,
    CompanyNames,
    Email,
    Emails,
    Phone
)


def test_get_protocol_field__given_present_field__returns_field() -> None:
    signal = Signal(
        company=Business(
            names=CompanyNames(
                lang_to_name={"EN": CompanyName(name="Coca cola")}
            ),
            emails=Emails(email=[Email(value="cocacola@gmail.com")]),
            company_state=OPEN,
        )
    )

    assert (
        get_protocol_field(signal, ("company", "names", "lang_to_name"))[
            "EN"
        ].name
        == "Coca cola"
    )
    assert (
        get_protocol_field(signal, ("company", "emails", "email"))
        == signal.company.emails.email
    )
    assert get_protocol_field(signal, ("company", "company_state")) == OPEN


def test_get_protocol_field__given_missing_field__returns_default_value() -> None:
    signal = Signal(company=Business())
    assert get_protocol_field(signal, ("company", "rubrics", "rubric")) is None
    assert get_protocol_field(signal, ("missing_attr",), True)


def test_optional_field__given_present_field__returns_field() -> None:
    signal = Signal(company=Business(company_state=OPEN))

    assert optional_field(signal.company, "company_state") == OPEN


def test_optional_field__given_missing_field__returns_none() -> None:
    signal = Signal(company=Business())
    assert optional_field(signal.company, "rubrics") is None


def test_optional_fields__given_list_of_fields__returns_present_and_missing_fields() -> None:
    signal = Signal(
        company=Business(
            emails=Emails(email=[Email(value="cocacola@gmail.com")]),
            company_state=OPEN,
        )
    )

    assert optional_fields(
        signal.company, ["company_state", "emails", "rubrics"]
    ) == [OPEN, signal.company.emails, None]


def test_empty_or_equals__given_empty_field__returns_true() -> None:
    signal = Signal(company=Business(company_state=OPEN))
    empty_signal = Signal(company=Business())

    assert empty_or_equals(
        empty_signal.company, signal.company, "company_state"
    )


def test_empty_or_equals__given_equal_field__returns_true() -> None:
    signal = Signal(company=Business(company_state=OPEN))
    assert empty_or_equals(signal.company, signal.company, "company_state")


def test_empty_or_equals__given_notequal_fields__returns_false() -> None:
    signal = Signal(company=Business(company_state=OPEN))
    empty_signal = Signal(company=Business())
    not_equal_signal = Signal(company=Business(company_state=CLOSED))

    assert not empty_or_equals(
        signal.company, empty_signal.company, "company_state"
    )
    assert not empty_or_equals(
        signal.company, not_equal_signal.company, "company_state"
    )


def test_clear_field_if_empty_str__given_notset_field__do_nothing() -> None:
    ph = Phone()
    clear_field_if_empty_str(ph, "ext")

    assert not ph.HasField("ext")


def test_clear_field_if_empty_str__given_set_field__do_nothing() -> None:
    ph = Phone(ext="123")
    clear_field_if_empty_str(ph, "ext")

    assert ph.HasField("ext")


def test_clear_field_if_empty_str__given_empty_field__clears_it() -> None:
    ph = Phone(ext="")
    clear_field_if_empty_str(ph, "ext")

    assert not ph.HasField("ext")
