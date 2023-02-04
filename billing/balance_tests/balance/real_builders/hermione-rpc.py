# coding=utf-8

import balance.balance_db as db
import btestlib.passport_steps as passport_steps
import btestlib.reporter as reporter
from werkzeug.wrappers import Request, Response
from werkzeug.serving import run_simple
from jsonrpc import JSONRPCResponseManager, dispatcher

import datetime
from balance.tests.conftest import get_free_user
from balance import balance_steps as steps
from btestlib.constants import Services, Products, Paysyses, Users, Roles, Permissions

from balance.real_builders import common_defaults

import balance.real_builders.sms.sms_verification
import balance.real_builders.orders.orders
import balance.real_builders.orders.orders_perm_11201
import balance.real_builders.orders.orders_charge_note
import balance.real_builders.acts.acts_special
import balance.real_builders.invoices.fictive.fictive
import balance.real_builders.persons.persons
import balance.real_builders.clients.persons
import balance.real_builders.clients.autooverdraft
import balance.real_builders.invoices.invoices
import balance.real_builders.invoices.prepayment_with_contract
import balance.real_builders.acts.acts
import balance.real_builders.contracts.contracts
import balance.real_builders.clients.clients
import balance.real_builders.clients.credits
import balance.real_builders.request.request
import balance.real_builders.promocodes.promocodes
import balance.real_builders.endbuyers.endbuyers


@dispatcher.add_method
def get_user(params):
    if "manager" in params:
        if params["manager"]:
            return {"login": Users.MANAGER.login, "password": Users.MANAGER.password, "id": Users.MANAGER.uid,
                    "roleDescription": "менеджер"}
    if "baseRole" and "env" in params:
        return get_user_by_base_role(params["baseRole"], params["include"], params["exclude"], params["env"])
    elif "baseRole" in params:
        return get_user_by_base_role(params["baseRole"], params["include"], params["exclude"])
    if "login" in params:
        return {"login": params["login"], "password": Users.HERMIONE_CLIENT.password,
                "id": steps.UserSteps.get_passport_id_by_login(params["login"]),
                "roleDescription": "клиент"}
    else:
        return get_user_by_params(params)


def get_user_by_base_role(base_role, include, exclude, env='prod'):
    user = next(get_free_user(need_cleanup=False, with_tus=True, env=env))()
    role_id = unique_role(base_role, include, exclude)
    db.balance().execute('insert into t_role_user(role_id, passport_id) values(:role_id, :passport_id)',
                         {'passport_id': user.id_, 'role_id': role_id})
    return {"login": user.login, "password": user.password, "id": user.id_, "roleDescription": user.role_description}


def unique_role(role_id, addperm,
                deleteperm):  # Вводим id роли, список прав, которые надо добавить и список прав, которые надо удалить
    reporter.log(u'Подбираем роль как {} с дополнительными правами: {} и без прав: {}'.
                 format(role_id, ','.join([str(x) for x in addperm]), ','.join([str(x) for x in deleteperm])))
    # Содаем словарь роль:[права]
    group_perms = db.balance().execute(
        "SELECT role_id, LISTAGG(perm, ' ') WITHIN GROUP (ORDER BY perm) AS perms FROM t_role GROUP BY role_id")
    dict_group_perms = {x['role_id']: [int(i) for i in x['perms'].split()] for x in group_perms}

    # Формируем искомый список прав
    find_list_perm = dict_group_perms[role_id] + addperm
    find_set_perm = set(find_list_perm) - set(deleteperm)

    # Ищем в словаре роль, соответсвующую искомому списку прав
    for exist_role_id, perms in dict_group_perms.items():
        if set(perms) == find_set_perm:
            reporter.log(u'Нашлась подходящая роль ' + str(exist_role_id))
            return exist_role_id

    # Если роль не нашлась выше - создаем новую
    new_role_id = db.balance().execute("SELECT BO.S_ROLE_ID.NEXTVAL FROM dual")  # ищем пустой айдишник для роли
    new_role_id = new_role_id[0].values()[0]
    db.balance().execute(
        "insert into t_role_name (id, name) values(:new_role_id, :role_name)",
        {'new_role_id': new_role_id,
         'role_name': 'like_{}_but_{}_not_{}'.format(role_id, '_'.join([str(x) for x in addperm]), '_'.join(
             [str(x) for x in deleteperm]))})  # вставляем айди роли и название

    # Вставляем необходимые права в роль
    for perm in find_set_perm:
        db.balance().execute(
            "insert into t_role_perm(role_id, perm) values (:new_role_id, :perm)",
            {'new_role_id': new_role_id, 'perm': perm})
    reporter.log(u'Создана подходящая роль ' + str(new_role_id))
    return new_role_id


