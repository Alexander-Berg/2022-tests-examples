# coding: utf-8

import pytest
import requests
import json
import btestlib.environments as env
from btestlib import utils, reporter, secrets, utils_tvm
from balance import balance_db as db
from btestlib.constants import User, TvmClientIds
from btestlib.secrets import get_secret, TvmSecrets
import balance.balance_api as api
import xml.etree.ElementTree as et
from balance.features import Features

pytestmark = [reporter.feature(Features.PERMISSION, Features.ROLE), pytest.mark.tickets('BALANCE-28282')]

domain = User(1120000000039907, 'furiousf')
domain_2 = User(1120000000042103, 'mashabond')
passport_1 = User(4013175352, 'balance-test-1')
passport_2 = User(4013175394, 'balance-test-2')
fake = User(None, 'UnExisTiK')
_TVM_TICKET = None


def get_tvm_ticket():
    """ Кэшируем тикет, чтобы не ходить на каждый тест за ним. Они сейчас живут по 12 часов. """
    global _TVM_TICKET
    if _TVM_TICKET is None:
        _TVM_TICKET = utils_tvm.get_tvm_ticket(
            dst_client_id=TvmClientIds.YB_MEDIUM,
            tvm_client_id=TvmClientIds.BALANCE_TESTS,
            secret=get_secret(*TvmSecrets.BALANCE_TESTS_SECRET)
        )

    return _TVM_TICKET


def clean_domain():
    db.balance().execute('''DELETE FROM T_ROLE_USER WHERE PASSPORT_ID IN (:id_1, :id_2, :id_3, :id_4)''',
                         {'id_1': str(domain.uid), 'id_2': str(passport_1.uid), 'id_3': str(passport_2.uid),
                          'id_4': str(domain_2.uid)})
    db.balance().execute('''UPDATE t_passport SET internal = NULL WHERE login IN (:login_1, :login_2)''',
                         {'login_1': passport_1.login, 'login_2': passport_2.login})


def get_internal(user):
    select = db.balance().execute('''SELECT * FROM t_passport WHERE LOGIN=:login''',
                                  {'login': user.login})[0]['internal']
    return int(select) if select is not None else None


def check_roles(user):
    select = db.balance().execute('''SELECT ROLE_ID FROM T_ROLE_USER WHERE PASSPORT_ID=:u_id''',
                                  {'u_id': str(user.uid)})
    for i in range(len(select)):
        select[i] = str(select[i]['role_id'])
    return select


def make_request(method, path, params=None):
    url = '{}/{}/'.format(env.balance_env().idm_url, path)
    headers = {
        'Content-Type': 'application/x-www-form-urlencoded',
        'X-Ya-Service-Ticket': get_tvm_ticket(),
    }
    response = requests.request(
        method, url, params=params,
        headers=headers, verify=False
    )

    response.raise_for_status()
    return response.json()


def add_role(user, role, passport=None):
    data = {"login": user.login, "role": json.dumps({"role": role})}
    if passport is not None:
        data.update({'fields': json.dumps({"passport-login": passport.login})})
    return make_request('post', 'add-role', data)


def get_all_roles():
    return make_request('get', 'get-all-roles')


def get_roles(user):
    return make_request('get', 'get-user-roles', {"login": user.login})


def get_roles_new(user):
    roles = []
    answer = get_roles(user)['roles']
    for list_ in answer:
        if len(list_) == 1:
            roles.append((list_['role'], user.login))
        else:
            roles.append((list_[0]['role'], list_[1]['passport-login']))
    return roles


def remove_role(user, role, passport=None):
    data = {"login": user.login, "role": json.dumps({"role": role})}
    if passport is not None:
        data.update({'fields': json.dumps({"passport-login": passport.login})})
    return make_request('post', 'remove-role', data)


def is_strong_pwd(user):
    answer_xml = api.test_balance().SetPassportAdmsubscribe(user.login, True, 'strongpwd')
    answer = et.fromstring(answer_xml).findtext('status')
    if answer == 'nothingtodo':
        return True
    if answer == 'ok':
        api.test_balance().SetPassportAdmsubscribe(user.login, False, 'strongpwd')
        return False


def is_weak_pwd(user):
    answer_xml = api.test_balance().SetPassportAdmsubscribe(user.login, False, 'strongpwd')
    answer = et.fromstring(answer_xml).findtext('status')
    if answer == 'nothingtodo':
        return True
    if answer == 'ok':
        api.test_balance().SetPassportAdmsubscribe(user.login, True, 'strongpwd')
        return False


# ADD_ROLE
# Проверяем: код ответа, наличие паспортного логина в ответе, привязку паспортного логина, наличие роли в базе,
# ответ ручки get-user-role

