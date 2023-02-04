
import json

import mock
import pytest
import httpretty

from balance.actions.nirvana.operations import billing_curl

from tests import object_builder as ob


@pytest.fixture(autouse=True)
def patch_sleep():
    with mock.patch('balance.actions.nirvana.operations.billing_curl.time.sleep'):
        yield


@pytest.fixture()
def httpretty_enabled_fixture():
    """ reduce indentation when using httpretty. """
    with httpretty.enabled(allow_net_connect=False):
        yield


def create_nirvana_block(session, options, data, **kwargs):
    download_url = 'https://download.ru'
    upload_url = 'https://upload.ru'

    httpretty.register_uri(
        httpretty.GET, download_url,
        body=json.dumps(data),
        content_type="text/json",
    )

    for d in data:
        httpretty.register_uri(
            httpretty.POST, d['url'],
            body=json.dumps(d.get('body', 'body')),
            content_type="text/json",
        )

    httpretty.register_uri(
        httpretty.PUT, upload_url,
    )

    return ob.NirvanaBlockBuilder.construct(
        session,
        operation='billing_curl',
        request={
            'data': {
                'inputs': {
                    'input': {
                        'items': [
                            {
                                'downloadURL': download_url
                            }
                        ]
                    }
                },
                'outputs': {
                    'output': {
                        'items': [
                            {
                                'uri': upload_url
                            }
                        ]
                    }
                },
                'options': options
            }
        },
        **kwargs
    )


@pytest.mark.usefixtures('httpretty_enabled_fixture')
@pytest.mark.parametrize(
    ['allowed_hosts'],
    [
        pytest.param(['test.com'], id='with allowed_hosts'),
        pytest.param([], id='without allowed_hosts'),
    ]
)
def test_ok(session, allowed_hosts):
    block = create_nirvana_block(
        session,
        options={'allowed_hosts': json.dumps(allowed_hosts)},
        data=[{
            'url': 'https://test.com:8000/getData?param=value',
            'body': 'body'
        }]
    )
    billing_curl.process(block)
    request = httpretty.last_request()
    assert request.method == httpretty.PUT
    body = json.loads(request.body)
    assert len(body) == 1
    result = json.loads(body[0])
    assert result['response_body'] == 'body'


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_not_allowed_host(session):
    block = create_nirvana_block(
        session,
        options={'allowed_hosts': json.dumps(['ya.com'])},
        data=[{'url': 'https://test.com:8000/getData?param=value'}]
    )
    with pytest.raises(AssertionError, match='Not allowed hostname'):
        billing_curl.process(block)
