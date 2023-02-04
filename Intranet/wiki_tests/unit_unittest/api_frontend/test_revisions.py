
import datetime

from django.conf import settings
from ujson import loads

from wiki.notifications.models import PageEvent
from wiki.utils import timezone
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase
from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import BaseGridsTest

GRID_STRUCTURE = """
{
  "title" : "List of conferences",
  "width" : "100%",
  "sorting" : [],
  "fields" : [
    {
      "name" : "text",
      "title" : "text",
      "type": "string",
      "required": true
    },
    {
      "name" : "number",
      "type": "number",
      "title" : "Number of films"
    }
  ]
}
"""


class APIRevisionsTest(BaseApiTestCase):
    def setUp(self):
        super(APIRevisionsTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')

    def test_show_revision(self):
        page = self.create_page(tag='Страница1', body='исходная ревизия')
        rev_id = page.revision_set.order_by('-id')[0].id

        request_url = '{api_url}/{page_supertag}/.revisions/{id}'.format(
            api_url=self.api_url, page_supertag=page.supertag, id=rev_id
        )
        assert_queries = 51 if not settings.WIKI_CODE == 'wiki' else 9
        with self.assertNumQueries(assert_queries):
            response = self.client.get(request_url)
        self.assertEqual(200, response.status_code)

        data = loads(response.content)['data']

        self.assertEqual(data['id'], rev_id)

    def test_unexistent_revision(self):
        page = self.create_page(tag='Страница1', body='исходная ревизия')

        request_url = '{api_url}/{page_supertag}/.revisions/{id}'.format(
            api_url=self.api_url, page_supertag=page.supertag, id='potato'
        )
        response = self.client.get(request_url)

        self.assertEqual(404, response.status_code)

    def test_show_revisions(self):
        from wiki.pages.models import Revision

        page = self.create_page(tag='Страница2', body='исходная ревизия')
        early_revision_id = page.revision_set.order_by('-id')[0].id

        page = self.create_page(id=page.id, body='вторая ревизия')
        late_revision_id = page.revision_set.order_by('-id')[0].id

        qs = Revision.objects.exclude(id__in=(early_revision_id, late_revision_id))
        qs.update(created_at=datetime.datetime(2013, 10, 1, 1, 1, 1))

        Revision.objects.filter(id=early_revision_id).update(created_at=datetime.datetime(2013, 11, 15, 15, 0o6, 00))
        Revision.objects.filter(id=early_revision_id).update(created_at=datetime.datetime(2013, 11, 15, 15, 0o6, 00))

        request_url = '{api_url}/{page_supertag}/.revisions'.format(api_url=self.api_url, page_supertag=page.supertag)
        with self.assertNumQueries(10 if settings.IS_INTRANET else 51):
            response = self.client.get(request_url)
        self.assertEqual(200, response.status_code)

        data = loads(response.content)['data']

        self.assertEqual(data['total'], 2)

        self.assertEqual(data['data'][0]['id'], late_revision_id)
        self.assertEqual(data['data'][1]['id'], early_revision_id)

    def test_show_events(self):
        page = self.create_page(tag='Страница3', body='исходная ревизия')
        rev0_id = page.revision_set.order_by('-id')[0].id

        PageEvent(
            page=page,
            timeout=timezone.now(),
            author=self.user_thasonic,
            event_type=PageEvent.EVENT_TYPES.create,
            meta={'revision_id': rev0_id},
        ).save()

        page = self.create_page(id=page.id, body='вторая ревизия')
        rev1_id = page.revision_set.order_by('-id')[0].id

        PageEvent(
            page=page,
            timeout=timezone.now(),
            author=self.user_thasonic,
            event_type=PageEvent.EVENT_TYPES.edit,
            meta={'revision_id': rev1_id},
        ).save()

        PageEvent(
            page=page,
            timeout=timezone.now(),
            author=self.user_thasonic,
            event_type=PageEvent.EVENT_TYPES.watch,
            meta={},
        ).save()

        meta = {
            'is_cluster': True,
            'pages': [page.supertag],
            'comment': 'My comments',
            'subscribed_user_id': self.user_chapson.id,
        }
        PageEvent(
            page=page,
            timeout=timezone.now(),
            author=self.user_thasonic,
            event_type=PageEvent.EVENT_TYPES.subscribe_other_user,
            meta=meta,
        ).save()

        request_url = '{api_url}/{page_supertag}/.events'.format(api_url=self.api_url, page_supertag=page.supertag)

        response = self.client.get(request_url)
        self.assertEqual(200, response.status_code)

        data = loads(response.content)['data']
        self.assertEqual(data['total'], 4)

        data_data = data['data']
        self.assertEqual(data_data[0]['event_type'], 'subscribe_other_user')
        self.assertEqual(data_data[1]['event_type'], 'watch')
        self.assertEqual(data_data[2]['event_type'], 'edit')
        self.assertEqual(data_data[3]['event_type'], 'create')

        self.assertEqual(data_data[0]['additional_fields']['subscribed_user_login'], self.user_chapson.username)
        self.assertEqual(data_data[1]['revision'], None)
        self.assertEqual(data_data[2]['revision']['id'], rev1_id)
        self.assertEqual(data_data[3]['revision']['id'], rev0_id)

    def test_show_events_by_event_ids(self):
        page = self.create_page(tag='Страница4', body='исходная ревизия')
        rev0_id = page.revision_set.order_by('-id')[0].id

        PageEvent(
            page=page,
            timeout=timezone.now(),
            author=self.user_thasonic,
            event_type=PageEvent.EVENT_TYPES.create,
            meta={'revision_id': rev0_id},
        ).save()

        page = self.create_page(id=page.id, body='вторая ревизия')
        rev1_id = page.revision_set.order_by('-id')[0].id

        PageEvent(
            page=page,
            timeout=timezone.now(),
            author=self.user_thasonic,
            event_type=PageEvent.EVENT_TYPES.edit,
            meta={'revision_id': rev1_id},
        ).save()

        PageEvent(
            page=page,
            timeout=timezone.now(),
            author=self.user_thasonic,
            event_type=PageEvent.EVENT_TYPES.watch,
            meta={},
        ).save()
        request_url = '{api_url}/{page_supertag}/.events?event_types={types}'.format(
            api_url=self.api_url, page_supertag=page.supertag, types='1,3'
        )
        r = self.client.get(request_url)
        self.assertEqual(200, r.status_code)

        data = loads(r.content)['data']

        self.assertEqual(data['total'], 2)

        self.assertEqual(data['data'][0]['event_type'], 'edit')
        self.assertEqual(data['data'][1]['event_type'], 'create')

        self.assertEqual(data['data'][0]['revision']['id'], rev1_id)
        self.assertEqual(data['data'][1]['revision']['id'], rev0_id)

    def test_show_events_by_event_names(self):
        page = self.create_page(tag='Страница4', body='исходная ревизия')
        rev0_id = page.revision_set.order_by('-id')[0].id

        PageEvent(
            page=page,
            timeout=timezone.now(),
            author=self.user_thasonic,
            event_type=PageEvent.EVENT_TYPES.create,
            meta={'revision_id': rev0_id},
        ).save()

        page = self.create_page(id=page.id, body='вторая ревизия')
        rev1_id = page.revision_set.order_by('-id')[0].id

        PageEvent(
            page=page,
            timeout=timezone.now(),
            author=self.user_thasonic,
            event_type=PageEvent.EVENT_TYPES.edit,
            meta={'revision_id': rev1_id},
        ).save()

        PageEvent(
            page=page,
            timeout=timezone.now(),
            author=self.user_thasonic,
            event_type=PageEvent.EVENT_TYPES.watch,
            meta={},
        ).save()

        request_url = '{api_url}/{page_supertag}/.events?event_types={types}'.format(
            api_url=self.api_url, page_supertag=page.supertag, types='edit,create'
        )
        r = self.client.get(request_url)
        self.assertEqual(200, r.status_code)

        data = loads(r.content)['data']

        self.assertEqual(data['total'], 2)

        self.assertEqual(data['data'][0]['event_type'], 'edit')
        self.assertEqual(data['data'][1]['event_type'], 'create')

        self.assertEqual(data['data'][0]['revision']['id'], rev1_id)
        self.assertEqual(data['data'][1]['revision']['id'], rev0_id)

    def test_show_events_by_event_ids_with_empty_result(self):
        page = self.create_page(tag='Страница4', body='исходная ревизия')
        rev0_id = page.revision_set.order_by('-id')[0].id

        PageEvent(
            page=page,
            timeout=timezone.now(),
            author=self.user_thasonic,
            event_type=PageEvent.EVENT_TYPES.create,
            meta={'revision_id': rev0_id},
        ).save()

        page = self.create_page(id=page.id, body='вторая ревизия')
        rev1_id = page.revision_set.order_by('-id')[0].id

        PageEvent(
            page=page,
            timeout=timezone.now(),
            author=self.user_thasonic,
            event_type=PageEvent.EVENT_TYPES.edit,
            meta={'revision_id': rev1_id},
        ).save()

        PageEvent(
            page=page,
            timeout=timezone.now(),
            author=self.user_thasonic,
            event_type=PageEvent.EVENT_TYPES.watch,
            meta={},
        ).save()

        request_url = '{api_url}/{page_supertag}/.events?event_types={types}'.format(
            api_url=self.api_url, page_supertag=page.supertag, types='6,7'
        )
        r = self.client.get(request_url)
        self.assertEqual(200, r.status_code)

        data = loads(r.content)['data']

        self.assertEqual(data['total'], 0)
        self.assertEqual(0, len(data['data']))


class APIGridRevisionsTest(BaseGridsTest):
    def _prepare_grid(self, supertag, structure):
        self._create_grid(supertag, structure, self.user_thasonic)
        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        version = loads(response.content)['data']['version']
        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='-1',
                            data=dict(
                                text='Killa Gorilla',
                                number=10,
                            ),
                        )
                    )
                ],
            ),
        )

        return loads(response.content)['data']['version']

    def test_show_revision(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag, GRID_STRUCTURE)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_row=dict(
                            id='1',
                            data=dict(
                                text='We\'re the Millers',
                                number=4,
                            ),
                        )
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        assert_queries = 11 if not settings.WIKI_CODE == 'wiki' else 9
        with self.assertNumQueries(assert_queries):
            response = self.client.get('/_api/frontend/{0}/.grid/revisions/{1}'.format(supertag, version))
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']

        assert data['rows'][0][0]['raw'] == 'Killa Gorilla'
        assert data['rows'][0][1]['raw'] == '10'
