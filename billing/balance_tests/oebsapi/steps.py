# coding: utf-8
__author__ = 'chihiro'
import requests

import balance.balance_db as db
import btestlib.environments as envs
from btestlib import utils


class OebsapiSteps(object):
    @staticmethod
    def _send(params, method='query_table', status_code=200):
        response = requests.post(envs.oebsapi_env().url + '/edo/' + method, params,
                                 cert=(utils.project_file('balalayka/collection/cert.pem'),
                                       utils.project_file('balalayka/collection/cert_private.key')),
                                 stream=True, timeout=30)
        assert response.status_code == status_code
        return response

    @staticmethod
    def query_table(table_name, where=False):
        params = {'table': table_name, 'where': 'status is null'} if where else {'table': table_name}
        return OebsapiSteps._send(params)

    @staticmethod
    def update_table(table_name, update, where):
        params = {'table': table_name, 'update': update, 'where': where}
        return OebsapiSteps._send(params, 'update_table')


class OebsSteps(object):
    @staticmethod
    def get_data(table, data=None):
        return db.oebs().execute(u'select * from {} where {}'.format(table, data if data else u'rownum=1'))[0]
