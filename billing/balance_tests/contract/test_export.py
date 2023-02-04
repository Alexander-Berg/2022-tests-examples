# -*- coding: utf-8 -*-
import pytest
import datetime
from sqlalchemy.testing import emits_warning

from balance.constants import ClientLinkType
from tests import object_builder as ob

from billing.contract_iface.constants import ContractTypeId
from balance import constants as cst


EXPECTED_QUEUE_NO_OEBS = {
    'CONTRACT_NOTIFY', 'RESUSPENSION', 'OFFER_ACTIVATION', 'NETTING', 'BY_FILE', 'PARTNER_FAST_BALANCE',
    'PARTNER_PROCESSING',
}
EXPECTED_QUEUE_WITH_OEBS = EXPECTED_QUEUE_NO_OEBS | {'OEBS'}
EXPECTED_QUEUE_WITH_OEBS_API = EXPECTED_QUEUE_NO_OEBS | {'OEBS_API'}


@emits_warning(
    "Usage of the 'collection append' operation is not currently "
    "supported within the execution stage of the flush process"
)
@emits_warning(
    "Attribute history events accumulated on 1 previously clean instances "
    "within inner-flush event handlers have been reset, and will not result in database updates"
)
def test_contract_to_oebs_flush(session):
    """При флаше и автоэкпорте, если теперь договор  """
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Contract': 0}

    contract = ob.ContractBuilder.construct(session, ctype='GENERAL', contract_type=2)
    contract.col0.discard_nds = 1
    contract.col0.commission_type = 60
    contract.col0.bank_details_id = 510
    assert contract.exportable == EXPECTED_QUEUE_WITH_OEBS
    assert contract.col0.exportable == EXPECTED_QUEUE_WITH_OEBS
    session.clear_cache()
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Contract': 1}
    contract.col0.discard_nds = 0
    session.flush()
    assert contract.exportable == EXPECTED_QUEUE_WITH_OEBS_API
    assert contract.col0.exportable == EXPECTED_QUEUE_NO_OEBS
    session.flush()


@pytest.mark.parametrize('object_to_export', ['contract', 'collateral'])
def test_contract_to_oebs_export(session, object_to_export):
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Contract': 0}
    contract = ob.ContractBuilder.construct(session, ctype='GENERAL', contract_type=2)
    contract.col0.discard_nds = 1
    contract.col0.commission_type = 60
    contract.col0.bank_details_id = 510
    assert contract.exportable == EXPECTED_QUEUE_WITH_OEBS
    assert contract.col0.exportable == EXPECTED_QUEUE_WITH_OEBS
    session.clear_cache()
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Contract': 1}
    if object_to_export == 'contract':
        contract.export()
    else:
        contract.col0.export()
    assert contract.exportable == EXPECTED_QUEUE_WITH_OEBS_API
    assert contract.col0.exportable == EXPECTED_QUEUE_NO_OEBS
    session.flush()


@emits_warning(
    "Usage of the 'collection append' operation is not currently "
    "supported within the execution stage of the flush process"
)
@emits_warning(
    "Attribute history events accumulated on 1 previously clean instances "
    "within inner-flush event handlers have been reset, and will not result in database updates"
)
@pytest.mark.parametrize('object_to_export', ['contract', 'collateral'])
def test_contract_brand_to_oebs_api_export(session, object_to_export):
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Contract': 1}
    contract = ob.ContractBuilder.construct(
        session,
        ctype='GENERAL',
        commission=ContractTypeId.ADVERTISING_BRAND,
        brand_type=ClientLinkType.DIRECT,
    )
    assert contract.exportable == EXPECTED_QUEUE_NO_OEBS