def get_user_by_params(params):
    if "isAdmin" not in params:
        params["isAdmin"] = False
    if "isReadonly" not in params:
        params["isReadonly"] = True
    if "testEnv" not in params:
        params["testEnv"] = False

    if params["testEnv"]:
        if params["isAdmin"]:
            if params["isReadonly"]:
                return get_user({'baseRole': Roles.READ_ONLY_12, 'include': [], 'exclude': [], 'env': ['test']})
            else:
                return get_user({'baseRole': Roles.ADMIN_0, 'include': [], 'exclude': [], 'env': ['test']})
        else:
            return get_user({'baseRole': Roles.CLIENT_3, 'include': [], 'exclude': [], 'env': ['test']})
    else:
        if params["isAdmin"]:
            if params["isReadonly"]:
                return get_user({'baseRole': Roles.READ_ONLY_12, 'include': [], 'exclude': []})
            else:
                return get_user({'baseRole': Roles.ADMIN_0, 'include': [], 'exclude': []})
        else:
            return get_user({'baseRole': Roles.CLIENT_3, 'include': [], 'exclude': []})


@dispatcher.add_method
def link_client_to_user(login, client_id):
    steps.ClientSteps.link(client_id, login)
    return client_id

@dispatcher.add_method
def create_empty_passport():
    user = next(get_free_user(need_cleanup=False, with_tus=True))()
    db.balance().execute("insert into t_role_user(role_id, passport_id) values(:role_id, :passport_id)",
                         {'role_id': Roles.CLIENT_3,'passport_id': user.id_})
    db.balance().execute("update t_passport set gecos = '', email='', client_id='' where passport_id=:passport_id",
                         {'passport_id': user.id_})
    return {"passport_id": user.id_}


@dispatcher.add_method
def create_full_passport():
    user = next(get_free_user(need_cleanup=False, with_tus=True))()
    passport_id = user.id_
    db.balance().execute("insert into t_role_user(role_id, passport_id) values(:role_id, :passport_id)",
                         {'role_id': Roles.CLIENT_3, 'passport_id': user.id_})
    db.balance().execute(
        " insert into t_role_user(role_id, passport_id) values(:role_id, :passport_id)",
        {'role_id': Roles.AGENCY_2, 'passport_id': passport_id})
    db.balance().execute(
        " UPDATE T_PASSPORT set client_id='' where passport_id=:passport_id",
        {'passport_id': passport_id})
    return {"passport_id": passport_id}


@dispatcher.add_method
def create_agency_for_user(login):
    client_id = steps.ClientSteps.create_agency()
    steps.ClientSteps.link(client_id, login)

    return {'client_id': client_id}


@dispatcher.add_method
def create_client():
    client_id = steps.ClientSteps.create(params={'NAME': u'Никифор'})

    return {'client_id': client_id}


@dispatcher.add_method
def create_client_for_user(login):
    client_id = steps.ClientSteps.create(params={'NAME': u'Никифор'})
    steps.ClientSteps.link(client_id, login)

    return {'client_id': client_id}


