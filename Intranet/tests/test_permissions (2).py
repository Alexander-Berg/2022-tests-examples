from builtins import object

import pytest
from mock import MagicMock

from kelvin.common import permissions as perms


class TestIsDetailRequest(object):
    """
    Тесты для класса `IsDetailRequest`
    """
    permission_cases = (
        ('list', False),
        ('retrieve', True),
    )

    @pytest.mark.parametrize('action,expected', permission_cases)
    def test_has_permission(self, action, expected):
        """
        Проверяет, что есть доступ только к `detail`-ручке
        """
        view = MagicMock()
        view.action = action
        assert perms.IsDetailRequest().has_permission(
            MagicMock, view) == expected


class TestIsListRequest(object):
    """
    Тесты для класса `IsListRequest`
    """
    permission_cases = (
        ('list', True),
        ('retrieve', False),
    )

    @pytest.mark.parametrize('action,expected', permission_cases)
    def test_has_permission(self, action, expected):
        """
        Проверяет, что есть доступ только к `list`-ручке
        """
        view = MagicMock()
        view.action = action
        assert perms.IsListRequest().has_permission(
            MagicMock, view) == expected


class TestObjectDetailRequest(object):
    """
    Тесты для класса `ObjectDetailRequest`
    """
    permission_cases = (
        ('list', False),
        ('retrieve', True),
    )

    @pytest.mark.parametrize('action,expected', permission_cases)
    def test_has_permission(self, action, expected):
        """
        Проверяет, что есть доступ только к `detail`-ручке
        """
        view = MagicMock()
        view.action = action
        assert perms.ObjectDetailRequest().has_object_permission(
            MagicMock, view, 'obj') == expected
