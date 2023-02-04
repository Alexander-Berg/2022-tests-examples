import pytest

from hashlib import md5
from model_mommy import mommy
from waffle.testutils import override_switch

from wiki.api_core.waffle_switches import DELIVER_FILES_VIA_SIGNED_URL
from wiki.files.models import File
from wiki.pages.models import LocationHistory
from wiki.uploads.s3_client import MOCKED_STORAGE_HOST

from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_author_only

pytestmark = [pytest.mark.django_db]

DOWNLOAD_API_URL = '/api/v2/public/pages/attachments/download_by_url'


@pytest.fixture
def page_files(wiki_users, page_cluster, request):
    file_storage = getattr(request, 'param', 'mds')

    storage_field = {
        'mds': 'mds_storage_id',
        's3': 's3_storage_key',
    }

    blueprint = [
        (page_cluster['root'], ['image_123.png', 'русское_название.png', '1.docx']),
        (page_cluster['root/a'], ['image_123.png', '2.docx']),
    ]

    mapping = {}
    for page, filenames in blueprint:
        mapping[page] = []
        for file_name in filenames:
            file = File(
                page=page,
                user=wiki_users.thasonic,
                name=file_name,
                url=File.get_unique_url(page, file_name),
                description='',
            )

            storage_key = f'{page.slug}:{md5(file_name.encode()).hexdigest()}'
            setattr(file, storage_field[file_storage], storage_key)
            file.save()

            mapping[page].append(file)

    return mapping


@pytest.mark.parametrize('page_files', ['s3', 'mds'], indirect=True)
def test_attachments(client, wiki_users, page_cluster, page_files):
    client.login(wiki_users.thasonic)
    page = page_cluster['root']

    # first page
    response = client.get(f'/api/v2/public/pages/{page.id}/attachments?page_size=2')
    assert response.status_code == 200

    res = response.json()
    assert len(res['results']) == 2
    assert res['prev_cursor'] is None and res['next_cursor']

    # last page
    response = client.get(f'/api/v2/public/pages/{page.id}/attachments?cursor={res["next_cursor"]}')
    assert response.status_code == 200

    res = response.json()
    assert len(res['results']) == 1
    assert res['prev_cursor'] and res['next_cursor'] is None


@pytest.mark.parametrize('page_files', ['s3', 'mds'], indirect=True)
def test_download_file_by_id(client, wiki_users, page_cluster, page_files):
    client.login(wiki_users.thasonic)
    page = page_cluster['root']
    response = client.get(
        f'/api/v2/public/pages/{page.id}/attachments/{page_files[page][0].id}/download',
    )
    assert response.status_code == 200

    with override_switch(DELIVER_FILES_VIA_SIGNED_URL, active=True):
        response = client.get(
            f'/api/v2/public/pages/{page.id}/attachments/{page_files[page][0].id}/download',
        )
        assert response.status_code == 302


@pytest.mark.parametrize('page_files', ['mds', 's3'], indirect=True)
def test_download_file_by_id__404(client, wiki_users, page_cluster, page_files):
    client.login(wiki_users.thasonic)
    page = page_cluster['root']
    response = client.get(
        f'/api/v2/public/pages/{page.id + 1}/attachments/{page_files[page][0].id}/download',
    )

    assert response.status_code == 404
    assert response.get('content-type') == 'image/png'


@pytest.mark.parametrize('page_files', ['mds', 's3'], indirect=True)
def test_download_file_by_id__403(client, wiki_users, page_cluster, page_files):
    client.login(wiki_users.asm)
    page = page_cluster['root']

    set_access_author_only(page)
    response = client.get(
        f'/api/v2/public/pages/{page.id}/attachments/{page_files[page][0].id}/download',
    )

    assert response.status_code == 403
    assert response.get('content-type') == 'image/png'


@pytest.mark.parametrize('page_files', ['mds', 's3'], indirect=True)
def test_download_file_by_frontend_url(client, wiki_users, page_cluster, page_files):
    client.login(wiki_users.thasonic)
    page = page_cluster['root']
    urls = [
        f'{DOWNLOAD_API_URL}?url={page.slug}/.files/{page_files[page][0].url}',
        f'{DOWNLOAD_API_URL}?url=роот/.files/{page_files[page][0].url}',
    ]

    for request_url in urls:
        response = client.get(request_url)
        assert response.status_code == 200

        with override_switch(DELIVER_FILES_VIA_SIGNED_URL, active=True):
            response = client.get(request_url)
            assert response.status_code == 302


@pytest.mark.parametrize('page_files', ['mds', 's3'], indirect=True)
@override_switch(DELIVER_FILES_VIA_SIGNED_URL, active=True)
def test_download_file_by_frontend_url__redirect_heuristics(client, wiki_users, page_cluster, page_files):
    client.login(wiki_users.thasonic)

    page_b = page_cluster['root/a']
    page = page_cluster['root']
    page.redirects_to = page_cluster['root/a']
    page.save()

    page_b_file = page_files[page_b][1]
    page_file = page_files[page][0]

    response = client.get(
        f'{DOWNLOAD_API_URL}?url={page.slug}/.files/{page_b_file.url}',
    )

    storage_key = page_b_file.s3_storage_key if page_b_file.is_stored_on_s3 else page_b_file.mds_storage_id
    assert response.status_code == 302
    assert response.get('location').replace(MOCKED_STORAGE_HOST, '') == storage_key

    # на странице стоит редирект root -> root/a; в обоих есть файл с именем "image_123.png".
    # Должен вернуться файл от root

    response = client.get(f'{DOWNLOAD_API_URL}?url={page.slug}/.files/{page_file.url}')

    storage_key = page_file.s3_storage_key if page_file.is_stored_on_s3 else page_file.mds_storage_id
    assert response.status_code == 302
    assert response.get('location').replace(MOCKED_STORAGE_HOST, '') == storage_key

    response = client.get(f'{DOWNLOAD_API_URL}?url=роот/.files/{page_file.url}')
    assert response.status_code == 302


@pytest.mark.parametrize('page_files', ['mds', 's3'], indirect=True)
@override_switch(DELIVER_FILES_VIA_SIGNED_URL, active=True)
def test_download_file_by_frontend_url__location_history_heuristics(client, wiki_users, page_cluster, page_files):
    client.login(wiki_users.thasonic)
    page = page_cluster['root']
    page_file = page_files[page][0]
    mommy.make(LocationHistory, slug='oldroot', page=page)

    response = client.get(f'{DOWNLOAD_API_URL}?url=oldroot/.files/{page_file.url}')

    assert response.status_code == 302

    storage_key = page_file.s3_storage_key if page_file.is_stored_on_s3 else page_file.mds_storage_id
    assert response.get('location').replace(MOCKED_STORAGE_HOST, '') == storage_key
