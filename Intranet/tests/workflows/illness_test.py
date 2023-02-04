import pytest
import mock

from staff.gap.controllers.gap import GapCtl
from staff.gap.controllers.templates import TemplatesCtl
from staff.gap.workflows.illness.workflow import IllnessWorkflow
from staff.gap.workflows.choices import GAP_STATES as GS


@pytest.mark.django_db
def test_new_gap(gap_test):
    now = gap_test.mongo_now()

    base_gap = gap_test.get_base_gap(IllnessWorkflow)
    base_gap['has_sicklist'] = True

    gap = IllnessWorkflow(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
        None,
    ).new_gap(base_gap)

    gap_test.base_assert_new_gap(IllnessWorkflow, now, base_gap, gap)

    assert gap['id'] == 1
    assert gap['state'] == GS.NEW
    assert gap['created_by_id'] == gap_test.DEFAULT_MODIFIER_ID
    assert gap['modified_by_id'] == gap_test.DEFAULT_MODIFIER_ID
    assert gap['has_sicklist'] is True
    assert gap['is_covid'] is False


@pytest.mark.django_db
def test_new_gap_with_covid(gap_test):
    now = gap_test.mongo_now()

    base_gap = gap_test.get_base_gap(IllnessWorkflow)
    base_gap['has_sicklist'] = True
    base_gap['is_covid'] = True

    gap = IllnessWorkflow(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
        None,
    ).new_gap(base_gap)

    gap_test.base_assert_new_gap(IllnessWorkflow, now, base_gap, gap)

    assert gap['id'] == 1
    assert gap['state'] == GS.NEW
    assert gap['created_by_id'] == gap_test.DEFAULT_MODIFIER_ID
    assert gap['modified_by_id'] == gap_test.DEFAULT_MODIFIER_ID
    assert gap['has_sicklist'] is True
    assert gap['is_covid'] is True


@pytest.mark.django_db
@mock.patch('staff.gap.workflows.startrek_mixin.StartrekCtl')
def test_gap_master_issue(patched_startrek_ctl, gap_test):
    now = gap_test.mongo_now()

    gap_data = gap_test.get_base_gap(IllnessWorkflow)
    gap_data['has_sicklist'] = False
    initial_workflow = IllnessWorkflow(gap_test.DEFAULT_MODIFIER_ID, gap_test.test_person.id, None)
    gap = initial_workflow.new_gap(gap_data)

    gap_test.base_assert_new_gap(IllnessWorkflow, now, gap_data, gap)
    assert gap['has_sicklist'] is False
    assert gap['master_issue'] is None

    TemplatesCtl().new(
        type='st_issue',
        tag='new_illness',
        template={
            'queue': 'test queue',
            'summary': 'test summary',
            'description': 'test covid description',
        }
    )
    fake_issue = {'key': 'ISSUE_KEY'}
    patched_startrek_ctl.return_value.create.return_value = fake_issue

    edit_data = gap_test.get_base_gap(IllnessWorkflow)
    edit_data['has_sicklist'] = True
    edit_workflow = IllnessWorkflow(gap_test.DEFAULT_MODIFIER_ID, gap_test.test_person.id, gap)
    edit_workflow.edit_gap(edit_data)

    gap = GapCtl().find_gap_by_id(gap['id'])

    assert gap['has_sicklist'] is True
    assert gap['master_issue'] == fake_issue['key']

    new_gap_data = gap_test.get_base_gap(IllnessWorkflow)
    new_gap_data['has_sicklist'] = True
    new_workflow = IllnessWorkflow(gap_test.DEFAULT_MODIFIER_ID, gap_test.test_person.id, None)
    new_gap = new_workflow.new_gap(new_gap_data)

    gap_test.base_assert_new_gap(IllnessWorkflow, now, new_gap_data, new_gap)
    new_gap = GapCtl().find_gap_by_id(new_gap['id'])

    assert new_gap['has_sicklist'] is True
    assert new_gap['master_issue'] == fake_issue['key']


@pytest.mark.django_db
@mock.patch('staff.gap.workflows.startrek_mixin.StartrekCtl')
def test_gap_covid_issue(patched_startrek_ctl, gap_test):
    now = gap_test.mongo_now()

    gap_data = gap_test.get_base_gap(IllnessWorkflow)
    gap_data['is_covid'] = False
    initial_workflow = IllnessWorkflow(gap_test.DEFAULT_MODIFIER_ID, gap_test.test_person.id, None)
    gap = initial_workflow.new_gap(gap_data)

    gap_test.base_assert_new_gap(IllnessWorkflow, now, gap_data, gap)
    assert gap['is_covid'] is False
    assert gap['master_issue'] is None
    assert gap['covid_issue'] is None

    TemplatesCtl().new(
        type='st_issue',
        tag='new_illness_covid',
        template={
            'queue': 'test covid queue',
            'summary': 'test covid summary',
            'description': 'test covid description',
        }
    )
    fake_issue = {'key': 'COVID_ISSUE_KEY'}
    patched_startrek_ctl.return_value.create.return_value = fake_issue

    edit_data = gap_test.get_base_gap(IllnessWorkflow)
    edit_data['is_covid'] = True
    edit_workflow = IllnessWorkflow(gap_test.DEFAULT_MODIFIER_ID, gap_test.test_person.id, gap)
    edit_workflow.edit_gap(edit_data)

    gap = GapCtl().find_gap_by_id(gap['id'])

    assert gap['is_covid'] is True
    assert gap['master_issue'] is None
    assert gap['covid_issue'] == fake_issue['key']

    new_gap_data = gap_test.get_base_gap(IllnessWorkflow)
    new_gap_data['is_covid'] = True
    new_workflow = IllnessWorkflow(gap_test.DEFAULT_MODIFIER_ID, gap_test.test_person.id, None)
    new_gap = new_workflow.new_gap(new_gap_data)

    gap_test.base_assert_new_gap(IllnessWorkflow, now, new_gap_data, new_gap)
    new_gap = GapCtl().find_gap_by_id(new_gap['id'])

    assert new_gap['is_covid'] is True
    assert new_gap['master_issue'] is None
    assert new_gap['covid_issue'] == fake_issue['key']

    cancel_covid_data = gap_test.get_base_gap(IllnessWorkflow)
    cancel_covid_data['is_covid'] = False
    cancel_covid_workflow = IllnessWorkflow(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
        new_gap,
    )
    cancel_covid_workflow.edit_gap(cancel_covid_data)

    assert new_gap['is_covid'] is False
    assert new_gap['master_issue'] is None
    assert new_gap['covid_issue'] == fake_issue['key']

    TemplatesCtl().new(
        type='st_issue',
        tag='new_illness_covid',
        template={
            'queue': 'test covid queue',
            'summary': 'test covid summary',
            'description': 'test covid description',
        }
    )
    fake_issue_new = {'key': 'COVID_ISSUE_KEY_NEW'}
    patched_startrek_ctl.return_value.create.return_value = fake_issue_new

    reset_covid_data = gap_test.get_base_gap(IllnessWorkflow)
    reset_covid_data['is_covid'] = True
    reset_covid_workflow = IllnessWorkflow(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
        new_gap,
    )
    reset_covid_workflow.edit_gap(reset_covid_data)
    new_gap = GapCtl().find_gap_by_id(new_gap['id'])

    assert new_gap['is_covid'] is True
    assert new_gap['master_issue'] is None
    assert new_gap['covid_issue'] == fake_issue_new['key']
