# -*- coding: utf-8 -*-
from __future__ import unicode_literals


pytest_plugins = [
    # explicit str constructor fixes assertion error
    str('core.tests.fixtures'),
]
