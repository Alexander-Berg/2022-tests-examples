from datetime import datetime
from _io import BytesIO
import json
from mock import patch, Mock
import pytest
from typing import List

from django.core.files.uploadedfile import InMemoryUploadedFile
from django.core.urlresolvers import reverse
from django.test.client import RequestFactory

from staff.lib.testing import StaffFactory

from staff.gap.api.views.import_mandatory_vacations import import_mandatory_vacations


def mocked_csv_file(headers: tuple, rows: List[tuple]):
    csv_header = ';'.join(headers)
    csv_rows = [';'.join(row) for row in rows]
    csv_content = ('\n'.join([csv_header, *csv_rows])).encode('utf-8-sig')

    return InMemoryUploadedFile(
        name='file.csv',
        file=BytesIO(csv_content),
        content_type='csv',
        size=len(csv_content),
        charset='utf-8',
        field_name='import_file',
    )


@pytest.mark.django_db
def test_import_mandatory_vacation_works(ya_user):
    staff_amount = 5
    staff = [StaffFactory() for _ in range(staff_amount)]

    rf = RequestFactory()

    request = rf.post(reverse('gap:api-import-mandatory-vacations'))
    request.user = ya_user
    request.FILES['import_file'] = mocked_csv_file(
        ('login', 'date_from', 'date_to'),
        [
            (st.login, '2022-01-01', '2022-02-01')
            for st in staff[:-1]
        ] + [(staff[-1].login, '2022-01-01')],
    )

    with patch('staff.gap.controllers.gap.GapCtl.find', Mock(return_value=[])):
        with patch('staff.gap.workflows.vacation.workflow.VacationWorkflow.new_gap') as patched_new_gap:
            response = import_mandatory_vacations(request)
            print(response.content)
            assert response.status_code == 200
            assert patched_new_gap.call_count == len(staff)


@pytest.mark.django_db
def test_import_mandatory_vacations_dont_works_when_csv_has_wrong_structure(ya_user):
    staff_amount = 5
    staff = [StaffFactory() for _ in range(staff_amount)]

    rf = RequestFactory()

    request = rf.post(reverse('gap:api-import-mandatory-vacations'))
    request.user = ya_user
    request.FILES['import_file'] = mocked_csv_file(
        ('login', 'data_s'),
        [
            (st.login, '2022-01-01')
            for st in staff
        ],
    )

    expected_errors = [
        {
            'code': 'missing-date_from-column',
            'params': {
                'login': st.login,
            },
        }
        for st in staff
    ]

    with patch('staff.gap.workflows.vacation.workflow.VacationWorkflow.new_gap') as patched_new_gap:
        response = import_mandatory_vacations(request)
        assert response.status_code == 400
        assert json.loads(response.content)['errors'] == expected_errors
        assert patched_new_gap.call_count == 0


@pytest.mark.django_db
def test_import_mandatory_vacations_dont_works_when_person_has_mandatory_vacation(ya_user):
    staff_amount = 5
    staff = [StaffFactory() for _ in range(staff_amount)]

    rf = RequestFactory()

    request = rf.post(reverse('gap:api-import-mandatory-vacations'))
    request.user = ya_user
    request.FILES['import_file'] = mocked_csv_file(
        ('login', 'date_from'),
        [
            (st.login, '2022-01-01')
            for st in staff
        ],
    )

    vacations = [
        {
            'person_login': staff[0].login,
            'date_from': datetime(2022, 5, 1),
            'date_to': datetime(2022, 6, 1),
            'id': 10001,
        },
    ]
    patch_gap_ctl_find = patch('staff.gap.controllers.gap.GapCtl.find', Mock(return_value=vacations))

    expected_errors = [
        {
            'code': 'already-has-mandatory-vacation-this-year',
            'params': {
                'login': vacations[0]['person_login'],
                'gap_id': vacations[0]['id'],
            },
        },
    ]
    with patch_gap_ctl_find:
        with patch('staff.gap.workflows.vacation.workflow.VacationWorkflow.new_gap') as patched_new_gap:
            response = import_mandatory_vacations(request)
            assert response.status_code == 400
            assert json.loads(response.content)['errors'] == expected_errors
            assert patched_new_gap.call_count == 0


@pytest.mark.django_db
def test_import_mandatory_vacations_works_when_person_has_mandatory_vacation_for_other_year(ya_user):
    staff_amount = 5
    staff = [StaffFactory() for _ in range(staff_amount)]

    rf = RequestFactory()

    request = rf.post(reverse('gap:api-import-mandatory-vacations'))
    request.user = ya_user
    request.FILES['import_file'] = mocked_csv_file(
        ('login', 'date_from'),
        [
            (st.login, '2022-01-01')
            for st in staff
        ],
    )

    vacations = [
        {
            'person_login': staff[0].login,
            'date_from': datetime(2021, 5, 1),
            'date_to': datetime(2021, 6, 1),
            'id': 10001,
        },
    ]

    with patch('staff.gap.controllers.gap.GapCtl.find', Mock(return_value=vacations)):
        with patch('staff.gap.workflows.vacation.workflow.VacationWorkflow.new_gap') as patched_new_gap:
            response = import_mandatory_vacations(request)
            assert response.status_code == 200
            assert patched_new_gap.call_count == len(staff)
