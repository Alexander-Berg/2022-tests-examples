# -*- coding: utf-8 -*-

import pytest
from hamcrest import equal_to, is_not, contains_string

import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance import balance_web, balance_db as db
from balance.features import Features
from btestlib.constants import Permissions, PersonTypes, Paysyses, Services, Products, User, Regions, Currencies
from btestlib.matchers import matches_in_time, contains_dicts_with_entries
import btestlib.config as balance_config

# Проблемы тестов на права:
#     1. Набор прав логина где-то кэшируется (в интерфейсе?) и после добавления права оно начинает
#         применяться где-то через 5 минут (удаление права применяется сразу)
#         Фактически эта проблема актуальна только при создании нового теста.
#     2. Если в тесте мы проверяем что какое-то действие выполняется под правами 1,2,3,
#         а потом право 3 перестало работать и теперь действие можно совершить под правами 1,2,
#         тест все равно будет выполнять действие с правами 1,2,3 и никаких ошибок не будет


pytestmark = [
    pytest.mark.priority('mid'),
    reporter.feature(Features.ROLE, Features.PERMISSION, Features.UI),
    pytest.mark.tickets('BALANCE-22650'),
]

SHOULD_BE_INVALIDATED = True

# ----------------------------   CreateBankPayments (3)  ----------------------------


def test_payment_btn_on_invoice_with_perm_3(get_free_user):
    # user = User(426177991, 'yb-atst-custom-role-1')
    user = get_free_user()

    steps.UserSteps.set_role_with_permissions_strict(user,
                                                     [Permissions.ADMIN_ACCESS_0, Permissions.CREATE_BANK_PAYMENTS_3])

    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    invoice_id, _, invoice_sum = _create_invoice(client_id)

    with reporter.step(u'Проверяем наличие и работоспособность кнопки Внести оплату на странице счета '
                       u'(под правом CreateBankPayments)'):
        with balance_web.Driver(user=user) as driver:
            invoice_page = balance_web.AdminInterface.InvoicePage.open(driver, invoice_id)
            # Не проверяем, так как может не успеть загрузиться. Явно ждём элемент уже в confirm_payment()
            # utils.check_that(invoice_page.is_confirm_payment_button_present(), equal_to(True),
            #                  step=u'Проверяем наличие кнопки Внести оплату на странице счета',
            #                  error=u'Не найдена кнопка Внести оплату на странице счета')
            invoice_page.confirm_payment()

        if balance_config.ENABLE_SINGLE_ACCOUNT:
            invoice_id = db.get_invoice_by_charge_note_id(invoice_id)[0]['id']

        utils.check_that(lambda: _get_receipt_sum_db(invoice_id),
                         matches_in_time(is_not(equal_to(0)), timeout=60),
                         step=u'Проверяем, что оплата зачислилась на счет',
                         error=u'Оплата не зачислилась на счет')

        utils.check_that(_get_receipt_sum_db(invoice_id), equal_to(invoice_sum),
                         step=u'Проверяем, что на счет зачислилась оплата равная сумме счета',
                         error=u'На счет зачислилась неверная сумма оплаты')


def test_payment_btn_on_invoice_without_perm_3(data_cache, get_free_user):
    # user = User(428034142, 'yb-atst-custom-role-2')
    user = get_free_user()

    perm_ids = set(steps.PermissionsSteps.get_all_permission_ids()) - {Permissions.REDIRECT_TO_YANDEX_TEAM_36,
                                                                       Permissions.CREATE_BANK_PAYMENTS_3}

    steps.UserSteps.set_role_with_permissions_strict(user, perm_ids)

    with utils.CachedData(data_cache, ['client_id', 'invoice_id'], force_invalidate=SHOULD_BE_INVALIDATED) as c:
        if not c: raise utils.SkipContextManagerBodyException()
        client_id = steps.ClientSteps.create()
        invoice_id, _, _ = _create_invoice(client_id)

    steps.ClientSteps.link(client_id, user.login)

    with reporter.step(u'Проверяем отсутствие кнопки Внести оплату на странице счета (без права CreateBankPayments)'):
        with balance_web.Driver(user=user) as driver:
            invoice_page = balance_web.AdminInterface.InvoicePage.open(driver, invoice_id)
            utils.check_that(invoice_page.is_confirm_payment_button_present(), equal_to(False),
                             step=u'Проверяем отсутствие кнопки Внести оплату',
                             error=u'Найдена кнопка Внести оплату на странице счета')


