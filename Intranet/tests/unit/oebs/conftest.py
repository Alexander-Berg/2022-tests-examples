import pytest
import pretend
from collections import namedtuple

from django.conf import settings

from plan.oebs import models as oebs_models
from plan.oebs.constants import ACTIONS, OEBS_FLAGS, STATES
from plan.services.models import ServiceMoveRequest
from plan.resources.models import ServiceResource
from plan.services.tasks import calculate_gradient_fields

from common import factories


StartrekIssue = namedtuple('StartrekIssue', ['key'])


@pytest.fixture()
def oebs_resource_type(db):
    return factories.ResourceTypeFactory(code=settings.OEBS_PRODUCT_RESOURCE_TYPE_CODE)


@pytest.fixture()
def oebs_service(person, owner_role, service, oebs_resource_type):
    """Связанный с OEBS сервис"""
    oebs_service = factories.ServiceFactory(
        parent=service,
        owner=person,
        use_for_hr=True,
    )
    factories.ServiceMemberFactory(
        service=oebs_service,
        role=owner_role,
        staff=person
    )
    oebs_resource = factories.ResourceFactory(type=oebs_resource_type)
    ServiceResource.objects.create_granted(
        service=oebs_service,
        resource=oebs_resource,
        type_id=oebs_resource_type.id,
        attributes={
            'parent_oebs_id': '123',
            'leaf_oebs_id': '234',
        },
    )
    return oebs_service


@pytest.fixture()
def dormant_oebs_service(person, owner_role, service, oebs_resource_type):
    """Сервис с OEBS ресурсом (бывший), у которого выключены OEBS флаги"""
    oebs_service = factories.ServiceFactory(
        parent=service,
        owner=person,
    )
    factories.ServiceMemberFactory(
        service=oebs_service,
        role=owner_role,
        staff=person
    )
    oebs_resource = factories.ResourceFactory(type=oebs_resource_type)
    ServiceResource.objects.create_granted(
        service=oebs_service,
        resource=oebs_resource,
        type_id=oebs_resource_type.id,
        attributes={
            'parent_oebs_id': '123',
            'leaf_oebs_id': '234',
        },
    )
    return oebs_service


@pytest.fixture()
def dormant_oebs_service_agreement(request, person, dormant_oebs_service):
    action = request.param if hasattr(request, 'param') else ACTIONS.RENAME
    return factories.OEBSAgreementFactory(
        action=action,
        requester=person,
        state=STATES.VALIDATED_IN_OEBS,
        notify_only=True,
        service=dormant_oebs_service,
    )


@pytest.fixture()
def move_request(person, service, oebs_service, metaservice):
    return factories.ServiceMoveRequestFactory(
        service=oebs_service,
        destination=metaservice,
        source=service,
        state=ServiceMoveRequest.REQUESTED,
        requester=person,
    )


@pytest.fixture()
def move_agreement(move_request, person):
    return factories.OEBSAgreementFactory(
        action=oebs_models.ACTIONS.MOVE,
        requester=person,
        move_request=move_request,
        service=move_request.service,
    )


@pytest.fixture()
def oebs_tags():
    tags = [
        factories.ServiceTagFactory(slug=tag_slug)
        for tag_slug in OEBS_FLAGS.values()
    ]
    return tags


@pytest.fixture()
def oebs_data(person, responsible_role, metaservices, dormant_oebs_service, oebs_resource_type, oebs_tags):
    metaservice, other_service = metaservices
    service = factories.ServiceFactory(
        parent=dormant_oebs_service,
        owner=person,
    )
    factories.ServiceMemberFactory(
        service=service,
        role=responsible_role,
        staff=person
    )

    resource = factories.ResourceFactory(type=oebs_resource_type, external_id='xxx')

    service_resource = factories.ServiceResourceFactory(
        resource=resource,
        service=service,
        state=ServiceResource.GRANTED,
        attributes={
            'parent_oebs_id': '123',
            'leaf_oebs_id': '234',
        }
    )

    role_revenue = factories.RoleFactory(code=settings.OEBS_REVENUE_ROLE_CODE, name=settings.OEBS_REVENUE_ROLE_CODE)
    role_hardware = factories.RoleFactory(code=settings.OEBS_HARDWARE_ROLE_CODE, name=settings.OEBS_HARDWARE_ROLE_CODE)
    role_hr = factories.RoleFactory(code=settings.OEBS_HR_ROLE_CODE, name=settings.OEBS_HR_ROLE_CODE)
    role_procurement = factories.RoleFactory(code=settings.OEBS_PROCUREMENT_ROLE_CODE, name=settings.OEBS_PROCUREMENT_ROLE_CODE)
    money_map_tag = factories.ServiceTagFactory(slug=settings.MONEY_MAP_TAG)

    return pretend.stub(
        service=service,
        service_resource=service_resource,
        staff=person,
        other_service=other_service,
        role_revenue=role_revenue,
        role_hardware=role_hardware,
        role_hr=role_hr,
        role_procurement=role_procurement,
        money_map_tag=money_map_tag,
    )


@pytest.fixture()
def service_tree(staff_factory, oebs_resource_type):
    """
     Структура сервисов:


        root_service (VS, oebs resource)
        |
        |_  service1 (oebs resource)
        |   |
        |   |_ service3 (oebs resource)
        |   |
        |   |_ service4
        |
        |_  service2
            |
            |_ service5 (oebs resource)


        other_root_service (BU)

    """

    vs_tag = factories.ServiceTagFactory(slug=settings.GRADIENT_VS)
    bu_tag = factories.ServiceTagFactory(slug=settings.BUSINESS_UNIT_TAG)

    owner = staff_factory()

    root_service = factories.ServiceFactory(owner=owner)
    root_service.tags.add(vs_tag)
    root_oebs = factories.ResourceFactory(type=oebs_resource_type)
    root_oebs_sr = factories.ServiceResourceFactory(service=root_service, resource=root_oebs, attributes={'parent_oebs_id': 12})

    service1 = factories.ServiceFactory(parent=root_service, owner=owner)
    oebs1 = factories.ResourceFactory(type=oebs_resource_type)
    oebs1_sr = factories.ServiceResourceFactory(service=service1, resource=oebs1, attributes={'parent_oebs_id': 34})
    service2 = factories.ServiceFactory(parent=root_service)

    service3 = factories.ServiceFactory(parent=service1, owner=owner)
    oebs3 = factories.ResourceFactory(type=oebs_resource_type)
    oebs3_sr = factories.ServiceResourceFactory(service=service3, resource=oebs3, attributes={'leaf_oebs_id': 56})
    service4 = factories.ServiceFactory(parent=service1, owner=owner)
    service5 = factories.ServiceFactory(parent=service2, owner=owner)
    oebs5 = factories.ResourceFactory(type=oebs_resource_type)
    oebs5_sr = factories.ServiceResourceFactory(service=service5, resource=oebs5,  attributes={'leaf_oebs_id': 78})

    other_root_service = factories.ServiceFactory()
    other_root_service.tags.add(bu_tag)

    calculate_gradient_fields(root_service.pk)

    return pretend.stub(
        root_service=root_service,
        service1=service1,
        service2=service2,
        service3=service3,
        service4=service4,
        service5=service5,
        other_root_service=other_root_service,
        root_oebs=root_oebs,
        oebs1=oebs1,
        oebs3=oebs3,
        oebs5=oebs5,
        root_oebs_sr=root_oebs_sr,
        oebs1_sr=oebs1_sr,
        oebs3_sr=oebs3_sr,
        oebs5_sr=oebs5_sr,
        owner=owner,
    )
