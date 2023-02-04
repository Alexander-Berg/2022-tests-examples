import pytest
import uuid

from constance.test import override_config
from unittest.mock import patch, ANY, Mock

from django.conf import settings
from django.contrib.auth.models import Permission
from django.db.models import F
from django.test import override_settings
from django.urls import reverse

from intranet.femida.src.hire_orders.choices import HIRE_ORDER_STATUSES, HIRE_ORDER_RESOLUTIONS
from intranet.femida.src.hire_orders.models import HireOrder
from intranet.femida.src.utils import datetime

from intranet.femida.tests import factories as f

from ..hire_orders.conftest import *  # noqa


pytestmark = pytest.mark.django_db


def test_hire_order_create_with_perm(client):
    user = f.create_user_with_perm('can_use_hire_orders')
    client.login(user.username)
    url = reverse('private-api:hire_orders:list')
    response = client.post(url)
    assert response.status_code == 400
    assert HireOrder.objects.exists()


def test_hire_order_create_without_perm(client):
    user = f.create_user()
    client.login(user.username)
    url = reverse('private-api:hire_orders:list')
    response = client.post(url)
    assert response.status_code == 403
    assert not HireOrder.objects.exists()


def test_hire_order_create_invalid_department(su_client, hire_order_raw_data):
    # Подразделение за пределами ветки КПБ
    department = f.DepartmentFactory(ancestors=[settings.YANDEX_DEPARTMENT_ID])
    hire_order_raw_data['vacancy']['department'] = department.url
    url = reverse('private-api:hire_orders:list')
    response = su_client.post(url, hire_order_raw_data)
    assert response.status_code == 400

    response_data = response.json()
    uuid = response_data['uuid']
    hire_order = HireOrder.objects.filter(uuid=uuid).first()

    assert response_data['errors'].keys() == {'vacancy[department]'}

    assert hire_order is not None
    assert hire_order.raw_data == hire_order_raw_data
    assert hire_order.status == HIRE_ORDER_STATUSES.closed
    assert hire_order.resolution == HIRE_ORDER_RESOLUTIONS.incorrect


def test_hire_order_create_invalid_profession(su_client, hire_order_raw_data):
    profession = f.ProfessionFactory()
    hire_order_raw_data['vacancy']['profession'] = profession.id
    url = reverse('private-api:hire_orders:list')
    with override_config(HIRE_ORDER_DISABLED_PROF_SPHERE_IDS=str(profession.professional_sphere_id)):
        response = su_client.post(url, hire_order_raw_data)
    assert response.status_code == 400

    response_data = response.json()
    uuid = response_data['uuid']
    hire_order = HireOrder.objects.filter(uuid=uuid).first()

    assert response_data['errors'].keys() == {'vacancy[profession]'}

    assert hire_order is not None
    assert hire_order.raw_data == hire_order_raw_data
    assert hire_order.status == HIRE_ORDER_STATUSES.closed
    assert hire_order.resolution == HIRE_ORDER_RESOLUTIONS.incorrect


@patch('intranet.femida.src.api.hire_orders.views.HireOrderWorkflow')
def test_hire_order_create(mocked_wf, client, hire_order_raw_data):
    user = f.create_user_with_perm('can_use_hire_orders')
    url = reverse('private-api:hire_orders:list')
    client.login(user.username)
    response = client.post(url, hire_order_raw_data)
    assert response.status_code == 201, response.content

    uuid = response.json()['uuid']
    hire_order = HireOrder.objects.filter(uuid=uuid).first()

    assert hire_order is not None
    mocked_wf.assert_called_once_with(hire_order, user=None)
    mocked_wf().delay.assert_called_once_with('prepare_candidate')
    assert hire_order.created_by == user
    assert hire_order.status == HIRE_ORDER_STATUSES.new
    assert hire_order.resolution == ''
    assert hire_order.raw_data == hire_order_raw_data
    assert hire_order.recruiter.username == hire_order_raw_data['recruiter']


