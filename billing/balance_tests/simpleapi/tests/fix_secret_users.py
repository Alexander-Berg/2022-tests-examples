import pytest

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common import logger
from simpleapi.common.oauth import Auth
from simpleapi.data.cards_pool import get_card
from simpleapi.data.uids_pool import secret, all_, mimino, test_passport, mutable, \
    user_is_marked, unmark_user, rbs, sberbank, phone_test_passport, uber, routing
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import trust_steps as trust

__author__ = 'fellow'

log = logger.get_logger()


def ids_user(val):
    return str(val)


@pytest.mark.parametrize('user', secret.values(), ids=ids_user)
def test_fix_users(user):
    card = get_card()
    token = trust.get_auth_token(Auth.get_auth(user), user)['access_token']
    resp = trust.bind_card(token, card)
    assert resp['status'] == 'success', 'Error while binding card. Response: %s' % resp
    log.debug('Now user {} get the card {}'.format(user, card))


@pytest.mark.parametrize('user', secret.values(), ids=ids_user)
def test_check_users(user):
    with check_mode(CheckMode.IGNORED):
        paymethods = simple.list_payment_methods(Services.STORE, user)
    assert paymethods


@pytest.mark.parametrize('user', all_.values(), ids=ids_user)
def test_unbind_all_users(user):
    trust.process_unbinding(user)


@pytest.mark.parametrize('pool', (mimino, test_passport, mutable, rbs, sberbank,
                                  phone_test_passport, uber, routing),
                         ids=('mimino', 'test_passport', 'mutable', 'rbs',
                              'sberbank', 'phone_users', 'uber', 'routing'))
def test_print_users_holder_info(pool):
    count = 0

    for user in pool.values():
        if user_is_marked(user):
            count += 1

    reporter.log('Users all {}, users in use {}'.format(len(pool), count))


@pytest.mark.parametrize('pool', (mimino, test_passport, mutable, rbs, sberbank,
                                  phone_test_passport, uber, routing),
                         ids=('mimino', 'test_passport', 'mutable', 'rbs',
                              'sberbank', 'phone_users', 'uber', 'routing'))
def test_free_users(pool):
    for user in pool.values():
        unmark_user(user)


def test_clear():
    from simpleapi.steps import mongo_steps as mongo
    mongo.PaymentQueue.clear()


if __name__ == '__main__':
    pytest.main()