@pytest.mark.no_parallel('idm')
def test_add_role_to_passport_login():
    clean_domain()
    answer = add_role(domain, '4', passport=passport_1)
    assert answer['code'] == 0
    assert answer['data'] == {"passport-login": passport_1.login}
    assert get_internal(passport_1) == domain.uid
    assert check_roles(passport_1) == ['4']
    assert check_roles(domain) == []
    assert get_roles_new(domain) == [('4', passport_1.login)]


@pytest.mark.no_parallel('idm')
def test_add_role_to_domain_login():
    clean_domain()
    answer = add_role(domain, '4')
    assert answer['code'] == 0
    assert check_roles(domain) == ['4']
    assert get_roles_new(domain) == [('4', domain.login)]


@pytest.mark.no_parallel('idm')
def test_add_role_like_domain_to_passport_login():
    clean_domain()
    answer_domain = add_role(domain, '4')
    answer_passport = add_role(domain, '4', passport=passport_2)
    assert answer_domain['code'] == 0
    assert answer_passport['code'] == 0
    assert answer_passport['data']['passport-login'] == passport_2.login
    assert get_internal(passport_2) == domain.uid
    assert check_roles(domain) == ['4']
    assert check_roles(passport_2) == ['4']
    assert get_roles_new(domain) == [('4', domain.login), ('4', passport_2.login)]


@pytest.mark.no_parallel('idm')
def test_add_role_not_like_domain_to_passport_login():
    clean_domain()
    answer_domain = add_role(domain, '4')
    answer_passport = add_role(domain, '100', passport=passport_2)
    assert answer_domain['code'] == 0
    assert answer_passport['code'] == 0
    assert answer_passport['data']['passport-login'] == passport_2.login
    assert get_internal(passport_2) == domain.uid
    assert check_roles(domain) == ['4']
    assert check_roles(passport_2) == ['100']
    assert get_roles_new(domain) == [('4', domain.login), ('100', passport_2.login)]


@pytest.mark.no_parallel('idm')
def test_add_same_role_to_passport_login():
    clean_domain()
    answer = add_role(domain, '4', passport=passport_1)
    answer_2 = add_role(domain, '4', passport=passport_1)
    assert answer['code'] == 0
    assert answer['data']['passport-login'] == passport_1.login
    assert answer_2['data']['passport-login'] == passport_1.login
    assert answer_2['code'] == 1
    assert answer_2['warning'] == u'Пользователь уже имеет эту роль.'
    assert get_internal(passport_1) == domain.uid
    assert check_roles(passport_1) == ['4']
    assert get_roles_new(domain) == [('4', passport_1.login)]


@pytest.mark.no_parallel('idm')
def test_add_another_role_to_passport_login():
    clean_domain()
    answer = add_role(domain, '4', passport=passport_1)
    answer_2 = add_role(domain, '100', passport=passport_1)
    assert answer['code'] == 0
    assert answer_2['code'] == 0
    assert answer['data']['passport-login'] == passport_1.login
    assert answer_2['data']['passport-login'] == passport_1.login
    assert get_internal(passport_1) == domain.uid
    assert set(check_roles(passport_1)) == {'100', '4'}
    assert set(get_roles_new(domain)) == {('100', passport_1.login), ('4', passport_1.login)}


@pytest.mark.no_parallel('idm')
def test_add_role_like_another_passport_to_passport_login():
    clean_domain()
    answer = add_role(domain, '4', passport=passport_1)
    answer_2 = add_role(domain, '4', passport=passport_2)
    assert answer['code'] == 0
    assert answer_2['code'] == 0
    assert answer['data']['passport-login'] == passport_1.login
    assert answer_2['data']['passport-login'] == passport_2.login
    assert get_internal(passport_1) == domain.uid
    assert get_internal(passport_2) == domain.uid
    assert check_roles(passport_1) == ['4']
    assert check_roles(passport_2) == ['4']
    assert get_roles_new(domain) == [('4', passport_1.login), ('4', passport_2.login)]


@pytest.mark.no_parallel('idm')
def test_add_role_not_like_another_passport_to_passport_login():
    clean_domain()
    answer = add_role(domain, '4', passport=passport_1)
    answer_2 = add_role(domain, '100', passport=passport_2)
    assert answer['code'] == 0
    assert answer_2['code'] == 0
    assert answer['data']['passport-login'] == passport_1.login
    assert answer_2['data']['passport-login'] == passport_2.login
    assert get_internal(passport_1) == domain.uid
    assert get_internal(passport_2) == domain.uid
    assert check_roles(passport_1) == ['4']
    assert check_roles(passport_2) == ['100']
    assert get_roles_new(domain) == [('4', passport_1.login), ('100', passport_2.login)]


