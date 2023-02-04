import collections
import contextlib
import datetime
import json
import weakref

import mock

from django.contrib.admin import ACTION_CHECKBOX_NAME
from django.urls import reverse
from django.utils import timezone
from django.contrib.auth.models import Group, Permission
from django.contrib.contenttypes.models import ContentType
from django.test.client import Client
from django.utils.encoding import force_text
from rest_framework import status
from library.python.vault_client.errors import ClientError

from intranet.crt.constants import AFFILIATION, CERT_STATUS, TAG_TYPE, TAG_FILTER_TYPE, CA_NAME, CERT_TYPE
from intranet.crt.tags.models import CertificateTag, TagFilter
from intranet.crt.users.models import CrtUser
from intranet.crt.core.ca.globalsign import GlobalSignZeepClient, GlobalSignProductionCA
from intranet.crt.core.models import CertificateType, Certificate, Host, ApproveRequest
from intranet.crt.utils.time import moscow_tz


class ResponseMock(object):
    def __init__(self, content, url=None, status_code=status.HTTP_200_OK):
        self.content = content
        self.url = url
        self.status_code = status_code

    @property
    def text(self):
        return force_text(self.content)


@contextlib.contextmanager
def capture_raw_http(answer=None, status_code=status.HTTP_200_OK, side_effect=None):
    with mock.patch('requests.sessions.Session.request') as mocked:
        if answer is not None:
            mocked.return_value = ResponseMock(answer, status_code=status_code)
        if side_effect is not None:
            mocked.side_effect = side_effect
        yield mocked


class CrtClient(Client):
    def __init__(self, *args, **kwargs):
        super(CrtClient, self).__init__(*args, **kwargs)
        self.json = JsonClient(weakref.proxy(self))


class JsonClient(object):
    """Класс, аналогичный django.test.client.Client, но делающий все запросы с заголовком content_type,
    равным application/json, а для запросов post, put и patch кодирующий тело запроса в JSON.
    Также добавляет к ответу метод json(), который пытается декодировать тело ответа из формата JSON.
    """
    def __init__(self, client):
        self.client = client

    def get(self, path, data=None, **extra):
        if data is None:
            data = {}
        response = self.client.get(path, data=data, **extra)
        return self.jsonify(response)

    def post(self, path, data=None, content_type='application/json', **extra):
        if data is None:
            data = {}
        data = json.dumps(data) if data else data
        return self.jsonify(self.client.post(path, data=data, content_type=content_type, **extra))

    def head(self, path, data=None, **extra):
        return self.jsonify(self.client.head(path, data=data, **extra))

    def options(self, path, data='', content_type='application/json', **extra):
        return self.jsonify(self.client.options(path, data=data, content_type=content_type, **extra))

    def put(self, path, data='', content_type='application/json', **extra):
        data = json.dumps(data) if data else data
        return self.jsonify(self.client.put(path, data=data, content_type=content_type, **extra))

    def patch(self, path, data='', content_type='application/json', **extra):
        data = json.dumps(data) if data else data
        return self.jsonify(self.client.patch(path, data=data, content_type=content_type, **extra))

    def delete(self, path, data='', content_type='application/json', **extra):
        data = json.dumps(data) if data else data
        return self.jsonify(self.client.delete(path, data=data, content_type=content_type, **extra))

    def jsonify(self, response):
        response.json = lambda: json.loads(force_text(response.content)) if hasattr(response, 'content') else None
        return response


def create_permission(codename, app_label_name, model_name):
    content_type = ContentType.objects.get(app_label=app_label_name, model=model_name)
    permission, created = Permission.objects.get_or_create(codename=codename,
                                                           content_type=content_type)
    if created:
        permission.name = codename
        permission.save()

    return permission


def create_group(name, permissions=None):
    if permissions is None:
        permissions = []

    group, created = Group.objects.get_or_create(name=name)
    if created:
        group.save()

    for permission in permissions:
        group.permissions.add(permission)

    return group


def create_host(hostname):
    return Host.objects.create(hostname=hostname)


def create_user(login, is_superuser=False, is_staff=False, groups=None, is_robot=False,
                is_active=True, affiliation=AFFILIATION.YANDEX):
    if groups is None:
        groups = []

    date_joined = datetime.datetime(2016, 1, 1)
    aware_date_joined = moscow_tz.localize(date_joined)

    user, created = CrtUser.objects.get_or_create(
        username=login,
        first_name=login,
        last_name=login,
        full_name='{} {}'.format(login.capitalize(), login.capitalize()),  # Не оч круто, конечно
        first_name_ru=login,
        last_name_ru=login,
        full_name_ru='{} {}'.format(login.capitalize(), login.capitalize()),
        email='{}@yandex-team.ru'.format(login),
        is_active=is_active,
        is_superuser=is_superuser,
        is_staff=is_superuser or is_staff,
        is_robot=is_robot,
        date_joined=aware_date_joined,
        country='ru',
        city='Moscow',
        unit='Infra',
        affiliation=affiliation,
    )

    if created:
        user.save()

    for group in groups:
        user.groups.add(group)

    return user


