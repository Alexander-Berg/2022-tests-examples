"""
With these settings, tests run faster.
"""

from .base import *  # noqa
from .base import env

# GENERAL
# ------------------------------------------------------------------------------
# https://docs.djangoproject.com/en/dev/ref/settings/#secret-key
SECRET_KEY = env(
    "DJANGO_SECRET_KEY",
    default="74PgxOcIvA9Hsg3KUGtiIv7ydjJP7bFwfED2xb4RZ3p86bJvCiwlug33aOFvV74Y",
)
# https://docs.djangoproject.com/en/dev/ref/settings/#test-runner
TEST_RUNNER = "django.test.runner.DiscoverRunner"

# MIDDLEWARE
# ------------------------------------------------------------------------------
MIDDLEWARE += [
    'django.contrib.auth.middleware.AuthenticationMiddleware',
]

# CACHES
# ------------------------------------------------------------------------------
# https://docs.djangoproject.com/en/dev/ref/settings/#caches
# CACHES = {
#     "default": {
#         "BACKEND": "django.core.cache.backends.locmem.LocMemCache",
#         "LOCATION": "",
#     }
# }

# PASSWORDS
# ------------------------------------------------------------------------------
# https://docs.djangoproject.com/en/dev/ref/settings/#password-hashers
PASSWORD_HASHERS = ["django.contrib.auth.hashers.MD5PasswordHasher"]


# EMAIL
# ------------------------------------------------------------------------------
# https://docs.djangoproject.com/en/dev/ref/settings/#email-backend
EMAIL_BACKEND = "django.core.mail.backends.locmem.EmailBackend"


# Celery
# ------------------------------------------------------------------------------
CELERY_TASK_ALWAYS_EAGER = True

CELERY_TASK_EAGER_PROPAGATES = True


# Sender
# ------------------------------------------------------------------------------
SENDER_ENABLED = False


# Your stuff...
# ------------------------------------------------------------------------------
API_ENABLED = True
EXTERNAL_API_ENABLED = True
ADMIN_ENABLED = True
SWAGGER_ENABLED = True

STAFF_API_SKIP_TEST = True

CALENDAR_API_SKIP_TEST = True

# TVM
# ------------------------------------------------------------------------------
TVM_DEBUG = True
MIDDLEWARE += ['lms.contrib.tvm.middleware.TVMDebugMiddleware']
DEBUG_TVM_SERVICE_ID = 100500
BLACKBOX_NAME = 'Test'