@pytest.mark.no_parallel('idm')
def test_add_same_role_to_domain_login():
    clean_domain()
    answer = add_role(domain, '4')
    answer_2 = add_role(domain, '4')
    assert answer['code'] == 0
    assert answer_2 == {'code': 1, 'warning': u'Пользователь уже имеет эту роль.'}
    assert check_roles(domain) == ['4']
    assert get_roles_new(domain) == [('4', domain.login)]


@pytest.mark.no_parallel('idm')
def test_add_another_role_to_domain_login():
    clean_domain()
    answer = add_role(domain, '4')
    answer_2 = add_role(domain, '5')
    assert answer['code'] == 0
    assert answer_2['code'] == 0
    assert set(get_roles_new(domain)) == {('5', domain.login), ('4', domain.login)}


@pytest.mark.no_parallel('idm')
def test_add_role_like_passport_to_domain_login():
    clean_domain()
    answer_passport = add_role(domain, '4', passport=passport_1)
    answer_domain = add_role(domain, '4')
    assert answer_domain['code'] == 0
    assert answer_passport['code'] == 0
    assert answer_passport['data']['passport-login'] == passport_1.login
    assert get_internal(passport_1) == domain.uid
    assert check_roles(domain) == ['4']
    assert check_roles(passport_1) == ['4']
    assert get_roles_new(domain) == [('4', domain.login), ('4', passport_1.login)]


@pytest.mark.no_parallel('idm')
def test_add_role_not_like_passport_to_domain_login():
    clean_domain()
    answer_passport = add_role(domain, '10', passport=passport_1)
    answer_domain = add_role(domain, '4')
    assert answer_domain['code'] == 0
    assert answer_passport['code'] == 0
    assert answer_passport['data']['passport-login'] == passport_1.login
    assert get_internal(passport_1) == domain.uid
    assert check_roles(domain) == ['4']
    assert check_roles(passport_1) == ['10']
    assert get_roles_new(domain) == [('4', domain.login), ('10', passport_1.login)]


@pytest.mark.no_parallel('idm')
def test_add_role_to_unexist_domain_login():
    clean_domain()
    assert add_role(fake, '4')['fatal'] == u'Пользователь не найден: UnExisTiK'
    assert add_role(fake, '4', passport=passport_1)['fatal'] == u'Пользователь не найден: UnExisTiK'


@pytest.mark.no_parallel('idm')
def test_add_role_to_unexist_passport_login():
    clean_domain()
    assert add_role(domain, '4', passport=fake)['fatal'] == u'Пользователь не найден: UnExisTiK'


@pytest.mark.no_parallel('idm')
def test_add_role_to_passport_bound_to_another_domain():
    clean_domain()
    add_role(domain_2, '4', passport=passport_1)
    assert add_role(domain, '4', passport=passport_1)[
               'error'] == u'Upravlyator.WrongLinkedPassport(Паспортный логин %s привязан к другому доменному логину %s!)' % (passport_1.login, domain_2.login)


# REMOVE-ROLE
# Проверяем: привязку паспортного логина, наличие роли в базе, ответ ручки get-user-role


@pytest.mark.no_parallel('idm')
def test_remove_only_role_from_domain_login():
    clean_domain()
    add_role(domain, '4')
    remove_role(domain, '4')
    assert check_roles(domain) == []
    assert get_roles_new(domain) == []


@pytest.mark.no_parallel('idm')
def test_remove_role_from_domain_login():
    clean_domain()
    add_role(domain, '4')
    add_role(domain, '5')
    remove_role(domain, '4')
    assert check_roles(domain) == ['5']
    assert get_roles_new(domain) == [('5', domain.login)]


@pytest.mark.no_parallel('idm')
def test_remove_only_role_like_passport_from_domain_login():
    clean_domain()
    add_role(domain, '4')
    add_role(domain, '4', passport=passport_1)
    remove_role(domain, '4')
    assert get_internal(passport_1) == domain.uid
    assert check_roles(domain) == []
    assert check_roles(passport_1) == ['4']
    assert get_roles_new(domain) == [('4', passport_1.login)]


@pytest.mark.no_parallel('idm')
def test_remove_role_not_like_passports_internal_from_domain_login():
    clean_domain()
    add_role(domain, '4')
    add_role(domain, '10')
    add_role(domain, '2', passport=passport_1)
    add_role(domain, '3', passport=passport_2)
    remove_role(domain, '4')
    assert get_internal(passport_1) == domain.uid
    assert get_internal(passport_2) == domain.uid
    assert check_roles(passport_1) == ['2']
    assert check_roles(passport_2) == ['3']
    assert check_roles(domain) == ['10']
    assert get_roles_new(domain) == [('10', domain.login)]


