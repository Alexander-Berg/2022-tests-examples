from maps.bizdir.sps.signal_processor.signal_comparator import (
    COMPARATOR_MAP,
    _are_hours_same,
    _are_time_range_same,
    _normalize_open_hours,
    _normalize_formatted_phone,
    _normalize_time_range,
    _are_open_hour_lists_same,
    _are_range_features_same,
    _are_number_features_same,
    _are_feature_values_same,
    _are_phones_same,
    _are_links_same,
    _are_lists_same,
    _is_submap,
    _are_names_same,
    _are_addresses_same,
    _are_email_lists_same,
    _are_rubric_lists_same,
    _are_link_lists_same,
    _are_phone_lists_same,
    _are_features_same,
    _are_georeferences_same,
    are_attrs_same,
)
from maps.bizdir.sps.proto.business_internal_pb2 import FeatureInfo
from maps.bizdir.sps.proto.business_internal_pb2 import CompanyStateInfo
from yandex.maps.proto.bizdir.common.hours_pb2 import (
    EVERYDAY,
    FRIDAY,
    MONDAY,
    SATURDAY,
    SUNDAY,
    THURSDAY,
    TUESDAY,
    WEDNESDAY,
    Hours,
    OpenHours,
    TimeRange,
)
from yandex.maps.proto.bizdir.common.business_pb2 import (
    CLOSED,
    OPEN,
    Address,
    Addresses,
    CompanyName,
    CompanyNames,
    Email,
    Emails,
    Feature,
    Links,
    Phone,
    Phones,
    RangeFeature,
    NumberFeature,
    FeatureValue,
    BoolFeature,
    EnumFeature,
    Link,
    Rubric,
    Rubrics,
)
from yandex.maps.proto.common2.attribution_pb2 import Link as Common2Link
from unittest.mock import Mock, patch
import pytest

from yandex.maps.proto.bizdir.sps.georeference_pb2 import Entrance, Georeference

from yandex.maps.proto.common2.geometry_pb2 import Point


PATH = "maps.bizdir.sps.signal_processor.signal_comparator"


@pytest.mark.parametrize(
    "phone_expected, phone",
    [
        ("0123456789", "+95123456789"),
        ("81234567890", "+71234567890"),
        ("0123456789", "0123456789"),
        ("84951234567", "+7(495) 123-4567"),
    ],
)
def test_normalize_formatted_phone__given_some_phone__returns_normalized_phone(
    phone_expected: str, phone: str
) -> None:
    assert phone_expected == _normalize_formatted_phone(phone)


def test_normalize_time_range__given_all_day__clears_from_to() -> None:
    tr = TimeRange(to=20, all_day=True)
    setattr(tr, "from", 10)
    tr_expected = TimeRange(all_day=True)
    assert tr_expected == _normalize_time_range(tr)


def test_normalize_time_range__given_same_from_to__returns_all_day() -> None:
    tr = TimeRange(to=0)
    setattr(tr, "from", 0)
    tr_expected = TimeRange(all_day=True)
    assert tr_expected == _normalize_time_range(tr)


def test_normalize_time_range__given_from_0h_to_24h__returns_all_day() -> None:
    tr = TimeRange(to=86400)
    setattr(tr, "from", 0)
    tr_expected = TimeRange(all_day=True)
    assert tr_expected == _normalize_time_range(tr)


def test_normalize_time_range__given_from_to__clears_all_day() -> None:
    tr = TimeRange(to=19, all_day=False)
    setattr(tr, "from", 10)
    tr_expected = tr
    tr_expected.ClearField("all_day")
    assert tr_expected == _normalize_time_range(tr)


def test_normalize_time_range__given_no_from_to__just_copies() -> None:
    tr = TimeRange(all_day=False)
    assert tr == _normalize_time_range(tr)


def test_normalize_open_hours__given_everyday_worktime__expands_it_to_concrete_days() -> None:
    hrs = OpenHours(
        hours=[Hours(day=[EVERYDAY], time_range=[TimeRange(all_day=True)])]
    )
    expected_hrs = OpenHours(
        hours=[
            Hours(day=[SUNDAY], time_range=[TimeRange(all_day=True)]),
            Hours(day=[MONDAY], time_range=[TimeRange(all_day=True)]),
            Hours(day=[TUESDAY], time_range=[TimeRange(all_day=True)]),
            Hours(day=[WEDNESDAY], time_range=[TimeRange(all_day=True)]),
            Hours(day=[THURSDAY], time_range=[TimeRange(all_day=True)]),
            Hours(day=[FRIDAY], time_range=[TimeRange(all_day=True)]),
            Hours(day=[SATURDAY], time_range=[TimeRange(all_day=True)]),
        ]
    )
    assert expected_hrs == _normalize_open_hours(hrs)


