import pytest
from ujson import loads

pytestmark = [
    pytest.mark.django_db,
]


class TestAPIAccessHandler:
    """
    Test for access api handlers
    """

    def test_access_handle_auth_get(self, client, wiki_users, test_page, api_url):
        """
        test if handle /.access answers correctly to page owner and guest
        """
        # owner GET
        client.login('thasonic')
        response = client.get(f'{api_url}/{test_page.supertag}/.access')
        assert response.status_code == 200

        page_data = loads(response.content)
        assert 'error' not in page_data

    def test_access_handle_auth_post(self, client, wiki_users, test_page, api_url):
        client.login('thasonic')
        response = client.post(f'{api_url}/{test_page.supertag}/.access', {'type': 'owner'})
        assert response.status_code == 200
        page_data = loads(response.content)
        assert 'error' not in page_data

    def test_access_guest_get(self, client, wiki_users, test_page, api_url):
        client.login('kolomeetz')
        response = client.get(f'{api_url}/{test_page.supertag}/.access')
        assert response.status_code == 200

        page_data = loads(response.content)
        assert 'error' not in page_data
