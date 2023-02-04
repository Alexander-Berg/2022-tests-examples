import maps.bizdir.sps.tools.rubrics_loader.lib.loaders as l
import maps.bizdir.sps.db.tables as db

from typing import Any
from unittest import mock


LIB_PATH = "maps.bizdir.sps.tools.rubrics_loader.lib.loaders"


def sprav_feature(**kwargs: Any) -> dict[str, Any]:
    return {
        "publishing_status": "publish",
        "is_generated": False,
        "hide": False,
        "is_tag": False,
        "permalink": "average_bill2",
        "value_type": "number",
        "enum_values": [1, 2, 3],
        "id": 1,
    } | dict(**kwargs)


def mock_yt_client(table: list[dict[str, Any]]) -> mock.Mock:
    yt_client = mock.Mock()
    yt_client.read_table.return_value = table
    return yt_client


def test_should_add_feature__given_obsolete_feature__returns_false() -> None:
    feature = sprav_feature(publishing_status="obsolete")

    assert l._should_add_feature(feature) is False


def test_should_add_feature__given_tag_feature__returns_false() -> None:
    feature = sprav_feature(is_tag=True)

    assert l._should_add_feature(feature) is False


def test_should_add_feature__given_generated_feature__returns_false() -> None:
    feature = sprav_feature(is_generated=True)

    assert l._should_add_feature(feature) is False


def test_should_add_feature__given_hidden_feature__returns_false() -> None:
    feature = sprav_feature(hide=True)

    assert l._should_add_feature(feature) is False


def test_should_add_feature__given_food_filter__returns_false() -> None:
    feature = sprav_feature(permalink="auto_generated_food_filter")

    assert l._should_add_feature(feature) is False


def test_should_add_feature__given_feature_with_invalid_type__returns_false() -> None:
    feature = sprav_feature(value_type="text_value")

    assert l._should_add_feature(feature) is False


def test_should_add_feature__given_valid_feature__returns_true() -> None:
    feature = sprav_feature()

    assert l._should_add_feature(feature) is True


@mock.patch(f"{LIB_PATH}._should_add_feature", return_value=False)
def test_get_features_info__given_invalid_feature__skips_it(
    _: mock.Mock
) -> None:
    yt_client = mock_yt_client([{}])

    assert not l._get_features_info(yt_client)


@mock.patch(f"{LIB_PATH}._should_add_feature", return_value=True)
def test_get_features_info__given_valid_feature__returns_it(
    _: mock.Mock
) -> None:
    yt_client = mock_yt_client([sprav_feature()])

    assert l._get_features_info(yt_client) == {
        1: l.Feature(
            permalink="average_bill2",
            feature_type=db.FeatureType.number_value,
            enum_ids=[1, 2, 3],
        )
    }


def build_name(name: str, lang: str, type: str = "main") -> dict[str, Any]:
    return {
        "value": {
            "locale": lang,
            "value": name,
        },
        "type": type,
    }


def test_convert_names__given_several_main_names__returns_them() -> None:
    names = [build_name("воздушные шары", "ru"), build_name("balloons", "en")]

    assert l._convert_names(names) == {
        "EN": "balloons",
        "RU": "воздушные шары",
    }


def test_convert_names__given_synonyms_names__skips_them() -> None:
    names = [
        build_name("воздушные шары", "ru", "synonym"),
        build_name("balloons", "en", "synonym"),
    ]

    assert l._convert_names(names) == {}


def test_get_feature_enum_values__given_obsolete_value__skips_it() -> None:
    yt_client = mock_yt_client([{"publishing_status": "obsolete"}])

    assert l._get_feature_enum_values(yt_client) == {}


def test_get_feature_enum_values__given_value_without_names__skips_it() -> None:
    yt_client = mock_yt_client(
        [
            {
                "publishing_status": "publish",
                "names": [],
            }
        ]
    )

    assert l._get_feature_enum_values(yt_client) == {}


def test_get_feature_enum_values__given_valid_value__returns_it() -> None:
    yt_client = mock_yt_client(
        [
            {
                "publishing_status": "publish",
                "names": [build_name("balloons", "en")],
                "id": 1,
                "permalink": "polygraphy_balloons",
            }
        ]
    )

    assert l._get_feature_enum_values(yt_client) == {
        1: l.FeatureEnumValue(
            permalink="polygraphy_balloons", names={"EN": "balloons"}
        )
    }


def test_get_unit_names__given_obsolete_value__skips_it() -> None:
    yt_client = mock_yt_client([{"publishing_status": "obsolete"}])

    assert l._get_unit_names(yt_client) == {}


def test_get_unit_names__given_valid_value__returns_it() -> None:
    yt_client = mock_yt_client(
        [
            {
                "publishing_status": "publish",
                "names": [build_name("rub", "en")],
                "id": 1,
            }
        ]
    )

    assert l._get_unit_names(yt_client) == {1: {"EN": "rub"}}


