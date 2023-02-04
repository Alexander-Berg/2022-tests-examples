import pytest
import os

from freezegun import freeze_time

from django.conf import settings
from django.core.urlresolvers import reverse
from django.utils import timezone

from common import factories
from plan.denormalization.tasks import update_denormalized_field
from plan.services import models
from plan.services.state import SERVICE_STATE
from plan.unistat.models import TaskMetric
from plan.unistat.tasks import closuretree_damaged_tree, service_level_broken
from plan.resources.models import ServiceResource


pytestmark = [
    pytest.mark.django_db, pytest.mark.postgresql
]

DATA_DIR = os.path.join(settings.TESTS_DIR, 'test_data/metrics')


@pytest.fixture
@freeze_time('2018-01-01')
def data(db):
    meta_other = factories.ServiceFactory(slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG)

    # meta_other
    for _ in range(2):
        serv = factories.ServiceFactory(
            parent=meta_other, oebs_parent_id=42,
            oebs_data={
                'name': 'smth', 'name_en': 'smth_en',
                'use_for_group_only': False,
                'use_for_revenue': False,
                'use_for_hr': False,
                'use_for_procurement': False,
                'deviation_reason': 'parent',
            }
        )
        update_denormalized_field('services.Service', serv.id, 'ancestors')

    for state in SERVICE_STATE.ALL_STATES - SERVICE_STATE.ACTIVE_STATES:
        factories.ServiceFactory(parent=meta_other, state=state)

    # non-exportable
    for _ in range(2):
        factories.ServiceFactory(is_exportable=False)

    # meta_other non-exportable
    for _ in range(2):
        factories.ServiceFactory(parent=meta_other, is_exportable=False)

    for state in SERVICE_STATE.ALL_STATES - SERVICE_STATE.ACTIVE_STATES:
        factories.ServiceFactory(parent=meta_other, state=state, is_exportable=False)

    # ownerless
    for _ in range(3):
        factories.ServiceFactory(owner=None)

    # move requests
    for state in models.ServiceMoveRequest.ACTIVE_STATES:
        factories.ServiceMoveRequestFactory(state=state)

    for state in models.ServiceMoveRequest.INACTIVE_STATES:
        factories.ServiceMoveRequestFactory(state=state)

    # readonly
    factories.ServiceFactory(readonly_state=models.Service.CREATING)
    for _ in range(2):
        factories.ServiceFactory(
            readonly_state=models.Service.MOVING,
            readonly_start_time=timezone.now() - timezone.timedelta(hours=1)
        )
    for _ in range(3):
        factories.ServiceFactory(
            readonly_state=models.Service.RENAMING,
            readonly_start_time=timezone.now() - timezone.timedelta(minutes=40))

    for _ in range(4):
        factories.ServiceFactory(readonly_state=models.Service.DELETING)

    # service states
    for _ in range(2):
        factories.ServiceFactory(state=models.Service.states.NEEDINFO)
    for _ in range(3):
        factories.ServiceFactory(state=models.Service.states.SUPPORTED)

    # inactive
    for state in SERVICE_STATE.ALL_STATES - SERVICE_STATE.ACTIVE_STATES:
        factories.ServiceFactory(state=state)

    # service resources
    resource_type = factories.ResourceTypeFactory(
        supplier_plugin='direct',
        code=settings.DIRECT_RESOURCE_TYPE_CODE,
    )

    for i in range(5):
        factories.ServiceResourceFactory(
            resource=factories.ResourceFactory(type=resource_type),
            service=meta_other,
            state=ServiceResource.GRANTED,
        )

    for i in range(6):
        factories.ServiceResourceFactory(
            resource=factories.ResourceFactory(type=resource_type),
            service=meta_other,
            state=ServiceResource.REQUESTED,
        )

    tvm_resource_type = factories.ResourceTypeFactory(
        code=settings.TVM_RESOURCE_TYPE_CODE,
    )
    tvm_resource = factories.ResourceFactory(
        type=tvm_resource_type
    )
    for _ in range(2):
        factories.ServiceResourceFactory(
            resource=tvm_resource,
            state=ServiceResource.GRANTED,
        )
    factories.ServiceResourceFactory(
        resource=factories.ResourceFactory(type=tvm_resource_type),
        state=ServiceResource.GRANTED,
        service=meta_other,
    )

    resource_type_without_code = factories.ResourceTypeFactory(
        supplier_plugin='direct',
    )

    factories.ServiceResourceFactory(
        resource=factories.ResourceFactory(type=resource_type_without_code),
        service=meta_other,
        state=ServiceResource.GRANTED,
    )


