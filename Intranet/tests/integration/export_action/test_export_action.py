import pytest

from urllib.parse import unquote

from django.contrib.contenttypes.models import ContentType
from django.urls.base import reverse

from intranet.femida.tests import factories as f


@pytest.mark.parametrize('has_perm, response_code, redirect_url', (
    (True, 200, None),
    (False, 302, '/admin/login/?next=/export_action/export/?ct={}&ids={}'),
))
@pytest.mark.filterwarnings('ignore:.*load admin_static.*')
def test_export_action(client, has_perm, response_code, redirect_url):
    candidate = f.CandidateFactory()
    content_type = ContentType.objects.get_for_model(candidate._meta.model)

    if has_perm:
        user = f.create_user_with_perm('can_export_from_admin', is_staff=True)
    else:
        user = f.create_user(is_staff=True)
    client.login(login=user.username)

    url = reverse('export_action:export')
    response = client.get(url, {'ct': content_type.id, 'ids': candidate.id})
    assert response.status_code == response_code
    if redirect_url:
        assert unquote(response.url) == redirect_url.format(content_type.id, candidate.id)
