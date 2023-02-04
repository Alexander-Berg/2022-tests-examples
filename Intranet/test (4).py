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
    default="uCfewoqxH35ZjNWQVs8gMbOfLdRB4OLUGwxgg5lXdEMupUMUNyCaT5gg35e1oTAG",
)
# https://docs.djangoproject.com/en/dev/ref/settings/#test-runner
TEST_RUNNER = "django.test.runner.DiscoverRunner"

# PASSWORDS
# ------------------------------------------------------------------------------
# https://docs.djangoproject.com/en/dev/ref/settings/#password-hashers
PASSWORD_HASHERS = ["django.contrib.auth.hashers.MD5PasswordHasher"]

# TEMPLATES
# ------------------------------------------------------------------------------
TEMPLATES[-1]["OPTIONS"]["loaders"] = [  # type: ignore[index] # noqa F405
    (
        "django.template.loaders.cached.Loader",
        [
            "django.template.loaders.filesystem.Loader",
            "django.template.loaders.app_directories.Loader",
        ],
    )
]

# EMAIL
# ------------------------------------------------------------------------------
# https://docs.djangoproject.com/en/dev/ref/settings/#email-backend
EMAIL_BACKEND = "django.core.mail.backends.locmem.EmailBackend"

# Celery
# ------------------------------------------------------------------------------
CELERY_TASK_ALWAYS_EAGER = True

CELERY_TASK_EAGER_PROPAGATES = True

# MIDDLEWARE
# ------------------------------------------------------------------------------
MIDDLEWARE += [
    "django.contrib.auth.middleware.AuthenticationMiddleware",
]

# ElasticSearch
# ------------------------------------------------------------------------------
ELASTICSEARCH_DSL = {
    "default": {"hosts": "elastic:9200"},
}

ELASTICSEARCH_DSL_AUTOSYNC = False

# Staff
# ------------------------------------------------------------------------------
STAFF_API_SKIP_TEST = True

# Sender
# ------------------------------------------------------------------------------
SENDER_SKIP_TEST = True
