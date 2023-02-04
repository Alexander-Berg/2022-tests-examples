from datetime import datetime
import pytz
import unittest

from django.urls import reverse
from django.test import override_settings
from ujson import loads
from waffle.testutils import override_switch

from wiki.api_core.waffle_switches import MS365_INTEGRATION
from wiki.integrations.ms.consts import DocCreationSourceType, Ms365DocType
from wiki.integrations.ms.exceptions import Ms365BadLink, Ms365BadUploadSession
from wiki.integrations.ms.serializers import CreatePageRequest
from wiki.integrations.ms.stub_client import DocRetrieverStubClient
from wiki.integrations.ms.tasks.sync_lastmodified import SyncLastModified
from wiki.integrations.ms.upload_intent import UploadIntentStorage
from wiki.pages.models import CloudPage, Page
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class MsApiTest(BaseTestCase):
    def setUp(self):
        super(MsApiTest, self).setUp()
        self.setUsers()
        self.setGroupMembers()
        self.doc_retriever_client = DocRetrieverStubClient()

    def test_parse_urls(self):
        url_common_share = """https://yandexteam.sharepoint.com/:x:/r/_layouts/15/Doc.aspx?sourcedoc=%7B848FC5BE-39E7-4455-871B-0459B0DBE535%7D&file=Реестр%20NDA%20%20юрид.лица.xlsx&action=default&mobileredirect=true&DefaultItemOpen=1"""  # noqa
        url_word = """https://yandexteam-my.sharepoint.com/:w:/r/personal/neofelis_yandex-team_ru/_layouts/15/Doc.aspx?sourcedoc=%7BEACA1432-469E-45AC-B897-EC30FA86BAA5%7D&file=Document.docx&wdOrigin=OFFICECOM-WEB.MAIN.REC&action=default&mobileredirect=true"""  # noqa
        url_excel = """https://yandexteam-my.sharepoint.com/:x:/r/personal/sofiushko_yandex-team_ru/_layouts/15/Doc.aspx?sourcedoc=%7B05608CD6-E653-47E3-BE7A-F08EFFFA12FE%7D&file=moskvaklinikiobshhijj-1%20(1).xlsx&wdOrigin=OFFICECOM-WEB.START.MRU&action=default&mobileredirect=true"""  # noqa
        url_pptx = """https://yandexteam-my.sharepoint.com/:p:/r/personal/neofelis_yandex-team_ru/_layouts/15/Doc.aspx?sourcedoc=%7B08F3AD5A-F65D-4A4E-B6E3-5BA5A7CB81B4%7D&file=empty.pptx&wdOrigin=OFFICECOM-WEB.MAIN.MRU&action=edit&mobileredirect=true"""  # noqa
        url_future = """https://yandexteam-my.sharepoint.com/:w:/r/personal/neofelis_yandex-team_ru/_layouts/10/Doc.aspx?sourcedoc=%7BEACA1432-469E-45AC-B897-EC30FA86BAA5%7D&file=Document.docx&wdOrigin=OFFICECOM-WEB.MAIN.REC&action=default&mobileredirect=true"""  # noqa
        url_no_sourcedoc = """"https://yandexteam-my.sharepoint.com/:w:/r/personal/neofelis_yandex-team_ru/_layouts/10/Doc.aspx?file=Document.docx&wdOrigin=OFFICECOM-WEB.MAIN.REC&action=default&mobileredirect=true"""  # noqa
        url_sharelink = 'https://yandexteam-my.sharepoint.com/:p:/g/personal/neofelis_yandex-team_ru/EVqt8whd9k5KtuNbpafLgbQBHl-FQJ412XKXTkNsBrAvNg?e=yKlbQI'  # noqa
        url_pptx_from_dev_sp = 'https://yandexteam.sharepoint.com/:p:/r/sites/wiki-dev/_layouts/15/Doc.aspx?sourcedoc=%7BB987DCD0-FDBF-4E88-8EEE-63021646048B%7D&file=Demo_Sprint_Kholodoque.pptx&action=edit&mobileredirect=true'  # noqa

        self.doc_retriever_client.resolve_url(url_word)
        self.doc_retriever_client.resolve_url(url_common_share)
        self.doc_retriever_client.resolve_url(url_excel)
        self.doc_retriever_client.resolve_url(url_pptx)
        self.doc_retriever_client.resolve_url(url_future)
        self.doc_retriever_client.resolve_url(url_pptx_from_dev_sp)

        with self.assertRaises(Ms365BadLink):
            self.doc_retriever_client.resolve_url('https://st.yandex-team.ru/dashboard/38243#')

        with self.assertRaises(Ms365BadLink):
            self.doc_retriever_client.resolve_url(url_no_sourcedoc)

        with self.assertRaises(Ms365BadLink):
            self.doc_retriever_client.resolve_url(url_sharelink)

    def test_feature_disabled(self):
        with override_switch(MS365_INTEGRATION, active=False):
            self.login('neofelis')
            response = self.client.post(reverse('frontend:ms365:svc_acc'), content_type='application/json')
            self.assertEqual(response.status_code, 409)

    def test_blackhole(self):
        fsize = 10000
        length = 10

        self.login('neofelis')
        response = self.client.put(
            reverse('frontend:ms365:put_blackhole'),
            content_type='application/octet-stream',
            data=b'0' * length,
            HTTP_CONTENT_RANGE='bytes 0-%s/%s' % (fsize - 1, fsize),
        )
        assert response.status_code == 202

        response = self.client.put(
            reverse('frontend:ms365:put_blackhole'),
            content_type='application/octet-stream',
            data=b'0' * length,
            HTTP_CONTENT_RANGE='bytes 0-%s/%s' % (fsize, fsize),
        )
        assert response.status_code == 400


