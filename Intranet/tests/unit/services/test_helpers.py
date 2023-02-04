import pytest

from plan.common.person import Person
from plan.denormalization.tasks import check_model_denormalized_fields
from plan.services import tasks, helpers

from common import factories


pytestmark = pytest.mark.django_db


def test_pre_move_node_builder(superuser, move_services_subtasks):
    old_meta = factories.ServiceFactory(parent=None)
    new_meta = factories.ServiceFactory(parent=None)

    grandparent = factories.ServiceFactory(parent=old_meta)
    parent = factories.ServiceFactory(parent=grandparent)
    service = factories.ServiceFactory(parent=parent)
    child = factories.ServiceFactory(parent=service)

    check_model_denormalized_fields('services.Service')

    family = [grandparent, parent, service, child]

    move_request = factories.ServiceMoveRequestFactory(
        service=grandparent,
        source=old_meta,
        destination=new_meta
    )

    move_request._approve_transition(Person(superuser))
    move_request.save()

    tasks.move_service(move_request.id)

    for member in family:
        member.refresh_from_db()

    assert grandparent.parent == new_meta

    nodes = helpers.generate_pre_move_service_nodes(move_request)

    compare = {
        grandparent: '/{}/'.format(
            '/'.join(('services', old_meta.slug, grandparent.slug))),
        parent: '/{}/'.format(
            '/'.join(('services', old_meta.slug, grandparent.slug, parent.slug))
        ),
        service: '/{}/'.format(
            '/'.join(('services', old_meta.slug, grandparent.slug, parent.slug, service.slug))
        ),
        child: '/{}/'.format(
            '/'.join(('services', old_meta.slug, grandparent.slug, parent.slug, service.slug, child.slug))
        )
    }

    for service, node in nodes:
        assert node.value_path == compare[service]
