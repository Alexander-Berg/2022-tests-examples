from datetime import datetime

import pytest
from mock import Mock, ANY, call, patch

from staff.departments.models import DepartmentRoles
from staff.lib.testing import DepartmentStaffFactory

from staff.gap.workflows.absence.workflow import AbsenceWorkflow
from staff.gap.workflows.choices import GAP_STATES as GS


@pytest.mark.django_db
def test_new_gap(gap_test, company):
    now = gap_test.mongo_now()

    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap.update({
        'person_login': company.persons['dep12-person'].login,
        'person_id': company.persons['dep12-person'].id,
    })

    gap = AbsenceWorkflow(
        modifier_id=gap_test.DEFAULT_MODIFIER_ID,
        person_id=base_gap['person_id'],
    ).new_gap(base_gap)

    gap_test.base_assert_new_gap(AbsenceWorkflow, now, base_gap, gap)

    assert gap['id'] == 1
    assert GS.NEW == gap['state']
    assert gap['created_by_id'] == gap_test.DEFAULT_MODIFIER_ID
    assert gap['modified_by_id'] == gap_test.DEFAULT_MODIFIER_ID


@pytest.mark.django_db
def test_person_followers(gap_test, company):
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap.update({
        'person_login': company.persons['dep11-person'].login,
        'person_id': company.persons['dep11-person'].id,
    })

    workflow = AbsenceWorkflow(
        modifier_id=gap_test.DEFAULT_MODIFIER_ID,
        person_id=base_gap['person_id'],
    )

    workflow.new_gap(base_gap)

    followers = workflow._chiefs_followers()
    assert 2 == len(followers)
    assert ['dep11-chief', 'dep1-chief'] == list(followers)


@pytest.mark.django_db
def test_chief_followers(gap_test, company):
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap.update({
        'person_login': company.persons['dep11-chief'].login,
        'person_id': company.persons['dep11-chief'].id,
    })

    workflow = AbsenceWorkflow(
        modifier_id=gap_test.DEFAULT_MODIFIER_ID,
        person_id=base_gap['person_id'],
    )

    workflow.new_gap(base_gap)

    followers = workflow._chiefs_followers()
    assert 2 == len(followers)
    assert ['dep1-chief', 'yandex-chief'] == list(followers)


@pytest.mark.django_db
def test_top_followers(gap_test, company):
    DepartmentStaffFactory(
        role_id=DepartmentRoles.HR_PARTNER.value,
        staff=company.persons['dep11-person'],
        department=company.yandex,
    )

    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap.update({
        'person_login': company.persons['yandex-chief'].login,
        'person_id': company.persons['yandex-chief'].id,
    })

    workflow = AbsenceWorkflow(
        modifier_id=gap_test.DEFAULT_MODIFIER_ID,
        person_id=base_gap['person_id'],
    )

    workflow.new_gap(base_gap)

    followers = workflow._chiefs_followers()
    assert 0 == len(followers)


@pytest.mark.django_db
def test_edit_long_absence(gap_test, company):
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap.update({
        'person_login': company.persons['dep12-person'].login,
        'person_id': company.persons['dep12-person'].id,
    })

    workflow = AbsenceWorkflow(
        modifier_id=gap_test.DEFAULT_MODIFIER_ID,
        person_id=base_gap['person_id'],
    )
    workflow.LONG_ABSENCES_WO_APPROVAL = 0

    workflow.issue_comment = Mock()

    workflow.new_gap(base_gap)
    workflow.edit_gap({'full_day': False})

    workflow.issue_comment.assert_has_calls([call('edit_long_absence', forbidden_statuses=ANY, gap_diff=ANY)])


@pytest.mark.django_db
def test_edit_short_absence(gap_test, company):
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap.update({
        'date_from': datetime(2015, 1, 1, 10, 20),
        'date_to': datetime(2015, 1, 1, 11, 30),
        'full_day': False,
        'person_login': company.persons['dep12-person'].login,
        'person_id': company.persons['dep12-person'].id,
    })

    workflow = AbsenceWorkflow(
        modifier_id=gap_test.DEFAULT_MODIFIER_ID,
        person_id=base_gap['person_id'],
    )

    workflow.issue_comment = Mock()

    workflow.new_gap(base_gap)
    workflow.edit_gap({
        'date_from': datetime(2015, 1, 1, 11, 20),
        'date_to': datetime(2015, 1, 1, 12, 30),
    })

    workflow.issue_comment.assert_not_called()


