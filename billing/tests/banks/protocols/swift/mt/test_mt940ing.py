import datetime
from decimal import Decimal

from typing import *

from bcl.banks.protocols.swift.mt.mt940ing import Mt940Ing


def test_parse(read_fixture):
    docs = Mt940Ing.fromstring(read_fixture('ing_mt940.txt', decode='utf-8'))  # type: List[Mt940Ing]

    assert len(docs) == 1

    mt_doc = docs[0]

    assert len(mt_doc.statement_line) == 13
    assert len(mt_doc.acc_owner_info) == 14
    assert mt_doc.ref_transaction == 'ING'
    assert mt_doc.account_id == 'NL35INGB0650977351'
    assert mt_doc.num_statement_sequence == '159'

    assert mt_doc.balance_opening == 'C170814EUR4429690,96'
    assert mt_doc.balance_opening_dc_mark == 'C'
    assert mt_doc.balance_opening_date == datetime.datetime(2017, 8, 14)
    assert mt_doc.balance_opening_currency == 'EUR'
    assert mt_doc.balance_opening_amount == Decimal('4429690.96')

    assert mt_doc.balance_closing == 'C170815EUR3077910,90'
    assert mt_doc.balance_closing_dc_mark == 'C'
    assert mt_doc.balance_closing_date == datetime.datetime(2017, 8, 15)
    assert mt_doc.balance_closing_currency == 'EUR'
    assert mt_doc.balance_closing_amount == Decimal('3077910.90')

    assert mt_doc.balance_closing_available == 'C170815EUR3077926,90'
    assert mt_doc.balance_forward_available[0] == 'C170816EUR3077910,90'
    assert mt_doc.balance_forward_available[1] == 'C170817EUR3077910,90'

    assert mt_doc.statement_line[0] == '170815C1000,00NTRFEREF//72270800171081\r\n/TRCD/00100/'
    assert mt_doc.statement_line_date_value[0] == datetime.datetime(2017, 8, 15)
    assert mt_doc.statement_line_dc_mark[0] == 'C'
    assert mt_doc.statement_line_amount[0] == Decimal('1000.00')
    assert mt_doc.statement_line_trans_type[0] == 'N'
    assert mt_doc.statement_line_id_code[0] == 'TRF'
    assert mt_doc.statement_line_ref_acc_owner[0] == 'EREF'
    assert mt_doc.statement_line_ref_acc_serv_inst[0] == '72270800171081'
    assert mt_doc.statement_line_details[0] == '/TRCD/00100/'

    assert mt_doc.acc_owner_info[
               0] == '/EREF/btzMPiDR110820170426575341//CNTP/IT84V088056507000100920029\r\n8/CCRTIT2TFOB/ALEAPRO SNC DI FIOROT CRISTIAN///REMI/USTD//Adverti\r\nsing campaign- Yandex Direct - Invoice No- EU-345323285-1/'

    assert mt_doc.payer_bank_bik[0] == 'CCRTIT2TFOB'
    assert mt_doc.payer_name[0] == 'ALEAPRO SNC DI FIOROT CRISTIAN'
    assert mt_doc.purpose[0] == 'USTD//Advertising campaign- Yandex Direct - Invoice No- EU-345323285-1'
    assert mt_doc.payer_account[0] == 'IT84V0880565070001009200298'

    assert mt_doc.payer_account[10] == ''
    assert mt_doc.payer_bank_bik[10] == 'BARCGB22XXX'
    assert mt_doc.payer_name[10] == 'UNIQUE DIGITAL MARKETING LIMITED'