def test_payment_btn_on_invoices_search_page_with_perm_3(get_free_user):
    # user = User(428034161, 'yb-atst-custom-role-3')
    user = get_free_user()

    steps.UserSteps.set_role_with_permissions_strict(user,
                                                     [Permissions.ADMIN_ACCESS_0, Permissions.CREATE_BANK_PAYMENTS_3])

    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    invoice_id, invoice_external_id, invoice_sum = _create_invoice(client_id)

    with reporter.step(u'Проверяем наличие и работоспособность кнопки Оплата на странице поиска счетов '
                       u'(под правом CreateBankPayments)'):
        with balance_web.Driver(user=user) as driver:
            invoices_page = balance_web.AdminInterface.InvoicesSearchPage.open(driver, invoice_eid=invoice_external_id)
            # Не проверяем, так как может не успеть загрузиться. Явно ждём элемент уже в confirm_payment()
            # utils.check_that(invoices_page.is_confirm_payment_button_present(invoice_id), equal_to(True),
            #                  step=u'Проверяем наличие кнопки Оплата у счета',
            #                  error=u'Не найдена кнопка Оплата у счета')
            invoices_page.confirm_payment(invoice_id)

        if balance_config.ENABLE_SINGLE_ACCOUNT:
            invoice_id = db.get_invoice_by_charge_note_id(invoice_id)[0]['id']

        utils.check_that(lambda: _get_receipt_sum_db(invoice_id),
                         matches_in_time(is_not(equal_to(0)), timeout=60),
                         step=u'Проверяем, что оплата зачислилась на счет',
                         error=u'Оплата не зачислилась на счет')

        utils.check_that(_get_receipt_sum_db(invoice_id), equal_to(invoice_sum),
                         step=u'Проверяем, что на счет зачислилась оплата равная сумме счета',
                         error=u'На счет зачислилась неверная сумма оплаты')


def test_payment_btn_on_invoices_search_page_without_perm_3(data_cache, get_free_user):
    # user = User(428034175, 'yb-atst-custom-role-4')
    user = get_free_user()

    perm_ids = set(steps.PermissionsSteps.get_all_permission_ids()) - {Permissions.REDIRECT_TO_YANDEX_TEAM_36,
                                                                       Permissions.CREATE_BANK_PAYMENTS_3}

    steps.UserSteps.set_role_with_permissions_strict(user, perm_ids)

    with utils.CachedData(data_cache, ['client_id', 'invoice_id', 'invoice_external_id'],
                          force_invalidate=SHOULD_BE_INVALIDATED) as c:
        if not c: raise utils.SkipContextManagerBodyException()
        client_id = steps.ClientSteps.create()
        invoice_id, invoice_external_id, _ = _create_invoice(client_id)

    steps.ClientSteps.link(client_id, user.login)

    with reporter.step(u'Проверяем отсутствие кнопки Оплата на странице поиска счетов (без права CreateBankPayments)'):
        with balance_web.Driver(user=user) as driver:
            invoices_page = balance_web.AdminInterface.InvoicesSearchPage.open(driver, invoice_eid=invoice_external_id)
            utils.check_that(invoices_page.is_confirm_payment_button_present(invoice_id), equal_to(False),
                             step=u'Проверяем отсутствие кнопки Оплата у счета',
                             error=u'Найдена кнопка Оплата у счета')


# ----------------------------   ManageBadDebts (21)  ----------------------------