@pytest.mark.django_db
def test_new_gap_sends_confirm_email_to_step_chief_for_usual_employee(gap_test, company, templates_ctl):
    templates_ctl.new(type='email', tag='new_gap', template='new_gap_email_template')
    templates_ctl.new(type='email', tag='new_gap_chief', template='new_gap_chief_email_template')

    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap.update({
        'person_login': company.persons['dep12-person'].login,
        'person_id': company.persons['dep12-person'].id,
    })

    gap = AbsenceWorkflow(
        modifier_id=gap_test.DEFAULT_MODIFIER_ID,
        person_id=base_gap['person_id'],
    )
    gap.LONG_ABSENCES_WO_APPROVAL = 0

    with patch('staff.gap.controllers.email_ctl.EmailCtl._send') as mock:
        gap.new_gap(base_gap)

        approver = company.persons['dep1-chief']
        chief = company.persons['dep12-chief']

        ((person, modifier, to_send, gap_diff), kwargs) = mock.call_args
        assert 2 == len(to_send)

        to_send_records = []

        for item in to_send:
            records = item['to_send']
            assert 1 == len(records)
            to_send_records.append(records[0])

        assert any(record['tag'] == 'new_gap_chief' for record in to_send_records)
        assert any(record['email'] == approver.work_email for record in to_send_records)

        assert any(record['tag'] == 'new_gap' for record in to_send_records)
        assert any(record['email'] == chief.work_email for record in to_send_records)


@pytest.mark.django_db
def test_new_gap_uses_step_chief_for_confirm_link_for_usual_employee(gap_test, company):
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap.update({
        'person_login': company.persons['dep12-person'].login,
        'person_id': company.persons['dep12-person'].id,
    })

    gap = AbsenceWorkflow(
        modifier_id=gap_test.DEFAULT_MODIFIER_ID,
        person_id=base_gap['person_id'],
    )
    gap.LONG_ABSENCES_WO_APPROVAL = 0

    with patch('staff.gap.workflows.absence.workflow.confirm_by_chief_hash_action') as mock:
        gap.new_gap(base_gap)

        approver = company.persons['dep1-chief']
        mock.assert_has_calls([call(ANY, ANY, ANY, approver.id)])


@pytest.mark.django_db
def test_new_gap_sends_confirm_email_to_direct_chief_when_chief_is_volozh_direct(gap_test, company, templates_ctl):
    templates_ctl.new(type='email', tag='new_gap', template='new_gap_email_template')
    templates_ctl.new(type='email', tag='new_gap_chief', template='new_gap_chief_email_template')

    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap.update({
        'person_login': company.persons['yandex-person'].login,
        'person_id': company.persons['yandex-person'].id,
    })

    gap = AbsenceWorkflow(
        modifier_id=gap_test.DEFAULT_MODIFIER_ID,
        person_id=base_gap['person_id'],
    )

    with patch('staff.gap.controllers.email_ctl.EmailCtl._send') as mock:
        gap.new_gap(base_gap)

        approver = company.persons['yandex-chief']
        ((person, modifier, to_send, gap_diff), kwargs) = mock.call_args
        assert 1 == len(to_send)
        assert 1 == len(to_send[0]['to_send'])
        assert to_send[0]['to_send'][0]['tag'] == 'new_gap_chief'
        assert to_send[0]['to_send'][0]['email'] == approver.work_email


@pytest.mark.django_db
def test_new_gap_uses_direct_chief_for_confirm_link_when_chief_is_volozh_direct(gap_test, company):
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap.update({
        'person_login': company.persons['yandex-person'].login,
        'person_id': company.persons['yandex-person'].id,
    })

    gap = AbsenceWorkflow(
        modifier_id=gap_test.DEFAULT_MODIFIER_ID,
        person_id=base_gap['person_id'],
    )
    gap.LONG_ABSENCES_WO_APPROVAL = 0

    with patch('staff.gap.workflows.absence.workflow.confirm_by_chief_hash_action') as mock:
        gap.new_gap(base_gap)

        approver = company.persons['yandex-chief']
        mock.assert_has_calls([call(ANY, ANY, ANY, approver.id)])


