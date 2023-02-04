from maps.bizdir.sps.signal_processor.hypothesis_creator import (
    HypothesisCreator,
    _get_clearing_list_field_name,
    _is_clearing_attr,
    _is_nonempty_attr_present,
)
from maps.bizdir.sps.proto.business_internal_pb2 import (
    BusinessInternal,
    CompanyStateInfo,
    EmailsInfo,
    FeatureInfo,
)
from yandex.maps.proto.bizdir.sps.business_pb2 import Business
from yandex.maps.proto.bizdir.sps.signal_pb2 import Signal
import yandex.maps.proto.bizdir.common.hours_pb2 as hours
from yandex.maps.proto.bizdir.common.business_pb2 import (
    CLOSED,
    OPEN,
    CompanyName,
    CompanyNames,
    Email,
    Emails,
    Feature,
    Links,
    Phones,
    Rubrics,
)
from datetime import datetime
from typing import Any
import pytest
from unittest.mock import Mock, patch
from yandex.maps.proto.bizdir.sps.hypothesis_pb2 import (
    SIGNAL,
    SPS,
    CompanyNamesEvents,
    CompanyStateEvents,
    EditCompanyHypothesis,
    EmailsEvents,
    EventMetadata,
    FeatureEvents,
    NewCompanyHypothesis,
)
from yandex.maps.proto.bizdir.sps.verdict_pb2 import AttributeVerdict


PATH = "maps.bizdir.sps.signal_processor"


def test_get_clearing_list_field_name__given_message_name__returns_list_name() -> None:
    assert _get_clearing_list_field_name("open_hours") == "hours"
    assert _get_clearing_list_field_name("emails") == "email"
    assert _get_clearing_list_field_name("rubrics") == "rubric"
    assert _get_clearing_list_field_name("phones") == "phone"
    assert _get_clearing_list_field_name("links") == "link"


def test_is_clearing_attr__given_unknown_name__returns_false() -> None:
    assert not _is_clearing_attr("unknown", Emails())


@pytest.mark.parametrize(
    "attr_name, attr",
    [
        ("emails", Emails(email=[])),
        ("rubrics", Rubrics(rubric=[])),
        ("open_hours", hours.OpenHours(hours=[])),
        ("phones", Phones(phone=[])),
        ("links", Links(link=[])),
    ],
)
def test_is_clearing_attr__given_clearing_attr__returns_true(
    attr_name: str, attr: Any
) -> None:
    assert _is_clearing_attr(attr_name, attr)


@patch(
    f"{PATH}.proto_helpers.get_protocol_field",
    return_value=[Email(value="cocacola@gmail.com")],
)
def test_is_nonempty_attr_present__if_it_is_present_on_business__returns_true(
    _: Mock,
) -> None:

    assert _is_nonempty_attr_present("emails", BusinessInternal())


@patch(f"{PATH}.proto_helpers.get_protocol_field", return_value=[])
def test_is_nonempty_attr_present__if_it_is_not_present_on_business__returns_false(
    _: Mock,
) -> None:

    assert not _is_nonempty_attr_present("emails", BusinessInternal())


class Fixture:
    def __init__(self) -> None:
        self.cur_time = datetime.now()
        self.creator = HypothesisCreator(
            Signal(comment="comment"),
            BusinessInternal(),
            self.cur_time,
            "123",
            "345",
        )


@pytest.fixture(autouse=True)
def f() -> Fixture:
    return Fixture()


def test_add_imported_attr__given_emails_attr__imports_it(f: Fixture) -> None:
    events = EmailsEvents()
    f.creator._add_imported_attr(
        events, Emails(email=[Email(value="a@ya.ru"), Email(value="b@ya.ru")])
    )
    assert events.event[0].new_value.email[0].value == "a@ya.ru"
    assert events.event[0].new_value.email[1].value == "b@ya.ru"
    assert events.event[0].metadata.imported.signal_id == "123"
    assert events.event[0].metadata.source == SIGNAL
    assert events.event[0].metadata.signal.signal_id == "123"
    assert events.event[0].metadata.timestamp == int(f.cur_time.timestamp())
    assert events.event[0].metadata.message == "comment"