@pytest.mark.no_parallel('idm')
def test_remove_role_not_like_passports_from_domain_login():
    clean_domain()
    add_role(domain, '4')
    add_role(domain, '10')
    add_role(domain, '100', passport=passport_1)
    add_role(domain, '101', passport=passport_2)
    remove_role(domain, '4')
    assert get_internal(passport_1) == domain.uid
    assert get_internal(passport_2) == domain.uid
    assert check_roles(passport_1) == ['100']
    assert check_roles(passport_2) == ['101']
    assert check_roles(domain) == ['10']
    assert set(get_roles_new(domain)) == {('10', domain.login), ('100', passport_1.login), ('101', passport_2.login)}


@pytest.mark.no_parallel('idm')
def test_remove_role_not_like_passports_internal2_from_domain_login():
    clean_domain()
    add_role(domain, '4')
    add_role(domain, '2', passport=passport_1)
    add_role(domain, '2', passport=passport_2)
    remove_role(domain, '4')
    assert get_internal(passport_1) is None
    assert get_internal(passport_2) is None
    assert check_roles(domain) == []
    assert check_roles(passport_1) == ['2']
    assert check_roles(passport_2) == ['2']
    assert get_roles_new(domain) == []


@pytest.mark.no_parallel('idm')
def test_remove_role_not_like_passports_internal3_from_domain_login():
    clean_domain()
    add_role(domain, '4')
    add_role(domain, '3', passport=passport_1)
    add_role(domain, '3', passport=passport_2)
    remove_role(domain, '4')
    assert get_internal(passport_1) is None
    assert get_internal(passport_2) is None
    assert check_roles(domain) == []
    assert check_roles(passport_1) == ['3']
    assert check_roles(passport_2) == ['3']
    assert get_roles_new(domain) == []


@pytest.mark.no_parallel('idm')
def test_remove_only_role_not_like_passports_from_domain_login():
    clean_domain()
    add_role(domain, '4')
    add_role(domain, '100', passport=passport_1)
    add_role(domain, '101', passport=passport_2)
    remove_role(domain, '4')
    assert get_internal(passport_1) == domain.uid
    assert get_internal(passport_2) == domain.uid
    assert check_roles(domain) == []
    assert check_roles(passport_1) == ['100']
    assert check_roles(passport_2) == ['101']
    assert set(get_roles_new(domain)) == {('100', passport_1.login), ('101', passport_2.login)}


@pytest.mark.no_parallel('idm')
def test_remove_only_role_like_passports_from_domain_login():
    clean_domain()
    add_role(domain, '4')
    add_role(domain, '4', passport=passport_1)
    add_role(domain, '4', passport=passport_2)
    remove_role(domain, '4')
    assert get_internal(passport_1) == domain.uid
    assert get_internal(passport_2) == domain.uid
    assert check_roles(passport_1) == ['4']
    assert check_roles(passport_2) == ['4']
    assert set(get_roles_new(domain)) == {('4', passport_1.login), ('4', passport_2.login)}


@pytest.mark.no_parallel('idm')
def test_remove_only_role_like_passport_not_like_passport_internal2_from_domain_login():
    clean_domain()
    add_role(domain, '4')
    add_role(domain, '4', passport=passport_1)
    add_role(domain, '2', passport=passport_2)
    remove_role(domain, '4')
    assert get_internal(passport_1) == domain.uid
    assert get_internal(passport_2) is None
    assert check_roles(domain) == []
    assert check_roles(passport_1) == ['4']
    assert check_roles(passport_2) == ['2']
    assert get_roles_new(domain) == [('4', passport_1.login)]


@pytest.mark.no_parallel('idm')
def test_remove_only_role_like_passport_not_like_passport_internal3_from_domain_login():
    clean_domain()
    add_role(domain, '4')
    add_role(domain, '4', passport=passport_1)
    add_role(domain, '3', passport=passport_2)
    remove_role(domain, '4')
    assert get_internal(passport_1) == domain.uid
    assert get_internal(passport_2) is None
    assert check_roles(domain) == []
    assert check_roles(passport_1) == ['4']
    assert check_roles(passport_2) == ['3']
    assert get_roles_new(domain) == [('4', passport_1.login)]


@pytest.mark.no_parallel('idm')
def test_remove_only_role_like_passport_with_bound_passport_from_domain_login():
    clean_domain()
    add_role(domain, '4')
    add_role(domain, '4', passport=passport_1)
    add_role(domain, '4', passport=passport_2)
    db.balance().execute('''DELETE FROM T_ROLE_USER WHERE PASSPORT_ID = :id_1''',
                         {'id_1': str(passport_2.uid)})
    remove_role(domain, '4')
    assert get_internal(passport_1) == domain.uid
    assert get_internal(passport_2) is None
    assert check_roles(domain) == []
    assert check_roles(passport_1) == ['4']
    assert check_roles(passport_2) == []
    assert get_roles_new(domain) == [('4', passport_1.login)]


