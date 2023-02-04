import pytest

from plan.denormalization.check import check_obj_with_denormalized_fields
from plan.denormalization.tasks import check_denormalized
from plan.services.models import Service, ServiceMember, ServiceSuspiciousReason

from common import factories


pytestmark = pytest.mark.django_db


def test_denormalization_checking_periodic_task(data):

    for field in Service.DENORMALIZED_FIELDS:
        assert not getattr(data.service, field)

    check_denormalized(model_path='services.Service')

    data.service.refresh_from_db()
    for field in Service.DENORMALIZED_FIELDS:
        assert getattr(data.service, field, None) is not None


def test_team_size_denormalization(data):
    assert data.metaservice.unique_members_count == 0
    assert data.metaservice.unique_immediate_members_count == 0
    assert data.metaservice.unique_immediate_robots_count == 0
    assert data.metaservice.unique_immediate_external_members_count == 0

    check_obj_with_denormalized_fields(data.metaservice, Service.DENORMALIZED_FIELDS, fix=True)

    data.metaservice.refresh_from_db()
    assert data.metaservice.unique_members_count == len(set(
        ServiceMember
        .objects
        .filter(service__in=data.metaservice.get_descendants(include_self=True))
        .values_list('staff__login', flat=True)
    ))
    assert data.metaservice.unique_immediate_members_count == len(set(
        ServiceMember
        .objects
        .filter(service=data.metaservice, staff__is_robot=False)
        .values_list('staff__login', flat=True)
    ))

    assert data.metaservice.unique_immediate_robots_count == len(set(
        ServiceMember
        .objects
        .filter(service=data.metaservice, staff__is_robot=True)
        .values_list('staff__login', flat=True)
    ))
    assert data.metaservice.unique_immediate_external_members_count == len(set(
        ServiceMember
        .objects
        .filter(service=data.metaservice, staff__affiliation='external', staff__is_robot=False)
        .values_list('staff__login', flat=True)
    ))


def test_ancestors_denormalization(data):
    assert data.service.ancestors == {}

    check_obj_with_denormalized_fields(data.service, Service.DENORMALIZED_FIELDS, fix=True)

    data.service.refresh_from_db()
    assert len(data.service.ancestors) == data.service.get_ancestors().count()


def test_children_count_denormalization(data):
    assert data.service.children_count is None

    check_obj_with_denormalized_fields(data.service, Service.DENORMALIZED_FIELDS, fix=True)

    data.service.refresh_from_db()

    assert data.service.children_count is not None
    assert data.service.children_count == data.service.get_children().count()


def test_descendant_count_denormalization(data):
    assert data.service.descendants_count is None

    check_obj_with_denormalized_fields(data.service, Service.DENORMALIZED_FIELDS, fix=True)

    data.service.refresh_from_db()

    assert data.service.descendants_count is not None
    assert data.service.descendants_count == data.service.get_descendants().count()


def test_has_external_members_denormalization(data):
    external = factories.StaffFactory(affiliation='external')
    factories.ServiceMemberFactory(staff=external, service=data.service)

    check_obj_with_denormalized_fields(data.service, Service.DENORMALIZED_FIELDS, fix=True)

    data.service.refresh_from_db()
    assert data.service.has_external_members is True

    external.is_robot = True
    external.save()
    check_obj_with_denormalized_fields(data.service, Service.DENORMALIZED_FIELDS, fix=True)

    assert data.service.has_external_members is False


def test_has_forced_suspicious_reason_denormalization():
    service = factories.ServiceFactory()
    factories.ServiceSuspiciousReasonFactory(service=service, reason=ServiceSuspiciousReason.FORCED, marked_by=None)
    assert not service.has_forced_suspicious_reason
    check_obj_with_denormalized_fields(service, Service.DENORMALIZED_FIELDS, fix=True)
    service.refresh_from_db()
    assert service.has_forced_suspicious_reason
