import pytest
from django.conf import settings


def only_biz(fn):
    return pytest.mark.skipif(not settings.IS_BUSINESS, reason='Only for business')(fn)


def only_intranet(fn):
    return pytest.mark.skipif(not settings.IS_INTRANET, reason='Only for intranet')(fn)
