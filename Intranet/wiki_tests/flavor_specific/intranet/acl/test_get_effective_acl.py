import json
from unittest import mock

import pytest
from waffle.testutils import override_switch
from django.urls import reverse
from wiki import access as wiki_access
from wiki.api_core.waffle_switches import ENABLE_ACL_MANAGEMENT
from wiki.integrations.consts import Ms365AclManagementType
from wiki.integrations.ms.consts import DocCreationSourceType, Ms365DocType
from wiki.integrations.ms.stub_client import DocRetrieverStubClient
from wiki.pages.models import CloudPage

pytestmark = [
    pytest.mark.django_db,
]


def test_effective_acl_owner_only(client, cloud_page_cluster, groups, wiki_users, add_user_to_group):
    root = cloud_page_cluster['root']
    root_a = cloud_page_cluster['root/a']

    add_user_to_group(groups.root_group, wiki_users.chapson)
    add_user_to_group(groups.side_group, wiki_users.kolomeetz)
    add_user_to_group(groups.child_group, wiki_users.asm)

    # автор у всех страниц - thasonic, доступ будет только для выделенных людей
    wiki_access.set_access_nopanic(
        root,
        wiki_access.TYPES.RESTRICTED,
        wiki_users.thasonic,
        staff_models=[
            wiki_users.volozh.staff,
        ],
        groups=[groups.side_group, groups.root_group],
    )

    wiki_access.set_access_nopanic(root_a, wiki_access.TYPES.INHERITED, wiki_users.thasonic)
    client.login('thasonic')
    response = client.post('/_api/svc/acl/.get_effective_acl', {'supertags': ['root', 'root/a']})

    data = json.loads(response.content)['data']

    # ['root/a', 'root/b', 'root/a/aa', 'root/a/ab']

    assert data['root/a']['acl']['acl_type'] == 'inherited'
    assert data['root/a']['acl']['document_id'] is not None
    assert data['root/a']['acl']['inherits_from'] == 'root'
    assert data['root/a']['acl']['owners'] == [wiki_users.thasonic.staff.id]

    assert data['root/a']['inherits_from']['acl_type'] == 'custom'
    assert set(data['root/a']['inherits_from']['group_ids']) == {
        groups.side_group.id,
        groups.root_group.id,
    }
    assert data['root/a']['inherits_from']['users'] == [wiki_users.volozh.staff.id]
    assert data['root/a']['inherits_from']['owners'] == [wiki_users.thasonic.staff.id]


DOC_URL = 'https://yandexteam-my.sharepoint.com/:w:/r/personal/neofelis_yandex-team_ru/_layouts/15/Doc.aspx?sourcedoc=%7BEACA1432-469E-45AC-B897-EC30FA86BAA5%7D&file=Document.docx&wdOrigin=OFFICECOM-WEB.MAIN.REC&action=default&mobileredirect=true'  # noqa


def test_cloud_page_creation_must_call_provision(client, cloud_page_cluster, settings):
    settings.DOC_RETRIEVER_USE_STUB = True
    client.login('thasonic')
    with override_switch(ENABLE_ACL_MANAGEMENT, active=True):
        with mock.patch.object(DocRetrieverStubClient, 'provision_page') as m:
            response = client.post(
                reverse('frontend:ms365:create_page'),
                content_type='application/json',
                data={
                    'title': 'test',
                    'supertag': 'test/doc',
                    'source': DocCreationSourceType.EMPTY_DOC.value,
                    'options': {'doctype': Ms365DocType.DOCX.value},
                },
            )
            assert response.status_code == 200

            response = client.post(
                reverse('frontend:ms365:create_page'),
                content_type='application/json',
                data={
                    'title': 'test',
                    'supertag': 'personalms/doc',
                    'source': DocCreationSourceType.FROM_URL.value,
                    'options': {'url': DOC_URL},
                },
            )

            # провижн должен быть только для страницы которая живет в нашем шейрпойнте
            assert m.call_count == 1
            assert 'test/doc' in m.call_args[0][0]

            assert response.status_code == 200

    cloud = CloudPage.objects.get(page__supertag='test/doc')
    cloud_2 = CloudPage.objects.get(page__supertag='personalms/doc')

    assert cloud.acl_management == Ms365AclManagementType.WIKI
    assert cloud_2.acl_management == Ms365AclManagementType.UNMANAGED
