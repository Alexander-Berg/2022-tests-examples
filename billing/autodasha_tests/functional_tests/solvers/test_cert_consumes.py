# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest
import mock
import random
from datetime import datetime as dt
from datetime import timedelta
from autodasha.solver_cl import CertConsume, ParseException
from autodasha.core.api.tracker import IssueTransitions
import balance.mapper as mapper
from tests.autodasha_tests.common import db_utils
from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils
import sqlalchemy as sa
from autodasha.db import scheme as a_scheme
from autodasha.solver_cl.base_solver import ExecutionException

COMMENTS = {
    'already_solved':
        'Эта задача уже была выполнена. Направьте новый запрос через '
        '((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ формы)). '
        'Если не найдёте подходящую, заполните, пожалуйста, общую форму.',
    'invalid_issue_data':
        'Не удалось извлечь необходимые данные из условия задачи. '
        'Пожалуйста, уточни данные и создай новую задачу через ту же форму.',
    'invalid_service_data':
        'Сервис должен быть одним из следующих: \n'
        'Медиаселлинг (70-)\n'
        'Баян (77-)\n'
        'Яндекс Баннерокрутилка (67-)\n'
        'Директ Рекламные кампании (7-)\n'
        'Пожалуйста, создай новую задачу через ту же форму с введением корректных данных.',
    'invalid_order_data':
        'Список заказов указан некорректно. Формат указания: '
        '"№ заказа - количество", каждая следующая пара - с новой строки.\n'
        'Пожалуйста, создай новую задачу через ту же форму с введением корректных данных.',
    'not_found_orders':
        'Следующие заказы не найдены в Биллинге:\n{}\nВозможные причины:\n'
        '   1) некорректно указаны сервис или № РК.\n'
        '   2) необходимо создать недовыставленный счет.\n'
        'Пожалуйста, уточни данные и создай новую задачу через ту же форму.',
    'approve_inner_clients':
        'Клиенты:\n'
        '{clients}\nне являются внутренними по сервису {service}.\n'
        'Добавляем клиентов в список внутренних?\n\n'
        'Для подтверждения требуется нажать кнопку \"Подтверждено\"\n'
        'https://jing.yandex-team.ru/files/autodasha/approve_button_2_ru.png\n'
        'For confirmation it is necessary to press the button \"Confirmed\"\n'
        'https://jing.yandex-team.ru/files/autodasha/approve_button_2_eng.png',
    'clarify_whose_budget':
        'кто:{author}, уточните, пожалуйста, чей будет бюджет.',
    'hidden_clients':
        'Зачисление сертификатных средств внутренним клиентам\n'
        '{clients}\n'
        'по сервису {service} временно приостановлено.\n'
        'За дополнительной информацией обратитесь в поддержку '
        'через ((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ общую форму)).',
    'unknown_error':
        'Внутренняя ошибка: зачислить сертификатные средства не удалось.',
    'resolved':
        '{order}: зачислено {qty_sum} ({qty_name})\n',
    'no_approval':
        'Подтверждение не было получено. При необходимости создай задачу через форму еще раз.',
    'added_clients': 'Клиенты:\n {added_clients} \nдобавлены в список внутренних.',
    'unmoderated': 'Заказ {order} не промодерирован. Зачислить сертификатные средства не удалось.\nКак только заказ будет промодерирован заполни, пожалуйста, форму еще раз.'
}


def check_client_status(session, service_id, clients):
    result = []
    for client, mode in clients:
        ic = a_scheme.inner_clients
        check_query = sa.select([ic.c.client_id, ic.c.hidden]).where(
            sa.and_(ic.c.service_id == service_id, ic.c.client_id == client)
        )
        res = session.execute(check_query).fetchone()
        mode = getattr(res, 'hidden', None)
        result.append((client, mode))

    return result


def create_inner_client(session, service_id, hidden=0, is_agency=False, agency=None):
    client = db_utils.create_client(session, is_agency=is_agency, agency=agency)
    ic = a_scheme.inner_clients
    values = dict(client_id=int(client.id), service_id=service_id, hidden=hidden)
    session.execute(ic.insert(), values)
    return client


service_map = {
    77: 'Баян (77-)',
    7: 'Директ Рекламные кампании (7-)',
    70: 'Медиаселлинг (70-)',
    67: 'Яндекс Баннерокрутилка (67-)',
    666: 'СОТОНА СЕРВИС (666-)',
}


class AbstractParseTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Зачисление сертификатных средств',
        'service': 'Сервис: %s',
        'order_qty_pairs': '№ РК и количество: %s',
        'comment': 'Комментарий: нужно больше золота!',
    }

    def __init__(self):
        self.service = None
        self.order_qty_pairs = None


