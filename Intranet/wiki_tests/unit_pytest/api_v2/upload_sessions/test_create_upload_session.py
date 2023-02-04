import pytest

from wiki.api_v2.public.upload_sessions.exceptions import UploadSessionFileSizeLimit, UploadSessionLimitExceeded
from wiki.uploads import logic
from wiki.uploads.consts import UploadSessionTargetType

pytestmark = [
    pytest.mark.django_db
]


def test_create_usual_case(client, wiki_users):
    client.login(wiki_users.thasonic)

    response = client.post('/api/v2/public/upload_sessions',
                           data={
                               'target': UploadSessionTargetType.ATTACHMENT,
                               'file_name': 'Amogus.zip',
                               'file_size': 4096,
                           },
                           )

    response_data = response.json()

    assert response.status_code == 200
    assert response_data['target'] == UploadSessionTargetType.ATTACHMENT.value
    assert response_data['file_name'] == 'Amogus.zip'
    assert response_data['file_size'] == 4096


def test_create_with_limit_sessions(client, wiki_users, monkeypatch):
    monkeypatch.setattr(logic, 'LIMIT_UPLOAD_SESSIONS_PER_USER', 2)

    client.login(wiki_users.thasonic)

    client.post('/api/v2/public/upload_sessions',
                data={
                    'target': UploadSessionTargetType.ATTACHMENT,
                    'file_name': 'amogus.zip',
                    'file_size': 4096,
                },
                )

    client.post('/api/v2/public/upload_sessions',
                data={
                    'target': UploadSessionTargetType.IMPORT_GRID,
                    'file_name': 'sugoma.txt',
                    'file_size': 10010,
                },
                )

    response = client.post('/api/v2/public/upload_sessions',
                           data={
                               'target': UploadSessionTargetType.IMPORT_PAGE,
                               'file_name': 'impostor',
                               'file_size': 8192,
                           },
                           )

    response_data = response.json()

    assert response.status_code == 400
    assert response_data['error_code'] == UploadSessionLimitExceeded.error_code


def test_create_limit_size(client, wiki_users):
    client.login(wiki_users.thasonic)

    response = client.post('/api/v2/public/upload_sessions',
                           data={
                               'target': UploadSessionTargetType.ATTACHMENT,
                               'file_name': 'amogus.zip',
                               'file_size': 500 * 1024 * 1024,
                           },
                           )

    response_data = response.json()

    assert response.status_code == 400
    assert response_data['error_code'] == UploadSessionFileSizeLimit.error_code


def test_create_incorrect_target(client, wiki_users):
    client.login(wiki_users.thasonic)

    response = client.post('/api/v2/public/upload_sessions',
                           data={
                               'target': 'hackerman',
                               'file_name': 'amogus.zip',
                               'file_size': 4096,
                           },
                           )

    response_data = response.json()

    assert response.status_code == 400
    assert response_data['error_code'] == 'VALIDATION_ERROR'


def test_create_incorrect_file_name(client, wiki_users):
    client.login(wiki_users.thasonic)

    response = client.post('/api/v2/public/upload_sessions',
                           data={
                               'target': UploadSessionTargetType.ATTACHMENT,
                               'file_name': 'incorrect/file.txt',
                               'file_size': 4096,
                           },
                           )

    response_data = response.json()

    assert response.status_code == 400
    assert response_data['error_code'] == 'VALIDATION_ERROR'

    response = client.post('/api/v2/public/upload_sessions',
                           data={
                               'target': UploadSessionTargetType.ATTACHMENT,
                               'file_name': 'incorrect>try',
                               'file_size': 4096,
                           },
                           )

    response_data = response.json()

    assert response.status_code == 400
    assert response_data['error_code'] == 'VALIDATION_ERROR'

    response = client.post('/api/v2/public/upload_sessions',
                           data={
                               'target': UploadSessionTargetType.ATTACHMENT,
                               'file_name': 'incorrecttry*',
                               'file_size': 4096,
                           },
                           )

    response_data = response.json()

    assert response.status_code == 400
    assert response_data['error_code'] == 'VALIDATION_ERROR'


def test_create_cyrillic_file_name(client, wiki_users):
    client.login(wiki_users.thasonic)

    request_data = {
        'target': UploadSessionTargetType.ATTACHMENT,
        'file_name': 'Отчет_за_2007й.jpg',
        'file_size': 1024,
    }
    response = client.post('/api/v2/public/upload_sessions', data=request_data)
    assert response.status_code == 200

    request_data = {
        'target': UploadSessionTargetType.IMPORT_PAGE,
        'file_name': 'Без расширения и с пробелом',
        'file_size': 1024,
    }
    response = client.post('/api/v2/public/upload_sessions', data=request_data)
    assert response.status_code == 200

    request_data['file_name'] = 'Без расширений и с пробелом (1)'
    response = client.post('/api/v2/public/upload_sessions', data=request_data)
    assert response.status_code == 200