@pytest.mark.no_parallel('idm')
def test_remove_roles_from_domain_login_with_bound_passports():
    # Если паспортный логин не передан - отрываем роли от доменного логина.
    # Если на доменном логине ролей не осталось (без учёта внутренних) -
    # отрываем паспортные логины на которых тоже нет ролей.
    clean_domain()
    add_role(domain, '4')
    add_role(domain, '12')
    db.balance().execute('''UPDATE t_passport SET INTERNAL = :domain WHERE PASSPORT_ID  IN (:id_1, :id_2)''',
                         {'domain': str(domain.uid), 'id_1': str(passport_1.uid), 'id_2': str(passport_2.uid)})
    remove_role(domain, '12')
    remove_role(domain, '4')
    assert get_internal(passport_1) is None
    assert get_internal(passport_2) is None


@pytest.mark.no_parallel('idm')
def test_remove_roles_from_domain_login_with_bound_passports_2():
    # То же самое, только вызываем с паспортным логином
    clean_domain()
    add_role(domain, '4')
    add_role(domain, '12')
    db.balance().execute('''UPDATE t_passport SET INTERNAL = :domain WHERE PASSPORT_ID  IN (:id_1, :id_2)''',
                         {'domain': str(domain.uid), 'id_1': str(passport_1.uid), 'id_2': str(passport_2.uid)})
    remove_role(domain, '12', passport=passport_1)
    assert get_internal(passport_1) == domain.uid
    assert get_internal(passport_2) == domain.uid
    remove_role(domain, '4', passport=passport_2)
    assert get_internal(passport_1) is None
    assert get_internal(passport_2) is None


@pytest.mark.no_parallel('idm')
def test_remove_only_role_from_passport_login():
    clean_domain()
    add_role(domain, '4', passport=passport_1)
    remove_role(domain, '4', passport=passport_1)
    assert get_internal(passport_1) is None
    assert check_roles(passport_1) == []
    assert get_roles_new(domain) == []


@pytest.mark.no_parallel('idm')
def test_remove_role_not_like_internal2_from_passport_login():
    clean_domain()
    add_role(domain, '4', passport=passport_1)
    add_role(domain, '2', passport=passport_1)
    remove_role(domain, '4', passport=passport_1)
    assert get_internal(passport_1) is None
    assert check_roles(passport_1) == ['2']
    assert get_roles_new(domain) == []


@pytest.mark.no_parallel('idm')
def test_remove_role_not_like_internal3_from_passport_login():
    clean_domain()
    add_role(domain, '4', passport=passport_1)
    add_role(domain, '3', passport=passport_1)
    remove_role(domain, '4', passport=passport_1)
    assert get_internal(passport_1) is None
    assert check_roles(passport_1) == ['3']
    assert get_roles_new(domain) == []


@pytest.mark.no_parallel('idm')
def test_remove_role_from_passport_login():
    clean_domain()
    add_role(domain, '4', passport=passport_1)
    add_role(domain, '10', passport=passport_1)
    remove_role(domain, '10', passport=passport_1)
    assert get_internal(passport_1) == domain.uid
    assert check_roles(domain) == []
    assert check_roles(passport_1) == ['4']
    assert get_roles_new(domain) == [('4', passport_1.login)]


@pytest.mark.no_parallel('idm')
def test_remove_only_role_like_domain_from_passport_login():
    clean_domain()
    add_role(domain, '4', passport=passport_1)
    add_role(domain, '4')
    remove_role(domain, '4', passport=passport_1)
    assert get_internal(passport_1) is None
    assert check_roles(domain) == ['4']
    assert check_roles(passport_1) == []
    assert get_roles_new(domain) == [('4', domain.login)]


@pytest.mark.no_parallel('idm')
def test_remove_role_like_domain_from_passport_login():
    clean_domain()
    add_role(domain, '4')
    add_role(domain, '4', passport=passport_1)
    add_role(domain, '5', passport=passport_1)
    remove_role(domain, '4', passport=passport_1)
    assert check_roles(passport_1) == ['5']
    assert check_roles(domain) == ['4']
    assert get_internal(passport_1) == domain.uid
    assert set(get_roles_new(domain)) == {('4', domain.login), ('5', passport_1.login)}


@pytest.mark.no_parallel('idm')
def test_remove_role_like_domain_not_like_internal2_from_passport_login():
    clean_domain()
    add_role(domain, '4', passport=passport_1)
    add_role(domain, '2', passport=passport_1)
    add_role(domain, '4')
    remove_role(domain, '4', passport=passport_1)
    assert get_internal(passport_1) is None
    assert check_roles(domain) == ['4']
    assert check_roles(passport_1) == ['2']
    assert get_roles_new(domain) == [('4', domain.login)]


