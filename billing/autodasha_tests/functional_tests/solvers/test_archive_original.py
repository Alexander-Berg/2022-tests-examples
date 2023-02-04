# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import StringIO
import datetime as dt
import functools
import itertools


import pytest
import mock
from sqlalchemy import orm

from balance import mapper
from balance.xls_export import get_excel_document
from balance import muzzle_util as ut

from autodasha.solver_cl import ArchiveOriginal
from autodasha.core.api.tracker import IssueTransitions
from autodasha.db import mapper as a_mapper

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils
from tests.autodasha_tests.common import db_utils, staff_utils
import random


def get_approve_message(*args, **kwargs):
    return 'Нажми кнопку "Подтвердить" или познай мощь Ктулху!' \
           ' https://jing.yandex-team.ru/files/autodasha/approve_button_something.png'


class RequiredResult(case_utils.RequiredResult):
    _comments = {
        'already_solved':
            'Эта задача уже была выполнена. Направьте новый запрос через '
            '((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ формы)). '
            'Если не найдёте подходящую, заполните, пожалуйста, общую форму.',
        'empty_file': 'В файле нет ни одной записи. Заполни, пожалуйста, форму еще раз, приложив корректный файл.',
        'incorrect_caption': 'В приложенном файле неверно указаны поля шапки. '
                             'Заполни, пожалуйста, форму еще раз, приложив корректный файл.',
        'incorrect_flag': 'Способ проставления отметки не заполнен или указан неверно. '
                          'Заполни, пожалуйста, форму еще раз, указав верное значение.',
        'incorrect_action': 'Поле "Что требуется сделать" не заполнено или указано неверно. '
                            'Заполни, пожалуйста, форму еще раз, указав верное значение.',
        'incorrect_date': 'Дата проставления отметки не заполнена или указана неверно. '
                          'Заполни, пожалуйста, форму еще раз, указав верное значение.',
        'incorrect_file_extention': 'Тип файла должен быть xlsx. Заполни, пожалуйста, форму еще раз.',
        'no_file': 'Не приложен файл. Заполни, пожалуйста, форму еще раз, приложив корректный файл.',
        'many_files': 'Приложено больше одного файла. Заполни, пожалуйста, форму еще раз, приложив корректный файл.',
        'archive': 'Требуется проставить галку "Принят в архив", лучше назначить на разработчиков.',
        'many_partner_contracts': 'Потребуется выгрузить большое количество партнерских договоров. '
                                  'Необходимо согласовать с коллегами.',
        'result_contracts': 'Выполнено, отметка Отправлен оригинал проставлена. '
                            'Проверь, пожалуйста, отчёт на наличие ошибок и проставь отметки, там где есть ошибки, вручную.',
        'no_approval': 'Нет подтверждения.'
                       ' Пожалуйста, уточни данные и напиши новую задачу через форму, если необходимо.',
        'enqueued': 'Изменённые договоры и ДС добавлены в очередь на выгрузку в ОЕБС.',
    }

    def __init__(self, summonees=None, num_comments=1, **kwargs):
        self.contract_states = []
        self.summonees = summonees
        self.num_comments = num_comments
        self.reexported_contracts = []
        self.reexported_collaterals = []
        self.commit = kwargs.get('commit', True)

        super(RequiredResult, self).__init__(**kwargs)
        self.delay = kwargs.get('delay', self.transition == IssueTransitions.none)

    def set_object_states(self, **kwargs):
        self.contract_states = kwargs.get('contract_states', [])
        self.add_result(*kwargs.get('result', []))

    def set_messages(self, result=None, changed_data=[], enqueue=False, **kwargs):
        super(RequiredResult, self).set_messages(**kwargs)
        if kwargs.get('empty_file'):
            self.add_message(self._comments['empty_file'])
        if kwargs.get('many_files'):
            self.add_message(self._comments['many_files'])
        if kwargs.get('no_file'):
            self.add_message(self._comments['no_file'])
        if kwargs.get('wrong_file_format'):
            self.add_message(self._comments['incorrect_file_extention'])
        if kwargs.get('incorrect_action'):
            self.add_message(self._comments['incorrect_action'])
        if kwargs.get('incorrect_date'):
            self.add_message(self._comments['incorrect_date'])
        if kwargs.get('incorrect_flag'):
            self.add_message(self._comments['incorrect_flag'])
        if kwargs.get('incorrect_caption'):
            self.add_message(self._comments['incorrect_caption'])
        if kwargs.get('many_partners'):
            self.add_message(self._comments['many_partner_contracts'])
        if result:
            self.add_message(self._comments['result_contracts'])
        if enqueue:
            self.add_message(self._comments['enqueued'])

        if changed_data:
            self.add_changed_contract_state(*changed_data)

    def add_changed_contract_state(self, contracts, collaterals):
        self.reexported_contracts = contracts
        self.reexported_collaterals = collaterals

    def add_result(self, *args):
        self.contract_states.extend((a, 1) for a in args)


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    _summary = 'Принят в архив/Отправлен оригинал'
    _description = """
Что требуется сделать: {method}
Дата: {set_date}
Если галка стоит, меняем дату на указанную: {update_flg}
Приложенный файл: {file_path}
    """

    def __init__(self):
        self._contract = None
        self._client = None
        self._person = None

    @staticmethod
    def _get_file(contracts, bad_contracts=[]):
        buffer = StringIO.StringIO()

        titles = ['Договор', 'ДС']

        def get_row(c):
            return c.external_id, c.col0.num

        def get_bad_row(c):
            return c['external_id'], c['col0']['num']

        buffer.write(get_excel_document(titles, map(get_row, contracts) + map(get_bad_row, bad_contracts), 0))
        return buffer.getvalue()

    def get_description(self, session):
        method, set_date, update_flg, file_path = self._get_data(session)
        return self._description.format(
                method=method,
                set_date=set_date,
                update_flg=update_flg,
                file_path=file_path
        )

    @staticmethod
    def _get_fake_contract(eid, num):
        return ut.Struct(
            external_id=eid,
            dt=dt.datetime.now(),
            col=ut.Struct(num='01')
        )

    def _init_objects(self, session):
        self._client, self._person = db_utils.create_client_person(session)

    def _get_contract(self, session, type=None, external_id=None):
        if not self._client:
            self._init_objects(session)
        ctype_methods = ['create_general_contract', 'create_partners_contract', 'create_distr_contract']
        if not type:
            contract = getattr(db_utils, random.choice(ctype_methods))(session, self._client, self._person,
                                                                       on_dt=dt.datetime(2018, 1, 1))
        else:
            contract = getattr(db_utils, 'create_{}_contract'.format(type))(session, self._client, self._person,
                                                                            on_dt=dt.datetime(2018, 1, 1))
        return contract

    def _set_sent_dt(self, contract, col=None):
        if col:
            col.sent_dt = dt.datetime.now()
        else:
            contract.col0.sent_dt = dt.datetime.now()


