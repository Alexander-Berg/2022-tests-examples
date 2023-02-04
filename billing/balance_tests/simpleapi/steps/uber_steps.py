# coding: utf-8

from hamcrest import has_key

from simpleapi.common.utils import call_http, return_only
from simpleapi.common.oauth import AuthUber as Auth
import btestlib.reporter as reporter
from btestlib.utils import CheckMode
from btestlib import environments

__author__ = 'fellow'


class Authorization(object):
    @staticmethod
    @return_only('access_token')
    @CheckMode.result_matches(has_key('access_token'))
    def get_token_for(user):
        oauth_url = environments.simpleapi_env().uber_oauth_url

        auth = Auth.get_auth(user)

        params = {'client_id': auth.client_id,
                  'client_secret': auth.client_secret,
                  'grant_type': 'client_credentials',
                  'scope': 'user.payment_methods.grant user.payment_methods.read'}

        with reporter.step(u'Получаем oauth-token Убера'):
            return call_http(oauth_url, params)
