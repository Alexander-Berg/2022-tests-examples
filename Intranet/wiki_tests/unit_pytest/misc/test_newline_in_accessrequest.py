import pytest
from wiki import access as wiki_access
from wiki.pages.models import AccessRequest

pytestmark = [
    pytest.mark.django_db,
]


def test_newline_in_accessrequest(wiki_users, api_url, client, test_page):
    wiki_access.set_access(test_page, wiki_access.TYPES.OWNER, wiki_users.thasonic)
    access_request = AccessRequest(applicant=wiki_users.asm, page=test_page, reason='It\'s reason\nthen')
    access_request.save()

    client.login(wiki_users.asm)
    resp = client.get(f'{api_url}/{test_page.supertag}')
    assert resp.status_code == 403
    assert 'It\'s reason then' in resp.json()['error']['message'][0]
