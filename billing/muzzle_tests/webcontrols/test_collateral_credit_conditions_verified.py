# -*- coding: utf-8 -*-
import pytest

import balance.exc as exc
from balance.contractpage import ContractPage
from tests.object_builder import ClientBuilder, PersonBuilder


dt = '2021-03-10T00:00:00'


@pytest.mark.parametrize(
    'params, servant_type, exc_message',
    [
        pytest.param({
            'col-new-is-signed': u'0',
            'col-new-credit-conditions-verified': u'0',
        }, 'medium', None),
        pytest.param({
            'col-new-is-signed': u'0',
            'col-new-is-signed-dt': dt,
            'col-new-credit-conditions-verified': u'1',
        }, 'medium', u'Атрибут "Кредитные условия проверены" может быть установлен только бэк-офисом'),
        pytest.param({
            'col-new-is-signed': u'1',
            'col-new-is-signed-dt': dt,
            'col-new-credit-conditions-verified': u'0',
        }, 'medium', u'Атрибут "Подписан" может быть установлен только вместе с атрибутом '
                     u'"Кредитные условия проверены" бэк-офисом'),
        pytest.param({
            'col-new-is-signed': u'1',
            'col-new-is-signed-dt': dt,
            'col-new-credit-conditions-verified': u'1',
        }, 'medium', u'Атрибут "Кредитные условия проверены" может быть установлен только бэк-офисом'),
        pytest.param({
            'col-new-is-signed': u'0',
            'col-new-credit-conditions-verified': u'0',
        }, 'muzzle', None),
        pytest.param({
            'col-new-is-signed': u'0',
            'col-new-credit-conditions-verified': u'1',
        }, 'muzzle', None),
        pytest.param({
            'col-new-is-signed': u'1',
            'col-new-is-signed-dt': dt,
            'col-new-credit-conditions-verified': u'0',
        }, 'muzzle', u'Нельзя установить атрибут "Подписан" без "Кредитные условия проверены"'),
        pytest.param({
            'col-new-is-signed': u'1',
            'col-new-is-signed-dt': dt,
            'col-new-credit-conditions-verified': u'1',
        }, 'muzzle', None),
    ]
)
def test_collateral_credit_conditions_verified(session, params, servant_type, exc_message):
    person = PersonBuilder(client=ClientBuilder(name="Test client"),
                           type='ur', operator_uid=0).build(session)

    contract_params = {
        'firm': u'13',
        'services': u'1',
        'services-111': u'111',
        'payment-type': u'3',
        'memo': u'req',
        'client-id': unicode(person.obj.client.id),
        'person-id': unicode(person.obj.id),
        'commission': u'0',
        'dt': u'2021-02-01T00:00:00',
        'manager-code': u'1134',
        'manager-bo-code': u'',
        'credit-type': u'2',
        'payment-term': u'10',
        'bank-details-id': u'510',
        'currency': u'810',
        'country': u'225',
        'is-booked': u'1',
        'is-booked-dt': u'2021-02-01T00:00:00',
        'is-faxed': u'1',
        'is-faxed-dt': u'2021-02-01T00:00:00',
        'partner-credit': u'1',
        'tickets': u'AAA-234',
        'atypical-conditions': u'1',
        'credit-conditions-verified': u'1',
        'is-signed': u'1',
        'is-signed-dt': u'2021-02-01T00:00:00',
    }

    pg = ContractPage(session, 0, servant_type='muzzle')
    pg.post(contract_params)
    session.flush()

    collateral_params = {
        'col-new-dt': u'2021-03-06T00:00:00',
        'col-new-memo': u'sss',
        'col-new-tickets': u'SOMEQUEUE-1',
        'col-new-collateral-type': u'1004',
        'col-new-num': u'0',
        'col-new-group02-grp-115-declared-sum': u'333 руб.',
        'col-new-group02-grp-115-discount-pct': u'33 %',
        'id': '%s' % pg.contract_id
    }
    collateral_params.update(params)

    pg = ContractPage(session, pg.contract_id, servant_type=servant_type)

    if exc_message:
        with pytest.raises(exc.CONTRACT_RULE_VIOLATION) as exc_info:
            pg.post(collateral_params)
        assert exc_message in exc_info.value.msg
    else:
        pg.post(collateral_params)
        assert pg.collaterals
