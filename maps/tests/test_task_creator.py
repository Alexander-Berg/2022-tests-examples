import maps.bizdir.sps.validator.generic as generic
import maps.bizdir.sps.validator.pb as pb
import maps.bizdir.sps.validator.task_creator as task_creator
import maps.bizdir.sps.proto.business_internal_pb2 as business_internal_pb2
import yandex.maps.proto.bizdir.common.business_pb2 as business_pb2
import yandex.maps.proto.bizdir.common.hours_pb2 as hours_pb2
import yandex.maps.proto.bizdir.sps.hypothesis_pb2 as hypothesis_pb2

from maps.bizdir.sps.validator.tests.utils import (
    validation_completed_event,
    validation_requested_event,
)

from collections.abc import Mapping
from typing import Optional, TypeVar
from unittest import mock

import pytest


ENG_LOCALE = "EN"
RU_LOCALE = "RU"
PATH = "maps.bizdir.sps.validator.task_creator.TaskCreator"
_T = TypeVar("_T")
_U = TypeVar("_U", bound=generic.GenericEvent)


def imported_event(
    value: Optional[_T], timestamp: int, factory: type[_U]
) -> _U:
    return factory(
        metadata=pb.EventMetadata(
            imported=pb.EventMetadata.Import(), timestamp=timestamp
        ),
        new_value=value,
    )


def open_hours(
    day: "hours_pb2.DayOfWeek.V" = hours_pb2.DayOfWeek.MONDAY,
) -> hours_pb2.OpenHours:
    return hours_pb2.OpenHours(hours=[hours_pb2.Hours(day=[day])])


def open_hours_info(
    day: "hours_pb2.DayOfWeek.V" = hours_pb2.DayOfWeek.SUNDAY,
) -> business_internal_pb2.OpenHoursInfo:
    return business_internal_pb2.OpenHoursInfo(value=open_hours(day))


def company_name(
    locale: str = ENG_LOCALE, name: str = "Test name"
) -> Mapping[str, business_pb2.CompanyName]:
    return {locale: business_pb2.CompanyName(name=name)}


def company_names_info(
    locale: str = ENG_LOCALE, name: str = "Pewterschmidt Industries"
) -> business_internal_pb2.CompanyNamesInfo:
    return business_internal_pb2.CompanyNamesInfo(
        value=business_pb2.CompanyNames(
            lang_to_name={locale: business_pb2.CompanyName(name=name)}
        )
    )


def phones(formatted_phones: list[str] = ["8-800-4242"]) -> business_pb2.Phones:
    return business_pb2.Phones(
        phone=[
            business_pb2.Phone(formatted=phone) for phone in formatted_phones
        ]
    )


def phones_info(
    formatted_phones: list[str] = ["8-555-4242"],
) -> business_internal_pb2.PhonesInfo:
    return business_internal_pb2.PhonesInfo(value=phones(formatted_phones))


def feature_with_value() -> business_pb2.Feature:
    return business_pb2.Feature(
        id="1",
        value=business_pb2.FeatureValue(
            bool_feature=business_pb2.BoolFeature(value=True)
        ),
    )


def feature_without_value() -> business_pb2.Feature:
    return business_pb2.Feature(
        id="1",
    )


def hypothesis(
    source: "pb.ValidationSource.V" = pb.ValidationSource.CALL_CENTER,
) -> pb.EditCompanyHypothesis:
    return pb.EditCompanyHypothesis(
        names=hypothesis_pb2.HypothesisCompanyNames(
            hypothesis=hypothesis_pb2.CompanyNamesEvents(
                event=[
                    validation_requested_event(
                        [source],
                        1,
                        hypothesis_pb2.CompanyNamesEvent,
                        business_pb2.CompanyNames(
                            lang_to_name=company_name(),
                        ),
                    )
                ]
            )
        )
    )


@pytest.fixture()
def creator() -> task_creator.TaskCreator:
    return task_creator.TaskCreator(ENG_LOCALE)