@patch('intranet.femida.src.api.hire_orders.views.TableFlowAPI.get_hire_order_offer_params')
@patch('intranet.femida.src.api.hire_orders.views.HireOrderWorkflow', Mock())
def test_hire_order_create_with_table_flow(mocked_table_flow_api, client,
                                           partial_hire_order_raw_data, table_flow_data):
    mocked_table_flow_api.return_value = table_flow_data
    user = f.create_user_with_perm('can_use_hire_orders')
    url = reverse('private-api:hire_orders:list')
    client.login(user.username)

    expected_offer_data = dict(partial_hire_order_raw_data['offer'], **table_flow_data)
    response = client.post(url, partial_hire_order_raw_data)
    assert response.status_code == 201, response.content

    uuid = response.json()['uuid']
    hire_order = HireOrder.objects.filter(uuid=uuid).first()
    department = partial_hire_order_raw_data['vacancy']['department']

    mocked_table_flow_api.assert_called_once_with(department)
    assert hire_order is not None
    assert hire_order.raw_data['offer'] == expected_offer_data
    assert hire_order.table_flow_data == table_flow_data


@patch('intranet.femida.src.api.hire_orders.views.HireOrderWorkflow.delay')
def test_hire_order_create_duplicate(mocked_wf_delay, client, hire_order_raw_data):
    user = f.create_user_with_perm('can_use_hire_orders')
    another_user = f.create_user_with_perm('can_use_hire_orders')
    url = reverse('private-api:hire_orders:list')
    client.login(user.username)

    response = client.post(url, hire_order_raw_data)
    assert response.status_code == 201, response.content
    mocked_wf_delay.assert_called_once_with('prepare_candidate')
    mocked_wf_delay.reset_mock()

    uuid = response.json()['uuid']

    # Повторяем создание
    response = client.post(url, hire_order_raw_data)
    assert response.status_code == 201, response.content
    assert response.json()['uuid'] == uuid
    assert not mocked_wf_delay.called

    # И ещё раз повторяем с включенным флагом
    f.create_waffle_switch('enable_409_on_hire_order_conflict')
    response = client.post(url, hire_order_raw_data)
    assert response.status_code == 409, response.content
    assert response.json()['uuid'] == uuid
    assert not mocked_wf_delay.called

    # Проверяем точно такой же запрос от другого пользователя
    client.login(another_user.username)
    response = client.post(url, hire_order_raw_data)
    assert response.status_code == 201, response.content
    assert response.json()['uuid'] != uuid
    mocked_wf_delay.assert_called_once_with('prepare_candidate')
    mocked_wf_delay.reset_mock()


@pytest.mark.parametrize('is_creator, with_perm, status_code', (
    (True, True, 200),
    (True, False, 403),
    (False, True, 403),
))
@pytest.mark.parametrize('view_name, http_method', (
    ('detail', 'get'),
    ('history', 'get'),
    ('cancel', 'post'),
))
def test_hire_order_permissions(client, is_creator, with_perm, status_code, view_name, http_method):
    creator = f.create_user()
    user = creator if is_creator else f.create_user()
    if with_perm:
        user.user_permissions.add(Permission.objects.get(codename='can_use_hire_orders'))

    hire_order = HireOrder.objects.create(created_by=creator, raw_data={})
    url = reverse(f'private-api:hire_orders:{view_name}', kwargs={'uuid': hire_order.uuid})
    client.login(user.username)
    http_method = getattr(client, http_method)
    response = http_method(url)

    assert response.status_code == status_code


@override_settings(FEMIDA_EXT_HOST='yandex.ru')
def test_hire_order_detail(su_client):
    hire_order = f.HireOrderFactory(
        vacancy__startrek_key='JOB-1',
        offer__newhire_id=101,
        offer__startrek_hr_key='HR-1',
        offer__startrek_hdrfs_key='HDRFS-1',
        offer__join_at='2021-01-01',
    )
    verification = f.create_actual_verification(
        candidate=hire_order.candidate,
        application=hire_order.application,
    )
    link = f.OfferLinkFactory(offer=hire_order.offer)

    url = reverse('private-api:hire_orders:detail', kwargs={'uuid': hire_order.uuid})
    response = su_client.get(url)
    assert response.status_code == 200

    response_data = response.json()
    assert response_data['uuid'] == str(hire_order.uuid)
    assert response_data['preprofile_id'] == 101
    assert response_data['tracker_job_key'] == 'JOB-1'
    assert response_data['tracker_hr_key'] == 'HR-1'
    assert response_data['tracker_hdrfs_key'] == 'HDRFS-1'
    assert response_data['verification_link'] == verification.link
    assert response_data['verification_status'] == verification.status
    assert response_data['verification_resolution'] == verification.resolution
    assert response_data['hire_link'] == f'https://yandex.ru/hire/offers/{link.uid.hex}'
    assert response_data['join_at'] == '2021-01-01'