class RequestConfirmationCase(AbstractDBTestCase):
    _representation = 'request_confirmation'

    def __init__(self):
        super(RequestConfirmationCase, self).__init__()
        self._contract = None
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = dt.datetime.now().strftime('%Y-%m-%d')
        update_flg = 'Да'
        file_path = 'http://say-my-name.xls'

        contract1 = self._get_contract(session)
        self._set_sent_dt(contract1)

        contract2 = self._get_contract(session)
        self._set_sent_dt(contract2)

        self._contracts = [contract1, contract2]
        session.flush()
        return method, date, update_flg, file_path

    author = 'some_rick'

    def get_attachments(self):
        return [('data0.xlsx', self._get_file(self._contracts))]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none,
                             summonees=['other_rick'],
                             messages=[get_approve_message()])
        return res


class RequestConfirmationAbsentCase(AbstractDBTestCase):
    _representation = 'request_confirmation_absent'

    def __init__(self):
        super(RequestConfirmationAbsentCase, self).__init__()
        self._contract = None
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = dt.datetime.now().strftime('%Y-%m-%d')
        update_flg = 'yes'
        file_path = 'http://say-my-name.xls'

        contract1 = self._get_contract(session)
        self._set_sent_dt(contract1)

        contract2 = self._get_contract(session)
        self._set_sent_dt(contract2)

        self._contracts = [contract1, contract2]
        session.flush()
        return method, date, update_flg, file_path

    author = 'some_rick'

    def get_attachments(self):
        return [('data0.xlsx', self._get_file(self._contracts))]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none,
                             summonees=['other_rick', 'other_boss'],
                             messages=[get_approve_message()])
        return res