def test_required_action__without_validation_requested__returns_no_action(
    creator: task_creator.TaskCreator,
) -> None:
    events = hypothesis_pb2.OpenHoursEvents(
        event=[
            imported_event(
                open_hours(),
                1,
                hypothesis_pb2.OpenHoursEvent,
            )
        ]
    )
    assert (
        creator._required_action(pb.ValidationSource.CALL_CENTER, events)
        == pb.RequiredAction.NO_ACTION
    )


def test_required_action__when_all_validation_requests_are_completed__returns_no_action(
    creator: task_creator.TaskCreator,
) -> None:
    events = hypothesis_pb2.LinksEvents(
        event=[
            validation_requested_event(
                [pb.ValidationSource.CALL_CENTER], 1, hypothesis_pb2.LinksEvent
            ),
            validation_completed_event(
                pb.ValidationSource.CALL_CENTER, 2, hypothesis_pb2.LinksEvent
            ),
        ]
    )
    assert (
        creator._required_action(pb.ValidationSource.CALL_CENTER, events)
        == pb.RequiredAction.NO_ACTION
    )


def test_required_action__when_validation_requested_for_another_validator__returns_no_action(
    creator: task_creator.TaskCreator,
) -> None:
    events = hypothesis_pb2.LinksEvents(
        event=[
            validation_requested_event(
                [pb.ValidationSource.CALL_CENTER], 1, hypothesis_pb2.LinksEvent
            ),
        ]
    )
    assert (
        creator._required_action(pb.ValidationSource.TOLOKA, events)
        == pb.RequiredAction.NO_ACTION
    )


def test_required_action__when_validation_requested__returns_validate(
    creator: task_creator.TaskCreator,
) -> None:
    events = hypothesis_pb2.LinksEvents(
        event=[
            validation_requested_event(
                [pb.ValidationSource.CALL_CENTER], 1, hypothesis_pb2.LinksEvent
            ),
        ]
    )
    assert (
        creator._required_action(pb.ValidationSource.CALL_CENTER, events)
        == pb.RequiredAction.VALIDATE
    )


def test_get_value_from_events__without_validation_requested__returns_none(
    creator: task_creator.TaskCreator,
) -> None:
    events = hypothesis_pb2.OpenHoursEvents()
    events.event.extend(
        [
            imported_event(
                None,
                1,
                hypothesis_pb2.OpenHoursEvent,
            )
        ]
    )

    value = creator._get_value_from_events(
        pb.ValidationSource.CALL_CENTER,
        events,
    )

    assert value is None


def test_get_value_from_events__no_new_value_in_validated_requested__returns_new_value_from_imported(
    creator: task_creator.TaskCreator,
) -> None:
    events = hypothesis_pb2.OpenHoursEvents()
    events.event.extend(
        [
            imported_event(
                open_hours(),
                1,
                hypothesis_pb2.OpenHoursEvent,
            ),
            validation_requested_event(
                [pb.ValidationSource.CALL_CENTER],
                2,
                hypothesis_pb2.OpenHoursEvent,
            ),
        ]
    )
    value = creator._get_value_from_events(
        pb.ValidationSource.CALL_CENTER,
        events,
    )

    assert value == open_hours()


def test_get_value_from_events__given_several_imported__returns_value_from_last_one(
    creator: task_creator.TaskCreator,
) -> None:
    events = hypothesis_pb2.OpenHoursEvents()
    events.event.extend(
        [
            imported_event(
                open_hours(hours_pb2.DayOfWeek.MONDAY),
                1,
                hypothesis_pb2.OpenHoursEvent,
            ),
            imported_event(
                open_hours(hours_pb2.DayOfWeek.TUESDAY),
                1,
                hypothesis_pb2.OpenHoursEvent,
            ),
            validation_requested_event(
                [pb.ValidationSource.CALL_CENTER],
                2,
                hypothesis_pb2.OpenHoursEvent,
            ),
        ]
    )
    value = creator._get_value_from_events(
        pb.ValidationSource.CALL_CENTER,
        events,
    )

    assert value == open_hours(hours_pb2.DayOfWeek.TUESDAY)


