# -*- coding: utf-8 -*-

import pytest
import yatest

import irt.monitoring.atoms.config
import irt.monitoring.atoms.config.funcs


@pytest.fixture
def mock_perl_common_options(monkeypatch):
    monkeypatch.setattr(irt.monitoring.atoms.config.funcs, "get_arcadia_root", yatest.common.build_path)


@pytest.mark.usefixtures("mock_perl_common_options")
def test_atoms_configuration():
    for atom in irt.monitoring.atoms.config.get_atoms_configuration():
        assert ("atom" in atom) and ("atom_args" in atom)
        assert ("solomon_sensor_params" in atom) or ("special_handler" in atom)