class WrongApproverCase(AbstractDBTestCase):
    _representation = 'wrong_approver'

    def __init__(self):
        super(WrongApproverCase, self).__init__()
        self._contract = None
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = dt.datetime.now().strftime('%Y-%m-%d')
        update_flg = 'true'
        file_path = 'http://say-my-name.xls'

        contract1 = self._get_contract(session)
        self._set_sent_dt(contract1)

        contract2 = self._get_contract(session)
        self._set_sent_dt(contract2)

        self._contracts = [contract1, contract2]
        session.flush()
        return method, date, update_flg, file_path

    author = 'some_rick'

    def get_attachments(self):
        return [('data0.xlsx', self._get_file(self._contracts))]

    def get_comments(self):
        return [('needed_rick', 'Подтверждено', dt.datetime.now())]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none,
                             summonees=['other_rick'],
                             messages=['Подтверждение ожидаем от other_rick.', get_approve_message()])
        return res


class WrongApproverAlreadyWarnedCase(AbstractDBTestCase):
    _representation = 'wrong_approver_already_warned'

    def __init__(self):
        super(WrongApproverAlreadyWarnedCase, self).__init__()
        self._contract = None
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = dt.datetime.now().strftime('%Y-%m-%d')
        update_flg = 'Да'
        file_path = 'http://say-my-name.xls'

        contract1 = self._get_contract(session)
        self._set_sent_dt(contract1)

        contract2 = self._get_contract(session)
        self._set_sent_dt(contract2)

        self._contracts = [contract1, contract2]
        session.flush()
        return method, date, update_flg, file_path

    author = 'some_rick'

    def get_attachments(self):
        return [('data0.xlsx', self._get_file(self._contracts))]

    def get_comments(self):
        return [('needed_rick', 'Подтверждено', dt.datetime.now()),
                ('autodasha', 'Подтверждение ожидаем от ', dt.datetime.now()),]

    def get_result(self):
        return None


class ConfirmedCase(AbstractDBTestCase):
    _representation = 'confirmed'

    def __init__(self):
        super(ConfirmedCase, self).__init__()
        self._contracts = None
        self._collaterals = None
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = dt.datetime.now().strftime('%Y-%m-%d')
        update_flg = 'Да'
        file_path = 'http://say-my-name.xls'

        contract1 = self._get_contract(session, external_id='1234321')
        self._set_sent_dt(contract1)

        contract2 = self._get_contract(session)
        self._set_sent_dt(contract2)

        self._contracts = [contract1, contract2]
        self._collaterals = [c.col0 for c in self._contracts]
        session.flush()
        return method, date, update_flg, file_path

    author = 'some_rick'

    def get_attachments(self):
        return [('data0.xlsx', self._get_file(self._contracts))]

    def get_comments(self):
        return [('other_rick', 'Подтверждено', dt.datetime.now())]

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.none, result=self._contracts,
                              changed_data=(self._contracts, self._collaterals),
                              enqueue=True)


class WaitingCase(AbstractDBTestCase):
    _representation = 'waiting'

    def __init__(self):
        super(WaitingCase, self).__init__()
        self._contract = None
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = dt.datetime.now().strftime('%Y-%m-%d')
        update_flg = 'Да'
        file_path = 'http://say-my-name.xls'

        contract1 = self._get_contract(session)
        self._set_sent_dt(contract1)

        contract2 = self._get_contract(session)
        self._set_sent_dt(contract2)

        self._contracts = [contract1, contract2]
        session.flush()
        return method, date, update_flg, file_path

    author = 'some_rick'

    def get_attachments(self):
        return [('data0.xlsx', self._get_file(self._contracts))]


    def get_comments(self):
        return [('autodasha', 'Ждем!', dt.datetime.now())]

    def get_result(self):
        return None


