import datetime
from datetime import timedelta
import json

import mock
import pytest

from django.conf import settings

from staff.lib.testing import UserFactory, StaffFactory

from staff.gap.controllers.templates import TemplatesCtl
from staff.gap.exceptions import (
    MandatoryVacationYearChangedError,
    MandatoryVacationCancelError,
    MandatoryVacationTooShortError,
    MandatoryVacationWithHolidaysTooShortError,
)
from staff.gap.edit_views.gap_state_views import gap_action
from staff.gap.workflows.vacation.workflow import VacationWorkflow
from staff.gap.workflows.choices import GAP_STATES as GS


@pytest.mark.django_db
def test_new_gap(gap_test, company):
    now = gap_test.mongo_now()
    person = company.persons['dep2-person']
    gap_test.test_person = person
    gap_test.DEFAULT_MODIFIER_ID = person.id

    base_gap = gap_test.get_base_gap(VacationWorkflow)
    base_gap['is_selfpaid'] = True

    workflow = VacationWorkflow(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
        None,
    )
    gap = workflow.new_gap(base_gap)

    gap_test.base_assert_new_gap(VacationWorkflow, now, base_gap, gap)

    assert gap['id'] == 1
    assert gap['state'] == GS.NEW
    assert gap['created_by_id'] == gap_test.DEFAULT_MODIFIER_ID
    assert gap['modified_by_id'] == gap_test.DEFAULT_MODIFIER_ID
    assert gap['is_selfpaid']
    expected_issue_params = {
        'hr': company.persons['dep2-hr-partner'].login,
        'currentHead': company.persons['dep2-chief'].login,
        'legalEntity2': person.organization.st_translation_id,
    }
    assert workflow.get_initial_issue_params() == expected_issue_params


@pytest.mark.django_db
def test_edit_vacation_gap(company, gap_test):
    person = company.persons['dep2-person']
    base_gap = gap_test.get_base_gap(VacationWorkflow)
    gap = VacationWorkflow(person.id, person.id, None).new_gap(base_gap)
    gap['master_issue'] = 'VACATION-1234'

    new_gap_data = gap_test.get_api_base_edit_gap(gap['id'])

    TemplatesCtl().new(
        type='st_issue_comment',
        tag='edit_gap',
        template={'text': 'edit_gap_template', 'state_action_id': 123}
    )

    with mock.patch('staff.gap.workflows.startrek_mixin.StartrekCtl') as mocked_st:
        VacationWorkflow.init_to_modify(person.id, gap=gap).edit_gap(new_gap_data)

    one_day = timedelta(days=1)
    last_date = new_gap_data['date_to'].date() - one_day  # в Гэпе дата окончания хранится как след. день 00:00

    mocked_st().issue.update.assert_called_once_with(
        start=new_gap_data['date_from'].date().isoformat(),
        end=last_date.isoformat(),
        staffDate=last_date.isoformat(),
        ignore_version_change=True,
    )


@pytest.mark.django_db
def test_edit_mandatory_vacation_gap_too_short(company, gap_test):
    person = company.persons['dep2-person']
    base_gap = gap_test.get_base_gap(VacationWorkflow)
    gap = VacationWorkflow(person.id, person.id, None).new_gap(base_gap)

    gap['date_from'] = datetime.datetime(2021, 10, 1)
    gap['date_to'] = datetime.datetime(2021, 10, 15)
    gap['mandatory'] = True
    gap['vacation_updated'] = False

    new_gap_data = gap_test.get_base_gap(VacationWorkflow)
    new_gap_data['date_from'] = datetime.datetime(2021, 10, 1)
    new_gap_data['date_to'] = datetime.datetime(2021, 10, 7)

    with pytest.raises(MandatoryVacationTooShortError):
        VacationWorkflow.init_to_modify(person.id, gap=gap).edit_gap(new_gap_data)


@pytest.mark.django_db
def test_edit_mandatory_vacation_without_validation_when_has_permission(company, gap_test):
    modifier = StaffFactory()

    super_user = UserFactory()
    super_user.has_perm = mock.Mock(side_effect=(lambda perm_code: perm_code == 'gap.can_edit_mandatory_vacation'))

    person = company.persons['dep2-person']
    base_gap = gap_test.get_base_gap(VacationWorkflow)
    gap = VacationWorkflow(person.id, person.id, None).new_gap(base_gap)

    gap['date_from'] = datetime.datetime(2021, 10, 1)
    gap['date_to'] = datetime.datetime(2021, 10, 15)
    gap['mandatory'] = True
    gap['vacation_updated'] = False

    new_gap_data = gap_test.get_base_gap(VacationWorkflow)
    new_gap_data['date_from'] = datetime.datetime(2021, 10, 1)
    new_gap_data['date_to'] = datetime.datetime(2021, 10, 7)

    with mock.patch('staff.gap.workflows.vacation.workflow.Staff.user', super_user):
        VacationWorkflow.init_to_modify(modifier.id, gap=gap).edit_gap(new_gap_data)


