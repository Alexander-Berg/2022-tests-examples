from maps.bizdir.sps.sprav_loader.yt_loader import (
    CHYT_ALIAS,
    YtLoader,
    _fix_proto,
    _get_chyt_alias,
    _get_exported_company,
    _get_feature_unit_ids,
    _get_head_permalink,
    _get_official_langs,
)

import maps.bizdir.sps.db.tables as db
import maps.bizdir.sps.sprav_loader.pb as pb
import maps.bizdir.sps.sprav_loader.common as common

import os
import pytest
import yt.wrapper as yt

from collections.abc import Generator
from unittest import mock
from typing import Optional, Any


PATH = "maps.bizdir.sps.sprav_loader.yt_loader"


@pytest.fixture(autouse=True)
def chyt_alias() -> None:
    os.environ[CHYT_ALIAS] = "alias"


def test_get_chyt_alias__when_env_variable_is_set__returns_it() -> None:
    assert _get_chyt_alias() == "alias"


def test_get_chyt_alias__when_variable_isnot_set__raises() -> None:
    _get_chyt_alias.cache_clear()
    del os.environ[CHYT_ALIAS]

    with pytest.raises(Exception):
        _get_chyt_alias()


@mock.patch(f"{PATH}.chyt")
def test_get_head_permalink__when_chyt_finds_head__returns_it(
    chyt: mock.Mock,
) -> None:
    chyt.execute.return_value = [{"company_id": 222}]

    assert _get_head_permalink(yt_client=None, permalink=111) == 222


@mock.patch(f"{PATH}.chyt")
def test_get_head_permalink__when_chyt_doesnt_find_head__returns_permalink(
    chyt: mock.Mock,
) -> None:
    chyt.execute.return_value = []

    assert _get_head_permalink(yt_client=None, permalink=111) == 111


@mock.patch(f"{PATH}.chyt")
def test_get_feature_unit_ids__for_features_with_unit_ids__returns_them(
    chyt: mock.Mock,
) -> None:
    chyt.execute.return_value = [
        {"feature_permalink": "man_haircut", "unit_id": 10501},
        {"feature_permalink": "women_haircut", "unit_id": 10501},
    ]

    assert _get_feature_unit_ids(yt_client=None, permalink=1) == {
        "man_haircut": 10501,
        "women_haircut": 10501,
    }


@mock.patch(f"{PATH}.chyt")
def test_get_feature_unit_ids__for_features_without_unit_ids__skips_them(
    chyt: mock.Mock,
) -> None:
    chyt.execute.return_value = [
        {"feature_permalink": "has_restaurant", "unit_id": None},
    ]

    assert _get_feature_unit_ids(yt_client=None, permalink=1) == {}


@mock.patch(f"{PATH}.chyt")
def test_get_exported_company__when_company_was_not_found__returns_none(
    chyt: mock.Mock,
) -> None:
    chyt.execute.return_value = []

    assert _get_exported_company(yt_client=None, permalink=1) is None


@mock.patch(f"{PATH}.chyt")
def test_get_exported_company__when_company_exists__returns_company(
    chyt: mock.Mock,
) -> None:
    chyt.execute.return_value = [b""]

    result = _get_exported_company(yt_client=None, permalink=1)

    assert result == pb.SpravCompany()


def test_fix_proto__given_broken_proto__fixes_it() -> None:
    samples = [
        (b"\\b", b"\b"),
        (b"\\f", b"\f"),
        (b"\\r", b"\r"),
        (b"\\n", b"\n"),
        (b"\\t", b"\t"),
        (b"\\0", b"\0"),
        (b"\\\\", b"\\"),
        (b"\\'", b"'"),
        (b"\\03", b"\0003"),
        (b"\\\\03", b"\\03"),
        (b"\\\\\\03", b"\\\0003"),
    ]

    for (sample, expected) in samples:
        assert _fix_proto(sample) == expected, f"{sample=}"


def geobase(region: Optional[dict[str, Any]]) -> mock.Mock:
    geobase = mock.Mock()
    geobase.get_region_by_id.return_value = region
    return geobase


def test_get_official_langs__given_valid_region_id__returns_langs() -> None:
    geodata = geobase({"official_languages": "ru,en"})

    assert _get_official_langs(geodata, 1) == {"RU", "EN"}


def test_get_official_langs__given_invalid_region_id__returns_empty() -> None:
    geodata = geobase(None)

    assert _get_official_langs(geodata, 1) == set()


def test_get_official_langs__given_region_without_official_langs__returns_empty() -> None:
    geodata = geobase({"names": []})

    assert _get_official_langs(geodata, 1) == set()


@pytest.fixture
@mock.patch(f"{PATH}.geobase")
def yt_loader(geobase: mock.Mock) -> YtLoader:
    return YtLoader([mock.MagicMock(), mock.MagicMock()], "")


