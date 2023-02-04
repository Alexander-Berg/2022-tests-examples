# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest

from core import triggers


def test_get_trigger_by_name():
    trigger = triggers.registry.get_trigger_by_name('yandex_sponsored')
    assert trigger.__name__ == 'yandex_sponsored'


def test_trigger_not_found():
    with pytest.raises(triggers.TriggerNotFound):
        triggers.registry.get_trigger_by_name('None')
