# coding=utf-8

import balance.balance_steps as steps
import balance.balance_db as db
import btestlib.environments as env
import btestlib.utils as utils
from btestlib.constants import Passports
from balance.tests.conftest import get_free_user


class CartSteps(object):

    @staticmethod
    def get_session(client_id):
        test_user = next(get_free_user())()
        steps.ClientSteps.link(client_id, test_user.login)
        return steps.passport_steps.auth_session(test_user, Passports.PROD)

    @staticmethod
    def get_session_and_token(client_id, session=None, service_id=7):
        if not session:
            session = CartSteps.get_session(client_id)
        resp = CartSteps.get_item_list(session, service_id)
        return session, resp.cookies['_csrf']

    @staticmethod
    def get_item_list(session, service_id=None, detailed=None, lang=None):
        url = '{}/cart/item/list'.format(env.balance_env().snout_url)
        params = {'service_id': service_id}
        headers = {'Referer': '{}/cart.xml?service_id={}'.format(env.balance_env().balance_ci, service_id)}
        response = utils.call_http(session, url, params, custom_headers=headers, method='GET')
        return response

    @staticmethod
    def post_item_add(session, service_id, service_order_id, qty, old_qty, _csrf):
        url = '{}/cart/item/add'.format(env.balance_env().snout_url)
        params = {'service_id': service_id, 'service_order_id': service_order_id, 'qty': qty, '_csrf': _csrf}
        response = utils.call_http(session, url, params)
        return response

    @staticmethod
    def delete(session, cart_item_ids, _csrf):
        url = '{}/cart/item/delete'.format(env.balance_env().snout_url)
        params = {'cart_item_ids': []}
        response = utils.call_http(session, url, params)
        return response

    @staticmethod
    def post_create_request(session, _csrf, service_id=None, item_ids=None, return_path=None):
        url = '{}/cart/create-request'.format(env.balance_env().snout_url)
        params = {'return_path': return_path, '_csrf': _csrf}
        if service_id:
            params.update({'service_id': service_id})
        if item_ids:
            str_item_ids = ','.join([str(item) for item in item_ids])
            params.update({'item_ids': str_item_ids})
        response = utils.call_http(session, url, params)
        return response


if __name__ == "__main__":
    pass
    resp = CartSteps.get_item_list(7)
    _csrf = resp.cookies['_csrf']
    CartSteps.post_item_add(7, 41194380, 50, None, _csrf)
