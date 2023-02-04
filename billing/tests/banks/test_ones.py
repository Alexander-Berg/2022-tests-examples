import copy

import pytest

from bcl.banks.common.payment_creator.ones import OneCPaymentCreator
from bcl.banks.registry import Sber
from bcl.core.views.rpc import Rpc


@pytest.mark.parametrize('payment_dict', [{}, {'income_type': '2'}])
def test_payment_creator(payment_dict, build_payment_bundle):

    bundle = build_payment_bundle(associate=Sber, payment_dicts=[payment_dict])
    assert 'Сумма=152.00\r' in bundle.tst_compiled
    assert 'ПоказательКБК=\r' in bundle.tst_compiled
    assert 'None' not in bundle.tst_compiled
    assert f"ВидПлатежа=Электронно\r\nКодНазПлатежа={payment_dict.get('income_type','')}" in bundle.tst_compiled


def test_sanitize():

    expected = (
        'Перечисление ДС по ЗП 40817//реестр//ZPR_052001_21022017_7736207543 '
        'работникам ООО Яндекс по договору N Z 18 от 21.04.2008. НДС не облагается.'
    )

    assert OneCPaymentCreator.sanitize_value(
        'Перечисление ДС по ЗП 40817//реестр//ZPR_052001_21022017_7736207543 '
        'работникам ООО Яндекс по договору № Z 18 от 21.04.2008.\nНДС не облагается.'
    ) == expected

    assert OneCPaymentCreator.sanitize_value(
        'Перечисление ДС по ЗП 40817//реестр//ZPR_052001_21022017_7736207543 '
        'работникам ООО Яндекс по договору № Z 18 от 21.04.2008.\r\nНДС не облагается.'
    ) == expected

    assert OneCPaymentCreator.sanitize_purpose('Текст \r\n«№20»') == 'Текст  "№20"'


@pytest.mark.parametrize(
    "changed_params",
    [{},
     {'account': ''},
     {'bik': ''},
     {'ground': ''},
     {'inn': ''},
     {'name': ''},
     {'account': '', 'bik': '', 'ground': '', 'inn': '', 'name': ''}
     ])
def test_check_parse_statement(changed_params, parse_statement_fixture):
    """Проверяет разбор выписки 1С (поля, которые далее фигурируют в ОЕБС)"""
    default_data = {'account': '40702810555070002569', 'bik': '044030653', 'direction': 'IN',
                    'doc_number': '1003838', 'ground': 'Проверка разбора',
                    'inn': '118111119', 'name': 'Плательщик', 'summ': '18.13',
                    'doc_date': '2018-06-26 00:00:00'}

    transaction_data = copy.deepcopy(default_data)
    transaction_data.update(changed_params)

    def replace_data(text):
        for key in transaction_data:
            text = text.replace(key, transaction_data[key])
        return text

    register, payments = parse_statement_fixture(
        'sberbank_check_statement_data.txt', Sber, '40702840338000001463', 'RUB', mutator_func=replace_data,
        encoding=Sber.statement_dispatcher.parsers[0].encoding
    )[0]

    assert register.is_valid
    payment = Rpc.to_dict_proved_pay(payments[0])
    for key in transaction_data:
        assert transaction_data[key] == payment[key]


def test_priority(build_payment_bundle):

    compiled = build_payment_bundle(
        Sber,
        payment_dicts=[
            {'priority': 33},
            {'priority': None},
            {'priority': 0},
        ]
    ).tst_compiled

    assert 'ПоказательДаты' in compiled
