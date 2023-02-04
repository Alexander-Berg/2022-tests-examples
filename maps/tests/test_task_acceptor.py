import maps.bizdir.sps.validator.generic as generic
import maps.bizdir.sps.validator.pb as pb
import maps.bizdir.sps.validator.task_acceptor as task_acceptor
import maps.bizdir.sps.validator.tests.utils as utils
import yandex.maps.proto.bizdir.callcenter.business_submitted_pb2 as business_submitted_pb2
import yandex.maps.proto.bizdir.callcenter.business_validated_pb2 as business_validated_pb2
import yandex.maps.proto.bizdir.callcenter.callcenter_pb2 as callcenter_pb2
import yandex.maps.proto.bizdir.sps.hypothesis_pb2 as hypothesis_pb2
import yandex.maps.proto.bizdir.common.business_pb2 as business_pb2
import yandex.maps.proto.bizdir.common.hours_pb2 as hours_pb2

from collections.abc import Sequence
from typing import TypeVar, cast
from unittest import mock

import pytest


ENG_LOCALE = "EN"
RU_LOCALE = "RU"
MESSAGE = "Test operator message"
PATH = "maps.bizdir.sps.validator.task_acceptor"
TEST_TASK_ID = "42"
VOICE_RECORD = [
    callcenter_pb2.VoiceRecord(url="test.test/test-record-1"),
    callcenter_pb2.VoiceRecord(url="test.test/test-record-2"),
]
TEST_FEATURE_ID = "test-feature"
_T = TypeVar("_T")
TEST_PHONE_NUMBER = "8-800-42-42"


def validated_open_hours(
    day: "hours_pb2.DayOfWeek.V" = hours_pb2.DayOfWeek.SUNDAY,
) -> business_validated_pb2.OpenHoursValidated:
    return business_validated_pb2.OpenHoursValidated(
        value=hours_pb2.OpenHours(hours=[hours_pb2.Hours(day=[day])]),
        validation_status=pb.ValidationStatus.VALID,
    )


def validated_emails() -> generic.GenericRepeated[business_validated_pb2.EmailValidated]:
    return cast(
        generic.GenericRepeated[business_validated_pb2.EmailValidated],
        [
            business_validated_pb2.EmailValidated(
                value=business_pb2.Email(value="test@test.test"),
                validation_status=pb.ValidationStatus.INVALID,
            ),
            business_validated_pb2.EmailValidated(
                value=business_pb2.Email(value="valid_email@test.test"),
                validation_status=pb.ValidationStatus.VALID,
            ),
        ]
    )


def validated_name() -> business_validated_pb2.CompanyNameValidated:
    return business_validated_pb2.CompanyNameValidated(
        value=business_pb2.CompanyName(name="Test name"),
        validation_status=pb.ValidationStatus.VALID,
    )


@pytest.fixture()
def merger() -> task_acceptor.Merger:
    return task_acceptor.Merger(
        TEST_TASK_ID,
        pb.ValidationSource.CALL_CENTER,
        ENG_LOCALE,
        MESSAGE,
        VOICE_RECORD,
        TEST_PHONE_NUMBER,
    )


@pytest.fixture()
def acceptor() -> task_acceptor.TaskAcceptor:
    return task_acceptor.TaskAcceptor(ENG_LOCALE)


def check_metadata(
    metadata: pb.EventMetadata,
    status: "pb.EventMetadata.ValidationCompleted.Status.V" = pb.EventMetadata.ValidationCompleted.DONE,
    source: "pb.ValidationSource.V" = pb.ValidationSource.CALL_CENTER,
) -> None:
    assert metadata.source == pb.EventSource.VALIDATION
    assert metadata.HasField("timestamp")
    assert TEST_PHONE_NUMBER + "\n\n" in metadata.message
    assert MESSAGE in metadata.message
    assert metadata.WhichOneof("event_source") == "validator"
    assert metadata.validator.source == source
    assert metadata.validator.validation_task_id == TEST_TASK_ID
    assert metadata.WhichOneof("event") == "validation_completed"
    assert len(metadata.validation_completed.media_file) == len(VOICE_RECORD)
    assert {it.url for it in VOICE_RECORD} == {
        it.url for it in metadata.validation_completed.media_file
    }
    assert metadata.validation_completed.status == status


