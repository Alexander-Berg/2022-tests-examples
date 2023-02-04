# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import StringIO
import pytest
import mock
import functools
import datetime as dt
from sqlalchemy import orm

import balance.muzzle_util as ut
from balance import mapper
from balance.xls_export import get_excel_document

from autodasha.solver_cl import CloseInvoiceBaseSolver, \
    CloseInvoice35, CreateCloseInvoice35, \
    ParseException
from autodasha.core.api.tracker import IssueReport
from autodasha.core.api.tracker import IssueTransitions
from autodasha.db import mapper as a_mapper
from autodasha.utils.solver_utils import D

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils
from tests.autodasha_tests.common import db_utils
from tests.autodasha_tests.common import staff_utils

BILLING_SHOP_ID = 35
AUTHOR = 'autodasha'
MONTH = dt.datetime.now()
TEMP_CLOSED = 0
LATEST_CLOSED = MONTH.replace(day=1) - dt.timedelta(days=1)

SOLVER_MAP = {
    'Сформировать акт по счету (35)': CloseInvoice35,
    'Сформировать счет и акт по договору (35)': CreateCloseInvoice35
}

COMMENTS = {
    'get_approve':
        'Это наша точка! Ты на "Подтверждено" нажал - должен был косарь отдать!'
        'https://jing.yandex-team.ru/files/autodasha/approve_button_something.png',
    'already_solved':
        'Эта задача уже была выполнена. Направьте новый запрос через '
        '((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ '
        'формы)). Если не найдёте подходящую, заполните, пожалуйста, общую форму.',
    'parsed_no_product':
        'Не удалось определить продукт. Заполни, пожалуйста, '
        'форму еще раз, указав корректный ID продукта.',
    'parsed_no_act_dt':
        'Не удалось определить дату. Заполни, пожалуйста, '
        'форму еще раз, указав корректную дату акта.',
    'invoice_main_data_not_found':
        'Не удалось определить список счетов и сумм. Заполни, пожалуйста, '
        'форму еще раз, приложив корректный список.',
    'contract_main_data_not_found':
        'Не удалось определить список договоров и сумм. Заполни, пожалуйста, '
        'форму еще раз, приложив корректный список.',
    'wrong_invoice_type':
        'Неправильный тип счета. Счет должен быть либо лицевым, либо предоплатным. '
        'Заполни, пожалуйста, форму еще раз, указав корректный тип счета.',
    'parsed_no_need_reward':
        'Не удалось определить требование расчета премии. '
        'Заполни, пожалуйста, форму еще раз.',
    'parsed_no_approver':
        'Не удалось определить подтверждающего. Заполни, пожалуйста, форму еще раз.',
    'invoice_main_data_parsing_error':
        'Список счетов и сумм заполнен некорректно. Заполни, пожалуйста, '
        'форму еще раз, приложив список корректного формата.',
    'contract_main_data_parsing_error':
        'Список договоров и сумм заполнен некорректно. Заполни, пожалуйста, '
        'форму еще раз, приложив список корректного формата.',
    'many_files':
        'Приложено больше одного файла. Заполни, пожалуйста, '
        'форму еще раз, приложив корректный файл.',
    'incorrect_file_extension':
        'Расширение файла должно быть xls или xlsx. Заполни, пожалуйста, '
        'форму еще раз, приложив корректный файл.',
    'empty_file':
        'Файл пустой. Заполни, пожалуйста, '
        'форму еще раз, приложив корректный файл.',
    'incorrect_caption':
        'В приложенном файле неверно указаны поля шапки. Заполни, пожалуйста, '
        'форму еще раз, приложив корректный файл.',
    'file_is_damaged':
        'Файл поврежден или некорректен. Заполни, пожалуйста, '
        'форму еще раз, приложив корректный файл.',
    'parsed_wrong_invoice_format':
        'Счет {} указан некорректно.',
    'parsed_wrong_order_format':
        'Заказ {} указан некорректно.',
    'parsed_wrong_invoice_act_sum':
        'Сумма {} указана некорректно. Счет: {}.',
    'parsed_wrong_contract_act_sum':
        'Сумма {} указана некорректно. Договор: {}.',
    'product_not_found':
        'Продукт {} не найден в биллинге. Заполни, пожалуйста, '
        'форму еще раз, указав корректный продукт.',
    'invoice_not_found':
        'Счет {} не найден в биллинге.',
    'order_not_found':
        'Заказ {} не найден в биллинге.',
    'product_not_in_order':
        'Продукт {} в заказе {} отличается от продукта, указанного в тикете - {}.',
    'not_one_invoice_order':
        'Невозможно определить нужный заказ по счету {}.',
    'no_active_contract':
        'Действующий договор {} не найден.',
    'multiple_active_contracts':
        'Найдено несколько действующих договоров {}.',
    'multiple_pa_found':
        'По договору {} найдено несколько лицевых счетов.',
    'other_pa_exception':
        'Ошибка во время получения лицевого счета: {}.',
    'error_adding_35_service':
        'Возникла ошибка во время добавления 35 сервиса в договор {}.',
    'error_creating_order_invoice':
        'Ошибка во время создания заказа. Счет {}.',
    'error_creating_invoice':
        'Ошибка во время создания счета. Договор {}.',
    'error_creating_order_contract':
        'Ошибка во время создания заказа. Договор {}.',
    'wrong_number_of_acts':
        'Ошибка. Счет {}, заказ {}. Получился не один акт: {}.',
    'wrong_act_amount':
        'Ошибка. Счет {}, заказ {}. Сумма получившегося акта {} не равна требуемой '
        'сумме {}. Цена фишки - {}; QTY по заказу - {}. '
        'Скорее всего у продукта {} изменилась цена.',
    'different_qtys':
        'Ошибка. Счет {}, заказ {}. Completion_qty по заказу {}, '
        'сумма completion_qty по всем конзюмам заказа {} и '
        'сумма act_qty по всем конзюмам заказа {} не равны между собой.',
    'empty_consume_qty':
        'Ошибка. Счет {}, заказ {}. Сonsume_qty заказа равен 0.',
    'is_closed_period':
        'Период закрыт. Выставление актов невозможно.',
    'waiting':
        'Ждем разморозки выручки.',
    'waiting_time_expired':
        'Не получено подтверждение, посмотри пожалуйста.',
    'partial_failed':
        'Данные строчки не были обработаны.',
    'done':
        'Отчет о сформированных актах во вложении.\n\n'
        'Измененные объекты проставлены в очередь на выгрузку в OEBS.',
    'need_developer':
        '\nПри формировании некоторых актов возникли ошибки. '
        'Задачу посмотрят разработчики.',
    'need_reward':
        '\nКоллеги, по данному тикету требуется расчет премии. '
        'Посмотрите, пожалуйста.'
    }


def _fake_init(self, *args, **kwargs):
    super(CloseInvoiceBaseSolver, self).__init__(*args, **kwargs)
    self.ro_session = self.session


def get_approve_message(*args, **kwargs):
    return (
        'Это наша точка! Ты на "Подтверждено" нажал - должен был косарь отдать!'
        'https://jing.yandex-team.ru/files/autodasha/approve_button_something.png'
    )


class AbstractMockTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'invoice_summary': 'Сформировать акт по счету (35)',
        'contract_summary': 'Сформировать счет и акт по договору (35)',
        'product_id': 'ID продукта:\n%s\r',
        'act_date': 'Дата:\n%s\r',
        'list_of_invoices_and_sums': 'Список счетов и сумм:\n%s\r',
        'list_of_contracts_and_sums': 'Список договоров и сумм:\n%s\r',
        'attachment_path': 'Вложение:\n%s',
        'needed_invoice': 'Счёт необходим:\n%s\r',
        'need_reward': 'Требуется расчет премии:\n%s\r',
        'approver': 'Подтверждающий руководитель:\n%s\r',
        'comment': 'Комментарий:\n%s'
    }


class AbstractParseFailTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        super(AbstractParseFailTestCase, self).__init__()
        self.comments = []

    def add_result_row(self, comment_id, *args, **kwargs):
        self.comments.append(COMMENTS[comment_id].format(*args, **kwargs))

    def prepare_result(self):
        raise NotImplementedError

    def get_result(self):
        req_result = self.prepare_result()
        return req_result, self.comments


class NoProductIdContractTestCase(AbstractParseFailTestCase):
    _representation = 'no_product_id'

    def get_data(self, mock_manager):
        lines = [
            u'ID Продукта:',
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_contracts_and_sums=
                                   '666/13 3.1415926\n1337/00 228'),
            self._get_default_line(needed_invoice='Предоплатный'),
            self._get_default_line(need_reward='Нет'),
            self._get_default_line(approver='Тот Кого Нельзя Называть (tomriddle)'),
            self._get_default_line(comment='Это мой первый тест за долгое время. '
                                           'Не судите строго.')
        ]
        return self._get_default_line('contract_summary'), lines

    def prepare_result(self):
        self.add_result_row('parsed_no_product')


class NoActDtInvoiceTestCase(AbstractParseFailTestCase):
    _representation = 'no_act_dt_invoice'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(list_of_invoices_and_sums=
                                   'ЛСД-420-8 58008\nБ-666-13 1408'),
            self._get_default_line(need_reward='Да'),
            self._get_default_line(approver='Тот Кого Нельзя Критиковать (putin)'),
            self._get_default_line(comment='Это мой второй тест за долгое время. '
                                           'Можете судить, но слегка.')
        ]
        return self._get_default_line('invoice_summary'), lines

    def prepare_result(self):
        self.add_result_row('parsed_no_act_dt')


class NoActDtContractTestCase(AbstractParseFailTestCase):
    _representation = 'no_act_dt_contract'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(list_of_contracts_and_sums=
                                   '666/13 3.1415926\n1337/00 228'),
            self._get_default_line(needed_invoice='Лицевой счет Такси, Корп. такси, Еды'
                                   ),
            self._get_default_line(need_reward='Нет'),
            self._get_default_line(approver='Тот Кого Нельзя Убивать (unicorn)'),
            self._get_default_line(comment='Это мой второй c половиной тест за долгое '
                                           'время. Можете судить, но слегка.')
        ]
        return self._get_default_line('contract_summary'), lines

    def prepare_result(self):
        self.add_result_row('parsed_no_act_dt')


class NoInvoiceDataTestCase(AbstractParseFailTestCase):
    _representation = 'no_invoice_data'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(need_reward='Yes'),
            self._get_default_line(approver='Те Кого Надо Слушаться (parents)'),
            self._get_default_line(comment='Это мой третий тест за долгое время. '
                                           'Становится легче!')
        ]
        return self._get_default_line('invoice_summary'), lines

    def prepare_result(self):
        self.add_result_row('invoice_main_data_not_found')


