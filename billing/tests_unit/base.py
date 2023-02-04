# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()


class TestCaseBrestBase(object):

    def get_passport_id(self):
        return 1234567890
