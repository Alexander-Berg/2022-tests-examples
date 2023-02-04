# -*- coding: utf-8 -*-

from balance import mapper
from balance.contractpage import ContractPage

from tests.object_builder import ClientBuilder, PersonBuilder


def test_postsecurity(session):
    person = PersonBuilder(client=ClientBuilder(is_agency=1, name="Test agency"),
                           type='yt', operator_uid=0).build(session)

    req = {
        'firm': u'1', 'services': u'1', 'services-11': u'11',
        'commission-type': u'11', 'payment-type': u'2', 'memo': u'req',
        'services-7': u'7',
        'client-id': unicode(person.obj.client.id),
        'person-id': unicode(person.obj.id), 'commission': u'6', 'commission-charge-type': u'0',
        'dt': u'2004-02-01T00:00:00', 'manager-code': u'1134', 'discard-nds': u'0',
        'finish-dt': u'2009-01-29T00:00:00', 'manager-bo-code': u'', 'supercommission': u'1',
        'account-type': u'0', 'declared-sum': u'2500000 руб.', 'discount-pct': u'15 %',
        'discount-policy-type': u'0', 'credit-type': u'1', 'payment-term': u'10',
        'turnover-forecast-18': u'22', 'turnover-forecast': u'1',
        'bank-details-id': u'21', 'currency': u'810'
    }

    pg = ContractPage(session, 0)
    pg.post(req)
    assert pg.contract_id is not None

    session.flush()

    pg = ContractPage(session, pg.contract_id)
    col = pg.contract.collaterals[0]

    assert col.discount_pct == 10

    pg.post({'id': '%s' % pg.contract_id, 'declared-sum': u'100 руб.', 'discount-pct': u'15 %'})
    assert col.discount_pct == 8

    pg = ContractPage(session, pg.contract_id)
    col = pg.contract.collaterals[0]
    col.discount_pct = 3
    pg.post({'id': '%s' % pg.contract_id, 'declared-sum': u'100 руб.', 'discount-pct': u'15 %'})
    assert col.discount_pct == 3

    # create collateral
    req = {
        'col-new-dt': '2008-06-06T00:00:00',
        'col-new-memo': 'sss',
        'col-new-collateral-type': '115',
        'col-new-num': '1',
        'col-new-group02-grp-115-declared-sum': u'333 руб.',
        'col-new-group02-grp-115-discount-pct': u'33 %',
        'id': '%s' % pg.contract_id
    }

    pg = ContractPage(session, pg.contract_id)
    pg.post(req)
    pg.session.flush()

    contract = session.query(mapper.Contract).getone(pg.contract_id)
    assert len(contract.collaterals) == 2

    pg = ContractPage(session, pg.contract_id)
    pg.get()