def test_add_imported_attr__given_company_state_attr__imports_it(
    f: Fixture,
) -> None:
    events = CompanyStateEvents()
    f.creator._add_imported_attr(events, CLOSED)
    assert events.event[0].new_value == CLOSED
    assert events.event[0].metadata.imported.signal_id == "123"
    assert events.event[0].metadata.source == SIGNAL
    assert events.event[0].metadata.signal.signal_id == "123"
    assert events.event[0].metadata.timestamp == int(f.cur_time.timestamp())
    assert events.event[0].metadata.message == "comment"


def test_add_technical_event__given_duplicate_event__adds_it(
    f: Fixture,
) -> None:
    events = CompanyStateEvents()
    f.creator._add_technical_event(events, AttributeVerdict.MarkAsDuplicate())
    assert events.event[0].metadata.source == SPS
    assert events.event[0].metadata.HasField("sps")
    assert events.event[0].metadata.WhichOneof("event") == "duplicate"


def test_add_technical_event__given_require_verdict_event__adds_it(
    f: Fixture,
) -> None:
    events = CompanyStateEvents()
    f.creator._add_technical_event(events, EventMetadata.RequireVerdict())
    assert events.event[0].metadata.source == SPS
    assert events.event[0].metadata.HasField("sps")
    assert events.event[0].metadata.WhichOneof("event") == "verdict_required"


def test_add_duplicated_import__given_attribute__add_2_events(
    f: Fixture,
) -> None:
    with (
        patch.object(f.creator, "_add_imported_attr") as add_imported_attr,
        patch.object(f.creator, "_add_technical_event") as add_technical_event,
    ):
        f.creator._add_duplicated_import(CompanyStateEvents(), OPEN)

        add_imported_attr.assert_called_once_with(
            CompanyStateEvents(), OPEN
        )
        add_technical_event.assert_called_once_with(
            CompanyStateEvents(), AttributeVerdict.MarkAsDuplicate()
        )


def test_add_verifiable_import__given_attribute__add_2_events(
    f: Fixture,
) -> None:
    with (
        patch.object(f.creator, "_add_imported_attr") as add_imported_attr,
        patch.object(f.creator, "_add_technical_event") as add_technical_event,
    ):
        f.creator._add_verifiable_import(CompanyStateEvents(), OPEN)

        add_imported_attr.assert_called_once_with(
            CompanyStateEvents(), OPEN
        )
        add_technical_event.assert_called_once_with(
            CompanyStateEvents(), EventMetadata.RequireVerdict()
        )


@patch(
    f"{PATH}.hypothesis_creator._is_nonempty_attr_present", return_value=False
)
def test_import_clearing_attr__if_it_is_not_present_on_business__calls_add_duplicated_import(
    _: Mock, f: Fixture
) -> None:
    with (
        patch.object(f.creator, "_add_duplicated_import")
        as add_duplicated_import,
        patch.object(f.creator, "_add_verifiable_import")
        as add_verifiable_import,
    ):
        f.creator._import_clearing_attr(Mock(), "some_name", Mock())

        add_duplicated_import.assert_called_once()
        add_verifiable_import.assert_not_called()


@patch(
    f"{PATH}.hypothesis_creator._is_nonempty_attr_present", return_value=True
)
def test_import_clearing_attr__if_it_is_present_on_business__calls_add_verifiable_import(
    _: Mock, f: Fixture
) -> None:
    with (
        patch.object(f.creator, "_add_duplicated_import")
        as add_duplicated_import,
        patch.object(f.creator, "_add_verifiable_import")
        as add_verifiable_import,
    ):
        f.creator._import_clearing_attr(Mock(), "some_name", Mock())

        add_duplicated_import.assert_not_called()
        add_verifiable_import.assert_called_once()