class WaitingTooLongCase(AbstractDBTestCase):
    _representation = 'waiting_too_long'

    def __init__(self):
        super(WaitingTooLongCase, self).__init__()
        self._contract = None
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = dt.datetime.now().strftime('%Y-%m-%d')
        update_flg = 'Да'
        file_path = 'http://say-my-name.xls'

        contract1 = self._get_contract(session)
        self._set_sent_dt(contract1)

        contract2 = self._get_contract(session)
        self._set_sent_dt(contract2)

        self._contracts = [contract1, contract2]
        session.flush()
        return method, date, update_flg, file_path

    author = 'some_rick'

    def get_attachments(self):
        return [('data0.xlsx', self._get_file(self._contracts))]

    def get_comments(self):
        return [('autodasha', 'Подтверждай, уже!', dt.datetime.now() - dt.timedelta(22))]

    def get_result(self):
        msg = 'Нет подтверждения. ' \
              'Пожалуйста, уточни данные и напиши новую задачу через форму, если необходимо.'
        return RequiredResult(messages=[msg], transition=IssueTransitions.wont_fix)


class FromApproverCase(AbstractDBTestCase):
    _representation = 'from_approver'

    def __init__(self):
        super(FromApproverCase, self).__init__()
        self._contracts = []
        self._collaterals = []
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = dt.datetime.now().strftime('%Y-%m-%d')
        update_flg = 'yes'
        file_path = 'http://say-my-name.xls'

        contract1 = self._get_contract(session)
        self._set_sent_dt(contract1)

        contract2 = self._get_contract(session)
        self._set_sent_dt(contract2)

        self._contracts = [contract1, contract2]
        self._collaterals = [c.col0 for c in self._contracts]
        session.flush()
        return method, date, update_flg, file_path

    author = 'other_rick'

    def get_attachments(self):
        return [('data0.xlsx', self._get_file(self._contracts))]

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.none, result=self._contracts,
                              changed_data=(self._contracts, self._collaterals), enqueue=True)


class NoFileCase(AbstractDBTestCase):
    _representation = 'no_file'

    def __init__(self):
        super(NoFileCase, self).__init__()
        self._contract = None
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = dt.datetime.now().strftime('%Y-%m-%d')
        update_flg = 'Да'
        file_path = ''

        return method, date, update_flg, file_path

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.wont_fix,
                              no_file=True)

    def get_attachments(self):
        return []


class IncorrectCaptionCase(AbstractDBTestCase):
    _representation = 'incorrect_caption'

    def __init__(self):
        super(IncorrectCaptionCase, self).__init__()
        self._contract = None
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = dt.datetime.now().strftime('%Y-%m-%d')
        update_flg = 'Да'
        file_path = 'http://say-my-name.xls'

        contract1 = self._get_contract(session)
        contract2 = self._get_contract(session)
        self._contracts = [contract1, contract2]
        session.flush()

        return method, date, update_flg, file_path

    @staticmethod
    def _get_file(contracts):
        buffer = StringIO.StringIO()

        titles = ['Дог.', 'дс']

        def get_row(c):
            return c.external_id, c.col0.num

        buffer.write(get_excel_document(titles, map(get_row, contracts), 0))
        return buffer.getvalue()

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.wont_fix,
                              incorrect_caption=True)

    def get_attachments(self):
        return [('data.xlsx', self._get_file(self._contracts))]


class EmptyFileCase(AbstractDBTestCase):
    _representation = 'empty_file'

    def __init__(self):
        super(EmptyFileCase, self).__init__()
        self._contract = None
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = dt.datetime.now().strftime('%Y-%m-%d')
        update_flg = 'Да'
        file_path = 'http://say-my-name.xls'

        contract1 = self._get_contract(session)
        contract2 = self._get_contract(session)
        self._contracts = [contract1, contract2]
        session.flush()

        return method, date, update_flg, file_path

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.wont_fix,
                              empty_file=True)

    def get_attachments(self):
        return [('data.xlsx', self._get_file([]))]


class WrongFileTypeCase(AbstractDBTestCase):
    _representation = 'wrong_file_type'

    def __init__(self):
        super(WrongFileTypeCase, self).__init__()
        self._contract = None
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = dt.datetime.now().strftime('%Y-%m-%d')
        update_flg = 'Да'
        file_path = 'http://say-my-name.xls'

        return method, date, update_flg, file_path

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.wont_fix,
                              wrong_file_format=True)

    def get_attachments(self):
        return [('data0.xls', 'res')]


class ManyFilesCase(AbstractDBTestCase):
    _representation = 'many_files'

    def __init__(self):
        super(ManyFilesCase, self).__init__()
        self._contract = None
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = dt.datetime.now().strftime('%Y-%m-%d')
        update_flg = 'Да'
        file_path = 'http://say-my-name.xls'

        return method, date, update_flg, file_path

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.wont_fix,
                              many_files=True)

    def get_attachments(self):
        return [('data0.xlsx', 'res'), ('data2.xlsx', 'data')]