def get_validation_completed_event(
    events: Sequence[generic.GenericEvent[_T]],
) -> generic.GenericEvent[_T]:
    assert (
        events[-1].metadata.WhichOneof("event") == "validation_completed"
        or events[-2].metadata.WhichOneof("event") == "validation_completed"
    )

    return (
        events[-1]
        if events[-1].metadata.WhichOneof("event") == "validation_completed"
        else events[-2]
    )


def test_get_event_metadata__given_task_id_and_source__returns_validation_completed_event(
    merger: task_acceptor.Merger,
) -> None:
    metadata = merger._get_event_metadata(
        pb.EventMetadata.ValidationCompleted.DONE
    )

    check_metadata(metadata)


def test_get_event_metadata__with_message__adds_phone_number_to_message(
    merger: task_acceptor.Merger,
) -> None:
    metadata = merger._get_event_metadata(
        pb.EventMetadata.ValidationCompleted.DONE
    )

    assert TEST_PHONE_NUMBER + "\n\n" in metadata.message
    assert MESSAGE in metadata.message


def test_get_event_metadata__phone_number_without_message__writes_phone_number_in_message() -> None:
    merger = task_acceptor.Merger(
        TEST_TASK_ID,
        pb.ValidationSource.CALL_CENTER,
        ENG_LOCALE,
        None,
        [],
        TEST_PHONE_NUMBER,
    )
    metadata = merger._get_event_metadata(
        pb.EventMetadata.ValidationCompleted.DONE
    )

    assert TEST_PHONE_NUMBER in metadata.message
    assert "\n" not in metadata.message


def test_get_event_metadata__given_message_with_only_spaces__does_not_add_it_to_message() -> None:
    merger = task_acceptor.Merger(
        TEST_TASK_ID,
        pb.ValidationSource.CALL_CENTER,
        ENG_LOCALE,
        "   ",
        [],
        TEST_PHONE_NUMBER,
    )
    metadata = merger._get_event_metadata(
        pb.EventMetadata.ValidationCompleted.DONE
    )

    assert TEST_PHONE_NUMBER in metadata.message
    assert "\n" not in metadata.message


def test_add_verdict_required__when_all_validations_completed__adds_verdict_required_event(
    merger: task_acceptor.Merger,
) -> None:
    events = hypothesis_pb2.LinksEvents(
        event=[
            utils.validation_requested_event(
                [pb.ValidationSource.CALL_CENTER], 1, hypothesis_pb2.LinksEvent
            ),
            utils.validation_completed_event(
                pb.ValidationSource.CALL_CENTER, 2, hypothesis_pb2.LinksEvent
            ),
        ]
    )
    merger._add_verdict_required(events.event)
    metadata = events.event[-1].metadata
    assert metadata.WhichOneof("event") == "verdict_required"
    assert metadata.HasField("timestamp")
    assert metadata.WhichOneof("event_source") == "sps"
    assert metadata.source == pb.EventSource.SPS


def test_add_verdict_required__when_not_all_validations_completed__does_not_add_verdict_required_event(
    merger: task_acceptor.Merger,
) -> None:
    events = hypothesis_pb2.LinksEvents(
        event=[
            utils.validation_requested_event(
                [pb.ValidationSource.CALL_CENTER], 1, hypothesis_pb2.LinksEvent
            ),
        ]
    )
    merger._add_verdict_required(events.event)
    assert len(events.event) == 1
    assert (
        events.event[-1].metadata.WhichOneof("event") == "validation_requested"
    )


def test_merge_singular__when_submitted_value_is_none__does_not_add_new_event(
    merger: task_acceptor.Merger,
) -> None:
    hypothesis = pb.EditCompanyHypothesis()
    merger.merge_singular(
        hypothesis_field=hypothesis.open_hours,
        validated_value=validated_open_hours(),
        submitted_value=None,
    )
    assert not hypothesis.HasField("open_hours")


def test_merge_singular__when_no_action_was_required__does_not_add_new_event(
    merger: task_acceptor.Merger,
) -> None:
    hypothesis = pb.EditCompanyHypothesis()
    merger.merge_singular(
        hypothesis_field=hypothesis.open_hours,
        validated_value=validated_open_hours(),
        submitted_value=business_submitted_pb2.OpenHoursSubmitted(
            required_action=pb.RequiredAction.NO_ACTION,
        ),
    )
    assert not hypothesis.HasField("open_hours")


