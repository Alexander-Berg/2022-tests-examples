from intranet.vconf.src.settings import *  # noqa


AUTH_TEST_USER = 'tester'
SECRET_KEY = '123'

MIDDLEWARE = [
    'django.middleware.security.SecurityMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.common.CommonMiddleware',
    'intranet.vconf.src.lib.middleware.AuthTestMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
]