@patch(f"{PATH}.signal_comparator.are_attrs_same", return_value=True)
def test_import_changing_attr__if_it_is_present_on_business__calls_add_duplicated_import(
    _: Mock, f: Fixture
) -> None:
    f.creator._org = BusinessInternal(
        company_state=CompanyStateInfo(value=OPEN),
    )
    with (
        patch.object(f.creator, "_add_duplicated_import")
        as add_duplicated_import,
        patch.object(f.creator, "_add_verifiable_import")
        as add_verifiable_import,
    ):
        f.creator._import_changing_attr(CompanyStateEvents(), "company_state", OPEN)

        add_duplicated_import.assert_called_once_with(
            CompanyStateEvents(), OPEN
        )
        add_verifiable_import.assert_not_called()


@patch(f"{PATH}.signal_comparator.are_attrs_same", return_value=True)
def test_import_changing_attr__if_feature_present_on_business__calls_add_duplicated_import(
    _: Mock, f: Fixture
) -> None:
    f.creator._org = BusinessInternal(
        feature=[FeatureInfo(value=Feature(id="1"))]
    )
    with (
        patch.object(f.creator, "_add_duplicated_import")
        as add_duplicated_import,
        patch.object(f.creator, "_add_verifiable_import")
        as add_verifiable_import,
    ):
        f.creator._import_changing_attr(FeatureEvents(), "feature", Feature(id="1"))

        add_duplicated_import.assert_called_once_with(
            FeatureEvents(), Feature(id="1")
        )
        add_verifiable_import.assert_not_called()


def test_import_changing_attr__if_feature_not_present_on_business__calls_add_verifiable_import(
    f: Fixture
) -> None:
    with (
        patch.object(f.creator, "_add_duplicated_import")
        as add_duplicated_import,
        patch.object(f.creator, "_add_verifiable_import")
        as add_verifiable_import,
    ):
        f.creator._import_changing_attr(FeatureEvents(), "feature", Feature(id="1"))

        add_verifiable_import.assert_called_once_with(
            FeatureEvents(), Feature(id="1")
        )
        add_duplicated_import.assert_not_called()


def test_import_changing_attr__if_it_is_not_present_on_business__calls_add_verifiable_import(
    f: Fixture,
) -> None:
    with (
        patch.object(f.creator, "_add_duplicated_import")
        as add_duplicated_import,
        patch.object(f.creator, "_add_verifiable_import")
        as add_verifiable_import,
    ):
        f.creator._import_changing_attr(
            CompanyStateEvents(), "company_state", CLOSED
        )

        add_duplicated_import.assert_not_called()
        add_verifiable_import.assert_called_once_with(
            CompanyStateEvents(), CLOSED
        )


@patch(f"{PATH}.signal_comparator.are_attrs_same", return_value=False)
def test_import_changing_attr__if_different_attr_present_on_business__calls_add_verifiable_import(
    _: Mock, f: Fixture
) -> None:
    f.creator._org = BusinessInternal(
        company_state=CompanyStateInfo(value=CLOSED),
    )
    with (
        patch.object(f.creator, "_add_duplicated_import")
        as add_duplicated_import,
        patch.object(f.creator, "_add_verifiable_import")
        as add_verifiable_import,
    ):
        f.creator._import_changing_attr(CompanyStateEvents(), "company_state", OPEN)

        add_duplicated_import.assert_not_called()
        add_verifiable_import.assert_called_once_with(
            CompanyStateEvents(), OPEN
        )


