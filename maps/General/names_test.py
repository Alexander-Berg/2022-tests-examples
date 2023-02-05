from maps.poi.pylibs.util.names import (
    cast_abbreviations_to_lowercase,
    is_name_renderable,
    make_poi_names,
    remove_extra_symbols
)

from maps.poi.pylibs.util.names import (
    NamesChecker,
    NamesStorage
)


def test_remove_extra_symbols():
    assert remove_extra_symbols('  ВТБ,   банкомат  ') == 'ВТБ'
    assert remove_extra_symbols('VTB, bankomat') == 'VTB'
    assert remove_extra_symbols('Какая-то   организация, ATM') == 'Какая-то организация'
    assert remove_extra_symbols('Китапханә, бищбармак') == 'Китапханә, бищбармак'


def test_is_name_renderable():
    assert is_name_renderable('Смотровая площадка')
    assert not is_name_renderable('Римско-Католический храм успения Божией матери')
    assert is_name_renderable('Детский сад # 47')
    assert not is_name_renderable('Место клизмы изменить нельзя')
    assert not is_name_renderable('Лывоавтлваотлоавтвоавлооалвав солвтстстсстстстстстс аффффффффкккк')
    assert is_name_renderable("Кам'яноярузька сільська рада")
    assert not is_name_renderable('Stikutiņas Hoodiņš. 🈲🈳㊙')
    assert not is_name_renderable('מוזיאון מועצת העם ומינהלתה')
    assert is_name_renderable('דיזנגוף סנטר')


def test_cast_abbreviations_to_lowercase():
    assert cast_abbreviations_to_lowercase('ЦНИИС ЧЛХ') == 'ЦНИИС ЧЛХ'
    assert cast_abbreviations_to_lowercase('КОМПАНИЯ ZiGaZaGa') == 'Компания ZiGaZaGa'
    assert cast_abbreviations_to_lowercase('КамазЦНИИЛЗТ') == 'КамазЦНИИЛЗТ'
    assert cast_abbreviations_to_lowercase('КОРАБЛИ ЛАВИРОВАЛи') == 'Корабли ЛАВИРОВАЛи'


def test_make_poi_names():
    names = {
        'ru': 'СБЕРБАНК г. Нижневартовск, банкомат',
        'en': '𝗠𝗶𝗠 𝗥𝗘𝗞𝗟𝗔𝗠 Dijital',
        'es': 'Iglesia de Nuestra Señora de la Asunción',
        'uk': 'СДЮСШОР # 123',
        'tr': 'Ramazanoğlu MESLEKI'
    }
    assert make_poi_names(names) == {
        'ru': 'Сбербанк г. Нижневартовск',
        'uk': 'СДЮСШОР # 123',
        'tr': 'Ramazanoğlu Mesleki'
    }


def test_has_stop_words():
    checker = NamesChecker()

    assert not checker.has_stop_words('Мировые Автошины')
    assert not checker.has_stop_words('Клининговая Компания')
    assert not checker.has_stop_words('Эммануэль, блаженство богов')
    assert not checker.has_stop_words('Музей Зенты Маурини')
    assert not checker.has_stop_words('Кигинский пищекомбинат')

    assert checker.has_stop_words('Интернет-магазин Timebox')
    assert checker.has_stop_words('Фотограф Андрей Воеводин')
    assert checker.has_stop_words('Психолог Карташова И.А.')
    assert checker.has_stop_words('ЖК Морские камни')
    assert checker.has_stop_words('Частный компьютерный мастер Геннадий')
    assert checker.has_stop_words('Лубенско-Оржицкий объединенный городской военный комиссариат')
    assert checker.has_stop_words('Слонимский Районный Отдел Управления КГБ РБ по Гродненской области')
    assert checker.has_stop_words('Сантехник')
    assert checker.has_stop_words('Югавторемонт, ИП')


def test_is_fio():
    checker = NamesChecker()

    assert not checker.is_fio('Мамонтов')
    assert not checker.is_fio('Музей В.Г. Короленко')
    assert not checker.is_fio('Дом-Музей Д.А. Кунаева')

    assert checker.is_fio('Мордвесский центр Образования Имени В. Ф. Романова')
    assert checker.is_fio('Аудиторские услуги Захаревич П. Г.')
    assert checker.is_fio('Риэлтор И.И. Горбачев')
    assert checker.is_fio('Психолог И.И. Горбачев')


def test_extra_org():
    checker = NamesChecker()

    assert checker.is_valid_extra_org_name('Яндекс', None, 'Яндекс')
    assert checker.is_valid_extra_org_name('ВТБ', 'ВТБ', 'ВТБ')

    assert not checker.is_valid_extra_org_name('$$$Казино$$$', None, '$$$Казино$$$')

    assert not checker.is_valid_extra_org_name(
        'Какое-то очень длинное название которое мне сильно лень придумывать', None, None)
    assert not checker.is_valid_extra_org_name(
        'Основное имя норм',
        'А вот короткое имя придумывал какой-то сочинялкин и все пошло по кошечке',
        None
    )
    assert not checker.is_valid_extra_org_name('Аркаша', 'ИП', 'ИП')

    assert checker.is_valid_extra_org_name(
        'Пункт выдачи интернет-магазина Ozon.ru', 'Ozon.ru', 'Ozon.ru')
    assert not checker.is_valid_extra_org_name(
        'Интернет-магазин Ozon.ru', None, 'Интернет-магазин Ozon.ru')
    assert not checker.is_valid_extra_org_name(
        'Интернет магазин Ozon.ru', None, 'Интернет магазин Ozon.ru')

    assert not checker.is_valid_extra_org_name('GoldHorse, магазин', None, 'GoldHorse, магазин')
    assert not checker.is_valid_extra_org_name('Выездной нарколог', None, 'Выездной нарколог')


def test_names_storage():
    storage = NamesStorage()

    assert storage.try_add({'ru': 'Калинка-Стокманн'})
    assert not storage.try_add({'ru': 'Магазин Калинка Стокманн'})
    assert storage.try_add({'ru': 'Калинка-Малинка'})

    assert storage.try_add({'ru': 'СШ 193'})
    assert not storage.try_add({'ru': 'Средняя школа № 193'})
    assert storage.try_add({'ru': 'Средняя школа № 194'})

    assert storage.try_add({'ru': 'Аптека АБК'})
    assert storage.try_add({'ru': 'Аптека Белфармация'})
    assert not storage.try_add({'ru': 'Аптека'})

    assert storage.try_add({'ru': 'Альфабанк'})
    assert not storage.try_add({'ru': 'Альфа-Банк'})
    assert not storage.try_add({'ru': 'Альфа Банк'})

    assert storage.try_add({'ru': 'Администрация района'})
    assert not storage.try_add({'ru': 'Администрация Истринского района'})
