import pytest

import maps.bizdir.sps.db.tables as db
import maps.bizdir.sps.sprav_loader.converters as converters
import maps.bizdir.sps.sprav_loader.pb as pb
from maps.bizdir.sps.sprav_loader.common import FeatureInfo, FeatureInfoGetter

import sprav.protos.export_pb2 as sprav_export
import sprav.protos.language_pb2 as sprav_language

import yandex.maps.proto.common2.attribution_pb2 as common_attribution
import yandex.maps.proto.common2.geometry_pb2 as common_geometry

from typing import Optional, NoReturn


@pytest.fixture
def company() -> pb.SpravCompany:
    return pb.SpravCompany()


def make_localized(
    text: str,
    lang: "sprav_language.Language.V" = sprav_language.RU,
) -> sprav_export.TLocalizedString:
    return sprav_export.TLocalizedString(Lang=lang, Value=text)


def test_convert_names__given_no_names__returns_none(
    company: pb.SpravCompany,
) -> None:
    del company.Name[:]
    del company.ShortName[:]

    assert converters._convert_names(company) is None


def test_convert_names__given_names__returns_them(
    company: pb.SpravCompany,
) -> None:
    company.Name.extend(
        [
            make_localized("Яндекс", sprav_language.RU),
            make_localized("Yandex", sprav_language.EN),
        ]
    )

    assert converters._convert_names(company) == pb.CompanyNames(
        lang_to_name={
            "RU": pb.CompanyName(name="Яндекс"),
            "EN": pb.CompanyName(name="Yandex"),
        }
    )


def test_convert_names__given_several_names_with_same_lang__returns_with_synonyms(
    company: pb.SpravCompany,
) -> None:
    company.Name.extend(
        [
            make_localized("Яндекс", sprav_language.RU),
            make_localized("яндекс", sprav_language.RU),
            make_localized("яндекс, офис", sprav_language.RU),
        ]
    )

    assert converters._convert_names(company) == pb.CompanyNames(
        lang_to_name={
            "RU": pb.CompanyName(
                name="Яндекс",
                synonym=["яндекс", "яндекс, офис"],
            )
        }
    )


def test_convert_names__given_short_names__returns_them(
    company: pb.SpravCompany,
) -> None:
    company.ShortName.extend(
        [
            make_localized("Яндекс", sprav_language.RU),
            make_localized("Yandex", sprav_language.EN),
        ]
    )

    assert converters._convert_names(company) == pb.CompanyNames(
        lang_to_name={
            "RU": pb.CompanyName(short_name="Яндекс"),
            "EN": pb.CompanyName(short_name="Yandex"),
        }
    )


def test_convert_emails__given_empty_emails__returns_none(
    company: pb.SpravCompany,
) -> None:
    del company.Email[:]

    assert converters._convert_emails(company) is None


def test_convert_emails__given_emails__returns_them(
    company: pb.SpravCompany,
) -> None:
    emails = ["email1", "email2"]
    company.Email.extend(emails)

    assert converters._convert_emails(company) == pb.Emails(
        email=[pb.Email(value=it) for it in emails]
    )


def test_convert_address__given_no_geo__returns_none(
    company: pb.SpravCompany,
) -> None:
    company.ClearField("Geo")

    assert converters._convert_address(company) is None


def test_convert_address__given_no_geo_address_and_location__returns_none(
    company: pb.SpravCompany,
) -> None:
    company.Geo.ClearField("Address")
    company.Geo.ClearField("Location")

    assert converters._convert_address(company) is None


def test_convert_address__given_empty_formatted_address_and_extra_info__returns_none(
    company: pb.SpravCompany,
) -> None:
    del company.Geo.Address.Formatted[:]
    del company.Geo.Location.ExtraAddressInfo[:]

    assert converters._convert_address(company) is None


def test_convert_address__given_formatted_address__returns_address(
    company: pb.SpravCompany,
) -> None:
    formatted = "Россия, Москва, улица Льва Толстого 16"
    company.Geo.Address.Formatted.extend(
        [make_localized(formatted, sprav_language.RU)]
    )

    assert converters._convert_address(company) == pb.Addresses(
        lang_to_address={"RU": pb.Address(address=formatted)}
    )


def test_convert_address__given_extra_address_info__returns_additional_info(
    company: pb.SpravCompany,
) -> None:
    info = "4 этаж"
    company.Geo.Location.ExtraAddressInfo.extend(
        [make_localized(info, sprav_language.RU)]
    )

    assert converters._convert_address(company) == pb.Addresses(
        lang_to_address={"RU": pb.Address(additional_info=info)}
    )


