import pytest

from walle.scenario.errors import KeyDoesNotExistsRegistryError, KeyAlreadyExistsRegistryError
from walle.scenario.utils import BaseRegistry


class TestBaseRegistry:
    @staticmethod
    def _initialize():
        class TestRegistry(BaseRegistry):
            ITEMS = {}

        return TestRegistry

    def test_get_keys(self):
        registry = self._initialize()
        keys = [x for x in range(10)]
        registry.ITEMS = {x: None for x in keys}

        result = registry.get_keys()

        assert keys == sorted(result)

    def test_get_successfully(self):
        registry = self._initialize()
        items = {x: x for x in range(5)}
        registry.ITEMS = items

        for key, value in items.items():
            assert value == registry.get(key)

    def test_get_with_exc(self):
        registry = self._initialize()

        with pytest.raises(KeyDoesNotExistsRegistryError):
            registry.get("any-key")

    def test_register(self):
        registry = self._initialize()
        key, func = "test-key", "test-func"

        registry.register(key)(func)

        assert registry.ITEMS == {key: func}

    def test_register_with_exc(self):
        registry = self._initialize()
        key, func = "test-key", "test-func"

        registry.ITEMS[key] = None

        with pytest.raises(KeyAlreadyExistsRegistryError):
            registry.register(key)(func)