@pytest.mark.no_parallel('idm')
def test_remove_role_like_domain_not_like_internal3_from_passport_login():
    clean_domain()
    add_role(domain, '4', passport=passport_1)
    add_role(domain, '3', passport=passport_1)
    add_role(domain, '4')
    remove_role(domain, '4', passport=passport_1)
    assert get_internal(passport_1) is None
    assert check_roles(domain) == ['4']
    assert check_roles(passport_1) == ['3']
    assert get_roles_new(domain) == [('4', domain.login)]


@pytest.mark.no_parallel('idm')
def test_remove_only_role_like_passport_from_passport_login():
    clean_domain()
    add_role(domain, '4', passport=passport_1)
    add_role(domain, '4', passport=passport_2)
    remove_role(domain, '4', passport=passport_1)
    assert get_internal(passport_1) is None
    assert get_internal(passport_2) == domain.uid
    assert check_roles(passport_1) == []
    assert check_roles(passport_2) == ['4']
    assert get_roles_new(domain) == [('4', passport_2.login)]


@pytest.mark.no_parallel('idm')
def test_remove_role_like_passport_from_passport_login():
    clean_domain()
    add_role(domain, '4', passport=passport_1)
    add_role(domain, '5', passport=passport_1)
    add_role(domain, '4', passport=passport_2)
    remove_role(domain, '4', passport=passport_1)
    assert check_roles(passport_1) == ['5']
    assert check_roles(passport_2) == ['4']
    assert get_internal(passport_1) == domain.uid
    assert get_internal(passport_2) == domain.uid
    assert set(get_roles_new(domain)) == {('5', passport_1.login), ('4', passport_2.login)}


@pytest.mark.no_parallel('idm')
def test_remove_role_like_passport_with_unternal2_from_passport_login():
    clean_domain()
    add_role(domain, '4', passport=passport_1)
    add_role(domain, '2', passport=passport_1)
    add_role(domain, '4', passport=passport_2)
    remove_role(domain, '4', passport=passport_1)
    assert get_internal(passport_1) is None
    assert check_roles(passport_1) == ['2']
    assert check_roles(passport_2) == ['4']
    assert get_roles_new(domain) == [('4', passport_2.login)]


@pytest.mark.no_parallel('idm')
def test_remove_role_like_passport_with_unternal3_from_passport_login():
    clean_domain()
    add_role(domain, '4', passport=passport_1)
    add_role(domain, '3', passport=passport_1)
    add_role(domain, '4', passport=passport_2)
    remove_role(domain, '4', passport=passport_1)
    assert get_internal(passport_1) is None
    assert get_internal(passport_2) == domain.uid
    assert check_roles(passport_1) == ['3']
    assert check_roles(passport_2) == ['4']
    assert get_roles_new(domain) == [('4', passport_2.login)]


@pytest.mark.no_parallel('idm')
def test_remove_role_from_unexist_domain_login():
    assert remove_role(fake, '4')['warning'] == u'Пользователь не найден: UnExisTiK'
    assert remove_role(fake, '4', passport=passport_1)['warning'] == u'Пользователь не найден: UnExisTiK'


@pytest.mark.no_parallel('idm')
def test_remove_role_from_unexist_passport_login():
    assert remove_role(domain, '4', passport=fake)['warning'] == u'Пользователь не найден: UnExisTiK'


@pytest.mark.no_parallel('idm')
def test_remove_unexist_role_from_domain_login():
    clean_domain()
    add_role(domain, '4')
    answer = remove_role(domain, '100')
    assert answer['warning'] == u'У пользователя нет роли 100 с ограничениями {}'
    assert answer['code'] == 0
    answer_2 = remove_role(domain, '9999')
    assert answer_2['warning'] == u'Роль не найдена'
    assert answer_2['code'] == 0


@pytest.mark.no_parallel('idm')
def test_remove_unexist_role_from_passport_login():
    clean_domain()
    add_role(domain, '4', passport=passport_1)
    answer = remove_role(domain, '100', passport=passport_1)
    assert answer['warning'] == u'У пользователя нет роли 100 с ограничениями {u\'passport-login\': u\'balance-test-1\'}'
    assert answer['code'] == 0
    answer_2 = remove_role(domain, '9999', passport=passport_1)
    assert answer_2['warning'] == u'Роль не найдена'
    assert answer_2['code'] == 0


@pytest.mark.no_parallel('idm')
def test_remove_bound_role_from_passport_login():
    clean_domain()
    add_role(domain_2, '4', passport=passport_1)
    assert remove_role(domain, '4', passport=passport_1)['fatal'] == u'Upravlyator.WrongLinkedPassport(Паспортный логин ' + passport_1.login + \
                                                                     u' привязан к другому доменному логину ' + \
                                                                     domain_2.login + u'!)'


# GET_ALL_ROLES


