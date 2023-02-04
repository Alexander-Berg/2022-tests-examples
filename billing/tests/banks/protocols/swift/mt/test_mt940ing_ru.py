from decimal import Decimal

import datetime
import pytest

from bcl.banks.protocols.swift.mt.mt940ing_ru import Mt940IngRu, Mt942IngRu


def test_parse_mt940(read_fixture):
    src = read_fixture('ing_ru_mt940-allday.txt', decode='cp1251')
    docs = Mt940IngRu.fromstring(src)

    assert len(docs) == 1

    mt_doc = docs[0]  # type: Mt942IngRu

    assert len(mt_doc.statement_line) == 5
    assert len(mt_doc.acc_owner_info) == 6
    assert mt_doc.ref_transaction == 'STMT20171003'
    assert mt_doc.account_id == '40702810300001004400'
    assert mt_doc.num_statement_sequence == '10275'

    assert mt_doc.balance_opening == 'C171002RUB6486256,49'
    assert mt_doc.balance_opening_dc_mark == 'C'
    assert mt_doc.balance_opening_date == datetime.datetime(2017, 10, 2)
    assert mt_doc.balance_opening_currency == 'RUB'
    assert mt_doc.balance_opening_amount == Decimal('6486256.49')

    assert mt_doc.balance_closing == 'C171002RUB5903944,20'
    assert mt_doc.balance_closing_dc_mark == 'C'
    assert mt_doc.balance_closing_date == datetime.datetime(2017, 10, 2)
    assert mt_doc.balance_closing_currency == 'RUB'
    assert mt_doc.balance_closing_amount == Decimal('5903944.20')

    assert mt_doc.balance_closing_available == 'C171002RUB5903944,20'

    assert mt_doc.statement_line[0] == '171002DB36,00NTRF //4468'
    assert mt_doc.statement_line_date_value[0] == datetime.datetime(2017, 10, 2)
    assert mt_doc.statement_line_dc_mark[0] == 'D'
    assert mt_doc.statement_line_funds_code[0] == 'B'
    assert mt_doc.statement_line_amount[0] == Decimal('36.00')
    assert mt_doc.statement_line_trans_type[0] == 'N'
    assert mt_doc.statement_line_id_code[0] == 'TRF'
    assert mt_doc.statement_line_ref_acc_serv_inst[0] == '4468'

    info = """~01ИНГ БАНК (ЕВРАЗИЯ) АО~027712014310~0370601810119232740206~0404\r\n4525222~05ИНГ БАНК (ЕВРАЗИЯ) АКЦИОНЕРНОЕ ОБЩЕСТВО~06Комиссия за п\r\nроведение 3 платежных документов за 02-10-2017 НДС не облагается.\r\n~1130101810500000000222~1320171002~1520171002~1705~1817~1900"""
    assert mt_doc.acc_owner_info[0] == info
    assert mt_doc.purpose[0] == 'Комиссия за проведение 3 платежных документов за 02-10-2017 НДС не облагается.'
    assert mt_doc.payer_bank_name[0] == 'ИНГ БАНК (ЕВРАЗИЯ) АКЦИОНЕРНОЕ ОБЩЕСТВО'
    assert mt_doc.payer_inn[0] == '7712014310'
    assert mt_doc.payer_name[0] == 'ИНГ БАНК (ЕВРАЗИЯ) АО'
    assert mt_doc.payer_bank_bik[0] == '044525222'
    assert mt_doc.operation_type[0] == '17'


def test_parse_mt942(read_fixture):
    src = read_fixture('ing_ru_mt942-intraday.txt', decode='cp1251')
    docs = Mt942IngRu.fromstring(src)

    assert len(docs) == 42

    mt_doc = docs[0]  # type: Mt942IngRu

    assert mt_doc.purpose[0] == 'ЗА 02/10/2017;Холоднюк Сергей Николаевич;Перерва 28/25;Оплата по счету Б 412939651 1'

    assert len(mt_doc.statement_line) == 1
    assert mt_doc.ref_transaction == '031017'
    assert mt_doc.account_id == '40702810100001005379'
    assert mt_doc.num_statement_sequence == '00310'
    assert mt_doc.dc_floor_limit_currency == 'RUB'
    assert mt_doc.dc_floor_limit_dc_mark == 'C'
    assert mt_doc.dc_floor_limit_amount == Decimal('0.00')
    assert mt_doc.statement_date.replace(tzinfo=None) == datetime.datetime(2017, 10, 3, 13, 3)

    assert mt_doc.statement_line[0] == '171003CB2000,00NTRF //92245'
    assert mt_doc.statement_line_date_value[0] == datetime.datetime(2017, 10, 3)
    assert mt_doc.statement_line_dc_mark[0] == 'C'
    assert mt_doc.statement_line_funds_code[0] == 'B'
    assert mt_doc.statement_line_amount[0] == Decimal('2000.00')
    assert mt_doc.statement_line_trans_type[0] == 'N'
    assert mt_doc.statement_line_id_code[0] == 'TRF'
    assert mt_doc.statement_line_ref_acc_serv_inst[0] == '92245'
    #
    info = """~01ПАО СБЕРБАНК//ХОЛОДНЮК СЕРГЕЙ НИКОЛАЕВИЧ//286804793406//РОССИЯ
 101000 МОСКОВСКАЯ Р-Н ОДИНЦОВСКИЙ Г ПАНСИОННЫЙ ЛЕСНОЙ УЛ ЛЕСНОЙ 
Д 15 КВ 5//~027707083893~0330233810749000600001~04045773603~05ЗАП
АДНО-УРАЛЬСКИЙ БАНК ПАО СБЕРБАНК~06ЗА 02/10/2017;Холоднюк Сергей 
Николаевич;Перерва 28/25;Оплата по счету Б 412939651 1~1130101810
900000000603~1320171003~1520171003~1705~1801~1900""".split('\n')

    assert mt_doc.acc_info[0] == '\r\n'.join(info)
    assert mt_doc.payer_bank_name[0] == 'ЗАПАДНО-УРАЛЬСКИЙ БАНК ПАО СБЕРБАНК'
    assert mt_doc.payer_inn[0] == '7707083893'
    assert mt_doc.payer_name[
               0] == 'ПАО СБЕРБАНК//ХОЛОДНЮК СЕРГЕЙ НИКОЛАЕВИЧ//286804793406//РОССИЯ 101000 МОСКОВСКАЯ Р-Н ОДИНЦОВСКИЙ Г ПАНСИОННЫЙ ЛЕСНОЙ УЛ ЛЕСНОЙ Д 15 КВ 5//'
    assert mt_doc.payer_bank_bik[0] == '045773603'
    assert mt_doc.operation_type[0] == '01'
