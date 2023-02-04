import copy
import random
import string
from unittest import mock
from xml.etree.ElementTree import XMLParser

import pytest
from lxml import etree
from lxml.etree import ElementBase

from avito_feed_transformer.builders.auto.auto import (
    H,
    AutoTreeBuilder,
    DuplicateIdException,
    OPTION_TAGS,
    OPTION_TAG,
)
from avito_feed_transformer.builders.auto.matcher import Mapper


@pytest.fixture
def ads() -> ElementBase:
    return etree.Element("Ads")


@pytest.fixture
def ad() -> ElementBase:
    element = etree.Element("Ad")
    element.text = ""
    return element


@pytest.fixture
def empty_feed(ads) -> str:
    return make_feed(ads)


@pytest.fixture
def mock_mapper():
    mock_mapper_: Mapper = mock.create_autospec(spec=Mapper)
    mock_mapper_.match = mock.MagicMock(return_value=None)
    return mock_mapper_


@pytest.fixture
def parser(mock_mapper) -> etree.XMLParser:
    return etree.XMLParser(target=AutoTreeBuilder(mock_mapper))


def safe_int_params():
    return [(999, 999), ("this_is_not_int", "this_is_not_int"), (None, "")]


@pytest.mark.parametrize("given,wanted", safe_int_params())
def test_safe_int(given, wanted):
    """
    expect to return int if it's possible otherwise return given value
    :return:
    """
    elem = etree.Element("someTag")
    if given:
        elem.text = str(given)

    got = H.safe_int(elem)
    assert wanted == got

    got = H.safe_int(given)
    assert wanted == got


def make_feed(root: ElementBase, *ads) -> str:
    tree_root = copy.deepcopy(root)
    tree_root.extend(map(copy.deepcopy, ads))
    return etree.tounicode(tree_root)


def test_feed_valid(parser: etree.XMLParser, empty_feed: str) -> None:
    """
    want <data><cars>...</cars><errors>...</errors></data>
    """
    parser.feed(empty_feed)
    data: ElementBase = parser.close()
    assert data
    assert 2 == len(data.getchildren())
    errors: ElementBase = data.find("errors")
    assert not errors.getchildren()
    cars: ElementBase = data.find("cars")
    assert not cars.getchildren()


def containing(ad: ElementBase, with_tag: str, having_value: str = None) -> ElementBase:
    etree.SubElement(ad, with_tag).text = having_value or ""
    return ad


def test_handle_id(ads: ElementBase, ad: ElementBase, parser: XMLParser) -> None:
    wanted_value = random_string()

    parser.feed(make_feed(ads, containing(ad, with_tag="Id", having_value=wanted_value)))
    tree = parser.close()
    unique_id_tag = tree.find("./cars/car/unique_id")
    assert unique_id_tag is not None
    assert wanted_value == unique_id_tag.text


def random_string():
    return "".join(random.choice(string.ascii_letters) for _ in range(16))


def test_handle_contact(ads: ElementBase, ad: ElementBase, parser: XMLParser) -> None:
    """
    expected on car tag:
    <contact_info>
        <contact>
            <name>Менеджер</name>
            <phone>84997020609</phone>
        </contact>
    </contact_info>
    """
    wanted_name, wanted_phone = random_string(), random_string()
    parser.feed(
        make_feed(
            ads,
            containing(
                ad=containing(ad, with_tag="ContactPhone", having_value=wanted_phone),
                with_tag="ManagerName",
                having_value=wanted_name,
            ),
        )
    )
    tree = parser.close()
    phone = tree.find("./cars/car/contact_info/contact/phone")
    assert phone is not None
    assert wanted_phone == phone.text
    name = tree.find("./cars/car/contact_info/contact/name")
    assert name is not None
    assert wanted_name == name.text


def test_handle_contact_with_one_tag_missing(ads: ElementBase, ad: ElementBase, parser: XMLParser) -> None:
    """
    expected on car tag:
    <contact_info>
        <contact>
            ...
            <phone>84997020609</phone>
                    or
            <name>Менеджер</name>
            ...
        </contact>
    </contact_info>
    """
    wanted_phone = random_string()
    parser.feed(make_feed(ads, containing(ad, with_tag="ContactPhone", having_value=wanted_phone)))
    tree = parser.close()
    phone = tree.find("./cars/car/contact_info/contact/phone")
    assert phone is not None
    assert wanted_phone == phone.text
    name = tree.find("./cars/car/contact_info/contact/name")
    assert name is None


