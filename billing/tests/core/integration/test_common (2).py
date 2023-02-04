import pytest
from mdh.core.exceptions import HttpIntegrationError
from mdh.core.integration.common import HttpClient


def test_http_client(response_mock):

    client = HttpClient(url_base='https://some.com/', raise_on_status=True)

    with response_mock('POST https://some.com/here/ -> 200:{"a": "b"}'):
        response = client.request('/here/', data={'f': 'j'})
        assert str(response) == '200 https://some.com/here/'
        assert response.dumped.startswith(' > POST /here/')
        assert response.url == 'https://some.com/here/'
        assert response.ok
        assert response.content == b'{"a": "b"}'
        assert response.status_code == 200
        assert response.text == '{"a": "b"}'
        assert response.json() == {'a': 'b'}

    with response_mock('POST https://some.com/there/ -> 400:{"a": "b"}'):
        with pytest.raises(HttpIntegrationError) as e:
            client.request('/there/', data=b'b=f')

        assert str(e.value) == '400 https://some.com/there/'