def test_convert_rubrics__given_empty_rubrics__returns_none(
    company: pb.SpravCompany,
) -> None:
    del company.Rubric[:]

    assert converters._convert_rubrics(company) is None


def test_convert_rubrics__given_rubrics__returns_them(
    company: pb.SpravCompany,
) -> None:
    company.Rubric.extend(
        [
            sprav_export.TRubric(Id=1, IsMain=True),
            sprav_export.TRubric(Id=2, IsMain=False),
            sprav_export.TRubric(Id=3),
        ]
    )

    assert converters._convert_rubrics(company) == pb.Rubrics(
        rubric=[
            pb.Rubric(id="1", is_main=True),
            pb.Rubric(id="2", is_main=False),
            pb.Rubric(id="3"),
        ]
    )


def test_convert_phones__given_empty_phones__returns_none(
    company: pb.SpravCompany,
) -> None:
    del company.Phone[:]

    assert converters._convert_phones(company) is None


def test_convert_phones__given_hidden_phones__skips_them(
    company: pb.SpravCompany,
) -> None:
    company.Phone.extend(
        [
            sprav_export.TPhone(
                Formatted="123456", Hide=True, Type=sprav_export.TPhone.Phone
            )
        ]
    )

    assert converters._convert_phones(company) is None


def test_convert_phones__given_all_fields__returns_them(
    company: pb.SpravCompany,
) -> None:
    company.Phone.extend(
        [
            sprav_export.TPhone(
                Type=sprav_export.TPhone.Phone,
                Hide=False,
                Formatted="8(495)123-45-67 ext 3",
                CountryCode="8",
                LocalCode="495",
                Number="1234567",
                Ext="3",
                Info=[make_localized("sale", sprav_language.RU)],
            ),
        ]
    )

    assert converters._convert_phones(company) == pb.Phones(
        phone=[
            pb.Phone(
                type=pb.Phone.PHONE,
                formatted="8(495)123-45-67 ext 3",
                country_code="8",
                region_code="495",
                number="1234567",
                ext="3",
                lang_to_info={"RU": "sale"},
            )
        ]
    )


def test_convert_phones__without_optional_fields__doesnt_set_them(
    company: pb.SpravCompany,
) -> None:
    company.Phone.extend(
        [sprav_export.TPhone(Formatted="8(495)123-45-67 ext 3")]
    )

    assert converters._convert_phones(company) == pb.Phones(
        phone=[
            pb.Phone(
                type=pb.Phone.PHONE,
                formatted="8(495)123-45-67 ext 3",
            )
        ]
    )


def test_convert_time__given_hours__parses_it() -> None:
    assert converters._convert_time("03:45") == 3 * 60 * 60 + 45 * 60


def test_convert_time__given_24_hour__returns_zero() -> None:
    assert converters._convert_time("24:00") == 0


def test_convert_hours__given_empty_hours__returns_none(
    company: pb.SpravCompany,
) -> None:
    del company.WorkingTime[:]

    assert converters._convert_hours(company) is None


def test_convert_hours__given_full_day__sets_all_day(
    company: pb.SpravCompany,
) -> None:
    company.WorkingTime.add(
        From="00:00", To="24:00", Day=sprav_export.TWorkingTime.Monday
    )

    assert converters._convert_hours(company) == pb.OpenHours(
        hours=[
            pb.Hours(
                day=[pb.DayOfWeek.MONDAY],
                time_range=[pb.TimeRange(all_day=True)],
            )
        ]
    )


def test_convert_hours__given_day_with_break__returns_several_time_ranges(
    company: pb.SpravCompany,
) -> None:
    company.WorkingTime.add(
        From="08:00", To="13:00", Day=sprav_export.TWorkingTime.Monday
    )
    company.WorkingTime.add(
        From="14:00", To="19:00", Day=sprav_export.TWorkingTime.Monday
    )

    first_range = pb.TimeRange(to=13 * 60 * 60)
    setattr(first_range, "from", 8 * 60 * 60)

    second_range = pb.TimeRange(to=19 * 60 * 60)
    setattr(second_range, "from", 14 * 60 * 60)

    assert converters._convert_hours(company) == pb.OpenHours(
        hours=[
            pb.Hours(
                day=[pb.DayOfWeek.MONDAY],
                time_range=[first_range, second_range],
            )
        ]
    )


def test_convert_hours__given_weekend__returns_both_days(
    company: pb.SpravCompany,
) -> None:
    company.WorkingTime.add(
        From="09:00", To="19:00", Day=sprav_export.TWorkingTime.Weekend
    )

    time_range = pb.TimeRange(to=19 * 60 * 60)
    setattr(time_range, "from", 9 * 60 * 60)

    assert converters._convert_hours(company) == pb.OpenHours(
        hours=[
            pb.Hours(day=[it], time_range=[time_range])
            for it in [pb.DayOfWeek.SATURDAY, pb.DayOfWeek.SUNDAY]
        ]
    )