def car_tags_params():
    return [
        ("Category", "category", "автомобили", "1"),
        ("Id", "unique_id", "abc123", "abc123"),
        ("Make", "mark_id", "the mark", "the mark"),
        ("Model", "folder_id", "the model", "the model"),
        ("BodyType", "body_type", "Купе", "Купе"),
        ("BodyType", "body_type", "Пикап", "Пикап"),
        ("BodyType", "body_type", "Седан", "Седан"),
        ("BodyType", "body_type", "Фургон", "Фургон"),
        ("BodyType", "body_type", "Минивэн", "Минивэн"),
        ("BodyType", "body_type", "Хетчбэк", "Хетчбэк"),
        ("BodyType", "body_type", "Кабриолет", "Кабриолет"),
        ("BodyType", "body_type", "Универсал", "Универсал"),
        ("BodyType", "body_type", "Внедорожник", "Внедорожник"),
        ("BodyType", "body_type", "Микроавтобус", "Микроавтобус"),
        ("WheelType", "wheel", "Левый", "левый"),
        ("WheelType", "wheel", "Правый", "правый"),
        ("Color", "color", "Красный", "Красный"),
        ("Color", "color", "Коричневый", "Коричневый"),
        ("Color", "color", "Оранжевый", "Оранжевый"),
        ("Color", "color", "Бежевый", "Бежевый"),
        ("Color", "color", "Жёлтый", "Желтый"),
        ("Color", "color", "Зелёный", "Зеленый"),
        ("Color", "color", "Голубой", "Голубой"),
        ("Color", "color", "Синий", "Синий"),
        ("Color", "color", "Фиолетовый", "Фиолетовый"),
        ("Color", "color", "Пурпурный", "Пурпурный"),
        ("Color", "color", "Розовый", "Розовый"),
        ("Color", "color", "Белый", "Белый"),
        ("Color", "color", "Серый", "Серый"),
        ("Color", "color", "Чёрный", "Черный"),
        ("Color", "color", "Золотой", "Золотой"),
        ("Color", "color", "Серебряный", "Серебряный"),
        ("Kilometrage", "run", "1000", "1000"),
        ("Kilometrage", "run", "a", "a"),
        ("Year", "year", "2020", "2020"),
        ("Price", "price", "50000", "50000"),
        ("VIN", "vin", "SOME_VIN", "SOME_VIN"),
        ("Description", "description", "simple description", "simple description"),
        ("Images", "images", None, None),
        ("VideoURL", "video", "http://smvid.com", "http://smvid.com"),
        ("CertificationNumber", "sts", "77OT12355", "77OT12355"),
        ("Doors", "doors_count", "4", "4"),
        ("Owners", "owners_number", "1", "Один владелец"),
        ("Owners", "owners_number", "2", "Два владельца"),
        ("Owners", "owners_number", "3", "Три и более"),
        ("Owners", "owners_number", "4", "Три и более"),
    ]


@pytest.mark.parametrize("avito_tag,wanted_tag,avito_val,wanted_val", car_tags_params())
def test_handling_car_tags(
    avito_tag, avito_val, wanted_tag, wanted_val, ads: ElementBase, ad: ElementBase, parser: XMLParser
):
    """
    test serverless implementation preserves core fields for extraction as it is in scala impl,
    checking fields that not mutated after parsing
    """
    feed: str = make_feed(ads, containing(ad, with_tag=avito_tag, having_value=avito_val))
    parser.feed(feed)
    tree: etree.ElementTree = parser.close()
    path = f"./cars/car/{wanted_tag}"
    tag = tree.find(path)
    assert tag is not None, f"expected to find <{wanted_tag}>{wanted_val}</{wanted_tag}> at path: {path}"
    assert wanted_val == tag.text


def test_unique_id_check_error(ads: ElementBase, ad: ElementBase, parser: XMLParser):
    non_unique_id = random_string()
    parser.feed(make_feed(ads, containing(ad, with_tag="Id", having_value=non_unique_id), copy.deepcopy(ad)))
    tree = parser.close()

    assert 1 == len(tree.findall("./cars/car"))
    error = tree.find("./errors/error")
    assert error is not None
    error_type = error.find("./type")
    assert error_type is not None
    assert DuplicateIdException.type == error_type.text
    error_msg = error.find("./msg")
    assert error_msg is not None
    assert error_msg.text


def test_unique_vin_check_error_contains_vin(ads: ElementBase, ad: ElementBase, parser: XMLParser) -> None:
    non_unique_vin = random_string()
    parser.feed(make_feed(ads, containing(ad, with_tag="VIN", having_value=non_unique_vin), copy.deepcopy(ad)))
    tree: ElementBase = parser.close()

    assert 1 == len(tree.findall("./cars/car"))
    details = tree.find("./errors/error/details")
    assert details is not None
    # parser converts vin to upppercase :)
    assert non_unique_vin.upper() == details.text


def test_all_extras_options_except_dealer_s_parsed_having_prefix(
    ads: ElementBase, ad: ElementBase, parser: XMLParser
) -> None:
    options_without_options_subtag = {"PowerSteering", "ClimateControl", "Interior", "PowerWindows", "Lights", "Wheels"}
    for option_tag, prefix in OPTION_TAGS.items():
        # for some reason copy/deepcopy not working here :(
        try:
            ads.remove(ad)
        except ValueError:
            ...

        option_value = random_string()

        if option_tag not in options_without_options_subtag:
            ad_ = containing(copy.deepcopy(ad), with_tag=option_tag)
            opt = ad_.find(option_tag)
            containing(opt, with_tag=OPTION_TAG, having_value=option_value)
        else:
            ad_ = containing(copy.deepcopy(ad), with_tag=option_tag, having_value=option_value)
        feed_str = make_feed(ads, ad_)
        parser.feed(feed_str)
        tree = parser.close()
        option = tree.find("./cars/car/extras/extra")
        assert option is not None, f"option {option_tag} was not handled"
        assert option.text.startswith(prefix) and option.text.endswith(
            option_value
        ), f"""
wanted {option_tag}, to be converted onto:
<extras>
    ...
    <extra>{prefix} {option_value}</extra>
    ...
</extras>
but got:
<extras>
    ...
    <extra>{option.text}</extra>
    ...
</extras>

input:
{feed_str}
output:
{etree.tounicode(tree)}
"""


def test_address_added(ads: ElementBase, ad: ElementBase, parser: XMLParser) -> None:
    address_tags = ["Region", "City", "District", "Street"]
    for _ in range(1, len(address_tags)):
        ad_ = copy.deepcopy(ad)
        expected_texts = []
        for addr_tag in address_tags[:_]:
            txt = random_string()
            containing(ad_, with_tag=addr_tag, having_value=txt)
            expected_texts.append(txt)
        parser.feed(make_feed(copy.deepcopy(ads), ad_))
        tree = parser.close()
        poi_id = tree.find("./cars/car/poi_id")
        assert poi_id is not None
        assert ", ".join(expected_texts) == poi_id.text
