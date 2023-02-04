# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest
import itertools
import mock
import datetime as dt
from sqlalchemy import orm

from balance import mapper
from balance.payments.service_clients import ClientFraudStatus
from autodasha.solver_cl import FraudCleaner, ParseException
from autodasha.core.api.tracker import IssueTransitions, IssueReport
from autodasha.db import mapper as a_mapper

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils
from tests.autodasha_tests.common import db_utils

DIRECT_ID = 7
YM_PROCESSING_ID = 10001
TRUST_API_PROCESSING_ID = 10501

COMMENTS = {
    'not_like_payment_id':
        '{} - не похож на внутренний ID платежа. Игнорируем.',
    'no_payment_ids':
        'Не удалось извлечь необходимые данные из условия задачи. '
        'Пожалуйста, уточни ID платежа и создай новую задачу через ту же форму.',
    'not_found_payment':
        '{} - платеж с таким ID не найден в базе. Игнорируем',
    'no_invoice':
        '{} - не найден счет для платежа.',
    'no_success_payment':
        '{} - платеж неуспешный, возврат невозможен.',
    'no_fraud_client':
        'Факт фрода не подтверждён. Возврат средств невозможен.',
    'already_fraud':
        'Клиент {} уже помечен как фродер. Подтверждение не требуется.',
    'half_fraud_comment':
        'Коллеги, у клиента {client_data} проставлен флажок %%{{active_flag}}%%, '
        'но не проставлен %%{no_active_flag}%%.\n'
        'Проверьте статус клиента в Балансе.\n'
        'Если клиент - фродер, нажмите "Подтверждено".\n'
        'При неподтверждении фрода нажмите "Не подтверждено".',
    'full_fraud_comment':
        'Коллеги, необходима проверка клиента: {client_data}.\n'
        'Если клиент - фродер, заблокируйте его в Балансе и нажмите "Подтверждено".\n'
        'При неподтверждении фрода нажмите "Не подтверждено".',
    'info_comment':
        'Информация о платежах:\n{}',
    'fraud_cleaner_report':
        'Отчет о проделанных действиях:\n{}',
    'previus_issue_exists':
        'Информация о клиенте {client_data} уже ожидается в задаче {issue_key}',
    'no_single_client':
        'Платежи в данном тикете не по одному клиенту. Это сильно усложняет автоматическую обработку.\n'
        'Раздели, пожалуйста, платежи по-клиентно, используя столбец CLIENT_ID в '
        'информации о платежах и пересоздай тикет через ту же форму.',
    'enqueued':
        'Сгенерированные акты добавлены в очередь на выгрузку в ОЕБС.',
    'empty_report':
        'Действий не выполнено. Похоже, эти платежи уже были обработаны в другой задаче.',
    'rechek_payments_id':
        'Перепроверь, пожалуйста, информацию и заполни форму заново.'
        ' Если у тебя остались вопросы, создай, пожалуйста, тикет в'
        ' ((https://wiki.yandex-team.ru/Tikety-po-blacklisted/ поддержку Траста)).',
    'all_is_done':
        'На стороне Баланса все необходимые действия выполнены.'
        ' К текущему тикету в скором времени прилинкуется тикет из поддержки Траста,'
        ' в котором будет решен вопрос с возвратом платежа'
}


class AbstractParseTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Мошенническое списание с карты',
        'payments': 'Внутренний ID платежа: %s',
        'comment': 'Дополнительная информация: вах вах'
    }

    def __init__(self):
        self.payments = []

    def get_payment_ids(self):
        return ', '.join(p.id for p in self.payments)