@pytest.mark.parametrize('w_new_api', [True, False])
@pytest.mark.parametrize('object_to_export', [
    'contract',
    'collateral'
])
def test_distr_child_contract_to_oebs_export(session, object_to_export, w_new_api):
    """Выгружаем дистрибуционный договор с ссылкой на родительский.
    Проверяем, что при экспорте дочернего договора или
    его ДС, в очередь OEBS_API встает только родительский договор."""
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Contract': 0}
    parent_contract = ob.ContractBuilder.construct(session, ctype='DISTRIBUTION', contract_type=2)
    contract = ob.ContractBuilder.construct(session, ctype='DISTRIBUTION', contract_type=2)
    contract.col0.parent_contract_id = parent_contract.id

    contract.exports['OEBS'].state = 1
    contract.col0.exports['OEBS'].state = 1
    parent_contract.exports['OEBS'].state = 1
    parent_contract.col0.exports['OEBS'].state = 1

    assert 'OEBS_API' not in parent_contract.exports
    assert 'OEBS_API' not in contract.exports

    session.clear_cache()
    if w_new_api:
        session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Contract': 1}
    if object_to_export == 'contract':
        contract.export()
    else:
        contract.col0.export()

    if w_new_api:

        assert contract.exportable == EXPECTED_QUEUE_NO_OEBS
        assert contract.exports['OEBS'].state == 1
        assert 'OEBS_API' not in contract.exports

        assert contract.col0.exports['OEBS'].state == 1
        assert 'OEBS_API' not in contract.col0.exports

        assert parent_contract.exports['OEBS'].state == 1
        assert parent_contract.exports['OEBS_API'].state == 0

        assert parent_contract.col0.exports['OEBS'].state == 1
        assert 'OEBS_API' not in parent_contract.col0.exports

    else:
        assert contract.exports['OEBS'].state == 0
        assert 'OEBS_API' not in contract.exports
        if object_to_export == 'contract':
            assert contract.col0.exports['OEBS'].state == 1
        else:
            assert contract.col0.exports['OEBS'].state == 0
        assert 'OEBS_API' not in contract.col0.exports

        assert parent_contract.exports['OEBS'].state == 1
        assert 'OEBS_API' not in parent_contract.exports

        assert parent_contract.col0.exports['OEBS'].state == 1
        assert 'OEBS_API' not in parent_contract.col0.exports


@pytest.mark.parametrize('w_new_api', [True, False])
@pytest.mark.parametrize('object_to_export', ['contract', 'collateral'])
def test_distr_contract_to_oebs_export(session, object_to_export, w_new_api):
    """Выгружаем родительский дистрибуционный договор."""
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Contract': 0}
    parent_contract = ob.ContractBuilder.construct(session, ctype='DISTRIBUTION', contract_type=2)
    contract = ob.ContractBuilder.construct(session, ctype='DISTRIBUTION', contract_type=2)
    contract.col0.parent_contract_id = parent_contract.id

    parent_contract.exports['OEBS'].state = 1
    parent_contract.col0.exports['OEBS'].state = 1

    assert 'OEBS_API' not in parent_contract.exports
    assert 'OEBS_API' not in contract.exports
    session.clear_cache()
    if w_new_api:
        session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Contract': 1}
    if object_to_export == 'contract':
        parent_contract.export()
    else:
        parent_contract.col0.export()

    if w_new_api:
        assert parent_contract.exports['OEBS'].state == 1
        assert parent_contract.exports['OEBS_API'].state == 0

        assert parent_contract.col0.exports['OEBS'].state == 1
        assert 'OEBS_API' not in parent_contract.col0.exports

    else:
        assert parent_contract.exports['OEBS'].state == 0
        assert 'OEBS_API' not in parent_contract.exports

        if object_to_export == 'contract':
            assert parent_contract.col0.exports['OEBS'].state == 1
        else:
            assert parent_contract.col0.exports['OEBS'].state == 0
        assert 'OEBS_API' not in parent_contract.col0.exports


@emits_warning(
    "Usage of the 'collection append' operation is not currently "
    "supported within the execution stage of the flush process"
)
@emits_warning(
    "Attribute history events accumulated on 1 previously clean instances "
    "within inner-flush event handlers have been reset, and will not result in database updates"
)
def test_not_export_after_daily_state_update(session):
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Contract': 1}
    contract = ob.ContractBuilder.construct(
        session,
        ctype='PARTNERS',
        contract_type=2,
        daily_state=datetime.datetime.now(),
        memo='my obj',  # fixme: remove after debugging
    )
    contract.exports['OEBS_API'].state = 1
    contract.daily_state = datetime.datetime.now() + datetime.timedelta(days=1)
    session.flush()
    assert contract.exports['OEBS_API'].state == 1