@pytest.mark.django_db
def test_new_gap_not_sends_confirm_email_when_gap_is_for_volozh_direct(gap_test, company):
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap.update({
        'person_login': company.persons['yandex-chief'].login,
        'person_id': company.persons['yandex-chief'].id,
    })

    gap = AbsenceWorkflow(
        modifier_id=gap_test.DEFAULT_MODIFIER_ID,
        person_id=base_gap['person_id'],
    )
    gap.LONG_ABSENCES_WO_APPROVAL = 0

    gap._new_gap_email = Mock()

    gap.new_gap(base_gap)

    gap._new_gap_email.assert_called_once_with(approver=None)


@pytest.mark.django_db
def test_edit_gap_sends_confirm_email_to_step_chief_for_usual_employee(gap_test, company, templates_ctl):
    templates_ctl.new(type='email', tag='edit_gap', template='new_gap_email_template')
    templates_ctl.new(type='email', tag='edit_gap_chief', template='new_gap_chief_email_template')

    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap.update({
        'person_login': company.persons['dep12-person'].login,
        'person_id': company.persons['dep12-person'].id,
    })

    gap = AbsenceWorkflow(
        modifier_id=gap_test.DEFAULT_MODIFIER_ID,
        person_id=base_gap['person_id'],
    )
    gap.LONG_ABSENCES_WO_APPROVAL = 0

    gap.new_gap(base_gap)

    with patch('staff.gap.controllers.email_ctl.EmailCtl._send') as mock:
        gap.edit_gap(data={'work_in_absence': False})

        approver = company.persons['dep1-chief']
        chief = company.persons['dep12-chief']

        ((person, modifier, to_send, gap_diff), kwargs) = mock.call_args
        assert 2 == len(to_send)

        to_send_records = []

        for item in to_send:
            records = item['to_send']
            assert 1 == len(records)
            to_send_records.append(records[0])

        assert any(record['tag'] == 'edit_gap_chief' for record in to_send_records)
        assert any(record['email'] == approver.work_email for record in to_send_records)

        assert any(record['tag'] == 'edit_gap' for record in to_send_records)
        assert any(record['email'] == chief.work_email for record in to_send_records)


@pytest.mark.django_db
def test_edit_gap_sends_confirm_email_to_direct_chief_when_chief_is_volozh_direct(gap_test, company, templates_ctl):
    templates_ctl.new(type='email', tag='edit_gap', template='new_gap_email_template')
    templates_ctl.new(type='email', tag='edit_gap_chief', template='new_gap_chief_email_template')

    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap.update({
        'person_login': company.persons['yandex-person'].login,
        'person_id': company.persons['yandex-person'].id,
    })

    gap = AbsenceWorkflow(
        modifier_id=gap_test.DEFAULT_MODIFIER_ID,
        person_id=base_gap['person_id'],
    )

    gap.new_gap(base_gap)

    with patch('staff.gap.controllers.email_ctl.EmailCtl._send') as mock:
        gap.edit_gap(data={'work_in_absence': False})

        approver = company.persons['yandex-chief']
        ((person, modifier, to_send, gap_diff), kwargs) = mock.call_args
        assert 1 == len(to_send)
        assert 1 == len(to_send[0]['to_send'])
        assert to_send[0]['to_send'][0]['tag'] == 'edit_gap_chief'
        assert to_send[0]['to_send'][0]['email'] == approver.work_email


@pytest.mark.django_db
def test_cancel_gap_sends_confirm_email_to_step_chief_for_usual_employee(gap_test, company, templates_ctl):
    templates_ctl.new(type='email', tag='cancel_gap', template='new_gap_email_template')
    templates_ctl.new(type='email', tag='cancel_gap_chief', template='new_gap_chief_email_template')

    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap.update({
        'person_login': company.persons['dep12-person'].login,
        'person_id': company.persons['dep12-person'].id,
    })

    gap = AbsenceWorkflow(
        modifier_id=gap_test.DEFAULT_MODIFIER_ID,
        person_id=base_gap['person_id'],
    )
    gap.LONG_ABSENCES_WO_APPROVAL = 0

    gap.new_gap(base_gap)

    with patch('staff.gap.controllers.email_ctl.EmailCtl._send') as mock:
        gap.cancel_gap()

        approver = company.persons['dep1-chief']
        chief = company.persons['dep12-chief']

        ((person, modifier, to_send, gap_diff), kwargs) = mock.call_args
        assert 2 == len(to_send)

        to_send_records = []

        for item in to_send:
            records = item['to_send']
            assert 1 == len(records)
            to_send_records.append(records[0])

        assert any(record['tag'] == 'cancel_gap_chief' for record in to_send_records)
        assert any(record['email'] == approver.work_email for record in to_send_records)

        assert any(record['tag'] == 'cancel_gap' for record in to_send_records)
        assert any(record['email'] == chief.work_email for record in to_send_records)