class AbstractFailedCheckFormTestCase(AbstractParseTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        super(AbstractFailedCheckFormTestCase, self).__init__()
        self.comments = []

    def add_result_row(self, comment_id, *args, **kwargs):
        self.comments.append(COMMENTS[comment_id].format(*args, **kwargs))

    def prepare_result(self):
        raise NotImplementedError

    def get_result(self):
        self.prepare_result()
        return self.comments


class AbstractSuccessCheckFormTestCase(AbstractParseTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class GoodPaymentsCase(AbstractSuccessCheckFormTestCase):
    _representation = 'good_payments'

    def get_data(self, mock_manager):
        self.payments = [
            mock_utils.create_trust_payment(mock_manager, id_=1111111111),
            mock_utils.create_trust_payment(mock_manager, id_=2222222222)
        ]
        lines = [
            self._get_default_line(payments='Мне бы вот по платежам 1111111111 и 2222222222 хуе мое'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return self.payments


class GoodPaymentsCase2(AbstractSuccessCheckFormTestCase):
    _representation = 'good_payments2'

    def get_data(self, mock_manager):
        self.payments = [
            mock_utils.create_trust_payment(mock_manager, id_=1111111111),
            mock_utils.create_trust_payment(mock_manager, id_=2222222222)
        ]
        lines = [
            self._get_default_line(payments='Мне бы вот по платежам 1111111111 и 2222222222 3 4 5 3232 32'
                                            '323232 42 хуе мое'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return self.payments


class GoodPaymentsCase3(AbstractSuccessCheckFormTestCase):
    _representation = 'good_payments3'

    def get_data(self, mock_manager):
        self.payments = [
            mock_utils.create_trust_payment(mock_manager, id_=1111111111),
            mock_utils.create_trust_payment(mock_manager, id_=2222222222),
            mock_utils.create_trust_payment(mock_manager, id_=33333333333)
        ]
        lines = [
            self._get_default_line(payments='Мне бы вот по платежам '
                                            '1111111111\n и 2222222222,33333333333,'
                                            '11111111111,  3 4 5 3232 32'
                                            '323232 42 хуе мое'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return self.payments


class NoPaymentIdsCase(AbstractFailedCheckFormTestCase):
    _representation = 'no_payment_ids'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(payments='Мне бы вот по платежам то да се'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def prepare_result(self):
        self.add_result_row('no_payment_ids')


class EmptyPaymentsCase(AbstractFailedCheckFormTestCase):
    _representation = 'empty_payments'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(payments=''),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def prepare_result(self):
        self.add_result_row('no_payment_ids')


class NoSuccessPaymentCase(AbstractFailedCheckFormTestCase):
    _representation = 'no_success_payment'

    def get_data(self, mock_manager):
        self.payments = [
            mock_utils.create_trust_payment(mock_manager, id_=1234567890, success=False)
        ]
        lines = [
            self._get_default_line(payments='Мне бы вот по платежам 1234567890'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def prepare_result(self):
        self.add_result_row('no_success_payment', 1234567890),
        self.add_result_row('no_payment_ids')


class UltimateFailedCase(AbstractFailedCheckFormTestCase):
    _representation = 'ultimate_failed_case'

    def get_data(self, mock_manager):
        self.payments = [
            mock_utils.create_trust_payment(mock_manager, id_=1234567891, success=False),
        ]
        lines = [
            self._get_default_line(payments='Мне бы вот по платежам 1234567891,'
                                            '1345, 123456789, 1, 12:00, 123456789111'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def prepare_result(self):
        self.add_result_row('no_success_payment', 1234567891),
        self.add_result_row('not_found_payment', 123456789111),
        self.add_result_row('not_like_payment_id', 1345),
        self.add_result_row('not_like_payment_id', 1)
        self.add_result_row('not_like_payment_id', 12)
        self.add_result_row('not_like_payment_id', 0)
        self.add_result_row('not_like_payment_id', 123456789)
        self.add_result_row('no_payment_ids')


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractSuccessCheckFormTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_success(mock_queue_object, mock_issue_data):
    ri = IssueReport()
    issue, case = mock_issue_data
    required_res = case.get_result()
    solver = FraudCleaner(mock_queue_object, issue)
    res = solver.parse_issue(ri)

    assert set(required_res) == set(res)


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractFailedCheckFormTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_failing(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    ri = IssueReport()

    req_comment = case.get_result()

    solver = FraudCleaner(mock_queue_object, issue)
    with pytest.raises(ParseException):
        solver.parse_issue(ri)

    res_comment = {row for row in ri.comment.splitlines() if row}
    assert set(req_comment) == res_comment


class RequiredResult(case_utils.RequiredResult):
    def __init__(self, **kwargs):
        self.commit = kwargs.get('commit')
        self.delay = kwargs.get('delay')
        self.summonees = kwargs.get('summonees', [])
        super(RequiredResult, self).__init__(**kwargs)
        self.assignee = 'autodasha'
        self.reexported_acts = []
        self.has_new_cfh_row = False
        self.tags = []

    @staticmethod
    def get_comment(key, *args, **kwargs):
        return COMMENTS.get(key).format(*args, **kwargs)


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    summary = 'Мошенническое списание с карты'
    author = 'anevskiy'
    _description = '''
Сервис: Такси
Внутренний ID платежа: {payment_ids}
Дополнительная информация: лалка

'''.strip()
    issue_key = 'test_fraud_cleaner'

    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        super(AbstractDBTestCase, self).__init__()
        self.payments = []
        self.client = None
        self.config = None

    def setup_config(self, session, config):
        config['FRAUD_CLEANER_SETTINGS'] = {
            'fraud_managers': ['fraud_m1', 'fraud_m2'],
            'support_logins': ['tis754'],
            'support_manager': 'lidzhievatt',
            'ym_procecessing_ids': [10001],
            'yamoney_managers': ['ym_manager']
        }
        self.config = config

    def add_payment(self,
                    session,
                    invoice=None,
                    service_id=DIRECT_ID,
                    processing_id=YM_PROCESSING_ID,
                    success=True,
                    paysys_code='TRUST'):
        if paysys_code == 'TRUST':
            p = db_utils.create_trust_payment(session, service_id, processing_id)
        else:
            p = db_utils.create_simple_payment(session, service_id, invoice)
        p.payment_dt = dt.datetime.now() if success else None
        p.invoice = invoice
        session.flush()
        self.payments.append(p)
        return p

    def add_trust_as_processing_payment(self, session, invoice=None, real_processing_id=YM_PROCESSING_ID):
        real_p = self.add_payment(session, processing_id=real_processing_id)
        self.payments = []
        trust_api_p = db_utils.create_simple_payment(
            session, DIRECT_ID, processing_id=TRUST_API_PROCESSING_ID
        )
        trust_api_p.transaction_id = real_p.purchase_token
        trust_api_p.invoice = invoice
        session.flush()
        self.trust_api_p = trust_api_p
        self.payments.append(real_p)
        return real_p

    @staticmethod
    def create_client(session, name='Mr.Fraud',
                      deny_cc=False, fraud_flag=False):

        client = mapper.Client(name=name)
        session.add(client)
        client.deny_cc = deny_cc
        fs = ClientFraudStatus(client)
        session.add(fs)
        fs.fraud_flag = fraud_flag
        session.flush()
        return client

    @staticmethod
    def create_invoice(session,
                       client,
                       order_qty=10,
                       completed=0,
                       do_acts=False
                       ):
        o = db_utils.create_order(session, client)
        p = db_utils.create_person(session)

        i = db_utils.create_invoice(
            session, client, [(o, order_qty)], person=p, turn_on=True
        )
        o.do_process_completion(completed)
        session.flush()
        if do_acts:
            i.generate_act(force=1)
            session.flush()

        return i

    def prepare_data(self, session):
        raise NotImplementedError

    def _get_data(self, session):
        self.prepare_data(session)
        return dict(payment_ids=','.join(str(p.id) for p in self.payments))

    @staticmethod
    def balance_url(obj):
        url = 'https://admin.balance.yandex-team.ru/'

        if isinstance(obj, mapper.Client):
            url += 'passports.xml?tcl_id=%s' % obj.id
        elif isinstance(obj, mapper.Invoice):
            url += 'invoice.xml?invoice_id=%s' % obj.id
        else:
            raise Exception(
                'Object %s is not available for this function' % str(obj)
            )

        return url

    def invoice_comment_data(self, i):
        invoice_data = '(({url} {inv.external_id}))'.format(
            url=self.balance_url(i), inv=i
        )
        return invoice_data

    def client_comment_data(self):
        cl = self.client
        client_name = cl.name or 'Наименование отсутствует'
        client_data = '(({url} {name} ({id_})))'.format(
            url=self.balance_url(cl), name=client_name, id_=cl.id
        )
        return client_data

    @staticmethod
    def st_csv_data(columns, data, delimiter=';'):
        csv_template = """
%%(csv delimiter={delimiter} head=1)
{columns}
{data}
%%
        """.strip()
        table_data = '\n'.join([delimiter.join(map(unicode, row)) for row in data])
        columns = delimiter.join(columns)
        table = csv_template.format(delimiter=delimiter,
                                    columns=columns,
                                    data=table_data)
        return table

    @staticmethod
    def _get_invoice(p):
        invoice = p.invoice or p.session.query(mapper.Payment).getone(transaction_id=p.purchase_token).invoice

        if isinstance(invoice, mapper.ChargeNote):
            return invoice.charge_invoice

        return invoice

    def get_payment_data(self, processing_id=None):
        def get_processing_info(p_):
            processing_info = '{pr.cc} ({pr.id})'.format(pr=p_.terminal.processing)
            return processing_info

        columns = (
            'PAYMENT_ID', 'CLIENT_ID', 'TRUST_PAYMENT_ID', 'INVOICE',
            'PROCESSING', 'PAYMENT_DT', 'AMOUNT', 'CURRENCY', 'CARD'
        )
        payments_list = []

        for p_ in self.payments:
            if p_.terminal.processing.cc == 'trustapi' and p_.transaction_id:
                p_orig = p_.session.query(mapper.Payment).getone(purchase_token=p_.transaction_id)
                payments_list.append(p_orig)
            else:
                payments_list.append(p_)

        data = [(p.id, self._get_invoice(p).client_id, p.trust_payment_id or 'no trust payment',
                 self.invoice_comment_data(self._get_invoice(p)), get_processing_info(p), p.payment_dt,
                 '%g' % float(p.postauth_amount or p.amount or 0), p.currency, p.user_account) for p in payments_list]

        if processing_id:
            data = filter(lambda row: str(processing_id) in row[4], data)

        comment = self.st_csv_data(columns, data)
        return comment

    def get_info_comment(self):
        data = self.get_payment_data()
        return COMMENTS.get('info_comment').format(data)

    def get_approve_request(self, force=0):
        picture_link = 'https://jing.yandex-team.ru/files/autodasha/approve_button_expanded.png'
        deny_cc, fraud_flag = self.client.deny_cc, self.client.fraud_status.fraud_flag
        cmt_data = self.client_comment_data()

        if deny_cc and fraud_flag and not force:
            raise Exception('Why do you need approve request?')

        if not (deny_cc or fraud_flag):
            comment = COMMENTS.get('full_fraud_comment').format(client_data=cmt_data)
        else:
            active_flag = 'DENY_CC' if deny_cc else 'FRAUD_FLAG'
            no_active_flag = 'DENY_CC' if not deny_cc else 'FRAUD_FLAG'
            comment = COMMENTS.get('half_fraud_comment').format(
                client_data=cmt_data, active_flag=active_flag, no_active_flag=no_active_flag
            )
        approve_request = '\n'.join((comment, picture_link))
        return approve_request

    def get_cleaner_report(self, additional_data):
        assert len(self.payments) == len(additional_data)
        data = []

        for i in range(len(self.payments)):
            p = self.payments[i]
            d = additional_data[i]
            row = (
                p.id, self._get_invoice(p).client_id,
                self.invoice_comment_data(self._get_invoice(p)),
                d[0], d[1], d[2], d[3]
            )
            data.append(row)

        columns = (
            'PAYMENT_ID', 'CLIENT_ID', 'INVOICE', 'REVERSED_FREE_QTY',
            'REVERSED_RECEIPT_SUM', 'NEW_ACTS', 'BAD_DEBTED_ACTS'
        )

        comment = self.st_csv_data(columns, data)

        return COMMENTS.get('fraud_cleaner_report').format(comment)

    def add_cfh_row(self, session, key=None, date=None):
        cfh = a_mapper.ClientFraudHistory(
            solver='FraudCleaner',
            issue_key=key or self.issue_key,
            object_id=self.client.id,
            classname='Client',
            dt=date or dt.datetime.now()
        )
        session.add(cfh)
        session.flush()


class NoInvoiceCase(AbstractDBTestCase):
    _representation = 'no_invoice'

    def prepare_data(self, session):
        self.add_payment(session)

    def get_result(self):
        res = RequiredResult(commit=False, delay=False)
        comments = [COMMENTS['no_invoice'].format(self.payments[0].id), COMMENTS['rechek_payments_id']]
        res.add_message('\n'.join(comments))
        res.transition = IssueTransitions.wont_fix
        res.summonees = ['anevskiy']
        return res


class TrustAsProcessingCase(AbstractDBTestCase):
    _representation = 'trust_as_processing'

    def prepare_data(self, session):
        self.client = self.create_client(session, deny_cc=True, fraud_flag=True)
        invoice = self.create_invoice(session, self.client)
        self.add_trust_as_processing_payment(session, invoice)

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.add_message(self.get_info_comment())
        res.add_message(COMMENTS.get('already_fraud').format(self.client_comment_data()))
        res.summonees = []
        data = [(10, 300, '-', '-')]
        res.add_message(self.get_cleaner_report(data))
        res.add_message(COMMENTS['all_is_done'])
        return res


class TrustAsProcessingWOInvoiceCase(AbstractDBTestCase):
    _representation = 'trust_as_processing_wo_invoice'

    def prepare_data(self, session):
        self.add_trust_as_processing_payment(session)

    def get_result(self):
        res = RequiredResult(commit=False, delay=False)
        comments = [COMMENTS['no_invoice'].format(self.payments[0].id), COMMENTS['rechek_payments_id']]
        res.add_message('\n'.join(comments))
        res.transition = IssueTransitions.wont_fix
        res.summonees = [self.author]
        return res


class TrustAsProcessingRbsRealProcessingCase(AbstractDBTestCase):
    _representation = 'trust_as_processing_rbs_real_processing'

    def prepare_data(self, session):
        self.client = self.create_client(session, deny_cc=True, fraud_flag=True)
        invoice = self.create_invoice(session, self.client)
        p = self.add_trust_as_processing_payment(session, invoice, real_processing_id=10105)
        trust_p = self.trust_api_p
        trust_p.payment_dt = dt.datetime.now()
        trust_p.invoice = invoice
        self.trust_api_p = p #бубны, чтобы тест работал без переделки логики всех тестов
        p.invoice = None
        self.payments = [trust_p]

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.add_message(self.get_info_comment())
        res.add_message(COMMENTS.get('already_fraud').format(self.client_comment_data()))
        res.summonees = []
        data = [(10, 300, '-', '-')]
        p_ = self.payments
        self.payments = [self.trust_api_p]  # тут бубны по той же причине
        res.add_message(self.get_cleaner_report(data))
        self.payments = p_
        res.add_message(COMMENTS['all_is_done'])
        return res


class TrustAsProcessingRbsRealProcessingTranstactionIdNoneCase(AbstractDBTestCase):
    _representation = 'trust_as_processing_rbs_real_processing_transaction_id_none'

    def prepare_data(self, session):
        self.client = self.create_client(session, deny_cc=True, fraud_flag=True)
        invoice = self.create_invoice(session, self.client)
        p = self.add_trust_as_processing_payment(session, invoice, real_processing_id=10105)
        trust_p = self.trust_api_p
        trust_p.payment_dt = dt.datetime.now()
        trust_p.invoice = invoice
        trust_p.transaction_id = None
        p.invoice = None
        self.payments = [trust_p]

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.add_message(self.get_info_comment())
        res.add_message(COMMENTS.get('already_fraud').format(self.client_comment_data()))
        res.summonees = []
        data = [(10, 300, '-', '-')]
        res.add_message(self.get_cleaner_report(data))
        res.add_message(COMMENTS['all_is_done'])
        return res


class HalfFraudDenyCCApproveCase(AbstractDBTestCase):
    _representation = 'half_fraud_deny_cc_approve'

    def prepare_data(self, session):
        self.client = self.create_client(session, deny_cc=True, fraud_flag=False)
        invoice = self.create_invoice(session, self.client)
        self.add_payment(session, invoice)

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        res.add_message(self.get_info_comment())
        res.add_message(self.get_approve_request())
        res.summonees = ['fraud_m1', 'fraud_m2']
        res.has_new_cfh_row = True
        res.tags = ['trust_m']
        return res


class HalfFraudFraudFlagApproveCase(AbstractDBTestCase):
    _representation = 'half_fraud_fraud_flag_approve'

    def prepare_data(self, session):
        self.client = self.create_client(session, deny_cc=False, fraud_flag=True)
        invoice = self.create_invoice(session, self.client)
        self.add_payment(session, invoice)

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        res.add_message(self.get_info_comment())
        res.add_message(self.get_approve_request())
        res.summonees = ['fraud_m1', 'fraud_m2']
        res.has_new_cfh_row = True
        res.tags = ['trust_m']
        return res


class FullFraudApproveCase(AbstractDBTestCase):
    _representation = 'full_fraud_approve'

    def prepare_data(self, session):
        self.client = self.create_client(session, deny_cc=False, fraud_flag=False)
        invoice = self.create_invoice(session, self.client)
        self.add_payment(session, invoice)

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        res.add_message(self.get_info_comment())
        res.add_message(self.get_approve_request())
        res.summonees = ['fraud_m1', 'fraud_m2']
        res.has_new_cfh_row = True
        res.tags = ['trust_m']
        return res


class AlreadyFraudSimpleCase(AbstractDBTestCase):
    _representation = 'already_fraud_simple'

    def prepare_data(self, session):
        self.client = self.create_client(session, deny_cc=True, fraud_flag=True)
        invoice = self.create_invoice(session, self.client, 10)
        self.add_payment(session, invoice)
        invoice2 = self.create_invoice(session, self.client, 20)
        self.add_payment(session, invoice2)

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.add_message(self.get_info_comment())
        res.add_message(COMMENTS.get('already_fraud').format(self.client_comment_data()))
        res.summonees = []
        data = [(10, 300, '-', '-'), [20, 600, '-', '-']]
        res.add_message(self.get_cleaner_report(data))
        res.add_message(COMMENTS['all_is_done'])
        # res.type_id = 52
        return res


"""
class AlreadyFraudWithApproveRequest(AbstractDBTestCase):
    _representation = 'already_fraud_w_approve_request'

    def prepare_data(self, session):
        self.client = self.create_client(session, deny_cc=True, fraud_flag=True)
        invoice = self.create_invoice(session, self.client, 10)
        self.add_payment(session, invoice)
        invoice2 = self.create_invoice(session, self.client, 20)
        self.add_payment(session, invoice2)

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        return res

    def get_comments(self):
        return [
            ('autodasha', self.get_info_comment()),
            {'author': 'autodasha', 'text': self.get_approve_request(force=1), 'summonees': ['fraud_m1']},
        ]
"""


class MultipleClientsCase(AbstractDBTestCase):
    _representation = 'no_single_client'

    def prepare_data(self, session):
        client1 = self.create_client(session)
        invoice1 = self.create_invoice(session, client1)
        self.add_payment(session, invoice1)

        client2 = self.create_client(session)
        invoice2 = self.create_invoice(session, client2)
        self.add_payment(session, invoice2)

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.add_message(self.get_info_comment())
        res.add_message(COMMENTS.get('no_single_client'))
        return res


class FromFraudManagersCase(AbstractDBTestCase):
    _representation = 'from_fraud_managers'

    summary = 'Мошенническое списаание с карты (Платежный фрод)'
    followers = ['fraud_m1']

    def prepare_data(self, session):
        self.client = self.create_client(session, deny_cc=False, fraud_flag=True)
        invoice = self.create_invoice(session, self.client, 10)
        self.add_payment(session, invoice)
        invoice2 = self.create_invoice(session, self.client, 20)
        self.add_payment(session, invoice2)

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.add_message(self.get_info_comment())
        data = [(10, 300, '-', '-'), [20, 600, '-', '-']]
        res.add_message(self.get_cleaner_report(data))
        return res


class UnapproveCommentCase(AbstractDBTestCase):
    _representation = 'unapprove_comment'

    def prepare_data(self, session):
        self.client = self.create_client(session, deny_cc=False, fraud_flag=False)
        invoice = self.create_invoice(session, self.client, 10)
        self.add_payment(session, invoice)

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.add_message(self.get_info_comment())
        res.add_message(COMMENTS.get('no_fraud_client'))
        return res

    def get_comments(self):
        return [('fraud_m1', 'Не подтверждено')]


class UnapproveCommentCase2(AbstractDBTestCase):
    _representation = 'unapprove_comment2'

    def prepare_data(self, session):
        self.client = self.create_client(session, deny_cc=False, fraud_flag=False)
        invoice = self.create_invoice(session, self.client, 10)
        self.add_payment(session, invoice)

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.add_message(self.get_info_comment())
        res.add_message(COMMENTS.get('no_fraud_client'))
        return res

    def get_comments(self):
        return [('tis754', 'Не подтверждено')]


class WrongAuthorUnapproveCommentCase(AbstractDBTestCase):
    _representation = 'wrong_author_unapprove_comment'

    def prepare_data(self, session):
        self.client = self.create_client(session, deny_cc=False, fraud_flag=False)
        invoice = self.create_invoice(session, self.client, 10)
        self.add_payment(session, invoice)

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        res.add_message(self.get_info_comment())
        res.add_message(self.get_approve_request())
        res.summonees = ['fraud_m1', 'fraud_m2']
        res.has_new_cfh_row = True
        res.tags = ['trust_m']
        return res

    def get_comments(self):
        return [
            ('lal_ya_naebshik', 'Не подтверждено')
        ]


class ApproveCommentCase(AbstractDBTestCase):
    _representation = 'approve_comment'

    def prepare_data(self, session):
        self.client = self.create_client(session, deny_cc=False, fraud_flag=False)
        invoice = self.create_invoice(session, self.client, 10)
        self.add_payment(session, invoice)

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.add_message(self.get_info_comment())
        data = [(10, 300, '-', '-')]
        res.add_message(self.get_cleaner_report(data))
        res.summonees = []
        res.add_message(COMMENTS['all_is_done'])
        # res.type_id = 52
        return res

    def get_comments(self):
        return [('fraud_m1', 'Подтверждено')]


class ApproveCommentCase2(AbstractDBTestCase):
    _representation = 'approve_comment2'

    def prepare_data(self, session):
        self.client = self.create_client(session, deny_cc=False, fraud_flag=False)
        invoice = self.create_invoice(session, self.client, 10)
        self.add_payment(session, invoice)

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.add_message(self.get_info_comment())
        data = [(10, 300, '-', '-')]
        res.add_message(self.get_cleaner_report(data))
        res.summonees = []
        res.add_message(COMMENTS['all_is_done'])
        # res.type_id = 52
        return res

    def get_comments(self):
        return [('tis754', 'Подтверждено')]


class WrongAuthorApproveCommentCase(AbstractDBTestCase):
    _representation = 'wrong_author_approve_comment'

    def prepare_data(self, session):
        self.client = self.create_client(session, deny_cc=False, fraud_flag=False)
        invoice = self.create_invoice(session, self.client, 10)
        self.add_payment(session, invoice)

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        return res

    def get_comments(self):
        return [
            ('autodasha', self.get_info_comment()),
            {'author': 'autodasha', 'text': self.get_approve_request(force=1), 'summonees': ['fraud_m1']},
            ('lal_ya_naebshik', 'Подтверждено')
        ]


class UltimateFraudCleanerCase(AbstractDBTestCase):
    _representation = 'ultimate_fraud_cleaner'

    def prepare_data(self, session):
        self.client = self.create_client(session, deny_cc=True, fraud_flag=True)

        invoice = self.create_invoice(session, self.client, 100, completed=50, do_acts=True)
        self.add_payment(session, invoice, processing_id=10105)

        invoice2 = self.create_invoice(session, self.client, 10)
        self.add_payment(session, invoice2)

        invoice3 = self.create_invoice(session, self.client, 30, completed=30)
        self.add_payment(session, invoice3)

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed

        res.add_message(self.get_info_comment())
        res.add_message(COMMENTS['already_fraud'].format(self.client_comment_data()))
        cleaner_data = [
            (50, 1500, '-', self.payments[0].invoice.acts[0].external_id),
            (10, 300, '-', '-'),
            (0, 0, self.payments[2].invoice.acts[0].external_id, self.payments[2].invoice.acts[0].external_id)
        ]
        res.add_message(self.get_cleaner_report(cleaner_data))
        res.add_message(COMMENTS['enqueued'])
        res.summonees = []
        res.reexported_acts = [self.payments[2].invoice.acts[0]]
        res.add_message(COMMENTS['all_is_done'])
        # res.type_id = 52
        return res


class PreviusIssueExistsCase(AbstractDBTestCase):
    _representation = 'previus_issue_exists'
    issue_key = 'ISSUE-123456'

    def prepare_data(self, session):
        self.client = self.create_client(session, deny_cc=True, fraud_flag=False)

        invoice = self.create_invoice(session, self.client, 100, completed=50, do_acts=True)
        self.add_payment(session, invoice, processing_id=10105)

        self.add_cfh_row(session, 'ISSUE-123')

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        res.add_message(self.get_info_comment())
        res.add_message(COMMENTS['previus_issue_exists'].format(client_data=self.client_comment_data(),
                                                                issue_key='ISSUE-123'))
        res.summonees = ['lidzhievatt']
        return res


class SilentCheckWasSolvedCase(AbstractDBTestCase):
    _representation = 'silent_check_was_solved'

    last_resolved = dt.datetime.now() - dt.timedelta(hours=1)

    def prepare_data(self, session):
        pass

    def get_result(self):
        pass


class EmptyReportCase(AbstractDBTestCase):
    _representation = 'empty_report'

    def prepare_data(self, session):
        self.client = self.create_client(session, deny_cc=True, fraud_flag=True)
        invoice = self.create_invoice(session, self.client, 0)
        self.add_payment(session, invoice)
        invoice2 = self.create_invoice(session, self.client, 0)
        self.add_payment(session, invoice2)

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.add_message(self.get_info_comment())
        res.add_message(COMMENTS.get('already_fraud').format(self.client_comment_data()))
        res.add_message(COMMENTS.get('empty_report'))
        res.summonees = []
        res.add_message(COMMENTS['all_is_done'])
        return res


class ChargeNoteCase(AbstractDBTestCase):
    _representation = 'charge_note_case'

    def prepare_data(self, session):
        self.client = self.create_client(session, deny_cc=True, fraud_flag=True)
        p = db_utils.create_person(session)
        pa = db_utils.create_personal_account(
            session, self.client, p, personal_account_fictive=0
        )
        o = db_utils.create_order(session, client=self.client)
        charge_note = db_utils.create_charge_note(
            session, pa, [(o, 10)]
        )
        charge_note.on_turn_on(
            mapper.Operation(2, charge_note), charge_note.effective_sum
        )
        session.flush()
        pa.create_receipt(charge_note.effective_sum)
        self.add_payment(session, charge_note)

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.add_message(self.get_info_comment())
        res.add_message(COMMENTS.get('already_fraud').format(self.client_comment_data()))
        # res.summonees = []
        data = [(10, 300, '-', '-')]
        res.add_message(self.get_cleaner_report(data))
        res.add_message(COMMENTS['all_is_done'])
        return res


@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data
    solver = FraudCleaner(queue_object, st_issue)
    res = solver.solve()

    session.flush()
    req_res = case.get_result()
    report = res.issue_report

    if req_res is None:
        assert report is None
        return

    assert res.commit == req_res.commit
    assert res.delay == req_res.delay

    report_comments = []
    for c in report.comments:
        c_text = c['text']
        for part in c_text.strip().split('\n'):
            report_comments.append(part.strip())

    req_res_comments = []
    for c in req_res.comments:
        for part in c.strip().split('\n'):
            req_res_comments.append(part.strip())

    report_comments = sorted(report_comments)
    req_res_comments = sorted(req_res_comments)

    assert len(report_comments) == len(req_res_comments)

    for i in range(len(req_res_comments)):
        assert req_res_comments[i] == report_comments[i]

    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee
    assert set(report.tags) == set(req_res.tags)

    assert report.type == req_res.type_id

    summonees = set(itertools.chain.from_iterable(cmt.summonees or [] for cmt in report.comments))
    assert set(req_res.summonees) == summonees

    try:
        export_queue = session.query(a_mapper.QueueObject).\
            filter(a_mapper.QueueObject.issue == queue_object.issue,
                   a_mapper.QueueObject.processor == 'EXPORT_CHECK').\
            one()
    except orm.exc.NoResultFound:
        assert not req_res.reexported_acts
    else:
        req_reexp_acts = {obj.object_id for obj in export_queue.proxies}
        reexp_acts = {act.id for act in req_res.reexported_acts}
        assert reexp_acts == req_reexp_acts

    if req_res.has_new_cfh_row:
        assert session.query(a_mapper.ClientFraudHistory).filter(
            a_mapper.ClientFraudHistory.issue_key == st_issue.key
        ).all()
