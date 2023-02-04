from kkt_srv.cashmachine.dataobjects import ReceiptContent, ReceiptRow, Payment, CorrectionReason, AdditionalUserRequisite, SupplierInfo

from decimal import Decimal
from datetime import datetime

CORRECTION_RECEIPT_RAWFORM = {
  "TagID": 31,
  "TagType": "stlv",
  "Value": [
    {
      "TagID": 1055,
      "TagType": "byte",
      "Value": 1
    },
    {
      "TagID": 1031,
      "TagType": "money",
      "Value": 0
    },
    {
      "TagID": 1081,
      "TagType": "money",
      "Value": 10050
    },
    {
      "TagID": 1215,
      "TagType": "money",
      "Value": 0
    },
    {
      "TagID": 1216,
      "TagType": "money",
      "Value": 0
    },
    {
      "TagID": 1217,
      "TagType": "money",
      "Value": 0
    },
    {
      "TagID": 1060,
      "TagType": "string",
      "Value": "nalog.ru"
    },
    {
      "TagID": 1173,
      "TagType": "byte",
      "Value": 0
    },
    {
      "TagID": 1174,
      "TagType": "stlv",
      "Value": [
        {
          "TagID": 1178,
          "TagType": "unixtime",
          "Value": "2017-07-15T00:00:00Z"
        },
        {
          "TagID": 1179,
          "TagType": "string",
          "Value": "0"
        }
      ]
    },
    {
      "TagID": 1108,
      "TagType": "byte",
      "Value": 1
    },
    {
      "TagID": 1036,
      "TagType": "string",
      "Value": "6660666"
    },
    {
      "TagID": 1209,
      "TagType": "byte",
      "Value": 2
    },
    {
      "TagID": 1102,
      "TagType": "money",
      "Value": 1675
    },
    {
      "TagID": 1008,
      "TagType": "string",
      "Value": "user@example.com"
    },
    {
      "TagID": 1084,
      "TagType": "stlv",
      "Value": [
        {
          "TagID": 1085,
          "TagType": "string",
          "Value": "queue_id"
        },
        {
          "TagID": 1086,
          "TagType": "string",
          "Value": "12345"
        }
      ]
    },
    {
      "TagID": 1059,
      "TagType": "stlv",
      "Value": [
        {
          "TagID": 1079,
          "TagType": "money",
          "Value": 5025
        },
        {
          "TagID": 1023,
          "TagType": "qty",
          "Value": 2000
        },
        {
          "TagID": 1043,
          "TagType": "money",
          "Value": 10050
        },
        {
          "TagID": 1199,
          "TagType": "byte",
          "Value": 1
        },
        {
          "TagID": 1200,
          "TagType": "money",
          "Value": 1675
        },
        {
          "TagID": 1030,
          "TagType": "string",
          "Value": "The title"
        },
        {
          "TagID": 1214,
          "TagType": "byte",
          "Value": 1
        },
        {
          "TagID": 1226,
          "TagType": "string",
          "Value": "132710341000"
        }
      ]
    },
    {
      "TagID": 1038,
      "TagType": "uint32",
      "Value": 1
    },
    {
      "TagID": 1042,
      "TagType": "uint32",
      "Value": 1
    },
    {
      "TagID": 1054,
      "TagType": "byte",
      "Value": 1
    },
    {
      "TagID": 1020,
      "TagType": "money",
      "Value": 10050
    },
    {
      "TagID": 1077,
      "TagType": "byte[]",
      "Value": "AAAOaQwA"
    },
    {
      "TagID": 1040,
      "TagType": "uint32",
      "Value": 3
    },
    {
      "TagID": 1018,
      "TagType": "string",
      "Value": "100051000501"
    },
    {
      "TagID": 1012,
      "TagType": "unixtime",
      "Value": "2021-10-07T13:19:00Z"
    },
    {
      "TagID": 1041,
      "TagType": "string",
      "Value": "9999000000000002"
    },
    {
      "TagID": 1037,
      "TagType": "string",
      "Value": "6666006666055612    "
    },
    {
      "TagID": 1057,
      "TagType": "byte",
      "Value": 64
    },
    {
      "TagID": 1048,
      "TagType": "string",
      "Value": "Яндекс"
    },
    {
      "TagID": 1117,
      "TagType": "string",
      "Value": "example@yandex.ru"
    },
    {
      "TagID": 1187,
      "TagType": "string",
      "Value": "Ивантеевка"
    }
  ]
}