def test_bad_debt_btn_with_perm_21(get_free_user):
    # user = User(428034189, 'yb-atst-custom-role-5')
    user = get_free_user()

    steps.UserSteps.set_role_with_permissions_strict(user,
                                                     [Permissions.ADMIN_ACCESS_0, Permissions.MANAGE_BAD_DEBTS_21])

    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    invoice_id, act_id = _create_invoice_and_act(client_id)

    with reporter.step(
            u'Проверяем наличие и работоспособность кнопки Плохой долг на странице счета (под правом ManageBadDebts)'):
        with balance_web.Driver(user=user) as driver:
            invoice_page = balance_web.AdminInterface.InvoicePage.open(driver, invoice_id)
            utils.check_that(lambda: invoice_page.is_set_bad_debt_button_present(),
                             matches_in_time(equal_to(True), timeout=60),
                             step=u'Проверяем наличие кнопки Плохой долг на странице счета',
                             error=u'Не найдена кнопка Плохой долг на странице счета')
            invoice_page.set_bad_debt()

        utils.check_that(lambda: steps.BadDebtSteps.is_bad_debt(act_id),
                         matches_in_time(equal_to(True), timeout=60),
                         step=u'Проверяем, что акт по счету признан плохим долгом',
                         error=u'Акт по счету не признан плохим долгом')


def test_bad_debt_btn_without_perm_21(data_cache, get_free_user):
    # user = User(430523060, 'yb-atst-custom-role-6')
    user = get_free_user()

    perm_ids = set(steps.PermissionsSteps.get_all_permission_ids()) - {Permissions.REDIRECT_TO_YANDEX_TEAM_36,
                                                                       Permissions.MANAGE_BAD_DEBTS_21}
    steps.UserSteps.set_role_with_permissions_strict(user, perm_ids)

    with utils.CachedData(data_cache, ['client_id', 'invoice_id'], force_invalidate=SHOULD_BE_INVALIDATED) as c:
        if not c: raise utils.SkipContextManagerBodyException()
        client_id = steps.ClientSteps.create()
        invoice_id, act_id = _create_invoice_and_act(client_id)

    steps.ClientSteps.link(client_id, user.login)

    with reporter.step(u'Проверяем отсутствие кнопки Плохой долг на странице счета (без права ManageBadDebts)'):
        with balance_web.Driver(user=user) as driver:
            invoice_page = balance_web.AdminInterface.InvoicePage.open(driver, invoice_id)
            utils.check_that(lambda: invoice_page.is_set_bad_debt_button_present(),
                             matches_in_time(equal_to(False), timeout=60),
                             step=u'Проверяем отсутствие кнопки Плохой долг',
                             error=u'Найдена кнопка Плохой долг на странице счета')


# ----------------------------   AdditionalFunctions (47)  ----------------------------

def set_role_without_perm_47(user, new_ui=False):
    exclude_perms = {Permissions.REDIRECT_TO_YANDEX_TEAM_36, Permissions.ADDITIONAL_FUNCTIONS_47}
    if not new_ui:
        exclude_perms.update({Permissions.NEW_UI_EARLY_64})
    perm_ids = set(steps.PermissionsSteps.get_all_permission_ids()) - exclude_perms
    steps.UserSteps.set_role_with_permissions_strict(user, perm_ids)


@pytest.mark.parametrize(
    'field_name, is_field_present, user', [
        (u'Регион', lambda page: page.is_region_select_present(),
         User(430523088, 'yb-atst-custom-role-7')),
        (u'Не должен получать овердрафт', lambda page: page.is_deny_overdraft_checkbox_present(),
         User(455291197, 'yb-atst-custom-role-13')),
        (u'Выставление счетов по оферте', lambda page: page.is_force_contractless_invoice_checkbox_present(),
         User(455291206, 'yb-atst-custom-role-14'))
    ],
    ids=['region', 'deny_overdraft', 'force_contractless_invoice']
)
def test_edit_client_fields_absence_without_perm_47(field_name, is_field_present, user, get_free_user):
    user = get_free_user()

    set_role_without_perm_47(user)
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    with balance_web.Driver(user) as driver:
        client_page = balance_web.AdminInterface.ClientEditPage.open(driver, client_id)
        utils.check_that(is_field_present(client_page), equal_to(False),
                         step=u"Проверяем отсутствие поля '{}' на странице редактирования клиента "
                              u"(без права AdditionalFunctions)".format(field_name),
                         error=u"Найдено поле '{}'".format(field_name))


