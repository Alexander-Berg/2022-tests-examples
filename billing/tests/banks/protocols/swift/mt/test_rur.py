from bcl.banks.protocols.swift.mt.rur import get_transliteration_table, transliterate, check_transliteratable


def test_get_transliteration_table():

    assert get_transliteration_table()['‘'] == 'j'
    assert get_transliteration_table(to_latin=False)['m'] == '”'


def test_check_transliteratable():

    assert not check_transliteratable('76b, Vaja-Pshavela Ave. Tbilisi,Georgia')
    assert check_transliteratable('76b, Текст Tbilisi,Georgia')
    assert check_transliteratable('да')


def test_transliterate():

    texts = [
        (
            '76b, Текст Vaja-Pshavela Ave. Tbilisi,Georgia',
            "76'B', TEKST 'VAJA'-'PSHAVELA AVE'. 'TBILISI','GEORGIA",
            '76B, ТЕКСТ VAJA-PSHAVELA AVE. TBILISI,GEORGIA',
        ),
        (
            'ПУШНИНА ВЫСШЕГО КАЧЕСТВА',
            'PUQNINA VYSQEGO KAcESTVA',
            None,
        ),
        (
            'ЧАПАЕВСКИЙ РЫБОПЕРЕРАБАТЫВАЮЩИЙ',
            'cAPAEVSKIi RYBOPERERABATYVAuqIi',
            None,
        ),
        (
            'РАЗЪЁМЫ ЭЛЕКТРИЧЕСКИЕ',
            'RAZxoMY eLEKTRIcESKIE',
            None,
        ),
        (
            'ЭТОТ ТЕКСТ ДОЛЖЕН КОРРЕКТНО ПЕРЕДАТЬСЯ ПО СЕТИ SWIFT В ДРУГОЙ БАНК',
            'eTOT TEKST DOLJEN KORREKTNO PEREDATXSa PO SETI \'SWIFT \'V DRUGOi BANK',
            None,
        ),
        (
            'Оплата за товар по счёту №123 от 12.01.2010, в т. ч. НДС (20%) 1800 руб.00 коп.',
            'OPLATA ZA TOVAR PO ScoTU n123 OT 12.01.2010, V T. c. NDS (20p) 1800 RUB.00 KOP.',
            'ОПЛАТА ЗА ТОВАР ПО СЧЁТУ №123 ОТ 12.01.2010, В Т. Ч. НДС (20%) 1800 РУБ.00 КОП.',
        ),
        (
            'Оплата компании DON’T WORRY по договору ABC1111 по счёту 2222DEF3333',
            "OPLATA KOMPANII 'DON'j'T WORRY 'PO DOGOVORU 'ABC1111 'PO ScoTU 2222'DEF3333",
            "ОПЛАТА КОМПАНИИ DON'T WORRY ПО ДОГОВОРУ ABC1111 ПО СЧЁТУ 2222DEF3333",
        ),
        (
            '{VO10040} Оплата по договору',
            "'(VO10040)' OPLATA PO DOGOVORU",
            '{VO10040} ОПЛАТА ПО ДОГОВОРУ',
        ),
    ]

    for text_cyr, text_lat, text_cyrdec in texts:
        result = transliterate(text_cyr)
        assert result[0] == text_lat
        assert len(result[1]) == 0

        text_cyrdec = text_cyrdec or text_cyr
        result = transliterate(text_lat, to_latin=False)
        assert result[0] == text_cyrdec
        assert len(result[1]) == 0

    assert len(transliterate('что-то — wrong')[1]) == 1