def create_certificate_type(type_name, is_active=True):
    cert_type, created = CertificateType.objects.get_or_create(name=type_name, is_active=is_active)

    if created:
        cert_type.save()

    return cert_type


def create_certificate(user, type, **params):
    days = params.get('days') or 356
    hosts = params.pop('hosts', [])

    is_revoked = params.get('status') in [CERT_STATUS.HOLD, CERT_STATUS.REVOKED]

    defaults = {
        'status': CERT_STATUS.ISSUED,
        'common_name': '{}@ld.yandex.ru'.format(user.username),
        'end_date': timezone.now() + timezone.timedelta(days),
        'begin_date': timezone.now(),
        'revoked': timezone.now() if is_revoked else None,
    }

    defaults.update(params)

    certificate = Certificate(
        user=user,
        requester=params.get(b'requester', user),
        type=type,
        status=defaults['status'],
        ca_name=defaults.get('ca_name', CA_NAME.TEST_CA),
        certificate='certificate',
        request='request',
        common_name=defaults['common_name'],
        serial_number=defaults.get('serial_number'),
        added=defaults.get('added'),
        begin_date=defaults['begin_date'],
        end_date=defaults['end_date'],
        revoked=defaults['revoked'],
        revoke_at=defaults.get('revoke_at'),
        pc_serial_number=defaults.get('pc_serial_number'),
        abc_service=params.get('abc_service'),
        uploaded_to_yav=params.get('uploaded_to_yav', False),
        private_key=params.get('private_key'),
        exclude_from_monitoring=params.get('exclude_from_monitoring', False),
        notify_on_expiration=params.get('notify_on_expiration', True),
        pc_inum=defaults.get('pc_inum'),
    )

    certificate.requested_by_csr = bool(defaults.get('request'))

    certificate.save()

    if 'added' in defaults:
        certificate.added = params['added']
        certificate.save()

    if type.name == CERT_TYPE.HOST:
        for hostname in hosts:
            host, _ = Host.objects.get_or_create(hostname=hostname)
            certificate.hosts.add(host)

    return certificate


def create_approve_request(ceritficate, approver, ticket):
    return ApproveRequest.objects.create(
        certificate=ceritficate,
        approver=approver,
        st_issue_key=ticket,
    )


def create_tag_filter(name, type=TAG_FILTER_TYPE.STAFF_API):
    tag_filter, created = TagFilter.objects.get_or_create(
        name=name,
        type=type,
        filter='{0}={0}'.format(name),
        description='',
    )

    if created:
        tag_filter.save()

    return tag_filter


def create_certificate_tag(name, filters=None, types=None, priority=0):
    if filters is None:
        filters = []

    if types is None:
        types = []

    tag, created = CertificateTag.objects.get_or_create(
        name=name,
        type=TAG_TYPE.NOC_USE,
        priority=priority,
        description='',
    )

    if created:
        tag.save()

    for tag_filter in filters:
        tag.filters.add(tag_filter)

    for cert_type in types:
        tag.filter_cert_types.add(cert_type)

    return tag


def assert_contains(text, snippets_list):
    """Проверяет, что все кусочки текста из списка упоминаются в тексте."""
    not_in = [snippet for snippet in snippets_list if snippet not in text]
    if not_in:
        raise AssertionError(
            'Эти фрагменты не найдены в тексте: ' + ', '.join(not_in)
        )


class MockStartrekResponse(object):
    sequence = 1
    instances = []

    def __init__(self, **kwargs):
        for key, value in kwargs.items():
            setattr(self, key, value)
        self.queue = getattr(self, 'queue', '<NO_QUEUE>')
        self.key = '{}-{}'.format(self.queue, MockStartrekResponse.sequence)
        MockStartrekResponse.sequence += 1
        MockStartrekResponse.instances.append(self)

    def __repr__(self):
        return '<StartrekIssue: {}>'.format(self.key)


@contextlib.contextmanager
def mock_startrek():
    with mock.patch('intranet.crt.utils.startrek.issues_repo') as mocked_repo:
        mocked_repo.create = mock.Mock()
        mocked_repo.create.side_effect = lambda **kwargs: MockStartrekResponse(**kwargs)
        mocked_repo.get.side_effect = lambda *args, **kwargs: []
        yield