def test_merge_singular__when_validated_value_is_none__adds_unsuccessful_validation_completed(
    merger: task_acceptor.Merger,
) -> None:
    hypothesis = pb.EditCompanyHypothesis()
    merger.merge_singular(
        hypothesis_field=hypothesis.company_state,
        validated_value=None,
        submitted_value=business_submitted_pb2.CompanyStateSubmitted(
            required_action=pb.RequiredAction.REQUEST,
        ),
    )

    validation_completed = get_validation_completed_event(
        hypothesis.company_state.hypothesis.event
    )
    check_metadata(
        validation_completed.metadata,
        pb.EventMetadata.ValidationCompleted.UNSUCCESSFUL,
    )
    assert not validation_completed.HasField("validated_value")


def test_merge_singular__given_validated_value__adds_done_validation_completed(
    merger: task_acceptor.Merger,
) -> None:
    hypothesis = pb.EditCompanyHypothesis()
    merger.merge_singular(
        hypothesis_field=hypothesis.open_hours,
        validated_value=validated_open_hours(),
        submitted_value=business_submitted_pb2.OpenHoursSubmitted(
            required_action=pb.RequiredAction.REQUEST,
        ),
    )

    validation_completed = get_validation_completed_event(
        hypothesis.open_hours.hypothesis.event
    )
    check_metadata(validation_completed.metadata)
    assert validation_completed.validated_value == validated_open_hours()


def test_merge_repeated__when_no_action_was_required__does_not_add_new_event(
    merger: task_acceptor.Merger,
) -> None:
    hypothesis = pb.EditCompanyHypothesis()
    merger.merge_repeated(
        field_name="email",
        hypothesis_field=hypothesis.emails,
        validated_value=validated_emails(),
        submitted_value=[
            business_submitted_pb2.EmailSubmitted(
                required_action=pb.RequiredAction.NO_ACTION,
            )
        ],
    )
    assert not hypothesis.HasField("emails")


def test_merge_repeated__when_validated_value_is_empty__adds_unsuccessful_validation_completed(
    merger: task_acceptor.Merger,
) -> None:
    hypothesis = pb.EditCompanyHypothesis()
    merger.merge_repeated(
        field_name="email",
        hypothesis_field=hypothesis.emails,
        validated_value=cast(
            generic.GenericRepeated[business_validated_pb2.EmailValidated],
            []
        ),
        submitted_value=[
            business_submitted_pb2.EmailSubmitted(
                required_action=pb.RequiredAction.REQUEST,
            )
        ],
    )

    validation_completed = get_validation_completed_event(
        hypothesis.emails.hypothesis.event
    )
    check_metadata(
        validation_completed.metadata,
        pb.EventMetadata.ValidationCompleted.UNSUCCESSFUL,
    )
    assert len(validation_completed.validated_value.email) == 0


def test_merge_repeated__given_validated_values__adds_done_validation_completed(
    merger: task_acceptor.Merger,
) -> None:
    hypothesis = pb.EditCompanyHypothesis()
    emails_validated = validated_emails()
    merger.merge_repeated(
        field_name="email",
        hypothesis_field=hypothesis.emails,
        validated_value=emails_validated,
        submitted_value=[
            business_submitted_pb2.EmailSubmitted(
                required_action=pb.RequiredAction.REQUEST,
            )
        ],
    )

    validation_completed = get_validation_completed_event(
        hypothesis.emails.hypothesis.event
    )
    check_metadata(validation_completed.metadata)
    assert len(validation_completed.validated_value.email) == len(
        emails_validated
    )
    for email in validation_completed.validated_value.email:
        assert any(email == x for x in emails_validated)


def test_merge_localized_map__when_submitted_value_is_none__does_not_add_new_event(
    merger: task_acceptor.Merger,
) -> None:
    hypothesis = pb.EditCompanyHypothesis()
    merger.merge_localized_map(
        hypothesis=hypothesis.names,
        map_name="lang_to_name",
        validated_value=validated_name(),
        submitted_value=None,
    )
    assert not hypothesis.HasField("names")


def test_merge_localized_map__when_no_action_was_required__does_not_add_new_event(
    merger: task_acceptor.Merger,
) -> None:
    hypothesis = pb.EditCompanyHypothesis()
    merger.merge_localized_map(
        hypothesis=hypothesis.names,
        map_name="lang_to_name",
        validated_value=validated_name(),
        submitted_value=business_submitted_pb2.CompanyNameSubmitted(
            required_action=pb.RequiredAction.NO_ACTION,
        ),
    )
    assert not hypothesis.HasField("names")