@pytest.mark.parametrize(
    'staff_role',
    ('own_only_viewer', 'services_viewer', 'full_access')
)
@freeze_time('2018-01-01')
def test_metrics(client, data, staff_role, staff_factory):
    TaskMetric.objects.create(
        task_name='task_name',
        last_success_end=timezone.now() - timezone.timedelta(seconds=100),
        send_to_unistat=True,
    )

    (
        service_with_all, service_with_roles, service_with_puncher_rules, service_with_active_resources
    ) = models.Service.objects.all()[:4]

    members = list()
    for i in range(100):
        members.append(
            factories.ServiceMemberFactory(
                found_in_staff_at=timezone.now() - timezone.timedelta(minutes=i)
            )
        )
        members[-1].created_at = timezone.now() - timezone.timedelta(minutes=2 * i)
        members[-1].save(update_fields=['created_at'])

    members[-1].state = 'depriving'
    members[-1].deprived_at = timezone.now()
    members[-1].save(update_fields=['state', 'deprived_at'])

    members[-2].state = 'depriving'
    members[-1].save(update_fields=['state'])

    resource = factories.ResourceFactory()
    another_resource = factories.ResourceFactory()
    one_more_resource = factories.ResourceFactory()

    # есть активные роли, активные правила в панчере и активные ресурсы
    service_with_all.puncher_rules_count = 1
    service_with_all.idm_roles_count = 1
    service_with_all.serviceresource_set.create(state='granted', resource=resource, type_id=resource.type_id)
    # есть активные роли и неактивный ресурс
    service_with_roles.idm_roles_count = 5
    service_with_roles.serviceresource_set.create(state='deprived', resource=resource, type_id=resource.type_id)
    # есть активные правила в панчере
    service_with_puncher_rules.puncher_rules_count = 5
    # есть активные и неактивные ресурсы
    service_with_active_resources.serviceresource_set.create(
        state='granted',
        resource=resource,
        type_id=resource.type_id,
    )
    service_with_active_resources.serviceresource_set.create(
        state='deprived',
        resource=another_resource,
        type_id=resource.type_id,
    )
    service_with_active_resources.serviceresource_set.create(
        state='granted',
        resource=one_more_resource,
        type_id=resource.type_id,
    )

    service_with_all.save()
    service_with_roles.save()
    service_with_puncher_rules.save()
    service_with_active_resources.save()

    no_owner, wrong_owner, parent_is_suspicious = models.Service.objects.all()[:3]
    no_owner.suspicious_date = timezone.now().date()
    no_owner.save()
    factories.ServiceSuspiciousReasonFactory(reason=models.ServiceSuspiciousReason.NO_ONWER, service=no_owner)
    factories.ServiceSuspiciousReasonFactory(reason=models.ServiceSuspiciousReason.WRONG_OWNER, service=wrong_owner)
    factories.ServiceSuspiciousReasonFactory(
        reason=models.ServiceSuspiciousReason.PARENT_IS_SUSPICIOUS, service=parent_is_suspicious
    )
    factories.ServiceSuspiciousReasonFactory(
        reason=models.ServiceSuspiciousReason.PARENT_IS_SUSPICIOUS, service=parent_is_suspicious
    )

    client.login(staff_factory(staff_role).login)

    response = client.json.get(
        reverse('unistat')
    )

    assert response.status_code == 200

    metrics = dict(response.json())

    assert set(metrics.keys()) == {
        'abc_task_name_max',
        'abc_active_memberships_max',
        'abc_center_last_touched_max',
        'abc_exportable_in_sandbox_max',
        'abc_live_move_requests_max',
        'abc_meta_other_services_max',
        'abc_non_exportable_services_max',
        'abc_non_exportable_services_not_in_sandbox_max',
        'abc_ownerless_services_max',
        'abc_services_with_active_resources_max',
        'abc_services_with_puncher_rules_max',
        'abc_services_with_roles_max',
        'abc_services_with_membership_inheritance_max',
        'abc_services_with_something_active_max',
        'abc_services_in_readonly_state_for_long_time_max',
        'abc_services_in_readonly_state_creating_max',
        'abc_services_in_readonly_state_deleting_max',
        'abc_services_in_readonly_state_moving_max',
        'abc_services_in_readonly_state_renaming_max',
        'abc_services_in_readonly_state_closing_max',
        'abc_total_services_max',
        'abc_services_with_important_resources_max',
        'abc_unique_robots_max',
        'abc_robot_memberships_max',
        'abc_depriving_deprived_members_max',
        'abc_unique_service_members_max',
        'abc_base_service_under_usual_max',
        'abc_usual_service_under_base_non_leaf_max',
        'abc_found_in_staff_time_for_24_hours_100_percentile_max',
        'abc_found_in_staff_time_for_24_hours_90_percentile_max',
        'abc_found_in_staff_time_for_24_hours_95_percentile_max',
        'abc_found_in_staff_time_for_1_hours_95_percentile_max',
        'abc_found_in_staff_time_for_1_hours_90_percentile_max',
        'abc_found_in_staff_time_for_1_hours_100_percentile_max',
        'abc_closuretree_damaged_tree_max',
        'abc_service_level_broken_max',
        'abc_granted_resources_direct_client_max',
        'abc_broken_department_max',
        'abc_tvm2_granted_duplicates_max',
        'abc_granted_resources_passport-tvm-application_max',
        'abc_oebs_deviation_max',
        'abc_oebs_deviation_parent_max',
        'abc_oebs_deviation_flag_max',
        'abc_oebs_deviation_name_max',
        'abc_oebs_deviation_resource_max',
        'abc_oebs_stale_agreements_max',
    }

    assert metrics['abc_granted_resources_direct_client_max'] == 5
    assert metrics['abc_found_in_staff_time_for_24_hours_95_percentile_max'] == timezone.timedelta(
        minutes=94
    ).total_seconds()
    assert metrics['abc_found_in_staff_time_for_24_hours_90_percentile_max'] == timezone.timedelta(
        minutes=89
    ).total_seconds()
    assert metrics['abc_found_in_staff_time_for_24_hours_100_percentile_max'] == timezone.timedelta(
        minutes=99
    ).total_seconds()

    assert metrics['abc_found_in_staff_time_for_1_hours_95_percentile_max'] == timezone.timedelta(
        minutes=56
    ).total_seconds()
    assert metrics['abc_found_in_staff_time_for_1_hours_90_percentile_max'] == timezone.timedelta(
        minutes=53
    ).total_seconds()
    assert metrics['abc_found_in_staff_time_for_1_hours_100_percentile_max'] == timezone.timedelta(
        minutes=59
    ).total_seconds()

    assert metrics['abc_tvm2_granted_duplicates_max'] == 1
    assert metrics['abc_task_name_max'] == 100
    assert metrics['abc_non_exportable_services_not_in_sandbox_max'] == 2
    assert metrics['abc_exportable_in_sandbox_max'] == 3    # Песочница и два потомка

    assert metrics['abc_total_services_max'] == models.Service.objects.active().count()
    assert metrics['abc_meta_other_services_max'] == 4
    assert metrics['abc_non_exportable_services_max'] == 4
    assert metrics['abc_ownerless_services_max'] == 3
    assert metrics['abc_live_move_requests_max'] == len(models.ServiceMoveRequest.ACTIVE_STATES)

    assert metrics['abc_services_in_readonly_state_creating_max'] == 1
    assert metrics['abc_services_in_readonly_state_moving_max'] == 2
    assert metrics['abc_services_in_readonly_state_renaming_max'] == 3
    assert metrics['abc_services_in_readonly_state_deleting_max'] == 4
    assert metrics['abc_services_in_readonly_state_for_long_time_max'] == 5

    assert metrics['abc_services_with_active_resources_max'] == 4
    assert metrics['abc_services_with_puncher_rules_max'] == 2
    assert metrics['abc_services_with_roles_max'] == 2
    assert metrics['abc_services_with_something_active_max'] == 6

    assert metrics['abc_base_service_under_usual_max'] == 0
    assert metrics['abc_usual_service_under_base_non_leaf_max'] == 0
    assert metrics['abc_oebs_deviation_max'] == 2
    assert metrics['abc_oebs_deviation_parent_max'] == 2
    assert metrics['abc_oebs_deviation_name_max'] == 2
    assert metrics['abc_depriving_deprived_members_max'] == 1