def test_get_rubrics_info__given_obsolete_rubric__skips_it() -> None:
    yt_client = mock_yt_client(
        [
            {
                "publishing_status": "obsolete",
            }
        ]
    )

    assert l._get_rubrics_info(yt_client) == []


def test_get_rubrics_info__given_not_ordinal_rubric__skips_it() -> None:
    yt_client = mock_yt_client(
        [{"publishing_status": "publish", "type": "group"}]
    )

    assert l._get_rubrics_info(yt_client) == []


def test_get_rubrics_info__given_valid_rubric__returns_it() -> None:
    yt_client = mock_yt_client(
        [
            {
                "permalink": "184106390",
                "publishing_status": "publish",
                "type": "ordinal",
                "features": [
                    {"feature_id": 123, "export_to_snippet": True},
                    {"feature_id": 234, "export_to_snippet": False},
                ],
            }
        ]
    )

    assert l._get_rubrics_info(yt_client) == [
        l.Rubric(
            permalink="184106390",
            features=[
                l.RubricFeatureInfo(internal_id=123, is_top_feature=True),
                l.RubricFeatureInfo(internal_id=234, is_top_feature=False),
            ],
        )
    ]


def test_upload_rubric_info__given_rubric_without_features__adds_none() -> None:
    session = mock.Mock()
    rubrics_info = [
        l.Rubric(
            permalink="184106390",
            features=[],
        )
    ]
    features_info: dict[int, l.Feature] = {}

    l._upload_rubric_info(session, rubrics_info, features_info)

    session.add_all.assert_called_once_with([])


def test_upload_rubric_info__given_rubric_with_invalid_feature__adds_none() -> None:
    session = mock.Mock()
    rubrics_info = [
        l.Rubric(
            permalink="184106390",
            features=[
                l.RubricFeatureInfo(internal_id=111, is_top_feature=True)
            ],
        )
    ]
    features_info: dict[int, l.Feature] = {}

    l._upload_rubric_info(session, rubrics_info, features_info)

    session.add_all.assert_called_once_with([])


def test_upload_rubric_info__given_rubric_with_valid_feature__adds_it() -> None:
    session = mock.Mock()
    rubrics_info = [
        l.Rubric(
            permalink="184106390",
            features=[
                l.RubricFeatureInfo(internal_id=111, is_top_feature=True)
            ],
        )
    ]
    features_info = {
        111: l.Feature(
            permalink="average_bill2",
            feature_type=db.FeatureType.range_value,
            enum_ids=[],
        )
    }

    l._upload_rubric_info(session, rubrics_info, features_info)

    rubric2feature = session.add_all.call_args.args[0]
    assert len(rubric2feature) == 1
    assert rubric2feature[0].rubric_id == "184106390"
    assert rubric2feature[0].feature_id == "average_bill2"
    assert rubric2feature[0].is_top_feature is True


def test_upload_feature_info__always__adds_it() -> None:
    session = mock.Mock()
    features_info = {
        11: l.Feature(
            permalink="tour_type",
            feature_type=db.FeatureType.enum_value,
            enum_ids=[1],
        )
    }

    l._upload_feature_info(session, features_info)

    info = session.add_all.call_args.args[0]
    assert len(info) == 1
    assert info[0].id == "tour_type"
    assert info[0].feature_type == db.FeatureType.enum_value


def test_upload_enum_info__given_enums_without_features__skips_them() -> None:
    session = mock.Mock()
    features_info = {
        1: l.Feature(
            feature_type=db.FeatureType.enum_value,
            permalink="products",
            enum_ids=[2, 3],
        )
    }
    enum_info = {
        1: l.FeatureEnumValue(
            permalink="polygraphy_balloons", names={"en": "balloons"}
        )
    }

    l._upload_enum_info(session, features_info, enum_info)

    session.add_all.assert_called_once_with([])


def test_upload_enum_info__given_enums_with_features__adds_them() -> None:
    session = mock.Mock()
    features_info = {
        1: l.Feature(
            feature_type=db.FeatureType.enum_value,
            permalink="products",
            enum_ids=[1],
        )
    }
    enum_info = {
        1: l.FeatureEnumValue(
            permalink="polygraphy_balloons", names={"EN": "balloons"}
        )
    }

    l._upload_enum_info(session, features_info, enum_info)

    info = session.add_all.call_args.args[0]
    assert len(info) == 1
    assert info[0].id == "polygraphy_balloons"
    assert info[0].lang == "EN"
    assert info[0].name == "balloons"


def test_upload_unit_info__always__adds_to_db() -> None:
    session = mock.Mock()
    unit_info = {10444: {"EN": "m"}}

    l._upload_unit_info(session, unit_info)

    info = session.add_all.call_args.args[0]
    assert len(info) == 1
    assert info[0].id == 10444
    assert info[0].lang == "EN"
    assert info[0].name == "m"
