import json

from django.conf import settings

from mock import patch
import pytest

from plan.resources.models import Resource, ServiceResource
from plan.resources.tasks import sync_with_qloud
from plan.unistat.models import MetricsRecord
from common import factories
from utils import Response


pytestmark = [pytest.mark.usefixtures('robot'), pytest.mark.postgresql]


@pytest.mark.parametrize('resource_deprived', [True, False])
def test_import(resource_deprived):
    service = factories.ServiceFactory(id=9999)
    project_resource_type = factories.ResourceTypeFactory(code='prj', has_automated_grant=True, import_link='')
    app_resource_type = factories.ResourceTypeFactory(code='app', has_automated_grant=True, import_link='')
    settings.QLOUD_IMPORT_SETTINGS = [('prj', 'app')]
    if resource_deprived:
        resource = factories.ResourceFactory(type=project_resource_type, name='begemot', link='/projects/begemot')
        factories.ServiceResourceFactory(resource=resource, service=service, state=ServiceResource.DEPRIVED)

    other_service = factories.ServiceFactory(id=1111)
    existing_resource = factories.ResourceFactory(
        type=app_resource_type,
        name='name1',
        external_id='id1',
        link='/projects/begemot/name1'
    )
    existing_sr = factories.ServiceResourceFactory(
        service=other_service,
        resource=existing_resource,
        state=ServiceResource.GRANTED
    )

    projects = [
        {
            "projectName": "begemot",
            "metaInfo": {
                "abcId": 9999
            }
        }
    ]
    applications = {
        "applications": [
            {
                "name": "name1",
                "objectId": "id1",
                "projectName": "begemot",
                "metaInfo": {
                    "abcId": 9999
                }
            },
            {
                "name": "name2",
                "objectId": "id2",
                "projectName": "begemot",
                "metaInfo": {}
            }
        ]
    }
    with patch('plan.common.utils.http.Session.get') as get:
        get.side_effect = [Response(200, json.dumps(projects)), Response(200, json.dumps(applications))]
        sync_with_qloud()

    existing_sr.refresh_from_db()
    assert existing_sr.state == ServiceResource.DEPRIVED

    assert Resource.objects.filter(type__code='app').count() == 2
    app = ServiceResource.objects.alive().get(type__code='app')
    assert app.state == ServiceResource.GRANTED
    assert Resource.objects.filter(type__code='prj').count() == 1
    project = ServiceResource.objects.alive().get(type__code='prj')
    assert project.state == ServiceResource.GRANTED

    metrics_record = MetricsRecord.objects.get()
    assert metrics_record.metrics == {
        'qloud_apps_with_abc': 1,
        'qloud_apps_without_abc': 1,
        'qloud_projects_with_abc': 1,
        'qloud_projects_without_abc': 0
    }

    projects = [
        {
            "projectName": "xxx",
            "metaInfo": {
                "abcId": 9999
            }
        }
    ]
    applications = {
        "applications": [
            {
                "name": "name2",
                "objectId": "id2",
                "projectName": "begemot",
                "metaInfo": {}
            }
        ]
    }
    with patch('plan.common.utils.http.Session.get') as get:
        get.side_effect = [Response(200, json.dumps(projects)), Response(200, json.dumps(applications))]
        sync_with_qloud()

    app.refresh_from_db()
    project.refresh_from_db()
    assert app.state == ServiceResource.DEPRIVED
    assert project.state == ServiceResource.DEPRIVED
