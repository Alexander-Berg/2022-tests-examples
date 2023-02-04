import pytest
from django.test import override_settings

from intranet.wiki.tests.wiki_tests.common.skips import only_intranet
from wiki.api_v2.public.pages.page.schemas import CreateMs365Method
from wiki.integrations.ms.consts import Ms365DocType
from wiki.pages.models import Page, CloudPage
from wiki.pages.models.consts import PageType

XLS_EMBEDDING = 'https://yandexteam.sharepoint.com/:x:/r/sites/wiki-dev/_layouts/15/Doc.aspx?sourcedoc=%7B1DDE5908-B772-4FED-90EB-0D9B8E89643D%7D&file=Book%203.xlsx&wdOrigin=OFFICECOM-WEB.MAIN.REC&ct=1640101032441&action=default&mobileredirect=true'  # noqa


@only_intranet
@pytest.mark.django_db
@override_settings(DOC_RETRIEVER_USE_STUB=True)
def test_create_page_from_url(client, wiki_users, organizations, page_cluster, test_org_id):
    client.login(wiki_users.thasonic)

    response = client.post(
        '/api/v2/public/pages?fields=content',
        data={
            'page_type': PageType.CLOUD_PAGE.value,
            'title': 'Test',
            'slug': 'root/some_slug',
            'subscribe_me': True,
            'cloud_page': {'method': CreateMs365Method.FROM_URL.value, 'url': XLS_EMBEDDING},
        },
    )

    pk = response.json()['id']
    page = Page.objects.get(pk=pk)
    cloud_page: CloudPage = CloudPage.objects.get(page=page)

    assert page.page_type == page.TYPES.CLOUD
    assert cloud_page.get_cloud_src()


IFRAME_URL = """<iframe src="https://yandexteam-my.sharepoint.com/personal/neofelis_yandex-team_ru/_layouts/15/Doc.aspx?sourcedoc={eaca1432-469e-45ac-b897-ec30fa86baa5}&amp;action=embedview&amp;wdStartOn=1" width="476px" height="288px" frameborder="0">This is an embedded <a target="_blank" href="https://office.com">Microsoft Office</a> document, powered by <a target="_blank" href="https://office.com/webapps">Office</a>.</iframe>"""  # noqa


@only_intranet
@pytest.mark.django_db
@override_settings(DOC_RETRIEVER_USE_STUB=True)
def test_create_page_from_iframe(client, wiki_users, organizations, page_cluster, test_org_id):
    client.login(wiki_users.thasonic)

    response = client.post(
        '/api/v2/public/pages?fields=content',
        data={
            'page_type': PageType.CLOUD_PAGE.value,
            'title': 'Test',
            'slug': 'root/some_slug',
            'subscribe_me': True,
            'cloud_page': {'method': CreateMs365Method.FROM_URL.value, 'url': IFRAME_URL},
        },
    )

    pk = response.json()['id']
    page = Page.objects.get(pk=pk)
    cloud_page: CloudPage = CloudPage.objects.get(page=page)

    assert page.page_type == page.TYPES.CLOUD

    assert cloud_page.presentation_params['wdStartOn'] == '1'
    cloud_src = cloud_page.get_cloud_src()

    assert cloud_src.embedding.domain == 'yandexteam-my.sharepoint.com'
    assert cloud_src.embedding.namespace == '/personal/neofelis_yandex-team_ru/'
    assert cloud_src.embedding.sourcedoc == '{eaca1432-469e-45ac-b897-ec30fa86baa5}'


@only_intranet
@pytest.mark.django_db
@override_settings(DOC_RETRIEVER_USE_STUB=True)
def test_create_page_from_url_bad_url(client, wiki_users, organizations, page_cluster, test_org_id):
    client.login(wiki_users.thasonic)

    response = client.post(
        '/api/v2/public/pages?fields=content',
        data={
            'page_type': PageType.CLOUD_PAGE.value,
            'title': 'Test',
            'slug': 'root/some_slug',
            'subscribe_me': True,
            'cloud_page': {'method': CreateMs365Method.FROM_URL.value, 'url': 'XLS_EMBEDDING'},
        },
    )
    assert response.status_code == 400
    assert response.json()['error_code'] == 'MS365_BAD_LINK'


@only_intranet
@pytest.mark.django_db
@override_settings(DOC_RETRIEVER_USE_STUB=True)
def test_create_page__new_doc(client, wiki_users, organizations, page_cluster, test_org_id):
    client.login(wiki_users.thasonic)

    response = client.post(
        '/api/v2/public/pages?fields=content',
        data={
            'page_type': PageType.CLOUD_PAGE.value,
            'title': 'Test',
            'slug': 'root/some_slug',
            'subscribe_me': True,
            'cloud_page': {'method': CreateMs365Method.EMPTY_DOC.value, 'doctype': Ms365DocType.DOCX},
        },
    )

    pk = response.json()['id']
    page = Page.objects.get(pk=pk)
    cloud_page: CloudPage = CloudPage.objects.get(page=page)

    assert page.page_type == page.TYPES.CLOUD
    assert cloud_page.get_cloud_src().document.type == Ms365DocType.DOCX


@only_intranet
@pytest.mark.django_db
@override_settings(DOC_RETRIEVER_USE_STUB=True)
def test_create_page__2phase_upload(client, wiki_users, organizations, page_cluster, test_org_id):
    client.login(wiki_users.thasonic)

    response = client.post(
        '/api/v2/public/pages?fields=content',
        data={
            'page_type': PageType.CLOUD_PAGE.value,
            'title': 'Test',
            'slug': 'root/some_slug',
            'subscribe_me': True,
            'cloud_page': {
                'method': CreateMs365Method.UPLOAD_DOC.value,
                'mimetype': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            },
        },
    )
    assert response.status_code == 200

    upload_session = response.json()['upload_session']

    response = client.post(
        '/api/v2/public/pages?fields=content',
        data={
            'page_type': PageType.CLOUD_PAGE.value,
            'title': 'Test',
            'slug': 'root/some_slug',
            'subscribe_me': True,
            'cloud_page': {'method': CreateMs365Method.FINALIZE_UPLOAD.value, 'upload_session': upload_session},
        },
    )

    assert response.status_code == 200
    pk = response.json()['id']
    page = Page.objects.get(pk=pk)
    cloud_page: CloudPage = CloudPage.objects.get(page=page)

    assert page.page_type == page.TYPES.CLOUD
    assert cloud_page.get_cloud_src()