@pytest.mark.parametrize(
    "field_name, field_name_db, set_field, user", [
        (u'Нерезидент', 'is_non_resident', lambda page: page.set_non_resident_fields(Currencies.RUB.char_code),
         User(455291222, 'yb-atst-custom-role-15')),
        (u'Проверка доменов', 'domain_check_status', lambda page: page.set_domain_check_status(1),
         User(455291229, 'yb-atst-custom-role-16'))
    ],
    ids=lambda field_name, field_name_db, set_field, user: field_name_db
)
def test_edit_client_web_error_without_perm_47(field_name, field_name_db, set_field, user, get_free_user):
    user = get_free_user()

    set_role_without_perm_47(user)
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    with balance_web.Driver(user) as driver:
        client_page = balance_web.AdminInterface.ClientEditPage.open(driver, client_id)
        set_field(client_page)
        with pytest.raises(utils.ServiceError) as excinfo:
            client_page.save_client()
    _check_error_text_on_page(excinfo, 'no permission AdditionalFunctions')
    value_db = db.balance().execute(
        'select {field_name_db} from t_client where id = {client_id}'.format(field_name_db=field_name_db,
                                                                             client_id=client_id),
        single_row=True)[field_name_db]
    utils.check_that(value_db, equal_to(0),
                     step=u"Проверяем, что признак '{}' у клиент не изменился".format(field_name))


def set_role_with_perms_to_edit_client(user):
    steps.UserSteps.set_role_with_permissions_strict(user, [Permissions.ADMIN_ACCESS_0,
                                                            Permissions.MANAGERS_OPERATIONS_23,
                                                            Permissions.EDIT_CLIENT_28,
                                                            Permissions.ADDITIONAL_FUNCTIONS_47])


@pytest.mark.parametrize(
    "field_name, field_name_db, set_field, expected_value, user", [
        (u'Регион', 'region_id', lambda page: page.set_region(Regions.RU.id), Regions.RU.id,
         User(430523115, 'yb-atst-custom-role-8')),
        (u'Нерезидент', 'is_non_resident', lambda page: page.set_non_resident_fields(Currencies.RUB.char_code), 1,
         User(455291235, 'yb-atst-custom-role-17')),
        (u'Проверка доменов', 'domain_check_status', lambda page: page.set_domain_check_status(1), 1,
         User(455291245, 'yb-atst-custom-role-18'))
    ],
    ids=lambda field_name, field_name_db, set_field, expected_value, user: field_name_db
)
def test_edit_client_with_needed_permissions(field_name, field_name_db, set_field, expected_value, user, get_free_user):
    user = get_free_user()

    set_role_with_perms_to_edit_client(user)
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    with reporter.step(u"Изменяем признак '{}' на странице редактирования клиента".format(field_name)):
        with balance_web.Driver(user) as driver:
            client_page = balance_web.AdminInterface.ClientEditPage.open(driver, client_id)
            set_field(client_page)
            client_page.save_client()
    value_db = db.balance().execute(
        'select {field_name_db} from t_client where id = {client_id}'.format(field_name_db=field_name_db,
                                                                             client_id=client_id),
        single_row=True)[field_name_db]
    utils.check_that(value_db, equal_to(expected_value),
                     step=u"Проверяем, что признак '{}' изменился".format(field_name),
                     error=u"Признак '{}' не изменился".format(field_name))


