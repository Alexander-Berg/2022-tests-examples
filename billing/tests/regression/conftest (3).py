import os

import pytest
import requests

TEST_HOST = os.environ.get('TEST_HOST', '')

try:
    import library.python
    import pkgutil

    ARCADIA_RUN = True

except ImportError:
    ARCADIA_RUN = False


@pytest.fixture
def refs():

    test_host = 'https://refs-test.paysys.yandex.net'

    if TEST_HOST and TEST_HOST != 't':
        # t для обратной совместимости со временами,
        # когда по умолчанию тесты ходили на localhost.
        test_host = 'http://localhost:8080' if TEST_HOST == 'l' else TEST_HOST

    return test_host


@pytest.fixture
def refs_get(refs):

    def refs_get_(url, *, fields=None):
        query_url = f'{refs}{url}'

        if fields:
            query_url = query_url % {'fields': ' '.join(fields)}

        result = requests.get(url=query_url, verify=False).json()
        return result

    return refs_get_