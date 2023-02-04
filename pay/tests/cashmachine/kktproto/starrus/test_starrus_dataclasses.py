from tests.cashmachine.testsdata import RECEIPT_CONTENT, COMPLEX_REQUEST_BODY
from kkt_srv.cashmachine.kktproto.starrus.starrus_dataclasses import StarrusReceiptContent105


def test_receipt_conv():
    converted = StarrusReceiptContent105.from_fiscal_data_class(RECEIPT_CONTENT).as_dict()
    assert converted == COMPLEX_REQUEST_BODY