def feature_info(
    type_: db.FeatureType, unit: Optional[str] = None
) -> FeatureInfoGetter:
    return lambda _: FeatureInfo(feature_type=type_, unit=unit)


def exception() -> NoReturn:
    raise Exception()


def test_convert_features__given_empty_features__returns_empty(
    company: pb.SpravCompany,
) -> None:
    del company.Feature[:]

    assert converters._convert_features(company, lambda _: exception()) == []


def test_convert_features__given_feature_without_values__skips_it(
    company: pb.SpravCompany,
) -> None:
    f = company.Feature.add(Id="unknown_feature_id")
    del f.ExportedValue[:]

    features = converters._convert_features(
        company, feature_info(db.FeatureType.bool_value)
    )

    assert features == []


def test_convert_features__given_unknown_feature__skips_it(
    company: pb.SpravCompany,
) -> None:
    company.Feature.add(Id="unknown_feature_id")

    assert converters._convert_features(company, lambda _: None) == []


def test_convert_features__given_true_bool_feature__returns_it(
    company: pb.SpravCompany,
) -> None:
    feature_id = "table_games"
    company.Feature.add(
        Id=feature_id,
        ExportedValue=[sprav_export.TExportedFeatureValue(TextValue="1")],
    )

    features = converters._convert_features(
        company, feature_info(db.FeatureType.bool_value)
    )

    assert len(features) == 1
    assert features[0].id == feature_id
    assert features[0].value.bool_feature.value is True


def test_convert_features__given_false_bool_feature__returns_it(
    company: pb.SpravCompany,
) -> None:
    feature_id = "table_games"
    company.Feature.add(
        Id=feature_id,
        ExportedValue=[sprav_export.TExportedFeatureValue(TextValue="0")],
    )

    features = converters._convert_features(
        company, feature_info(db.FeatureType.bool_value)
    )

    assert len(features) == 1
    assert features[0].id == feature_id
    assert features[0].value.bool_feature.value is False


def test_convert_features__given_invalid_bool_feature__skips_it(
    company: pb.SpravCompany,
) -> None:
    feature_id = "table_games"
    company.Feature.add(
        Id=feature_id,
        ExportedValue=[sprav_export.TExportedFeatureValue(TextValue="invalid")],
    )

    features = converters._convert_features(
        company, feature_info(db.FeatureType.bool_value)
    )

    assert features == []


def test_convert_features__given_enum_feature__returns_it(
    company: pb.SpravCompany,
) -> None:
    feature_id = "special_menu"
    enum_values = ["lenten_menu", "seasonal_menu"]
    company.Feature.add(
        Id=feature_id,
        ExportedValue=[
            sprav_export.TExportedFeatureValue(TextValue=it)
            for it in enum_values
        ],
    )

    features = converters._convert_features(
        company, feature_info(db.FeatureType.enum_value)
    )

    assert len(features) == 1
    assert features[0].id == feature_id
    assert features[0].value.enum_feature == pb.EnumFeature(id=enum_values)


def test_convert_features__given_numeric_feature__returns_it(
    company: pb.SpravCompany,
) -> None:
    feature_id = "business lunch price"
    company.Feature.add(
        Id=feature_id,
        ExportedValue=[
            sprav_export.TExportedFeatureValue(NumericValue=840),
        ],
    )

    features = converters._convert_features(
        company, feature_info(db.FeatureType.number_value, "₽")
    )

    assert len(features) == 1
    assert features[0].id == feature_id
    assert features[0].value.number_feature == pb.NumberFeature(
        unit="₽", value=840.0
    )


def test_convert_features__given_numeric_feature_without_required_fields__skips_it(
    company: pb.SpravCompany,
) -> None:
    feature_id = "business lunch price"
    company.Feature.add(
        Id=feature_id,
        ExportedValue=[
            sprav_export.TExportedFeatureValue(TextValue="840 ₽"),
        ],
    )

    features = converters._convert_features(
        company, feature_info(db.FeatureType.number_value)
    )

    assert features == []


def test_convert_features__given_range_feature__returns_it(
    company: pb.SpravCompany,
) -> None:
    feature_id = "average_bill2"
    company.Feature.add(
        Id=feature_id,
        ExportedValue=[
            sprav_export.TExportedFeatureValue(MinValue=3500, MaxValue=4500),
        ],
    )

    features = converters._convert_features(
        company, feature_info(db.FeatureType.range_value, "₽")
    )

    assert len(features) == 1
    assert features[0].id == feature_id
    assert features[0].value.range_feature.unit == "₽"
    assert features[0].value.range_feature.to == 4500.0
    assert getattr(features[0].value.range_feature, "from") == 3500.0