class MockedIntrasearchRepository(object):
    def __init__(self, layer, objects, exception=None):
        self.layer = layer
        self.objects = objects
        if exception is None:
            self.get.side_effect = self.make_response
        else:
            self.get.side_effect = exception

    def make_response(self, lookup, **options):
        return [{
            'layer': self.layer,
            'result': self.objects,
            'pagination': {
                'page': lookup.get('{}.page'.format(self.layer), 0),
                'per_page': lookup.get('{}.per_page'.format(self.layer), 5),
                'pages': 1234
            }
        }]

    get = mock.Mock()


@contextlib.contextmanager
def mock_ids_repo(repo_key, *args, **kwargs):
    mocked_repo_classes = {
        'intrasearch': MockedIntrasearchRepository,
        # to be continued...
    }

    with mock.patch('ids.registry.registry.get_repository') as get_repository:
        mocked_repo_class = mocked_repo_classes[repo_key]
        get_repository.return_value = mocked_repo_class(*args, **kwargs)
        yield mocked_repo_class


class MockYavClient(object):
    def __init__(self):
        self.args = []
        self.kwargs = []
        self.roles_to_secrets = collections.defaultdict(set)

    def get_secret(self, secret_uuid, return_raw=False):
        if secret_uuid in ['valid_secret_uuid', 'other_valid_secret_uuid']:
            return {
                'acl': [],
                'secret_roles': [],
                'secret_versions': [{'version': '1', 'created_at': 1}, {'version': '2', 'created_at': 2}],
                'uuid': 'valid_secret_uuid',
            }

        elif secret_uuid == 'access_denied_secret_uuid':
            raise ClientError(**{
                'request_id': '*****',
                'code': 'access_error',
                'message': 'Access denied',
                'secret_state_name': 'normal',
                'status': 'error',
            })
        else:
            raise ClientError(**{
                'request_id': '*****',
                'class': 'Secret',
                'code': 'nonexistent_entity_error',
                'id': secret_uuid,
                'message': 'Requested a non-existent entity',
                'status': 'error'
            })

    def add_user_role_to_secret(self, secret_uuid, role, abc_id=None, abc_scope=None, staff_id=None,
                                uid=None, login=None, return_raw=False):
        customer = staff_id or uid or login or (abc_id, abc_scope)
        self.roles_to_secrets[secret_uuid].add((role, customer,))

    def delete_user_role_from_secret(self, secret_uuid, role, abc_id=None, abc_scope=None,
                                     staff_id=None, uid=None, login=None, return_raw=False):
        customer = staff_id or uid or login or (abc_id, abc_scope)
        self.roles_to_secrets[secret_uuid].remove((role, customer,))

    def create_secret(self, *args, **kwargs):
        self.roles_to_secrets['valid_secret_uuid'].add(('OWNER', 'robot-crt',))
        return 'valid_secret_uuid'

    def create_secret_version(self, *args, **kwargs):
        self.args.append(args)
        self.kwargs.append(kwargs)
        return '1'

    def create_diff_version(self, *args, **kwargs):
        self.args.append(args)
        self.kwargs.append(kwargs)
        return '2'


class attrdict(dict):
    def __getattr__(self, item):
        return self[item]


def approve_cert(client, should_approve=True):
    client_initial_login = client.login
    client.login('infosec-user')
    approve_url = reverse('admin:core_approverequest_changelist')
    with mock.patch('intranet.crt.api.v1.approverequests.serializers.close_st_issue_for_cert_approval'):
        client.post(approve_url, {
            'index': 0 if should_approve else 1,
            'action': 'approve' if should_approve else 'reject',
            ACTION_CHECKBOX_NAME: [ApproveRequest.objects.last().id],
        })
    client.login(client_initial_login)


def make_tagfilter_role_spec(username, tagfilter_name):
    return {
        'fields': 'null',
        'login': username,
        'path': f'/type/tagfilter/tagfilter/{tagfilter_name}/',
        'role': '{"type": "tagfilter", "tagfilter": "%s"}' % tagfilter_name,
    }


def make_tag_role_spec(username, tag_name, serial_number):
    return {
        'fields': '{"serial_number": "%s"}' % serial_number,
        'login': username,
        'path': f'/type/tag/tag/{tag_name}/',
        'role': '{"type": "tag", "tag": "%s"}' % tag_name,
    }


class RecursiveCallableAttrDict(dict):
    def __setattr__(self, key, value):
        self.setdefault(key, value)

    def __getattr__(self, item):
        return self.get(item, RecursiveCallableAttrDict())

    def __call__(self, *args, **kwargs):
        return self.get('#__call__', RecursiveCallableAttrDict())


