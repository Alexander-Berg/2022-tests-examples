import pytest

from datetime import date
from unittest.mock import patch, ANY, Mock

from intranet.femida.src.offers.choices import EMPLOYEE_TYPES
from intranet.femida.src.offers.controllers import OfferCtl
from intranet.femida.src.offers.newhire.serializers import (
    NewhireOfferRemoteSerializer,
    NewhireOfferStoredSerializer,
)

from intranet.femida.tests import factories as f
from intranet.femida.tests.mock.offers import FakeNewhireAPI
from intranet.femida.tests.utils import eager_task


@patch('intranet.femida.src.offers.controllers.NewhireAPI', FakeNewhireAPI)
def test_save_in_newhire():
    offer = f.OfferFactory()
    ctl = OfferCtl(offer)
    ctl.save_in_newhire()

    assert offer.newhire_id is not None


@patch('intranet.femida.src.offers.controllers.NewhireAPI', FakeNewhireAPI)
def test_update_in_newhire():
    offer = f.OfferFactory(newhire_id=1)
    ctl = OfferCtl(offer)
    data = {
        'join_at': date(2018, 2, 1),
        'username': 'username',
        'employee_type': EMPLOYEE_TYPES.current,
    }
    expected_data = dict(
        data,
        join_at='2018-02-01',
    )
    ctl.update_in_newhire(data)
    assert all(
        ctl.newhire_data[k] == expected_data[k]
        for k in data if k in ctl.newhire_data
    )


@patch('intranet.femida.src.offers.controllers.NewhireAPI', FakeNewhireAPI)
def test_cancel_in_newhire():
    offer = f.OfferFactory(newhire_id=1)
    ctl = OfferCtl(offer)
    ctl.cancel_in_newhire()


@pytest.mark.parametrize('data', (
    {
        'candidate_type': 'NEW_EMPLOYEE',
        'login': 'username',
        'hdrfs_ticket': 'HDRFS-123',
        'join_at': '2019-01-01',
        'newhire_status': 'NEW',
    },
    {
        'candidate_type': 'FORMER_EMPLOYEE',
        'login': 'abc123',
        'newhire_status': 'APPROVED',
    },
    {
        'candidate_type': 'EXTERNAL_EMPLOYEE',
        'login': 'curemp',
        'join_at': '2019-01-01',
        'newhire_status': 'READY',
    },
    {
        'hdrfs_ticket': 'HDRFS-3',
        'newhire_status': 'CLOSED',
    },
    {
        'join_at': '2020-02-01',
        'newhire_status': 'CANCELLED',
    },
))
def test_update_from_newhire(data):
    offer = f.OfferFactory(newhire_id=1)
    ctl = OfferCtl(offer)
    ctl._update_from_newhire(data)

    remote_data = NewhireOfferRemoteSerializer(data, partial=True).data
    stored_data = NewhireOfferStoredSerializer(offer).data

    is_updated = not any(
        (v or stored_data[k]) and v != stored_data[k]
        for k, v in remote_data.items()
    )
    assert is_updated


@patch('intranet.femida.src.offers.controllers.IssueUpdateOperation')
@patch('intranet.femida.src.offers.controllers.update_oebs_login_task.delay')
@patch('intranet.femida.src.offers.controllers.add_issue_comment_task.delay')
def test_update_username_from_newhire(mocked_add_issue_comment, mocked_update_oebs_login,
                                      mocked_update_issue):
    offer = f.OfferFactory(
        newhire_id=1,
        oebs_person_id=10,
        username='username',
        startrek_hr_key='HR-1',
        startrek_eds_key='EDS-1',
        startrek_relocation_key='RELOCATION-1',
        startrek_signup_key='SIGNUP-1',
        startrek_bootcamp_key='BOOTCAMP-1',
        startrek_adaptation_key='ADAPTATION-1',
    )
    ctl = OfferCtl(offer)

    new_username = 'new-username'
    ctl._update_from_newhire({'login': new_username})

    offer.refresh_from_db()
    assert offer.username == new_username

    # Проверяем, что изменение логина улетает во все возможные тикеты
    mocked_add_issue_comment.assert_any_call('ADAPTATION-1', ANY)
    mocked_add_issue_comment.assert_any_call('RELOCATION-1', ANY)
    mocked_add_issue_comment.assert_any_call('SIGNUP-1', ANY)
    mocked_add_issue_comment.assert_any_call('BOOTCAMP-1', ANY)

    mocked_update_issue.assert_any_call('HR-1')
    mocked_update_issue.assert_any_call('EDS-1')
    mocked_update_issue().delay.assert_called_with(comment=ANY, userLogin=new_username)
    assert mocked_update_issue().delay.call_count == 2

    # Проверяем, что изменение логина улетает в Я.Найм
    mocked_update_oebs_login.assert_called_once_with(person_id=10, login=new_username)


