# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import uuid
import os

import pytest

from balance import muzzle_util as ut
from balance.application import getApplication, Application
from balance.constants import BALANCE_DATABASE_ID, META_DATABASE_ID

from autodasha.db import mapper as a_mapper

from tests.autodasha_tests.common import TestConfig, staff_utils
from tests import test_application

import mock_utils
from case_utils import get_issue


class TestAttachment(object):
    def __init__(self, name, contents):
        self.name = name
        self._contents = contents

    def read(self):
        return [self._contents]


@pytest.fixture(scope='session')
def app():
    os.environ['NLS_LANG'] = 'AMERICAN_CIS.UTF8'
    os.environ['NLS_NUMERIC_CHARACTERS'] = '. '
    try:
        return getApplication()
    except RuntimeError:
        return test_application.ApplicationForTests()


@pytest.fixture
def session(app):
    dbhelper = app.get_new_dbhelper(cfg=app.cfg, database_id=BALANCE_DATABASE_ID)
    dbhelper.transactional = True
    dbhelper.update_selectors()
    session_ = dbhelper.create_session(oper_id=app.cfg.findtext('AutodashaSettings/OperID'))
    session_.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {}
    session_.config.__dict__['PROMO_USE_ORDERLESS_SERVICES'] = [129, 642]

    yield session_

    session_.rollback()


@pytest.fixture
def meta_session(app):
    dbhelper = app.get_new_dbhelper(cfg=app.cfg, database_id=META_DATABASE_ID)
    dbhelper.transactional = True
    dbhelper.update_selectors()
    session_ = dbhelper.create_session(oper_id=app.cfg.findtext('AutodashaSettings/OperID'))

    yield session_

    session_.rollback()


@pytest.fixture
def config(session, app):
    items = session.query(a_mapper.TblConfig).all()
    items = dict((item.item, item.value) for item in items)
    items['responsible_manager'] = 'mscnad7'
    items['support_logins'] = {'autodasha', 'lazareva', app.cfg.findtext('AutodashaSettings/SelfLogin'), 'mscnad7'}
    items['payment_invoice_logins'] = {'ashul', 'nevskiy'}

    items['summoned_accountants'] = {'accountant'}
    items['accountants_logins'] = {'bahira', 'accountant', 'noob_accountant'}
    items['accountants_departments'] = {'lala', }

    items['summoned_treasurers'] = {'treasurer'}
    items['treasurers_logins'] = {'treasurer', 'noob_treasurer'}

    items['contract_id_picture_link'] = 'https://yandex.ru/images/'
    items['CERTIFICATES_APPROVERS'] = {
        "7": ["direct_approver1", "direct_approver2"],
        "70": ["media_approver1"]
    }
    items['solvers'] = {
        'AddSetMarkup',
        'AvgTurnover',
        'BadDebt',
        'BlacklistedUsers',
        'BuhDelete',
        'CertConsume',
        'ChangePerson',
        'ChangeContract',
        'ExportContract',
        'GoodDebt',
        'KopeechkiSolver',
        'Promo',
        'PromoConnect',
        'RemoveGoodDebt',
        'RunClientBatch',
        'ReturnCompleted',
        'ReturnOrderless',
        'ReturnReceipt',
        'TransferOrder',
        'UnhidePerson',
        'TaxiActDetalisation',
        'CreateSettingsFirm',
        'CreateSettingsService',
        'FraudCleaner',
        'ManualDocsRemovingSolver',
        'ContractDetalisation',
        'CloseInvoice35',
        'CreateCloseInvoice35',
        'ReverseInvoice',
        'SemiAutoDatabaseModifySolver',
        'Contract3070InvoiceTurnOnSolver',
        'TransferClientCashbackSolver',
        'PromoChangeSolver'
    }
    items['top_departments'] = ['yandex']
    items['approvers_settings'] = staff_utils.create_approvers_settings()
    items['absence_check_steps'] = 2
    items['check_absence'] = 1
    items['MANUAL_DOCS_REMOVING_SETTINGS'] = {'support_manager': 'ashul'}
    items['CLOSE_INVOICE_35_SETTINGS'] = {'support_manager': 'barsukovpt',
                                          'support_developer': 'robot-octopool'}
    items['SEMI_AUTO_DATABASE_MODIFY'] = {'support_approvers': ['truba', 'ashul'],
                                          'primary_managers': ['arkasha_primary']}
    items['CHANGE_CONTRACT'] = {'off_calculating_pages': [100125, 100126]}
    items['check_outdated'] = 1
    items['promo'] = {
        'need_approve': True,
        'support_manager': 'electricsheep',
        'responsible_manager': 'electricsheep',
        'currency_product_map': {
            'RUB': '503162',
            'USD': ['503163', '508588'],
            'EUR': '503164',
            'FISH': '1475',
            'USZ': '1475'
        }
    }
    items['ALLOWED_PROMO_BONUS_ERROR_PCT'] = 0.5
    items['REVERSE_INVOICE_MAX_FILE_LENGTHS'] = None
    items['TRANSFER_CLIENT_CASHBACK_SETTINGS'] = {'allowed_services': {7}}
    items['ALLOW_TO_ERASE_PROMO_FOR_REQUEST'] = 1
    config_ = TestConfig()
    config_._items = items
    return config_


