import pytest
from django.test.utils import override_settings

from wiki.api_v2.public.upload_sessions.views import CONTENT_TYPE_OCTET_STREAM
from wiki.uploads.consts import UploadSessionTargetType
from wiki.uploads.models import UploadSession

pytestmark = [pytest.mark.django_db]


@override_settings(DATA_UPLOAD_MAX_MEMORY_SIZE=16 * 1024 * 1024)
def test_upload_chunks_and_finalize(client, wiki_users, upload_sessions):
    client.login(wiki_users.thasonic)
    demo_data = b'1234567890abc' * 50
    response = client.post(
        '/api/v2/public/upload_sessions',
        data={
            'target': UploadSessionTargetType.ATTACHMENT,
            'file_name': 'amogus.zip',
            'file_size': len(demo_data),
        },
    )
    assert response.status_code == 200
    session_id = response.json()['session_id']

    session = UploadSession.objects.get(session_id=session_id)
    ptr = 0
    part_number = 1
    chunk_size = 100
    while ptr < len(demo_data):
        resp = client.put_multipart(
            f'/api/v2/public/upload_sessions/{session_id}/upload_part?part_number={part_number}',
            data=demo_data[ptr : ptr + chunk_size],
            content_type=CONTENT_TYPE_OCTET_STREAM,
        )

        assert resp.status_code == 200
        session.refresh_from_db()

        ptr += chunk_size
        assert session.uploaded_size == min(ptr, len(demo_data))
        part_number += 1

    resp = client.post(f'/api/v2/public/upload_sessions/{session_id}/finish')

    assert resp.status_code == 200
