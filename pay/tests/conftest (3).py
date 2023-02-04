from paysys.sre.tools.deploy.validator.lib.clients.deploy import DeployClient
from paysys.sre.tools.deploy.validator.lib.clients.racktables import RacktablesClient
from paysys.sre.tools.deploy.validator.lib.clients.abc import AbcClient
import pytest
import os


def pytest_addoption(parser):
    parser.addoption(
        "--stage", action="store", default="billing-accounts-prod-stage"
    )

    parser.addoption(
        "--yav-token", action="store", default=os.getenv("YAV_TOKEN")
    )

    parser.addoption(
        "--abc-token", action="store", default=os.getenv("ABC_TOKEN")
    )


@pytest.fixture(scope='session')
def secrets(stage):
    return


def _get_stage(request):
    return DeployClient().get_stage_spec(request.config.getoption("--stage"))


@pytest.fixture(scope='session')
def stage(request):
    return _get_stage(request)


@pytest.fixture(scope='session')
def warn_ids(stage):
    warn_envs = ["testing", "production", "stable"]
    return [p.format(project=stage.get('meta').get('project_id'), env=e) for p in ["{project}-{env}-stage"] for e in warn_envs]


@pytest.fixture(scope='session')
def id(stage):
    return stage.get('meta').get('id')


@pytest.fixture(scope='session')
def allowed_ids(stage):
    allowed_envs = ["load", "test", "testing", "prod", "production", "stable", "canary"]
    return [p.format(project=stage.get('meta').get('project_id'), env=e) for p in ["{project}-{env}-stage"] for e in allowed_envs]


@pytest.fixture(scope='session')
def environment(id):
    return id.split('-')[-2]


@pytest.fixture(scope='session')
def production(environment):
    return environment in ["prod", "stable", "production"]


def _get_deploy_units(stage):
    return stage.get('spec').get('deploy_units').items()


@pytest.fixture(scope='session')
def deploy_units(stage):
    return _get_deploy_units(stage)


def _get_secrets(stage):
    return stage.get('spec').get('')


def pytest_generate_tests(metafunc):
    if "deploy_unit" in metafunc.fixturenames:
        du = _get_deploy_units(_get_stage(metafunc))
        metafunc.parametrize("deploy_unit", du, ids=["{} du".format(k) for k, v in du])

    if "secret" in metafunc.fixturenames:
        sec = _get_secrets(_get_stage(metafunc))
        metafunc.parametrize("secret", sec, ids=["{} sec".format(k) for k, v in sec])


@pytest.fixture(scope='session')
def project(stage):
    return stage.get('meta')


@pytest.fixture(scope='session')
def macros_owners():
    return RacktablesClient.get_macros_owners()


@pytest.fixture(scope='session')
def abc_service(stage, request):
    return AbcClient(request.config.getoption('--abc-token')).get_service_by_id(stage.get('meta').get('account_id').split(':')[-1])


@pytest.fixture(scope='session')
def network_macro_valid_abc_roles(abc_service):
    pattern = 'svc_{slug}_{role}'
    roles = ['devops', 'administration']
    slug = abc_service.get('slug')
    return [pattern.format(slug=slug, role=r) for r in roles]