def _extract_comment(row_):
    if isinstance(row_, dict):
        return row_

    if len(row_) == 2:
        author, txt = row_
        dt_ = None
    elif len(row_) == 3:
        author, txt, dt_ = row_
    else:
        raise ValueError('Incorrect number or values in get_comments')

    return {'author': author, 'text': txt, 'dt': dt_}


@pytest.fixture
def mock_config():
    items = {
        'responsible_manager': 'mscnad7',
        'approvers_settings': staff_utils.create_approvers_settings(),
        'payment_invoice_logins': {'ashul', 'nevskiy'},
        'contract_id_picture_link': 'https://yandex.ru/images/',
        'BLACKLISTED_USERS_SETTINGS': dict(),
        'FRAUD_CLEANER_SETTINGS': dict(),
        'MANUAL_DOCS_REMOVING_SETTINGS': dict(),
        'CLOSE_INVOICE_35_SETTINGS': dict(),
        'SEMI_AUTO_DATABASE_MODIFY':  {'support_approvers': ['truba', 'ashul'],
                                        'primary_managers': ['arkasha_primary']},
        'CHANGE_CONTRACT': dict(),
        'REVERSE_INVOICE_MAX_FILE_LENGTHS': None,
        'TRANSFER_CLIENT_CASHBACK_SETTINGS': {'allowed_services': {7}},
        'CERTIFICATES_SERVICE_MAP': {
            'Баян (77-)': 77,
            'Директ Рекламные кампании (7-)': 7,
            'Медиаселлинг (70-)': 70,
            'Яндекс Баннерокрутилка (67-)': 67,
        },
    }
    config_ = TestConfig()
    config_._items = items
    return config_


@pytest.fixture
def mock_manager():
    return mock_utils.MockObjectManager()


@pytest.fixture
def mock_issue_data(mock_config, mock_manager, request):
    case = request.param
    summary, lines = case.get_data(mock_manager)

    issue = get_issue()
    issue.summary = summary
    issue.description = '\n'.join(lines)
    issue.comments = map(_extract_comment, case.get_comments())
    issue.last_resolved = case.last_resolved

    issue.author = case.author

    return issue, case


@pytest.fixture
def mock_queue_object(mock_manager):
    session = mock_manager.construct_session()
    return ut.Struct(issue=ut.Struct(session=session), session=session)


@pytest.fixture
def issue_data(session, config, request):
    case = request.param

    case.setup_config(session, config)

    id_ = uuid.uuid4().hex
    db_issue = a_mapper.Issue(id=id_, key=case.issue_key or id_)
    session.add(db_issue)
    queue_object = db_issue.enqueue('SOLVER')

    st_issue = get_issue()
    st_issue.summary = case.summary
    st_issue.description = case.get_description(session)
    st_issue.status = case.status
    st_issue.resolution = case.resolution
    st_issue.author = case.author
    st_issue.key = case.issue_key
    st_issue.dt = case.issue_dt
    st_issue.assignee = case.assignee
    st_issue.last_resolved = case.last_resolved
    st_issue.followers = case.followers

    st_issue.comments = map(_extract_comment, case.get_comments())
    st_issue.attachments = map(lambda row: TestAttachment(*row), case.get_attachments())

    session.flush()
    session.expire_all()

    return queue_object, st_issue, case


# preserve session for autodasha-specific staff
# meta session for case data
@pytest.fixture
def issue_data_meta(session, meta_session, config, request):
    case = request.param

    case.setup_config(session, config)

    id_ = uuid.uuid4().hex
    db_issue = a_mapper.Issue(id=id_, key=case.issue_key or id_)
    session.add(db_issue)
    queue_object = db_issue.enqueue('SOLVER')

    st_issue = get_issue()
    st_issue.summary = case.summary
    st_issue.description = case.get_description(meta_session)
    st_issue.status = case.status
    st_issue.resolution = case.resolution
    st_issue.author = case.author
    st_issue.key = case.issue_key
    st_issue.dt = case.issue_dt
    st_issue.assignee = case.assignee
    st_issue.last_resolved = case.last_resolved

    st_issue.comments = map(_extract_comment, case.get_comments())
    st_issue.attachments = map(lambda row: TestAttachment(*row), case.get_attachments())

    session.flush()
    session.expire_all()

    meta_session.flush()
    meta_session.expire_all()

    return queue_object, st_issue, case
