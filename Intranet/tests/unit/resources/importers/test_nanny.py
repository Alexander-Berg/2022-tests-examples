import json

from mock import patch
import pytest

from plan.resources.models import Resource, ServiceResource
from plan.resources.tasks import sync_with_nanny
from plan.unistat.models import MetricsRecord
from common import factories
from utils import Response


pytestmark = [pytest.mark.usefixtures('robot'), pytest.mark.postgresql]


def test_import():
    service = factories.ServiceFactory(id=8888)
    other_service = factories.ServiceFactory(id=9999)
    nanny_type = factories.ResourceTypeFactory(code='nanny_service', has_automated_grant=True, import_link='')
    nanny_services = {
        "result": [
            {
                "info_attrs": {
                    "content": {}
                },
                "_id": 1,
            },
            {
                "info_attrs": {
                    "content": {
                        "abc_group": 8888,
                        "desc": "desc"
                    }
                },
                "_id": 2
            },
        ]
    }

    existing_resource = factories.ResourceFactory(
        type=nanny_type,
        external_id=2,
        name=2,
        link='/ui/#/services/catalog/2/'
    )
    existing_sr = factories.ServiceResourceFactory(
        resource=existing_resource,
        service=other_service,
        state=ServiceResource.GRANTED
    )

    with patch('plan.common.utils.http.Session.get') as get:
        get.side_effect = [Response(200, json.dumps(nanny_services)), Response(200, json.dumps({"result": []}))]
        sync_with_nanny()

    assert Resource.objects.filter(type__code='nanny_service').count() == 2

    existing_sr.refresh_from_db()
    assert existing_sr.state == ServiceResource.DEPRIVED

    new_sr = ServiceResource.objects.get(type__code='nanny_service', service=service)
    assert new_sr.state == ServiceResource.GRANTED
    assert new_sr.resource.attributes == {'description': 'desc'}

    metrics_record = MetricsRecord.objects.get()
    assert metrics_record.metrics == {
        'nanny_services_with_abc': 1,
        'nanny_services_without_abc': 1,
    }

    nanny_services = {
        "result": [
            {
                "info_attrs": {
                    "content": {}
                },
                "_id": 1,
            },
            {
                "info_attrs": {
                    "content": {
                        "abc_group": 8888,
                        "desc": "desc1"
                    }
                },
                "_id": 2
            },
        ]
    }

    with patch('plan.common.utils.http.Session.get') as get:
        get.side_effect = [Response(200, json.dumps(nanny_services)), Response(200, json.dumps({"result": []}))]
        sync_with_nanny()

    assert Resource.objects.filter(type__code='nanny_service').count() == 2

    sr = ServiceResource.objects.alive().get(type__code='nanny_service')
    assert sr.resource.attributes == {'description': 'desc1'}
