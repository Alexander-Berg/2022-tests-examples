# coding: utf-8

from settings import *

DUMMY_TEST_USER = 'sorgoz'   # заглушка вместо авторизации (если пустая строка, то заглушка выключается)
DUMMY_PASSPORT_USER = DUMMY_TEST_USER

TEST_RUNNER = 'django_teamcity.run_tests'

TEMPLATE_CONTEXT_PROCESSORS = (
    'django.core.context_processors.auth',
    'django.core.context_processors.debug',
    'django.core.context_processors.media',
    'django.core.context_processors.request',
)

INSTALLED_APPS = (
    'django.contrib.sessions',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.admin',
    'south',
    'mptt',
    'tagging',
    'django_russian',
    'libra.books',
    'libra.users',
    'libra.api',
)

# Исключен Xscript из middlewares
MIDDLEWARE_CLASSES = (
    'django.middleware.common.CommonMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    'django_intranet_stuff.middleware.PassportAuthMiddleware',
    'libra.books.middleware.DummyUserAuthMiddleware',
)

CENTER_RPC_USER = '_rpc_libra'
CENTER_RPC_PASSWORD = 'eKBQ2RKtCXsuZDGfecWTew8T'
CENTER_MASTER = 'center.test.tools.yandex-team.ru'
