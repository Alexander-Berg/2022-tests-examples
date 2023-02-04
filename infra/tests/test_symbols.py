import pytest

from infra.reconf.util.symbols import SymLoader


def test_symbols_loader():
    assert SymLoader is SymLoader().load('infra.reconf.util.symbols.SymLoader')
    assert SymLoader is not SymLoader().load('pytest.Collector')


def test_symbols_loader_module_doesnt_exist():
    with pytest.raises(ModuleNotFoundError):
        SymLoader().load('module.does.not.Exist')