class NoContractDataTestCase(AbstractParseFailTestCase):
    _representation = 'no_contract_data'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(needed_invoice='Предоплатный'),
            self._get_default_line(need_reward='No'),
            self._get_default_line(approver='Тот Кого Надо Уважать (father)'),
            self._get_default_line(comment='Это мой третий c половиной тест за долгое '
                                           'время. Становится легче!')
        ]
        return self._get_default_line('contract_summary'), lines

    def prepare_result(self):
        self.add_result_row('contract_main_data_not_found')


class WrongInvoiceTypeContractTestCase(AbstractParseFailTestCase):
    _representation = 'wrong_invoice_type'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_contracts_and_sums=
                                   '666/13 3.1415926\n1337/00 228'),
            self._get_default_line(needed_invoice='Бесплатный!'),
            self._get_default_line(need_reward='Да'),
            self._get_default_line(approver='Та Кого Надо Слушаться (mother)'),
            self._get_default_line(comment='Это мой четвертый тест за долгое время. '
                                           'Уже потихоньку набиваю руку.')
        ]
        return self._get_default_line('contract_summary'), lines

    def prepare_result(self):
        self.add_result_row('wrong_invoice_type')


class NoNeedRewardInvoiceTestCase(AbstractParseFailTestCase):
    _representation = 'no_need_reward_invoice'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_invoices_and_sums=
                                   'ЛСД-420-8 58008\nБ-666-13 1408'),
            self._get_default_line(need_reward='\n'),
            self._get_default_line(approver='Те Кого Нельзя Обижать (kids_and_pets)'),
            self._get_default_line(comment='Это мой пятый тест за долгое время. '
                                           'Написан под Монеточку в 3,5 часа ночи.')
        ]
        return self._get_default_line('invoice_summary'), lines

    def prepare_result(self):
        self.add_result_row('parsed_no_need_reward')


class NoNeedRewardContractTestCase(AbstractParseFailTestCase):
    _representation = 'no_need_reward_contract'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_contracts_and_sums=
                                   '666/13 3.1415926\n1337/00 228'),
            self._get_default_line(needed_invoice='Лицевой счет Такси, Корп. такси, Еды'
                                   ),
            self._get_default_line(approver='Тот Кому Надо Давать Пять (bro)'),
            self._get_default_line(comment='Это мой пятый с половиной тест за долгое '
                                           'время. Написан под Billie Eilish '
                                           'в 3,5 часа ночи.')
        ]
        return self._get_default_line('contract_summary'), lines

    def prepare_result(self):
        self.add_result_row('parsed_no_need_reward')


class NoApproverInvoiceTestCase(AbstractParseFailTestCase):
    _representation = 'no_approver_invoice'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_invoices_and_sums=
                                   'ЛСД-420-8 58008\nБ-666-13 1408'),
            self._get_default_line(need_reward='Да'),
            self._get_default_line(comment='Это мой шестой тест за долгое время. '
                                           'Потихоньку становится плохо...')
        ]
        return self._get_default_line('invoice_summary'), lines

    def prepare_result(self):
        self.add_result_row('parsed_no_approver')


class NoApproverContractTestCase(AbstractParseFailTestCase):
    _representation = 'no_approver_contract'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_contracts_and_sums=
                                   '666/13 3.1415926\n1337/00 228'),
            self._get_default_line(needed_invoice='Предоплатный'),
            self._get_default_line(need_reward='Нет'),
            self._get_default_line(comment='Это мой шестой с половиной тест за долгое '
                                           'время. Потихоньку становится плохо...')
        ]
        return self._get_default_line('contract_summary'), lines

    def prepare_result(self):
        self.add_result_row('parsed_no_approver')


class WrongListOfInvoicesTestCase(AbstractParseFailTestCase):
    _representation = 'wrong_list_of_invoices'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_invoices_and_sums='А что сюда нужно писать?'
                                                             '\nНомер счета?\n'
                                                             'Ладно, вот вам ваш '
                                                             'номер договора: 2'),
            self._get_default_line(need_reward='Да'),
            self._get_default_line(approver='То Что Невозможно Смотреть (anime)'),
            self._get_default_line(comment='Это мой седьмой тест за долгое время. '
                                           'С поспал и стало лучше!')
        ]
        return self._get_default_line('invoice_summary'), lines

    def prepare_result(self):
        self.add_result_row('invoice_main_data_parsing_error')


class WrongListOfContractsTestCase(AbstractParseFailTestCase):
    _representation = 'wrong_list_of_contracts'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_contracts_and_sums='А сюда я приложу счет!\n'
                                                              'ХА - ХА - ХА!'),
            self._get_default_line(needed_invoice='Предоплатный'),
            self._get_default_line(need_reward='Нет'),
            self._get_default_line(approver='То Что Невозможно Слушать (k-pop)'),
            self._get_default_line(comment='Надоело.')
        ]
        return self._get_default_line('contract_summary'), lines

    def prepare_result(self):
        self.add_result_row('contract_main_data_parsing_error')


class WrongInvoiceFormatCase(AbstractParseFailTestCase):
    _representation = 'wrong_invoice_format'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_invoices_and_sums=
                                   'НуТипаНеправильныйФорматСчета 666.13'),
            self._get_default_line(need_reward='Да'),
            self._get_default_line(approver='То Что Невозможно Поднять (rub)'),
            self._get_default_line(comment='Когда-нибудь это законится...')
        ]
        return self._get_default_line('invoice_summary'), lines

    def prepare_result(self):
        self.add_result_row('parsed_wrong_invoice_format',
                            'НуТипаНеправильныйФорматСчета')

        return ut.Struct(product_id='112358',
                         act_dt=dt.datetime.strptime('2020-02-29', '%Y-%m-%d'),
                         need_reward=True,
                         approver='rub',
                         main_data=[{'invoice_eid': 'НуТипаНеправильныйФорматСчета',
                                     'act_sum': '666.13'}],
                         invoice_type=None
                         )


class WrongInvoiceActSumFormatCase(AbstractParseFailTestCase):
    _representation = 'wrong_invoice_act_sum'

    def get_data(self, mock_manager):
        mock_utils.create_invoice(mock_manager, external_id='ЛСД-420-8')

        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_invoices_and_sums=
                                   'ЛСД-420-8 ВсюСуммуПожалуйста'),
            self._get_default_line(need_reward='No'),
            self._get_default_line(approver='То Что Невозможно Убить (tardigrade)'),
            self._get_default_line(comment='Когда-нибудь это закончится...')
        ]
        return self._get_default_line('invoice_summary'), lines

    def prepare_result(self):
        self.add_result_row('parsed_wrong_invoice_act_sum', 'ВсюСуммуПожалуйста',
                            'ЛСД-420-8')

        return ut.Struct(product_id='112358',
                         act_dt=dt.datetime.strptime('2020-02-29', '%Y-%m-%d'),
                         need_reward=True,
                         approver='rub',
                         main_data=[{'invoice_eid': 'ЛСД-420-8',
                                     'act_sum': 'ВсюСуммуПожалуйста'}],
                         invoice_type=None
                         )


class WrongContractActSumFormatCase(AbstractParseFailTestCase):
    _representation = 'wrong_contract_act_sum'

    def get_data(self, mock_manager):
        product = mock_utils.create_product(mock_manager, id_='112358')
        col0 = mock_utils.create_contract_collateral(mock_manager, is_signed=True,
                                                     is_faxed=True, is_cancelled=None)
        contract = mock_utils.create_contract(mock_manager, external_id='666/13',
                                              col0=col0, current_signed=lambda: None)

        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_contracts_and_sums=
                                   '666/13 ВсюСуммуПожалуйста'),
            self._get_default_line(needed_invoice='Предоплатный'),
            self._get_default_line(need_reward='No'),
            self._get_default_line(approver='То Что Невозможно Есть (lettuce)'),
            self._get_default_line(comment='Когда-нибудь это закончится...')
        ]
        return self._get_default_line('contract_summary'), lines

    def prepare_result(self):
        self.add_result_row('parsed_wrong_contract_act_sum', 'ВсюСуммуПожалуйста',
                            '666/13')

        return ut.Struct(product_id='112358',
                         act_dt=dt.datetime.strptime('2020-02-29', '%Y-%m-%d'),
                         need_reward=True,
                         approver='lettuce',
                         main_data=[{'contract_eid': '666/13',
                                     'act_sum': 'ВсюСуммуПожалуйста'}],
                         invoice_type='prepayment'
                         )


class WrongOrderFormatCase(AbstractParseFailTestCase):
    _representation = 'wrong_order_format'

    def get_data(self, mock_manager):
        mock_utils.create_invoice(mock_manager, external_id='ЛСД-420-8')
        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_invoices_and_sums=
                                   'ЛСД-420-8 666.13 КакБыНомерЗаказа'),
            self._get_default_line(need_reward='No'),
            self._get_default_line(approver='То Что Невозможно Пить (whiskey)'),
            self._get_default_line(comment='Пожалуйста...')
        ]
        return self._get_default_line('invoice_summary'), lines

    def prepare_result(self):
        self.add_result_row('parsed_wrong_order_format',
                            'КакБыНомерЗаказа', 'ЛСД-420-8')

        return ut.Struct(product_id='112358',
                         act_dt=dt.datetime.strptime('2020-02-29', '%Y-%m-%d'),
                         need_reward=True,
                         approver='rub',
                         main_data=[{'invoice_eid': 'ЛСД-420-8',
                                     'act_sum': '666.13 ',
                                     'order_eid': 'КакБыНомерЗаказа'}],
                         invoice_type=None
                         )


class InvoiceNotFoundCase(AbstractParseFailTestCase):
    _representation = 'invoice_not_found'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_invoices_and_sums=
                                   'ЛСД-420-8 666.13'),
            self._get_default_line(need_reward='No'),
            self._get_default_line(approver='То Что Невозможно Убить (tardigrade)'),
            self._get_default_line(comment='Когда-нибудь это закончится...')
        ]
        return self._get_default_line('invoice_summary'), lines

    def prepare_result(self):
        self.add_result_row('invoice_not_found', 'ЛСД-420-8')

        return ut.Struct(product_id='112358',
                         act_dt=dt.datetime.strptime('2020-02-29', '%Y-%m-%d'),
                         need_reward=True,
                         approver='tardigrade',
                         main_data=[{'invoice_eid': 'ЛСД-420-8',
                                     'act_sum': '666.13'}],
                         invoice_type=None
                         )