class IncorrectActionCase(AbstractDBTestCase):
    _representation = 'incorrect_action'

    def __init__(self):
        super(IncorrectActionCase, self).__init__()
        self._contract = None
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Поесть гречки'
        date = dt.datetime.now().strftime('%Y-%m-%d')
        update_flg = 'Да'
        file_path = 'http://say-my-name.xls'

        return method, date, update_flg, file_path

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.wont_fix,
                              incorrect_action=True)

    def get_attachments(self):
        return [('data0.xlsx', 'res')]


class IncorrectDateCase(AbstractDBTestCase):
    _representation = 'incorrect_date'

    def __init__(self):
        super(IncorrectDateCase, self).__init__()
        self._contract = None
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = '2018-13-20'
        update_flg = 'Да'
        file_path = 'http://say-my-name.xls'

        return method, date, update_flg, file_path

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.wont_fix,
                              incorrect_date=True)

    def get_attachments(self):
        return [('data0.xlsx', 'res')]


class NoDateCase(AbstractDBTestCase):
    _representation = 'no_date'

    def __init__(self):
        super(NoDateCase, self).__init__()
        self._contract = None
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = ''
        update_flg = 'Да'
        file_path = 'http://say-my-name.xls'

        return method, date, update_flg, file_path

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.wont_fix,
                              incorrect_date=True)

    def get_attachments(self):
        return [('data0.xlsx', 'res')]


class IncorrectFlagCase(AbstractDBTestCase):
    _representation = 'incorrect_flag'

    def __init__(self):
        super(IncorrectFlagCase, self).__init__()
        self._contract = None
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = '2018-10-10'
        update_flg = 'Лурдес'
        file_path = 'http://say-my-name.xls'

        return method, date, update_flg, file_path

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.wont_fix,
                              incorrect_flag=True)

    def get_attachments(self):
        return [('data0.xlsx', 'res')]


class ArchiveCase(AbstractDBTestCase):
    _representation = 'archive'

    def __init__(self):
        super(ArchiveCase, self).__init__()
        self._contract = None
        self._client = None
        self._person = None

    author = 'some_rick'

    def get_comments(self):
        return [('other_rick', 'Подтверждено', dt.datetime.now())]

    def _get_data(self, session):
        method = 'Проставить галку "Принят в архив"'
        date = '2018-10-10'
        update_flg = 'Нет'
        file_path = 'http://say-my-name.xls'

        contract1 = self._get_contract(session)
        contract2 = self._get_contract(session)
        self._contracts = [contract1, contract2]
        session.flush()

        return method, date, update_flg, file_path

    def get_result(self):
        msg = 'Требуется проставить галку "Принят в архив", лучше назначить на разработчиков.'
        res = RequiredResult(transition=IssueTransitions.none,
                             messages=[msg],
                             assignee='mscnad7',
                             commit=True,
                             delay=False)
        return res

    def get_attachments(self):
        return [('data0.xlsx',  self._get_file(self._contracts))]


class ManyPartnersCase(AbstractDBTestCase):
    _representation = 'many_partners'

    def __init__(self):
        super(ManyPartnersCase, self).__init__()
        self._contract = None
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = dt.datetime.now().strftime('%Y-%m-%d')
        update_flg = 'Да'
        file_path = 'http://say-my-name.xls'
        self._contracts = [self._get_contract(session, type='partners') for _ in range(51)]
        session.flush()

        return method, date, update_flg, file_path

    author = 'some_rick'

    def get_attachments(self):
        return [('data0.xlsx', self._get_file(self._contracts))]

    def get_comments(self):
        return [('other_rick', 'Подтверждено', dt.datetime.now())]

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.none,
                             many_partners=True,
                             assignee='mscnad7',
                             commit=True,
                             delay=True)


