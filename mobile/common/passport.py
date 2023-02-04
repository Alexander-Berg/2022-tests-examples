import logging
import requests
import ticket_parser2 as tp2
import time
from blackbox import JsonBlackbox
from django.conf import settings
from ticket_parser2 import BlackboxClientId
from ticket_parser2.low_level import ServiceContext
from tvm2.protocol import BLACKBOX_MAP

from yaphone.advisor.common.tvm import TVMServiceTicketError, add_service_ticket_header

logger = logging.getLogger(__name__)

# passing this attribute id in request to blackbox to get info about Yandex.Plus subscription
# for more info see
# https://wiki.yandex-team.ru/passport/plus/#kakpoluchitinformacijujavljaetsjalipolzovatelempodpischikomjandeks.pljusa
HAS_PLUS_ATTRIBUTE_ID = '1015'

# https://wiki.yandex-team.ru/passport/dbmoving/#tipyaliasov
PASSPORT_LOGIN_ALIAS_ID = '1'

STATUS_KEY = u'status'
ERROR_KEY = u'error'


class PassportTokenError(Exception):
    pass


def request_blackbox_api(params, blackbox_client, is_fallback):
    result = {}
    try:
        # Going first to mimino, if falling back to test blackbox in testing environment.
        # For more info see https://st.yandex-team.ru/ADVISOR-1787
        if is_fallback:
            headers = add_tvm_ticket_from_blackbox_test_env()
        else:
            headers = add_service_ticket_header(blackbox_client.value)

        response = requests.get(get_blackbox_url(blackbox_client), params=params, headers=headers)
        if response.ok:
            result = response.json()
        else:
            logger.warning('Cant extract info from blackbox, status_code=%s, response=%s', response.status_code,
                           response.content)
    except (requests.exceptions.RequestException, requests.exceptions.BaseHTTPError) as e:
        logger.error('Blackbox request failed: %s', e)  # don't publish url by security reason (oauth_token)
    except TVMServiceTicketError as e:
        logger.error(e)
    except Exception as e:
        logger.error('unexpected error occurred:', e)
        raise
    return result


def get_info(authorization_token, user_ip, blackbox_client=settings.BLACKBOX_CLIENT, is_fallback=False):
    params = {
        'method': 'oauth',
        'format': 'json',
        'oauth_token': authorization_token,
        'userip': user_ip,
        'aliases': PASSPORT_LOGIN_ALIAS_ID,
        'attributes': HAS_PLUS_ATTRIBUTE_ID,
    }

    answer = request_blackbox_api(params, blackbox_client=blackbox_client, is_fallback=is_fallback)
    if STATUS_KEY in answer:
        if answer[STATUS_KEY]['id'] != 0:
            msg = 'passed oauth token cannot be used: ' + answer[ERROR_KEY]
            logger.info(msg)
            raise PassportTokenError(msg)

    if 'uid' in answer:
        try:
            passport_info = {
                'passport_uid': answer['uid']['value'],
                'has_plus': answer['attributes'].get(HAS_PLUS_ATTRIBUTE_ID) == '1',
                'is_portal_login': PASSPORT_LOGIN_ALIAS_ID in answer['aliases'],
            }
            return passport_info
        except (KeyError, TypeError) as e:
            logger.error('request_blackbox_api() returned malformed answer: %s', e)
            return {}
    return {}


def get_blackbox_url(blackbox_client):
    return BLACKBOX_MAP[blackbox_client]['url']


# Need to go both to mimino and test blackbox in testing env. But TVM2 is a Singleton, so can't change blackbox_client
# That's why using another api to get service tickets
# TODO: delete this and all fallback logic when testing is finished
def add_tvm_ticket_from_blackbox_test_env(headers=None):
    if headers is None:
        headers = {}

    secret = settings.TVM_CLIENT_SECRET
    tvm_api_url = 'tvm-api.yandex.net'
    ts = int(time.time())
    src = int(settings.TVM_CLIENT_ID)
    dst = int(BlackboxClientId.Test.value)

    # Getting TVM keys
    tvm_keys = requests.get(
        'https://{tvm_api_url}/2/keys?lib_version={version}'.format(
            tvm_api_url=tvm_api_url,
            version=tp2.__version__,
        )
    ).content

    # Create context
    service_context = ServiceContext(src, secret, tvm_keys)

    # Getting tickets
    ticket_response = requests.post(
        'https://%s/2/ticket/' % tvm_api_url,
        data={
            'grant_type': 'client_credentials',
            'src': src,
            'dst': dst,
            'ts': ts,
            'sign': service_context.sign(ts, dst)
        }
    ).json()

    ticket_for_dst = ticket_response[str(dst)]['ticket']

    headers['X-Ya-Service-Ticket'] = ticket_for_dst
    return headers


def get_blackbox_api_client():
    return JsonBlackbox(
        tvm2_client_id=settings.TVM_CLIENT_ID,
        tvm2_secret=settings.TVM_CLIENT_SECRET,
        blackbox_client=settings.BLACKBOX_CLIENT,
    )


def user_info_by_login(login):
    return _user_info(login=login)


def user_info_by_uid(uid):
    return _user_info(uid=uid)


def _user_info(**kwargs):
    blackbox_api = get_blackbox_api_client()

    params = {
        'userip': '127.0.0.1',  # Necessary parameter, so passing localhost to ignore it
        'aliases': PASSPORT_LOGIN_ALIAS_ID,
        'attributes': HAS_PLUS_ATTRIBUTE_ID,
    }
    params.update(kwargs)

    response = blackbox_api.userinfo(**params)
    user = response['users'][0]
    if not user['id']:
        return {}

    return {
        'uid': user['id'],
        'login': user['login'],
        'has_plus': user['attributes'].get(HAS_PLUS_ATTRIBUTE_ID) == '1',
        'is_portal_login': PASSPORT_LOGIN_ALIAS_ID in user['aliases'],
    }
