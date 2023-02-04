# -*- coding: utf-8 -*-
import hamcrest
import pytest

from balance import balance_api as api
from balance import balance_steps as steps
from btestlib import utils, secrets
from btestlib.constants import User
from btestlib.data import defaults


# https://wiki.yandex-team.ru/balance/docs/process/passports/


@pytest.mark.tickets('BALANCE-27894')
@pytest.mark.no_parallel('GetPassportByUid_extended_1')
@pytest.mark.parametrize('relations, is_passport_in_answer',
                         [({'LimitedClientIds': 1}, True),
                          ({'RepresentedClientIds': 1}, False),
                          ({'ServiceClientIds': 1}, False),
                          ({'AllClientIds': 1}, True),
                          ({'LimitedClientIds': 0, 'RepresentedClientIds': 0, 'ServiceClientIds': 0, 'AllClientIds': 1},
                           False),
                          ({'LimitedClientIds': 1, 'RepresentedClientIds': 1, 'ServiceClientIds': 1, 'AllClientIds': 0},
                           True),
                          ({'LimitedClientIds': 1, 'RepresentedClientIds': 1, 'AllClientIds': 0}, True)])
def test_get_passport_from_limited(relations, is_passport_in_answer):
    # login = yb-atst-user-20 , password = secrets.get_secret(*secrets.Passport.CLIENTUID_PWD), uid = 436363530
    user_limited = User(436363530, 'yb-atst-user-20', secrets.get_secret(*secrets.UsersPwd.CLIENTUID_PWD))

    # Отвязываем всех клиентов от представителя
    steps.ClientSteps.unlink_from_login(user_limited.uid)

    agency_id_limited = steps.ClientSteps.create()
    client_id_limited = steps.ClientSteps.create()

    # Привязываем представителя с ограниченным доступом
    api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, agency_id_limited, user_limited.uid,
                                                   [client_id_limited])
    # if is_passport_in_answer:
    #     answer = {'Uid': 436363530, 'LimitedClientIds': [client_id_limited], 'ClientId': agency_id_limited,
    #               'IsMain': 0, 'Login': 'yb-atst-user-20', 'Name': 'Pupkin Vasily'}
    # else:
    #     answer = {'Uid': 436363530, 'ClientId': agency_id_limited, 'IsMain': 0, 'Login': 'yb-atst-user-20',
    #               'Name': 'Pupkin Vasily'}

    if is_passport_in_answer:
        answer = {'Uid': int(user_limited.uid), 'LimitedClientIds': [client_id_limited], 'ClientId': agency_id_limited,
                  'IsMain': 0, 'Login': user_limited.login, 'Name': 'Pupkin Vasily'}
    else:
        answer = {'Uid': int(user_limited.uid), 'ClientId': agency_id_limited, 'IsMain': 0, 'Login': user_limited.login,
                  'Name': 'Pupkin Vasily'}

    utils.check_that(steps.PassportSteps.get_passport_by_uid(user_limited.uid, relations=relations),
                     hamcrest.equal_to(answer),
                     error=u'Ошибка в работе GetPassportByUid при наличии представителя с ограниченнмы доступом')

    utils.check_that(steps.PassportSteps.get_passport_by_login(user_limited.login, relations=relations),
                     hamcrest.equal_to(answer),
                     error=u'Ошибка в работе GetPassportByLogin при наличии представителя с ограниченнмы доступом')


@pytest.mark.tickets('BALANCE-27894')
@pytest.mark.no_parallel('GetPassportByUid_extended_2')
@pytest.mark.parametrize('relations, is_passport_in_answer',
                         [({'LimitedClientIds': 1}, False),
                          ({'RepresentedClientIds': 1}, True),
                          ({'ServiceClientIds': 1}, False),
                          ({'AllClientIds': 1}, True),
                          ({'LimitedClientIds': 0, 'RepresentedClientIds': 0, 'ServiceClientIds': 0, 'AllClientIds': 1},
                           False),
                          ({'LimitedClientIds': 1, 'RepresentedClientIds': 1, 'ServiceClientIds': 1, 'AllClientIds': 0},
                           True),
                          ({'LimitedClientIds': 1, 'RepresentedClientIds': 1, 'AllClientIds': 0}, True)])
