import pytest
from mock import MagicMock, ANY

import random

from staff.departments.tests.factories import HRProductFactory
from staff.lib.testing import DepartmentFactory, ValueStreamFactory, GeographyFactory

from staff.budget_position import const
from staff.budget_position.models import FemidaPushOutbox
from staff.budget_position.tests.workflow_tests.utils import WorkflowModelFactory
from staff.budget_position.workflow_service import gateways, entities


@pytest.mark.django_db
def test_schedule_department_push_creates_entry_in_outbox():
    # given
    existing_workflow_id = WorkflowModelFactory(status=const.WORKFLOW_STATUS.CONFIRMED).id
    femida_service = gateways.FemidaService(MagicMock())
    outbox_items = FemidaPushOutbox.objects.count()

    # when
    femida_service.schedule_department_push(existing_workflow_id)

    # then
    assert FemidaPushOutbox.objects.count() == outbox_items + 1


@pytest.mark.django_db
@pytest.mark.parametrize('femida_status_code', [200, 304])
def test_push_actually_pushes_changes_to_femida_hr_product_id(femida_status_code):
    department = DepartmentFactory()
    hr_product = HRProductFactory(value_stream=ValueStreamFactory())
    ticket = 'SOME-123'
    hr_product_resolver = MagicMock()
    value_stream_id = None
    hr_product_resolver.resolve_hr_product.return_value = value_stream_id, None

    _push_actually_pushes_changes_to_femida_test(
        entities.Change(department_id=department.id, ticket=ticket, hr_product_id=hr_product.id),
        {
            'department': department.id,
            'startrek_approval_issue_key': ticket,
            'value_stream': value_stream_id,
            'geography': None,
        },
        femida_status_code=femida_status_code,
        hr_product_resolver=hr_product_resolver,
    )

    assert FemidaPushOutbox.objects.count() == 0
    hr_product_resolver.resolve_hr_product.assert_called_once_with(hr_product.id)


@pytest.mark.django_db
@pytest.mark.parametrize('femida_status_code', [200, 304])
def test_push_actually_pushes_changes_to_femida_geography_id(femida_status_code):
    department = DepartmentFactory()
    geography = GeographyFactory()
    ticket = 'SOME-123'
    geography_resolver = MagicMock()
    geography_id = None
    geography_resolver.resolve_geography.return_value = geography_id, None

    _push_actually_pushes_changes_to_femida_test(
        entities.Change(department_id=department.id, ticket=ticket, geography_url=geography.department_instance.url),
        {
            'department': department.id,
            'startrek_approval_issue_key': ticket,
            'geography': geography_id,
            'value_stream': None,
        },
        femida_status_code=femida_status_code,
        geography_resolver=geography_resolver,
    )

    assert FemidaPushOutbox.objects.count() == 0
    geography_resolver.resolve_geography.assert_called_once_with(geography.department_instance.url)


@pytest.mark.django_db
@pytest.mark.parametrize(
    'femida_status_code, ticket',
    [
        (200, 'TICKET-123'),
        (304, None),
    ],
)
def test_push_actually_pushes_geography_changes_to_femida(femida_status_code, ticket):
    department = DepartmentFactory()

    error_message = f'resolver error {random.random()}'
    resolver_instance = MagicMock()
    resolving_url = str(random.randint(1, 10056))
    resolved_id = random.randint(1, 10056)
    resolver_instance.resolve_geography.return_value = resolved_id, error_message

    startrek_service = MagicMock()

    change = entities.Change(department_id=department.id, ticket=ticket)
    change.geography_url = resolving_url

    _push_actually_pushes_changes_to_femida_test(
        change=change,
        expected_femida_post={
            'department': department.id,
            'startrek_approval_issue_key': ticket,
            'geography': resolved_id,
            'value_stream': None,
        },
        femida_status_code=femida_status_code,
        startrek_service=startrek_service,
        **{'geography_resolver': resolver_instance},
    )

    assert FemidaPushOutbox.objects.count() == 0
    resolver_instance.resolve_geography.assert_called_once_with(resolving_url)

    if ticket:
        startrek_service.add_comment.assert_called_once()
    else:
        startrek_service.add_comment.assert_not_called()