def test_normalize_open_hours__given_some_days_worktime__expands_to_separate_days() -> None:
    hrs = OpenHours(
        hours=[
            Hours(
                day=[MONDAY, TUESDAY, WEDNESDAY], time_range=[TimeRange(to=20)]
            )
        ]
    )
    expected_hrs = OpenHours(
        hours=[
            Hours(day=[MONDAY], time_range=[TimeRange(to=20)]),
            Hours(day=[TUESDAY], time_range=[TimeRange(to=20)]),
            Hours(day=[WEDNESDAY], time_range=[TimeRange(to=20)]),
        ]
    )
    assert expected_hrs == _normalize_open_hours(hrs)


def test_are_range_features_same__given_same_range_features__returns_true() -> None:
    rf1 = RangeFeature(to=20, unit="$")
    setattr(rf1, "from", 10)
    assert _are_range_features_same(rf1, rf1)
    assert _are_range_features_same(RangeFeature(to=20, unit="$"), rf1)
    assert _are_range_features_same(
        RangeFeature(unit="$"), RangeFeature(to=20, unit="$")
    )
    assert _are_range_features_same(
        RangeFeature(to=20), RangeFeature(to=20, unit="$")
    )


def test_are_range_features_same__given_different_range_features__returns_false() -> None:
    assert not _are_range_features_same(
        RangeFeature(to=10, unit="P"), RangeFeature(to=20, unit="$")
    )
    assert not _are_range_features_same(
        RangeFeature(to=10, unit="P"), RangeFeature(to=10)
    )


def test_are_number_features_same__given_same_number_features__returns_true() -> None:
    assert _are_number_features_same(
        NumberFeature(value=20, unit="$"), NumberFeature(value=20, unit="$")
    )
    assert _are_number_features_same(
        NumberFeature(value=20), NumberFeature(value=20, unit="$")
    )
    assert _are_number_features_same(
        NumberFeature(unit="$"), NumberFeature(value=20, unit="$")
    )


def test_are_number_features_same__given_different_number_features__returns_false() -> None:
    assert not _are_number_features_same(
        NumberFeature(value=20, unit="P"), NumberFeature(value=20)
    )
    assert not _are_number_features_same(
        NumberFeature(value=10, unit="$"), NumberFeature(value=20, unit="$")
    )
    assert not _are_number_features_same(
        NumberFeature(value=10, unit="P"), NumberFeature(value=10, unit="$")
    )


def test_are_feature_values_same__given_both_empty_features__returns_true() -> None:
    assert _are_feature_values_same(None, None)


def test_are_feature_values_same__given_same_features__returns_true() -> None:
    assert _are_feature_values_same(
        FeatureValue(number_feature=NumberFeature(value=20, unit="$")),
        FeatureValue(number_feature=NumberFeature(value=20, unit="$")),
    )
    assert _are_feature_values_same(
        FeatureValue(bool_feature=BoolFeature(value=True)),
        FeatureValue(bool_feature=BoolFeature(value=True)),
    )
    assert _are_feature_values_same(
        FeatureValue(enum_feature=EnumFeature(id=["1", "2", "3"])),
        FeatureValue(enum_feature=EnumFeature(id=["3", "2", "1"])),
    )
    assert _are_feature_values_same(
        FeatureValue(range_feature=RangeFeature(to=20, unit="$")),
        FeatureValue(range_feature=RangeFeature(to=20, unit="$")),
    )


