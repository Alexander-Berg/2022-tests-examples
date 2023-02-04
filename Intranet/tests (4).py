#!/usr/bin/env python
#coding: utf-8
import os
import sys

import django
from django.conf import settings
from django.core.management import call_command


settings.configure(
    INSTALLED_APPS=(
        'django.contrib.sites',
        'django.contrib.contenttypes',
        'django.contrib.auth',

        'django_nose',

        'emission.django.emission_master',
    ),
    SITE_ID=1,
    DATABASES={'default': {
        'ENGINE': 'django.db.backends.sqlite3',
        'NAME': os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'db.sqlite3'),
    }},

    ROOT_URLCONF='emission.django.emission_master.urls',

    TEST_RUNNER='django_nose.NoseTestSuiteRunner',

    NOSE_ARGS=[
        '--nocapture',
        '--nologcapture',
        '--with-coverage',
        '--cover-package=emission'
    ],

    EMISSION_MASTER_REPLICATED_MODELS=[],
    EMISSION_TRANSACTION_WAIT_DELTA=30,
)


if __name__ == "__main__":
    django.setup()
    call_command('test', sys.argv[1] if len(sys.argv) > 1 else './tests', verbosity=2)