@pytest.mark.parametrize(
    "field_name, field_name_db, set_field, user", [
        (u'Не должен получать овердрафт', 'deny_overdraft', lambda page: page.set_deny_overdraft(),
         User(455291254, 'yb-atst-custom-role-19')),
        (u'Выставление счетов по оферте', 'force_contractless_invoice',
         lambda page: page.set_force_contractless_invoice(),
         User(455291281, 'yb-atst-custom-role-20'))
    ],
    ids=lambda field_name, field_name_db, set_field, user: field_name_db
)
def test_edit_client_extprops_with_needed_permissions(field_name, field_name_db, set_field, user, get_free_user):
    user = get_free_user()

    set_role_with_perms_to_edit_client(user)
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    with reporter.step(u"Изменяем признак '{}' на странице редактирования клиента".format(field_name)):
        with balance_web.Driver(user) as driver:
            client_page = balance_web.AdminInterface.ClientEditPage.open(driver, client_id)
            set_field(client_page)
            client_page.save_client()
    extprops = db.balance().get_extprops_by_object_id(classname='Client', object_id=client_id)
    utils.check_that(extprops,
                     contains_dicts_with_entries([{'attrname': field_name_db, 'value_num': 1}], same_length=False),
                     step=u"Проверяем что признак '{}' изменился".format(field_name),
                     error=u"Признак '{}' не изменился".format(field_name))


def test_oebs_reexport_btn_with_perm_47(data_cache, get_free_user):
    # user = User(430523133, 'yb-atst-custom-role-9')
    user = get_free_user()

    steps.UserSteps.set_role_with_permissions_strict(user, [Permissions.ADMIN_ACCESS_0,
                                                            Permissions.BILLING_SUPPORT_1100,
                                                            Permissions.OEBS_REEXPORT_INVOICE])

    client_id = steps.ClientSteps.create()
    invoice_id, _, _ = _create_invoice(client_id, need_export=True)
    steps.ExportSteps.export_oebs(invoice_id=invoice_id)
    # steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)

    steps.ClientSteps.link(client_id, user.login)

    with balance_web.Driver(user) as driver:
        invoice_page = balance_web.AdminInterface.InvoicePage.open(driver, invoice_id)
        invoice_page.reexport_invoice()


def test_oebs_reexport_btn_without_perm_47(data_cache, get_free_user):
    user = get_free_user()

    perm_ids = set(steps.PermissionsSteps.get_all_permission_ids()) - {Permissions.REDIRECT_TO_YANDEX_TEAM_36,
                                                                       Permissions.OEBS_REEXPORT_INVOICE}
    steps.UserSteps.set_role_with_permissions_strict(user, perm_ids)

    client_id = steps.ClientSteps.create()
    invoice_id, _, _ = _create_invoice(client_id, need_export=True)
    # steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)
    steps.ExportSteps.export_oebs(invoice_id=invoice_id)

    steps.ClientSteps.link(client_id, user.login)

    with balance_web.Driver(user) as driver:
        invoice_page = balance_web.AdminInterface.InvoicePage.open(driver, invoice_id)
        utils.check_that(invoice_page.is_reexport_invoice_button_present(), equal_to(False))


def test_close_invoice_without_perm_47(data_cache, get_free_user):
    # user = User(455291172, 'yb-atst-custom-role-11')
    user = get_free_user()

    perm_ids = set(steps.PermissionsSteps.get_all_permission_ids()) - {Permissions.REDIRECT_TO_YANDEX_TEAM_36,
                                                                       Permissions.ADDITIONAL_FUNCTIONS_47}
    steps.UserSteps.set_role_with_permissions_strict(user, perm_ids)
    with utils.CachedData(data_cache, ['client_id', 'invoice_id'], force_invalidate=SHOULD_BE_INVALIDATED) as c:
        if not c: raise utils.SkipContextManagerBodyException()
        client_id = steps.ClientSteps.create()
        invoice_id = _create_invoice_from_shop(client_id)

    steps.ClientSteps.link(client_id, user.login)

    with reporter.step(u"Проверяем отсутствие кнопки 'Закрыть счет' (без права AdditionalFunctions)"):
        with balance_web.Driver(user=user) as driver:
            invoice_page = balance_web.AdminInterface.InvoicePage.open(driver, invoice_id)
            utils.check_that(invoice_page.is_close_invoice_button_present(), equal_to(False),
                             step=u'Проверяем отсутствие кнопки Закрыть счет',
                             error=u'Найдена кнопка Закрыть счет')


