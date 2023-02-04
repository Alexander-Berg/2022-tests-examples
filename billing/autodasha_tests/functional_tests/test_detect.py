# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest

from cluster_tools.autodasha_enqueue import get_solver
from autodasha.solver_cl import *

from tests.autodasha_tests.common import staff_utils
from tests.autodasha_tests.functional_tests.conftest import get_issue
import mock


@pytest.fixture
def issue(request):
    issue_ = get_issue()
    param = globals()[request.param]
    issue_.summary = param.summary
    issue_.description = param.get('description', '')
    issue_.author = param.get('author', '')
    issue_.dt = param.get('dt')
    issue_.status = param.get('status', IssueStatuses.new)
    issue_.comments = param.get('comments')
    return issue_


add_set_markup = ut.Struct(summary='Добавить / проставить наценки')

avg_turnover = ut.Struct(summary='Расчет среднемесячного оборота лояльных клиентов')

bad_debt = ut.Struct(summary='Включить акты в расчет лимита кредита')

blacklisted_users_good = ut.Struct(summary='Непрохождение платежа (blacklisted)',
                                   comments=[])

blacklisted_users_bad = ut.Struct(summary='Непрохождение платежа (blacklisted)',
                                  comments=[{'author': 'autodasha', 'text': 'Бип-боп'}])

buh_delete_v1 = ut.Struct(summary='Удаление логина бухгалтера')
buh_delete_v2 = ut.Struct(summary='Удаление бухгалтерского логина')

change_contract_v1 = ut.Struct(summary='Внесение изменений в договора')
change_contract_v2 = ut.Struct(summary='Внесение изменений в договоры')

cert_consume = ut.Struct(summary='Зачисление сертификатных средств')

change_person = ut.Struct(summary='Изменение плательщика в счетах')

contract_detalisation = ut.Struct(summary='Детализация платежей и комиссии по договорам')

export_contract = ut.Struct(summary='Перевыгрузить Договор и ДС в OEBS')

good_debt = ut.Struct(summary='Исключение актов из расчета лимита кредита')

kopeechki_new = ut.Struct(
    summary='Расхождения на копейки между счетами и актами за 06.06.6666',
    status=IssueStatuses.new
)
kopeechki_need_info = ut.Struct(
    summary='Расхождения на копейки между счетами и актами за 06.06.6666',
    status=IssueStatuses.need_info
)

promo_connect = ut.Struct(summary='Зачисление бонуса по промокоду')

remove_good_debt_v1 = ut.Struct(summary='Разблокировать оплаченные акты')
remove_good_debt_v2 = ut.Struct(summary='Разблокированы оплаченные акты')

return_completed_buh = ut.Struct(summary='Вернула', author='bahira')
return_completed_buh_summary = ut.Struct(summary='вернул долг родине', author='bahira')
return_completed_nebuh = ut.Struct(summary='Вернула', author='nebuh')

return_orderless_v1 = ut.Struct(summary='Снятие средств со счета', comments=[])
return_orderless_v2 = ut.Struct(summary='Снятие средств с предоплатного счета', comments=[])
return_orderless_v3 = ut.Struct(summary='Снятие средств с предоплатного и овердрафтного счета', comments=[])
return_orderless_comment = ut.Struct(
    summary='Снятие средств со счета',
    comments=[{
        'author': 'autodasha',
        'text': 'По счёту есть заявки, где откручено меньше, чем заакчено. Средства вернём вручную.'
    }]
)

return_receipt_v1 = ut.Struct(summary='Возврат средств на заказ по счету')
return_receipt_v2 = ut.Struct(summary='Зачисление средств на заказ по счету')

run_client_batch_v1 = ut.Struct(summary='Запуск расчётов', author='lazareva')
run_client_batch_v2 = ut.Struct(summary='Запуск расчёта', author='lazareva')
run_client_batch_author = ut.Struct(summary='Запуск расчёта', author='hren_s_mountain')

transfer_order_valid_descr = ut.Struct(
    summary='Перенос средств с заказа на заказ',
    description='''Данные по заказам (в вышеописаном формате):
70-666666 70-7777777
Перенос подтверждаю под свою ответственность: True''',
    comments=[]
)
transfer_order_invalid_descr = ut.Struct(
    summary='Перенос средств с заказа на заказ',
    description='''Данные по заказам (в хрен знает каком формате):
абыр-абырвалг горлум-горлум
Перенос подтверждаю под свою ответственность: Не-а''',
    comments=[]
)
transfer_order_commented = ut.Struct(
    summary='Перенос средств с заказа на заказ',
    description='''Данные по заказам (в вышеописаном формате):
70-666666 70-7777777
Перенос подтверждаю под свою ответственность: True''',
    comments=[{'author': 'autodasha', 'text': 'Бип-боп'}]
)

unhide_person_v1 = ut.Struct(
    summary='Разархивирование плательщика',
    description='Название, id плательщика: Иванов Иван Иванович (id 666)'
)
unhide_person_v2 = ut.Struct(
    summary='Разархивировать плательщика',
    description='Название, id плательщика: Иванов Иван Иванович (id 666)'
)
unhide_person_v3 = ut.Struct(
    summary='Разархивировать плательщика',
    description='Название, id плательщика: Гадя Хренова'
)
unhide_person_fail_summary = ut.Struct(
    summary='Разархивировация плательщика',
    description='Название, id плательщика: Гадя Хренова'
)
unhide_person_fail_description = ut.Struct(
    summary='Разархивировать плательщика',
    description='Название плательщика: Гадя Хренова'
)