@patch(f"{PATH}.hypothesis_creator._is_clearing_attr", return_value=True)
def test_build_edit_company_hypothesis__for_clearing_signal__calls_import_clearing_attr(
    _: Mock, f: Fixture
) -> None:
    f.creator._signal = Signal(company=Business(emails=Emails(email=[])))
    with (
        patch.object(f.creator, "_import_clearing_attr")
        as import_clearing_attr,
        patch.object(f.creator, "_import_changing_attr")
        as import_changing_attr,
    ):
        f.creator._build_edit_company_hypothesis()

        import_clearing_attr.assert_called_once_with(
            EmailsEvents(), "emails", Emails(email=[])
        )
        import_changing_attr.assert_not_called()


@patch(f"{PATH}.hypothesis_creator._is_clearing_attr", return_value=True)
def test_build_new_company_hypothesis__for_clearing_signal__no_attributes_imported(
    _: Mock, f: Fixture
) -> None:
    f.creator._signal = Signal(company=Business(links=Links(link=[])))
    with (
        patch.object(f.creator, "_add_verifiable_import")
        as add_verifiable_import
    ):
        f.creator._build_new_company_hypothesis()

        add_verifiable_import.assert_not_called()


@patch(f"{PATH}.hypothesis_creator._is_clearing_attr", return_value=False)
def test_build_edit_company_hypothesis__for_changing_signal__calls_import_changing_attr(
    _: Mock, f: Fixture
) -> None:
    f.creator._signal = Signal(company=Business(company_state=OPEN))
    with (
        patch.object(f.creator, "_import_clearing_attr")
        as import_clearing_attr,
        patch.object(f.creator, "_import_changing_attr")
        as import_changing_attr,
    ):
        f.creator._build_edit_company_hypothesis()

        import_clearing_attr.assert_not_called()
        import_changing_attr.assert_called_once_with(
            CompanyStateEvents(), "company_state", OPEN
        )


@patch(f"{PATH}.hypothesis_creator._is_clearing_attr", return_value=False)
def test_build_edit_company_hypothesis__for_feature_signal__calls_import_changing_attr(
    _: Mock, f: Fixture
) -> None:
    f.creator._signal = Signal(
        company=Business(
            feature=[Feature(id="1")],
        )
    )
    with (
        patch.object(f.creator, "_import_clearing_attr")
        as import_clearing_attr,
        patch.object(f.creator, "_import_changing_attr")
        as import_changing_attr,
    ):
        f.creator._build_edit_company_hypothesis()

        import_clearing_attr.assert_not_called()
        import_changing_attr.assert_called_once_with(
            FeatureEvents(), "feature", Feature(id="1")
        )


@patch(f"{PATH}.hypothesis_creator._is_clearing_attr", return_value=False)
def test_build_new_company_hypothesis__for_not_clearing_attr__imports_attrs(
    _: Mock,
    f: Fixture,
) -> None:
    f.creator._org = None
    f.creator._signal = Signal(
        company=Business(
            names=CompanyNames(
                lang_to_name={"EN": CompanyName(name="Coca cola")}
            )
        )
    )
    with (
        patch.object(f.creator, "_add_verifiable_import")
        as add_verifiable_import
    ):
        f.creator._build_new_company_hypothesis()

        add_verifiable_import.assert_called_once_with(
            CompanyNamesEvents(), f.creator._signal.company.names
        )


@patch(f"{PATH}.hypothesis_creator._is_clearing_attr", return_value=False)
def test_build_new_company_hypothesis__for_feature_attr__imports_attrs(
    _: Mock,
    f: Fixture,
) -> None:
    f.creator._org = None
    f.creator._signal = Signal(
        company=Business(
            feature=[Feature(id="1")],
        )
    )
    with (
        patch.object(f.creator, "_add_verifiable_import")
        as add_verifiable_import
    ):
        f.creator._build_new_company_hypothesis()

        add_verifiable_import.assert_called_once_with(
            FeatureEvents(), Feature(id="1")
        )


