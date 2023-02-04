# -*- coding: utf-8 -*-
import datetime
import xmlrpclib

import mock
import pytest

from balance import muzzle_util as mut
from balance.constants import FirmId
from balance.contractpage import ContractPage
from billing.contract_iface.cmeta import general, spendable
from tests import object_builder as ob
from tests.tutils import get_exception_code

NOW = mut.trunc_date(datetime.datetime.now())
YESTERDAY = NOW - datetime.timedelta(days=1)


def create_contract_or_collateral(session,
                                  w_collateral,
                                  col_type=None,
                                  contract_type='GENERAL',
                                  signed_dt=None,
                                  faxed_dt=None,
                                  sent_dt=None):
    person = ob.PersonBuilder.construct(session, type='ur')
    contract = ob.ContractBuilder.construct(session,
                                            ctype=contract_type,
                                            is_signed=signed_dt,
                                            is_faxed=faxed_dt,
                                            services={637},
                                            sent_dt=sent_dt,
                                            payment_term=12,
                                            person=person,
                                            firm=FirmId.YANDEX_OOO)
    collateral = contract.col0
    if w_collateral:
        contract.append_collateral(dt=datetime.datetime.now(),
                                   collateral_type=col_type or general.collateral_types[10],
                                   is_signed=signed_dt,
                                   is_faxed=faxed_dt,
                                   sent_dt=sent_dt,
                                   )

        collateral = contract.collaterals[1]
    session.flush()
    return collateral


def check_dates(collateral, signed_dt=None, faxed_dt=None, sent_dt=None):
    assert collateral.is_signed == signed_dt
    assert collateral.is_faxed == faxed_dt
    assert collateral.sent_dt == sent_dt


@pytest.mark.parametrize('contract_type', ['GENERAL', 'SPENDABLE'])
@pytest.mark.parametrize('w_collateral', [True, False])
def test_all_dates_contract(xmlrpcserver, session, w_collateral, contract_type):
    """
    Проставляем все три даты в неподписанном договоре/ДС
    """
    passport = ob.PassportBuilder().build(session).obj
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = 1
    if contract_type == 'GENERAL':
        collateral_type = general.collateral_types[10]
    else:
        collateral_type = spendable.collateral_types[7020]
    collateral = create_contract_or_collateral(session, w_collateral, collateral_type, contract_type=contract_type)
    check_dates(collateral, signed_dt=None, faxed_dt=None, sent_dt=None)

    xmlrpcserver.SignCollateral(passport.passport_id, collateral.id, {'signed_dt': NOW.isoformat(),
                                                                      'faxed_dt': NOW.isoformat(),
                                                                      'sent_dt': NOW.isoformat(),
                                                                      })
    assert [attr for attr in collateral.attributes if attr.code == 'SENT_DT'][0].passport_id == passport.passport_id

    check_dates(collateral, signed_dt=NOW, faxed_dt=NOW, sent_dt=NOW)


@pytest.mark.parametrize('params',
                         [
                             {'signed_dt': None,
                              'faxed_dt': NOW.isoformat(),
                              'sent_dt': NOW.isoformat(),
                              },
                             {'signed_dt': NOW.isoformat(),
                              'faxed_dt': None,
                              'sent_dt': NOW.isoformat(),
                              },
                             {'signed_dt': NOW.isoformat(),
                              'faxed_dt': NOW.isoformat(),
                              'sent_dt': None,
                              }
                         ])
@pytest.mark.parametrize('w_collateral', [True, False])
def test_dt_to_none(xmlrpcserver, session, w_collateral, params):
    """
    Пытаемся снять дату подписания в договоре/ДС
    """
    collateral = create_contract_or_collateral(session, w_collateral, signed_dt=NOW, faxed_dt=NOW, sent_dt=NOW)
    check_dates(collateral, signed_dt=NOW, faxed_dt=NOW, sent_dt=NOW)

    with pytest.raises(xmlrpclib.Fault) as exc_info:
        xmlrpcserver.SignCollateral(0, collateral.id, params)
    error_msg = 'Invalid parameter for function: {} must be specified'.format(
        [k for k, v in params.iteritems() if v is None][0])
    assert get_exception_code(exc_info.value, 'msg') == error_msg
    check_dates(collateral, signed_dt=NOW, faxed_dt=NOW, sent_dt=NOW)