def test_get_value_from_events__with_new_value_in_validated_requested__returns_new_value_from_validation_requested(
    creator: task_creator.TaskCreator,
) -> None:
    events = hypothesis_pb2.OpenHoursEvents()
    events.event.extend(
        [
            imported_event(
                open_hours(hours_pb2.DayOfWeek.MONDAY),
                1,
                hypothesis_pb2.OpenHoursEvent,
            ),
            validation_requested_event(
                [pb.ValidationSource.CALL_CENTER],
                2,
                hypothesis_pb2.OpenHoursEvent,
                open_hours(hours_pb2.DayOfWeek.SUNDAY),
            ),
        ]
    )

    value = creator._get_value_from_events(
        pb.ValidationSource.CALL_CENTER,
        events,
    )

    assert value == open_hours(hours_pb2.DayOfWeek.SUNDAY)


def test_get_value_from_events__without_value_in_imported_and_request__returns_none(
    creator: task_creator.TaskCreator,
) -> None:
    events = hypothesis_pb2.OpenHoursEvents()
    events.event.extend(
        [
            imported_event(
                None,
                1,
                hypothesis_pb2.OpenHoursEvent,
            ),
            validation_requested_event(
                [pb.ValidationSource.CALL_CENTER],
                2,
                hypothesis_pb2.OpenHoursEvent,
            ),
        ]
    )

    value = creator._get_value_from_events(
        pb.ValidationSource.CALL_CENTER,
        events,
    )

    assert value is None


@mock.patch(
    f"{PATH}._required_action", return_value=pb.RequiredAction.NO_ACTION
)
@mock.patch(f"{PATH}._get_localized_map_from_events")
def test_convert_localized_map__given_no_action__returns_value_from_business_info(
    mocked_value: mock.Mock,
    mocked_action: mock.Mock,
    creator: task_creator.TaskCreator,
) -> None:
    mocked_value.return_value = company_name()
    names_info = company_names_info()
    converted = creator._convert_localized_map(
        pb.ValidationSource.CALL_CENTER,
        hypothesis_pb2.CompanyNamesEvents(),
        names_info,
        "lang_to_name",
        pb.CompanyNameSubmitted,
    )
    assert converted is not None
    assert converted.value == names_info.value.lang_to_name[ENG_LOCALE]
    assert converted.required_action == pb.RequiredAction.NO_ACTION


@mock.patch(
    f"{PATH}._required_action", return_value=pb.RequiredAction.NO_ACTION
)
@mock.patch(f"{PATH}._get_localized_map_from_events")
def test_convert_localized_map__given_no_action_and_no_needed_locale__returns_None(
    mocked_value: mock.Mock,
    mocked_action: mock.Mock,
    creator: task_creator.TaskCreator,
) -> None:
    mocked_value.return_value = company_name()
    converted = creator._convert_localized_map(
        pb.ValidationSource.CALL_CENTER,
        hypothesis_pb2.CompanyNamesEvents(),
        company_names_info(RU_LOCALE),
        "lang_to_name",
        pb.CompanyNameSubmitted,
    )
    assert converted is None


@mock.patch(f"{PATH}._required_action", return_value=pb.RequiredAction.VALIDATE)
@mock.patch(f"{PATH}._get_localized_map_from_events")
def test_convert_localized_map__given_action_validate_and_no_new_value__raises(
    mocked_value: mock.Mock,
    mocked_action: mock.Mock,
    creator: task_creator.TaskCreator,
) -> None:
    mocked_value.return_value = {}
    with pytest.raises(Exception):
        creator._convert_localized_map(
            pb.ValidationSource.CALL_CENTER,
            hypothesis_pb2.CompanyNamesEvents(),
            company_names_info(),
            "lang_to_name",
            pb.CompanyNameSubmitted,
        )