class AbstractFailedCheckFormTestCase(AbstractParseTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class AbstractSuccessCheckFormTestCase(AbstractParseTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


# Неверная строка с сервисом
class WrongIssueDataSCase(AbstractFailedCheckFormTestCase):
    _representation = 'wrong_issue_data_service'

    def get_data(self, mock_manager):
        lines = [
            'Сервис сервис: %s' % service_map[70],
            self._get_default_line('order_qty_pairs'),
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_issue_data']


# Отсутсвтие строки с комментарием
class WrongIssueDataCCase(AbstractFailedCheckFormTestCase):
    _representation = 'wrong_issue_data_comment'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(service=service_map[77]),
            self._get_default_line('order_qty_pairs'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_issue_data']


# Неверно указано тело задачи с № РК и количеством
class WrongIssueDataBCase(AbstractFailedCheckFormTestCase):
    _representation = 'wrong_issue_data_body'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line('service'),
            'Заказы: %s' % '6666-5555\n8888-333',
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_issue_data']


# Неверное название сервиса
class WrongServiceCase(AbstractFailedCheckFormTestCase):
    _representation = 'wrong_service_name'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(service=service_map[666]),
            self._get_default_line('order_qty_pairs'),
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_service_data']


# Не указаны пары заказ-количество
class WrongOrderDataCase(AbstractFailedCheckFormTestCase):
    _representation = 'no_order_qty_pairs'

    def get_data(self, mock_manager):
        mock_utils.create_service(mock_manager, id=70)
        lines = [
            self._get_default_line(service=service_map[70]),
            self._get_default_line(order_qty_pairs='\n'),
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_order_data']


# Не указано количество единиц заказа
class WrongOrderDataCase(AbstractFailedCheckFormTestCase):
    _representation = 'wrong_order_data_no_qty'

    def get_data(self, mock_manager):
        mock_utils.create_service(mock_manager, id=70)
        lines = [
            self._get_default_line(service=service_map[70]),
            self._get_default_line(order_qty_pairs='666-\n777-666'),
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_order_data']


# Слишком много тире
class WrongOrderData2Case(AbstractFailedCheckFormTestCase):
    _representation = 'wrong_order_data_extra_dash'

    def get_data(self, mock_manager):
        mock_utils.create_service(mock_manager, id=70)
        lines = [
            self._get_default_line(service=service_map[70]),
            self._get_default_line(order_qty_pairs='6-666-666\n777-666'),
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_order_data']


# Буквы в заказе
class WrongOrderData3Case(AbstractFailedCheckFormTestCase):
    _representation = 'wrong_order_data_letters_in_order'

    def get_data(self, mock_manager):
        mock_utils.create_service(mock_manager, id=70)
        lines = [
            self._get_default_line(service=service_map[70]),
            self._get_default_line(order_qty_pairs='666aa-666\n777-666'),
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_order_data']


# Единицы заказа с точкой
class WrongQtyAmountCase(AbstractFailedCheckFormTestCase):
    _representation = 'wrong_qty_amount_point'

    def get_data(self, mock_manager):
        mock_utils.create_service(mock_manager, id=70)
        lines = [
            self._get_default_line(service=service_map[70]),
            self._get_default_line(order_qty_pairs='666-999\n777-555.66'),
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_order_data']


# Единицы заказа с запятой
class WrongQtyAmount2Case(AbstractFailedCheckFormTestCase):
    _representation = 'wrong_qty_amount_comma'

    def get_data(self, mock_manager):
        mock_utils.create_service(mock_manager, id=70)
        lines = [
            self._get_default_line(service=service_map[70]),
            self._get_default_line(order_qty_pairs='666-999\n777-555,66'),
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_order_data']


# Буквы в единицах заказа
class WrongQtyAmount3Case(AbstractFailedCheckFormTestCase):
    _representation = 'wrong_qty_amount_letters_qty'

    def get_data(self, mock_manager):
        mock_utils.create_service(mock_manager, id=70)
        lines = [
            self._get_default_line(service=service_map[70]),
            self._get_default_line(order_qty_pairs='666-999\n777-555a66'),
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_order_data']


# 0 единиц к зачислению
class WrongQtyAmount4Case(AbstractFailedCheckFormTestCase):
    _representation = 'wrong_qty_amount_zero_qty'

    def get_data(self, mock_manager):
        mock_utils.create_order(mock_manager, service_id=70, service_order_id=666)
        mock_utils.create_order(mock_manager, service_id=70, service_order_id=777)
        mock_utils.create_service(mock_manager, id=70)
        lines = [
            self._get_default_line(service=service_map[70]),
            self._get_default_line(order_qty_pairs='666-999\n777-0'),
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_order_data']


# Заказы отсутсвуют в базе(неверный сервис)
class OrdersNotInDBCase(AbstractFailedCheckFormTestCase):
    _representation = 'orders_not_in_db_wrong_service'

    def get_data(self, mock_manager):
        mock_utils.create_order(mock_manager, service_id=70, service_order_id=666)
        mock_utils.create_order(mock_manager, service_id=7, service_order_id=777)
        mock_utils.create_service(mock_manager, id=70)
        lines = [
            self._get_default_line(service=service_map[70]),
            self._get_default_line(order_qty_pairs='666-999\n777-666'),
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['not_found_orders'].format('70-777')


# Неверный service_order_id
class OrdersNotInDB2Case(AbstractFailedCheckFormTestCase):
    _representation = 'orders_not_in_db_wrong_service_order_id'

    def get_data(self, mock_manager):
        mock_utils.create_order(mock_manager, service_id=70, service_order_id=555)
        mock_utils.create_order(mock_manager, service_id=70, service_order_id=888)
        mock_utils.create_service(mock_manager, id=70)
        lines = [
            self._get_default_line(service=service_map[70]),
            self._get_default_line(order_qty_pairs='666-999\n777-666'),
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['not_found_orders'].format('70-666\n70-777')


# Успешная парсилка
class SuccessMockTestCase(AbstractSuccessCheckFormTestCase):
    _representation = 'success_mock_test'

    def get_data(self, mock_manager):
        self.o1 = mock_utils.create_order(
            mock_manager, service_id=67, service_order_id=6666666
        )
        self.o2 = mock_utils.create_order(
            mock_manager, service_id=67, service_order_id=7777777
        )
        mock_utils.create_service(mock_manager, id=67)
        lines = [
            self._get_default_line(service=service_map[67]),
            self._get_default_line(order_qty_pairs='6666666-999\n7777777-666'),
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return [(self.o1, 999), (self.o2, 666)]


# Куча пробелов, парсилка хавает
class SuccessMockWithSpacesTestCase(AbstractSuccessCheckFormTestCase):
    _representation = 'success_mock_with_spaces_test'

    def get_data(self, mock_manager):
        self.o1 = mock_utils.create_order(
            mock_manager, service_id=67, service_order_id=6666666
        )
        self.o2 = mock_utils.create_order(
            mock_manager, service_id=67, service_order_id=7777777
        )
        mock_utils.create_service(mock_manager, id=67)
        lines = [
            self._get_default_line(service='    ' + service_map[67] + '   '),
            self._get_default_line(
                order_qty_pairs='  6666666  -  999   \n  7777777  -  666   '
            ),
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return [(self.o1, 999), (self.o2, 666)]


# Пустые строки, парсилка хавает
class SuccessMockEmptyLinesCase(AbstractSuccessCheckFormTestCase):
    _representation = 'success_mock_empty_lines_test'

    def get_data(self, mock_manager):
        self.o1 = mock_utils.create_order(
            mock_manager, service_id=67, service_order_id=6666666
        )
        self.o2 = mock_utils.create_order(
            mock_manager, service_id=67, service_order_id=7777777
        )
        mock_utils.create_service(mock_manager, id=67)
        lines = [
            self._get_default_line(service=service_map[67]),
            self._get_default_line(order_qty_pairs='6666666-999\n\n\n7777777-666'),
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return [(self.o1, 999), (self.o2, 666)]


# Комментарий: Комментарий:
class TooManyCommentsCase(AbstractSuccessCheckFormTestCase):
    _representation = 'too_many_comments'

    def get_data(self, mock_manager):
        self.o1 = mock_utils.create_order(
            mock_manager, service_id=67, service_order_id=6666666
        )
        self.o2 = mock_utils.create_order(
            mock_manager, service_id=67, service_order_id=7777777
        )
        mock_utils.create_service(mock_manager, id=67)
        lines = [
            self._get_default_line(service=service_map[67]),
            self._get_default_line(order_qty_pairs='6666666-999\n\n\n7777777-666'),
            'Комментарий: Комментарий: лоигн клиента - bro190517',
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return [(self.o1, 999), (self.o2, 666)]


# Призыв сатаны в комментариях
class RandomCommentTestCase(AbstractSuccessCheckFormTestCase):
    _representation = 'random_comment_test'

    def get_data(self, mock_manager):
        self.o1 = mock_utils.create_order(
            mock_manager, service_id=7, service_order_id=6666666
        )
        self.o2 = mock_utils.create_order(
            mock_manager, service_id=7, service_order_id=7777777
        )
        mock_utils.create_service(mock_manager, id=7)
        lines = [
            self._get_default_line(service='    ' + service_map[7] + '   '),
            self._get_default_line(
                order_qty_pairs='  6666666  -  999   \n  7777777  -  666   '
            ),
            'Комментарий: Этот комментарий никак не парсится, нам по сути не очень важно зачем они зачисляют\n'
            'свои сертификатные средства, поэтому попробуем призвать СОТОНУ!\n'
            '666!!!',
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return [(self.o1, 999), (self.o2, 666)]


@pytest.mark.parametrize(
    'mock_issue_data',
    [case() for case in AbstractSuccessCheckFormTestCase._cases],
    ids=lambda case: str(case),
    indirect=['mock_issue_data'],
)
def test_parse_success(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    required_res = case.get_result()
    solver = CertConsume(mock_queue_object, issue)
    res = solver._parse_issue()

    assert required_res == res


@pytest.mark.parametrize(
    'mock_issue_data',
    [case() for case in AbstractFailedCheckFormTestCase._cases],
    ids=lambda case: str(case),
    indirect=['mock_issue_data'],
)
def test_parse_failing(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data

    req_comment = case.get_result()

    solver = CertConsume(mock_queue_object, issue)
    with pytest.raises(ParseException) as exc:
        solver._parse_issue()

    assert req_comment in exc.value.message


class RequiredResult(case_utils.RequiredResult):
    def __init__(self, **kwargs):
        self.clients = kwargs.get('clients')
        self.order_qty_pairs = kwargs.get('order_qty_pairs')
        self.service_id = kwargs.get('service_id')
        self.delay = False
        self.commit = True
        super(RequiredResult, self).__init__(**kwargs)


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    _summary = 'Зачисление сертификатных средств'
    _description = '''
Сервис: {service}
№ РК и количество: {order_qty_pairs}
Комментарий: нужно больше золота!
'''
    issue_key = 'test_cert_consume'

    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def setup_config(self, session, config):
        config['CERTIFICATES_SERVICE_MAP'] = {
            'Баян (77-)': 77,
            'Директ Рекламные кампании (7-)': 7,
            'Медиаселлинг (70-)': 70,
            'Яндекс Баннерокрутилка (67-)': 67,
        }

    def __init__(self):
        super(AbstractDBTestCase, self).__init__()
        self.service = None
        self.order_qty_pairs = None
        self.clients = []


class NeedApproveCase(AbstractDBTestCase):
    _representation = 'need_approve'

    def _get_data(self, session):
        service_code = 7
        client = db_utils.create_client(session)
        client1 = create_inner_client(session, service_id=service_code)
        self.clients_ids = [str(client.id)]

        o1 = db_utils.create_order(session, client=client, service_id=service_code)
        o2 = db_utils.create_order(session, client=client1, service_id=service_code)
        pairs = [
            str(o.service_order_id) + ' - ' + str(qty)
            for o, qty in [(o1, 333), (o2, 111)]
        ]
        self.order_qty_pairs = '\n'.join(pairs)

        self.service = session.query(mapper.Service).getone(service_code)
        self.orders = [(o1, 0), (o2, 0)]
        self.clients = [(client.id, None), (client1.id, 0)]

        return {
            'service': service_map[service_code],
            'order_qty_pairs': self.order_qty_pairs,
        }

    author = 'samvel94'

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.opened,
            clients=self.clients,
            order_qty_pairs=self.orders,
            service_id=self.service.id,
        )
        res.add_message(
            (
                COMMENTS['approve_inner_clients'].format(
                    clients='\n'.join(self.clients_ids),
                    service=self.service.name
                ),
                ['direct_approver1', 'direct_approver2'],
            )
        )
        res.add_message(
            (COMMENTS['clarify_whose_budget'].format(author='samvel94'), ['samvel94'])
        )
        res.delay = True
        return res


class NeedApproveInvalidServiceCase(AbstractDBTestCase):
    _representation = 'need_approve_with_invalid_service'

    def _get_data(self, session):
        service_code = 70
        client = db_utils.create_client(session)
        client1 = create_inner_client(session, service_id=service_code)
        client2 = create_inner_client(session, service_id=7)
        client3 = create_inner_client(session, service_id=67)
        self.clients_ids = [str(client.id), str(client2.id), str(client3.id)]
        self.clients_ids.sort()

        o1 = db_utils.create_order(session, client=client, service_id=service_code)
        o2 = db_utils.create_order(session, client=client1, service_id=service_code)
        o3 = db_utils.create_order(session, client=client2, service_id=service_code)
        o4 = db_utils.create_order(session, client=client3, service_id=service_code)
        pairs = [
            str(o.service_order_id) + ' - ' + str(qty)
            for o, qty in [(o1, 333), (o2, 111), (o3, 333), (o4, 444)]
        ]
        self.order_qty_pairs = '\n'.join(pairs)

        self.service = session.query(mapper.Service).getone(service_code)
        self.orders = [(o1, 0), (o2, 0), (o3, 0), (o4, 0)]
        self.clients = [
            (client.id, None),
            (client1.id, 0),
            (client2.id, None),
            (client3.id, None),
        ]

        return {
            'service': service_map[service_code],
            'order_qty_pairs': self.order_qty_pairs,
        }

    author = 'samvel94'

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.opened,
            clients=self.clients,
            order_qty_pairs=self.orders,
            service_id=self.service.id,
        )
        res.add_message(
            (
                COMMENTS['approve_inner_clients'].format(
                    clients='\n'.join(self.clients_ids),
                    service=self.service.name
                ),
                ['media_approver1'],
            )
        )
        res.add_message(
            (COMMENTS['clarify_whose_budget'].format(author='samvel94'), ['samvel94'])
        )
        res.delay = True
        return res


class HiddenClients(AbstractDBTestCase):
    _representation = 'hidden_clients'

    def _get_data(self, session):
        service_code = 7
        client = create_inner_client(session, service_id=service_code, hidden=1)
        client1 = db_utils.create_client(session)
        self.clients_ids = [str(client.id)]

        o1 = db_utils.create_order(session, client=client, service_id=service_code)
        o2 = db_utils.create_order(session, client=client1, service_id=service_code)
        pairs = [
            str(o.service_order_id) + ' - ' + str(qty)
            for o, qty in [(o1, 111), (o2, 333)]
        ]
        self.order_qty_pairs = '\n'.join(pairs)

        self.service = session.query(mapper.Service).getone(service_code)
        self.orders = [(o1, 0), (o2, 0)]
        self.clients = [(client.id, 1), (client1.id, None)]

        return {
            'service': service_map[service_code],
            'order_qty_pairs': self.order_qty_pairs,
        }

    author = 'samvel94'

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.wont_fix,
            clients=self.clients,
            order_qty_pairs=self.orders,
            service_id=self.service.id,
        )
        res.add_message(
            (
                COMMENTS['hidden_clients'].format(
                    clients='\n'.join(self.clients_ids), service=self.service.name
                ),
                '',
            )
        )
        return res


class HiddenInnerClients(AbstractDBTestCase):
    _representation = 'hidden_inner_clients'

    def _get_data(self, session):
        service_code = 7
        client = create_inner_client(session, service_id=service_code, hidden=1)
        client1 = create_inner_client(session, service_id=service_code)
        self.clients_ids = [str(client.id)]

        o1 = db_utils.create_order(session, client=client, service_id=service_code)
        o2 = db_utils.create_order(session, client=client1, service_id=service_code)
        pairs = [
            str(o.service_order_id) + ' - ' + str(qty)
            for o, qty in [(o1, 111), (o2, 333)]
        ]
        self.order_qty_pairs = '\n'.join(pairs)

        self.service = session.query(mapper.Service).getone(service_code)
        self.orders = [(o1, 0), (o2, 0)]
        self.clients = [(client.id, 1), (client1.id, 0)]

        return {
            'service': service_map[service_code],
            'order_qty_pairs': self.order_qty_pairs,
        }

    author = 'samvel94'

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.wont_fix,
            clients=self.clients,
            order_qty_pairs=self.orders,
            service_id=self.service.id,
        )
        res.add_message(
            (
                COMMENTS['hidden_clients'].format(
                    clients='\n'.join(self.clients_ids), service=self.service.name
                ),
                '',
            )
        )
        return res


class WaitingApprove(AbstractDBTestCase):
    _representation = 'waiting_approve'

    def _get_data(self, session):
        service_code = 7
        client = db_utils.create_client(session)
        client1 = create_inner_client(session, service_id=service_code)
        self.clients_ids = [str(client.id)]
        o1 = db_utils.create_order(session, client=client, service_id=service_code)
        o2 = db_utils.create_order(session, client=client1, service_id=service_code)
        pairs = [
            str(o.service_order_id) + ' - ' + str(random.randint(1, 666))
            for o in [o1, o2]
        ]
        self.order_qty_pairs = '\n'.join(pairs)

        self.service = session.query(mapper.Service).getone(service_code)

        return {
            'service': service_map[service_code],
            'order_qty_pairs': self.order_qty_pairs,
        }

    author = 'samvel94'

    def get_comments(self):
        return [
            (
                'autodasha',
                'https://jing.yandex-team.ru/files/autodasha/approve_button',
                dt.now(),
            )
        ]

    def get_result(self):
        return None


class WaitingTooLonge(AbstractDBTestCase):
    _representation = 'waiting_too_long'

    def _get_data(self, session):
        service_code = 7
        client = db_utils.create_client(session)
        client1 = create_inner_client(session, service_id=service_code)
        self.clients_ids = [str(client.id)]
        o1 = db_utils.create_order(session, client=client, service_id=service_code)
        o2 = db_utils.create_order(session, client=client1, service_id=service_code)
        pairs = [
            str(o.service_order_id) + ' - ' + str(qty)
            for o, qty in [(o1, 111), (o2, 333)]
        ]
        self.order_qty_pairs = '\n'.join(pairs)

        self.orders = [(o1, 0), (o2, 0)]
        self.clients = [(client.id, None), (client1.id, 0)]
        self.service = session.query(mapper.Service).getone(service_code)

        return {
            'service': service_map[service_code],
            'order_qty_pairs': self.order_qty_pairs,
        }

    author = 'samvel94'

    def get_comments(self):
        return [
            (
                'autodasha',
                'https://jing.yandex-team.ru/files/autodasha/approve_button',
                dt.now() - timedelta(22),
            )
        ]

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.wont_fix,
            clients=self.clients,
            order_qty_pairs=self.orders,
            service_id=self.service.id,
        )
        res.add_message((COMMENTS['no_approval'], ''))
        return res


class AuthorIsApprover(AbstractDBTestCase):
    _representation = 'author_is_approver'

    def _get_data(self, session):
        service_code = 7
        client1 = db_utils.create_client(session)
        client2 = db_utils.create_client(session)
        client3 = create_inner_client(session, service_id=service_code)
        self.clients_ids = [str(client1.id), str(client2.id)]
        self.clients_ids.sort()

        o1 = db_utils.create_order(session, client=client1, service_id=service_code)
        o2 = db_utils.create_order(session, client=client2, service_id=service_code)
        o3 = db_utils.create_order(session, client=client3, service_id=service_code)
        pairs = [
            str(o.service_order_id) + ' - ' + str(qty)
            for o, qty in [(o1, 111), (o2, 333), (o3, 444)]
        ]
        self.order_qty_pairs = '\n'.join(pairs)

        self.orders = [(o1, 111), (o2, 333), (o3, 444)]
        self.clients = [(client1.id, 0), (client2.id, 0), (client3.id, 0)]
        self.service = session.query(mapper.Service).getone(service_code)

        return {
            'service': service_map[service_code],
            'order_qty_pairs': self.order_qty_pairs,
        }

    author = 'direct_approver1'

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            clients=self.clients,
            order_qty_pairs=self.orders,
            service_id=self.service.id,
        )
        res.add_message(
            (
                COMMENTS['clarify_whose_budget'].format(
                    author='direct_approver1'
                ),
                ['direct_approver1']

            )
        )
        res.add_message(
            (
                COMMENTS['added_clients'].format(
                    added_clients='\n'.join(self.clients_ids)
                ),
                '',
            )
        )
        order_comments = []
        for order, qty in self.orders:
            qty_name = 'у.е.'
            order_comments.append(
                COMMENTS['resolved'].format(
                    order=order.eid, qty_sum=qty, qty_name=qty_name
                )
            )
        res.add_message(('\n'.join(order_comments), ''))
        return res


class AuthorChangedToApprover(AbstractDBTestCase):
    _representation = 'author_changed_to_approver'

    def _get_data(self, session):
        service_code = 7
        client1 = db_utils.create_client(session)
        client2 = db_utils.create_client(session)
        client3 = create_inner_client(session, service_id=service_code)
        self.clients_ids = [str(client1.id), str(client2.id)]
        self.clients_ids.sort()

        o1 = db_utils.create_order(session, client=client1, service_id=service_code)
        o2 = db_utils.create_order(session, client=client2, service_id=service_code)
        o3 = db_utils.create_order(session, client=client3, service_id=service_code)
        pairs = [
            str(o.service_order_id) + ' - ' + str(qty)
            for o, qty in [(o1, 111), (o2, 333), (o3, 444)]
        ]
        self.order_qty_pairs = '\n'.join(pairs)

        self.orders = [(o1, 111), (o2, 333), (o3, 444)]
        self.clients = [(client1.id, 0), (client2.id, 0), (client3.id, 0)]
        self.service = session.query(mapper.Service).getone(service_code)

        return {
            'service': service_map[service_code],
            'order_qty_pairs': self.order_qty_pairs,
        }

    author = 'direct_approver1'

    def get_comments(self):
        return [
            ('autodasha', (COMMENTS['clarify_whose_budget'].format(author='samvel94'))),
            (
                'autodasha',
                (
                    COMMENTS['approve_inner_clients'].format(
                        clients='\n'.join(self.clients_ids),
                        service=self.service.name
                    )
                ),
            ),
        ]

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            clients=self.clients,
            order_qty_pairs=self.orders,
            service_id=self.service.id,
        )
        res.add_message(
            (
                COMMENTS['added_clients'].format(
                    added_clients='\n'.join(self.clients_ids)
                ),
                '',
            )
        )
        order_comments = []
        for order, qty in self.orders:
            qty_name = 'у.е.'
            order_comments.append(
                COMMENTS['resolved'].format(
                    order=order.eid, qty_sum=qty, qty_name=qty_name
                )
            )
        res.add_message(('\n'.join(order_comments), ''))
        return res


class AuthorIsApproverHiddenClients(AbstractDBTestCase):
    _representation = 'author_is_approver_hidden_clients'

    def _get_data(self, session):
        service_code = 7
        client1 = db_utils.create_client(session)
        client2 = create_inner_client(session, service_id=service_code, hidden=1)
        client3 = create_inner_client(session, service_id=service_code)

        o1 = db_utils.create_order(session, client=client1, service_id=service_code)
        o2 = db_utils.create_order(session, client=client2, service_id=service_code)
        o3 = db_utils.create_order(session, client=client3, service_id=service_code)
        pairs = [
            str(o.service_order_id) + ' - ' + str(qty)
            for o, qty in [(o1, 111), (o2, 333), (o3, 444)]
        ]
        self.order_qty_pairs = '\n'.join(pairs)
        self.clients_ids = [str(client2.id)]
        self.orders = [(o1, 0), (o2, 0), (o3, 0)]
        self.clients = [(client1.id, None), (client2.id, 1), (client3.id, 0)]
        self.service = session.query(mapper.Service).getone(service_code)

        return {
            'service': service_map[service_code],
            'order_qty_pairs': self.order_qty_pairs,
        }

    author = 'igorpro'

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.wont_fix,
            clients=self.clients,
            order_qty_pairs=self.orders,
            service_id=self.service.id,
        )
        res.add_message(
            (
                COMMENTS['hidden_clients'].format(
                    clients='\n'.join(self.clients_ids), service=self.service.name
                ),
                '',
            )
        )
        return res


class Approved(AbstractDBTestCase):
    _representation = 'approved'

    def _get_data(self, session):
        service_code = 7
        client1 = db_utils.create_client(session)
        client2 = db_utils.create_client(session)
        client3 = create_inner_client(session, service_id=service_code)
        self.clients_ids = [str(client1.id), str(client2.id)]
        self.clients_ids.sort()

        o1 = db_utils.create_order(session, client=client1, service_id=service_code)
        o2 = db_utils.create_order(session, client=client2, service_id=service_code)
        o3 = db_utils.create_order(session, client=client3, service_id=service_code)
        pairs = [
            str(o.service_order_id) + ' - ' + str(qty)
            for o, qty in [(o1, 111), (o2, 222), (o3, 333)]
        ]
        self.order_qty_pairs = '\n'.join(pairs)

        self.orders = [(o1, 111), (o2, 222), (o3, 333)]
        # self.clients = [(client1, 0), (client2, 0), (client3, 0)]
        self.clients = [(client1.id, 0), (client2.id, 0), (client3.id, 0)]
        self.clients_for_check = [client1.id, client2.id, client3.id]
        self.service = session.query(mapper.Service).getone(service_code)

        return {
            'service': service_map[service_code],
            'order_qty_pairs': self.order_qty_pairs,
        }

    author = 'samvel94'

    def get_comments(self):
        return [('direct_approver2', 'Подтверждено', dt.now())]

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            clients=self.clients,
            order_qty_pairs=self.orders,
            service_id=self.service.id,
        )
        res.add_message(
            (
                COMMENTS['added_clients'].format(
                    added_clients='\n'.join(self.clients_ids)
                ),
                '',
            )
        )
        order_comments = []
        for order, qty in self.orders:
            qty_name = 'у.е.'
            order_comments.append(
                COMMENTS['resolved'].format(
                    order=order.eid, qty_sum=qty, qty_name=qty_name
                )
            )
        res.add_message(('\n'.join(order_comments), ''))
        return res


class AllInnerClients(AbstractDBTestCase):
    _representation = 'all_inner_clients'

    def _get_data(self, session):
        service_code = 7
        client1 = create_inner_client(session, service_code)
        client2 = create_inner_client(session, service_code)
        client3 = create_inner_client(session, service_code)

        o1 = db_utils.create_order(session, client=client1, service_id=service_code)
        o2 = db_utils.create_order(session, client=client3, service_id=service_code)
        o3 = db_utils.create_order(session, client=client2, service_id=service_code)

        pairs = [
            str(o.service_order_id) + ' - ' + str(qty)
            for o, qty in [(o1, 111), (o2, 222), (o3, 333)]
        ]
        self.order_qty_pairs = '\n'.join(pairs)

        self.orders = [(o1, 111), (o2, 222), (o3, 333)]
        self.clients = [(client1.id, 0), (client2.id, 0), (client3.id, 0)]
        self.service = session.query(mapper.Service).getone(service_code)

        return {
            'service': service_map[service_code],
            'order_qty_pairs': self.order_qty_pairs,
        }

    author = 'samvel94'

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            clients=self.clients,
            order_qty_pairs=self.orders,
            service_id=self.service.id,
        )
        order_comments = []
        for order, qty in self.orders:
            qty_name = 'у.е.'
            order_comments.append(
                COMMENTS['resolved'].format(
                    order=order.eid, qty_sum=qty, qty_name=qty_name
                )
            )
        res.add_message(('\n'.join(order_comments), ''))
        return res


class MainOrderCase(AbstractDBTestCase):
    _representation = 'main_order_case'

    def _get_data(self, session):
        service_code = 7
        client1 = create_inner_client(session, service_code)
        client2 = create_inner_client(session, service_code)
        client3 = create_inner_client(session, service_code)

        main_oder = db_utils.create_order(
            session,
            client=client2,
            service_id=service_code,
            product_id=1475,
            main_order=1,
        )
        o2 = db_utils.create_order(
            session,
            client=client2,
            service_id=service_code,
            product_id=1475,
            group_order_id=main_oder.id,
        )
        o3 = db_utils.create_order(
            session,
            client=client2,
            service_id=service_code,
            product_id=1475,
            group_order_id=main_oder.id,
        )

        main_oder.unmoderated = 1
        o2.unmoderated = 1
        o3.unmoderated = 1

        pairs = [
            str(o.service_order_id) + ' - ' + str(qty)
            for o, qty in [(o2, 222), (o3, 333)]
        ]
        self.order_qty_pairs = '\n'.join(pairs)

        self.orders = [(o2, 222), (o3, 333)]
        self.clients = [(client1.id, 0), (client2.id, 0), (client3.id, 0)]
        self.service = session.query(mapper.Service).getone(service_code)

        return {
            'service': service_map[service_code],
            'order_qty_pairs': self.order_qty_pairs,
        }

    author = 'samvel94'

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed,
                             clients=self.clients,
                             order_qty_pairs=self.orders,
                             service_id=self.service.id)
        order_comments = []
        for order, qty in self.orders:
            qty_name = 'у.е.'
            order_comments.append(COMMENTS['resolved'].format(order=order.eid,
                                                              qty_sum=qty,
                                                              qty_name=qty_name))
        res.add_message(('\n'.join(order_comments), ''))
        return res


class UnmoderatedOrderCase(AbstractDBTestCase):
    _representation = 'unmoderated_order_case'

    def _get_data(self, session):
        service_code = 70
        client2 = create_inner_client(session, service_code)
        client3 = create_inner_client(session, service_code)

        o2 = db_utils.create_order(
            session, client=client2, service_id=service_code, product_id=1475
        )
        o3 = db_utils.create_order(
            session, client=client2, service_id=service_code, product_id=1475
        )

        o2.unmoderated = 1
        o3.unmoderated = 1

        pairs = [
            str(o.service_order_id) + ' - ' + str(qty)
            for o, qty in [(o2, 222), (o3, 333)]
        ]
        self.order_qty_pairs = '\n'.join(pairs)

        self.orders = [(o2, 222), (o3, 333)]
        self.clients = [(client2.id, 0), (client3.id, 0)]
        self.service = session.query(mapper.Service).getone(service_code)

        return {
            'service': service_map[service_code],
            'order_qty_pairs': self.order_qty_pairs,
        }

    author = 'samvel94'

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            clients=self.clients,
            order_qty_pairs=self.orders,
            service_id=self.service.id,
        )

        order_comments = []
        qty_name = 'у.е.'
        for order, qty in self.orders:
            order_comments.append(
                COMMENTS['resolved'].format(order=order.eid, qty_sum=qty, qty_name=qty_name)
            )
        res.add_message(('\n'.join(order_comments), ''))
        return res


class UnexpectedInnerClients(AbstractDBTestCase):
    _representation = 'unexpected_inner_clients'

    def _get_data(self, session):
        service_code = 7
        client1 = create_inner_client(session, service_code)
        client2 = create_inner_client(session, service_code)
        client3 = create_inner_client(session, service_code)

        o1 = db_utils.create_order(session, client=client1, service_id=service_code)
        o2 = db_utils.create_order(session, client=client3, service_id=service_code)
        o3 = db_utils.create_order(session, client=client2, service_id=service_code)

        pairs = [
            str(o.service_order_id) + ' - ' + str(qty)
            for o, qty in [(o1, 111), (o2, 222), (o3, 333)]
        ]
        self.order_qty_pairs = '\n'.join(pairs)

        self.orders = [(o1, 111), (o2, 222), (o3, 333)]
        self.clients = [(client1.id, 0), (client2.id, 0), (client3.id, 0)]
        self.service = session.query(mapper.Service).getone(service_code)

        return {
            'service': service_map[service_code],
            'order_qty_pairs': self.order_qty_pairs,
        }

    author = 'samvel94'

    def get_comments(self):
        return [
            (
                'autodasha',
                'https://jing.yandex-team.ru/files/autodasha/approve_button',
                dt.now() - timedelta(19),
            )
        ]

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            clients=self.clients,
            order_qty_pairs=self.orders,
            service_id=self.service.id,
        )
        order_comments = []
        for order, qty in self.orders:
            qty_name = 'у.е.'
            order_comments.append(
                COMMENTS['resolved'].format(
                    order=order.eid, qty_sum=qty, qty_name=qty_name
                )
            )
        res.add_message(('\n'.join(order_comments), ''))
        return res


class VeryUnexpectedInnerClients(AbstractDBTestCase):
    _representation = 'very_unexpected_inner_clients'

    def _get_data(self, session):
        service_code = 7
        client1 = create_inner_client(session, service_code)
        client2 = create_inner_client(session, service_code)
        client3 = create_inner_client(session, service_code)

        o1 = db_utils.create_order(session, client=client1, service_id=service_code)
        o2 = db_utils.create_order(session, client=client3, service_id=service_code)
        o3 = db_utils.create_order(session, client=client2, service_id=service_code)

        pairs = [
            str(o.service_order_id) + ' - ' + str(qty)
            for o, qty in [(o1, 111), (o2, 222), (o3, 333)]
        ]
        self.order_qty_pairs = '\n'.join(pairs)

        self.orders = [(o1, 111), (o2, 222), (o3, 333)]
        self.clients = [(client1.id, 0), (client2.id, 0), (client3.id, 0)]
        self.service = session.query(mapper.Service).getone(service_code)

        return {
            'service': service_map[service_code],
            'order_qty_pairs': self.order_qty_pairs,
        }

    author = 'samvel94'

    def get_comments(self):
        return [
            (
                'autodasha',
                'https://jing.yandex-team.ru/files/autodasha/approve_button',
                dt.now() - timedelta(25),
            )
        ]

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            clients=self.clients,
            order_qty_pairs=self.orders,
            service_id=self.service.id,
        )
        order_comments = []
        for order, qty in self.orders:
            qty_name = 'у.е.'
            order_comments.append(
                COMMENTS['resolved'].format(
                    order=order.eid, qty_sum=qty, qty_name=qty_name
                )
            )
        res.add_message(('\n'.join(order_comments), ''))
        return res


class ThousandOfProductUnit(AbstractDBTestCase):
    _representation = 'thousand_of_product_unit'

    def _get_data(self, session):
        service_code = 70
        client1 = create_inner_client(session, service_code)
        client2 = create_inner_client(session, service_code)

        o1 = db_utils.create_order(
            session, client=client1, service_id=service_code, product_id=508137
        )
        o2 = db_utils.create_order(
            session, client=client2, service_id=service_code, product_id=503278
        )

        pairs = [
            str(o.service_order_id) + ' - ' + str(qty)
            for o, qty in [(o1, 13457), (o2, 6666666)]
        ]
        self.order_qty_pairs = '\n'.join(pairs)

        self.orders = [(o1, 13457), (o2, 6666666)]
        self.product_qty_names = {
            508137: 'показы',
            503278: 'показы',
        }
        self.clients = [(client1.id, 0), (client2.id, 0)]
        self.service = session.query(mapper.Service).getone(service_code)

        return {
            'service': service_map[service_code],
            'order_qty_pairs': self.order_qty_pairs,
        }

    author = 'samvel94'

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            clients=self.clients,
            order_qty_pairs=self.orders,
            service_id=self.service.id,
        )
        order_comments = []
        for order, qty in self.orders:
            qty_name = self.product_qty_names[order.product.id]
            order_comments.append(
                COMMENTS['resolved'].format(
                    order=order.eid, qty_sum=qty, qty_name=qty_name
                )
            )
        res.add_message(('\n'.join(order_comments), ''))
        return res


class CurrencyProductUnitCase(AbstractDBTestCase):
    _representation = 'currency_product_unit'

    def _get_data(self, session):
        service_code = 70
        client1 = create_inner_client(session, service_code)
        client2 = create_inner_client(session, service_code)

        o1 = db_utils.create_order(
            session, client=client1, service_id=service_code, product_id=506714
        )
        o2 = db_utils.create_order(
            session, client=client2, service_id=service_code, product_id=507356
        )

        pairs = [
            str(o.service_order_id) + ' - ' + str(qty)
            for o, qty in [(o1, 1000), (o2, 10000)]
        ]
        self.order_qty_pairs = '\n'.join(pairs)

        self.orders = [(o1, 1000), (o2, 10000)]
        self.product_qty_names = {
            506714: 'RUB',
            507356: 'EUR',
        }
        self.clients = [(client1.id, 0), (client2.id, 0)]
        self.service = session.query(mapper.Service).getone(service_code)

        return {
            'service': service_map[service_code],
            'order_qty_pairs': self.order_qty_pairs,
        }

    author = 'samvel94'

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            clients=self.clients,
            order_qty_pairs=self.orders,
            service_id=self.service.id,
        )
        order_comments = []
        for order, qty in self.orders:
            qty_name = self.product_qty_names[order.product.id]
            order_comments.append(
                COMMENTS['resolved'].format(
                    order=order.eid, qty_sum=qty, qty_name=qty_name
                )
            )
        res.add_message(('\n'.join(order_comments), ''))
        return res


class IssueAlreadySolved(AbstractDBTestCase):
    _representation = 'issue_already_solved'

    def _get_data(self, session):
        service_code = 70
        client1 = create_inner_client(session, service_code)
        client2 = create_inner_client(session, service_code)

        o1 = db_utils.create_order(
            session, client=client1, service_id=service_code, product_id=506714
        )
        o2 = db_utils.create_order(
            session, client=client2, service_id=service_code, product_id=507356
        )

        pairs = [
            str(o.service_order_id) + ' - ' + str(qty)
            for o, qty in [(o1, 1000), (o2, 10000)]
        ]
        self.order_qty_pairs = '\n'.join(pairs)

        self.orders = [(o1, 0), (o2, 0)]
        self.clients = [(client1.id, 0), (client2.id, 0)]
        self.service = session.query(mapper.Service).getone(service_code)

        return {
            'service': service_map[service_code],
            'order_qty_pairs': self.order_qty_pairs,
        }

    author = 'samvel94'
    last_resolved = True

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.wont_fix,
            clients=self.clients,
            order_qty_pairs=self.orders,
            service_id=self.service.id,
        )
        res.commit = False
        res.add_message((COMMENTS['already_solved'], ''))

        return res


@pytest.mark.parametrize(
    'issue_data',
    [case() for case in AbstractDBTestCase._cases],
    ids=lambda case: str(case),
    indirect=['issue_data'],
)
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data

    req_res = case.get_result()
    solver = CertConsume(queue_object, st_issue)
    res = solver.solve()
    session.flush()

    if req_res is None:
        assert res.commit is True
        assert res.delay is True
        assert res.issue_report is None
        return

    assert res.commit == req_res.commit
    assert res.delay == req_res.delay
    report = res.issue_report

    solver_comments = [
        (comment.text, ','.join(comment.summonees or []))
        for comment in report.comments
    ]

    req_comments = [
        (comment[0], ','.join(comment[1] or []))
        for comment in req_res.comments
    ]

    assert set(solver_comments) == set(req_comments)

    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee

    res = check_client_status(session, req_res.service_id, req_res.clients)
    assert set(req_res.clients) == set(res)

    for order, qty in req_res.order_qty_pairs:
        assert sum([q.consume_qty for q in order.consumes]) == qty
        if qty == 0:
            assert len(order.consumes) == 0
        else:
            assert order.consumes[-1].invoice.paysys.cc == 'ce'


def raise_execution_exception(*args, **kwargs):
    raise ExecutionException(COMMENTS['unknown_error'])


@pytest.mark.parametrize(
    'issue_data',
    [case() for case in [Approved, AuthorChangedToApprover, AuthorIsApprover]],
    ids=lambda case: str(case),
    indirect=['issue_data'],
)
def test_clients_exceptions(session, issue_data):
    queue_object, st_issue, case = issue_data

    req_res = case.get_result()
    solver = CertConsume(queue_object, st_issue)
    with mock.patch(
        'autodasha.solver_cl.CertConsume._add_inner_clients',
        new=raise_execution_exception,
    ):
        res = solver.solve()

    assert not res.commit
    assert not res.delay

    report = res.issue_report
    assert report.assignee == 'mscnad7'
    assert report.transition == ('open', None)
    assert set(
        [
            (comment.text, ','.join(comment.summonees or []))
            for comment in report.comments
        ]
    ) == set([(COMMENTS['unknown_error'], '')])


@pytest.mark.parametrize(
    'issue_data',
    [
        case()
        for case in [
            Approved,
            AuthorChangedToApprover,
            AuthorIsApprover,
            AllInnerClients,
            UnexpectedInnerClients,
            VeryUnexpectedInnerClients,
            ThousandOfProductUnit,
            CurrencyProductUnitCase,
            MainOrderCase
        ]
    ],
    ids=lambda case: str(case),
    indirect=['issue_data'],
)
def test_certificates_exceptions(session, issue_data):
    queue_object, st_issue, case = issue_data

    req_res = case.get_result()
    solver = CertConsume(queue_object, st_issue)
    with mock.patch(
        'autodasha.solver_cl.CertConsume._transfer_certificates',
        new=raise_execution_exception,
    ):
        res = solver.solve()

    assert not res.commit
    assert not res.delay

    report = res.issue_report
    assert report.assignee == 'mscnad7'
    assert report.transition == ('open', None)
    assert set(
        [
            (comment.text, ','.join(comment.summonees or []))
            for comment in report.comments
        ]
    ) == set([(COMMENTS['unknown_error'], '')])