class CreatePageRequestTest(unittest.TestCase):
    @override_settings(DOC_RETRIEVER_USE_STUB=True)
    def test_validate_request_params(self):
        serializer = CreatePageRequest(
            data={
                'title': 'title',
                'supertag': 'supertag',
                'source': DocCreationSourceType.EMPTY_DOC,
                'options': {'doctype': Ms365DocType.DOCX},
            }
        )
        self.assertEqual(serializer.is_valid(), True)

        serializer = CreatePageRequest(
            data={'title': 'title', 'supertag': 'supertag', 'source': '----', 'options': {'doctype': '-----'}}
        )
        self.assertEqual(serializer.is_valid(), False)
        self.assertEqual(set(serializer.errors.keys()), set(['source', 'options']))

        serializer = CreatePageRequest(data={})
        self.assertEqual(serializer.is_valid(), False)
        self.assertEqual(set(serializer.errors.keys()), set(['title', 'supertag', 'options', 'source']))

        serializer = CreatePageRequest(
            data={
                'title': 'title',
                'supertag': 'supertag',
                'source': DocCreationSourceType.FROM_URL,
                'options': {'mime': 'mimetype', 'doctype': Ms365DocType.DOCX},
            }
        )
        self.assertEqual(serializer.is_valid(), False)
        self.assertEqual(set(serializer.errors.keys()), set(['options']))

        serializer = CreatePageRequest(
            data={
                'title': 'title',
                'supertag': 'supertag',
                'source': DocCreationSourceType.UPLOAD_DOC,
                'options': {'url': 'http://abc.com', 'doctype': Ms365DocType.DOCX},
            }
        )
        self.assertEqual(serializer.is_valid(), False)
        self.assertEqual(set(serializer.errors.keys()), set(['options']))

        serializer = CreatePageRequest(
            data={
                'title': 'title',
                'supertag': 'supertag',
                'source': DocCreationSourceType.EMPTY_DOC,
                'options': {'url': 'http://abc.com', 'mime': 'mimetype'},
            }
        )
        self.assertEqual(serializer.is_valid(), False)
        self.assertEqual(set(serializer.errors.keys()), set(['options']))


