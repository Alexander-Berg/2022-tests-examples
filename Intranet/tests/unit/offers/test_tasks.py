from datetime import timedelta

import pytest
from unittest.mock import patch

from constance import config
from django.utils import timezone

from intranet.femida.src.offers.choices import (
    OFFER_STATUSES,
    OFFER_DOCS_PROCESSING_STATUSES,
    OFFER_DOCS_PROCESSING_RESOLUTIONS,
    EMPLOYEE_TYPES,
)
from intranet.femida.src.offers.tasks import (
    handle_expired_doc_requests_task,
    set_issues_employee_task,
    update_offer_current_dismissed_to_former_task,
)
from intranet.femida.src.utils.datetime import shifted_now

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


@patch('intranet.femida.src.offers.tasks.notify_hr_about_docs_processing_problem')
@patch('intranet.femida.src.offers.controllers.create_oebs_person_and_assignment')
def test_handle_expired_doc_requests_task(mocked_create_oebs_person_and_assignment,
                                          mocked_notify_hr):
    offer = f.create_offer(
        status=OFFER_STATUSES.accepted,
        docs_processing_status=OFFER_DOCS_PROCESSING_STATUSES.need_information,
        passport_data={},
    )

    handle_expired_doc_requests_task()
    offer.refresh_from_db()

    assert offer.docs_processing_status == OFFER_DOCS_PROCESSING_STATUSES.finished
    assert offer.docs_processing_resolution == OFFER_DOCS_PROCESSING_RESOLUTIONS.expired
    mocked_create_oebs_person_and_assignment.assert_called_once()
    mocked_notify_hr.assert_called_once_with(offer)


@pytest.mark.parametrize('use_timestamp', (True, False))
@patch('intranet.femida.src.offers.tasks.IssueUpdateOperation')
def test_set_issues_employee_task(mocked_update_operation, use_timestamp):
    common_fields = {
        'status': OFFER_STATUSES.closed,
        'startrek_hr_key': 'HR-1',
    }
    with patch('intranet.femida.src.utils.datetime.timezone.now', return_value=timezone.now()):
        timestamp = shifted_now(
            hours=-config.TRACKER_STAFF_SYNC_TIME - 1,
        ).replace(minute=0, second=0, microsecond=0)
        offer = f.create_offer(closed_at=timestamp + timedelta(minutes=30), **common_fields)
        older_offer = f.create_offer(closed_at=timestamp - timedelta(minutes=30), **common_fields)
        f.create_offer(closed_at=timestamp + timedelta(hours=1, minutes=30), **common_fields)

        if use_timestamp:
            dt = timestamp - timedelta(hours=1)
            set_issues_employee_task(offer_closed_time=dt.strftime('%Y-%m-%d %H'))
            mocked_update_operation.assert_called_once_with(older_offer.startrek_hr_key)
        else:
            set_issues_employee_task()
            mocked_update_operation.assert_called_once_with(offer.startrek_hr_key)


@pytest.mark.parametrize('is_user_dismissed, newhire_id, expected_employee_type', (
    (True, None, EMPLOYEE_TYPES.former),
    (True, 1, EMPLOYEE_TYPES.current),
    (False, None, EMPLOYEE_TYPES.current),
))
@patch('intranet.femida.src.offers.tasks.update_newhire_employee_type_task.delay')
def test_update_offer_current_dismissed_to_former_task(mocked_task, is_user_dismissed, newhire_id,
                                                       expected_employee_type):
    user = f.create_user(is_dismissed=is_user_dismissed)
    offer = f.create_offer(
        employee_type=EMPLOYEE_TYPES.current,
        newhire_id=newhire_id,
        username=user.username,
        status=OFFER_STATUSES.accepted,
    )
    update_offer_current_dismissed_to_former_task()
    offer.refresh_from_db()
    assert offer.employee_type == expected_employee_type
    if newhire_id is not None:
        mocked_task.assert_called()
    else:
        mocked_task.assert_not_called()