@pytest.mark.django_db
def test_cancel_gap_sends_confirm_email_to_direct_chief_when_chief_is_volozh_direct(gap_test, company, templates_ctl):
    templates_ctl.new(type='email', tag='cancel_gap', template='new_gap_email_template')
    templates_ctl.new(type='email', tag='cancel_gap_chief', template='new_gap_chief_email_template')

    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap.update({
        'person_login': company.persons['yandex-person'].login,
        'person_id': company.persons['yandex-person'].id,
    })

    gap = AbsenceWorkflow(
        modifier_id=gap_test.DEFAULT_MODIFIER_ID,
        person_id=base_gap['person_id'],
    )

    gap.new_gap(base_gap)

    with patch('staff.gap.controllers.email_ctl.EmailCtl._send') as mock:
        gap.cancel_gap()

        approver = company.persons['yandex-chief']
        ((person, modifier, to_send, gap_diff), kwargs) = mock.call_args
        assert 1 == len(to_send)
        assert 1 == len(to_send[0]['to_send'])
        assert to_send[0]['to_send'][0]['tag'] == 'cancel_gap_chief'
        assert to_send[0]['to_send'][0]['email'] == approver.work_email


@pytest.mark.django_db
def test_edit_gap_confirms_gap_when_gap_is_for_volozh_direct(gap_test, company):
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap.update({
        'person_login': company.persons['yandex-chief'].login,
        'person_id': company.persons['yandex-chief'].id,
    })

    gap = AbsenceWorkflow(modifier_id=gap_test.DEFAULT_MODIFIER_ID, person_id=base_gap['person_id'])
    gap.LONG_ABSENCES_WO_APPROVAL = 0
    gap.new_gap(base_gap)
    assert gap.gap['state'] == GS.CONFIRMED
    gap.confirm_gap = Mock()

    gap.edit_gap(data={'work_in_absence': False})
    gap.confirm_gap.assert_called_once()


def new_absence_gap(gap_test, person):
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap.update({
        'person_login': person.login,
        'person_id': person.id,
    })

    return AbsenceWorkflow(modifier_id=gap_test.DEFAULT_MODIFIER_ID, person_id=base_gap['person_id']).new_gap(base_gap)


@pytest.mark.django_db
def test_person_can_create_up_to_three_long_absences_wo_approval(gap_test, company):
    gap1 = new_absence_gap(gap_test, company.persons['dep12-person'])
    gap2 = new_absence_gap(gap_test, company.persons['dep12-person'])
    gap3 = new_absence_gap(gap_test, company.persons['dep12-person'])

    assert not gap1['need_approval']
    assert not gap2['need_approval']
    assert not gap3['need_approval']


@pytest.mark.django_db
def test_fourth_long_absence_needs_approval(gap_test, company):
    new_absence_gap(gap_test, company.persons['dep12-person'])
    new_absence_gap(gap_test, company.persons['dep12-person'])
    new_absence_gap(gap_test, company.persons['dep12-person'])
    gap = new_absence_gap(gap_test, company.persons['dep12-person'])

    assert gap['need_approval']


@pytest.mark.django_db
def test_long_absence_approval_logic_doesnt_intersects_with_other_people(gap_test, company):
    new_absence_gap(gap_test, company.persons['dep11-person'])
    new_absence_gap(gap_test, company.persons['dep1-person'])

    gap1 = new_absence_gap(gap_test, company.persons['dep12-person'])
    gap2 = new_absence_gap(gap_test, company.persons['dep12-person'])
    gap3 = new_absence_gap(gap_test, company.persons['dep12-person'])
    gap4 = new_absence_gap(gap_test, company.persons['dep12-person'])

    assert not gap1['need_approval']
    assert not gap2['need_approval']
    assert not gap3['need_approval']
    assert gap4['need_approval']