def test_are_feature_values_same__given_different_features__returns_false() -> None:
    assert not _are_feature_values_same(
        FeatureValue(number_feature=NumberFeature(value=10, unit="$")),
        FeatureValue(number_feature=NumberFeature(value=20, unit="$")),
    )
    assert not _are_feature_values_same(
        FeatureValue(bool_feature=BoolFeature(value=False)),
        FeatureValue(bool_feature=BoolFeature(value=True)),
    )
    assert not _are_feature_values_same(
        FeatureValue(enum_feature=EnumFeature(id=["1", "2", "4"])),
        FeatureValue(enum_feature=EnumFeature(id=["3", "2", "1"])),
    )
    assert not _are_feature_values_same(
        FeatureValue(range_feature=RangeFeature(to=10, unit="$")),
        FeatureValue(range_feature=RangeFeature(to=20, unit="$")),
    )
    assert not _are_feature_values_same(
        FeatureValue(bool_feature=BoolFeature(value=True)),
        FeatureValue(enum_feature=EnumFeature(id=["3", "2", "1"])),
    )


def test_are_feature_values_same__given_one_empty_feature__returns_false() -> None:
    assert not _are_feature_values_same(
        None,
        FeatureValue(number_feature=NumberFeature(value=20, unit="$")),
    )
    assert not _are_feature_values_same(
        FeatureValue(number_feature=NumberFeature(value=20, unit="$")),
        None,
    )


def test_are_time_range_same__given_same_timeranges__returns_true() -> None:
    tr1 = TimeRange(to=20, all_day=False)
    setattr(tr1, "from", 10)
    assert _are_time_range_same(tr1, tr1)
    assert _are_time_range_same(TimeRange(to=20, all_day=False), tr1)
    assert _are_time_range_same(
        TimeRange(all_day=False), TimeRange(to=20, all_day=False)
    )
    assert _are_time_range_same(
        TimeRange(to=20), TimeRange(to=20, all_day=False)
    )


def test_are_time_range_same__given_different_timeranges__returns_false() -> None:
    assert not _are_time_range_same(
        TimeRange(to=10, all_day=False), TimeRange(to=20, all_day=False)
    )
    assert not _are_time_range_same(
        TimeRange(to=10, all_day=False), TimeRange(to=20)
    )
    assert not _are_time_range_same(
        TimeRange(to=10, all_day=False), TimeRange(all_day=False)
    )


def test_are_hours_same__given_same_hours__returns_true() -> None:
    assert _are_hours_same(
        Hours(
            day=[SUNDAY],
            time_range=[TimeRange(all_day=True)],
        ),
        Hours(
            day=[SUNDAY],
            time_range=[TimeRange(all_day=True)],
        ),
    )
    assert _are_hours_same(
        Hours(
            day=[SATURDAY, SUNDAY],
            time_range=[TimeRange(to=10), TimeRange(to=18)],
        ),
        Hours(
            day=[SUNDAY, SATURDAY],
            time_range=[TimeRange(to=18), TimeRange(to=10)],
        ),
    )


def test_are_hours_same__given_different_hours__returns_false() -> None:
    assert not _are_hours_same(
        Hours(day=[MONDAY], time_range=[TimeRange(to=10), TimeRange(to=18)]),
        Hours(
            day=[SUNDAY, SATURDAY],
            time_range=[TimeRange(to=18), TimeRange(to=10)],
        ),
    )
    assert not _are_hours_same(
        Hours(
            day=[SUNDAY, SATURDAY],
            time_range=[TimeRange(to=10), TimeRange(to=18)],
        ),
        Hours(day=[SUNDAY, SATURDAY], time_range=[TimeRange(to=10)]),
    )


def test_are_phones_same__given_same_phones__returns_true() -> None:
    phone = Phone(
        formatted="+7-495-123-4567",
        type=Phone.PHONE,
        country_code="+7",
        region_code="495",
        number="123-4567",
        ext="4562",
        lang_to_info={"EN": "info"},
    )

    assert _are_phones_same(phone, phone)


def test_are_phones_same__given_empty_str_fields__returns_true() -> None:
    phone1 = Phone(
        type=Phone.PHONE,
        formatted="+7 (47542) 6-11-30",
        country_code="",
        region_code="",
        number="",
        ext="",
    )
    phone2 = Phone(
        type=Phone.PHONE,
        formatted="+7 (47542) 6-11-30",
    )

    assert _are_phones_same(phone1, phone2)


def test_are_phones_same__given_phone_with_blank_fields__returns_true() -> None:
    assert _are_phones_same(
        Phone(
            formatted="+7-495-123-4567",
        ),
        Phone(
            formatted="+7-495-123-4567",
            type=Phone.PHONE,
            country_code="+7",
            region_code="495",
            number="123-4567",
            ext="4562",
            lang_to_info={"EN": "info"},
        ),
    )


