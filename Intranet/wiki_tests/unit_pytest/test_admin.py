import logging
from wiki.users.models import User
import pytest

pytestmark = [
    pytest.mark.django_db,
]


def test_error_code_logging(wiki_users, client, test_page, caplog):
    """
    500ки/400ки/409ки должен оставить в логах код ошибки
    """

    u = User.objects.get(username='thasonic')
    u.is_superuser = True
    u.is_staff = True
    u.save()

    with caplog.at_level(logging.INFO):
        client.login('thasonic')
        response = client.get('/_api/svc/.smoke_500')

    assert response.status_code == 500
    assert 'error_code: UNKNOWN_ERROR' in caplog.text