RECEIPT_CONTENT = ReceiptContent(
    client_email_or_phone='user@example.com',
    fiscal_doc_type='CorrectionReceipt',
    rows=[
      ReceiptRow(
        price=Decimal('50.25'),
        qty=Decimal('2'),
        text='The title',
        payment_type_type='full_prepayment_wo_delivery',
        tax_type='nds_20',
        amount=Decimal('100.50'),
        tax_amount=Decimal('16.75'),
        supplier_info=SupplierInfo(inn='132710341000')
      )
    ],
    receipt_type='income',
    taxation_type='OSN',
    firm_inn='100051000501',
    payments=[
      Payment(amount=Decimal('100.50'), payment_type='card'),
      Payment(amount=Decimal('0.00'), payment_type='prepayment'),
      Payment(amount=Decimal('0.00'), payment_type='credit'),
      Payment(amount=Decimal('0.00'), payment_type='extension'),
      Payment(amount=Decimal('0.00'), payment_type='cash')
    ],
    additional_user_requisite=AdditionalUserRequisite(name='queue_id', value='12345'),
    correction_type='independently',
    agent_type='agent',
    reason=CorrectionReason(
      dt=datetime(2017, 7, 15, 0, 0),
      number='0'
    )
)


RECEIPT_CONTENT_DOUBLE = ReceiptContent(
  client_email_or_phone='example123456@yandex-team.ru',
  fiscal_doc_type='CorrectionReceipt',
  rows=[
    ReceiptRow(
      price=Decimal('757.00'),
      qty=Decimal('1'),
      text='Transport de passagers et de bagage',
      payment_type_type='prepayment',
      tax_type='nds_20',
      supplier_info=SupplierInfo(inn='132710341000')
    ),
    ReceiptRow(
      price=Decimal('85.00'),
      qty=Decimal('1'),
      text='Abonnement Yandex Plus prolong',
      payment_type_type='prepayment',
      tax_type='nds_none',
      supplier_info=SupplierInfo(inn='7736207543')
    )
  ],
  receipt_type='income',
  taxation_type='OSN',
  firm_inn='100051000501',
  payments=[
    Payment(amount=Decimal('842.00'), payment_type='card')
  ],
  correction_type='independently',
  reason=CorrectionReason(name=" ", number="0", dt=datetime(2021, 9, 14)),
  additional_user_requisite=AdditionalUserRequisite(name='queue_id', value='12345'),
  agent_type='agent',
  firm_url='yandex.ru',
  firm_reply_email='example12345@yandex-team.ru'
)


COMPLEX_REQUEST_BODY = {
    'CorrectionType': 0,
    'DocumentType': 128,
    'Lines': [{
        'Description': 'The title',
        'PayAttribute': 1,
        'Price': 5025,
        'Qty': 2000,
        'TaxId': 1,
        'ProviderData': {'INN': '132710341000'}
    }],
    'NonCash': [10050],
    'AdvancePayment': 0,
    'Credit': 0,
    'Cash': 0,
    'Consideration': 0,
    'PhoneOrEmail': 'user@example.com',
    'Reason': {'Date': {'Day': 15, 'Month': 7, 'Year': 17}, 'Number': '0'},
    'TaxMode': 1,
    'UserRequisite': {'Title': 'queue_id', 'Value': '12345'},
    'PaymentAgentModes': 64,
    'FullResponse': True,
    'TaxCalculationMethod': 1,
    'MaxDocumentsInTurn': 1000,
}