def test_are_phones_same__given_different_phones__returns_false() -> None:
    assert not _are_phones_same(
        Phone(
            formatted="+7-495-123-4567",
        ),
        Phone(
            formatted="+7-495-123-4568",
        ),
    )
    assert not _are_phones_same(
        Phone(
            formatted="+7-495-123-4567",
            type=Phone.PHONE,
        ),
        Phone(
            formatted="+7-495-123-4567",
            type=Phone.FAX,
        ),
    )
    assert not _are_phones_same(
        Phone(
            formatted="123-4567",
            region_code="495",
        ),
        Phone(
            formatted="123-4567",
            region_code="499",
        ),
    )
    assert not _are_phones_same(
        Phone(
            formatted="495-123-4567",
            country_code="+7",
        ),
        Phone(
            formatted="495-123-4567",
            country_code="+95",
        ),
    )
    assert not _are_phones_same(
        Phone(
            formatted="+7-495-123-4567",
            number="123-4567",
        ),
        Phone(
            formatted="+7-495-123-4567",
            number="123-4568",
        ),
    )
    assert not _are_phones_same(
        Phone(
            formatted="+7-495-123-4567",
            ext="1111",
        ),
        Phone(
            formatted="+7-495-123-4567",
            ext="2222",
        ),
    )
    assert not _are_phones_same(
        Phone(
            formatted="+7-495-123-4567",
            lang_to_info={"RU": "info 1"},
        ),
        Phone(
            formatted="+7-495-123-4567",
            lang_to_info={"EN": "info 2"},
        ),
    )


def test_are_links_same__given_same_links__returns_true() -> None:
    assert _are_links_same(
        Link(
            link=Common2Link(href="www.google.com"),
            type=Link.SELF,
            lang_to_info={"EN": "info"},
        ),
        Link(
            link=Common2Link(href="www.google.com"),
            type=Link.SELF,
            lang_to_info={"EN": "info"},
        ),
    )
    assert _are_links_same(
        Link(
            link=Common2Link(href="www.google.com"),
            lang_to_info={"EN": "info"},
        ),
        Link(
            link=Common2Link(href="www.google.com"),
            type=Link.SELF,
            lang_to_info={"EN": "info"},
        ),
    )
    assert _are_links_same(
        Link(
            link=Common2Link(href="www.google.com"),
        ),
        Link(
            link=Common2Link(href="www.google.com"),
            lang_to_info={"EN": "info"},
        ),
    )


def test_are_links_same__given_different_links__returns_false() -> None:
    assert not _are_links_same(
        Link(
            link=Common2Link(href="www.google.com"),
        ),
        Link(
            link=Common2Link(href="www.yandex.ru"),
        ),
    )
    assert not _are_links_same(
        Link(
            link=Common2Link(href="www.google.com"),
            type=Link.SELF,
        ),
        Link(
            link=Common2Link(href="www.google.com"),
            type=Link.SOCIAL,
        ),
    )
    assert not _are_links_same(
        Link(
            link=Common2Link(href="www.google.com"),
            lang_to_info={"EN": "info 1"},
        ),
        Link(
            link=Common2Link(href="www.google.com"),
            lang_to_info={"EN": "info 2"},
        ),
    )


def test_are_lists_same__given_same_lists__returns_true() -> None:
    assert _are_lists_same([1, 2, 3], [3, 1, 2])


def test_are_lists_same__given_different_lists__returns_false() -> None:
    assert not _are_lists_same([1, 2, 3], [1, 2, 3, 4])
    assert not _are_lists_same([1, 2, 3], [3, 1, 0])


def test_is_submap__given_same_maps__returns_true() -> None:
    assert _is_submap({"a": 1, "b": 2}, {"a": 1, "b": 2, "c": 3})


def test_is_submap__given_deifferent_maps__returns_false() -> None:
    assert not _is_submap({"a": 1, "b": 2, "c": 3}, {"a": 1, "b": 2})
    assert not _is_submap({"a": 1, "b": 2}, {"a": 1, "b": 5, "c": 3})


def test_are_names_same__given_same_name_maps__returns_true() -> None:
    assert _are_names_same(
        CompanyNames(
            lang_to_name={
                "EN": CompanyName(short_name="name 1"),
                "RU": CompanyName(name="name 2"),
                "FR": CompanyName(name="name 3", short_name="name 4"),
            }
        ),
        CompanyNames(
            lang_to_name={
                "EN": CompanyName(short_name="name 1"),
                "RU": CompanyName(name="name 2"),
                "FR": CompanyName(name="name 3", short_name="name 4"),
            }
        ),
    )