def test_get_passport_from_represented(relations, is_passport_in_answer):
    user_represented = User(450606159, 'yb-atst-user-21', secrets.get_secret(*secrets.UsersPwd.CLIENTUID_PWD))
    # login = yb-atst-user-21, password = secrets.get_secret(*secrets.Passport.CLIENTUID_PWD), uid = 450606159

    # Отвязываем всех клиентов от представителя
    steps.ClientSteps.unlink_from_login(user_represented.uid)

    client_id_represented = steps.ClientSteps.create()

    # Привязываем бухгалтерский логин
    steps.ClientSteps.add_accountant_role(user_represented, client_id_represented)

    # if is_passport_in_answer:
    #     answer = {'RepresentedClientIds': [client_id_represented], 'IsMain': 0, 'Login': 'yb-atst-user-21',
    #               'Uid': 450606159, 'Name': 'Pupkin Vasily'}
    # else:
    #     answer = {'IsMain': 0, 'Login': 'yb-atst-user-21', 'Uid': 450606159, 'Name': 'Pupkin Vasily'}

    if is_passport_in_answer:
        answer = {'RepresentedClientIds': [client_id_represented], 'IsMain': 0, 'Login': user_represented.login,
                  'Uid': int(user_represented.uid), 'Name': 'Pupkin Vasily'}
    else:
        answer = {'IsMain': 0, 'Login': user_represented.login, 'Uid': int(user_represented.uid), 'Name': 'Pupkin Vasily'}

    utils.check_that(steps.PassportSteps.get_passport_by_uid(user_represented.uid, relations=relations),
                     hamcrest.equal_to(answer),
                     error=u'Ошибка в работе GetPassportByUid при наличии бухгалтерского логина')

    utils.check_that(steps.PassportSteps.get_passport_by_login(user_represented.login, relations=relations),
                     hamcrest.equal_to(answer),
                     error=u'Ошибка в работе GetPassportByLogin при наличии бухгалтерского логина')


@pytest.mark.tickets('BALANCE-27894')
@pytest.mark.no_parallel('GetPassportByUid_extended_3')
@pytest.mark.parametrize('relations, is_passport_in_answer',
                         [({'LimitedClientIds': 1}, False),
                          ({'RepresentedClientIds': 1}, False),
                          ({'ServiceClientIds': 1}, True),
                          ({'AllClientIds': 1}, True),
                          ({'LimitedClientIds': 0, 'RepresentedClientIds': 0, 'ServiceClientIds': 0, 'AllClientIds': 1},
                           False),
                          ({'LimitedClientIds': 1, 'RepresentedClientIds': 1, 'ServiceClientIds': 1, 'AllClientIds': 0},
                           True),
                          ({'LimitedClientIds': 1, 'ServiceClientIds': 1, 'AllClientIds': 0}, True)])
def test_get_passport_from_service(relations, is_passport_in_answer):
    user_service = User(450606171, 'yb-atst-user-22', secrets.get_secret(*secrets.UsersPwd.CLIENTUID_PWD))
    # login = yb-atst-user-22 , password = secrets.get_secret(*secrets.Passport.CLIENTUID_PWD), uid = 450606171

    # Отвязываем всех клиентов от представителя
    steps.ClientSteps.unlink_from_login(user_service.uid)

    client_id_service = steps.ClientSteps.create({'SERVICE_ID': 23})

    # Связка через t_service_client
    api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, client_id_service, user_service.uid)

    # if is_passport_in_answer:
    #     answer = {'IsMain': 0, 'Login': 'yb-atst-user-22', 'Name': 'Pupkin Vasily',
    #               'ServiceClientIds': [{'ServiceID': 23, 'ClientID': client_id_service}], 'Uid': 450606171}
    # else:
    #     answer = {'IsMain': 0, 'Login': 'yb-atst-user-22', 'Name': 'Pupkin Vasily', 'Uid': 450606171}

    if is_passport_in_answer:
        answer = {'IsMain': 0, 'Login': user_service.login, 'Name': 'Pupkin Vasily',
                  'ServiceClientIds': [{'ServiceID': 23, 'ClientID': client_id_service}], 'Uid': int(user_service.uid)}
    else:
        answer = {'IsMain': 0, 'Login': user_service.login, 'Name': 'Pupkin Vasily', 'Uid': int(user_service.uid)}

    utils.check_that(steps.PassportSteps.get_passport_by_uid(user_service.uid, relations=relations),
                     hamcrest.equal_to(answer),
                     error=u'Ошибка в работе GetPassportByUid при наличии связки через t_service_client')

    utils.check_that(steps.PassportSteps.get_passport_by_login(user_service.login, relations=relations),
                     hamcrest.equal_to(answer),
                     error=u'Ошибка в работе GetPassportByLogin при наличии связки через t_service_client')
