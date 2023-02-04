import asyncio
import json

import pytest
from billing.hot.faas.lib.python.example.foo.foo import async_foo, foo

from billing.hot.faas.python.faas.core.exceptions import (
    FunctionNotFoundError,
    FunctionNotSetError,
    FunctionRaceError,
)
from billing.hot.faas.python.faas.core.function import (
    FunctionItem,
    FunctionRegistry,
    get_function,
)


class TestFunction:
    @pytest.fixture
    def iscoroutinefunction(self):
        return False

    @pytest.fixture
    def returned_func(self, faas_settings):
        def _inner():
            return get_function(faas_settings.FUNCTION)

        return _inner

    def test_module_not_found(self, faas_settings, returned_func):
        faas_settings.FUNCTION = "foo"
        with pytest.raises(FunctionNotFoundError) as exc:
            returned_func()
        assert exc.value.params == {"module": "foo"}

    def test_module_set(self, faas_settings, returned_func):
        faas_settings.FUNCTION = "billing.hot.faas.lib"
        with pytest.raises(FunctionNotSetError) as exc:
            returned_func()
        assert exc.value.params == {"module": "billing.hot.faas.lib"}

    def test_function_not_found(self, faas_settings, rands, returned_func):
        function_name = rands()
        faas_settings.FUNCTION = (
            f"billing.hot.faas.lib.python.example.foo.{function_name}"
        )
        with pytest.raises(FunctionNotFoundError) as exc:
            returned_func()
        assert exc.value.params == {
            "module": "billing.hot.faas.lib.python.example.foo",
            "function": function_name,
        }

    def test_class_run_not_found(self, faas_settings, returned_func):
        faas_settings.FUNCTION = (
            "billing.hot.faas.lib.python.example.bar.bar.InvalidCalculatorClassNoRun"
        )
        with pytest.raises(FunctionNotFoundError) as exc:
            returned_func()
        assert exc.value.params == {
            "module": "billing.hot.faas.lib.python.example.bar.bar",
            "class": "InvalidCalculatorClassNoRun",
            "method": "run",
        }

    def test_class_run_not_func(self, faas_settings, returned_func):
        faas_settings.FUNCTION = "billing.hot.faas.lib.python.example.bar.bar.InvalidCalculatorClassRunNotFunc"
        with pytest.raises(FunctionNotFoundError) as exc:
            returned_func()
        assert exc.value.params == {
            "module": "billing.hot.faas.lib.python.example.bar.bar",
            "class": "InvalidCalculatorClassRunNotFunc",
            "method": "run",
        }

    @pytest.mark.parametrize(
        "name,iscoroutinefunction",
        [
            ("foo.foo.foo", False),
            ("foo.foo.async_foo", True),
            ("bar.bar.Bar", False),
            ("bar.bar.AsyncBar", True),
        ],
    )
    def test_function(self, name, iscoroutinefunction, faas_settings, returned_func):
        faas_settings.FUNCTION = f"billing.hot.faas.lib.python.example.{name}"
        assert iscoroutinefunction == asyncio.iscoroutinefunction(returned_func())


class TestFunctionRegistry:
    @pytest.fixture
    def create_registry(self, function):
        def _inner():
            return FunctionRegistry(function)

        return _inner

    @pytest.fixture
    def function_registry(self, create_registry):
        return create_registry()

    @pytest.mark.parametrize("function", [None])
    def test_invalid_function(self, create_registry):
        with pytest.raises(FunctionNotSetError):
            create_registry()

    class TestSingleFunction:
        @pytest.fixture(params=["billing.hot.faas.lib.python.example.foo.foo.foo", foo])
        def function(self, request):
            return request.param

        def test_single_function_registry(self, function_registry):
            assert function_registry.default_function == FunctionItem(func=foo)
            assert list(function_registry.named_functions) == []

        def test_single_function_get(self, function_registry):
            assert function_registry.get(None) == FunctionItem(func=foo)
            assert function_registry[None] == FunctionItem(func=foo)

        def test_single_function_not_found(self, rands, function_registry):
            rnd_name = rands()
            assert function_registry.get(rnd_name) is None
            with pytest.raises(FunctionNotFoundError):
                _ = function_registry[rnd_name]

    class TestMultipleFunctions:
        @pytest.fixture
        def settings(self, rands):
            return {rands(): rands()}

        @pytest.fixture
        def function(self, settings):
            return json.dumps(
                [
                    {
                        "name": "foo",
                        "function": "billing.hot.faas.lib.python.example.foo.foo.foo",
                        "settings": settings,
                    },
                    {
                        "name": "async_foo",
                        "function": "billing.hot.faas.lib.python.example.foo.foo.async_foo",
                        "settings": settings,
                    },
                ]
            )

        def test_multiple_functions_registry(self, settings, function_registry):
            assert function_registry.default_function is None
            assert dict(function_registry.named_functions) == {
                "foo": FunctionItem(func=foo, settings=settings),
                "async_foo": FunctionItem(func=async_foo, settings=settings),
            }

        @pytest.mark.parametrize("name,func", (("async_foo", async_foo), ("foo", foo)))
        def test_multiple_functions_get(self, function_registry, name, func, settings):
            assert function_registry[name] == FunctionItem(func=func, settings=settings)
            assert function_registry.get(name) == FunctionItem(
                func=func, settings=settings
            )

        def test_multiple_functions_name_not_found(self, rands, function_registry):
            rnd_name = rands()
            assert function_registry.get(rnd_name) is None
            with pytest.raises(FunctionNotFoundError):
                _ = function_registry[rnd_name]

        def test_multiple_functions_default_not_found(self, function_registry):
            assert function_registry.get(None) is None
            with pytest.raises(FunctionNotFoundError):
                _ = function_registry[None]

    class TestMultipleFunctionsWithDefault:
        @pytest.fixture
        def function(self):
            return json.dumps(
                [
                    {
                        "name": "foo",
                        "function": "billing.hot.faas.lib.python.example.foo.foo.foo",
                        "default": True,
                    },
                    {
                        "name": "async_foo",
                        "function": "billing.hot.faas.lib.python.example.foo.foo.async_foo",
                    },
                ]
            )

        def test_multiple_functions_registry(self, function_registry):
            assert function_registry.default_function == FunctionItem(func=foo)
            assert dict(function_registry.named_functions) == {
                "foo": FunctionItem(func=foo),
                "async_foo": FunctionItem(func=async_foo),
            }

        def test_multiple_functions_get_default(self, function_registry):
            assert function_registry.get(None) == FunctionItem(func=foo)
            assert function_registry[None] == FunctionItem(func=foo)

    class TestMultipleDefaultFunctions:
        @pytest.fixture
        def function(self):
            return json.dumps(
                [
                    {
                        "name": "foo",
                        "function": "billing.hot.faas.lib.python.example.foo.foo.foo",
                        "default": True,
                    },
                    {
                        "name": "async_foo",
                        "function": "billing.hot.faas.lib.python.example.foo.foo.async_foo",
                        "default": True,
                    },
                ]
            )

        def test_multiple_default_functions_error(self, create_registry):
            with pytest.raises(FunctionRaceError):
                create_registry()