class FakeGlobalsignClient(GlobalSignZeepClient):
    DNS_TEXT_DATA = '_globalsign-domain-verification=Zbe9nAtEzUXNCpgmo_yw8N_A3PM763kFPAAf11fVGG'
    MSSL_DOMAIN_ID = 'mssl_domain_id'
    CERT_BODY = 'certbody'

    ALREADY_ADDED_TO_PROFILE_ERROR_FLAG = False
    VERIFY_DOMAIN_ERROR_FLAG = False
    VERIFY_DOMAIN_STATUS = GlobalSignProductionCA.DOMAIN_STATUS_AVAILABLE

    @classmethod
    def create(cls, wsdl_url, username, password):
        instance = FakeGlobalsignClient(RecursiveCallableAttrDict(), username, password)
        return instance

    def call(self, method, *args, **kwargs):
        match method:
            case 'PVOrder':
                return self.order()
            case 'AddDomainToProfile':
                return self.add_to_profile()
            case 'VerifyMsslDomain':
                return self.verify_domain()
            case 'GetMSSLDomains':
                return self.get_domains()
            case 'GetOrderByOrderID':
                return self.get_order_id()
            case _:
                raise NotImplementedError

    def get_order_id(self):
        return attrdict({
            'OrderDetail': attrdict({
                'OrderInfo': attrdict(
                    {'OrderStatus': GlobalSignProductionCA.CERTIFICATE_STATUS_ISSUED}),
                'Fulfillment': attrdict({'ServerCertificate': attrdict({'X509Cert': FakeGlobalsignClient.CERT_BODY})}),
            }),
        } | self._get_header_dict())

    def get_domains(self):
        return attrdict({
            'SearchMsslDomainDetails': attrdict({
                'SearchMsslDomainDetail': [attrdict({
                    'MSSLDomainID': 'DSMS20000149831',
                    'MSSLProfileID': '132479_SMS2_4152',
                    'MSSLDomainName': 'translate-image.yandex',
                    'MSSLDomainStatus': FakeGlobalsignClient.VERIFY_DOMAIN_STATUS,
                    'NotBefore': None,
                    'NotAfter': None,
                    'Delete_Date': None
                })],
            }),
        } | self._get_header_dict())

    def verify_domain(self):
        if self.VERIFY_DOMAIN_ERROR_FLAG:
            return attrdict({'OrderResponseHeader': attrdict({
                'SuccessCode': -1,
                'Errors': attrdict({
                    'Error': [attrdict({
                        'ErrorCode': str(GlobalSignProductionCA.ERROR_INTERNAL_SYSTEM_ERROR),
                        'ErrorMessage': (
                            'Internal system error. Please reexecute what you were doing. '
                            'If error persists, please contact GlobalSign Support'
                        ),
                    })],
                }),
            })} | self._get_header_dict())
        return attrdict({
            'DomainID': self.MSSL_DOMAIN_ID,
        } | self._get_header_dict())

    def add_to_profile(self, *args, **kwargs):
        if self.ALREADY_ADDED_TO_PROFILE_ERROR_FLAG:
            return attrdict({'QueryResponseHeader': attrdict({
                'SuccessCode': -1,
                'Errors': attrdict({
                    'Error': [attrdict({
                        'ErrorCode': str(
                            GlobalSignProductionCA.ERROR_DOMAIN_NAME_ALREADY_EXISTS_FOR_THE_MSSL_PROFILE_ID),
                        'ErrorMessage': 'The DomainName already exists for the MSSLProfileID',
                    }),
                    ]}),
            })})
        return attrdict({
            'DnsTXT': self.DNS_TEXT_DATA,
            'MSSLDomainID': self.MSSL_DOMAIN_ID,
        } | self._get_header_dict())

    def order(self, *args, **kwargs):
        return attrdict({
            'OrderID': 'id',
            'PVOrderDetail': attrdict({
                'Fulfillment': attrdict({'ServerCertificate': attrdict({'X509Cert': 'certbody'})}),
            }),
        } | self._get_header_dict())

    @staticmethod
    def _get_header_dict() -> dict:
        return {'QueryResponseHeader': attrdict({
            'SuccessCode': 0,
            'Errors': []
        })}


@contextlib.contextmanager
def patch_globalsign_client():
    with mock.patch('intranet.crt.core.ca.globalsign.GlobalSignZeepClient', FakeGlobalsignClient) as mocked:
        yield mocked


@contextlib.contextmanager
def patch_get_host_txt_codes(validation_code):
    get_host_ips_function = 'intranet.crt.utils.dns.get_host_ips'
    with mock.patch(get_host_ips_function, return_value=["ip1", "ip2"]):
        with mock.patch('dns.resolver.Resolver') as mock_resolver:
            mock_resolver().query.return_value = [attrdict(strings=[validation_code.code])]
            yield