class AlreadySolvedCase(AbstractDBTestCase):
    _representation = 'already_solved'

    last_resolved = dt.datetime.now() - dt.timedelta(hours=1)

    def __init__(self):
        super(AlreadySolvedCase, self).__init__()
        self._contract = None
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = dt.datetime.now().strftime('%Y-%m-%d')
        update_flg = 'Да'
        file_path = 'http://say-my-name.xls'

        contract1 = self._get_contract(session)
        self._set_sent_dt(contract1)

        contract2 = self._get_contract(session)
        self._set_sent_dt(contract2)

        self._contracts = [contract1, contract2]
        self._collaterals = [c.col0 for c in self._contracts]
        session.flush()
        return method, date, update_flg, file_path

    def get_comments(self):
        msg = 'Выполнено, отметка Отправлен оригинал проставлена. ' \
              'Проверь, пожалуйста, отчёт на наличие ошибок и проставь отметки, там где есть ошибки, вручную.' \
              'Изменённые договоры и ДС добавлены в очередь на выгрузку в ОЕБС.'
        return [('autodasha', msg), ('autodasha', 'Всё выгрузилось!')]

    def get_result(self):
        msg = (
            'Эта задача уже была выполнена. Направьте новый запрос через '
            '((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ формы)). '
            'Если не найдёте подходящую, заполните, пожалуйста, общую форму.'
        )
        return RequiredResult(transition=IssueTransitions.fixed,
                              messages=[msg],
                              commit=False)


class ExportEnqueuedCase(AbstractDBTestCase):
    _representation = 'export_enqueued'

    def __init__(self):
        super(ExportEnqueuedCase, self).__init__()
        self._contract = None
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = dt.datetime.now().strftime('%Y-%m-%d')
        update_flg = 'Да'
        file_path = 'http://say-my-name.xls'

        contract1 = self._get_contract(session)
        self._set_sent_dt(contract1)

        contract2 = self._get_contract(session)
        self._set_sent_dt(contract2)

        self._contracts = [contract1, contract2]
        self._collaterals = [c.col0 for c in self._contracts]
        session.flush()
        return method, date, update_flg, file_path

    def get_comments(self):
        msg = 'Выполнено, отметка Отправлен оригинал проставлена. '\
              'Проверь, пожалуйста, отчёт на наличие ошибок и проставь отметки, там где есть ошибки, вручную.'\
              'Изменённые договоры и ДС добавлены в очередь на выгрузку в ОЕБС.'
        return [('autodasha', msg)]

    def get_result(self):
        return 'Enqueued'


class ConfirmedHalfExistsCase(AbstractDBTestCase):
    _representation = 'confirmed_half_exists'

    def __init__(self):
        super(ConfirmedHalfExistsCase, self).__init__()
        self._contracts = []
        self._collaterals = []
        self._bad_contracts = []
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = dt.datetime.now().strftime('%Y-%m-%d')
        update_flg = 'Да'
        file_path = 'http://say-my-name.xls'
        bad_contracts = (
            {'external_id': 'aaa/aa', 'col0': {'num': '01'}},
            {'external_id': 'ОФ-123ОФ', 'col0': {'num': 'Соглашение о расторжении'}},
            {'external_id': 12323, 'col0': {'num': '01'}}
        )

        for _ in range(50):
            contract = self._get_contract(session)
            self._set_sent_dt(contract)
            self._contracts.append(contract)
            for contract in bad_contracts:
                self._bad_contracts.append(contract)

        self._collaterals = [c.col0 for c in self._contracts]
        session.flush()
        return method, date, update_flg, file_path

    author = 'some_rick'

    def get_attachments(self):
        return [('data0.xlsx', self._get_file(self._contracts, self._bad_contracts))]

    def get_comments(self):
        return [('other_rick', 'Подтверждено', dt.datetime.now())]

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.none, result=self._contracts,
                              changed_data=(self._contracts, self._collaterals),
                              enqueue=True)