@pytest.mark.django_db
@pytest.mark.parametrize(
    'femida_status_code, ticket, from_entity, to_entity',
    [
        (200, 'TICKET-123', 'hr_product', 'value_stream'),
        (304, None, 'hr_product', 'value_stream'),
    ],
)
def test_push_actually_pushes_changes_to_femida_resolving_error(femida_status_code, ticket, from_entity, to_entity):
    resolver = from_entity + '_resolver'
    method = 'resolve_' + from_entity
    other_entity = list({'value_stream', 'geography'} - {to_entity})[0]
    department = DepartmentFactory()

    error_message = f'resolver error {random.random()}'
    resolver_instance = MagicMock()
    resolving_id = random.randint(1, 10056)
    resolved_id = random.randint(1, 10056)
    getattr(resolver_instance, method).return_value = resolved_id, error_message

    startrek_service = MagicMock()

    change = entities.Change(department_id=department.id, ticket=ticket)
    setattr(change, from_entity + '_id', resolving_id)

    _push_actually_pushes_changes_to_femida_test(
        change=change,
        expected_femida_post={
            'department': department.id,
            'startrek_approval_issue_key': ticket,
            to_entity: resolved_id,
            other_entity: None,
        },
        femida_status_code=femida_status_code,
        startrek_service=startrek_service,
        **{resolver: resolver_instance},
    )

    assert FemidaPushOutbox.objects.count() == 0
    getattr(resolver_instance, method).assert_called_once_with(resolving_id)

    if ticket:
        startrek_service.add_comment.assert_called_once()
    else:
        startrek_service.add_comment.assert_not_called()


@pytest.mark.django_db
def test_push_no_data():
    existing_workflow_id = WorkflowModelFactory(code='1.1.1', status=const.WORKFLOW_STATUS.CONFIRMED).id
    FemidaPushOutbox.objects.create(workflow_id=existing_workflow_id)

    workflow = entities.workflows.Workflow1_1_1(
        workflow_id=existing_workflow_id,
        changes=[entities.Change(department_id=None, hr_product_id=None, geography_url=None)],
    )
    workflow.vacancy_id = 100500

    femida_service = gateways.FemidaService(gateways.WorkflowRepository())
    femida_service._repository.get_by_id = MagicMock(return_value=workflow)
    femida_service._session.post = MagicMock()

    femida_service.push_scheduled()

    assert FemidaPushOutbox.objects.count() == 0
    femida_service._session.post.assert_not_called()


@pytest.mark.django_db
@pytest.mark.parametrize('femida_status_code', [200, 304])
def test_push_actually_pushes_changes_to_femida(femida_status_code):
    department = DepartmentFactory()
    hr_product = HRProductFactory(value_stream=ValueStreamFactory())
    geography = GeographyFactory()
    ticket = 'SOME-123'
    change = entities.Change(
        department_id=department.id,
        ticket=ticket,
        hr_product_id=hr_product.id,
        geography_url=geography.department_instance.url,
    )

    _push_actually_pushes_changes_to_femida_test(
        change,
        {
            'department': department.id,
            'value_stream': hr_product.value_stream.id,
            'geography': geography.department_instance.id,
            'startrek_approval_issue_key': ticket,
        },
        femida_status_code=femida_status_code,
    )
    assert FemidaPushOutbox.objects.count() == 0


@pytest.mark.django_db
def test_push_scheduled_femida_error():
    department = DepartmentFactory()
    ticket = 'SOME-123'

    _push_actually_pushes_changes_to_femida_test(
        entities.Change(department_id=department.id, ticket=ticket),
        {'department': department.id, 'startrek_approval_issue_key': ticket, 'value_stream': None, 'geography': None},
        femida_status_code=400,
    )
    assert FemidaPushOutbox.objects.count() == 1


def _push_actually_pushes_changes_to_femida_test(
    change,
    expected_femida_post,
    femida_status_code=200,
    **kwargs
):
    # given
    existing_workflow_id = WorkflowModelFactory(code='1.1.1', status=const.WORKFLOW_STATUS.CONFIRMED).id
    FemidaPushOutbox.objects.create(workflow_id=existing_workflow_id)

    workflow = entities.workflows.Workflow1_1_1(
        workflow_id=existing_workflow_id,
        changes=[change],
    )
    workflow.vacancy_id = 100500

    femida_service = gateways.FemidaService(gateways.WorkflowRepository(), **kwargs)
    femida_service._repository.get_by_id = MagicMock(return_value=workflow)
    femida_service._session.post = MagicMock(return_value=MagicMock(status_code=femida_status_code))

    # when
    femida_service.push_scheduled()

    # then
    femida_service._session.post.assert_called_once_with(
        femida_service.change_vacancy_url(workflow.vacancy_id),
        json=expected_femida_post,
        timeout=ANY,
        no_log_json=True,
    )
