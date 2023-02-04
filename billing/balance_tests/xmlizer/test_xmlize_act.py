from balance.actions.invoice_turnon import InvoiceTurnOn
from balance.xmlizer import getxmlizer, xml_text
from tests.balance_tests.xmlizer.xmlizer_common import create_invoice
from balance import muzzle_util as ut


def test_act_xmlize(session):
    invoice = create_invoice(session)
    InvoiceTurnOn(invoice, manual=True).do()
    invoice.close_invoice(session.now())

    act = invoice.acts[0]

    assert xml_text(act.id) == getxmlizer(act).xmlize().find('id').text
    assert xml_text(act.external_id) == getxmlizer(act).xmlize().find('external-id').text
    assert xml_text(act.dt) == getxmlizer(act).xmlize().find('dt').text
    assert getxmlizer(act).xmlize().find('factura').text is None
    assert xml_text(ut.dsum(a.act_sum for a in act.rows)) == getxmlizer(act).xmlize().find('act-sum').text
    assert xml_text(act.invoice.id) == getxmlizer(act).xmlize().find('invoice/id').text
    assert xml_text(act.invoice.external_id) == getxmlizer(act).xmlize().find('invoice/external-id').text
    assert xml_text(act.invoice.currency) == getxmlizer(act).xmlize().find('invoice/currency').text
    assert xml_text(act.invoice.receipt_sum_1c) == getxmlizer(act).xmlize().find('invoice/receipt-sum-1c').text
    assert xml_text(act.invoice.total_sum) == getxmlizer(act).xmlize().find('invoice/total-sum').text

    for c in getxmlizer(act).xmlize().findall('rows/rows/price'):
        assert xml_text(28.9989) == c.text
    for c in getxmlizer(act).xmlize().findall('rows/rows/invoice-sum'):
        assert xml_text(63797.59) == c.text

    act_trans = getxmlizer(act).xmlize().findall('rows/rows')
    for i in xrange(len(act_trans)):
        at = act.rows[i]
        _at_ = lambda x: act_trans[i].find(x).text
        assert xml_text(at.id) == _at_('id')
        assert xml_text(at.act_sum) == _at_('act-sum')
        assert xml_text(at.amount) == _at_('amount')
        assert xml_text(ut.round00(at.act_sum)) == _at_('finish_sum')