@pytest.mark.skip(u'больше не используем в проде get_all_users')
@pytest.mark.no_parallel('idm')
def test_get_all_roles_from_bd():
    logins = db.balance().execute('''SELECT LOGIN FROM T_PASSPORT
                                      WHERE PASSPORT_ID BETWEEN 1120000000000000 AND 1129999999999999''')
    # print logins
    db_logins = [i['login'] for i in logins]
    get_all_logins = [user['login'] for user in get_all_roles()['users']]
    assert set(db_logins) == set(get_all_logins)


@pytest.mark.skip(u'больше не используем в проде get_all_users')
@pytest.mark.no_parallel('idm')
def test_get_all_roles_format():
    clean_domain()
    assert [i for i in get_all_roles()['users'] if i['login'] == domain.login] == \
           [{u'login': domain.login, u'roles': []}]
    add_role(domain, '4')
    assert [i for i in get_all_roles()['users'] if i['login'] == domain.login] == \
           [{u'login': domain.login, u'roles': [{u'role': u'4'}]}]
    add_role(domain, '10')
    assert [i for i in get_all_roles()['users'] if i['login'] == domain.login] in (
        [{u'login': domain.login, u'roles': [{u'role': u'4'}, {u'role': u'10'}]}],
        [{u'login': domain.login, u'roles': [{u'role': u'10'}, {u'role': u'4'}]}])
    add_role(domain, '4', passport=passport_1)
    assert [i for i in get_all_roles()['users'] if i['login'] == domain.login] in (
        [{u'login': domain.login, u'roles': [{u'role': u'4'}, {u'role': u'10'},
                                             [{u'role': u'4'}, {u'passport-login': passport_1.login}]]}],
        [{u'login': domain.login, u'roles': [{u'role': u'10'}, {u'role': u'4'},
                                             [{u'role': u'4'}, {u'passport-login': passport_1.login}]]}])
    add_role(domain, '10', passport=passport_1)
    assert [i for i in get_all_roles()['users'] if i['login'] == domain.login] in (
        [{u'login': domain.login, u'roles': [{u'role': u'4'}, {u'role': u'10'},
                                             [{u'role': u'4'}, {u'passport-login': passport_1.login}],
                                             [{u'role': u'10'}, {u'passport-login': passport_1.login}]]}],
        [{u'login': domain.login, u'roles': [{u'role': u'4'}, {u'role': u'10'},
                                             [{u'role': u'10'}, {u'passport-login': passport_1.login}],
                                             [{u'role': u'4'}, {u'passport-login': passport_1.login}]]}],
        [{u'login': domain.login, u'roles': [{u'role': u'10'}, {u'role': u'4'},
                                             [{u'role': u'4'}, {u'passport-login': passport_1.login}],
                                             [{u'role': u'10'}, {u'passport-login': passport_1.login}]]}],
        [{u'login': domain.login, u'roles': [{u'role': u'10'}, {u'role': u'4'},
                                             [{u'role': u'10'}, {u'passport-login': passport_1.login}],
                                             [{u'role': u'4'}, {u'passport-login': passport_1.login}]]}])


@pytest.mark.skip(u'больше не используем в проде get_all_users')
@pytest.mark.no_parallel('idm')
def test_get_all_roles_only_passport_role():
    clean_domain()
    add_role(domain, '4', passport=passport_1)
    assert [i for i in get_all_roles()['users'] if i['login'] == domain.login] == \
           [{u'login': domain.login, u'roles': [[{u'role': u'4'}, {u'passport-login': passport_1.login}]]}]


@pytest.mark.skip(u'больше не используем в проде get_all_users')
@pytest.mark.no_parallel('idm')
def test_get_all_roles_internal():  # Для доменных возвращаются все, так как раньше возвращались все.
    clean_domain()
    add_role(domain, '2')
    assert [i for i in get_all_roles()['users'] if i['login'] == domain.login] == \
           [{u'login': domain.login, u'roles': [{u'role': u'2'}]}]


@pytest.mark.skip(u'больше не используем в проде get_all_users')
@pytest.mark.no_parallel('idm')
def test_get_all_roles_only_passport_internal():
    # Для паспортных возвращаются только внешние, чтобы после выкладки кода мы не начали резко
    # отдавать IDMу кучу паспортных с внутренними.
    clean_domain()
    add_role(domain, '2', passport=passport_1)
    assert [i for i in get_all_roles()['users'] if i['login'] == domain.login] == \
           [{u'login': domain.login, u'roles': []}]


# GET_USER_ROLES