@freeze_time('2018-01-01')
def test_metrics_base_service_under_usual(client):
    factories.ServiceFactory(slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG)

    usual_service = factories.ServiceFactory(is_base=False)
    factories.ServiceFactory(is_base=True, parent=usual_service)

    TaskMetric.objects.create(
        task_name='task_name',
        last_success_end=timezone.now() - timezone.timedelta(seconds=100),
        send_to_unistat=True,
    )
    response = client.json.get(
        reverse('unistat')
    )

    assert response.status_code == 200
    metrics = dict(response.json())

    assert metrics['abc_base_service_under_usual_max'] == 1
    assert metrics['abc_usual_service_under_base_non_leaf_max'] == 0


@freeze_time('2018-01-01')
def test_metrics_usual_service_under_base_non_leaf(client):
    factories.ServiceFactory(slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG)

    non_leaf = factories.ServiceFactory(is_base=True)
    factories.ServiceFactory(is_base=True, parent=non_leaf)
    factories.ServiceFactory(is_base=False, parent=non_leaf)

    TaskMetric.objects.create(
        task_name='task_name',
        last_success_end=timezone.now() - timezone.timedelta(seconds=100),
        send_to_unistat=True,
    )
    response = client.json.get(
        reverse('unistat')
    )

    assert response.status_code == 200
    metrics = dict(response.json())

    assert metrics['abc_base_service_under_usual_max'] == 0
    assert metrics['abc_usual_service_under_base_non_leaf_max'] == 1


