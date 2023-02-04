# -*- coding: utf-8 -*-
from django.test import TestCase
from events.common_app.utils import ParamsSetter


class Man(object):
    def __init__(self):
        self.name = None
        self.surname = None


class TestParamsSetter(TestCase):
    def setUp(self):
        self.target = Man()
        self.empty_values = ParamsSetter(target=object, data={}).get_empty_values()

    def test_should_set_only_existing_attrs(self):
        data = {
            'name': 'gena',
            'another_param': 'hello'
        }
        params_setter = ParamsSetter(target=self.target, data=data)
        params_setter.set_params()

        self.assertEqual(self.target.name, 'gena')
        msg = 'метод set_params должен устанавливать только те значения, которые существуют в target'
        self.assertFalse(hasattr(self.target, 'another_param'), msg=msg)

    def test_empty_values(self):
        params_setter = ParamsSetter(target=self.target, data={})
        self.assertEqual(params_setter.get_empty_values(), ['', None])

    def test_by_default_should_set_only_empty_params(self):
        data = {
            'name':    'jorge',
            'surname': 'bush'
        }

        for empty_item in self.empty_values:
            self.target.name = empty_item
            self.target.surname = 'chibisov'

            params_setter = ParamsSetter(target=self.target, data=data)
            params_setter.set_params()
            msg = 'по умолчанию метод set_params должен обновлять только пустые поля в target'
            self.assertEqual(self.target.name, 'jorge', msg=msg + '. name был пуст')
            self.assertEqual(self.target.surname, 'chibisov', msg=msg + '. surname был заполнен')

    def test_should_set_all_params_if_not_is_only_for_empty_params(self):
        data = {
            'name':    'jorge',
            'surname': 'bush'
        }

        for empty_item in self.empty_values:
            self.target.name = empty_item
            self.target.surname = 'chibisov'

            params_setter = ParamsSetter(target=self.target, data=data)
            params_setter.set_params(is_only_for_empty_params=False)
            msg = 'метод set_params должен обновлять все поля target, если is_only_for_empty_params=False'
            self.assertEqual(self.target.name, 'jorge', msg=msg + '. name был пуст')
            self.assertEqual(self.target.surname, 'bush', msg=msg + '. surname был заполнен')

    def test_by_default_should_not_erase_filled_param_in_profile_with_empty_param_from_passport(self):
        for empty_item in self.empty_values:
            data = {
                'name':    'gena',
                'surname': empty_item
            }

            self.target.name = 'gena'
            self.target.surname = 'chibisov'

            params_setter = ParamsSetter(target=self.target, data=data)
            params_setter.set_params(is_only_for_empty_params=False)
            msg = ('метод set_params не должен по умолчанию заменять существующие данные target '
                   'пустыми данными из data')
            self.assertEqual(self.target.name, 'gena', msg=msg + '. name был заполнен')
            self.assertEqual(self.target.surname, 'chibisov', msg=msg + '. surname был заполнен')

    def test_should_erase_filled_param_in_profile_with_empty_param_from_passport_if_is_use_empty_values(self):
        for empty_item in self.empty_values:
            data = {
                'name':    'gena',
                'surname': empty_item
            }

            self.target.name = 'gena'
            self.target.surname = 'chibisov'

            params_setter = ParamsSetter(target=self.target, data=data)
            params_setter.set_params(is_only_for_empty_params=False, is_use_empty_values=True)
            msg = ('метод set_params должен заменять существующие данные target '
                   'пустыми данными из data, если is_use_empty_values=True и is_only_for_empty_params=False')
            self.assertEqual(self.target.name, 'gena', msg=msg + '. name был заполнен')
            self.assertEqual(self.target.surname, empty_item, msg=msg + '. surname был заполнен')