class ConfirmedCollateralsCase(AbstractDBTestCase):
    _representation = 'confirmed_collaterals'

    def __init__(self):
        super(ConfirmedCollateralsCase, self).__init__()
        self._contracts = []
        self._collaterals = []
        self._bad_contracts = []
        self._data = []
        self._client = None
        self._person = None

    def _get_data(self, session):
        method = 'Проставить галку "Отправлен оригинал"'
        date = dt.datetime.now().strftime('%Y-%m-%d')
        update_flg = 'Да'
        file_path = 'http://say-my-name.xls'
        bad_contract = {'external_id': 'aaa/aa', 'col0': {'num': '01'}}

        for _ in range(1):
            contract = self._get_contract(session)
            self._set_sent_dt(contract)
            self._contracts.append(contract)
            self._data.append((contract, contract.col0))
            self._collaterals.append(contract.col0)
            self._bad_contracts.append(bad_contract)
        for _ in range(1):
            contract = self._get_contract(session)
            col = db_utils.add_collateral(contract, is_signed=dt.datetime(2018, 10, 10))
            self._set_sent_dt(contract, col)
            self._data.append((contract, col))
            self._collaterals.append(contract.col0)
            self._collaterals.append(col)
            self._contracts.append(contract)
        session.flush()

        return method, date, update_flg, file_path

    author = 'some_rick'

    @staticmethod
    def _get_file(contracts, bad_contracts=[]):
        buffer = StringIO.StringIO()

        def get_bad_row(c):
            return c['external_id'], c['col0']['num']

        titles = ['Договор', 'ДС']
        data = [(c.external_id, cc.num) for c, cc in contracts] + map(get_bad_row, bad_contracts)

        buffer.write(get_excel_document(titles, data, 0))
        return buffer.getvalue()

    def get_attachments(self):
        return [('data0.xlsx', self._get_file(self._data, self._bad_contracts))]

    def get_comments(self):
        return [('other_rick', 'Подтверждено', dt.datetime.now())]

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.none, result=self._contracts,
                              changed_data=(self._contracts, self._collaterals),
                              enqueue=True)


def mock_staff(testfunc):
    some_rick = staff_utils.Person('some_rick')
    needed_rick = staff_utils.Person('needed_rick')
    deputy = staff_utils.Person('deputy')
    boss = staff_utils.Person('boss')
    some_dept = staff_utils.Department('some_dep', [boss], [deputy], [some_rick, needed_rick])

    other_rick = staff_utils.Person('other_rick')
    other_boss = staff_utils.Person('other_boss')
    other_dept = staff_utils.Department('other_dept', [other_boss], [], [other_rick])

    subrick = staff_utils.Person('subrick')
    subboss1 = staff_utils.Person('subboss1')
    subdept1 = staff_utils.Department('subdept1', [subboss1], [], [subrick])

    subboss2 = staff_utils.Person('subboss2')
    subdept2 = staff_utils.Department('subdept2', [subboss2], [], [], [subdept1])

    subboss3 = staff_utils.Person('subboss3')
    subdept3 = staff_utils.Department('subdept3', [subboss3], [], [], [subdept2])

    holy_spirit = staff_utils.Person('holy_spirit')
    son = staff_utils.Person('son')
    god = staff_utils.Person('god')
    yandex = staff_utils.Department('yandex', [god], [], [son, holy_spirit], [some_dept, other_dept, subdept3])

    staff = staff_utils.StaffMock(yandex)

    staff_path = 'autodasha.core.api.staff.Staff.%s'

    @mock.patch(staff_path % '_get_person_data', lambda s, *a, **k: staff._get_person_data(*a, **k))
    @mock.patch(staff_path % '_get_department_data', lambda s, *a, **k: staff._get_department_data(*a, **k))
    @mock.patch(staff_path % '__init__', lambda *args: None)
    @functools.wraps(testfunc)
    def deco(session, issue_data):
        return testfunc(session, issue_data)

    return deco


def gap_patch(testfunc):
    gap = staff_utils.GapMock([])

    gap_path = 'autodasha.core.api.gap.Gap.%s'

    @mock.patch(gap_path % '_find_gaps', lambda s, *a, **k: gap._find_gaps(*a, **k))
    @mock.patch(gap_path % '__init__', lambda *args: None)
    @functools.wraps(testfunc)
    def deco(session, issue_data):
        return testfunc(session, issue_data)

    return deco


def gap_patch_rick(testfunc):
    other_rick = staff_utils.PersonGap('other_rick')
    gap = staff_utils.GapMock([other_rick])

    gap_path = 'autodasha.core.api.gap.Gap.%s'

    @mock.patch(gap_path % '_find_gaps', lambda s, *a, **k: gap._find_gaps(*a, **k))
    @mock.patch(gap_path % '__init__', lambda *args: None)
    @functools.wraps(testfunc)
    def deco(session, issue_data):
        return testfunc(session, issue_data)

    return deco


@pytest.fixture
def config(config):

    old_ = config._items.get('archive_original_approvers')
    config._items['archive_original_approvers'] = ['other_rick', ['other_boss']]

    yield config

    config._items['archive_original_approvers'] = old_


