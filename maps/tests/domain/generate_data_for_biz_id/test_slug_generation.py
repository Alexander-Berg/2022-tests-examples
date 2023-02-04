import pytest

from maps_adv.geosmb.clients.geosearch import AddressComponent

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.mock_dm,
    pytest.mark.freeze_time("2020-12-23"),
]


@pytest.mark.parametrize(
    "name, expected",
    (
        ["insta-site_", "insta-site"],
        ["insta_site", "insta-site"],
        ["Кафе с едой", "kafe-s-edoj"],
        ["Тойота центр Невский", "tojota-tsentr-nevskij"],
        ["Альянс-Моторс", "aljans-motors"],
        ["Орга %@#99", "orga-99"],
        ["  Орга  ", "orga"],
        ["MØS", "moes"],
        ["Окна Schüco - партнёр", "okna-schuco-partner"],
        ["Good Work Studio", "good-work-studio"],
    ),
)
async def test_generates_expected_slug(name, expected, domain, dm, geosearch):
    geosearch.resolve_org.coro.return_value.name = name

    await domain.generate_data_for_biz_id(biz_id=15)

    dm.create_biz_state.assert_called_with(biz_id=15, permalink="54321", slug=expected)


@pytest.mark.parametrize(
    "name, expected",
    [
        # no more than 30 symbols after cropping
        ("Тойота центр Невский Централ Петербург", "tojota-tsentr-nevskij-tsentral"),
        # no more than 30 symbols after cropping
        ("Очень длинное название для кафе с едой", "ochen-dlinnoe-nazvanie-dlja"),
        # but if first word is too long, it stays as is
        (
            "Рентгеноэлектрокардиографический кабинет",
            "rentgenoelektrokardiograficheskij",
        ),
    ],
)
async def test_shortens_long_slug_by_words(
    name, expected, domain, dm, geosearch
):
    geosearch.resolve_org.coro.return_value.name = name

    await domain.generate_data_for_biz_id(biz_id=15)

    dm.create_biz_state.assert_called_with(biz_id=15, permalink="54321", slug=expected)


@pytest.mark.parametrize(
    "name, expected",
    [
        # not cropped
        ("Кафе с едой", "kafe-s-edoj-muhosranskaja"),
        # cropped
        (
            "Тойота центр Невский Централ Петербург",
            "tojota-tsentr-nevskij-tsentral-muhosranskaja",
        ),
        (
            "Очень длинное название для кафе с едой",
            "ochen-dlinnoe-nazvanie-dlja-muhosranskaja",
        ),
        (
            "Рентгеноэлектрокардиографический кабинет",
            "rentgenoelektrokardiograficheskij-muhosranskaja",
        ),
    ],
)
async def test_adds_street_if_slug_exists(name, expected, domain, dm, geosearch):
    dm.fetch_biz_state_by_slug.coro.side_effect = [{}, None]
    geosearch.resolve_org.coro.return_value.name = name
    geosearch.resolve_org.coro.return_value.address_components = {
        AddressComponent.STREET: "Мухосранская"
    }

    await domain.generate_data_for_biz_id(biz_id=15)

    dm.create_biz_state.assert_called_with(biz_id=15, permalink="54321", slug=expected)


@pytest.mark.parametrize(
    "name, expected",
    [
        # not cropped
        ("Кафе с едой", "kafe-s-edoj-1608681600"),
        # cropped
        (
            "Тойота центр Невский Централ Петербург",
            "tojota-tsentr-nevskij-tsentral-1608681600",
        ),
        (
            "Очень длинное название для кафе с едой",
            "ochen-dlinnoe-nazvanie-dlja-1608681600",
        ),
        (
            "Рентгеноэлектрокардиографический кабинет",
            "rentgenoelektrokardiograficheskij-1608681600",
        ),
    ],
)
async def test_adds_timestamp_if_has_no_street(
    name, expected, domain, dm, geosearch
):
    dm.fetch_biz_state_by_slug.coro.side_effect = [{}, None]
    geosearch.resolve_org.coro.return_value.name = name
    geosearch.resolve_org.coro.return_value.address_components = {}

    await domain.generate_data_for_biz_id(biz_id=15)

    dm.create_biz_state.assert_called_with(biz_id=15, permalink="54321", slug=expected)


@pytest.mark.parametrize(
    "name, expected",
    [
        # not cropped
        (
            "Тойота центр Новоурюпинск",
            "tojota-tsentr-novourjupinsk-1608681600",
        ),
        # cropped
        (
            "Рентгеноэлектрокардиографический кабинет",
            "rentgenoelektrokardiograficheskij-1608681600",
        ),
    ],
)
async def test_adds_timestamp_if_generated_slug_with_street_is_too_long(
    name, expected, domain, dm, geosearch
):
    dm.fetch_biz_state_by_slug.coro.side_effect = [{}, None]
    geosearch.resolve_org.coro.return_value.name = name
    geosearch.resolve_org.coro.return_value.address_components = {
        AddressComponent.STREET: "Дофига очень длинное название улицы"
    }

    await domain.generate_data_for_biz_id(biz_id=15)

    dm.create_biz_state.assert_called_with(biz_id=15, permalink="54321", slug=expected)


@pytest.mark.parametrize(
    "name, expected",
    [
        # not cropped
        ("Кафе с едой", "kafe-s-edoj-1608681600"),
        # cropped
        (
            "Очень длинное название для кафе с едой",
            "ochen-dlinnoe-nazvanie-dlja-1608681600",
        ),
    ],
)
async def test_adds_timestamp_if_slug_with_address_already_exists(
    name, expected, domain, dm, geosearch
):
    dm.fetch_biz_state.coro.side_effect = [None, {}]
    dm.fetch_biz_state_by_slug.coro.side_effect = [{}, {}]
    geosearch.resolve_org.coro.return_value.name = name
    geosearch.resolve_org.coro.return_value.address_components = {
        AddressComponent.STREET: "Мухосранская"
    }

    await domain.generate_data_for_biz_id(biz_id=15)

    dm.create_biz_state.assert_called_with(biz_id=15, permalink="54321", slug=expected)