@mock.patch(f"{PATH}._required_action", return_value=pb.RequiredAction.VALIDATE)
@mock.patch(f"{PATH}._get_localized_map_from_events")
def test_convert_localized_map__given_action_validate_and_no_needed_locale__raises(
    mocked_value: mock.Mock,
    mocked_action: mock.Mock,
    creator: task_creator.TaskCreator,
) -> None:
    mocked_value.return_value = company_name(RU_LOCALE)
    with pytest.raises(Exception):
        creator._convert_localized_map(
            pb.ValidationSource.CALL_CENTER,
            hypothesis_pb2.CompanyNamesEvents(),
            company_names_info(),
            "lang_to_name",
            pb.CompanyNameSubmitted,
        )


@mock.patch(f"{PATH}._required_action", return_value=pb.RequiredAction.VALIDATE)
@mock.patch(f"{PATH}._get_localized_map_from_events")
def test_convert_localized_map__given_action_validate_and_with_locale_in_new_value__copies_new_value(
    mocked_value: mock.Mock,
    mocked_action: mock.Mock,
    creator: task_creator.TaskCreator,
) -> None:
    mocked_value.return_value = company_name()
    converted = creator._convert_localized_map(
        pb.ValidationSource.CALL_CENTER,
        hypothesis_pb2.CompanyNamesEvents(),
        company_names_info(),
        "lang_to_name",
        pb.CompanyNameSubmitted,
    )
    assert converted is not None
    assert converted.value == mocked_value.return_value[ENG_LOCALE]
    assert converted.required_action == pb.RequiredAction.VALIDATE


@mock.patch(
    f"{PATH}._required_action", return_value=pb.RequiredAction.NO_ACTION
)
@mock.patch(f"{PATH}._get_value_from_events")
def test_convert_singular_field__given_no_action__returns_value_from_business_info(
    mocked_value: mock.Mock,
    mocked_action: mock.Mock,
    creator: task_creator.TaskCreator,
) -> None:
    mocked_value.return_value = open_hours()
    hours = open_hours_info()
    converted = creator._convert_singular_field(
        pb.ValidationSource.CALL_CENTER,
        hypothesis_pb2.OpenHoursEvents(),
        hours,
        pb.OpenHoursSubmitted,
    )
    assert converted is not None
    assert converted.value == hours.value
    assert converted.required_action == pb.RequiredAction.NO_ACTION


@mock.patch(
    f"{PATH}._required_action", return_value=pb.RequiredAction.NO_ACTION
)
@mock.patch(f"{PATH}._get_value_from_events")
def test_convert_singular_field__given_no_action_no_value__returns_None(
    mocked_value: mock.Mock,
    mocked_action: mock.Mock,
    creator: task_creator.TaskCreator,
) -> None:
    mocked_value.return_value = open_hours()
    converted = creator._convert_singular_field(
        pb.ValidationSource.CALL_CENTER,
        hypothesis_pb2.OpenHoursEvents(),
        business_internal_pb2.OpenHoursInfo(),
        pb.OpenHoursSubmitted,
    )
    assert converted is None


@mock.patch(f"{PATH}._required_action", return_value=pb.RequiredAction.VALIDATE)
@mock.patch(f"{PATH}._get_value_from_events", return_value=None)
def test_convert_singular_field__given_action_validate_and_no_new_value__raises(
    mocked_value: mock.Mock,
    mocked_action: mock.Mock,
    creator: task_creator.TaskCreator,
) -> None:
    with pytest.raises(Exception):
        creator._convert_singular_field(
            pb.ValidationSource.CALL_CENTER,
            hypothesis_pb2.OpenHoursEvents(),
            open_hours_info(),
            pb.OpenHoursSubmitted,
        )


@mock.patch(f"{PATH}._required_action", return_value=pb.RequiredAction.VALIDATE)
@mock.patch(f"{PATH}._get_value_from_events")
def test_convert_singular_field__given_action_validate_with_new_value__copies_new_value(
    mocked_value: mock.Mock,
    mocked_action: mock.Mock,
    creator: task_creator.TaskCreator,
) -> None:
    mocked_value.return_value = open_hours()
    converted = creator._convert_singular_field(
        pb.ValidationSource.CALL_CENTER,
        hypothesis_pb2.OpenHoursEvents(),
        open_hours_info(),
        pb.OpenHoursSubmitted,
    )
    assert converted is not None
    assert converted.value == mocked_value.return_value
    assert converted.required_action == pb.RequiredAction.VALIDATE