@mock.patch('autodasha.solver_cl.base_solver.BaseSolver.get_approve_message', get_approve_message)
@mock_staff
@gap_patch
@pytest.mark.usefixtures('config')
@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBTestCase._cases if
                          str(case()) not in ('request_confirmation_absent',)],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data

    req_res = case.get_result()
    solver = ArchiveOriginal(queue_object, st_issue)
    res = solver.solve()
    session.flush()

    if req_res is None:
        assert res.commit is False
        assert res.delay is True
        assert res.issue_report is None
        return

    if req_res == 'Enqueued':
        assert res.commit is False
        assert res.delay is False
        assert res.issue_report is None
        return

    assert res.commit is req_res.commit
    assert res.delay is req_res.delay

    report = res.issue_report
    assert len(report.comments) == req_res.num_comments
    assert case_utils.prepare_comment('\n'.join(c.text for c in report.comments)) == set(req_res.comments)

    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee

    summonees = set(itertools.chain.from_iterable(c.summonees or [] for c in report.comments))
    assert summonees == set(req_res.summonees or [])

    try:
        export_queue = session.query(a_mapper.QueueObject).\
            filter(a_mapper.QueueObject.issue == queue_object.issue,
                   a_mapper.QueueObject.processor == 'EXPORT_CHECK').\
            one()
    except orm.exc.NoResultFound:
        assert not req_res.reexported_contracts
        assert not req_res.reexported_collaterals
    else:
        req_reexp_contracts = {obj.object_id for obj in export_queue.proxies if obj.classname == 'Contract'}
        req_reexp_collaterals = {obj.object_id for obj in export_queue.proxies if obj.classname == 'ContractCollateral'}
        assert req_reexp_contracts | req_reexp_collaterals == {obj.object_id for obj in export_queue.proxies}

        reexp_contracts = {c.id for c in req_res.reexported_contracts}
        reexp_collaterals = {cc.id for cc in req_res.reexported_collaterals}
        assert reexp_contracts == req_reexp_contracts
        assert reexp_collaterals == req_reexp_collaterals


@mock.patch('autodasha.solver_cl.base_solver.BaseSolver.get_approve_message', get_approve_message)
@mock_staff
@gap_patch_rick
@pytest.mark.usefixtures('config')
@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBTestCase._cases if
                          str(case()) in ('request_confirmation_absent',)],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_approvers(session, issue_data):
    queue_object, st_issue, case = issue_data

    req_res = case.get_result()
    solver = ArchiveOriginal(queue_object, st_issue)
    res = solver.solve()
    session.flush()

    if req_res is None:
        assert res.commit is False
        assert res.delay is True
        assert res.issue_report is None
        return

    if req_res == 'Enqueued':
        assert res.commit is False
        assert res.delay is False
        assert res.issue_report is None
        return

    assert res.commit is req_res.commit
    assert res.delay is req_res.delay

    report = res.issue_report
    assert len(report.comments) == req_res.num_comments
    assert case_utils.prepare_comment('\n'.join(c.text for c in report.comments)) == set(req_res.comments)

    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee

    summonees = set(itertools.chain.from_iterable(c.summonees or [] for c in report.comments))
    assert summonees == set(req_res.summonees or [])

    try:
        export_queue = session.query(a_mapper.QueueObject).\
            filter(a_mapper.QueueObject.issue == queue_object.issue,
                   a_mapper.QueueObject.processor == 'EXPORT_CHECK').\
            one()
    except orm.exc.NoResultFound:
        assert not req_res.reexported_contracts
        assert not req_res.reexported_collaterals
    else:
        req_reexp_contracts = {obj.object_id for obj in export_queue.proxies if obj.classname == 'Contract'}
        req_reexp_collaterals = {obj.object_id for obj in export_queue.proxies if obj.classname == 'ContractCollateral'}
        assert req_reexp_contracts | req_reexp_collaterals == {obj.object_id for obj in export_queue.proxies}

        reexp_contracts = {c.id for c in req_res.reexported_contracts}
        reexp_collaterals = {cc.id for cc in req_res.reexported_collaterals}
        assert reexp_contracts == req_reexp_contracts
        assert reexp_collaterals == req_reexp_collaterals
