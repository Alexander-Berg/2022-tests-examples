from maps.b2bgeo.pipedrive_gate.lib.logic.score import (
    DescriptionType, DESCRIPTION_NOTE_SCORE,
    UtmType,
    OKVED_SCORE, _get_okved,
    POSITION_NOTE_SCORE,
    _get_city,
    _get_region_code_by_phone,
)


def test_get_region_code_by_phone():
    assert _get_region_code_by_phone("+442083661177") == "GB"
    assert _get_region_code_by_phone("+74593330099") == "RU"
    assert _get_region_code_by_phone("+79687776655") == "RU"
    assert _get_region_code_by_phone("+375 (17) 309-25-02 ") == "BY"
    assert _get_region_code_by_phone("+ 7 (7172) 574714") == "KZ"
    assert _get_region_code_by_phone("+99871 244 4141") == "UZ"
    assert _get_region_code_by_phone("+9not a phoine") is None
    assert _get_region_code_by_phone("79687776655") is None
    assert _get_region_code_by_phone("89687776655") is None
    assert _get_region_code_by_phone("9687776655") is None


def test_city_to_score():
    assert _get_city(None) is None
    assert _get_city("") is None
    assert _get_city("г.Москва, ул. первая. и тд") == "moscow"
    assert _get_city("ул. никакая. г.Москва") == "moscow"
    assert _get_city("г. Санкт-Петербург, Невский проспект") == "saint_petersburg"
    assert _get_city("г. Санкт Петербург, Невский проспект") == "saint_petersburg"


def test_utm_term_to_score():
    assert UtmType.from_term(None) == UtmType.undefined
    assert UtmType.from_term("") == UtmType.undefined
    assert UtmType.from_term("best_yandex") == UtmType.yandex
    assert UtmType.from_term("из_яндекса") == UtmType.yandex
    assert UtmType.from_term("иное") == UtmType.another
    assert UtmType.from_term("another") == UtmType.another


def test_position_to_score():
    assert POSITION_NOTE_SCORE.get_value([("Описание", "Доставка"), ("Должность", "Логист")]) == "Логист"
    assert POSITION_NOTE_SCORE.get_value([("Описание", "Доставка"), ("Должность", "")]) == ""
    assert POSITION_NOTE_SCORE.get_value([("Описание", "Доставка"), ("Должность", None)]) is None
    assert POSITION_NOTE_SCORE.get_value([("Описание", "Доставка")]) is None
    assert POSITION_NOTE_SCORE.get_value([]) is None


def test_description_to_score():
    keyword = 'Описание проблемы'
    assert DescriptionType.from_desciption(DESCRIPTION_NOTE_SCORE.get_value([(keyword, None)])) == DescriptionType.undefined
    assert DescriptionType.from_desciption(DESCRIPTION_NOTE_SCORE.get_value([(keyword, "")])) == DescriptionType.undefined
    assert DescriptionType.from_desciption(DESCRIPTION_NOTE_SCORE.get_value([(keyword, "упаковываем, рассылаем")])) == DescriptionType.short
    assert DescriptionType.from_desciption(DESCRIPTION_NOTE_SCORE.get_value([(keyword, "упаковываем и рассылаем")])) == DescriptionType.short
    assert DescriptionType.from_desciption(DESCRIPTION_NOTE_SCORE.get_value([(keyword, "делам лучшую доставту в мире")])) == DescriptionType.long


def test_okved_to_score():
    assert OKVED_SCORE.build(_get_okved("38")).score_increment == 1
    assert OKVED_SCORE.build(_get_okved("30")).score_increment == 2
    assert OKVED_SCORE.build(_get_okved("96.01")).score_increment == 1
    assert OKVED_SCORE.build(_get_okved("")).score_increment == 0
    assert OKVED_SCORE.build(_get_okved(None)).score_increment == 0