@pytest.mark.parametrize('w_collateral', [True, False])
def test_same_dates(xmlrpcserver, session, w_collateral):
    """используем те же даты, ошибки нет"""
    collateral = create_contract_or_collateral(session, w_collateral, signed_dt=NOW, faxed_dt=NOW, sent_dt=NOW)
    check_dates(collateral, signed_dt=NOW, faxed_dt=NOW, sent_dt=NOW)
    xmlrpcserver.SignCollateral(0, collateral.id, {'signed_dt': NOW.isoformat(),
                                                   'faxed_dt': NOW.isoformat(),
                                                   'sent_dt': NOW.isoformat(),
                                                   })
    check_dates(collateral, signed_dt=NOW, faxed_dt=NOW, sent_dt=NOW)


@pytest.mark.parametrize('params',
                         [
                             {'signed_dt': NOW.isoformat(),
                              'faxed_dt': YESTERDAY.isoformat(),
                              'sent_dt': YESTERDAY.isoformat(),
                              },
                             {'signed_dt': YESTERDAY.isoformat(),
                              'faxed_dt': NOW.isoformat(),
                              'sent_dt': YESTERDAY.isoformat(),
                              },
                             {'signed_dt': YESTERDAY.isoformat(),
                              'faxed_dt': YESTERDAY.isoformat(),
                              'sent_dt': NOW.isoformat(),
                              }
                         ])
@pytest.mark.parametrize('w_collateral', [True, False])
def test_update_date(xmlrpcserver, session, w_collateral, params):
    """обновляем какую-нибудь дату"""
    collateral = create_contract_or_collateral(session, w_collateral, signed_dt=YESTERDAY, faxed_dt=YESTERDAY,
                                               sent_dt=YESTERDAY)
    check_dates(collateral, signed_dt=YESTERDAY, faxed_dt=YESTERDAY, sent_dt=YESTERDAY)

    with pytest.raises(xmlrpclib.Fault) as exc_info:
        xmlrpcserver.SignCollateral(0, collateral.id, params)
    error = '{} has already been set, cannot be updated'.format(
        [k for k, v in params.iteritems() if v == NOW.isoformat()][0])
    assert get_exception_code(exc_info.value, 'msg') == 'Invalid parameter for function: {}'.format(error)
    check_dates(collateral, signed_dt=YESTERDAY, faxed_dt=YESTERDAY, sent_dt=YESTERDAY)


@pytest.mark.parametrize('params',
                         [
                             {'signed_dt': NOW.isoformat(),
                              },
                             {'sent_dt': NOW.isoformat(),
                              },
                             {'faxed_dt': NOW.isoformat(),
                              }
                         ])
@pytest.mark.parametrize('w_collateral', [True, False])
def test_sign_contract_attrs_werent_set(xmlrpcserver, session, params, w_collateral):
    """проверяем, что есть ошибка, если параметр не был установлен"""
    collateral = create_contract_or_collateral(session, w_collateral)
    check_dates(collateral, signed_dt=None, faxed_dt=None, sent_dt=None)

    with mock.patch('medium.medium_logic.ContractPage') as contract_page:
        contract_page.return_value.post.return_value = 1
        with pytest.raises(xmlrpclib.Fault) as exc_info:
            xmlrpcserver.SignCollateral(0, collateral.id, params)

    error = '{} has not been set'.format(params.keys()[0])
    assert get_exception_code(exc_info.value, 'msg') == 'Invalid parameter for function: {}'.format(error)


