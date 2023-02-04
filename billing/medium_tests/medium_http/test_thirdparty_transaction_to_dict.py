# coding=utf-8

from datetime import datetime

from balance import mapper
from balance.constants import ExportState
from medium.medium_http import thirdparty_transaction_to_dict


def test_empty():
    """ Проверяем, что функция не падает, если все необязательные поля - пустые """
    transaction = mapper.ThirdPartyTransaction(id=1)
    assert thirdparty_transaction_to_dict(transaction) == {
        'id': transaction.id,
        'contract_external_id': None,
        'contract_id': None,
        'invoice_eid': None,
        'iso_currency': None,
        'commission_currency': None,
        'commission_iso_currency': None,
        'partner_iso_currency': None,
        'transaction_type': None,
        'payment_type': None,
        'paysys_type_cc': None,
        'amount': None,
        'amount_fee': None,
        'client_amount': None,
        'internal': None,
        'service_id': None,
        'client_id': None,
        'yandex_reward': None,
        'transaction_dt': None,
        'payout_ready_dt': None,
        'oebs_export_state': None,
        'oebs_export_dt': None,
    }


def test_filled():
    """ Проверяем, что функция вставляет все преобразуемые значения, если они переданы """

    contract = mapper.Contact(id=2, external_id='ДС-1')
    transaction = mapper.ThirdPartyTransaction(id=1, contract_id=contract.id, contract=contract,
                                               transaction_dt=datetime.now(),
                                               payout_ready_dt=datetime.now())
    export = mapper.Export(state=ExportState.exported, export_dt=datetime.now())
    transaction.exports['OEBS'] = export
    assert thirdparty_transaction_to_dict(transaction) == {
        'id': transaction.id,
        'contract_external_id': contract.external_id,
        'contract_id': contract.id,
        'invoice_eid': None,
        'iso_currency': None,
        'commission_currency': None,
        'commission_iso_currency': None,
        'partner_iso_currency': None,
        'transaction_type': None,
        'payment_type': None,
        'paysys_type_cc': None,
        'amount': None,
        'amount_fee': None,
        'client_amount': None,
        'internal': None,
        'service_id': None,
        'client_id': None,
        'yandex_reward': None,
        'transaction_dt': transaction.transaction_dt.strftime('%Y%m%d%H%M%S'),
        'payout_ready_dt': transaction.transaction_dt.strftime('%Y%m%d%H%M%S'),
        'oebs_export_state': export.state,
        'oebs_export_dt': export.export_dt.strftime('%Y%m%d%H%M%S'),
    }