class NoContractFoundCase(AbstractParseFailTestCase):
    _representation = 'no_active_contract'

    def get_data(self, mock_manager):
        product = mock_utils.create_product(mock_manager, id_='112358')

        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_contracts_and_sums=
                                   '666/13 3.1415926'),
            self._get_default_line(needed_invoice='Предоплатный'),
            self._get_default_line(need_reward='No'),
            self._get_default_line(approver='То Что Невозможно Есть (lettuce)'),
            self._get_default_line(comment='Когда-нибудь это закончится...')
        ]
        return self._get_default_line('contract_summary'), lines

    def prepare_result(self):
        self.add_result_row('no_active_contract', '666/13')

        return ut.Struct(product_id='112358',
                         act_dt=dt.datetime.strptime('2020-02-29', '%Y-%m-%d'),
                         need_reward=True,
                         approver='lettuce',
                         main_data=[{'contract_eid': '666/13',
                                     'act_sum': '3.1415926'}],
                         invoice_type='prepayment'
                         )


class OrderNotFoundInvoiceCase(AbstractParseFailTestCase):
    _representation = 'order_not_found'

    def get_data(self, mock_manager):
        mock_utils.create_invoice(mock_manager, external_id='ЛСД-420-8')

        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_invoices_and_sums=
                                   'ЛСД-420-8 666.13 35-11111111'),
            self._get_default_line(need_reward='No'),
            self._get_default_line(approver='То Что Невозможно Убить (tardigrade)'),
            self._get_default_line(comment='Когда-нибудь это закончится...')
        ]
        return self._get_default_line('invoice_summary'), lines

    def prepare_result(self):
        self.add_result_row('order_not_found', '35-11111111')

        return ut.Struct(product_id='112358',
                         act_dt=dt.datetime.strptime('2020-02-29', '%Y-%m-%d'),
                         need_reward=True,
                         approver='tardigrade',
                         main_data=[{'invoice_eid': 'ЛСД-420-8',
                                     'act_sum': '666.13', 'order_eid': '35-11111111'}],
                         invoice_type=None
                         )


class ProductNotFoundInvoiceCase(AbstractParseFailTestCase):
    _representation = 'product_not_found_invoice'

    def get_data(self, mock_manager):
        mock_utils.create_invoice(mock_manager, external_id='ЛСД-420-8')

        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_invoices_and_sums=
                                   'ЛСД-420-8 666.13'),
            self._get_default_line(need_reward='No'),
            self._get_default_line(approver='То Что Невозможно Убить (tardigrade)'),
            self._get_default_line(comment='Когда-нибудь это закончится...')
        ]
        return self._get_default_line('invoice_summary'), lines

    def prepare_result(self):
        self.add_result_row('product_not_found', '112358')

        return ut.Struct(product_id='112358',
                         act_dt=dt.datetime.strptime('2020-02-29', '%Y-%m-%d'),
                         need_reward=True,
                         approver='tardigrade',
                         main_data=[{'invoice_eid': 'ЛСД-420-8',
                                     'act_sum': '666.13'}],
                         invoice_type=None
                         )


class ProductNotFoundContractCase(AbstractParseFailTestCase):
    _representation = 'product_not_found_contract'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_contracts_and_sums=
                                   '666/13 3.1415926'),
            self._get_default_line(needed_invoice='Предоплатный'),
            self._get_default_line(need_reward='No'),
            self._get_default_line(approver='То Что Невозможно Есть (lettuce)'),
            self._get_default_line(comment='Когда-нибудь это закончится...')
        ]
        return self._get_default_line('contract_summary'), lines

    def prepare_result(self):
        self.add_result_row('product_not_found', '112358')

        return ut.Struct(product_id='112358',
                         act_dt=dt.datetime.strptime('2020-02-29', '%Y-%m-%d'),
                         need_reward=True,
                         approver='lettuce',
                         main_data=[{'contract_eid': '666/13',
                                     'act_sum': '3.1415926'}],
                         invoice_type='prepayment'
                         )


class ProductNotInOrderInvoiceCase(AbstractParseFailTestCase):
    _representation = 'product_not_in_order_invoice'

    def get_data(self, mock_manager):
        mock_utils.create_invoice(mock_manager, external_id='ЛСД-420-8')
        order = mock_utils.create_order(mock_manager, service_id=35,
                                        service_order_id='11111111')
        order.configure_mock(service_code='666666')

        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_invoices_and_sums=
                                   'ЛСД-420-8 666.13 35-11111111'),
            self._get_default_line(need_reward='No'),
            self._get_default_line(approver='То Что Невозможно Убить (tardigrade)'),
            self._get_default_line(comment='Когда-нибудь это закончится...')
        ]
        return self._get_default_line('invoice_summary'), lines

    def prepare_result(self):
        self.add_result_row('product_not_in_order', '666666', '35-11111111', '112358')

        return ut.Struct(product_id='112358',
                         act_dt=dt.datetime.strptime('2020-02-29', '%Y-%m-%d'),
                         need_reward=True,
                         approver='tardigrade',
                         main_data=[{'invoice_eid': 'ЛСД-420-8',
                                     'act_sum': '666.13', 'order_eid': '35-11111111'}],
                         invoice_type=None
                         )


class ZeroInvoiceOrdersCase(AbstractParseFailTestCase):
    _representation = 'zero_invoice_orders'

    def get_data(self, mock_manager):
        invoice = mock_utils.create_invoice(mock_manager, external_id='ЛСД-420-8')
        invoice.configure_mock(invoice_orders=[])

        lines = [
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_invoices_and_sums=
                                   'ЛСД-420-8 666.13'),
            self._get_default_line(need_reward='No'),
            self._get_default_line(approver='То Что Невозможно Убить (tardigrade)'),
            self._get_default_line(comment='Когда-нибудь это закончится...')
        ]
        return self._get_default_line('invoice_summary'), lines

    def prepare_result(self):
        self.add_result_row('not_one_invoice_order', 'ЛСД-420-8')

        return ut.Struct(act_dt=dt.datetime.strptime('2020-02-29', '%Y-%m-%d'),
                         need_reward=True,
                         approver='tardigrade',
                         main_data=[{'invoice_eid': 'ЛСД-420-8',
                                     'act_sum': '666.13'}],
                         invoice_type=None
                         )


class MoreThanOneInvoiceOrdersCase(AbstractParseFailTestCase):
    _representation = 'more_than_one_invoice_orders'

    def get_data(self, mock_manager):
        invoice = mock_utils.create_invoice(mock_manager, external_id='ЛСД-420-8')
        invoice.configure_mock(invoice_orders=['Мне инвойс_ордер', 'И моему сыну тоже'])

        lines = [
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_invoices_and_sums=
                                   'ЛСД-420-8 666.13'),
            self._get_default_line(need_reward='No'),
            self._get_default_line(approver='То Что Невозможно Убить (tardigrade)'),
            self._get_default_line(comment='Когда-нибудь это закончится...')
        ]
        return self._get_default_line('invoice_summary'), lines

    def prepare_result(self):
        self.add_result_row('not_one_invoice_order', 'ЛСД-420-8')

        return ut.Struct(act_dt=dt.datetime.strptime('2020-02-29', '%Y-%m-%d'),
                         need_reward=True,
                         approver='tardigrade',
                         main_data=[{'invoice_eid': 'ЛСД-420-8',
                                     'act_sum': '666.13'}],
                         invoice_type=None
                         )


@mock.patch('autodasha.solver_cl.close_invoice_35.CloseInvoiceBaseSolver.__init__',
            _fake_init)
@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractParseFailTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_failing(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    ri = IssueReport()

    req_result, req_comment = case.get_result()

    solver = SOLVER_MAP[issue.summary]

    solver_res = solver(mock_queue_object, issue)
    if not req_result:
        with pytest.raises(ParseException) as exc:
            solver_res.parse_issue()
        assert req_comment[0] in exc.value.message
    else:
        parsed_data = solver_res.parse_issue()
        res = solver_res.check_and_get_data(ri, parsed_data.product_id,
                                            parsed_data.main_data,
                                            parsed_data.invoice_type)
        comments = [text.get('text') for text in ri.comments]
        comments = ' '.join(comments)
        assert req_comment[0] == comments


class AbstractParseSuccessTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class ItsAllGoodManInvoiceWithProductTestCase(AbstractParseSuccessTestCase):
    _representation = 'success_invoice_w_product_parse_and_check'

    def get_data(self, mock_manager):
        self.product = mock_utils.create_product(mock_manager, id_='112358')
        self.invoice_ls = mock_utils.create_invoice(mock_manager,
                                                    external_id='ЛСД-420-8')
        self.order_ls = mock_utils.create_order(mock_manager, service_id=35,
                                                service_order_id='111111')
        self.order_ls.configure_mock(service_code=112358, product=self.product)

        self.invoice_b = mock_utils.create_invoice(mock_manager,
                                                   external_id='Б-666-13')
        self.order_b = mock_utils.create_order(mock_manager, service_id=35,
                                               service_order_id='222222')
        self.order_b.configure_mock(service_code='112358')
        self.invoice_b.configure_mock(consumes=[ut.Struct(dt='2020-02-20',
                                                          order=self.order_b)])

        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_invoices_and_sums=
                                   'ЛСД-420-8 58008 35-111111\nБ-666-13 1408'),
            self._get_default_line(need_reward='Да'),
            self._get_default_line(approver='Тот Кого Можно Призвать (robot-octopool)'),
            self._get_default_line(comment='...')
        ]
        return self._get_default_line('invoice_summary'), lines

    def get_result(self):
        return ut.Struct(product_id='112358',
                         act_dt=dt.datetime.strptime('2020-02-29', '%Y-%m-%d'),
                         need_reward=True,
                         approver='robot-octopool',
                         main_data=[{'invoice_eid': 'ЛСД-420-8', 'act_sum': '58008',
                                     'order_eid': '35-111111'},
                                    {'invoice_eid': 'Б-666-13', 'act_sum': '1408'}],
                         invoice_type=None
                         ), \
               [{'product': self.product, 'invoice': self.invoice_ls,
                 'act_sum': D(58008), 'order': self.order_ls
                 },
                {'product': self.product, 'invoice': self.invoice_b,
                 'act_sum': D(1408), 'order': self.order_b}
                ]


class ItsAllGoodManInvoiceWOProductTestCase(AbstractParseSuccessTestCase):
    _representation = 'success_invoice_wo_product_parse_and_check'

    def get_data(self, mock_manager):
        self.product = mock_utils.create_product(mock_manager, id_='112358')
        self.invoice_ls = mock_utils.create_invoice(mock_manager,
                                                    external_id='ЛСД-420-8')
        self.order_ls = mock_utils.create_order(mock_manager, service_id=35,
                                                service_order_id='111111')
        self.order_ls.configure_mock(product=self.product)

        self.invoice_b = mock_utils.create_invoice(mock_manager,
                                                   external_id='Б-666-13')
        self.order_b = mock_utils.create_order(mock_manager, service_id=35,
                                               service_order_id='222222')
        self.order_b.configure_mock(product=self.product)
        self.invoice_b.configure_mock(consumes=[ut.Struct(order=self.order_b)])

        lines = [
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_invoices_and_sums=
                                   'ЛСД-420-8 58008 35-111111\nБ-666-13 1408'),
            self._get_default_line(need_reward='Да'),
            self._get_default_line(approver='Тот Кого Можно Призвать (robot-octopool)'),
            self._get_default_line(comment='...')
        ]
        return self._get_default_line('invoice_summary'), lines

    def get_result(self):
        return ut.Struct(act_dt=dt.datetime.strptime('2020-02-29', '%Y-%m-%d'),
                         need_reward=True,
                         approver='robot-octopool',
                         main_data=[{'invoice_eid': 'ЛСД-420-8', 'act_sum': '58008',
                                     'order_eid': '35-111111'},
                                    {'invoice_eid': 'Б-666-13', 'act_sum': '1408'}],
                         invoice_type=None
                         ), \
               [{'product': self.product, 'invoice': self.invoice_ls,
                 'act_sum': 58008, 'order': self.order_ls
                 },
                {'product': self.product, 'invoice': self.invoice_b,
                 'act_sum': 1408, 'order': self.order_b}
                ]


