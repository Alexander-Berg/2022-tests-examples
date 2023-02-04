from decimal import Decimal

from datetime import datetime

from bcl.banks.protocols.arm.documents import PaymentOrder, compile_payment_bundle


def test_payment():

    order1 = PaymentOrder()
    order1.doc_number = '123'
    order1.doc_date = datetime(2017, 5, 26)

    order1.payer_account = '12321'
    order1.payer_inn = '12345'

    order1.payee_account = '88888'
    order1.payee_name = 'some'

    order1.amount = Decimal('20.15')
    order1.purpose = 'purp'

    orders = [
        order1,
    ]

    compiled = compile_payment_bundle(orders)

    assert compiled == (
        "<?xml version='1.0' encoding='utf-8' standalone='yes'?>\n"
        '<As_Import-Export_File>'
        '<PayOrd CAPTION="Documents (Payment Inside of RA)">'
        '<PayOrd DOCNUM="123" DOCDATE="26/05/17" PAYERACC="12321" TAXCODE="12345" '
        'BENACC="88888" BENEFICIARY="some" AMOUNT="20.15" DETAILS="purp"/>'
        '</PayOrd>'
        '</As_Import-Export_File>')