@mock.patch(
    f"{PATH}._required_action", return_value=pb.RequiredAction.NO_ACTION
)
@mock.patch(f"{PATH}._get_value_from_events")
def test_convert_repeated_field__given_no_action__returns_value_from_business_info(
    mocked_value: mock.Mock,
    mocked_action: mock.Mock,
    creator: task_creator.TaskCreator,
) -> None:
    mocked_value.return_value = phones()
    info_phones = phones_info()
    converted = creator._convert_repeated_field(
        pb.ValidationSource.CALL_CENTER,
        hypothesis_pb2.PhonesEvents(),
        info_phones,
        "phone",
        pb.PhoneSubmitted,
    )
    assert len(converted) == len(info_phones.value.phone)
    for phone in converted:
        assert phone.required_action == pb.RequiredAction.NO_ACTION
        assert any(phone.value == x for x in info_phones.value.phone)


@mock.patch(
    f"{PATH}._required_action", return_value=pb.RequiredAction.NO_ACTION
)
@mock.patch(f"{PATH}._get_value_from_events")
def test_convert_repeated_field__given_no_action_no_value__returns_empty_list(
    mocked_value: mock.Mock,
    mocked_action: mock.Mock,
    creator: task_creator.TaskCreator,
) -> None:
    mocked_value.return_value = phones()
    converted = creator._convert_repeated_field(
        pb.ValidationSource.CALL_CENTER,
        hypothesis_pb2.PhonesEvents(),
        business_internal_pb2.PhonesInfo(),
        "phone",
        pb.PhoneSubmitted,
    )
    assert len(converted) == 0


@mock.patch(f"{PATH}._required_action", return_value=pb.RequiredAction.VALIDATE)
@mock.patch(f"{PATH}._get_value_from_events")
def test_convert_repeated_field__given_action_validate_and_no_values__raises(
    mocked_value: mock.Mock,
    mocked_action: mock.Mock,
    creator: task_creator.TaskCreator,
) -> None:
    mocked_value.return_value = business_pb2.Phones()
    with pytest.raises(Exception):
        creator._convert_repeated_field(
            pb.ValidationSource.CALL_CENTER,
            hypothesis_pb2.PhonesEvents(),
            business_internal_pb2.PhonesInfo(),
            "phone",
            pb.PhoneSubmitted,
        )


@mock.patch(f"{PATH}._required_action", return_value=pb.RequiredAction.VALIDATE)
@mock.patch(f"{PATH}._get_value_from_events")
def test_convert_repeated_field__given_action_validate__merges_values_from_business_and_hypothesis(
    mocked_value: mock.Mock,
    mocked_action: mock.Mock,
    creator: task_creator.TaskCreator,
) -> None:
    mocked_value.return_value = phones()
    info_phones = phones_info()
    converted = creator._convert_repeated_field(
        pb.ValidationSource.CALL_CENTER,
        hypothesis_pb2.PhonesEvents(),
        info_phones,
        "phone",
        pb.PhoneSubmitted,
    )
    assert len(converted) == len(info_phones.value.phone) + len(
        mocked_value.return_value.phone
    )
    for phone in converted:
        assert phone.required_action == pb.RequiredAction.VALIDATE
        assert any(phone.value == x for x in info_phones.value.phone) or any(
            phone.value == x for x in mocked_value.return_value.phone
        )


@mock.patch(f"{PATH}._required_action", return_value=pb.RequiredAction.VALIDATE)
@mock.patch(f"{PATH}._get_value_from_events")
def test_convert_repeated_field__given_action_validate__remove_duplicates_while_merging(
    mocked_value: mock.Mock,
    mocked_action: mock.Mock,
    creator: task_creator.TaskCreator,
) -> None:
    mocked_value.return_value = phones_info().value
    converted = creator._convert_repeated_field(
        pb.ValidationSource.CALL_CENTER,
        hypothesis_pb2.PhonesEvents(),
        phones_info(),
        "phone",
        pb.PhoneSubmitted,
    )
    assert len(converted) == len(mocked_value.return_value.phone)