def test_are_names_same__given_name_submap__returns_true() -> None:
    assert _are_names_same(
        CompanyNames(
            lang_to_name={
                "EN": CompanyName(short_name="name 2"),
                "RU": CompanyName(name="name 3"),
            }
        ),
        CompanyNames(
            lang_to_name={
                "EN": CompanyName(name="name 1", short_name="name 2"),
                "RU": CompanyName(name="name 3", short_name="name 4"),
                "FR": CompanyName(name="name 5", short_name="name 6"),
            }
        ),
    )


def test_are_names_same__given_different_names__returns_false() -> None:
    assert not _are_names_same(
        CompanyNames(lang_to_name={"EN": CompanyName(short_name="name 1")}),
        CompanyNames(lang_to_name={"EN": CompanyName(short_name="name 2")}),
    )
    assert not _are_names_same(
        CompanyNames(
            lang_to_name={"EN": CompanyName(name="name 1", short_name="name 2")}
        ),
        CompanyNames(
            lang_to_name={"EN": CompanyName(name="name 1", short_name="name 3")}
        ),
    )
    assert not _are_names_same(
        CompanyNames(lang_to_name={"EN": CompanyName(name="name 1")}),
        CompanyNames(lang_to_name={"EN": CompanyName(short_name="name 1")}),
    )
    assert not _are_names_same(
        CompanyNames(lang_to_name={"EN": CompanyName(short_name="name 1")}),
        CompanyNames(lang_to_name={"EN": CompanyName(name="name 1")}),
    )


def test_are_names_same__given_different_maps__returns_false() -> None:
    assert not _are_names_same(
        CompanyNames(
            lang_to_name={
                "EN": CompanyName(short_name="name 1"),
                "RU": CompanyName(name="name 2"),
            }
        ),
        CompanyNames(
            lang_to_name={
                "RU": CompanyName(name="name 2"),
            }
        ),
    )


def test_are_addresses_same__given_same_address_maps__returns_true() -> None:
    assert _are_addresses_same(
        Addresses(
            lang_to_address={
                "EN": Address(address="address 1"),
                "RU": Address(address="address 2", additional_info="info 2"),
            }
        ),
        Addresses(
            lang_to_address={
                "EN": Address(address="address 1"),
                "RU": Address(address="address 2", additional_info="info 2"),
            }
        ),
    )


def test_are_addresses_same__given_address_submap__returns_true() -> None:
    assert _are_addresses_same(
        Addresses(
            lang_to_address={
                "EN": Address(address="address 1"),
                "FR": Address(address="address 3"),
            }
        ),
        Addresses(
            lang_to_address={
                "EN": Address(address="address 1", additional_info="info 1"),
                "RU": Address(address="address 2", additional_info="info 2"),
                "FR": Address(address="address 3", additional_info="info 3"),
            }
        ),
    )


def test_are_addresses_same__given_different_addresses__returns_false() -> None:
    assert not _are_addresses_same(
        Addresses(lang_to_address={"EN": Address(address="address 1")}),
        Addresses(lang_to_address={"EN": Address(address="address 2")}),
    )
    assert not _are_addresses_same(
        Addresses(
            lang_to_address={
                "EN": Address(address="address 1", additional_info="info 1")
            }
        ),
        Addresses(lang_to_address={"EN": Address(address="address 1")}),
    )
    assert not _are_addresses_same(
        Addresses(
            lang_to_address={
                "EN": Address(address="address 1", additional_info="info 1")
            }
        ),
        Addresses(
            lang_to_address={
                "EN": Address(address="address 1", additional_info="info 2")
            }
        ),
    )


def test_are_addresses_same__given_different_maps__returns_false() -> None:
    assert not _are_addresses_same(
        Addresses(
            lang_to_address={
                "EN": Address(address="address 1"),
                "RU": Address(address="address 2"),
            }
        ),
        Addresses(
            lang_to_address={
                "RU": Address(address="address 2"),
            }
        ),
    )


def test_are_email_lists_same__given_same_emails_in_different_order__returns_true() -> None:
    assert _are_email_lists_same(
        Emails(email=[Email(value="1@ya.ru"), Email(value="2@ya.ru")]),
        Emails(email=[Email(value="2@ya.ru"), Email(value="1@ya.ru")]),
    )