@pytest.mark.parametrize('w_collateral', [
    True,
    False
])
@pytest.mark.parametrize(
    'params',
    [
        {
            "signed_dt": YESTERDAY,
        },
        {
            "signed_dt": YESTERDAY,
            "faxed_dt": YESTERDAY - datetime.timedelta(days=1)
        }
    ]
)
def test_sign_booked(session, xmlrpcserver, w_collateral, params):
    signed_date = params['signed_dt']
    booked_date = signed_date - datetime.timedelta(days=2)

    col = create_contract_or_collateral(session, w_collateral)
    col.is_booked = 1
    col.is_booked_dt = booked_date
    col.is_faxed = booked_date  # проставляется при установке is_booked

    session.flush()

    xmlrpcserver.SignCollateral(
        0, col.id, params
    )

    session.refresh(col)

    assert col.is_signed == signed_date
    assert col.is_faxed == params.get('faxed_dt') or NOW
    assert not col.is_booked
    assert col.is_booked_dt == booked_date


@pytest.mark.parametrize('w_collateral', [
    True,
    False
])
def test_fax_booked(session, xmlrpcserver, w_collateral):
    faxed_date = YESTERDAY
    booked_date = faxed_date - datetime.timedelta(days=1)

    col = create_contract_or_collateral(session, w_collateral)
    col.is_booked = 1
    col.is_booked_dt = booked_date
    col.is_faxed = booked_date  # проставляется при установке is_booked
    session.flush()

    xmlrpcserver.SignCollateral(
        0, col.id, {'faxed_dt': faxed_date}
    )

    session.refresh(col)
    assert col.is_faxed == faxed_date
    assert not col.is_booked
    assert col.is_booked_dt == booked_date


@pytest.mark.parametrize(
    'w_collateral', [
        True,
        False
    ]
)
def test_book_contract_in_ui(session, w_collateral):
    book_date = NOW
    col = create_contract_or_collateral(session, w_collateral)

    attr_prefix = 'col-{}-'.format(col.id) if w_collateral else ''
    params = {
        attr_prefix + 'is-booked': '',
        attr_prefix + 'is-booked-checkpassed': 1,
    }
    if w_collateral:
        params[attr_prefix + 'collateral-form'] = 1

    cp = ContractPage(
        session,
        col.contract2_id,
        servant_type='muzzle',
    )
    cp.post(params)
    session.flush()
    session.refresh(col)

    assert col.is_faxed == book_date
    assert col.is_booked
    assert mut.trunc_date(col.is_booked_dt) == book_date


@pytest.mark.parametrize(
    'w_collateral', [
        True,
        False
    ]
)
def test_unbook_contract_in_ui(session, w_collateral):
    book_date = YESTERDAY

    col = create_contract_or_collateral(session, w_collateral)
    col.is_booked = 1
    col.is_booked_dt = book_date
    col.is_faxed = book_date  # проставляется при установке is_booked
    session.flush()

    attr_prefix = 'col-{}-'.format(col.id) if w_collateral else ''
    params = {
        attr_prefix + 'is-booked': 0,
    }
    if w_collateral:
        params[attr_prefix + 'collateral-form'] = 1

    cp = ContractPage(
        session,
        col.contract2_id,
        servant_type='muzzle',
    )
    cp.post(params)
    session.flush()
    session.refresh(col)

    assert not col.is_booked
    assert mut.trunc_date(col.is_booked_dt) == book_date
    assert col.is_faxed == NOW


@pytest.mark.parametrize('w_collateral', [
    True,
    False
])
def test_sign_faxed(session, xmlrpcserver, w_collateral):
    sign_date = YESTERDAY
    faxed_date = sign_date - datetime.timedelta(days=1)

    col = create_contract_or_collateral(session, w_collateral)
    col.is_faxed = faxed_date
    session.flush()

    xmlrpcserver.SignCollateral(
        0, col.id, {'signed_dt': sign_date}
    )

    session.refresh(col)
    assert col.is_faxed == faxed_date
    assert col.is_signed == sign_date
