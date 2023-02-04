import pytest
import waffle

from intranet.crt.constants import CERT_TYPE, CA_NAME, CERT_STATUS
from intranet.crt.core.models import Certificate
from django.core.cache import cache


pytestmark = pytest.mark.django_db


@pytest.mark.xfail
def test_request(crt_client, users):
    request_data = {
        'type': CERT_TYPE.RC_SERVER,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': 'xxx.search.yandex.net',
    }
    crt_client.login('rc_server_user')

    switch = waffle.models.Switch.objects.create(name='intranet.crtreadonly', active=True)

    response = crt_client.json.post('/api/certificate/', data=request_data)
    assert response.status_code == 500

    switch.active = False
    switch.save()
    switch.refresh_from_db()
    assert switch.active is False

    cache.clear()

    response = crt_client.json.post('/api/certificate/', data=request_data)
    assert response.status_code == 201

    cert = Certificate.objects.get()
    assert cert.status == CERT_STATUS.ISSUED
