from kkt_srv.cashmachine.kktproto.starrus.kkt_data import TaxTypeData  # noqa # pylint: disable=unused-import
from kkt_srv.cashmachine.dataobjects import (
  SupplierInfo, ReceiptRow, Receipt, KktInfo, ReceiptExtraContent, Tax, ReceiptContent
)
from kkt_srv.cashmachine.kktproto.starrus.converters import _convert_rawform, traf_row, traf_recp, _cnv_taxes_and_supplier_inn
from kkt_srv.cashmachine.fns_constants import (
    PaymentTypeType, TaxTypeFN
)

from testsdata import RECEIPT_CONTENT, CORRECTION_RECEIPT_RAWFORM, RECEIPT_CONTENT_DOUBLE

from decimal import Decimal
from datetime import datetime


def test_receipt_row_conv():
    tags = {
        1079: "1000",
        1023: "1000",
        1030: "Description",
        1214: 1,
        1199: 1,
        1222: 0,
        1226: '1234567890',
        1224: [{1225: 'Name', 1171: '1-23-45'}]
    }
    res = traf_row.transform(tags)
    if res.get('supplier_inn'):
        res.setdefault('supplier_info', {})['inn'] = res['supplier_inn']
        del res['supplier_inn']

    row = ReceiptRow(
        price=Decimal("10.0"),
        qty=Decimal("1.0"),
        text="Description",
        payment_type_type='full_prepayment_wo_delivery',
        tax_type='nds_20',
        agent_type='none_agent',
        supplier_info=SupplierInfo(inn='1234567890', name='Name', phone='1-23-45')
    )
    assert ReceiptRow.parse_obj(res) == row


def test_real_rawform_conv():
    expected_content = Receipt(
      document_index=1,
      shift_number=1,
      dt=datetime(2021, 10, 7, 13, 19),
      id=3,
      kkt=KktInfo(
        automatic_machine_number='6660666',
        rn='6666006666055612',
      ),
      fn={'sn': '9999000000000002'},
      ofd={'check_url': 'nalog.ru'},
      receipt_content=RECEIPT_CONTENT,
      receipt_extra_content=ReceiptExtraContent(
        fp='AAAOaQwA',
        ffd_version='1.05',
        total=Decimal('100.50'),
        tax_totals=[
          Tax(tax_amount=Decimal('16.75'), tax_type='nds_20'),
        ],
        cash_total=Decimal('0.00')
      ),
    )

    doc_type, converted = _convert_rawform(CORRECTION_RECEIPT_RAWFORM)
    transformed = traf_recp.transform(converted)
    _cnv_taxes_and_supplier_inn(transformed)
    for row in transformed['receipt_content']['rows']:
        row['tax_type'] = row['tax_type'].value.fn_enum
    for tax_row in transformed['receipt_extra_content']['tax_totals']:
        tax_row['tax_type'] = tax_row['tax_type'].value.fn_enum
    transformed['receipt_content']['fiscal_doc_type'] = doc_type

    assert Receipt.parse_obj(transformed) == expected_content


def test_as_dict():
    real = ReceiptRow(
        price=Decimal("10.0"),
        qty=Decimal("1.0"),
        text="Description",
        payment_type_type='full_prepayment_wo_delivery',
        tax_type='nds_20',
        item_code="12345",
        supplier_info=SupplierInfo(inn='1234567890', name='Name', phone='1-23-45')
    ).dict(exclude_unset=True)
    assert real == {
      'item_code': '12345',
      'payment_type_type': PaymentTypeType.full_prepayment_wo_delivery,
      'price': Decimal('10.0'),
      'qty': Decimal('1.0'),
      'supplier_info': {'inn': '1234567890', 'name': 'Name', 'phone': '1-23-45'},
      'tax_type': TaxTypeFN.nds_18and20,
      'text': 'Description'
    }


def test_from_dict():
    jsn = {
        "firm_inn": "100051000501",
        "receipt_type": "income",
        "taxation_type": "OSN",
        "agent_type": "agent",
        "fiscal_doc_type": 'CorrectionReceipt',
        "client_email_or_phone": "example123456@yandex-team.ru",
        "firm_reply_email": "example12345@yandex-team.ru",
        "firm_url": "yandex.ru",
        "additional_user_requisite": {
          "name": "queue_id",
          "value": "12345"
        },
        "rows": [
          {
            "price": "757.00",
            "qty": "1",
            "tax_type": "nds_20",
            "payment_type_type": "prepayment",
            "text": "Transport de passagers et de bagage",
            "supplier_info": {
              "inn": "132710341000"
            }
          },
          {
            "price": "85.00",
            "qty": "1",
            "tax_type": "nds_none",
            "payment_type_type": "prepayment",
            "text": "Abonnement Yandex Plus prolong",
            "supplier_info": {
              "inn": "7736207543"
            }
          }
        ],
        "payments": [
          {
            "amount": "842.00",
            "payment_type": "card"
          }
        ],
        "correction_type": "independently",
        "reason": {
            "name": " ",
            "number": "0",
            "dt": "2021-09-14"
        }
    }
    assert ReceiptContent.parse_obj(jsn) == RECEIPT_CONTENT_DOUBLE
