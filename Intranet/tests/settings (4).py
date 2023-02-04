from __future__ import unicode_literals
SECRET_KEY = 'some-secret-key'

INSTALLED_APPS = (
    'django.contrib.sites',
    'django.contrib.contenttypes',
    'django.contrib.auth',

    'tests.app',
)

SITE_ID = 1

DATABASES = {'default': {'ENGINE': 'django.db.backends.sqlite3'}}
