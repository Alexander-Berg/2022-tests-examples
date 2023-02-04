import pytest
from pydantic import ValidationError

from maps.infra.yacare.scripts.make_configs.lib.spec import (
    ServantEndpoint, RatelimiterSettings, QuotatekaSettings, TvmAuthSettings
)


def extract_errors(exc_info: pytest.ExceptionInfo) -> list[str]:
    return [error['msg'] for error in exc_info.value.errors()]


def test_ratelimiter_missing_resource():
    with pytest.raises(ValidationError) as err:
        RatelimiterSettings.parse_obj({})
    assert extract_errors(err) == ['missing ratelimiter resource']


def test_ratelimiter_ambiguous_resource():
    with pytest.raises(ValidationError) as err:
        RatelimiterSettings.parse_obj({
            'resource': 'whatever',
            'resource_script': './some.lua'
        })
    assert extract_errors(err) == ['ambiguous ratelimiter resource']


def test_ratelimiter_default_weight():
    ratelimiter = RatelimiterSettings.parse_obj({'resource': 'whatever'})
    assert ratelimiter == RatelimiterSettings(
        resource='whatever',
        weight=1
    )


def test_ratelimiter_ambiguous_weight():
    with pytest.raises(ValidationError) as err:
        RatelimiterSettings.parse_obj({
            'resource': 'whatever',
            'weight': 153,
            'weight_script': 'heavy.lua',
        })
    assert extract_errors(err) == ['ambiguous ratelimiter weight']


def test_endpoint_ratelimiter_globals():
    endpoint = ServantEndpoint.parse_obj({
        'path': '/where',
        'ratelimiter_global_config': {
            'auth_types': ['ohmy'],
            'resource': ['testapp'],
            'weight': 42
        },
        'ratelimiter_endpoint_config': {
            'resource_script': 'ohmy.lua',
        }
    })
    assert endpoint == ServantEndpoint(
        path='/where',
        # expect merged global and endpoint params
        ratelimiter=RatelimiterSettings(
            auth_types=['ohmy'],
            resource_script='ohmy.lua',
            weight=42
        ),
    )


def test_endpoint_ratelimiter_plus_qttk():
    endpoint = ServantEndpoint.parse_obj({
        'path': '/where',
        'ratelimiter_endpoint_config': {
            'resource': 'wat',
            'quotateka_enable': True,
            'quotateka_endpoint': '/qttk'
        }
    })
    assert endpoint == ServantEndpoint(
        path='/where',
        ratelimiter=RatelimiterSettings(resource='wat', weight=1),
        quotateka=QuotatekaSettings(endpoint='/qttk'),
    )


def test_endpoint_qttk_autodetect():
    endpoint = ServantEndpoint.parse_obj({
        'path': '/where',
        'ratelimiter_endpoint_config': {
            'quotateka_enable': True
        }
    })
    assert endpoint == ServantEndpoint(
        path='/where',
        quotateka=QuotatekaSettings(endpoint='/where')
    )


def test_endpoint_tvm():
    endpoint = ServantEndpoint.parse_obj({
        'path': '/where',
        'tvm2_service': 'testapp',
        'tvm2_required': True
    })
    assert endpoint == ServantEndpoint(
        path='/where',
        tvm=TvmAuthSettings(
            service='testapp',
            service_ticket_required=True
        )
    )


def test_endpoint_local_only():
    endpoint = ServantEndpoint.parse_obj({
        'path': '/where',
        'local_only': True,
        'tvm2_service': 'teapot',
        'ratelimiter_global_config': {
            'auth_types': ['header'],
            'resource': 'testapp',
            'weight': 42,
            'quotateka_enable': True
        },
        'ratelimiter_endpoint_config': {
            'resource': 'testapp',
        }
    })
    # Expect tvm and global ratelimiter settings ignored
    # But endpoint ratelimiter settings applied
    assert endpoint == ServantEndpoint(
        path='/where',
        local_only=True,
        ratelimiter=RatelimiterSettings(resource='testapp', auth_types=['tvm'])
    )


def test_endpoint_minimal():
    endpoint = ServantEndpoint.parse_obj({'path': '/ping'})
    assert endpoint == ServantEndpoint(path='/ping')
