from datetime import datetime, timedelta

import pytest

from staff.gap.workflows.trip.workflow import TripWorkflow
from staff.person.models import Staff

from staff.person_filter.controller.f_120_gap import Equal


@pytest.mark.django_db
def test_equal_returns_proper_q_for_trip_workflow(gap_test, company):
    class WorkflowStub(object):
        name = None

    base_gap = gap_test.get_base_gap(TripWorkflow)

    base_gap.update({
        'person_login': company.persons['dep12-person'].login,
        'person_id': company.persons['dep12-person'].id,
        'form_key': None,
        'date_from': datetime.now() - timedelta(days=1),
        'date_to': datetime.now() + timedelta(days=1)
    })

    gap = TripWorkflow(modifier_id=gap_test.DEFAULT_MODIFIER_ID, person_id=base_gap['person_id'])
    gap.tq_add_gap(base_gap)
    gap.tq_confirm_gap()

    workflow_stub = WorkflowStub()
    workflow_stub.name = 'trip'
    e = Equal()
    e.cleaned_data = {'workflow': workflow_stub, 'will_work_in_absence': True}

    q = e.q()
    staffs = list(Staff.objects.filter(q))
    assert len(staffs) == 1
    assert staffs[0].login == company.persons['dep12-person'].login