@freeze_time('2018-01-01')
def test_services_in_readonly_state_for_long_time_with_dispenser(client, data):
    old_readonly_long_time = models.Service.objects.filter(
        state__in=models.Service.states.ACTIVE_STATES,
        readonly_state__isnull=False,
        readonly_start_time__lt=timezone.now() - timezone.timedelta(
            minutes=settings.MINUTES_BEFORE_SERVICE_DIE_IN_READ_ONLY_STATE
        ),
    ).count()

    for _ in range(10):
        factories.ServiceFactory(
            readonly_state=models.Service.CLOSING,
            readonly_start_time=timezone.now() - timezone.timedelta(hours=1),
        )

    factories.ServiceFactory(
        readonly_state=models.Service.CLOSING,
        readonly_start_time=timezone.now() - timezone.timedelta(minutes=10),
    )

    abc = factories.ServiceCloseRequestFactory(state=models.ServiceRequestMixin.PROCESSING_D)
    abc.service.readonly_state = models.Service.CLOSING
    abc.readonly_start_time = timezone.now() - timezone.timedelta(minutes=1442)
    abc.save()

    dispenser = factories.ServiceDeleteRequestFactory(state=models.ServiceRequestMixin.PROCESSING_D)
    dispenser.service.readonly_state = models.Service.DELETING
    dispenser.readonly_start_time = timezone.now() - timezone.timedelta(minutes=1200)
    dispenser.service.save()

    goalz = factories.ServiceMoveRequestFactory(state=models.ServiceRequestMixin.PROCESSING_D)
    goalz.service.readonly_state = models.Service.MOVING
    goalz.readonly_start_time = timezone.now() - timezone.timedelta(minutes=10)
    goalz.service.save()

    response = client.json.get(
        reverse('unistat')
    )

    assert response.status_code == 200
    metrics = dict(response.json())

    assert metrics['abc_services_in_readonly_state_for_long_time_max'] == old_readonly_long_time + 10


def test_closuretree_damage():
    parent1 = factories.ServiceFactory()
    parent2 = factories.ServiceFactory()
    child = factories.ServiceFactory(parent=parent1)
    assert closuretree_damaged_tree() == 0
    models.Service.objects.filter(id=child.id).update(parent_id=parent2.id)
    assert closuretree_damaged_tree() == 1


def test_broken_level():
    ok_zero_level = factories.ServiceFactory(parent=None)
    factories.ServiceFactory(parent=ok_zero_level)
    assert service_level_broken() == 0
    bad_zero_level = factories.ServiceFactory(parent=None)
    models.Service.objects.filter(id=bad_zero_level.id).update(level=3)
    assert service_level_broken() == 1
    bad_child = factories.ServiceFactory(parent=ok_zero_level)
    models.Service.objects.filter(id=bad_child.id).update(level=0)
    assert service_level_broken() == 1


def test_broken_department(client, metaservices):
    department_1 = factories.DepartmentFactory()
    department_2 = factories.DepartmentFactory(parent=department_1)
    department_2.level -= 1
    links = department_2._closure_model.objects.all()
    links.delete()

    assert len(department_2.get_ancestors()) == 0

    TaskMetric.objects.create(
        task_name='task_name',
        last_success_end=timezone.now() - timezone.timedelta(seconds=100),
        send_to_unistat=True,
    )

    response = client.json.get(
        reverse('unistat')
    )

    assert response.status_code == 200
    metrics = dict(response.json())

    assert metrics['abc_broken_department_max'] == 1
