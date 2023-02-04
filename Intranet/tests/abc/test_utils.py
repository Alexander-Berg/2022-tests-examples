# -*- coding: utf-8 -*-
from django.test import TestCase
from unittest.mock import patch, call

from events.abc.client import AbcClient, ROLE_TVM_MANAGER, ROLE_FORM_MANAGER
from events.abc.utils import has_role_tvm_or_form_manager


class TestHasRoleTvmManager(TestCase):
    def test_should_return_true(self):
        with (
            patch.object(AbcClient, 'get_services_by_tvm_client') as mock_get_services,
            patch.object(AbcClient, 'has_roles_in_service') as mock_has_role,
        ):
            mock_get_services.return_value = [100, 200]
            mock_has_role.side_effect = [False, True]
            has_role = has_role_tvm_or_form_manager('12345', '120001')

        self.assertTrue(has_role)
        mock_get_services.assert_called_once_with('12345')
        mock_has_role.assert_has_calls([
            call(100, [ROLE_TVM_MANAGER, ROLE_FORM_MANAGER], uid='120001'),
            call(200, [ROLE_TVM_MANAGER, ROLE_FORM_MANAGER], uid='120001'),
        ])

    def test_should_return_true_after_first_try(self):
        with (
            patch.object(AbcClient, 'get_services_by_tvm_client') as mock_get_services,
            patch.object(AbcClient, 'has_roles_in_service') as mock_has_role,
        ):
            mock_get_services.return_value = [100, 200]
            mock_has_role.return_value = True
            has_role = has_role_tvm_or_form_manager('12345', '120001')

        self.assertTrue(has_role)
        mock_get_services.assert_called_once_with('12345')
        mock_has_role.assert_called_once_with(100, [ROLE_TVM_MANAGER, ROLE_FORM_MANAGER], uid='120001')

    def test_should_return_false(self):
        with (
            patch.object(AbcClient, 'get_services_by_tvm_client') as mock_get_services,
            patch.object(AbcClient, 'has_roles_in_service') as mock_has_role,
        ):
            mock_get_services.return_value = [100, 200]
            mock_has_role.side_effect = [False, False]
            has_role = has_role_tvm_or_form_manager('12345', '120001')

        self.assertFalse(has_role)
        mock_get_services.assert_called_once_with('12345')
        mock_has_role.assert_has_calls([
            call(100, [ROLE_TVM_MANAGER, ROLE_FORM_MANAGER], uid='120001'),
            call(200, [ROLE_TVM_MANAGER, ROLE_FORM_MANAGER], uid='120001'),
        ])

    def test_should_return_false_when_empty_service_list(self):
        with (
            patch.object(AbcClient, 'get_services_by_tvm_client') as mock_get_services,
            patch.object(AbcClient, 'has_roles_in_service') as mock_has_role,
        ):
            mock_get_services.return_value = []
            has_role = has_role_tvm_or_form_manager('12345', '120001')

        self.assertFalse(has_role)
        mock_get_services.assert_called_once_with('12345')
        mock_has_role.assert_not_called()