def test_are_email_lists_same__given_equal_emails_with_partial_info__returns_true() -> None:
    assert _are_email_lists_same(
        Emails(email=[Email(value="1@ya.ru", lang_to_info={"RU": "info 1"})]),
        Emails(
            email=[
                Email(
                    value="1@ya.ru",
                    lang_to_info={"RU": "info 1", "EN": "info 2"},
                )
            ]
        ),
    )


def test_are_email_lists_same__given_different_emails__returns_false() -> None:
    assert not _are_email_lists_same(
        Emails(email=[Email(value="1@ya.ru")]),
        Emails(email=[Email(value="2@ya.ru")]),
    )


def test_are_email_lists_same__given_different_email_info__returns_false() -> None:
    assert not _are_email_lists_same(
        Emails(
            email=[
                Email(
                    value="1@ya.ru",
                    lang_to_info={"RU": "info 1", "EN": "info 2"},
                )
            ]
        ),
        Emails(email=[Email(value="1@ya.ru", lang_to_info={"RU": "info 1"})]),
    )


def test_are_rubric_lists_same__given_same_rubrics__returns_true() -> None:
    assert _are_rubric_lists_same(
        Rubrics(
            rubric=[
                Rubric(id="rubric 1", is_main=False),
                Rubric(id="rubric 2", is_main=True),
                Rubric(is_main=True),
            ]
        ),
        Rubrics(
            rubric=[
                Rubric(is_main=True),
                Rubric(id="rubric 2", is_main=True),
                Rubric(id="rubric 1", is_main=False),
            ]
        ),
    )
    assert _are_rubric_lists_same(
        Rubrics(rubric=[Rubric(id="rubric 1")]),
        Rubrics(rubric=[Rubric(id="rubric 1", is_main=True)]),
    )


def test_are_rubric_lists_same__given_different_rubrics__returns_false() -> None:
    assert not _are_rubric_lists_same(
        Rubrics(rubric=[Rubric(id="rubric 1")]),
        Rubrics(rubric=[Rubric(id="rubric 2")]),
    )
    assert not _are_rubric_lists_same(
        Rubrics(rubric=[Rubric(id="rubric 1", is_main=True)]),
        Rubrics(rubric=[Rubric(id="rubric 1", is_main=False)]),
    )
    assert not _are_rubric_lists_same(
        Rubrics(rubric=[Rubric(id="rubric 1", is_main=True)]),
        Rubrics(rubric=[Rubric(id="rubric 1")]),
    )


def test_are_link_lists_same__given_same_links_in_different_order__returns_true() -> None:
    assert _are_link_lists_same(
        Links(
            link=[
                Link(link=Common2Link(href="www.google.com")),
                Link(link=Common2Link(href="www.yandex.ru")),
            ]
        ),
        Links(
            link=[
                Link(link=Common2Link(href="www.yandex.ru")),
                Link(link=Common2Link(href="www.google.com")),
            ]
        ),
    )


def test_are_link_lists_same__given_different_links__returns_false() -> None:
    assert not _are_link_lists_same(
        Links(link=[Link(link=Common2Link(href="www.google.com"))]),
        Links(link=[Link(link=Common2Link(href="www.yandex.ru"))]),
    )


def test_are_phone_lists_same__given_same_phones_in_different_order__returns_true() -> None:
    assert _are_phone_lists_same(
        Phones(
            phone=[
                Phone(formatted="+74991234567"),
                Phone(formatted="+74997654321"),
            ]
        ),
        Phones(
            phone=[
                Phone(formatted="+74997654321"),
                Phone(formatted="+74991234567"),
            ]
        ),
    )


def test_are_phone_lists_same__given_different_phones__returns_false() -> None:
    assert not _are_phone_lists_same(
        Phones(phone=[Phone(formatted="+74997654321")]),
        Phones(phone=[Phone(formatted="+74991234567")]),
    )


def test_are_open_hour_lists_same__given_different_days_order__returns_true() -> None:
    hours1 = Hours(
        day=[MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY],
        time_range=[TimeRange(to=20)],
    )
    hours2 = Hours(day=[SATURDAY, SUNDAY], time_range=[TimeRange(to=18)])

    assert _are_open_hour_lists_same(
        OpenHours(hours=[hours1, hours2]),
        OpenHours(hours=[hours2, hours1]),
    )