@dispatcher.add_method
def create_client_with_person_for_user(login, person_type, is_partner, full=False):
    client_id = steps.ClientSteps.create(params=None)
    if person_type == 'ur':
        params = common_defaults.FIXED_UR_PARAMS
    elif person_type == 'ur_autoru':
        params = common_defaults.FIXED_UR_AUTORU_PARAMS
    elif person_type == 'ur_ytkz':
        params = common_defaults.FIXED_UR_YTKZ_PARAMS
    elif person_type == 'us_yt':
        params = common_defaults.FIXED_US_YT_PARAMS
    elif person_type == 'us_ytph':
            params = common_defaults.FIXED_US_YTPH_PARAMS
    elif person_type == 'ph':
        params = common_defaults.FIXED_PH_PARAMS
    elif person_type == 'ph_autoru':
        params = common_defaults.FIXED_PH_AUTORU_PARAMS
    elif person_type == 'yt':
        params = common_defaults.FIXED_YT_PARAMS
    elif person_type == 'am_jp':
        params = common_defaults.FIXED_AM_JP_PARAMS
    elif person_type == 'byu':
        params = common_defaults.FIXED_BYU_PARAMS
    elif person_type == 'byp':
        params = common_defaults.FIXED_BYP_PARAMS
    elif person_type == 'by_ytph':
        params = common_defaults.FIXED_BY_YTPH_PARAMS
    elif person_type == 'hk_ytph':
        params = common_defaults.FIXED_HK_YTPH_PARAMS
    elif person_type == 'hk_ytph_with_yamoney':
        person_type = 'hk_ytph'
        params = common_defaults.FIXED_HK_YTPH_PARAMS_WITH_YAMONEY
    elif person_type == 'eu_yt':
        params = common_defaults.FIXED_EU_YT_PARAMS
    elif person_type == 'sk_ur':
        params = common_defaults.FIXED_SK_UR_PARAMS
    elif person_type == 'sw_ph':
        params = common_defaults.FIXED_SW_PH_PARAMS
    elif person_type == 'sw_ur':
        params = common_defaults.FIXED_SW_UR_PARAMS
    elif person_type == 'sw_yt':
        params = common_defaults.FIXED_SW_YT_PARAMS
    elif person_type == 'sw_ytph':
        params = common_defaults.FIXED_SW_YTPH_PARAMS
    elif person_type == 'kzu':
        params = common_defaults.FIXED_KZU_PARAMS
    elif person_type == 'kzp':
        params = common_defaults.FIXED_KZP_PARAMS
    elif person_type == 'il_ur':
        params = common_defaults.FIXED_IL_UR_PARAMS
    elif person_type == 'usp':
        params = common_defaults.FIXED_USP_PARAMS
    elif person_type == 'usu':
        params = common_defaults.FIXED_USU_PARAMS
    elif person_type == 'ytph':
        params = common_defaults.FIXED_YTPH_PARAMS
    elif person_type == 'yt_kzp':
        params = common_defaults.FIXED_YT_KZP_PARAMS
    elif person_type == 'yt_kzu':
        params = common_defaults.FIXED_YT_KZU_PARAMS
    elif person_type == 'fr_ur':
        params = common_defaults.FIXED_FR_UR_PARAMS
    elif person_type == 'gb_ur':
        params = common_defaults.FIXED_GB_UR_PARAMS
    elif person_type == 'hk_yt':
        params = common_defaults.FIXED_HK_YT_PARAMS
    elif person_type == 'az_ur':
        params = common_defaults.FIXED_AZ_UR_PARAMS
    elif person_type == 'ro_ur':
        params = common_defaults.FIXED_RO_UR_PARAMS
    elif person_type == 'de_ur':
        params = common_defaults.FIXED_DE_UR_PARAMS
    elif person_type == 'de_ph':
        params = common_defaults.FIXED_DE_PH_PARAMS
    elif person_type == 'de_yt':
        params = common_defaults.FIXED_DE_YT_PARAMS
    elif person_type == 'de_ytph':
        params = common_defaults.FIXED_DE_YTPH_PARAMS
    else:
        raise ValueError('Unsupported person type %s' % person_type)
    params = params.copy()
    params.update({'is-partner': is_partner})
    person_id = steps.PersonSteps.create(client_id, person_type, params, full=full)

    steps.ClientSteps.link(client_id, login)

    return {'client_id': client_id, 'person_id': person_id}


@dispatcher.add_method
def create_client_with_autooverdraft():
    from balance.real_builders.clients.autooverdraft import test_direct_firm_1_autooverdraft

    client_id, person_id = test_direct_firm_1_autooverdraft()
    return {'client_id': client_id}