class Fixture:
    def __init__(
        self,
        yt_loader: YtLoader,
        _get_feature_unit_ids: mock.Mock,
        _get_exported_company: mock.Mock,
        _get_head_permalink: mock.Mock,
    ) -> None:
        self.yt_loader = yt_loader
        self.company = pb.SpravCompany(Id=1)
        self.session = mock.Mock()
        self._get_feature_unit_ids = _get_feature_unit_ids
        self._get_exported_company = _get_exported_company
        self._get_head_permalink = _get_head_permalink


@pytest.fixture
def f(yt_loader: YtLoader) -> Generator:
    with (
        mock.patch(f"{PATH}._get_feature_unit_ids") as f1,
        mock.patch(f"{PATH}._get_exported_company") as f2,
        mock.patch(f"{PATH}._get_head_permalink") as f3,
    ):
        yield Fixture(yt_loader, f1, f2, f3)


def test_get_business_info__for_company_in_export__returns_business_info(
    f: Fixture,
) -> None:
    f._get_exported_company.return_value = f.company

    assert f.yt_loader.get_business_info(f.session, 1) is not None


def test_get_business_info__for_company_not_in_export__returns_none(
    f: Fixture,
) -> None:
    f._get_exported_company.return_value = None

    assert f.yt_loader.get_business_info(f.session, 1) is None


def test_get_business_info__when_fallback_is_successfull__returns_business_info(
    f: Fixture,
) -> None:
    f._get_exported_company.side_effect = [
        yt.errors.YtProxyUnavailable(mock.MagicMock()),
        f.company,
    ]

    assert f.yt_loader.get_business_info(f.session, 1) is not None


def test_get_business_info__for_unavailable_yt_proxy__raises(
    f: Fixture,
) -> None:
    f._get_exported_company.side_effect = yt.errors.YtProxyUnavailable(
        mock.MagicMock()
    )

    with pytest.raises(Exception):
        f.yt_loader.get_business_info(f.session, 1)


def test_get_feature_info__given_unknown_feature_id__returns_none(
    f: Fixture,
) -> None:
    f._get_feature_unit_ids.return_value = {}
    f.session.query().filter_by().one_or_none.return_value = None

    get_feature_info = f.yt_loader._get_feature_info(
        mock.Mock(), f.session, f.company
    )

    assert get_feature_info("unknown_id") is None


def test_get_feature_info__given_feature_without_unit__returns_only_type(
    f: Fixture,
) -> None:
    f._get_feature_unit_ids.return_value = {}
    f.session.query().filter_by().one_or_none.return_value = db.FeatureInfo(
        feature_type=db.FeatureType.bool_value
    )

    get_feature_info = f.yt_loader._get_feature_info(
        mock.Mock(), f.session, f.company
    )

    assert get_feature_info("wifi") == common.FeatureInfo(
        feature_type=db.FeatureType.bool_value
    )


def test_get_feature_info__given_unit_without_langs__returns_first_unit_name(
    f: Fixture,
) -> None:
    f._get_feature_unit_ids.return_value = {"average_bill2": 123}
    f.session.query().filter_by().one_or_none.return_value = db.FeatureInfo(
        feature_type=db.FeatureType.number_value
    )
    f.session.query().filter_by().all.return_value = [
        db.UnitName(name="руб", lang="RU"),
        db.UnitName(name="rub", lang="EN"),
    ]

    get_feature_info = f.yt_loader._get_feature_info(
        mock.Mock(), f.session, f.company
    )

    assert get_feature_info("average_bill2") == common.FeatureInfo(
        feature_type=db.FeatureType.number_value, unit="руб"
    )


@mock.patch(f"{PATH}._get_official_langs")
def test_get_feature_info__given_unit_with_langs__returns_name_by_lang(
    _get_official_langs: mock.Mock, f: Fixture
) -> None:
    f._get_feature_unit_ids.return_value = {"average_bill2": 123}
    _get_official_langs.return_value = {"EN"}
    f.company.Geo.Location.GeoId = 1
    f.session.query().filter_by().one_or_none.return_value = db.FeatureInfo(
        feature_type=db.FeatureType.number_value
    )
    f.session.query().filter_by().all.return_value = [
        db.UnitName(name="руб", lang="RU"),
        db.UnitName(name="rub", lang="EN"),
    ]

    get_feature_info = f.yt_loader._get_feature_info(
        mock.Mock(), f.session, f.company
    )

    assert get_feature_info("average_bill2") == common.FeatureInfo(
        feature_type=db.FeatureType.number_value, unit="rub"
    )


def test_get_feature_info__given_unit_without_names__returns_no_unit(
    f: Fixture,
) -> None:
    f._get_feature_unit_ids.return_value = {"average_bill2": 123}
    f.session.query().filter_by().one_or_none.return_value = db.FeatureInfo(
        feature_type=db.FeatureType.number_value
    )
    f.session.query().filter_by().all.return_value = []

    get_feature_info = f.yt_loader._get_feature_info(
        mock.Mock(), f.session, f.company
    )

    assert get_feature_info("average_bill2") == common.FeatureInfo(
        feature_type=db.FeatureType.number_value
    )