def test_create_hypothesis__for_changing_signal__creates_hypothesis_with_extrafields(
    f: Fixture,
) -> None:
    with (
        patch.object(f.creator, "_build_new_company_hypothesis")
        as build_new_company_hypothesis,
        patch.object(f.creator, "_build_edit_company_hypothesis")
        as build_edit_company_hypothesis,
    ):
        build_edit_company_hypothesis.return_value=EditCompanyHypothesis()
        hypothesis = f.creator.create_hypothesis()

        build_new_company_hypothesis.assert_not_called()
        assert hypothesis.WhichOneof("hypothesis") == "edit_company"


def test_create_hypothesis__for_new_signal__creates_hypothesis_with_extrafields(
    f: Fixture,
) -> None:
    f.creator._org = None
    with (
        patch.object(f.creator, "_build_new_company_hypothesis")
        as build_new_company_hypothesis,
        patch.object(f.creator, "_build_edit_company_hypothesis")
        as build_edit_company_hypothesis,
    ):
        build_new_company_hypothesis.return_value = NewCompanyHypothesis()
        hypothesis = f.creator.create_hypothesis()

        build_edit_company_hypothesis.assert_not_called()
        assert hypothesis.WhichOneof("hypothesis") == "new_company"


def test_create_hypothesis__for_new_signal__fills_with_extrafields(
    f: Fixture,
) -> None:
    with (
        patch.object(f.creator, "_build_edit_company_hypothesis")
        as build_edit_company_hypothesis,
    ):
        build_edit_company_hypothesis.return_value = EditCompanyHypothesis()
        hypothesis = f.creator.create_hypothesis()

        assert hypothesis.media_file == f.creator._signal.file
        assert hypothesis.message == f.creator._signal.comment


# integration tests below


def test_create_hypothesis__given_new_status__returns_hypothesis_to_verify() -> None:
    signal = Signal(company=Business(company_state=OPEN))
    org = BusinessInternal(company_state=CompanyStateInfo(value=CLOSED))
    creator = HypothesisCreator(
        signal, org, datetime.now(), "123", "345"
    )
    hypothesis = creator.create_hypothesis()

    result_events = hypothesis.edit_company.company_state.hypothesis.event
    assert result_events[0].new_value == OPEN
    assert result_events[0].metadata.signal.signal_id == "123"
    assert result_events[0].metadata.imported.signal_id == "123"
    assert result_events[1].metadata.HasField("verdict_required")


@patch(f"{PATH}.signal_comparator.are_attrs_same", return_value=True)
def test_create_hypothesis__given_old_status__returns_hypothesis_with_duplicate(
    _: Mock,
) -> None:
    signal = Signal(company=Business(company_state=OPEN))
    org = BusinessInternal(company_state=CompanyStateInfo(value=OPEN))
    creator = HypothesisCreator(
        signal, org, datetime.now(), "123", "345"
    )
    hypothesis = creator.create_hypothesis()

    result_events = hypothesis.edit_company.company_state.hypothesis.event
    assert result_events[0].new_value == OPEN
    assert result_events[0].metadata.signal.signal_id == "123"
    assert result_events[0].metadata.imported.signal_id == "123"
    assert result_events[1].metadata.HasField("duplicate")


def test_create_hypothesis__given_new_email__returns_hypothesis() -> None:
    signal = Signal(
        company=Business(
            emails=Emails(email=[Email(value="cocacola@gmail.com")])
        )
    )
    org = BusinessInternal(
        emails=EmailsInfo(value=Emails(email=[Email(value="fanta@gmail.com")]))
    )
    creator = HypothesisCreator(
        signal, org, datetime.now(), "123", "345"
    )
    hypothesis = creator.create_hypothesis()

    result_events = hypothesis.edit_company.emails.hypothesis.event
    assert result_events[0].new_value.email[0].value == "cocacola@gmail.com"
    assert result_events[0].metadata.signal.signal_id == "123"
    assert result_events[0].metadata.imported.signal_id == "123"
    assert result_events[1].metadata.HasField("verdict_required")