@dispatcher.add_method
def create_client_with_selfemployed_person():
    from balance.real_builders.clients.persons import test_selfemployed_ur_person

    client_id, person_id = test_selfemployed_ur_person()
    return {'client_id': client_id}


@dispatcher.add_method
def create_client_with_ur_and_ph_persons():
    from balance.real_builders.clients.persons import test_several_persons

    client_id = test_several_persons([common_defaults.FIXED_UR_PARAMS, common_defaults.FIXED_PH_PARAMS])

    return {"client_id": client_id}


@dispatcher.add_method
def create_client_with_buy_and_sw_yt_and_sw_ytph_persons():
    from balance.real_builders.clients.persons import test_several_persons

    client_id = test_several_persons(
        [common_defaults.FIXED_BYU_PARAMS, common_defaults.FIXED_SW_YT_PARAMS, common_defaults.FIXED_SW_YTPH_PARAMS])

    return {"client_id": client_id}


@dispatcher.add_method
def export_person(person_id):
    import balance.balance_db as db

    query = "update t_export set state=1, export_dt=:export_dt where classname = 'Person' and object_id = :person_id"
    db.balance().execute(query, {'export_dt': steps.NOW, 'person_id': person_id})
    return {"exported": True}


@dispatcher.add_method
def create_client_with_act(login):
    client_id = steps.ClientSteps.create(params=None)

    steps.ClientSteps.link(client_id, login)

    person_id = steps.PersonSteps.create(client_id, 'ur')
    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    now = datetime.datetime.now()
    paysys_id = Paysyses.BANK_UR_RUB.id

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)  # внешний ID заказа

    steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                            product_id=product_id, params={'AgencyID': None})

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': now}]

    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
                                                 contract_id=None, overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 100}, 0, now)

    steps.ActsSteps.generate(client_id, force=1, date=now)[0]

    return {"client_id": client_id}


@dispatcher.add_method
def create_empty_agency():
    from balance.real_builders.clients.clients import test_empty_agency
    client_id = test_empty_agency()
    return {"client_id": client_id}


@dispatcher.add_method
def client_discounts(fixed, scale):
    from balance.real_builders.clients.discounts import test_discounts
    client_id = test_discounts(fixed, scale)
    return {"client_id": client_id}


@dispatcher.add_method
def multiple_subclients():
    from balance.real_builders.clients.subclients import test_no_pagination_various_subclients
    agency_id, clients_with_orders, client_without_order = test_no_pagination_various_subclients()
    return {"agency_id": agency_id, "clients_with_orders": clients_with_orders,
            "client_without_order": client_without_order}


@dispatcher.add_method
def client_with_many_subclients_for_pagination():
    from balance.real_builders.clients.subclients import test_pagination_same_subclients
    agency_id = test_pagination_same_subclients()
    return {"agency_id": agency_id}


@dispatcher.add_method
def unlink_all_clients_from_login(uid):
    steps.ClientSteps.unlink_from_login(uid)
    return {"uid": uid}


@dispatcher.add_method
def create_client_with_aliases_for_user(login):
    from balance.real_builders.clients.aliases import test_aliases

    client_id = test_aliases()
    steps.ClientSteps.link(client_id, login)

    return {"client_id": client_id}


@dispatcher.add_method
def create_client_with_places(login):
    from balance.real_builders.clients.client_places import test_place

    client_id = test_place()

    return {"client_id": client_id}


@dispatcher.add_method
def create_client_with_unfunds():
    from balance.real_builders.clients.unfunds import test_unfunds
    client_id = test_unfunds()

    return {"client_id": client_id}

@dispatcher.add_method
def clear_sent_contract_emails(contract_id):
    db.balance().execute(
        "UPDATE T_EXTPROPS SET VALUE_CLOB='[]' WHERE CLASSNAME='ContractCollateral' AND ATTRNAME='print_tpl_email_log' AND (OBJECT_ID) IN (SELECT id FROM t_contract_collateral WHERE contract2_id=:contract_id)",
        {'contract_id': contract_id}
    )

    return {}

@Request.application
def application(request):
    response = JSONRPCResponseManager.handle(request.data, dispatcher)
    return Response(response.json, mimetype='application/json')


if __name__ == '__main__':
    run_simple('::', 4000, application)