@pytest.mark.django_db
def test_edit_mandatory_vacation_with_holidays_gap_too_short(company, gap_test):
    person = company.persons['dep2-person']
    base_gap = gap_test.get_base_gap(VacationWorkflow)
    gap = VacationWorkflow(person.id, person.id, None).new_gap(base_gap)

    gap['date_from'] = datetime.datetime(2021, 10, 1)
    gap['date_to'] = datetime.datetime(2021, 10, 15)
    gap['mandatory'] = True
    gap['vacation_updated'] = False

    new_gap_data = gap_test.get_base_gap(VacationWorkflow)
    new_gap_data['date_from'] = datetime.datetime(2021, 10, 1)
    new_gap_data['date_to'] = datetime.datetime(2021, 10, 16)

    mocked_min_end_date = mock.Mock(return_value=datetime.date(2021, 10, 20))
    with mock.patch('staff.gap.workflows.vacation.workflow.get_min_mandatory_vacation_date_to', mocked_min_end_date):
        with pytest.raises(MandatoryVacationWithHolidaysTooShortError):
            VacationWorkflow.init_to_modify(person.id, gap=gap).edit_gap(new_gap_data)


@pytest.mark.django_db
def test_edit_mandatory_vacation_gap_moved_next_year(company, gap_test):
    person = company.persons['dep2-person']
    base_gap = gap_test.get_base_gap(VacationWorkflow)
    gap = VacationWorkflow(person.id, person.id, None).new_gap(base_gap)

    gap['date_from'] = datetime.datetime(2021, 10, 1)
    gap['date_to'] = datetime.datetime(2021, 10, 15)
    gap['mandatory'] = True
    gap['vacation_updated'] = False

    new_gap_data = gap_test.get_base_gap(VacationWorkflow)
    new_gap_data['date_from'] = datetime.datetime(2022, 10, 1)
    new_gap_data['date_to'] = datetime.datetime(2022, 10, 15)

    with pytest.raises(MandatoryVacationYearChangedError):
        VacationWorkflow.init_to_modify(person.id, gap=gap).edit_gap(new_gap_data)


@pytest.mark.django_db
def test_vacation_gap_params_without_hr(company, gap_test):
    person = company.persons['yandex-person']
    base_gap = gap_test.get_base_gap(VacationWorkflow)

    workflow = VacationWorkflow(
        person_id=person.id,
        modifier_id=person.id,
        gap=None,
    )
    workflow.new_gap(base_gap)
    received_issue_params = workflow.get_initial_issue_params()

    expected_issue_params = {
        'hr': None,
        'currentHead': company.persons['yandex-chief'].login,
        'legalEntity2': person.organization.st_translation_id,
    }
    assert received_issue_params == expected_issue_params


@pytest.mark.django_db
def test_vacation_mandatory_gap_params(gap_test, company):
    person = company.persons['dep2-person']

    base_gap = gap_test.get_base_gap(VacationWorkflow)
    base_gap['mandatory'] = True

    workflow = VacationWorkflow(
        person_id=person.id,
        modifier_id=person.id,
        gap=None,
    )
    workflow.new_gap(base_gap)

    initial_issue_params = workflow.get_initial_issue_params()
    assert initial_issue_params[settings.STARTREK_MANDATORY_GAP_FIELD] == 'Y'


@pytest.mark.django_db
@mock.patch('staff.gap.edit_views.gap_state_views.get_accessible_logins', mock.Mock(return_value=True))
@mock.patch('staff.gap.edit_views.gap_state_views.get_short_person_with_department', mock.Mock())
@mock.patch('staff.gap.edit_views.gap_state_views.can_see_gap', mock.Mock(return_value=True))
@mock.patch('staff.gap.workflows.vacation.workflow.VacationWorkflow.init_to_modify', mock.Mock())
def test_cancel_mandatory_vacation_no_permission():
    ya_user = UserFactory()
    ya_user.get_profile = mock.Mock()
    ya_user.has_perm = mock.Mock(return_value=False)
    fake_gap = {
        'person_login': 'uhura',
        'workflow': 'vacation',
        'mandatory': True,
    }
    with mock.patch('staff.gap.controllers.gap.GapCtl.find_gap_by_id', mock.Mock(return_value=fake_gap)):
        res = gap_action(ya_user, 123, 'cancel_gap')
        assert json.loads(res.content) == MandatoryVacationCancelError().error_dict


@pytest.mark.django_db
@mock.patch('staff.gap.edit_views.gap_state_views.get_accessible_logins', mock.Mock(return_value=True))
@mock.patch('staff.gap.edit_views.gap_state_views.get_short_person_with_department', mock.Mock())
@mock.patch('staff.gap.edit_views.gap_state_views.can_see_gap', mock.Mock(return_value=True))
def test_cancel_mandatory_vacation_has_permission():
    ya_user = UserFactory()
    ya_user.get_profile = mock.Mock()
    ya_user.has_perm = mock.Mock(return_value=True)
    fake_gap = {
        'person_login': 'uhura',
        'workflow': 'vacation',
        'mandatory': True,
    }

    mocked_workflow = mock.Mock()
    workflow_patch = mock.patch(
        'staff.gap.workflows.vacation.workflow.VacationWorkflow.init_to_modify',
        mock.Mock(return_value=mocked_workflow),
    )

    with mock.patch('staff.gap.controllers.gap.GapCtl.find_gap_by_id', mock.Mock(return_value=fake_gap)):
        with workflow_patch:
            mocked_cancel = mock.Mock()
            mocked_workflow.cancel_gap = mocked_cancel
            gap_action(ya_user, 123, 'cancel_gap')
            mocked_workflow.cancel_gap.assert_called_once()