def test_close_invoice_with_perm_47(get_free_user):
    # user = User(455291189, 'yb-atst-custom-role-12')
    user = get_free_user()

    steps.UserSteps.set_role_with_permissions_strict(user, [Permissions.ADMIN_ACCESS_0,
                                                            Permissions.CLOSE_INVOICE])
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    invoice_id = _create_invoice_from_shop(client_id)
    with balance_web.Driver(user=user) as driver:
        invoice_page = balance_web.AdminInterface.InvoicePage.open(driver, invoice_id)
        invoice_page.close_invoice()
        # Акт не всегда успевает появиться в БД до обновления страницы (после закрытия счёта). Ждём его:
        utils.wait_until(lambda: _get_acts_qty_for_invoice(invoice_id), equal_to(1),
                         descr=u'Ожидаем появленния акта')
        utils.check_that(_get_acts_qty_for_invoice(invoice_id), equal_to(1),
                         step=u"Проверяем, что по счету выставился акт",
                         error=u"Не выставился акт при закрытии счета")


# ------------------------------------------------------------------------------


def _create_invoice(client_id, service_id=None, service_order_id=None, need_export=False):
    PAYSYS_ID = Paysyses.BANK_UR_RUB.id
    SERVICE_ID = Services.DIRECT.id
    PRODUCT_ID = Products.DIRECT_FISH.id

    with reporter.step(u'Создаем счет на клиента (id={id})'.format(id=client_id)):
        person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)

        _service_id = service_id if service_id else SERVICE_ID
        _service_order_id = service_order_id if service_order_id else steps.OrderSteps.next_id(service_id=_service_id)
        steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=_service_id,
                                service_order_id=_service_order_id)

        orders_list = [{'ServiceID': _service_id, 'ServiceOrderID': _service_order_id, 'Qty': 200}]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
        response = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID)
        if need_export:
            steps.ExportSteps.export_oebs(client_id=client_id, person_id=person_id)
        return response


def _create_invoice_and_act(client_id):
    with reporter.step(u'Создаем счет и акт на клиента (id={id})'.format(id=client_id)):
        service_id = Services.DIRECT.id
        service_order_id = steps.OrderSteps.next_id(service_id=service_id)
        invoice_id, _, _ = _create_invoice(client_id, service_id, service_order_id)
        steps.InvoiceSteps.turn_on(invoice_id)

        steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 200})

        act_id = steps.ActsSteps.generate(client_id, force=0)[0]

        return invoice_id, act_id


def _create_invoice_from_shop(client_id):
    PERSON_TYPE = PersonTypes.UR.code
    PAYSYS_ID = Paysyses.BANK_UR_RUB.id

    with reporter.step(u'Создаем счет из магазина на клиента (id={id})'.format(id=client_id)):
        person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
        request_id = steps.RequestSteps.create_from_shop(client_id=client_id)
        return steps.InvoiceSteps.create_prepay_http(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID)


def _get_receipt_sum_db(invoice_id):
    sql = "SELECT receipt_sum FROM t_invoice WHERE id = :invoice_id"
    params = {'invoice_id': invoice_id}
    return db.balance().execute(sql, params, descr='Получаем данные по счету из t_invoice',
                                single_row=True)['receipt_sum']


def _get_acts_qty_for_invoice(invoice_id):
    sql = "select count(1) as qty from t_act where invoice_id = {inv_id} and hidden = 0".format(inv_id=invoice_id)
    return db.balance().execute(sql, single_row=True)['qty']


def _check_error_text_on_page(exception_info, expected_error_text):
    utils.check_that(exception_info.value.message, contains_string(expected_error_text),
                     step=u"Проверяем, что на странице отображается ошибка",
                     error=u"На странице не отображается ожидаемая ошибка")