settings_creation_firm = ut.Struct(
    summary='Заявка на заведение фирмы в Балансе: Яндекс.Российская Империя'
)
settings_creation_service = ut.Struct(
    summary='Заявка на заведение сервиса в Балансе: Яндекс.Дибиллинг'
)

fraud_cleaner = ut.Struct(summary='Мошенническое списание с карты')

manual_docs_removing = ut.Struct(summary='Удалить ручные документы (35)')

promo = ut.Struct(
    summary='Завести промокод Кодзима - Гений'
)

close_invoice_35_v1 = ut.Struct(summary='Сформировать акт по счету (35)')
close_invoice_35_v2 = ut.Struct(summary='Сформировать счет и акт по договору (35)')

reverse_invoice_v1 = ut.Struct(summary='Снять свободные средства по счету')
reverse_invoice_v2 = ut.Struct(summary='Снять свободные средства с заказа по счету')
reverse_invoice_v3 = ut.Struct(summary='Снять свободные средства по списку')

semi_auto_database_modify = ut.Struct(summary='Не so important - modify')

contract_30_70_turn_on = ut.Struct(summary='Зачислить средства на заказ по счету, договор 30/70')

transfer_client_cashback = ut.Struct(summary='Перенос кэшбека - В АД (666)')

promo_change = ut.Struct(summary='Изменить промокод - Зачем')


def mock_staff(testfunc):
    p = staff_utils.Person('bahira')
    d = staff_utils.Department('lala', [], [], [p])

    p1 = staff_utils.Person('nebuh')
    p2 = staff_utils.Person('')
    p3 = staff_utils.Person('lazareva')
    p4 = staff_utils.Person('hren_s_mountain')

    d1 = staff_utils.Department('lala2', [], [], [p1, p2, p3, p4])

    yandex = staff_utils.Department('yandex', [], [], [],
                                    [d, d1])

    staff = staff_utils.StaffMock(yandex)

    staff_path = 'autodasha.core.api.staff.Staff.%s'

    @mock.patch(staff_path % '_get_person_data',
                lambda s, *a, **k: staff._get_person_data(*a, **k))
    @mock.patch(staff_path % 'is_person_related_to_departments',
                lambda s, *a, **k: staff.is_person_related_to_departments(*a, **k))
    @mock.patch(staff_path % '__init__',
                lambda *args: None)
    @functools.wraps(testfunc)
    def deco(issue, req_solver, config):
        return testfunc(issue, req_solver, config)

    return deco


@pytest.mark.parametrize(['issue', 'req_solver'], [
    ('add_set_markup', AddSetMarkup),
    ('avg_turnover', AvgTurnover),
    ('blacklisted_users_good', BlacklistedUsers),
    ('blacklisted_users_bad', None),
    ('buh_delete_v1', BuhDelete),
    ('buh_delete_v2', BuhDelete),
    ('change_contract_v1', ChangeContract),
    ('change_contract_v2', ChangeContract),
    ('change_person', ChangePerson),
    ('cert_consume', CertConsume),
    ('export_contract', ExportContract),
    ('good_debt', GoodDebt),
    ('kopeechki_new', KopeechkiSolver),
    ('kopeechki_need_info', None),
    ('promo_connect', PromoConnect),
    ('remove_good_debt_v1', RemoveGoodDebt),
    ('remove_good_debt_v2', RemoveGoodDebt),
    ('return_completed_buh', ReturnCompleted),
    ('return_completed_buh_summary', ReturnCompleted),
    ('return_completed_nebuh', None),
    ('return_orderless_v1', ReturnOrderless),
    ('return_orderless_v2', ReturnOrderless),
    ('return_orderless_v3', ReturnOrderless),
    ('return_orderless_comment', None),
    ('return_receipt_v1', ReturnReceipt),
    ('return_receipt_v2', ReturnReceipt),
    ('run_client_batch_v1', RunClientBatch),
    ('run_client_batch_v2', RunClientBatch),
    ('run_client_batch_author', None),
    ('transfer_order_valid_descr', TransferOrder),
    ('transfer_order_invalid_descr', None),
    ('transfer_order_commented', None),
    ('unhide_person_v1', UnhidePerson),
    ('unhide_person_v2', UnhidePerson),
    ('unhide_person_v3', UnhidePerson),
    ('unhide_person_fail_summary', None),
    ('unhide_person_fail_description', None),
    ('settings_creation_firm', CreateSettingsFirm),
    ('settings_creation_service', CreateSettingsService),
    ('fraud_cleaner', FraudCleaner),
    ('manual_docs_removing', ManualDocsRemovingSolver),
    ('promo', Promo),
    ('contract_detalisation', ContractDetalisation),
    ('close_invoice_35_v1', CloseInvoice35),
    ('close_invoice_35_v2', CreateCloseInvoice35),
    ('reverse_invoice_v1', ReverseInvoice),
    ('reverse_invoice_v2', ReverseInvoice),
    ('reverse_invoice_v3', ReverseInvoice),
    ('semi_auto_database_modify', SemiAutoDatabaseModifySolver),
    ('contract_30_70_turn_on', Contract3070InvoiceTurnOnSolver),
    ('transfer_client_cashback', TransferClientCashbackSolver),
    ('promo_change', PromoChangeSolver)
], indirect=['issue'])
@mock_staff
def test_detect(issue, req_solver, config):
    detected_solver = get_solver(issue, config)
    assert detected_solver is req_solver
