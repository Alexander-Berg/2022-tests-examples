# -*- coding: utf-8 -*-

import datetime as dt

import pytest
import mock

from autodasha.core.logic import partner_acts
from balance import mapper
from tests.autodasha_tests.common import db_utils


def to_dt(str_date):
    return dt.datetime.strptime(str_date, '%Y-%m-%d')


@pytest.fixture(autouse=True)
def patch_gen():
    partner_acts.get_generator = mock.Mock()


@pytest.fixture()
def contract(session, request):
    client, person = db_utils.create_client_person(session)
    if request.param == 'spendable_calc':
        return db_utils.create_spendable_contract(session, client, person, services=[207])
    if request.param == 'spendable':
        return db_utils.create_spendable_contract(session, client, person, services=[0])
    if request.param == 'distribution':
        return db_utils.create_distr_contract(session, client, person)
    if request.param == 'distribution_parent':
        return db_utils.create_distr_contract(session, client, person, contract_type=5)
    if request.param == 'partners':
        return db_utils.create_partners_contract(session, client, person)


@pytest.mark.parametrize(
    ['start_dt', 'finish_dt', 'req_res'],
    [
        [
            to_dt('2018-01-01'),
            to_dt('2018-03-01'),
            ['ActMonth(2018.1)', 'ActMonth(2018.2)', 'ActMonth(2018.3)']
        ],
        [
            to_dt('2018-01-01'),
            to_dt('2018-01-01'),
            ['ActMonth(2018.1)']
        ],
        [
            to_dt('2018-01-01'),
            to_dt('2017-12-01'),
            []
        ],
        [
            to_dt('2018-01-26'),
            to_dt('2018-02-03'),
            ['ActMonth(2018.1)', 'ActMonth(2018.2)']
        ]
    ]
)
def test_generate_months(start_dt, finish_dt, req_res):
    res = partner_acts.generate_months(start_dt, finish_dt)
    res = map(lambda m_: str(m_), res)
    assert set(res) == set(req_res)


@pytest.mark.parametrize(
    'contract, is_called',
    [
        ('spendable_calc', True),
        ('spendable', False),
        ('distribution', True),
        ('distribution_parent', False)
    ],
    indirect=['contract']
)
def test_calc_called(contract, is_called):
    with mock.patch('autodasha.core.logic.partner_acts.ContractCalculator') as mock_cc:
        partner_acts.recalculate_reward(contract, [mapper.ActMonth()], [])
        assert mock_cc.called is is_called


@pytest.mark.parametrize(
    'contract',
    [
        'partners',
    ],
    indirect=['contract']
)
def test_off_calculating_pages(contract, config):
    session = contract.session
    off_calculating_pages = config.CHANGE_CONTRACT.get('off_calculating_pages')
    start_dt, finish_dt = to_dt('2020-01-01'), to_dt('2020-01-01')
    months = partner_acts.generate_months(start_dt, finish_dt)
    pages = off_calculating_pages[:]
    pages.append(666)
    for page_id in pages:
        pad_row = mapper.PartnerActData(**dict(
            partner_contract_id=contract.id,
            dt=start_dt,
            end_dt=finish_dt,
            nds=0,
            partner_reward_wo_nds=100,
            currency=810,
            iso_currency=643,
            status=0,
            page_id=page_id
        ))
        session.add(pad_row)
        session.flush()
    partner_acts.recalculate_reward(contract, months, off_calculating_pages)
    session.flush()
    pads = session.query(mapper.PartnerActData).filter_by(partner_contract_id=contract.id).all()
    assert len(pads) == len(off_calculating_pages)
    pad_pages = [pad.page_id for pad in pads]
    assert set(pad_pages) == set(off_calculating_pages)
