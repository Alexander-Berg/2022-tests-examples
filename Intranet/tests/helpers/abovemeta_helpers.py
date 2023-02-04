import json
from copy import deepcopy
from io import StringIO
from base64 import b64encode
from collections import defaultdict
from urllib.parse import urlencode, urlunparse, urlparse, urlsplit, parse_qsl

from tornado import gen
from tornado.httpclient import HTTPResponse

from intranet.search.abovemeta import settings
from intranet.search.abovemeta.steps.auth import USER_ORGS_ATTR


class RequesterFetchMock:
    """ Мок для реквестера в abovemeta. Патчит все запросы, которые делаются в abovemeta
    """
    REQ_GET_SERVICE_TICKET = 'tvm.get_service_ticket'
    REQ_CHECK_SERVICE_TICKET = 'tvm.check_service_ticket'
    REQ_AUTH = 'auth.auth'
    REQ_STAFF_PERSON = 'staff.person'
    REQ_STAFF_GROUPS = 'staff.groups'
    REQ_STAFF_GROUP_PERMISSIONS = 'staff.group_permissions'
    REQ_ST_QUEUES = 'st.queues'
    REQ_UAAS = 'ab.uaas'
    REQ_DIRECTORY_USER = 'directory.user'
    REQ_FEMIDA_ACL = 'acl.femida_acl'
    REQ_CONDUCTOR_USER = 'conductor.conductor_user'

    REQ_API_BEGEMOT = 'api.begemot'
    REQ_API_WIZARD_RULES = 'api.external_wizard_rules'
    REQ_API_FORMULAS = 'api.formulas'
    REQ_API_REVISIONS = 'api.revisions'
    REQ_API_FEATURES = 'api.features'
    REQ_API_FACET_LABELS = 'api.facets_labels'
    REQ_API_PERMISSIONS = 'api.permissions'
    REQ_API_USER_META = 'api.user_meta'

    REQ_SEARCH_ALL = 'search'
    REQ_SEARCH_RESULTS = 'search.search_results'
    REQ_SEARCH_WIZARD = 'search.wizard'
    REQ_SEARCH_DECIDER = 'decider'

    REQUEST_DEFAULT = {
        REQ_GET_SERVICE_TICKET: {
            'body': {
                'blackbox': {
                    'ticket': '3:serv:someticket'
                },
                'directory': {
                    'ticket': '3:serv:anotherticket'
                }
            }
        },
        REQ_CHECK_SERVICE_TICKET: {'body': {'src': 'some_service'}},
        REQ_AUTH: {
            'body': {
                'status': {
                    'value': 'VALID'
                },
                'login': 'some_ya_user',
                'uid': {
                    'value': '1' * 10
                },
                'user_ticket': 'tvm2:user:ticket',
                'attributes': {
                    USER_ORGS_ATTR: '1'
                },
            },
        },
        REQ_STAFF_PERSON: {
            'body': {
                'id': 1,
            },
        },
        REQ_STAFF_GROUPS: {
            'body': {
                'id': 1,
                'language': {
                    'auto_translate': False,
                },
                'groups': [],
                'location': {
                    'office': {},
                    'table': {
                        'floor': {}
                    }
                },
                'official': {
                    'affiliation': 'yandex',
                },
            },
        },
        REQ_STAFF_GROUP_PERMISSIONS: {
            'body': {
                'result': [],
            }
        },
        REQ_ST_QUEUES: {'body': {}},
        REQ_UAAS: {
            'body': 'USERSPLIT',
            'headers': {
                'Content-Type': 'text/plain',
                'X-Yandex-ExpBoxes': '',
                'X-Yandex-ExpFlags': ''
            }
        },
        REQ_DIRECTORY_USER: {
            'body': {
                'org_id': 1,
                'is_admin': False,
                'services': [{'slug': s} for s in settings.ISEARCH['scopes']],
                'organization': {'organization_type': 'common'},
            }
        },
        REQ_FEMIDA_ACL: {'body': {'allowed': True}},
        REQ_CONDUCTOR_USER: {'body': {'value': {'role': 'user'}}},

        REQ_API_BEGEMOT: {'body': {}},
        REQ_API_WIZARD_RULES: {'body': []},
        REQ_API_FORMULAS: {'body': []},
        REQ_API_REVISIONS: {'body': []},
        REQ_API_FEATURES: {'body': {}},
        REQ_API_FACET_LABELS: {'body': {}},
        REQ_API_PERMISSIONS: {'body': {
            'common': [],
            'search': list(settings.ISEARCH['scopes']),
            'suggest': list(settings.ISEARCH['suggest']['layers']),
        }},
        REQ_API_USER_META: {'body': {
            'frequently_searched_people': {},
            'recently_searched_people': {},
        }},

        REQ_SEARCH_DECIDER: {'body': {}},
        REQ_SEARCH_ALL: {'body': {'request_query': 'test'}},
    }

    def __init__(self):
        self.reset()

    def reset(self):
        self.called = defaultdict(list)
        self.state = None
        self.request_mocking = {}
        self.set_default_requests()

    def set_default_requests(self):
        for name, params in self.REQUEST_DEFAULT.items():
            self.patch(name, **params)

    @gen.coroutine
    def __call__(self, request, state=None):
        key = f'{request.type}.{request.name}'
        self.called[key].append(request)
        self.state = state

        if key not in self.request_mocking:
            if request.type not in self.request_mocking:
                raise Exception('Try to call non mocked function: %s', key)
            else:
                key = request.type

        m = self.request_mocking[key]
        response = HTTPResponse(request, code=m['code'],
                                headers=m.get('headers', {}),
                                buffer=StringIO(m.get('body', '')))
        raise gen.Return(response)

    def patch(self, name, code=200, body=None, headers=None):
        headers = headers or {}
        if not headers.get('Content-Type'):
            headers['Content-Type'] = 'application/json'
        if headers['Content-Type'] == 'application/json':
            body = json.dumps(body)
        self.request_mocking[name] = {'code': code, 'body': body, 'headers': headers}

    def patch_blackbox(self, status='VALID', uid='1', login='user', body=None, user_orgs_attr='1'):
        if not body:
            body = {
                'status': {'value': status},
                'uid': {'value': uid},
                'login': login,
            }
        if 'attributes' not in body:
            body['attributes'] = {USER_ORGS_ATTR: user_orgs_attr}
        self.patch(self.REQ_AUTH, body=body)

    def patch_user_ticket_blackbox(self, uid=None, login=None, user_orgs_attr='1'):
        body = {
            'users': [{
                'uid': {'value': uid},
                'login': login,
                'attributes': {USER_ORGS_ATTR: user_orgs_attr}
            }]
        }
        self.patch_blackbox(body=body)

    def patch_check_service_ticket(self, code=200, body=None, headers=None):
        self.patch(self.REQ_CHECK_SERVICE_TICKET, code=code, body=body, headers=headers)

    def patch_uaas(self, buckets='', flags=None, features=None, code=200):
        if features and not flags:
            flags = {'HANDLER': 'INTRASEARCH',
                     'CONTEXT': {'INTRASEARCH': {'FEATURES': features}}}
        if isinstance(flags, dict):
            flags = encode_uaas_flags(flags)

        headers = {'Content-Type': 'text/plain',
                   'X-Yandex-ExpBoxes': buckets,
                   'X-Yandex-ExpFlags': flags}
        self.patch(self.REQ_UAAS, code=code, body='USERSPLIT', headers=headers)

    def patch_api_revisions(self, revision_list):
        self.patch(self.REQ_API_REVISIONS, body=revision_list)

    def patch_api_features(self, features_list):
        self.patch(self.REQ_API_FEATURES, body=features_list)

    def patch_api_begemot(self, first_name, last_name):
        body = {'Fio': [{'FirstName': first_name, 'LastName': last_name}]}
        self.patch(self.REQ_API_BEGEMOT, body=body)

    def patch_api_directory_for_external_admin(self, organizations=None):
        body = {'result': [{'id': org_id} for org_id in (organizations or [])]}
        self.patch(self.REQ_DIRECTORY_USER, body=body)

    def patch_api_permissions(self, *, common=None, search=None, suggest=None):
        body = {
            'common': common or [],
            'search': search or [],
            'suggest': suggest or [],
        }
        self.patch(self.REQ_API_PERMISSIONS, body=body)

    def patch_staff_groups(self, affiliation='yandex'):
        body = deepcopy(self.REQ_STAFF_GROUPS)
        body['official']['affiliation'] = affiliation
        self.patch(self.REQ_STAFF_GROUPS, body=body)

    def patch_api_user_meta(self, *, frequently_searched_people=None,
                            recently_searched_people=None):
        body = {
            'frequently_searched_people': frequently_searched_people or {},
            'recently_searched_people': recently_searched_people or {},
        }
        self.patch(self.REQ_API_USER_META, body=body)


def request_search(http_client, base_url, params=None, headers=None):
    if not params or 'scope' not in params:
        params = params or {}
        params['scope'] = 'search' if settings.APP_NAME == 'isearch' else 'directory'

    if params:
        url_parts = list(urlparse(base_url))
        url_parts[4] = urlencode(params)
        url = urlunparse(url_parts)
    else:
        url = base_url
    return http_client.fetch(url, raise_error=False, headers=headers)


def request_suggest(http_client, base_url, params=None, headers=None):
    url = base_url + '/suggest/'
    return request_search(http_client, url, params, headers)


def parse_response(response):
    return json.loads(response.body)


def get_request_params(request):
    params = {}
    params.update(request.params)
    params.update(dict(parse_qsl(urlsplit(request.url).query)))
    return params


def encode_uaas_flags(flags):
    flags_as_bytes = json.dumps(flags).encode('utf-8')
    return b64encode(flags_as_bytes).decode('utf-8')
