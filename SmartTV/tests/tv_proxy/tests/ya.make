PY2TEST()

OWNER(g:smarttv)

ENV(DJANGO_SETTINGS_MODULE=smarttv.alice.tv_proxy.tests.settings)

PEERDIR(
    library/python/django
    contrib/python/django/django-1.11
    contrib/python/djangorestframework
    contrib/python/pytest
    contrib/python/pytest-django
    contrib/python/pytest-mock
    contrib/python/mock
    smarttv/alice/tv_proxy
    smarttv/alice/tv_proxy/proxy
    saas/library/persqueue/writer/python
)

PY_SRCS(
    settings.py
    mocks.py
)

TEST_SRCS(
    test_indexing.py
    test_deletion.py
)

SIZE(SMALL)

NO_CHECK_IMPORTS()

END()
