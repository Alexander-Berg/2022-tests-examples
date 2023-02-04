# -*- coding: utf-8 -*-
import datetime
import pytest
from sqlalchemy.testing import emits_warning

from balance import constants as cst, exc
from tests import object_builder as ob
from balance.processors.oebs_api.handlers import ContractExportHandler

MONTH_BEFORE = datetime.datetime.now() - datetime.timedelta(days=30)
TWO_MONTH_BEFORE = (datetime.datetime.now() - datetime.timedelta(days=60)).replace(microsecond=0)
NOW = datetime.datetime.now()
MONTH_LATER = datetime.datetime.now() + datetime.timedelta(days=30)
TWO_MONTH_LATER = datetime.datetime.now() + datetime.timedelta(days=60)


def create_linked_contract(session):
    return ob.ContractBuilder.construct(
        session,
        ctype='GENERAL',
        services={cst.ServiceId.GEOCON},
        dt=TWO_MONTH_BEFORE,
        is_signed=TWO_MONTH_BEFORE,
        firm=1,
    )


def create_contract(session, link_contract_id, linked_contracts=None):
    contract = ob.ContractBuilder.construct(
        session,
        ctype='GENERAL',
        dt=TWO_MONTH_BEFORE,
        is_signed=TWO_MONTH_BEFORE,
        services={cst.ServiceId.TAXI_CORP},
        firm=1,
        link_contract_id=link_contract_id,
        linked_contracts=linked_contracts,
    )
    return contract


@emits_warning(
    "Usage of the 'collection append' operation is not currently "
    "supported within the execution stage of the flush process"
)
@emits_warning(
    "Attribute history events accumulated on 1 previously clean instances "
    "within inner-flush event handlers have been reset, and will not result in database updates"
)
@pytest.mark.parametrize('export_dt', [None, NOW])
def test_exported_depend_on_export_dt(session, export_dt, service_ticket_mock):
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Contract': 1}
    linked_contract_1 = create_linked_contract(session)
    linked_contract_1.exports['OEBS_API'].state = 1
    linked_contract_1.exports['OEBS_API'].export_dt = export_dt
    linked_contract_2 = create_linked_contract(session)
    linked_contract_2.exports['OEBS_API'].state = 1
    contract = create_contract(
        session,
        link_contract_id=linked_contract_1.id,
        linked_contracts={linked_contract_2.id},
    )
    session.flush()
    with pytest.raises(exc.DEFERRED_ERROR) as exc_info:
        ContractExportHandler(contract.exports['OEBS_API']).get_info()
    if export_dt:
        assert exc_info.value.msg == 'Export has been deferred because related Contract {0}' \
                                     ' is not in OEBS.'.format([linked_contract_2.id])
        assert linked_contract_1.exports['OEBS_API'].state == 1
        assert linked_contract_2.exports['OEBS_API'].state == 0

    else:
        assert exc_info.value.msg == 'Export has been deferred because related Contract {0}' \
                                     ' is not in OEBS.'.format([linked_contract_1.id, linked_contract_2.id])
        assert linked_contract_1.exports['OEBS_API'].state == 0
        assert linked_contract_2.exports['OEBS_API'].state == 0


@emits_warning(
    "Usage of the 'collection append' operation is not currently "
    "supported within the execution stage of the flush process"
)
@emits_warning(
    "Attribute history events accumulated on 1 previously clean instances "
    "within inner-flush event handlers have been reset, and will not result in database updates"
)
@pytest.mark.parametrize('other_contract_state', [0, 1])
@pytest.mark.parametrize('failed_state', [2, 666])
def test_one_is_failed(session, other_contract_state, failed_state, service_ticket_mock):
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Contract': 1}
    linked_contract_1 = create_linked_contract(session)
    linked_contract_1.exports['OEBS_API'].state = failed_state
    linked_contract_2 = create_linked_contract(session)
    linked_contract_2.exports['OEBS_API'].state = other_contract_state
    contract = create_contract(
        session,
        link_contract_id=linked_contract_1.id,
        linked_contracts={linked_contract_2.id},
    )
    session.flush()
    with pytest.raises(exc.CRITICAL_ERROR) as exc_info:
        ContractExportHandler(contract.exports['OEBS_API']).get_info()
    assert exc_info.value.msg == 'Export has been failed because related Contract {}' \
                                 ' had been failed or has unknown state.'.format([linked_contract_1.id])
    assert linked_contract_1.exports['OEBS_API'].state == failed_state
    assert linked_contract_2.exports['OEBS_API'].state == 0


@emits_warning(
    "Usage of the 'collection append' operation is not currently "
    "supported within the execution stage of the flush process"
)
@emits_warning(
    "Attribute history events accumulated on 1 previously clean instances "
    "within inner-flush event handlers have been reset, and will not result in database updates"
)
def test_never_exported(session, service_ticket_mock):
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Contract': 0}
    linked_contract_1 = create_linked_contract(session)
    session.flush()
    session.clear_cache()
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Contract': 1}
    contract = create_contract(session,
                               link_contract_id=linked_contract_1.id)
    session.flush()
    session.clear_cache()
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Contract': 1}
    with pytest.raises(exc.DEFERRED_ERROR) as exc_info:
        ContractExportHandler(contract.exports['OEBS_API']).get_info()
    assert exc_info.value.msg == 'Export has been deferred because related Contract {0}' \
                                 ' is not in OEBS.'.format([linked_contract_1.id])
    session.refresh(linked_contract_1)
    assert linked_contract_1.exports['OEBS_API'].state == 0
