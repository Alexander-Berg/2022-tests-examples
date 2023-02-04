# coding: utf-8
from __future__ import unicode_literals


def use_account_type(account_type, initial_data_attr='form_data'):
    """
    Используется в качестве замены pytest.mark.paramertize для разных типов аккаунтов,
    т.к. parametrize не работает для django.test.TestCase
    """
    account_data_by_type = {
        'account': {'account_type': 'account', 'account': 'AA00QWERTY'},
        'ben_account': {'account_type': 'ben_account', 'ben_account': '1'},
        'none': {},
    }
    account_data = account_data_by_type.get(account_type)

    def decorator(func):
        def inner(self):
            initial_data = getattr(self, initial_data_attr)
            form_data = dict(initial_data, **account_data)
            return func(self, form_data)
        return inner

    if callable(account_type):
        return decorator(func=account_type)

    return decorator