class ItsAllGoodManContractTestCase(AbstractParseSuccessTestCase):
    _representation = 'success_contract_parse_and_check'

    def get_data(self, mock_manager):
        self.product = mock_utils.create_product(mock_manager, id_='112358')
        col0 = mock_utils.create_contract_collateral(mock_manager, is_signed=True,
                                                     is_faxed=True, is_cancelled=None)
        self.contract1 = mock_utils.create_contract(mock_manager, external_id='666/13',
                                                    col0=col0,
                                                    current_signed=lambda: None)
        self.contract2 = mock_utils.create_contract(mock_manager, external_id='1337/00',
                                                    col0=col0,
                                                    current_signed=lambda: None)

        lines = [
            self._get_default_line(product_id='112358'),
            self._get_default_line(act_date='2020-02-29'),
            self._get_default_line(list_of_contracts_and_sums=
                                   '666/13 31415926\n1337/00 228'),
            self._get_default_line(needed_invoice='Предоплатный'),
            self._get_default_line(need_reward='Нет'),
            self._get_default_line(approver='Тот Кого Можно Призвать (robot-octopool)'),
            self._get_default_line(comment='...')
        ]
        return self._get_default_line('contract_summary'), lines

    def get_result(self):
        return ut.Struct(product_id='112358',
                         act_dt=dt.datetime.strptime('2020-02-29', '%Y-%m-%d'),
                         need_reward=False,
                         approver='robot-octopool',
                         main_data=[{'contract_eid': '666/13', 'act_sum': '31415926'},
                                    {'contract_eid': '1337/00', 'act_sum': '228'}],
                         invoice_type='prepayment'
                         ), \
               [{'product': self.product, 'contract': self.contract1,
                 'act_sum': 31415926, 'invoice_type': 'prepayment'
                 },
                {'product': self.product, 'contract': self.contract2,
                 'act_sum': 228, 'invoice_type': 'prepayment'}]


@mock.patch('autodasha.solver_cl.close_invoice_35.CloseInvoiceBaseSolver.__init__',
            _fake_init)
