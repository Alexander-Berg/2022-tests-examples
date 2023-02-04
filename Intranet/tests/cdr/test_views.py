import pytest
from .conftest import load_xml


pytestmark = pytest.mark.django_db


def test_cdr_view(client, cms_participant_join_record):
    data = load_xml('call_leg_end_call_end')
    response = client.post(
        '/cdr/',
        data=data,
        content_type='application/xml'
    )
    assert response.status_code == 200