def test_convert_features__given_range_feature_without_required_fields__skips_it(
    company: pb.SpravCompany,
) -> None:
    feature_id = "average_bill2"
    company.Feature.add(
        Id=feature_id,
        ExportedValue=[
            sprav_export.TExportedFeatureValue(TextValue="3500-4500 ₽"),
        ],
    )

    features = converters._convert_features(
        company, feature_info(db.FeatureType.range_value)
    )

    assert features == []


def test_convert_links__given_empty_links__returns_none(
    company: pb.SpravCompany,
) -> None:
    del company.Link[:]

    assert converters._convert_links(company) is None


def test_convert_links__given_links_with_valid_type__returns_them(
    company: pb.SpravCompany,
) -> None:
    company.Link.extend(
        [
            sprav_export.TLink(
                Type=sprav_export.TLink.Social, Href="http://facebook.com/"
            )
        ]
    )

    assert converters._convert_links(company) == pb.Links(
        link=[
            pb.Link(
                link=common_attribution.Link(href="http://facebook.com/"),
                type=pb.Link.Type.SOCIAL,
            )
        ]
    )


def test_convert_links__given_links_with_invalid_type__skips_them(
    company: pb.SpravCompany,
) -> None:
    company.Link.extend(
        [
            sprav_export.TLink(
                Type=sprav_export.TLink.Attribution, Href="http://facebook.com/"
            )
        ]
    )

    assert converters._convert_links(company) is None


def test_convert_links__given_empty_urls__returns_none(
    company: pb.SpravCompany,
) -> None:
    del company.Url[:]

    assert converters._convert_links(company) is None


def test_convert_links__given_urls__returns_them(
    company: pb.SpravCompany,
) -> None:
    company.Url.extend(["http://facebook.com/"])

    assert converters._convert_links(company) == pb.Links(
        link=[
            pb.Link(
                link=common_attribution.Link(href="http://facebook.com/"),
                type=pb.Link.Type.SELF,
            )
        ]
    )


def test_convert_status__given_no_status__returns_none(
    company: pb.SpravCompany,
) -> None:
    company.ClearField("PublishingStatus")

    assert converters._convert_status(company) is None


def test_convert_status__given_valid_status__returns_it(
    company: pb.SpravCompany,
) -> None:
    company.PublishingStatus = pb.TDSPublishingStatus.Publish

    assert converters._convert_status(company) == pb.CompanyState.OPEN


def test_convert_status__given_unknown_status__returns_none(
    company: pb.SpravCompany,
) -> None:
    company.PublishingStatus = pb.TDSPublishingStatus.Unchecked

    assert converters._convert_status(company) is None


def test_convert_georeference__given_company_without_geo__returns_none(
    company: pb.SpravCompany,
) -> None:
    company.ClearField("Geo")

    assert converters._convert_georeference(company) is None


def test_convert_georeference__given_company_without_geo_location__returns_none(
    company: pb.SpravCompany,
) -> None:
    company.Geo.ClearField("Location")

    assert converters._convert_georeference(company) is None


def test_convert_georeference__given_company_with_pos__returns_georef(
    company: pb.SpravCompany,
) -> None:
    company.Geo.Location.Pos.Lon = 37.6
    company.Geo.Location.Pos.Lat = 55.7

    assert converters._convert_georeference(company) == pb.Georeference(
        point=common_geometry.Point(lon=37.6, lat=55.7)
    )


def test_convert_georeference__given_company_entrances__returns_georef(
    company: pb.SpravCompany,
) -> None:
    company.Geo.Location.Entrance.extend(
        [sprav_export.TEntrance(Pos=sprav_export.TPos(Lon=37.6, Lat=55.7))]
    )

    georef = converters._convert_georeference(company)

    assert georef
    assert len(georef.entrance) == 1
    assert georef.entrance[0] == pb.Entrance(
        point=common_geometry.Point(lon=37.6, lat=55.7)
    )


def test_convert_company__given_no_sprav_company__returns_none() -> None:
    assert converters.convert_company(None, lambda _: None) is None


def test_convert_company__given_sprav_company__returns_converted(
    company: pb.SpravCompany,
) -> None:
    company.Id = 1
    company.DuplicateId.append(2)

    result = converters.convert_company(company, lambda _: None)

    assert result
    assert result.head_permalink == "1"
    assert result.permalinks == ["1", "2"]