@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractParseSuccessTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_good(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    ri = IssueReport()
    req_comment = case.get_result()

    solver = SOLVER_MAP[issue.summary]

    solver_res = solver(mock_queue_object, issue)
    parsed_data = solver_res.parse_issue()
    if parsed_data.get('product_id'):
        assert req_comment[0].product_id == parsed_data.product_id

    assert req_comment[0].act_dt == parsed_data.act_dt
    assert req_comment[0].need_reward == parsed_data.need_reward
    assert req_comment[0].approver == parsed_data.approver
    assert req_comment[0].main_data == parsed_data.main_data
    assert req_comment[0].invoice_type == parsed_data.invoice_type

    res = solver_res.check_and_get_data(ri, parsed_data.get('product_id'),
                                        parsed_data.main_data,
                                        parsed_data.invoice_type)

    assert req_comment[1] == res[0]


class RequiredResult(case_utils.RequiredResult):
    def __init__(self, **kwargs):
        self.commit = kwargs.get('commit')
        self.delay = kwargs.get('delay')
        self.summonees = kwargs.get('summonees', [])
        self.assignee = 'autodasha'
        self.objects_for_export = []
        super(RequiredResult, self).__init__(**kwargs)

    @staticmethod
    def get_comment(key, *args, **kwargs):
        return COMMENTS.get(key).format(*args, **kwargs)


class AbstractDBInvoiceTestCase(case_utils.AbstractDBTestCase):
    summary = 'Сформировать акт по счету (35)'
    _description = '''
ID продукта:\n{product_id}
Дата:\n{act_dt}
Список счетов и сумм:\n{invoice_list}
Требуется расчет премии:\n{need_reward}
Подтверждающий руководитель:\n{approver}
Комментарий:\n
'''.strip()

    issue_key = 'test_close_invoice_35'

    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        super(AbstractDBInvoiceTestCase, self).__init__()
        self.config = None
        self._temp_closed = 0

    def setup_config(self, session, config):
        self.config = config
        config['CLOSE_INVOICE_35_SETTINGS'] = {"support_manager": "barsukovpt",
                                               "support_developer": "robot-octopool"}
        config['changeable_acts_dt'] = session.execute(
            '''select value_dt from autodasha.t_config where item = 'LATEST_CLOSED' '''
        ).fetchone()[0]
        config['is_temporary_closed'] = self._temp_closed

    @staticmethod
    def _get_file(main_data):
        buff = StringIO.StringIO()

        titles = ['Счет', 'Сумма', 'Заказ']

        def get_row(md):
            return md.get('invoice_eid'), md.get('act_sum'), md.get('order_eid')

        buff.write(get_excel_document(titles, map(get_row, main_data), 0))
        return buff.getvalue()

    def _get_data(self, meta_session):
        raise NotImplementedError


class AbstractDBContractTestCase(case_utils.AbstractDBTestCase):
    summary = 'Сформировать счет и акт по договору (35)'
    _description = '''
ID продукта:\n{product_id}
Дата:\n{act_dt}
Список договоров и сумм:\n{contract_list}
Счёт необходим:\n{needed_invoice}
Требуется расчет премии:\n{need_reward}
Подтверждающий руководитель:\n{approver}
Комментарий:\n'''.strip()

    issue_key = 'test_create_close_invoice_35'

    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        super(AbstractDBContractTestCase, self).__init__()
        self.config = None
        self._temp_closed = 0

    def setup_config(self, session, config):
        config['CLOSE_INVOICE_35_SETTINGS'] = {"support_manager": "barsukovpt",
                                               "support_developer": "robot-octopool"}
        config['changeable_acts_dt'] = session.execute(
            '''select value_dt from autodasha.t_config where item = 'LATEST_CLOSED' '''
        ).fetchone()[0]
        config['is_temporary_closed'] = self._temp_closed

    @staticmethod
    def _get_file(main_data):
        buff = StringIO.StringIO()

        titles = ['Договор', 'Сумма']

        def get_row(md):
            return md.get('contract_eid'), md.get('act_sum')

        buff.write(get_excel_document(titles, map(get_row, main_data), 0))
        return buff.getvalue()

    def _get_data(self, meta_session):
        raise NotImplementedError


class ManyFilesDBInvoiceTestCase(AbstractDBInvoiceTestCase):
    _representation = 'many_files_invoice'
    _description = '''
ID продукта:\n{product_id}
Дата:\n{act_dt}
Вложение:\n{attachment}
Требуется расчет премии:\n{need_reward}
Подтверждающий руководитель:\n{approver}
Комментарий:\n'''.strip()

    def get_attachments(self):
        return [('data0.xlsx', 'ha'), ('data2.xlsx', 'haha')]

    def _get_data(self, meta_session):
        return {
            'product_id': '112358',
            'act_dt': '2020-02-29',
            'attachment': 'https://pls-help-me-god.xlsx',
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['many_files'])
        res.obj_for_export = False
        return res


class ManyFilesDBContractTestCase(AbstractDBContractTestCase):
    _representation = 'many_files_contract'
    _description = '''
ID продукта:\n{product_id}
Дата:\n{act_dt}
Вложение:\n{attachment}
Счёт необходим:\n{needed_invoice}
Требуется расчет премии:\n{need_reward}
Подтверждающий руководитель:\n{approver}
Комментарий:\n'''.strip()

    def get_attachments(self):
        return [('data0.xlsx', 'ha'), ('data2.xlsx', 'haha')]

    def _get_data(self, meta_session):
        return {
            'product_id': '112358',
            'act_dt': '2020-02-29',
            'attachment': 'https://pls-help-me-god.xlsx',
            'needed_invoice': 'Предоплатный',
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['many_files'])
        res.obj_for_export = False
        return res


class WrongFileExtensionDBInvoiceTestCase(AbstractDBInvoiceTestCase):
    _representation = 'incorrect_file_extension_invoice'
    _description = '''
ID продукта:\n{product_id}
Дата:\n{act_dt}
Вложение:\n{attachment}
Требуется расчет премии:\n{need_reward}
Подтверждающий руководитель:\n{approver}
Комментарий:\n'''.strip()

    def get_attachments(self):
        return [('myvacationpic.jpg', 'hehe')]

    def _get_data(self, meta_session):
        return {
            'product_id': '112358',
            'act_dt': '2020-02-29',
            'attachment': 'https://pls-help-me-god.jpg',
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['invoice_main_data_not_found'])
        res.obj_for_export = False
        return res


class WrongFileExtensionDBContractTestCase(AbstractDBContractTestCase):
    _representation = 'incorrect_file_extension_contract'
    _description = '''
ID продукта:\n{product_id}
Дата:\n{act_dt}
Вложение:\n{attachment}
Счёт необходим:\n{needed_invoice}
Требуется расчет премии:\n{need_reward}
Подтверждающий руководитель:\n{approver}
Комментарий:\n'''.strip()

    def get_attachments(self):
        return [('myvacationpic.jpg', 'hehe')]

    def _get_data(self, meta_session):
        return {
            'product_id': '112358',
            'act_dt': '2020-02-29',
            'attachment': 'https://pls-help-me-god.jpg',
            'needed_invoice': 'Предоплатный',
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['contract_main_data_not_found'])
        res.obj_for_export = False
        return res


class EmptyFileDBInvoiceTestCase(AbstractDBInvoiceTestCase):
    _representation = 'empty_file_invoice'
    _description = '''
ID продукта:\n{product_id}
Дата:\n{act_dt}
Вложение:\n{attachment}
Требуется расчет премии:\n{need_reward}
Подтверждающий руководитель:\n{approver}
Комментарий:\n'''.strip()

    def get_attachments(self):
        main_data = []
        return [('data0.xlsx', self._get_file(main_data))]

    def _get_data(self, meta_session):
        return {
            'product_id': '112358',
            'act_dt': '2020-02-29',
            'attachment': 'https://pls-help-me-god.xlsx',
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['empty_file'])
        res.obj_for_export = False
        return res


class EmptyFileDBContractTestCase(AbstractDBContractTestCase):
    _representation = 'empty_file_contract'
    _description = '''
ID продукта:\n{product_id}
Дата:\n{act_dt}
Вложение:\n{attachment}
Счёт необходим:\n{needed_invoice}
Требуется расчет премии:\n{need_reward}
Подтверждающий руководитель:\n{approver}
Комментарий:\n'''.strip()

    def get_attachments(self):
        main_data = []
        return [('data0.xlsx', self._get_file(main_data))]

    def _get_data(self, meta_session):
        return {
            'product_id': '112358',
            'act_dt': '2020-02-29',
            'attachment': 'https://pls-help-me-god.xlsx',
            'needed_invoice': 'Предоплатный',
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['empty_file'])
        res.obj_for_export = False
        return res


class WrongCaptionDBInvoiceTestCase(AbstractDBInvoiceTestCase):
    _representation = 'incorrect_caption_invoice'
    _description = '''
ID продукта:\n{product_id}
Дата:\n{act_dt}
Вложение:\n{attachment}
Требуется расчет премии:\n{need_reward}
Подтверждающий руководитель:\n{approver}
Комментарий:\n'''.strip()

    @staticmethod
    def _get_file(main_data):
        buff = StringIO.StringIO()

        titles = ['Номер счета',
                  'Сумма (Я указал сумму без НДС, т.к. думаю, '
                  'что у нас все автоматически накинется', 'ID № номер # заказа']

        def get_row(md):
            return md.get('invoice_eid'), md.get('act_sum'), md.get('order_eid')

        buff.write(get_excel_document(titles, map(get_row, main_data), 0))
        return buff.getvalue()

    def get_attachments(self):
        main_data = [{'invoice_eid': 'ЛСД-420-8', 'act_sum': '58008'},
                     {'invoice_eid': 'Б-666-13', 'act_sum': '1408'}]
        return [('data0.xlsx', self._get_file(main_data))]

    def _get_data(self, meta_session):
        return {
            'product_id': '112358',
            'act_dt': '2020-02-29',
            'attachment': 'https://pls-help-me-god.xlsx',
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['incorrect_caption'])
        res.obj_for_export = False
        return res


class WrongCaptionDBContractTestCase(AbstractDBContractTestCase):
    _representation = 'incorrect_caption_contract'
    _description = '''
ID продукта:\n{product_id}
Дата:\n{act_dt}
Вложение:\n{attachment}
Счёт необходим:\n{needed_invoice}
Требуется расчет премии:\n{need_reward}
Подтверждающий руководитель:\n{approver}
Комментарий:\n'''.strip()

    @staticmethod
    def _get_file(main_data):
        buff = StringIO.StringIO()

        titles = ['Номер счета',
                  'Сумма (Я указал сумму без НДС, т.к. думаю, '
                  'что у нас все автоматически накинется', 'ID № номер # заказа']

        def get_row(md):
            return md.get('invoice_eid'), md.get('act_sum'), md.get('order_eid')

        buff.write(get_excel_document(titles, map(get_row, main_data), 0))
        return buff.getvalue()

    def get_attachments(self):
        main_data = [{'contract_eid': '666/13', 'act_sum': '3.1415926'},
                     {'contract_eid': '1337/00', 'act_sum': '228'}]
        return [('data0.xlsx', self._get_file(main_data))]

    def _get_data(self, meta_session):
        return {
            'product_id': '112358',
            'act_dt': '2020-02-29',
            'attachment': 'https://pls-help-me-god.xlsx',
            'needed_invoice': 'Предоплатный',
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['incorrect_caption'])
        res.obj_for_export = False
        return res


class WaitingDBInvoiceTestCase(AbstractDBInvoiceTestCase):
    _representation = 'waiting_unfreeze_db_invoice'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(WaitingDBInvoiceTestCase, self).__init__()
        self._temp_closed = 1
        self._latest_closed = dt_ - dt.timedelta(days=31)
        self._month_dt = dt_

    def setup_config(self, meta_session, config):
        config['LATEST_CLOSED'] = self._latest_closed
        config['TEMP_CLOSED'] = self._temp_closed
        config['changeable_acts_dt'] = self._latest_closed
        config['is_temporary_closed'] = self._temp_closed

        meta_session.execute(
            "update autodasha.t_config set value_num = :val where item = 'TEMP_CLOSED'"
            , {'val': self._temp_closed},
        )

        meta_session.execute(
            "update autodasha.t_config set value_dt = :val where item = 'LATEST_CLOSED'"
            , {'val': self._latest_closed},
        )

    def _get_data(self, meta_session):
        return {
            'product_id': '112358',
            'act_dt': (dt.datetime.now().replace(day=1) -
                       dt.timedelta(days=31)).strftime('%Y-%m-%d'),
            'invoice_list': 'ЛСД-420-8 58008\nБ-666-13 1408',
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.waiting
        res.summonees = 'pepe'
        res.add_message(COMMENTS['waiting'])
        res.obj_for_export = False
        return res


class WaitingDBContractTestCase(AbstractDBContractTestCase):
    _representation = 'waiting_unfreeze_db_contract'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(WaitingDBContractTestCase, self).__init__()
        self._temp_closed = 1
        self._latest_closed = dt_ - dt.timedelta(days=31)
        self._month_dt = dt_

    def setup_config(self, meta_session, config):
        config['LATEST_CLOSED'] = self._latest_closed
        config['TEMP_CLOSED'] = self._temp_closed
        config['changeable_acts_dt'] = self._latest_closed
        config['is_temporary_closed'] = self._temp_closed

        meta_session.execute(
            "update autodasha.t_config set value_num = :val where item = 'TEMP_CLOSED'"
            , {'val': self._temp_closed},
        )

        meta_session.execute(
            "update autodasha.t_config set value_dt = :val where item = 'LATEST_CLOSED'"
            , {'val': self._latest_closed},
        )

    def _get_data(self, meta_session):
        return {
            'product_id': '112358',
            'act_dt': (dt.datetime.now().replace(day=1) -
                       dt.timedelta(days=31)).strftime('%Y-%m-%d'),
            'contract_list': '666/13 3.1415926\n1337/00 228',
            'needed_invoice': 'Предоплатный',
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.waiting
        res.summonees = 'pepe'
        res.add_message(COMMENTS['waiting'])
        res.obj_for_export = False
        return res


class RequestApproveDBInvoiceTestCase(AbstractDBInvoiceTestCase):
    _representation = 'request_approve_db_invoice'

    def _get_data(self, meta_session):
        return {
            'product_id': '112358',
            'act_dt':  dt.datetime.now().strftime('%Y-%m-%d'),
            'invoice_list': 'ЛСД-420-8 58008\nБ-666-13 1408',
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        res.summonees = 'pepe'
        res.add_message(COMMENTS['get_approve'])
        res.obj_for_export = False
        return res


class RequestApproveDBContractTestCase(AbstractDBContractTestCase):
    _representation = 'request_approve_db_contract'

    def _get_data(self, meta_session):
        return {
            'product_id': '112358',
            'act_dt': dt.datetime.now().strftime('%Y-%m-%d'),
            'contract_list': '666/13 3.1415926\n1337/00 228',
            'needed_invoice': 'Предоплатный',
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        res.summonees = 'pepe'
        res.add_message(COMMENTS['get_approve'])
        res.obj_for_export = False
        return res


# class RequestExpiredApproveDBInvoiceTestCase(AbstractDBInvoiceTestCase):
#     _representation = 'request_expired_db_invoice'
#
#     def get_comments(self):
#         return [
#             {
#                 'author': AUTHOR,
#                 'text': COMMENTS['get_approve'],
#                 'summonees': ['pepe'],
#                 'dt': dt.datetime.now() - dt.timedelta(days=15),
#             }
#         ]
#
#     def _get_data(self, meta_session):
#         return {
#             'product_id': '112358',
#             'act_dt': dt.datetime.now().strftime('%Y-%m-%d'),
#             'invoice_list': 'ЛСД-420-8 58008\nБ-666-13 1408',
#             'need_reward': 'Нет',
#             'approver': 'Pepe The Frog (pepe)',
#         }
#
#     def get_result(self):
#         res = RequiredResult(commit=True, delay=False)
#         res.transition = IssueTransitions.none
#         res.summonees = 'pepe'
#         res.add_message(COMMENTS['waiting_time_expired'])
#         res.obj_for_export = False
#         return res
#
#
# class RequestExpiredApproveDBContractTestCase(AbstractDBContractTestCase):
#     _representation = 'request_expired_db_contract'
#
#     def get_comments(self):
#         return [
#             {
#                 'author': AUTHOR,
#                 'text': COMMENTS['get_approve'],
#                 'summonees': ['pepe'],
#                 'dt': dt.datetime.now() - dt.timedelta(days=15),
#             }
#         ]
#
#     def _get_data(self, meta_session):
#         return {
#             'product_id': '112358',
#             'act_dt': dt.datetime.now().strftime('%Y-%m-%d'),
#             'contract_list': '666/13 3.1415926\n1337/00 228',
#             'needed_invoice': 'Предоплатный',
#             'need_reward': 'Нет',
#             'approver': 'Pepe The Frog (pepe)',
#         }
#
#     def get_result(self):
#         res = RequiredResult(commit=True, delay=False)
#         res.transition = IssueTransitions.none
#         res.summonees = 'pepe'
#         res.add_message(COMMENTS['waiting_time_expired'])
#         res.obj_for_export = False
#         return res


class MultipleContractsFoundDBContractTestCase(AbstractDBContractTestCase):
    _representation = 'multiple_contracts_found_db_contract'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['pepe'],
                'dt': dt.datetime.now(),
            },
            ('pepe', 'Подтверждено')
        ]

    def _get_data(self, meta_session):
        client, person = db_utils.create_client_person(meta_session)
        contract1 = db_utils.create_general_contract(meta_session, client, person,
                                                     on_dt=dt.datetime(2020, 1, 1),
                                                     external_id='666666/66')
        contract2 = db_utils.create_general_contract(meta_session, client, person,
                                                     on_dt=dt.datetime(2020, 1, 1),
                                                     external_id='666666/66')
        product_type = db_utils.create_product_type(meta_session)
        product_unit = db_utils.create_product_unit(meta_session,
                                                    product_type_id=product_type.id)
        product = db_utils.create_product(meta_session, id=112358,
                                          unit_id=product_unit.id)
        return {
            'product_id': '112358',
            'act_dt': (dt.datetime.now() + dt.timedelta(days=1)).strftime('%Y-%m-%d'),
            'contract_list': '666666/66 58008',
            'needed_invoice': 'Предоплатный',
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.summonees = 'pepe'
        res.add_message(COMMENTS['multiple_active_contracts'].
                        format('666666/66'))
        res.add_message(COMMENTS['partial_failed'])
        res.obj_for_export = False
        return res


class MultiplePAFoundDBContractTestCase(AbstractDBContractTestCase):
    _representation = 'multiple_pa_found_db_contract'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['pepe'],
                'dt': dt.datetime.now(),
            },
            ('pepe', 'Подтверждено')
        ]

    def _get_data(self, meta_session):
        client, person = db_utils.create_client_person(meta_session)
        contract = db_utils.create_general_contract(meta_session, client, person,
                                                    on_dt=dt.datetime(2020, 1, 1),
                                                    external_id='666666/66')
        product_type = db_utils.create_product_type(meta_session)
        product_unit = db_utils.create_product_unit(meta_session,
                                                    product_type_id=product_type.id)
        product = db_utils.create_product(meta_session, id=112358,
                                          unit_id=product_unit.id)
        tax_policy = db_utils.create_tax_policy(meta_session)
        tax_policy_pct = db_utils.create_tax_policy_pct(meta_session,
                                                        dt=dt.datetime(2020, 1, 1),
                                                        tax_policy_id=tax_policy.id)
        tax_1 = db_utils.create_tax(meta_session, dt=dt.datetime(2020, 1, 1),
                                    product_id=product.id,
                                    tax_policy_id=tax_policy.id)
        price_1 = db_utils.create_price(meta_session, dt=dt.datetime(2020, 1, 1),
                                        product_id=product.id)

        pa1 = db_utils.create_personal_account(meta_session, contract=contract)
        pa2 = db_utils.create_personal_account(meta_session, contract=contract,
                                               paysys_id=1001)
        pa2.paysys = meta_session.query(mapper.Paysys).getone(1003)
        pa2.paysys_id = 1003
        meta_session.flush()

        return {
            'product_id': '112358',
            'act_dt': (dt.datetime.now() + dt.timedelta(days=1)).strftime('%Y-%m-%d'),
            'contract_list': '666666/66 58008',
            'needed_invoice': 'Лицевой счет Такси, Корп. такси, Еды',
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.summonees = 'pepe'
        res.assignee = 'robot-octopool'
        res.add_message(COMMENTS['multiple_pa_found'].
                        format('666666/66'))
        res.add_message(COMMENTS['partial_failed'])
        res.add_message(COMMENTS['need_developer'])
        res.obj_for_export = False
        return res


class ErrorCreatingInvoiceDBContractTestCase(AbstractDBContractTestCase):
    _representation = 'error_creating_invoice_db_contract'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['pepe'],
                'dt': dt.datetime.now(),
            },
            ('pepe', 'Подтверждено')
        ]

    def _get_data(self, meta_session):
        client, person = db_utils.create_client_person(meta_session)
        contract = db_utils.create_general_contract(meta_session, client, person,
                                                    on_dt=dt.datetime(2020, 1, 1),
                                                    external_id='666666/66')
        contract.col0.finish_dt = dt.datetime.now() - dt.timedelta(days=1)
        product_type = db_utils.create_product_type(meta_session)
        product_unit = db_utils.create_product_unit(meta_session,
                                                    product_type_id=product_type.id)
        product = db_utils.create_product(meta_session, id=112358,
                                          unit_id=product_unit.id)
        tax_policy = db_utils.create_tax_policy(meta_session)
        tax_policy_pct = db_utils.create_tax_policy_pct(meta_session,
                                                        dt=dt.datetime(2020, 1, 1),
                                                        tax_policy_id=tax_policy.id)
        tax_1 = db_utils.create_tax(meta_session, dt=dt.datetime(2020, 1, 1),
                                    product_id=product.id,
                                    tax_policy_id=tax_policy.id)
        price_1 = db_utils.create_price(meta_session, dt=dt.datetime(2020, 1, 1),
                                        product_id=product.id)

        return {
            'product_id': '112358',
            'act_dt': (dt.datetime.now() + dt.timedelta(days=1)).strftime('%Y-%m-%d'),
            'contract_list': '666666/66 58008',
            'needed_invoice': 'Предоплатный',
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.summonees = 'pepe'
        res.assignee = 'robot-octopool'
        res.add_message(COMMENTS['error_creating_invoice'].
                        format('666666/66'))
        res.add_message(COMMENTS['partial_failed'])
        res.add_message(COMMENTS['need_developer'])
        res.obj_for_export = False
        return res


class WrongNumberOfActsGeneratedDBInvoiceTestCase(AbstractDBInvoiceTestCase):
    _representation = 'wrong_number_of_acts_generated_db_invoice'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['pepe'],
                'dt': dt.datetime.now(),
            },
            ('pepe', 'Подтверждено')
        ]

    def _get_data(self, meta_session):
        client, person = db_utils.create_client_person(meta_session)
        contract = db_utils.create_general_contract(meta_session, client, person,
                                                    on_dt=dt.datetime(2020, 1, 1))
        product_type = db_utils.create_product_type(meta_session)
        product_unit = db_utils.create_product_unit(meta_session,
                                                    product_type_id=product_type.id)
        product = db_utils.create_product(meta_session, id=112358,
                                          unit_id=product_unit.id)
        tax_policy = db_utils.create_tax_policy(meta_session)
        tax_policy_pct = db_utils.create_tax_policy_pct(meta_session,
                                                        dt=dt.datetime(2020, 1, 1),
                                                        tax_policy_id=tax_policy.id)
        tax = db_utils.create_tax(meta_session, dt=dt.datetime(2020, 1, 1),
                                  product_id=product.id,
                                  tax_policy_id=tax_policy.id)
        price = db_utils.create_price(meta_session, dt=dt.datetime(2020, 1, 1),
                                      product_id=product.id)

        self.order = db_utils.create_order(meta_session, client, service_id=35,
                                           product=product)
        consume_qty = 666

        self.invoice = db_utils.create_invoice(
            meta_session,
            client,
            [(self.order, consume_qty)],
            contract=contract,
            turn_on=True
        )

        return {
            'product_id': '112358',
            'act_dt': (dt.datetime.now() - dt.timedelta(days=1)).strftime('%Y-%m-%d'),
            'invoice_list': '%s 58008' % self.invoice.external_id,
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.summonees = 'pepe'
        res.assignee = 'robot-octopool'
        res.add_message(COMMENTS['wrong_number_of_acts'].
                        format(self.invoice.external_id, '%s-%s' %
                               (self.invoice.invoice_orders[0].order.service_id,
                                self.invoice.invoice_orders[0].order.service_order_id),
                               []))
        res.add_message(COMMENTS['partial_failed'])
        res.add_message(COMMENTS['need_developer'])
        res.obj_for_export = False
        return res


class EmptyConsumeQtyDBInvoiceTestCase(AbstractDBInvoiceTestCase):
    _representation = 'empty_consume_qty_db_invoice'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['pepe'],
                'dt': dt.datetime.now(),
            },
            ('pepe', 'Подтверждено')
        ]

    def _get_data(self, meta_session):
        client, person = db_utils.create_client_person(meta_session)
        contract = db_utils.create_general_contract(meta_session, client, person,
                                                    on_dt=dt.datetime(2020, 1, 1))
        product_type = db_utils.create_product_type(meta_session)
        product_unit = db_utils.create_product_unit(meta_session,
                                                    product_type_id=product_type.id)
        product = db_utils.create_product(meta_session, id=112358,
                                          unit_id=product_unit.id)
        tax_policy = db_utils.create_tax_policy(meta_session)
        tax_policy_pct = db_utils.create_tax_policy_pct(meta_session,
                                                        dt=dt.datetime(2020, 1, 1),
                                                        tax_policy_id=tax_policy.id)
        tax = db_utils.create_tax(meta_session, dt=dt.datetime(2020, 1, 1),
                                  product_id=product.id,
                                  tax_policy_id=tax_policy.id)
        price = db_utils.create_price(meta_session, dt=dt.datetime(2020, 1, 1),
                                      product_id=product.id)

        self.order = db_utils.create_order(meta_session, client, service_id=35,
                                           product=product)
        consume_qty = 666

        self.invoice = db_utils.create_invoice(
            meta_session,
            client,
            [(self.order, consume_qty)],
            contract=contract,
            turn_on=True
        )

        self.order.consume_qty = 0

        return {
            'product_id': '112358',
            'act_dt': (dt.datetime.now() + dt.timedelta(days=1)).strftime('%Y-%m-%d'),
            'invoice_list': '%s 1' % self.invoice.external_id,
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.summonees = 'pepe'
        res.assignee = 'robot-octopool'
        res.add_message(COMMENTS['empty_consume_qty'].
                        format(self.invoice.external_id, '%s-%s' %
                               (self.invoice.invoice_orders[0].order.service_id,
                                self.invoice.invoice_orders[0].order.service_order_id)))
        res.add_message(COMMENTS['partial_failed'])
        res.add_message(COMMENTS['need_developer'])
        res.obj_for_export = False
        return res


class WrongActAmountDBInvoiceTestCase(AbstractDBInvoiceTestCase):
    _representation = 'wrong_act_amount_db_invoice'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['pepe'],
                'dt': dt.datetime.now(),
            },
            ('pepe', 'Подтверждено')
        ]

    def _get_data(self, meta_session):
        client, person = db_utils.create_client_person(meta_session)
        contract = db_utils.create_general_contract(meta_session, client, person,
                                                    on_dt=dt.datetime(2020, 1, 1))
        product_type = db_utils.create_product_type(meta_session)
        product_unit = db_utils.create_product_unit(meta_session,
                                                    product_type_id=product_type.id)
        product = db_utils.create_product(meta_session, id=112358,
                                          unit_id=product_unit.id)
        tax_policy = db_utils.create_tax_policy(meta_session)
        tax_policy_pct = db_utils.create_tax_policy_pct(meta_session,
                                                        dt=dt.datetime(2020, 1, 1),
                                                        tax_policy_id=tax_policy.id)
        tax = db_utils.create_tax(meta_session, dt=dt.datetime(2020, 1, 1),
                                  product_id=product.id,
                                  tax_policy_id=tax_policy.id)
        price = db_utils.create_price(meta_session, dt=dt.datetime(2020, 1, 1),
                                      product_id=product.id)

        self.order = db_utils.create_order(meta_session, client, service_id=35,
                                           product=product)
        consume_qty = 666

        self.invoice = db_utils.create_invoice(
            meta_session,
            client,
            [(self.order, consume_qty)],
            contract=contract,
            turn_on=True
        )

        return {
            'product_id': '112358',
            'act_dt': (dt.datetime.now() + dt.timedelta(days=1)).strftime('%Y-%m-%d'),
            'invoice_list': '%s 666000' % self.invoice.external_id,
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.summonees = 'pepe'
        res.assignee = 'robot-octopool'
        res.add_message(COMMENTS['wrong_act_amount'].
                        format(self.invoice.external_id, '%s-%s' %
                               (self.invoice.invoice_orders[0].order.service_id,
                                self.invoice.invoice_orders[0].order.service_order_id),
                        799.2, 666000, 1.2, 555000, 112358))
        res.add_message(COMMENTS['partial_failed'])
        res.add_message(COMMENTS['need_developer'])
        res.obj_for_export = False
        return res


class SuccessPrepaymentWORewardDBInvoiceTestCase(AbstractDBInvoiceTestCase):
    _representation = 'success_prepayment_wo_reward_db_invoice'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['pepe'],
                'dt': dt.datetime.now(),
            },
            ('pepe', 'Подтверждено')
        ]

    def _get_data(self, meta_session):
        client, person = db_utils.create_client_person(meta_session)
        contract = db_utils.create_general_contract(meta_session, client, person,
                                                    on_dt=dt.datetime(2020, 1, 1))
        product_type = db_utils.create_product_type(meta_session)
        product_unit = db_utils.create_product_unit(meta_session,
                                                    product_type_id=product_type.id)
        product = db_utils.create_product(meta_session, id=112358,
                                          unit_id=product_unit.id)
        tax_policy = db_utils.create_tax_policy(meta_session)
        tax_policy_pct = db_utils.create_tax_policy_pct(meta_session,
                                                        dt=dt.datetime(2020, 1, 1),
                                                        tax_policy_id=tax_policy.id)
        tax = db_utils.create_tax(meta_session, dt=dt.datetime(2020, 1, 1),
                                  product_id=product.id,
                                  tax_policy_id=tax_policy.id)
        price = db_utils.create_price(meta_session, dt=dt.datetime(2020, 1, 1),
                                      product_id=product.id)

        self.order = db_utils.create_order(meta_session, client, service_id=35,
                                           product=product)
        consume_qty = 666

        self.invoice = db_utils.create_invoice(
            meta_session,
            client,
            [(self.order, consume_qty)],
            contract=contract,
            turn_on=True
        )

        return {
            'product_id': '112358',
            'act_dt': (dt.datetime.now() + dt.timedelta(days=1)).strftime('%Y-%m-%d'),
            'invoice_list': '%s 555.6' % self.invoice.external_id,
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.summonees = 'pepe'
        res.assignee = 'autodasha'
        res.add_message(COMMENTS['done'])
        res.obj_for_export = [self.invoice.acts[0]]
        return res


class SuccessPrepaymentWOContractDBInvoiceTestCase(AbstractDBInvoiceTestCase):
    _representation = 'success_prepayment_wo_contract_db_invoice'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['pepe'],
                'dt': dt.datetime.now(),
            },
            ('pepe', 'Подтверждено')
        ]

    def _get_data(self, meta_session):
        client, person = db_utils.create_client_person(meta_session)
        product_type = db_utils.create_product_type(meta_session)
        product_unit = db_utils.create_product_unit(meta_session,
                                                    product_type_id=product_type.id)
        product = db_utils.create_product(meta_session, id=112358,
                                          unit_id=product_unit.id)
        tax_policy = db_utils.create_tax_policy(meta_session)
        tax_policy_pct = db_utils.create_tax_policy_pct(meta_session,
                                                        dt=dt.datetime(2020, 1, 1),
                                                        tax_policy_id=tax_policy.id)
        tax = db_utils.create_tax(meta_session, dt=dt.datetime(2020, 1, 1),
                                  product_id=product.id,
                                  tax_policy_id=tax_policy.id)
        price = db_utils.create_price(meta_session, dt=dt.datetime(2020, 1, 1),
                                      product_id=product.id)

        self.order = db_utils.create_order(meta_session, client, service_id=35,
                                           product=product)
        consume_qty = 666

        self.invoice = db_utils.create_invoice(
            meta_session,
            client,
            [(self.order, consume_qty)],
            person=person,
            turn_on=True
        )

        return {
            'product_id': '112358',
            'act_dt': (dt.datetime.now() + dt.timedelta(days=1)).strftime('%Y-%m-%d'),
            'invoice_list': '%s 555.6' % self.invoice.external_id,
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.summonees = 'pepe'
        res.assignee = 'autodasha'
        res.add_message(COMMENTS['done'])
        res.obj_for_export = [self.invoice.acts[0]]
        return res


class SuccessPAWORewardDBInvoiceTestCase(AbstractDBInvoiceTestCase):
    _representation = 'success_pa_wo_reward_db_invoice'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['pepe'],
                'dt': dt.datetime.now(),
            },
            ('pepe', 'Подтверждено')
        ]

    def _get_data(self, meta_session):
        client, person = db_utils.create_client_person(meta_session)
        contract1 = db_utils.create_general_contract(meta_session, client, person,
                                                    on_dt=dt.datetime(2020, 1, 1))
        contract2 = db_utils.create_general_contract(meta_session, client, person,
                                                     on_dt=dt.datetime(2020, 1, 1))
        product_type = db_utils.create_product_type(meta_session)
        product_unit = db_utils.create_product_unit(meta_session,
                                                    product_type_id=product_type.id)
        product = db_utils.create_product(meta_session, id=112358,
                                          unit_id=product_unit.id)
        tax_policy = db_utils.create_tax_policy(meta_session)
        tax_policy_pct = db_utils.create_tax_policy_pct(meta_session,
                                                        dt=dt.datetime(2020, 1, 1),
                                                        tax_policy_id=tax_policy.id)
        tax = db_utils.create_tax(meta_session, dt=dt.datetime(2020, 1, 1),
                                  product_id=product.id,
                                  tax_policy_id=tax_policy.id)
        price = db_utils.create_price(meta_session, dt=dt.datetime(2020, 1, 1),
                                      product_id=product.id)

        self.invoice1 = db_utils.create_personal_account(meta_session,
                                                         contract=contract1)
        order = db_utils.create_order(meta_session, client, service_id=35,
                                      product=product)
        self.invoice1.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(dt.datetime.now(), {order.shipment_type: 30})
        self.act1, = self.invoice1.generate_act(force=True, backdate=dt.datetime.now())

        self.invoice2 = db_utils.create_personal_account(meta_session,
                                                         contract=contract2)

        return {
            'product_id': '112358',
            'act_dt': (dt.datetime.now() + dt.timedelta(days=1)).strftime(
                '%Y-%m-%d'),
            'invoice_list': '%s 666\n%s 666' % (self.invoice1.external_id,
                                                self.invoice2.external_id),
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.summonees = 'pepe'
        res.assignee = 'autodasha'
        res.add_message(COMMENTS['done'])
        act_to_check, = [a for a in self.invoice1.acts if a != self.act1]
        res.obj_for_export = [act_to_check, self.invoice2.acts[0]]
        return res


class SuccessPrepaymentWithRewardDBInvoiceTestCase(AbstractDBInvoiceTestCase):
    _representation = 'success_prepayment_with_reward_db_invoice'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['pepe'],
                'dt': dt.datetime.now(),
            },
            ('pepe', 'Подтверждено')
        ]

    def _get_data(self, meta_session):
        client, person = db_utils.create_client_person(meta_session)
        contract = db_utils.create_general_contract(meta_session, client, person,
                                                    on_dt=dt.datetime(2020, 1, 1))
        product_type = db_utils.create_product_type(meta_session)
        product_unit = db_utils.create_product_unit(meta_session,
                                                    product_type_id=product_type.id)
        product = db_utils.create_product(meta_session, id=112358,
                                          unit_id=product_unit.id)
        tax_policy = db_utils.create_tax_policy(meta_session)
        tax_policy_pct = db_utils.create_tax_policy_pct(meta_session,
                                                        dt=dt.datetime(2020, 1, 1),
                                                        tax_policy_id=tax_policy.id)
        tax = db_utils.create_tax(meta_session, dt=dt.datetime(2020, 1, 1),
                                  product_id=product.id,
                                  tax_policy_id=tax_policy.id)
        price = db_utils.create_price(meta_session, dt=dt.datetime(2020, 1, 1),
                                      product_id=product.id)

        self.order = db_utils.create_order(meta_session, client, service_id=35,
                                           product=product)
        consume_qty = 666

        self.invoice = db_utils.create_invoice(
            meta_session,
            client,
            [(self.order, consume_qty)],
            contract=contract,
            turn_on=True
        )

        return {
            'product_id': '112358',
            'act_dt': (dt.datetime.now() + dt.timedelta(days=1)).strftime('%Y-%m-%d'),
            'invoice_list': '%s 555.6' % self.invoice.external_id,
            'need_reward': 'Да',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.summonees = 'pepe'
        res.assignee = 'barsukovpt'
        res.add_message(COMMENTS['done'])
        res.add_message(COMMENTS['need_reward'])
        res.obj_for_export = [self.invoice.acts[0]]
        return res


class SuccessPAWithRewardDBInvoiceTestCase(AbstractDBInvoiceTestCase):
    _representation = 'success_pa_with_reward_db_invoice'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['pepe'],
                'dt': dt.datetime.now(),
            },
            ('pepe', 'Подтверждено')
        ]

    def _get_data(self, meta_session):
        client, person = db_utils.create_client_person(meta_session)

        contract1 = db_utils.create_general_contract(meta_session, client, person,
                                                     on_dt=dt.datetime(2020, 1, 1))
        contract2 = db_utils.create_general_contract(meta_session, client, person,
                                                     on_dt=dt.datetime(2020, 1, 1))
        product_type = db_utils.create_product_type(meta_session)
        product_unit = db_utils.create_product_unit(meta_session,
                                                    product_type_id=product_type.id)
        product = db_utils.create_product(meta_session, id=112358,
                                          unit_id=product_unit.id)
        tax_policy = db_utils.create_tax_policy(meta_session)
        tax_policy_pct = db_utils.create_tax_policy_pct(meta_session,
                                                        dt=dt.datetime(2020, 1, 1),
                                                        tax_policy_id=tax_policy.id)
        tax = db_utils.create_tax(meta_session, dt=dt.datetime(2020, 1, 1),
                                  product_id=product.id,
                                  tax_policy_id=tax_policy.id)
        price = db_utils.create_price(meta_session, dt=dt.datetime(2020, 1, 1),
                                      product_id=product.id)

        self.invoice1 = db_utils.create_personal_account(meta_session,
                                                         contract=contract1)
        order = db_utils.create_order(meta_session, client, service_id=35,
                                      product=product)
        self.invoice1.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(dt.datetime.now(), {order.shipment_type: 30})
        self.act1, = self.invoice1.generate_act(force=True, backdate=dt.datetime.now())

        self.invoice2 = db_utils.create_personal_account(meta_session,
                                                         contract=contract2)

        return {
            'product_id': '112358',
            'act_dt': (dt.datetime.now() + dt.timedelta(days=1)).strftime(
                '%Y-%m-%d'),
            'invoice_list': '%s 666\n%s 666' % (self.invoice1.external_id,
                                                self.invoice2.external_id),
            'need_reward': 'Да',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.summonees = 'pepe'
        res.assignee = 'barsukovpt'
        res.add_message(COMMENTS['done'])
        res.add_message(COMMENTS['need_reward'])
        act_to_check, = [a for a in self.invoice1.acts if a != self.act1]
        res.obj_for_export = [act_to_check, self.invoice2.acts[0]]
        return res


class ContractPrepaymentWORewardDBContractTestCase(AbstractDBContractTestCase):
    _representation = 'success_prepayment_wo_reward_db_contract'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['pepe'],
                'dt': dt.datetime.now(),
            },
            ('pepe', 'Подтверждено')
        ]

    def _get_data(self, meta_session):
        client, person = db_utils.create_client_person(meta_session)
        self.contract = db_utils.create_general_contract(meta_session, client, person,
                                                         on_dt=dt.datetime(2020, 1, 1),
                                                         external_id='666666/66')
        product_type = db_utils.create_product_type(meta_session)
        product_unit = db_utils.create_product_unit(meta_session,
                                                    product_type_id=product_type.id)
        product = db_utils.create_product(meta_session, id=112358,
                                          unit_id=product_unit.id)
        tax_policy = db_utils.create_tax_policy(meta_session)
        tax_policy_pct = db_utils.create_tax_policy_pct(meta_session,
                                                        dt=dt.datetime(2020, 1, 1),
                                                        tax_policy_id=tax_policy.id)
        tax = db_utils.create_tax(meta_session, dt=dt.datetime(2020, 1, 1),
                                  product_id=product.id,
                                  tax_policy_id=tax_policy.id)
        price = db_utils.create_price(meta_session, dt=dt.datetime(2020, 1, 1),
                                      product_id=product.id)

        return {
            'product_id': '112358',
            'act_dt': (dt.datetime.now() + dt.timedelta(days=1)).strftime('%Y-%m-%d'),
            'contract_list': '666666/66 666',
            'needed_invoice': 'Предоплатный',
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.summonees = 'pepe'
        res.assignee = 'autodasha'
        res.add_message(COMMENTS['done'])
        res.obj_for_export = [self.contract.invoices[0],
                              self.contract.invoices[0].acts[0]]
        return res


class ContractPrepaymentWithRewardDBContractTestCase(AbstractDBContractTestCase):
    _representation = 'success_prepayment_with_reward_db_contract'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['pepe'],
                'dt': dt.datetime.now(),
            },
            ('pepe', 'Подтверждено')
        ]

    def _get_data(self, meta_session):
        client, person = db_utils.create_client_person(meta_session)
        self.contract = db_utils.create_general_contract(meta_session, client, person,
                                                         on_dt=dt.datetime(2020, 1, 1),
                                                         external_id='666666/66')
        product_type = db_utils.create_product_type(meta_session)
        product_unit = db_utils.create_product_unit(meta_session,
                                                    product_type_id=product_type.id)
        product = db_utils.create_product(meta_session, id=112358,
                                          unit_id=product_unit.id)
        tax_policy = db_utils.create_tax_policy(meta_session)
        tax_policy_pct = db_utils.create_tax_policy_pct(meta_session,
                                                        dt=dt.datetime(2020, 1, 1),
                                                        tax_policy_id=tax_policy.id)
        tax = db_utils.create_tax(meta_session, dt=dt.datetime(2020, 1, 1),
                                  product_id=product.id,
                                  tax_policy_id=tax_policy.id)
        price = db_utils.create_price(meta_session, dt=dt.datetime(2020, 1, 1),
                                      product_id=product.id)

        return {
            'product_id': '112358',
            'act_dt': (dt.datetime.now() + dt.timedelta(days=1)).strftime('%Y-%m-%d'),
            'contract_list': '666666/66 666',
            'needed_invoice': 'Предоплатный',
            'need_reward': 'Да',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.summonees = 'pepe'
        res.assignee = 'barsukovpt'
        res.add_message(COMMENTS['done'])
        res.add_message(COMMENTS['need_reward'])
        res.obj_for_export = [self.contract.invoices[0],
                              self.contract.invoices[0].acts[0]]
        return res


class ContractPAWORewardDBContractTestCase(AbstractDBContractTestCase):
    _representation = 'success_pa_wo_reward_db_contract'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['pepe'],
                'dt': dt.datetime.now(),
            },
            ('pepe', 'Подтверждено')
        ]

    def _get_data(self, meta_session):
        client, person = db_utils.create_client_person(meta_session)
        self.contract = db_utils.create_general_contract(meta_session, client, person,
                                                         on_dt=dt.datetime(2020, 1, 1),
                                                         external_id='666666/66')
        product_type = db_utils.create_product_type(meta_session)
        product_unit = db_utils.create_product_unit(meta_session,
                                                    product_type_id=product_type.id)
        product = db_utils.create_product(meta_session, id=112358,
                                          unit_id=product_unit.id)
        tax_policy = db_utils.create_tax_policy(meta_session)
        tax_policy_pct = db_utils.create_tax_policy_pct(meta_session,
                                                        dt=dt.datetime(2020, 1, 1),
                                                        tax_policy_id=tax_policy.id)
        tax = db_utils.create_tax(meta_session, dt=dt.datetime(2020, 1, 1),
                                  product_id=product.id,
                                  tax_policy_id=tax_policy.id)
        price = db_utils.create_price(meta_session, dt=dt.datetime(2020, 1, 1),
                                      product_id=product.id)

        return {
            'product_id': '112358',
            'act_dt': (dt.datetime.now() + dt.timedelta(days=1)).strftime('%Y-%m-%d'),
            'contract_list': '666666/66 666',
            'needed_invoice': 'Лицевой счет Такси, Корп. такси, Еды',
            'need_reward': 'Нет',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.summonees = 'pepe'
        res.assignee = 'autodasha'
        res.add_message(COMMENTS['done'])
        res.obj_for_export = [self.contract.invoices[0],
                              self.contract.invoices[0].acts[0]]
        return res


class ContractPAtWithRewardDBContractTestCase(AbstractDBContractTestCase):
    _representation = 'success_pa_with_reward_db_contract'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['pepe'],
                'dt': dt.datetime.now(),
            },
            ('pepe', 'Подтверждено')
        ]

    def _get_data(self, meta_session):
        client, person = db_utils.create_client_person(meta_session)
        self.contract = db_utils.create_general_contract(meta_session, client, person,
                                                         on_dt=dt.datetime(2020, 1, 1),
                                                         external_id='666666/66')
        product_type = db_utils.create_product_type(meta_session)
        product_unit = db_utils.create_product_unit(meta_session,
                                                    product_type_id=product_type.id)
        product = db_utils.create_product(meta_session, id=112358,
                                          unit_id=product_unit.id)
        tax_policy = db_utils.create_tax_policy(meta_session)
        tax_policy_pct = db_utils.create_tax_policy_pct(meta_session,
                                                        dt=dt.datetime(2020, 1, 1),
                                                        tax_policy_id=tax_policy.id)
        tax = db_utils.create_tax(meta_session, dt=dt.datetime(2020, 1, 1),
                                  product_id=product.id,
                                  tax_policy_id=tax_policy.id)
        price = db_utils.create_price(meta_session, dt=dt.datetime(2020, 1, 1),
                                      product_id=product.id)

        return {
            'product_id': '112358',
            'act_dt': (dt.datetime.now() + dt.timedelta(days=1)).strftime('%Y-%m-%d'),
            'contract_list': '666666/66 666',
            'needed_invoice': 'Лицевой счет Такси, Корп. такси, Еды',
            'need_reward': 'Да',
            'approver': 'Pepe The Frog (pepe)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.summonees = 'pepe'
        res.assignee = 'barsukovpt'
        res.add_message(COMMENTS['done'])
        res.add_message(COMMENTS['need_reward'])
        res.obj_for_export = [self.contract.invoices[0],
                              self.contract.invoices[0].acts[0]]
        return res


def mock_staff(testfunc):

    other_dick = staff_utils.Person('pepe')
    other_boss = staff_utils.Person('other_boss')
    other_dept = staff_utils.Department('other_dept', [other_boss], [], [other_dick])

    yandex = staff_utils.Department(
        'yandex', childs=[other_dept]
    )

    staff = staff_utils.StaffMock(yandex)

    staff_path = 'autodasha.core.api.staff.Staff.%s'

    @mock.patch(
        staff_path % '_get_person_data',
        lambda s, *a, **k: staff._get_person_data(*a, **k),
    )
    @mock.patch(staff_path % '__init__', lambda *args: None)
    @functools.wraps(testfunc)
    def deco(meta_session, issue_data_meta):
        return testfunc(meta_session, issue_data_meta)

    return deco


@mock_staff
@mock.patch(
    'autodasha.solver_cl.base_solver.BaseSolver.get_approve_message',
    get_approve_message,
)
@pytest.mark.parametrize(
    'issue_data_meta',
    [case() for case in AbstractDBInvoiceTestCase._cases] +
    [case() for case in AbstractDBContractTestCase._cases],
    ids=lambda case: str(case),
    indirect=['issue_data_meta'],
)
def test_db(meta_session, issue_data_meta):
    queue_object, st_issue, case = issue_data_meta
    solver = SOLVER_MAP[st_issue.summary](queue_object, st_issue)

    solver.meta_session = meta_session
    solver.session = meta_session
    solver_res = solver.solve()

    meta_session.flush()
    meta_session.expire_all()

    req_res = case.get_result()
    report = solver_res.issue_report

    assert solver_res.commit == req_res.commit
    assert solver_res.delay == req_res.delay

    report_comments = []
    for c in report.comments:
        c_text = c['text']
        for part in c_text.strip().split('\n'):
            if part:
                report_comments.append(part.strip())

    req_res_comments = []
    for c in req_res.comments:
        for part in c.strip().split('\n'):
            if part:
                req_res_comments.append(part.strip())

    report_comments = sorted(report_comments)
    req_res_comments = sorted(req_res_comments)

    assert len(report_comments) == len(req_res_comments)

    for i in range(len(req_res_comments)):
        assert req_res_comments[i] == report_comments[i]

    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee

    try:
        export_queue = (
            queue_object.issue.session.query(a_mapper.QueueObject)
            .filter(
                a_mapper.QueueObject.object_id == queue_object.issue.id,
                a_mapper.QueueObject.processor == 'EXPORT_CHECK',
            )
            .one()
        )
    except orm.exc.NoResultFound:
        assert not req_res.obj_for_export
    else:
        req_objecs_for_export = {obj.object_id for obj in export_queue.proxies}
        objecs_for_export = {obj.id for obj in req_res.obj_for_export}
        assert req_objecs_for_export == objecs_for_export
