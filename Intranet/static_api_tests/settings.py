# coding: utf-8
from __future__ import unicode_literals

SECRET_KEY = 'Wow such secret very key'

# need for special params parser test
PAGE_SIZE = 300

# need in modules, which imports utils
PROCESS_LOCK_TIMEOUT = 300

MONGO_URL = 'mongodb://localhost/static-api-test?replicaSet=toolsrepl'
MONGO_DB = 'static-api-test'

from static_api.settings import *

STATIC_API_ENTITY_COLLECTIONS = ('person',)

SERVICE_NAME = 'Someservice API'
API_VERSION = 'v3'

STATIC_API_WILDCARD_FIELD_ACCESS = '*'

IDM_ROLE_REQUEST_URL_TEMPLATE = (
    'https://idm.yandex-team.ru/#rf-role=SkuUnGO8#staffapi-test/resource_access/'
    '%(resource)s(fields:()),rf-expanded=SkuUnGO8,rf=1'
)

IDM_ROLE_REQUEST_MANUAL_URL = 'https://wiki.yandex-team.ru/staff/staffroles/#rolnadostupkdannymvstaff-api'