@mock.patch(f"{PATH}._required_action", return_value=pb.RequiredAction.VALIDATE)
@mock.patch(f"{PATH}._get_value_from_events")
def test_convert_features__given_action_validate_and_no_value__raises(
    mocked_value: mock.Mock,
    mocked_action: mock.Mock,
    creator: task_creator.TaskCreator,
) -> None:
    mocked_value.return_value = feature_without_value()
    with pytest.raises(Exception):
        creator._convert_features(
            pb.ValidationSource.CALL_CENTER,
            [pb.HypothesisFeature()],
        )


@mock.patch(f"{PATH}._required_action", return_value=pb.RequiredAction.VALIDATE)
@mock.patch(f"{PATH}._get_value_from_events")
def test_convert_features__given_action_validate_and_value__copies_value(
    mocked_value: mock.Mock,
    mocked_action: mock.Mock,
    creator: task_creator.TaskCreator,
) -> None:
    mocked_value.return_value = feature_with_value()
    converted = creator._convert_features(
        pb.ValidationSource.CALL_CENTER,
        [pb.HypothesisFeature()],
    )
    assert len(converted) == 1
    assert converted[0].id == mocked_value.return_value.id
    assert converted[0].value == mocked_value.return_value.value
    assert converted[0].required_action == pb.RequiredAction.VALIDATE


@mock.patch(
    f"{PATH}._required_action", return_value=pb.RequiredAction.NO_ACTION
)
@mock.patch(f"{PATH}._get_value_from_events")
def test_convert_features__given_no_action__doesnt_add_feature(
    mocked_value: mock.Mock,
    mocked_action: mock.Mock,
    creator: task_creator.TaskCreator,
) -> None:
    mocked_value.return_value = feature_with_value()
    converted = creator._convert_features(
        pb.ValidationSource.CALL_CENTER,
        [pb.HypothesisFeature()],
    )
    assert len(converted) == 0


@mock.patch(f"{PATH}._required_action", return_value=pb.RequiredAction.VALIDATE)
@mock.patch(f"{PATH}._get_value_from_events", return_value=None)
def test_convert_features__when_value_is_none__doesnt_add_feature(
    mocked_value: mock.Mock,
    mocked_action: mock.Mock,
    creator: task_creator.TaskCreator,
) -> None:
    converted = creator._convert_features(
        pb.ValidationSource.CALL_CENTER,
        [pb.HypothesisFeature()],
    )
    assert len(converted) == 0


@mock.patch(
    f"{PATH}._required_action", return_value=pb.RequiredAction.NO_ACTION
)
def test_create_validation_task__when_no_action_required_for_all_fields__returns_none(
    mocked_value: mock.Mock, creator: task_creator.TaskCreator
) -> None:
    assert (
        creator.create_validation_task(
            pb.ValidationSource.CALL_CENTER,
            pb.EditCompanyHypothesis(),
            pb.BusinessInternal(),
        )
        is None
    )


def test_create_validation_task__without_phone_in_business_submitted__returns_none(
    creator: task_creator.TaskCreator,
) -> None:
    assert (
        creator.create_validation_task(
            pb.ValidationSource.CALL_CENTER,
            hypothesis(),
            business_internal_pb2.BusinessInternal(names=company_names_info()),
        )
        is None
    )


def test_create_validation_task__given_hypothesis_with_validation_requested_and_phone__returns_business_submitted(
    creator: task_creator.TaskCreator,
) -> None:
    task = creator.create_validation_task(
        pb.ValidationSource.CALL_CENTER,
        hypothesis(),
        business_internal_pb2.BusinessInternal(
            names=company_names_info(),
            phones=phones_info(),
        ),
    )
    assert isinstance(task, pb.Task)