def test_hire_order_history_no_order(su_client):
    url = reverse('private-api:hire_orders:history', kwargs={'uuid': uuid.uuid4()})
    response = su_client.get(url)
    assert response.status_code == 404


def test_hire_order_history(su_client):
    statuses = (
        HIRE_ORDER_STATUSES.new,
        HIRE_ORDER_STATUSES.candidate_prepared,
        HIRE_ORDER_STATUSES.vacancy_prepared,
        HIRE_ORDER_STATUSES.offer_sent,
        HIRE_ORDER_STATUSES.closed,
    )
    resolutions = (('',) * (len(statuses) - 1)) + (HIRE_ORDER_RESOLUTIONS.hired,)
    status_iter = iter(statuses)
    resolutions_iter = iter(resolutions)
    hire_order = f.HireOrderFactory(status=next(status_iter), resolution=next(resolutions_iter))
    for status, resolution in zip(status_iter, resolutions_iter):
        hire_order.status = status
        hire_order.resolution = resolution
        hire_order.save()

    expected = [
        {
            'id': ANY,
            'changed_at': ANY,
            'status': status,
            'resolution': resolution,
        }
        for status, resolution in zip(reversed(statuses), reversed(resolutions))
    ]

    url = reverse('private-api:hire_orders:history', kwargs={'uuid': hire_order.uuid})
    response = su_client.get(url)
    assert response.status_code == 200
    assert response.json()['results'] == expected, response.content


@pytest.mark.parametrize('hire_order_status, status_code', (
    (HIRE_ORDER_STATUSES.new, 200),
    (HIRE_ORDER_STATUSES.closed, 403),
))
def test_hire_order_cancel(su_client, hire_order_status, status_code):
    hire_order = f.create_hire_order_for_status(status=hire_order_status)
    url = reverse('private-api:hire_orders:cancel', kwargs={'uuid': hire_order.uuid})
    response = su_client.post(url)

    hire_order.refresh_from_db()
    assert response.status_code == status_code, response.content
    assert hire_order.status == HIRE_ORDER_STATUSES.closed
    if status_code == 200:
        assert hire_order.resolution == HIRE_ORDER_RESOLUTIONS.cancelled


@pytest.mark.parametrize('modified__gte, date_format, candidate_idx, other_params, count', (
    pytest.param(None, None, None, {}, 5, id='full-set'),
    pytest.param(None, None, None, {'superuser': True}, 10, id='su-full-set'),
    pytest.param(1, None, None, {}, 0, id='out-filtered-mdate'),
    pytest.param(-2, None, None, {}, 3, id='partial-out-filtered-mdate'),
    pytest.param(-3, None, 0, {}, 1, id='full-filter'),
    pytest.param(None, None, 0, {}, 1, id='first-candidate'),
    pytest.param(None, None, 1, {}, 1, id='second-candidate'),
    pytest.param(None, None, None, {'page_size': 2}, 2, id='full-set-paginated'),
    pytest.param(-2, ' %H:%M:%S%z', None, {}, 3, id='hh-mm-ss-tz'),
    pytest.param(-2, 'T%H:%M:%S%z', None, {}, 3, id='t-hh-mm-ss-tz'),
))
def test_hire_order_list(client, modified__gte, candidate_idx, other_params, count, date_format):
    candidates = []
    if other_params.get('superuser', False):
        user = f.create_superuser()
    else:
        user = f.create_user_with_perm('can_use_hire_orders')
    another_user = f.create_user_with_perm('can_use_hire_orders')

    for idx in range(5):
        candidate = f.CandidateFactory.create()
        candidates.append(candidate)
        cdate = datetime.shifted_now(days=-idx)
        f.HireOrderFactory.create(
            created=cdate,
            candidate=candidate,
            created_by=user,
        )
        f.HireOrderFactory.create(
            created=cdate,
            created_by=another_user
        )

    HireOrder.objects.update(modified=F('created'))

    request_kwargs = other_params
    if modified__gte is not None:
        search_date = datetime.shifted_now(days=modified__gte, minutes=-5)
        date_format = f'%Y-%m-%d{date_format}' if date_format is not None else '%Y-%m-%d %H:%M:%S'
        request_kwargs['modified__gte'] = search_date.strftime(date_format)
    if candidate_idx is not None:
        request_kwargs['candidate_id'] = candidates[candidate_idx].id

    url = reverse('private-api:hire_orders:list')
    client.login(user.username)
    response = client.get(url, request_kwargs)

    assert response.status_code == 200

    result = response.json().get('results')
    assert len(result) == count
