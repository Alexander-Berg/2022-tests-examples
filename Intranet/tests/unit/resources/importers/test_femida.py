import json

from mock import patch
import pytest

from plan.resources.models import Resource, ServiceResource
from plan.resources.tasks import sync_with_femida
from common import factories
from utils import Response


pytestmark = [pytest.mark.usefixtures('robot'), pytest.mark.postgresql]


def test_import():
    factories.ServiceFactory(id=7777)
    factories.ServiceFactory(id=6666)
    femida_type = factories.ResourceTypeFactory(code='femida_vacancy', has_automated_grant=True, import_link='')

    other_service = factories.ServiceFactory(id=2222)
    existing_resource = factories.ResourceFactory(
        type=femida_type,
        external_id=1,
        link='/vacancies/publications/1/'
    )
    existing_sr = factories.ServiceResourceFactory(
        resource=existing_resource,
        service=other_service,
        state=ServiceResource.GRANTED
    )

    vacancies = {
        'results': [
            {
                'abc_services': [
                    {
                        'id': 7777
                    },
                    {
                        'id': 6666
                    }
                ],
                'id': 1,
                'publication_title': 'title_1'
            },
            {
                'abc_services': [],
                'id': 2,
                'publication_title': 'title_2'
            },
        ]
    }
    with patch('plan.common.utils.http.Session.get') as get:
        get.side_effect = [Response(200, json.dumps(vacancies)), Response(200, json.dumps({"result": []}))]
        sync_with_femida()

    existing_sr.refresh_from_db()
    assert existing_sr.state == ServiceResource.DEPRIVED

    assert Resource.objects.count() == 2
    assert ServiceResource.objects.alive().count() == 2
    sr1 = ServiceResource.objects.get(type__code='femida_vacancy', service_id=7777)
    sr2 = ServiceResource.objects.get(type__code='femida_vacancy', service_id=6666)
    assert sr1.state == sr2.state == ServiceResource.GRANTED
    assert sr1.resource.attributes == sr2.resource.attributes == {}
    assert sr1.resource.name == sr2.resource.name == 'title_1'
    assert sr1.resource.external_id == sr2.resource.external_id == '1'