def test_merge_localized_map__when_validated_value_is_none__adds_unsuccessful_validation_completed(
    merger: task_acceptor.Merger,
) -> None:
    hypothesis = pb.EditCompanyHypothesis()
    merger.merge_localized_map(
        hypothesis=hypothesis.names,
        map_name="lang_to_name",
        validated_value=None,
        submitted_value=business_submitted_pb2.CompanyNameSubmitted(
            required_action=pb.RequiredAction.REQUEST,
        ),
    )

    validation_completed = get_validation_completed_event(
        hypothesis.names.hypothesis.event
    )
    check_metadata(
        validation_completed.metadata,
        pb.EventMetadata.ValidationCompleted.UNSUCCESSFUL,
    )
    assert not validation_completed.HasField("validated_value")


def test_merge_localized_map__given_validated_values__adds_done_validation_completed(
    merger: task_acceptor.Merger,
) -> None:
    hypothesis = pb.EditCompanyHypothesis()
    merger.merge_localized_map(
        hypothesis=hypothesis.names,
        map_name="lang_to_name",
        validated_value=validated_name(),
        submitted_value=business_submitted_pb2.CompanyNameSubmitted(
            required_action=pb.RequiredAction.REQUEST,
        ),
    )

    validation_completed = get_validation_completed_event(
        hypothesis.names.hypothesis.event
    )
    check_metadata(validation_completed.metadata)
    assert ENG_LOCALE in validation_completed.validated_value.lang_to_name
    assert (
        validation_completed.validated_value.lang_to_name[ENG_LOCALE]
        == validated_name()
    )


@mock.patch(f"{PATH}.Merger.merge_singular")
def test_merge_features__when_submitted_feature_is_not_in_hypothesis__does_not_call_merge_singular(
    merge_singular_mock: mock.Mock, merger: task_acceptor.Merger
) -> None:
    hypothesis = pb.EditCompanyHypothesis(
        feature=[
            pb.HypothesisFeature(value=business_pb2.Feature(id=TEST_FEATURE_ID))
        ]
    )
    merger.merge_features(
        hypothesis_features=hypothesis.feature,
        validated_features=[],
        submitted_features=[
            pb.FeatureSubmitted(
                id="test-feature-2",
            )
        ],
    )

    merge_singular_mock.assert_not_called()


@mock.patch(f"{PATH}.Merger.merge_singular")
def test_merge_features__without_submitted_features__does_not_call_merge_singular(
    merge_singular_mock: mock.Mock, merger: task_acceptor.Merger
) -> None:
    hypothesis = pb.EditCompanyHypothesis(
        feature=[
            pb.HypothesisFeature(value=business_pb2.Feature(id=TEST_FEATURE_ID))
        ]
    )
    merger.merge_features(
        hypothesis_features=hypothesis.feature,
        validated_features=[pb.FeatureValidated(id=TEST_FEATURE_ID)],
        submitted_features=[],
    )

    merge_singular_mock.assert_not_called()


@mock.patch(f"{PATH}.Merger.merge_singular")
def test_merge_features__when_feature_in_hypothesis_and_submitted__calls_merge_singular(
    merge_singular_mock: mock.Mock, merger: task_acceptor.Merger
) -> None:
    hypothesis = pb.EditCompanyHypothesis()
    hypothesis_feature = hypothesis.feature.add(
        value=business_pb2.Feature(id=TEST_FEATURE_ID)
    )
    feature_submitted = pb.FeatureSubmitted(
        id=TEST_FEATURE_ID,
    )
    feature_validated = pb.FeatureValidated(
        id=TEST_FEATURE_ID,
    )
    merger.merge_features(
        hypothesis_features=hypothesis.feature,
        validated_features=[feature_validated],
        submitted_features=[feature_submitted],
    )

    merge_singular_mock.assert_called_once_with(
        hypothesis_feature,
        feature_validated,
        feature_submitted,
    )


@mock.patch(f"{PATH}.TaskAcceptor._merge_business_with_validated_business")
def test_process_task_result__given_task_result__calls_merge_for_each_call_result(
    merge_mock: mock.Mock, acceptor: task_acceptor.TaskAcceptor
) -> None:
    task_result = pb.TaskResult(
        task=callcenter_pb2.Task(),
        call_result=[
            callcenter_pb2.CallResult(),
            callcenter_pb2.CallResult(),
        ],
    )
    acceptor.process_task_result(
        TEST_TASK_ID,
        pb.ValidationSource.CALL_CENTER,
        pb.EditCompanyHypothesis(),
        task_result,
    )
    assert merge_mock.call_count == 2
