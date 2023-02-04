"""Tests test utilities."""

import pytest

from infra.walle.server.tests.lib.util import mock_object


def test_function_mocking():
    def func(x, y=0):
        pass

    func_mock = mock_object(func, return_value=42)

    assert func_mock(0) == 42
    assert func_mock(0, 1) == 42
    assert func_mock(x=0, y=1) == 42

    with pytest.raises(TypeError):
        func_mock()

    with pytest.raises(TypeError):
        func_mock(x=0, z=1)


def test_class_mocking():
    class C:
        def method(self, x, y=0):
            pass

    obj_mock = mock_object(C)
    obj_mock.method.return_value = 42

    with pytest.raises(AttributeError):
        obj_mock.invalid

    assert obj_mock.method(0) == 42
    assert obj_mock.method(0, 1) == 42
    assert obj_mock.method(x=0, y=1) == 42

    with pytest.raises(TypeError):
        obj_mock.method()

    with pytest.raises(TypeError):
        obj_mock.method(x=0, z=1)
