# -*- coding: utf-8 -*-


import pytest
from django.core.exceptions import ValidationError
from idm.core.validators import sudoers_entry

from idm.tests.utils import assert_contains


def test_sudo_entry():
    with pytest.raises(ValidationError) as excinfo:
        sudoers_entry('XXX')

    assert_contains((
        'Ошибка в правиле sudoers',
        'syntax error near line 1'
    ), excinfo.value.message)

    with pytest.raises(ValidationError) as excinfo:
        sudoers_entry('Привет')

    assert_contains((
        'Ошибка в правиле sudoers',
        'syntax error near line 1',
    ), excinfo.value.message)

    with pytest.raises(ValidationError) as excinfo:
        sudoers_entry('ALL=(ALL:ALL) XXX')

    assert_contains((
        'Предупреждение в правиле sudoers',
        'Warning: Cmnd_Alias "XXX" referenced but not defined'
    ), excinfo.value.message)

    # Valid string
    sudoers_entry('ALL=(ALL:ALL) ALL')
