import json

import mock
import pytest
from django.db.models import Q
from waffle.testutils import override_switch

from wiki.api_core.waffle_switches import E2E_TESTS
from wiki.pages.models import Page

SRC = 'root/a'


@mock.patch('wiki.e2e_testing.views.prepare.E2E_RESERVED', SRC)
@pytest.mark.django_db
def test_prepare_cleanup(client, wiki_users, page_cluster):
    with override_switch(E2E_TESTS, active=True):
        with mock.patch('wiki.e2e_testing.utils.track_links'):
            client.login(wiki_users.thasonic)

            response = client.post('/_api/svc/e2e/.prepare', data={})
            assert 200 == response.status_code

            content = json.loads(response.content)
            run_id = content['data']['run_id']

            mds = []
            for p in Page.objects.filter(Q(supertag__startswith=SRC + '/') | Q(supertag=SRC)):
                suffix: str = p.supertag[len(SRC) :]
                estimated_supertag = f'e2e/{run_id}{suffix}'
                assert Page.objects.filter(supertag=estimated_supertag).exists()
                mds.append(Page.objects.get(supertag=estimated_supertag).mds_storage_id.name)

            with mock.patch('wiki.e2e_testing.utils.MDS_STORAGE.delete'):
                response = client.post('/_api/svc/e2e/.cleanup', data={'run_id': run_id})
                assert 200 == response.status_code

            for p in Page.objects.filter(Q(supertag__startswith=SRC + '/') | Q(supertag=SRC)):
                suffix: str = p.supertag[len(SRC) :]
                estimated_supertag = f'e2e/{run_id}{suffix}'
                assert not Page.objects.filter(supertag=estimated_supertag).exists()
