import pytest

from django.test import override_settings

from wiki.api_v2.public.upload_sessions.exceptions import InvalidUploadSession
from wiki.pages.models import Page
from wiki.uploads.consts import UploadSessionStatusType

from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_author_only


pytestmark = [pytest.mark.django_db]


def test_get_attachments(client, wiki_users, test_files):
    page = test_files['file1'].page
    client.login(wiki_users.thasonic)
    response = client.get(f'/api/v2/public/pages/{page.id}/attachments')
    assert response.status_code == 200
    results = response.json()['results']
    assert len(results) == 2
    assert results[0]['id'] == test_files['file2'].id
    assert results[0]['name'] == test_files['file2'].name
    assert results[1]['id'] == test_files['file1'].id
    assert results[1]['name'] == test_files['file1'].name


def test_get_attachments__cow(client, wiki_users):
    client.login(wiki_users.thasonic)

    idx, slug = -101, 'cow-users'
    copy_on_write_info = {'id': idx, 'ru': {'title': 'Личные разделы пользователей', 'template': 'pages/ru/users.txt'}}

    with override_settings(COPY_ON_WRITE_TAGS={slug: copy_on_write_info}, COPY_ON_WRITE_IDS={idx: slug}):
        response = client.get(f'/api/v2/public/pages/{idx}/attachments')
        assert response.status_code == 200
        assert response.json()['results'] == []


def test_get_attachments_ordered(client, wiki_users, test_files):
    page = test_files['file1'].page
    client.login(wiki_users.thasonic)
    response = client.get(f'/api/v2/public/pages/{page.id}/attachments?order_by=name&order_direction=asc')
    assert response.status_code == 200
    results = response.json()['results']
    assert len(results) == 2
    assert results[0]['id'] == test_files['file1'].id
    assert results[0]['name'] == test_files['file1'].name
    assert results[1]['id'] == test_files['file2'].id
    assert results[1]['name'] == test_files['file2'].name

    response = client.get(f'/api/v2/public/pages/{page.id}/attachments?order_by=name&order_direction=desc')
    assert response.status_code == 200
    results = response.json()['results']
    assert len(results) == 2
    assert results[0]['id'] == test_files['file2'].id
    assert results[0]['name'] == test_files['file2'].name
    assert results[1]['id'] == test_files['file1'].id
    assert results[1]['name'] == test_files['file1'].name


def test_get_attachments_no_access(client, wiki_users, test_files):
    page = test_files['file1'].page
    set_access_author_only(page)
    client.login(wiki_users.chapson)
    response = client.get(f'/api/v2/public/pages/{page.id}/attachments')
    assert response.status_code == 403


def test_attach_file(client, wiki_users, test_page, upload_sessions):
    client.login(wiki_users.thasonic)
    first_session = '9b18deaa-b969-4caa-a4f0-b13e455b610b'
    second_session = 'bb143df4-9309-4ae1-97d1-93f8d86d9805'

    upload_sessions[first_session].status = UploadSessionStatusType.FINISHED
    upload_sessions[first_session].save()

    upload_sessions[second_session].status = UploadSessionStatusType.FINISHED
    upload_sessions[second_session].save()

    response = client.post(
        f'/api/v2/public/pages/{test_page.id}/attachments', {'upload_sessions': [first_session, second_session]}
    )
    assert response.status_code == 200

    response_results = response.json()['results']
    assert response_results[0]['name'] == 'sugoma.txt'
    assert response_results[0]['size'] == '2.00'
    assert response_results[0]['download_url'] == '/testpage/.files/sugoma.txt'

    assert response_results[1]['name'] == 'funnycat.png'
    assert response_results[1]['size'] == '1.00'
    assert response_results[1]['download_url'] == '/testpage/.files/funnycat.png'

    test_page.refresh_from_db()
    upload_sessions[first_session].refresh_from_db()
    upload_sessions[second_session].refresh_from_db()
    assert test_page.files == 2
    assert upload_sessions[first_session].status == UploadSessionStatusType.USED
    assert upload_sessions[second_session].status == UploadSessionStatusType.USED


