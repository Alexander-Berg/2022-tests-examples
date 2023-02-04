# coding=utf-8
import datetime
import pytest

from balance import constants as cst, mapper
from tests import object_builder as ob

POSTPAY_AUTORU_PARAMS = {
    'firm': cst.FirmId.VERTIKALI,
    'services': u'1',
    ('services-%s' % cst.ServiceId.AUTORU): cst.ServiceId.AUTORU,
    'payment-type': cst.POSTPAY_PAYMENT_TYPE,
    'payment_term': 10
}

@pytest.mark.parametrize(
    'contract_params',
    [
        dict(POSTPAY_AUTORU_PARAMS, **{
            'commission': cst.ContractTypeId.NON_AGENCY,
            'valid_template': cst.ContractPrintTpl.AUTORU_DIRECT_1,
            'wrong_template': cst.ContractPrintTpl.AUTORU_DIRECT_2
        }),
        dict(POSTPAY_AUTORU_PARAMS, **{
            'commission': cst.ContractTypeId.DIRECT_AGENCY,
            'valid_template': cst.ContractPrintTpl.AUTORU_DIRECT_2,
            'wrong_template': cst.ContractPrintTpl.AUTORU_DIRECT_1
        })
    ]
)
def test_match_templates(session, xmlrpcserver, contract_params):
    client = ob.ClientBuilder(region_id=cst.RegionId.RUSSIA).build(session).obj
    person = ob.PersonBuilder(client=client).build(session).obj
    manager = ob.SingleManagerBuilder(manager_type=1).build(session).obj
    dt = session.now().replace(microsecond=0)

    params = {
        'client-id': client.id,
        'person-id': person.id,
        'manager_code': manager.manager_code,
        'dt': dt,
        'is-signed-dt': dt.isoformat(),
        'finish-dt': dt + datetime.timedelta(days=10),
    }

    params.update(contract_params)
    res = xmlrpcserver.CreateContract(session.oper_id, params)
    contract = session.query(mapper.Contract).getone(res['ID'])
    state = contract.col0
    tpl = state.print_template
    assert tpl == params['valid_template'], "Print template doesn't match the valid template."
    assert tpl != params['wrong_template'], "Print template matches the wrong template."
