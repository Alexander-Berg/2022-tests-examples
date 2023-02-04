# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import pytest
from hamcrest import assert_that, equal_to

from brest.tests_unit.base import TestCaseBrestBase
from brest.utils import text as text_util


@pytest.mark.smoke
class TestCaseText(TestCaseBrestBase):
    def test_mask(self):
        assert_that(
            text_util.mask('3:serv:12345678901234567890ABCDEFGHIJKLMN'),
            equal_to('3:serv:12345678XXXX0ABCDEFGHIJKLMN'),
        )
        assert_that(
            text_util.mask('3:user:12345678901234567890ABCDEFGHIJKLMN'),
            equal_to('3:user:12345678XXXX0ABCDEFGHIJKLMN'),
        )