def test_attach_file__cow(client, wiki_users, upload_sessions):
    client.login(wiki_users.thasonic)

    session = '9b18deaa-b969-4caa-a4f0-b13e455b610b'
    upload_sessions[session].status = UploadSessionStatusType.FINISHED
    upload_sessions[session].save()

    idx, slug = -101, 'cow-users'
    copy_on_write_info = {'id': idx, 'ru': {'title': 'Личные разделы пользователей', 'template': 'pages/ru/users.txt'}}

    with override_settings(COPY_ON_WRITE_TAGS={slug: copy_on_write_info}, COPY_ON_WRITE_IDS={idx: slug}):
        response = client.post(f'/api/v2/public/pages/{idx}/attachments', {'upload_sessions': [session]})
        assert response.status_code == 200
        assert response.json()['results'][0]['name'] == 'sugoma.txt'

    assert Page.objects.get(supertag=slug).files == 1
    upload_sessions[session].refresh_from_db()
    assert upload_sessions[session].status == UploadSessionStatusType.USED


def test_wrong_status(client, wiki_users, test_page, upload_sessions):
    client.login(wiki_users.thasonic)
    session_id = '9b18deaa-b969-4caa-a4f0-b13e455b610b'

    response = client.post(f'/api/v2/public/pages/{test_page.id}/attachments', {'upload_sessions': [session_id]})
    assert response.status_code == 400
    assert response.json()['error_code'] == InvalidUploadSession.error_code


def test_wrong_target(client, wiki_users, test_page, upload_sessions):
    client.login(wiki_users.thasonic)
    session_id = '3ee4bb73-57f4-4048-ac66-ce1777341cb0'

    upload_sessions[session_id].status = UploadSessionStatusType.FINISHED
    upload_sessions[session_id].save()

    response = client.post(f'/api/v2/public/pages/{test_page.id}/attachments', {'upload_sessions': [session_id]})
    assert response.status_code == 400
    assert response.json()['error_code'] == InvalidUploadSession.error_code


def test_two_sessions_error(client, wiki_users, test_page, upload_sessions):
    client.login(wiki_users.thasonic)
    good_session_id = '9b18deaa-b969-4caa-a4f0-b13e455b610b'
    bad_session_id = 'bb143df4-9309-4ae1-97d1-93f8d86d9805'

    upload_sessions[good_session_id].status = UploadSessionStatusType.FINISHED
    upload_sessions[good_session_id].save()

    response = client.post(
        f'/api/v2/public/pages/{test_page.id}/attachments', {'upload_sessions': [good_session_id, bad_session_id]}
    )
    assert response.status_code == 400
    assert response.json()['error_code'] == InvalidUploadSession.error_code
    test_page.refresh_from_db()
    upload_sessions[good_session_id].refresh_from_db()
    upload_sessions[bad_session_id].refresh_from_db()
    assert test_page.files == 0
    assert upload_sessions[good_session_id].status != UploadSessionStatusType.USED
    assert upload_sessions[bad_session_id].status != UploadSessionStatusType.USED


def test_delete_attach(client, wiki_users, test_files):
    test_file = test_files['file1']
    page = test_file.page
    assert page.files == 2

    client.login(wiki_users.thasonic)
    response = client.delete(f'/api/v2/public/pages/{page.id}/attachments/{test_file.id}')
    assert response.status_code == 204

    test_file.refresh_from_db()
    assert test_file.status == 0

    page.refresh_from_db()
    assert page.files == 1

    response = client.delete(f'/api/v2/public/pages/{page.id}/attachments/{test_file.id}')
    assert response.status_code == 404


def test_delete_attach_no_access(client, wiki_users, test_files):
    test_file = test_files['file1']
    page = test_file.page

    client.login(wiki_users.chapson)
    response = client.delete(f'/api/v2/public/pages/{page.id}/attachments/{test_file.id}')
    assert response.status_code == 403


def test_delete_attach_wrong_page(client, wiki_users, test_files, page_cluster):
    test_file = test_files['file1']
    page = page_cluster['root']

    client.login(wiki_users.thasonic)
    response = client.delete(f'/api/v2/public/pages/{page.id}/attachments/{test_file.id}')
    assert response.status_code == 404
