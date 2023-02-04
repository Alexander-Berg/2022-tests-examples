import pytest
from mock import Mock, patch

import random
from datetime import date

from staff.departments.tree.persons_list_filler import PersonsListFiller


@pytest.mark.django_db
def test_get_as_list_duty():
    filter_context = Mock()
    target = PersonsListFiller(filter_context)
    person_id = random.randint(1, 10000)

    persons = [
        {
            'id': person_id,
            'login': 'randomperson',
            'birthday': None,
            'join_at': date.today(),
            'stafflastoffice__updated_at': None,
        },
    ]

    gaps = {
        person_id: {
            'workflow': 'duty',
            'left_edge': random.random(),
            'right_edge': random.random(),
            'date_from': random.random(),
            'date_to': random.random(),
            'full_day': random.random(),
            'work_in_absence': random.random(),
            'comment': random.random(),
            'id': random.random(),
            'service_slug': random.random(),
            'service_name': random.random(),
            'shift_id': random.random(),
            'role_on_duty': random.random(),
        },
    }

    with patch('staff.departments.tree.persons_list_filler.GapsByPersons') as gaps_by_persons_patch:
        get_by_ids = gaps_by_persons_patch.return_value.sort_by_rank.return_value.only_one.return_value.get_by_ids
        get_by_ids.return_value = gaps
        result = target.get_as_list(persons)
        get_by_ids.assert_called_once_with([person_id])

    for field in ('service_slug', 'service_name', 'shift_id', 'role_on_duty'):
        assert result[0]['gaps'][0][field] == gaps[person_id][field]