@pytest.mark.no_parallel('idm')
def test_get_roles_format():
    clean_domain()
    assert get_roles(domain) == {u'login': domain.login, u'code': 0, u'roles': []}
    add_role(domain, '4')
    assert get_roles(domain) == {u'code': 0, u'login': domain.login, u'roles': [{u'role': u'4'}]}
    add_role(domain, '10')
    print get_roles(domain)
    assert get_roles(domain) in (
        {u'code': 0, u'login': domain.login, u'roles': [{u'role': u'4'}, {u'role': u'10'}]},
        {u'code': 0, u'login': domain.login, u'roles': [{u'role': u'10'}, {u'role': u'4'}]})
    add_role(domain, '4', passport=passport_1)
    assert get_roles(domain) in (
        {u'code': 0, u'login': domain.login, u'roles': [{u'role': u'4'}, {u'role': u'10'},
                                                        [{u'role': u'4'}, {u'passport-login': passport_1.login}]]},
        {u'code': 0, u'login': domain.login, u'roles': [{u'role': u'10'}, {u'role': u'4'},
                                                        [{u'role': u'4'}, {u'passport-login': passport_1.login}]]})
    add_role(domain, '10', passport=passport_1)
    assert get_roles(domain) in (
        {u'code': 0, u'login': domain.login, u'roles': [{u'role': u'4'}, {u'role': u'10'},
                                                        [{u'role': u'4'}, {u'passport-login': passport_1.login}],
                                                        [{u'role': u'10'}, {u'passport-login': passport_1.login}]]},
        {u'code': 0, u'login': domain.login, u'roles': [{u'role': u'4'}, {u'role': u'10'},
                                                        [{u'role': u'10'}, {u'passport-login': passport_1.login}],
                                                        [{u'role': u'4'}, {u'passport-login': passport_1.login}]]},
        {u'code': 0, u'login': domain.login, u'roles': [{u'role': u'10'}, {u'role': u'4'},
                                                        [{u'role': u'4'}, {u'passport-login': passport_1.login}],
                                                        [{u'role': u'10'}, {u'passport-login': passport_1.login}]]},
        {u'code': 0, u'login': domain.login, u'roles': [{u'role': u'10'}, {u'role': u'4'},
                                                        [{u'role': u'10'}, {u'passport-login': passport_1.login}],
                                                        [{u'role': u'4'}, {u'passport-login': passport_1.login}]]})


@pytest.mark.no_parallel('idm')
def test_get_roles_passport():
    clean_domain()
    add_role(domain, '4', passport=passport_1)
    assert get_roles_new(domain) == [('4', passport_1.login)]


@pytest.mark.no_parallel('idm')
def test_get_roles_internal():  # Для доменных возвращаются все, так как раньше возвращались все.
    clean_domain()
    add_role(domain, '2')
    assert get_roles_new(domain) == [('2', domain.login)]


@pytest.mark.no_parallel('idm')
def test_get_roles_passsport_internal():
    # Для паспортных возвращаются только внешние, чтобы после выкладки кода мы не начали резко
    # отдавать IDMу кучу паспортных с внутренними.
    clean_domain()
    add_role(domain, '2', passport=passport_1)
    assert get_roles_new(domain) == []


@pytest.mark.no_parallel('idm')
def test_get_roles_unexist():
    assert get_roles(fake)['fatal'] == u'Пользователь не найден: UnExisTiK'


# STRONG PWD


@pytest.mark.no_parallel('idm')
def test_set_strong_policy_during_add():
    clean_domain()
    api.test_balance().SetPassportAdmsubscribe(passport_1.login, False, 'strongpwd')
    api.test_balance().SetPassportAdmsubscribe(passport_2.login, False, 'strongpwd')
    add_role(domain, '3', passport=passport_1)
    add_role(domain, '10', passport=passport_2)
    assert is_weak_pwd(passport_1)
    assert is_strong_pwd(passport_2)
    add_role(domain, '10', passport=passport_1)
    assert is_strong_pwd(passport_1)


@pytest.mark.no_parallel('idm')
def test_set_strong_policy_during_remove():
    clean_domain()
    api.test_balance().SetPassportAdmsubscribe(passport_1.login, False, 'strongpwd')
    api.test_balance().SetPassportAdmsubscribe(passport_2.login, False, 'strongpwd')
    add_role(domain, '10', passport=passport_1)
    add_role(domain, '10', passport=passport_2)
    remove_role(domain, '10', passport=passport_2)
    assert is_strong_pwd(passport_2)  # BALANCE-32902
    assert is_strong_pwd(passport_1)
    add_role(domain, '3', passport=passport_1)
    remove_role(domain, '3', passport=passport_1)
    assert is_strong_pwd(passport_1)


@pytest.mark.no_parallel('idm')
def test_keep_strong_policy_during_add():
    clean_domain()
    api.test_balance().SetPassportAdmsubscribe(passport_1.login, False, 'strongpwd')
    add_role(domain, '10', passport=passport_1)
    assert is_strong_pwd(passport_1)
    add_role(domain, '2', passport=passport_1)
    assert is_strong_pwd(passport_1)
    add_role(domain, '12', passport=passport_1)
    assert is_strong_pwd(passport_1)
