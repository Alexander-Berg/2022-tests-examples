# -*- coding: utf-8 -*-
import pytest

import balance.exc as exc
from balance import mapper
from balance.contractpage import ContractPage

from tests.object_builder import ClientBuilder, PersonBuilder

@pytest.mark.parametrize(
    'params, after_params, servant_type, exc_message, after_exc_message',
    [
        pytest.param({'atypical-conditions': u'0', 'credit-conditions-verified': u'0',
                      'is-signed': u'1', 'is-signed-dt': u'2021-02-01T00:00:00'},
                     None,
                     'muzzle', None, None,
                     id='MUZZLE_NOT_ATYPICAL'),
        pytest.param({'atypical-conditions': u'1', 'credit-conditions-verified': u'0'},
                     None,
                     'muzzle', None, None, id='MUZZLE_NOT_SIGNED_ATTR_NOT_SET'),
        pytest.param({'atypical-conditions': u'1', 'credit-conditions-verified': u'1',
                      'is-signed': u'1', 'is-signed-dt': u'2021-02-01T00:00:00'},
                     None,
                     'muzzle', None, None,
                     id='MUZZLE_SIGNED_ATTR_SET'),
        pytest.param({'atypical-conditions': u'1', 'credit-conditions-verified': u'0',
                      'is-signed': u'1', 'is-signed-dt': u'2021-02-01T00:00:00'},
                     None,
                     'muzzle', u'Нельзя установить атрибут "Подписан" без "Кредитные условия проверены"', None,
                     id='MUZZLE_SIGNED_ATTR_NOT_SET'),
        pytest.param({'atypical-conditions': u'0', 'credit-conditions-verified': u'0',
                      'is-signed': u'1', 'is-signed-dt': u'2021-02-01T00:00:00'},
                     None,
                     'medium', None, None,
                     id='MEDIUM_NOT_ATYPICAL'),
        pytest.param({'atypical-conditions': u'1', 'credit-conditions-verified': u'0'},
                     None,
                     'medium', None, None,
                     id='MEDIUMN_SIGNED_ATTR_NOT_SET'),
        pytest.param({'atypical-conditions': u'1', 'credit-conditions-verified': u'1',
                      'is-signed': u'1', 'is-signed-dt': u'2021-02-01T00:00:00'},
                     None,
                     'medium', u'Атрибут "Кредитные условия проверены" может быть установлен только бэк-офисом', None,
                     id='MEDIUM_SIGNED_ATTR_SET'),
        pytest.param({'atypical-conditions': u'1', 'credit-conditions-verified': u'0',
                      'is-signed': u'1', 'is-signed-dt': u'2021-02-01T00:00:00'},
                     None,
                     'medium', u'Атрибут "Подписан" может быть установлен только вместе с атрибутом '
                               u'"Кредитные условия проверены" бэк-офисом', None,
                     id='MEDIUM_SIGNED_ATTR_NOT_SET'),
        pytest.param({'atypical-conditions': u'1', 'credit-conditions-verified': u'0'},
                     {'credit-conditions-verified': u'1',
                      'is-signed': u'1', 'is-signed-dt': u'2021-02-01T00:00:00'},
                     'muzzle', None, None,
                     id='MUZZLE_AFTER_SET_SIGNED_SET_ATTR'),
        pytest.param({'atypical-conditions': u'1', 'credit-conditions-verified': u'0'},
                     {'credit-conditions-verified': u'0',
                      'is-signed': u'1', 'is-signed-dt': u'2021-02-01T00:00:00'},
                     'muzzle', None, u'Нельзя установить атрибут "Подписан" без "Кредитные условия проверены"',
                     id='MUZZLE_AFTER_SET_SIGNED_NOT_SET_ATTR'),
    ]
)
def test_credit_conditions_verified(session, params, after_params, servant_type, exc_message, after_exc_message):
    person = PersonBuilder(client=ClientBuilder(name="Test client"),
                           type='ur', operator_uid=0).build(session)

    req = {
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
        'tickets': u'AAA-234'
    }

    req.update(params)
    pg = ContractPage(session, 0, servant_type=servant_type)
    if exc_message:
        with pytest.raises(exc.CONTRACT_RULE_VIOLATION) as exc_info:
            pg.post(req)
        assert exc_message in exc_info.value.msg
    else:
        pg.post(req)
        assert pg.contract_id is not None

    if after_params:
        session.flush()
        req.update(after_params)
        pg = ContractPage(session, pg.contract_id, servant_type=servant_type)

        if after_exc_message:
            with pytest.raises(exc.CONTRACT_RULE_VIOLATION) as exc_info:
                pg.post(req)
            assert after_exc_message in exc_info.value.msg
        else:
            pg.post(req)