def test_are_open_hour_lists_same__given_different_hours__returns_false() -> None:
    assert not _are_open_hour_lists_same(
        OpenHours(hours=[Hours(day=[SATURDAY])]),
        OpenHours(hours=[Hours(day=[SUNDAY])]),
    )


@patch(f"{PATH}._are_feature_values_same", return_value=True)
def test_are_features_same__given_same_features__returns_true(_: Mock) -> None:
    assert _are_features_same(
        Feature(id="feature 1", value=FeatureValue()),
        [
            FeatureInfo(),
            FeatureInfo(value=Feature(id="feature 1", value=FeatureValue())),
        ],
    )
    assert _are_features_same(
        Feature(id="feature 1"),
        [FeatureInfo(value=Feature(id="feature 1")), FeatureInfo()],
    )


@patch(f"{PATH}._are_feature_values_same", return_value=False)
def test_are_features_same__given_different_features__returns_false(
    _: Mock,
) -> None:
    assert not _are_features_same(
        Feature(id="feature 1"), [FeatureInfo(value=Feature(id="feature 2"))]
    )
    assert not _are_features_same(
        Feature(id="feature 1", value=FeatureValue()),
        [FeatureInfo(value=Feature(id="feature 1"))],
    )
    assert not _are_features_same(
        Feature(id="feature 1"),
        [FeatureInfo(value=Feature(id="feature 1", value=FeatureValue()))],
    )
    assert not _are_features_same(
        Feature(id="feature 1", value=FeatureValue()),
        [FeatureInfo(value=Feature(id="feature 1", value=FeatureValue()))],
    )


def test_are_georeferences_same__given_same_georeferences__returns_true() -> None:
    assert _are_georeferences_same(
        Georeference(point=Point(lon=1, lat=2)),
        Georeference(point=Point(lon=1, lat=2)),
    )
    assert _are_georeferences_same(
        Georeference(
            entrance=[
                Entrance(point=Point(lon=1, lat=2)),
                Entrance(point=Point(lon=2, lat=1)),
            ]
        ),
        Georeference(
            entrance=[
                Entrance(point=Point(lon=2, lat=1)),
                Entrance(point=Point(lon=1, lat=2)),
            ]
        ),
    )


def test_are_georeferences_same__given_different_georeferences__returns_false() -> None:
    assert not _are_georeferences_same(
        Georeference(point=Point(lon=1, lat=2)),
        Georeference(
            point=Point(lon=1, lat=2),
            entrance=[Entrance(point=Point(lon=2, lat=1))],
        ),
    )
    assert not _are_georeferences_same(
        Georeference(point=Point(lon=1, lat=2)),
        Georeference(),
    )


@pytest.mark.parametrize(
    "attr_name",
    [
        "names",
        "addresses",
        "emails",
        "links",
        "phones",
        "open_hours",
        "rubrics",
    ],
)
def test_are_attrs_same__given_attr__calls_corresponding_comparer(
    attr_name: str,
) -> None:
    comparer_mock = Mock()
    with patch.dict(COMPARATOR_MAP, {attr_name: comparer_mock}):
        are_attrs_same(attr_name, Mock(), Mock())

        comparer_mock.assert_called_once()


@patch(f"{PATH}._are_georeferences_same")
def test_are_attrs_same__given_georeference_attr__calls_are_georeferences_same(
    _are_georeferences_same: Mock,
) -> None:
    are_attrs_same("georeference", Mock(), Mock())

    _are_georeferences_same.assert_called_once()


@patch(f"{PATH}._are_features_same")
def test_are_attrs_same__given_features_attr__calls_are_features_same(
    _are_features_same: Mock,
) -> None:
    are_attrs_same("feature", Mock(), Mock())

    _are_features_same.assert_called_once()


def test_are_attrs_same__for_empty_business_attr__returns_false() -> None:
    assert not are_attrs_same("company_state", OPEN, CompanyStateInfo())


def test_are_attrs_same__given_same_company_state_attr__returns_true() -> None:
    assert are_attrs_same("company_state", OPEN, CompanyStateInfo(value=OPEN))


def test_are_attrs_same__given_different_company_state_attr__returns_false() -> None:
    assert not are_attrs_same(
        "company_state", OPEN, CompanyStateInfo(value=CLOSED)
    )
