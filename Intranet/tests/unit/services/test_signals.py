from unittest import mock

from django.contrib.auth.models import ContentType, Permission

from plan.services.constants.permissions import CHANGE_SERVICE_TAG_CODENAME

from common import factories


def test_remove_service_tag_node(db):
    """При удалении ServiceTag удаляется его нода в IDM"""
    service_tag = factories.ServiceTagFactory()
    assert Permission.objects.filter(codename=CHANGE_SERVICE_TAG_CODENAME % service_tag.slug).count() == 1

    with mock.patch('plan.idm.nodes.RoleNodeManager') as RoleNodeManagerMock:
        service_tag.delete()

    assert RoleNodeManagerMock.delete.called_once()
    call_kwargs = RoleNodeManagerMock.delete.call_args.kwargs
    assert call_kwargs['system'] == 'abc-ext'
    assert call_kwargs['node_path'] == '/type/service_tags/service_tags_key/%s/' % service_tag.slug
    assert not Permission.objects.filter(codename=CHANGE_SERVICE_TAG_CODENAME % service_tag.slug).exists()