@patch('intranet.femida.src.offers.controllers.update_oebs_assignment_task.delay')
@patch('intranet.femida.src.offers.controllers.update_issue_task.delay')
@patch('intranet.femida.src.offers.controllers.IssueUpdateOperation')
def test_update_join_at_from_newhire(mocked_issue_update_operation, mocked_issue_update_task,
                                     mocked_update_oebs_assignment):
    rotation = f.RotationFactory(
        startrek_rotation_key='ROTATION-1',
        startrek_myrotation_key='MYROTATION-1',
    )
    application = f.ApplicationFactory(
        vacancy__startrek_key='JOB-1',
        consideration__rotation=rotation,
    )
    offer = f.OfferFactory(
        newhire_id=1,
        oebs_person_id=10,
        join_at=date(2020, 5, 1),
        application=application,
        startrek_hr_key='HR-1',
        startrek_eds_key='EDS-1',
        startrek_relocation_key='RELOCATION-1',
        startrek_signup_key='SIGNUP-1',
        startrek_bootcamp_key='BOOTCAMP-1',
        startrek_adaptation_key='ADAPTATION-1',
    )
    ctl = OfferCtl(offer)

    new_join_at = '2020-04-01'
    ctl._update_from_newhire({'join_at': new_join_at})

    offer.refresh_from_db()
    assert offer.join_at.isoformat() == new_join_at

    # Проверяем, что изменение даты выхода улетает во все возможные тикеты
    mocked_issue_update_operation.assert_called_once_with('JOB-1')
    mocked_issue_update_operation().delay.assert_called_once_with(start=new_join_at)

    mocked_issue_update_task.assert_any_call(keys='HR-1', start=new_join_at, comment=ANY)
    mocked_issue_update_task.assert_any_call(keys='EDS-1', start=new_join_at, comment=ANY)
    mocked_issue_update_task.assert_any_call(keys='RELOCATION-1', start=new_join_at, comment=ANY)
    mocked_issue_update_task.assert_any_call(keys='SIGNUP-1', start=new_join_at, comment=ANY)
    mocked_issue_update_task.assert_any_call(keys='BOOTCAMP-1', start=new_join_at, comment=ANY)
    assert any(c[1] and c[1][0] == 'ADAPTATION-1' for c in mocked_issue_update_task.mock_calls)

    # Проверяем, что изменение даты выхода улетает в Я.Найм
    mocked_update_oebs_assignment.assert_called_once_with(offer.id)


@eager_task('intranet.femida.src.offers.controllers.link_offer_issues_task')
@patch('intranet.femida.src.offers.tasks.get_issue')
@patch('intranet.femida.src.offers.controllers.is_adaptation_needed', Mock(return_value=True))
def test_update_hdrfs_from_newhire(mocked_get_issue, mocked_task):
    offer = f.OfferFactory(
        newhire_id=100500,
        startrek_adaptation_key='ADAPTATION-100500',
    )
    ctl = OfferCtl(offer)
    ctl._update_from_newhire({'hdrfs_ticket': 'HDRFS-100500'})

    offer.refresh_from_db()
    assert offer.startrek_hdrfs_key == 'HDRFS-100500'
    mocked_get_issue.assert_called_once_with('HDRFS-100500')
    mocked_get_issue().links.create.assert_called_once_with('relates', 'ADAPTATION-100500')
