import pytest

import random

from staff.lib.testing import GroupFactory, StaffFactory, ValueStreamFactory


from staff.budget_position.controllers import export_person_value_streams_controller
from staff.budget_position.tests.utils import BudgetPositionAssignmentFactory


@pytest.mark.django_db
def test_export_person_value_streams_controller():
    root_service_tags = ['aa', 'bb']
    root_value_stream = ValueStreamFactory()
    root_service_group = GroupFactory(
        url=root_value_stream.url,
        service_id=random.randint(1, 42342),
        service_tags=','.join(root_service_tags),
    )

    nested_service_tags = ['aa2', 'bb2']
    nested_value_stream = ValueStreamFactory(parent=root_value_stream)
    nested_service_group = GroupFactory(
        url=nested_value_stream.url,
        service_id=random.randint(1, 42342),
        service_tags=','.join(nested_service_tags),
    )

    person = StaffFactory()
    BudgetPositionAssignmentFactory(
        person=person,
        value_stream=nested_value_stream,
        main_assignment=True,
    )

    expected_value_streams = [
        {
            'url': root_value_stream.url,
            'name': root_value_stream.name,
            'name_en': root_value_stream.name_en,
            'abc_service_id': root_service_group.service_id,
            'service_tags': [x for x in root_service_group.service_tags.split(',') if x],
        },
        {
            'url': nested_value_stream.url,
            'name': nested_value_stream.name,
            'name_en': nested_value_stream.name_en,
            'abc_service_id': nested_service_group.service_id,
            'service_tags': [x for x in nested_service_group.service_tags.split(',') if x],
        },
    ]

    result = export_person_value_streams_controller([person.login])

    assert result == {person.login: expected_value_streams}


@pytest.mark.django_db
def test_export_person_value_streams_controller_no_main_assignment():
    person = StaffFactory()
    BudgetPositionAssignmentFactory(
        person=person,
        main_assignment=False,
    )

    result = export_person_value_streams_controller([person.login])

    assert result == {person.login: []}
