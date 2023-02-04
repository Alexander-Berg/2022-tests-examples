import pytest
from pydantic import ValidationError

from maps.infra.sedem.lib.config.defaults import ServiceDefaults
from maps.infra.sedem.lib.config.schema import ServiceConfig
from maps.infra.sedem.lib.config.schema.deploy import (
    AcceptanceTest,
    DeployProfile,
    DeploySection,
    SandboxDeploySettings,
    StageSettings,
)
from maps.infra.sedem.lib.config.schema.tests.shared import DEFAULT_CONFIG_CONTENT, extract_errors


def test_minimal_valid_stage_settings() -> None:
    stage_settings = StageSettings.parse_obj({
        'targets': []
    })

    assert stage_settings == StageSettings.construct(
        min_time=None,
        max_alert_history_window=None,
        targets=[],
    )


def test_valid_featured_stage_settings() -> None:
    stage_settings = StageSettings.parse_obj({
        'min_time': 60 * 60,
        'max_alert_history_window': '1d 12h 30m',
        'targets': ['testing', 'load'],
    })

    assert stage_settings == StageSettings.construct(
        min_time=3600,
        max_alert_history_window=131400,
        targets=['testing', 'load'],
    )


def test_invalid_stage_settings_with_bad_timedelta() -> None:
    with pytest.raises(ValidationError) as exc:
        StageSettings.parse_obj({
            'min_time': ...,
            'max_alert_history_window': 'abc',
            'targets': []
        })

    assert extract_errors(exc) == [
        'field requires integer or string value if present',
        'invalid string format, expected: "[{days}d][{hours}h][{minutes}m]"',
    ]


def test_minimal_valid_deploy_profile() -> None:
    deploy_profile = DeployProfile.parse_obj({})

    assert deploy_profile == DeployProfile.construct(
        initial_step_name='testing',
        unstable=None,
        testing=None,
        prestable=None,
        stable=None,
    )


def test_valid_featured_deploy_profile() -> None:
    deploy_profile = DeployProfile.parse_obj({
        'initial_step_name': 'unstable',
        'unstable': {
            'targets': ['unstable'],
        },
        'testing': {
            'min_time': '1h',
            'max_alert_history_window': '30m',
            'targets': ['testing', 'load'],
        },
        'stable': {
            'targets': ['stable', 'datatesting'],
        },
    })

    assert deploy_profile == DeployProfile.construct(
        initial_step_name='unstable',
        unstable=StageSettings(
            targets=['unstable'],
        ),
        testing=StageSettings(
            min_time='1h',
            max_alert_history_window='30m',
            targets=['testing', 'load'],
        ),
        prestable=None,
        stable=StageSettings(
            targets=['stable', 'datatesting'],
        ),
    )


def test_invalid_deploy_profile_with_duplicates() -> None:
    with pytest.raises(ValidationError) as exc:
        DeployProfile.parse_obj({
            'unstable': {
                'targets': ['unstable'],
            },
            'testing': {
                'targets': ['unstable', 'testing'],
            },
        })

    assert extract_errors(exc) == [
        'duplication of "unstable" deploy unit(-s) in different stages'
    ]


def test_minimal_valid_deploy_profiles_section() -> None:
    config = ServiceConfig.parse_obj({
        **DEFAULT_CONFIG_CONTENT,
        'deploy_profiles': {
            'default': {},
        },
    })

    assert config.deploy_profiles == {
        'default': DeployProfile(
            initial_step_name='testing',
            unstable=None,
            testing=None,
            prestable=None,
            stable=None,
        ),
    }


def test_minimal_valid_sandbox_settings() -> None:
    sandbox_settings = SandboxDeploySettings.parse_obj({
        'owner': 'MAPS-CI',
    }, defaults=ServiceDefaults('maps-core-fake-service'))

    assert sandbox_settings == SandboxDeploySettings.construct(
        owner='MAPS-CI',
        task_type='FAKE_SERVICE',
    )


def test_minimal_valid_deploy_section() -> None:
    deploy = DeploySection.parse_obj({})

    assert deploy == DeploySection.construct(
        deploy_profile='default',
        sandbox=None,
    )


def test_valid_acceptance_test_with_scheduler() -> None:
    acceptance = AcceptanceTest.parse_obj({
        'scheduler_id': 12345,
    })

    assert acceptance == AcceptanceTest.construct(
        scheduler_id='12345',
    )


def test_valid_acceptance_test_with_template() -> None:
    acceptance = AcceptanceTest.parse_obj({
        'template_name': 'MY_TEMPLATE'
    })

    assert acceptance == AcceptanceTest.construct(
        template_name='MY_TEMPLATE',
    )


def test_invalid_acceptance_with_scheduler_and_template() -> None:
    with pytest.raises(ValidationError) as exc:
        AcceptanceTest.parse_obj({
            'scheduler_id': 12345,
            'template_name': 'MY_TEMPLATE'
        })

    assert extract_errors(exc) == [
        'one and only one of "scheduler_id" or "template_name" must be defined'
    ]


def test_invalid_acceptance_without_anything() -> None:
    with pytest.raises(ValidationError) as exc:
        AcceptanceTest.parse_obj({})

    assert extract_errors(exc) == [
        'one and only one of "scheduler_id" or "template_name" must be defined'
    ]


def test_valid_minimal_acceptances_section() -> None:
    config = ServiceConfig.parse_obj({
        **DEFAULT_CONFIG_CONTENT,
        'acceptance': {
            'stable': [{
                'scheduler_id': 12345,
            }],
        },
    })

    assert config.acceptance == {
        'stable': [AcceptanceTest(scheduler_id='12345')],
    }
