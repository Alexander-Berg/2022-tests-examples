import hamcrest
import pytest

import btestlib.matchers as matchers
import btestlib.utils as utils
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib.constants import User

LIMITED_ROLE = 101


def check_role_client_users(user, limited_list):
    role_client_users = db.get_role_client_user_by_passport(user.uid)
    rows = []
    for client in limited_list:
        row = {'passport_id': user.uid,
               'client_id': client,
               'role_id': LIMITED_ROLE}
        rows.append(row)
    utils.check_that(role_client_users, matchers.contains_dicts_with_entries(rows, same_length=True))


def check_get_passport_by_method(user, client_id, limited_list, passport_info):
    result = {'Uid': user.uid,
              'ClientId': client_id,
              'IsMain': 0,
              'Login': user.login,
              'Name': 'Pupkin Vasily'}
    if limited_list:
        result['LimitedClientIds'] = set(limited_list)
    utils.check_that([passport_info], matchers.contains_dicts_with_entries([result], same_length=True))


@pytest.mark.no_parallel('link_limited')
@pytest.mark.parametrize("limited_list, limited_list_after", [
    # (lambda: None, lambda: None),
    (lambda: [None], lambda: None),
    (lambda: [], lambda: None),
    (lambda: [], lambda: [steps.ClientSteps.create()]),
    (lambda: [steps.ClientSteps.create()], lambda: None),
    (lambda: [steps.ClientSteps.create()], lambda: []),
    (lambda: [steps.ClientSteps.create(),
              steps.ClientSteps.create()], lambda: None),
    (lambda: [steps.ClientSteps.create(), steps.ClientSteps.create()],
     lambda: [steps.ClientSteps.create(), steps.ClientSteps.create()])
])
def test_ordinary_link(limited_list, limited_list_after):
    user = User(436363445, 'yb-atst-user-16')
    steps.ClientSteps.unlink_from_login(user.uid)

    client_id = steps.ClientSteps.create()
    limited_list = limited_list()
    steps.ClientSteps.fair_link(client_id, user.uid, limited_list=limited_list)

    linked_client_id = db.get_passport_by_login(user.login)[0]['client_id']
    utils.check_that(linked_client_id, hamcrest.equal_to(client_id))

    if limited_list is not None:
        check_role_client_users(user, limited_list)

        passport_info = steps.PassportSteps.get_passport_by_uid(user.uid, relations={'LimitedClientIds': 1})
        check_get_passport_by_method(user, client_id, limited_list, passport_info)

        passport_info = steps.PassportSteps.get_passport_by_login(user.login, relations={'LimitedClientIds': 1})
        check_get_passport_by_method(user, client_id, limited_list, passport_info)

    limited_list_after = limited_list_after()

    if limited_list_after is not None:
        steps.ClientSteps.fair_link(client_id, user.uid, limited_list=limited_list_after)
        check_role_client_users(user, limited_list_after)

        passport_info = steps.PassportSteps.get_passport_by_uid(user.uid, relations={'LimitedClientIds': 1})
        check_get_passport_by_method(user, client_id, limited_list_after, passport_info)

        passport_info = steps.PassportSteps.get_passport_by_login(user.login, relations={'LimitedClientIds': 1})
        check_get_passport_by_method(user, client_id, limited_list_after, passport_info)


@pytest.mark.no_parallel('link_limited')
@pytest.mark.parametrize("limited_list", [lambda: [steps.ClientSteps.create()]])
def test_link_then_remove(limited_list):
    user = User(436363445, 'yb-atst-user-16')
    steps.ClientSteps.unlink_from_login(user.uid)
    client_id = steps.ClientSteps.create()
    limited_list = limited_list()
    steps.ClientSteps.fair_link(client_id, user.uid, limited_list=limited_list)
    linked_client_id = db.get_passport_by_login(user.login)[0]['client_id']
    utils.check_that(linked_client_id, hamcrest.equal_to(client_id))
    check_role_client_users(user, limited_list)
    steps.ClientSteps.fair_unlink_from_login(client_id, user.uid)
    check_role_client_users(user, limited_list=[])