DOC_URL = 'https://yandexteam-my.sharepoint.com/:w:/r/personal/neofelis_yandex-team_ru/_layouts/15/Doc.aspx?sourcedoc=%7BEACA1432-469E-45AC-B897-EC30FA86BAA5%7D&file=Document.docx&wdOrigin=OFFICECOM-WEB.MAIN.REC&action=default&mobileredirect=true'  # noqa
IFRAME_URL = """<iframe src="https://yandexteam-my.sharepoint.com/personal/neofelis_yandex-team_ru/_layouts/15/Doc.aspx?sourcedoc={eaca1432-469e-45ac-b897-ec30fa86baa5}&amp;action=embedview&amp;wdStartOn=1" width="476px" height="288px" frameborder="0">This is an embedded <a target="_blank" href="https://office.com">Microsoft Office</a> document, powered by <a target="_blank" href="https://office.com/webapps">Office</a>.</iframe>"""  # noqa
IFRAME_SRC = """https://yandexteam-my.sharepoint.com/personal/neofelis_yandex-team_ru/_layouts/15/Doc.aspx?sourcedoc={eaca1432-469e-45ac-b897-ec30fa86baa5}&amp;action=embedview&amp;wdStartOn=1"""  # noqa


class CreateCloudPageViewTest(BaseApiTestCase):
    def setUp(self):
        super(CreateCloudPageViewTest, self).setUp()
        self.setUsers()
        self.setGroupMembers()
        self.login('elisei')
        self.doc_retriever_client = DocRetrieverStubClient()

    def test_create_bad_supertag(self):
        response = self.client.post(
            reverse('frontend:ms365:create_page'),
            content_type='application/json',
            data={
                'title': 'title',
                'supertag': ';,!',
                'source': DocCreationSourceType.FROM_URL.value,
                'options': {'url': DOC_URL},
            },
        )

        self.assertEqual(response.status_code, 409)

    def get_page_data(self, supertag):
        response = self.client.get(
            '{}/{}'.format(self.api_url, supertag),
        )
        return loads(response.content)

    def create_cloud_page(self, supertag, title, source, options):
        response = self.client.post(
            reverse('frontend:ms365:create_page'),
            content_type='application/json',
            data={'title': title, 'supertag': supertag, 'source': source, 'options': options},
        )
        self.assertEqual(response.status_code, 200)
        response_data = loads(response.content)
        return response_data

    @override_settings(DOC_RETRIEVER_USE_STUB=True)
    def test_create_page_from_url(self):
        supertag = 'users/elisei/testpage'
        title = 'new page from url'
        response_data = self.create_cloud_page(
            supertag, title, source=DocCreationSourceType.FROM_URL.value, options={'url': DOC_URL}
        )
        self.assertEqual(response_data['data']['supertag'], supertag)

        page_data = self.get_page_data(supertag)
        self.assertEqual(page_data['data']['page_type'], Page.TYPES[Page.TYPES.CLOUD])
        self.assertEqual(page_data['data']['title'], title)

    @override_settings(DOC_RETRIEVER_USE_STUB=True)
    def test_create_page_from_iframe(self):
        supertag = 'users/elisei/testpage'
        title = 'new page from iframe'

        response_data = self.create_cloud_page(
            supertag, title, source=DocCreationSourceType.FROM_URL.value, options={'url': IFRAME_URL}
        )
        self.assertEqual(response_data['data']['supertag'], supertag)

        cloud = CloudPage.objects.all()[0]
        self.assertEqual(cloud.presentation_params['wdStartOn'], '1')
        self.assertEqual(
            cloud.cloud_src['embedding'],
            {
                'domain': 'yandexteam-my.sharepoint.com',
                'namespace': '/personal/neofelis_yandex-team_ru/',
                'sourcedoc': '{eaca1432-469e-45ac-b897-ec30fa86baa5}',
            },
        )

    @override_settings(DOC_RETRIEVER_USE_STUB=True)
    def test_check_access(self):
        response = self.client.post(
            reverse('frontend:ms365:check_access'),
            content_type='application/json',
            data={
                'iframe_src': IFRAME_SRC,
            },
        )

        self.assertEqual(response.status_code, 200)
        response_data = loads(response.content)
        self.assertEqual(response_data['data']['access_level'], 'read')

    @override_settings(DOC_RETRIEVER_USE_STUB=True)
    def test_create_page_empty_doc(self):
        supertag = 'users/elisei/testpage'
        title = 'new empty page'

        response_data = self.create_cloud_page(
            supertag, title, source=DocCreationSourceType.EMPTY_DOC.value, options={'doctype': Ms365DocType.DOCX.value}
        )
        self.assertEqual(response_data['data']['supertag'], supertag)

        page_data = self.get_page_data(supertag)
        self.assertEqual(page_data['data']['page_type'], Page.TYPES[Page.TYPES.CLOUD])
        self.assertEqual(page_data['data']['title'], title)

    @override_settings(DOC_RETRIEVER_USE_STUB=True)
    def test_create_page_empty_doc__colon(self):
        invalid_slug, valid_slug = 'sl::ug', 'slug'
        title = 'new empty page'

        response_data = self.create_cloud_page(
            invalid_slug,
            title,
            source=DocCreationSourceType.EMPTY_DOC.value,
            options={'doctype': Ms365DocType.DOCX.value},
        )
        self.assertEqual(response_data['data']['supertag'], valid_slug)

        cloud_page = CloudPage.objects.get(page__supertag=valid_slug)
        self.assertEqual(cloud_page.get_cloud_src().document.filename, f'{valid_slug}.{Ms365DocType.DOCX}')

    @override_settings(DOC_RETRIEVER_USE_STUB=True)
    def test_intent_storage(self):
        d = {
            'upload_to': 'bar',
            'signature': 'leet',
            'data': {
                'title': 'new empty page',
                'supertag': 'users/elisei/testpage',
                'source': DocCreationSourceType.EMPTY_DOC.value,
                'options': {'doctype': Ms365DocType.DOCX.value},
            },
        }
        guid = UploadIntentStorage.store_intent(d)

        ret_val = UploadIntentStorage.load_intent(guid)
        self.assertEqual(d, ret_val)

        UploadIntentStorage.delete_intent(guid)

        with self.assertRaises(Ms365BadUploadSession):
            UploadIntentStorage.load_intent(guid)

    @override_settings(DOC_RETRIEVER_USE_STUB=True)
    def test_upload_file(self):
        supertag = 'users/elisei/testpage'
        title = 'upload file test page'
        response_data = self.create_cloud_page(
            supertag,
            title,
            source=DocCreationSourceType.UPLOAD_DOC.value,
            options={'mime': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'},
        )

        session_guid = response_data['data']['upload_session']

        response = self.client.post(
            reverse('frontend:ms365:finalize_upload'),
            content_type='application/json',
            data={'upload_session': session_guid},
        )
        response_data = loads(response.content)
        self.assertEqual(response_data['data']['supertag'], supertag)


class SyncLastModifiedTest(BaseTestCase):
    @override_settings(DOC_RETRIEVER_USE_STUB=True)
    def test_sync_lastmodified(self):
        page = self.create_page(
            supertag='cloud_page',
            title='Cloud page',
            modified_at=datetime.fromtimestamp(42),
            modified_at_for_index=datetime.fromtimestamp(42),
            status=1,
            page_type=Page.TYPES.CLOUD,
        )

        cloud_src = {
            'document': {'type': 'docx'},
            'driveitem': {'drive_id': 'xxx', 'item_id': 'zzz'},
        }
        CloudPage.objects.create(
            page=page,
            cloud_src=cloud_src,
        )

        SyncLastModified().run()
        page.refresh_from_db()

        modified_at = datetime(2020, 9, 11, 9, 11, 34, tzinfo=pytz.utc)
        self.assertEqual(page.modified_at, modified_at)
        self.assertEqual(page.modified_at_for_index, modified_at)
