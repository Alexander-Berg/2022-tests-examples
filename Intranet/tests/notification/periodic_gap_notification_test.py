import pytest
from mock import patch

from staff.gap.workflows.office_work.workflow import OfficeWorkWorkflow
from staff.gap.workflows.remote_work.workflow import RemoteWorkWorkflow


@pytest.mark.parametrize('workflow_cls', [OfficeWorkWorkflow, RemoteWorkWorkflow])
@pytest.mark.django_db
def test_new_periodic_gap_email(gap_test, company, templates_ctl, workflow_cls):
    templates_ctl.new(type='email', tag='new_periodic_gap', template='new_gap_email_template')
    templates_ctl.new(type='email', tag='new_periodic_gap_chief', template='new_gap_chief_email_template')

    base_periodic_gap = gap_test.get_base_periodic_gap(workflow_cls)

    base_periodic_gap.update({
        'person_login': company.persons['dep12-person'].login,
        'person_id': company.persons['dep12-person'].id,
    })

    periodic_gap = workflow_cls(
        modifier_id=gap_test.DEFAULT_MODIFIER_ID,
        person_id=base_periodic_gap['person_id'],
    )

    with patch('staff.gap.controllers.email_ctl.EmailCtl._send') as mock:

        periodic_gap.new_periodic_gap(base_periodic_gap)

        chief = company.persons['dep12-chief']
        ((person, modifier, to_send, gap_diff), kwargs) = mock.call_args
        assert 1 == len(to_send)

        to_send_records = []

        for item in to_send:
            records = item['to_send']
            assert 1 == len(records)
            to_send_records.append(records[0])

        assert any(record['tag'] == 'new_periodic_gap_chief' for record in to_send_records)
        assert any(record['email'] == chief.work_email for record in to_send_records)
