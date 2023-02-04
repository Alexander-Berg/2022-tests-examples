# coding: utf-8
import pytest

from cached_property import cached_property
from yatest.common import source_path

from infra.rtc.walle_validator.lib.filters import Any
from infra.rtc.walle_validator.lib.store import ConfigStore


class WalleResolver(object):

    @cached_property
    def config_store(self):
        return ConfigStore(
            source_path("infra/rtc/walle_validator/projects/configs"),
            source_path("infra/rtc/walle_validator/projects/auxiliaries"),
            source_path("infra/orly/rules"),
            source_path("infra/maxwell/specs")
        )

    @cached_property
    def projects(self):
        return list(self.config_store.iter_projects())

    @cached_property
    def setup_configs(self):
        return list(self.config_store.iter_setup_configs())

    @cached_property
    def setup_config_map(self):
        return {setup_config.name: setup_config for setup_config in self.setup_configs}

    @cached_property
    def host_counts(self):
        return self.config_store.get_host_counts()

    @cached_property
    def orly_rules(self):
        return set(self.config_store.iter_orly_rules())

    @cached_property
    def maxwell_specs(self):
        return {maxwell_spec.name: maxwell_spec for maxwell_spec in self.config_store.iter_maxwell_specs()}

    @cached_property
    def automation_plots(self):
        return {automation_plot.name: automation_plot for automation_plot in self.config_store.iter_automation_plots()}


_resolver = WalleResolver()


def pytest_generate_tests(metafunc):
    if "project" in metafunc.fixturenames:
        condition = Any()
        project_filter_mark = metafunc.definition.get_closest_marker("project_filter")
        if project_filter_mark is not None:
            args = project_filter_mark.args
            assert len(args) > 0
            condition = args[0]
            if len(args) > 1:
                coverage_unit = args[1]
                coverage_unit.add(condition)
        projects = [project for project in _resolver.projects if condition.match(project)]
        assert projects, "{}.{} can't find projects to check".format(metafunc.module.__name__, metafunc.function.__name__)
        metafunc.parametrize("project", projects, ids=[project.id for project in projects])
    if "setup_config" in metafunc.fixturenames:
        setup_configs = _resolver.setup_configs
        assert setup_configs, "{}.{} can't find setup_configs to check".format(metafunc.module.__name__, metafunc.function.__name__)
        metafunc.parametrize("setup_config", setup_configs, ids=[setup_config.name for setup_config in setup_configs])


@pytest.hookimpl(tryfirst=True)
def pytest_runtest_makereport(item, call):
    # hack to enable non-strict mode for xfail
    if hasattr(item, "_evalxfail") and call.when == "call" and not call.excinfo:
        delattr(item, "_evalxfail")


@pytest.fixture(scope="session")
def all_projects():
    return _resolver.projects


@pytest.fixture(scope="session")
def host_counts():
    return _resolver.host_counts


@pytest.fixture(scope="session")
def all_setup_configs():
    return _resolver.setup_configs


@pytest.fixture(scope="session")
def all_setup_config_map():
    return _resolver.setup_config_map


@pytest.fixture(scope="session")
def orly_rules():
    return _resolver.orly_rules


@pytest.fixture(scope="session")
def maxwell_specs():
    return _resolver.maxwell_specs


@pytest.fixture(scope="session")
def automation_plots():
    return _resolver.automation_plots
