# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import pytest

from brest.core.tests.utils import (
    generate_random_numeric_str,
    generate_random_alpha_numeric_str,
)


@pytest.fixture(name='yandex_uid')
def generate_yandex_uid():
    """
    :rtype: str
    """
    return generate_random_numeric_str(10)


@pytest.fixture(name='oper_id')
def generate_oper_id():
    """
    :rtype: str
    """
    return generate_random_numeric_str(10)


@pytest.fixture(name='session_id')
def generate_session_id():
    """
    :rtype: str
    """
    return generate_random_alpha_numeric_str(50)


@pytest.fixture(name='session_id2')
def generate_session_id2():
    """
    :rtype: str
    """
    return generate_random_alpha_numeric_str(50)
