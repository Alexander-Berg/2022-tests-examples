# coding=utf-8

import balance.balance_steps as steps
import btestlib.environments as env
import btestlib.utils as utils
from btestlib.constants import Passports, Users, Roles
from .api_steps import HEADERS
from balance.tests.conftest import get_free_user


class InvoiceSteps(object):
    @staticmethod
    def get_session_and_token(invoice_id):
        session = steps.passport_steps.auth_session(Users.YB_ADM, Passports.PROD)
        url = '{}/common/hello'.format(env.balance_env().snout_url)
        headers = {'Referer': '{}/common/hello'.format(env.balance_env().balance_ai)}
        resp = utils.call_http(session, url, {}, method='GET', custom_headers=headers)
        return session, resp.cookies['_csrf']

    @staticmethod
    def rollback(session, _csrf, invoice_id, amount, unused_funds_lock='OFF'):
        url = '{}/invoice/rollback'.format(env.balance_env().snout_url)
        params = {'unused_funds_lock': unused_funds_lock, 'invoice_id': invoice_id, 'amount': amount, '_csrf': _csrf}
        response = utils.call_http(session, url, params, custom_headers=HEADERS['POST'], method='POST')
        return response

    @staticmethod
    def transfer_from_unfunds(session, _csrf, invoice_id, dst_order_id):
        url = '{}/invoice/transfer/from-unused-funds'.format(env.balance_env().snout_url)
        params = {'invoice_id': invoice_id, 'dst_order_id': dst_order_id, '_csrf': _csrf}
        response = utils.call_http(session, url, params, custom_headers=HEADERS['POST'], method='POST')
        return response

    @staticmethod
    def get_session(client_id, required_roles=[Roles.ADMIN_0]):
        test_user = next(get_free_user())()
        steps.ClientSteps.link(client_id, test_user.login)
        for role in required_roles:
            steps.UserSteps.set_role(test_user, role)
        return steps.passport_steps.auth_session(test_user, Passports.PROD)

    @staticmethod
    def get_invoice_data(session, invoice_id):
        url = '{}/invoice'.format(env.balance_env().snout_url)
        params = {'invoice_id': invoice_id}
        headers = {'Referer': 'https://admin-balance.greed-tm.paysys.yandex.ru/'}
        response = utils.call_http(session, url, params, method='GET', custom_headers=headers)
        return response
