import json
import uuid
import requests_mock
import re
from copy import deepcopy

import mock
import pytest
from smarttv.droideka.proxy.blackbox import UserInfo
from smarttv.droideka.proxy.models import Device, ValidIdentifier, calculate_hardware_id
from smarttv.droideka.tests.mock import MediaBillingHttpMock, MediaBillingMock
from smarttv.droideka.proxy.views.setup_wizard import NOT_ALLOWED_MAC

SERIAL_1 = 'serial1'
SERIAL_2 = 'serial2'
WIFI_MAC_1 = '00:25:90:94:2a:f2'
WIFI_MAC_2 = '00:25:90:94:2a:f3'
ETHERNET_MAC_1 = '00:25:90:e4:d0:29'
ETHERNET_MAC_2 = '00:25:90:e4:d0:2a'
SOME_PUID = 123456
OTHER_PUID = 654321


@pytest.mark.parametrize('ethernet_mac,wifi_mac,hardware_id', [
    (ETHERNET_MAC_1, WIFI_MAC_1, 'f5ea16f869365e48ec52ef6694def1e9'),
    (ETHERNET_MAC_1, None, '5b40792eba0dbac51fb208e47b7e104f'),
    (None, WIFI_MAC_1, 'ef972bab3eae73c20c8dd18c6ba35bde'),
    (ETHERNET_MAC_1.lower(), WIFI_MAC_1.lower(), 'f5ea16f869365e48ec52ef6694def1e9'),
    (ETHERNET_MAC_1.upper(), WIFI_MAC_1.upper(), 'f5ea16f869365e48ec52ef6694def1e9'),
    (ETHERNET_MAC_1, WIFI_MAC_2, '6f00fdc78a3ba7980237fde1e5e60ee7'),
    (ETHERNET_MAC_2, WIFI_MAC_1, '0bb1476a5346b412dc6a98294b36694f'),
])
def test_calculate_hardware_id(ethernet_mac, wifi_mac, hardware_id):
    assert calculate_hardware_id(ethernet_mac, wifi_mac) == hardware_id


@mock.patch('smarttv.droideka.proxy.views.setup_wizard.BaseSUWView.load_memento_configs', mock.Mock(return_value=None))
@pytest.mark.django_db
class TestSUWActivationV4:
    endpoint = '/api/v4/suw/activation'
    extra = {'HTTP_SERIAL': SERIAL_1, 'HTTP_X_WIFI_MAC': WIFI_MAC_1, 'HTTP_X_ETHERNET_MAC': ETHERNET_MAC_1}
    hardware_id = calculate_hardware_id(ETHERNET_MAC_1, WIFI_MAC_1)

    @pytest.fixture(autouse=True)
    def setup_stuff(self, db):
        ValidIdentifier.objects.create(type='ethernet_mac', value=ETHERNET_MAC_1)

    def test_ok(self, client):
        response = client.post(path=self.endpoint, **self.extra)
        assert response.status_code == 204, response.content

    def test_model_is_created(self, client):
        assert Device.objects.filter(hardware_id=self.hardware_id).count() == 0
        client.post(path=self.endpoint, **self.extra)
        assert Device.objects.filter(hardware_id=self.hardware_id).count() == 1

    def test_model_fields(self, client):
        client.post(path=self.endpoint, **self.extra)
        device = Device.objects.get(hardware_id=self.hardware_id)
        assert device.kp_gifts_id is not None
        assert device.kp_gifts_given is False
        assert isinstance(device.kp_gifts_id, uuid.UUID)

    def test_unknown_identifiers(self, client):
        ValidIdentifier.objects.all().delete()
        ValidIdentifier.objects.create(type='ethernet_mac', value=ETHERNET_MAC_2)

        response = client.post(path=self.endpoint, **self.extra)
        assert response.status_code == 403, response.content

    def test_not_allowed_mac(self, client):
        extra = deepcopy(self.extra)
        extra['HTTP_X_WIFI_MAC'] = NOT_ALLOWED_MAC
        response = client.post(path=self.endpoint, **extra)
        assert response.status_code == 400, response.content

    def test_double_activation_ok(self, client):
        response = client.post(path=self.endpoint, **self.extra)
        assert response.status_code == 204, response.content
        response = client.post(path=self.endpoint, **self.extra)
        assert response.status_code == 204, response.content

    def test_double_activation_with_different_wifi_macs(self, client):
        response = client.post(path=self.endpoint, **self.extra)
        assert response.status_code == 204, response.content

        extra = self.extra.copy()
        extra['HTTP_X_WIFI_MAC'] = WIFI_MAC_2
        response = client.post(path=self.endpoint, **extra)
        assert response.status_code == 403, response.content

    def test_double_activation_with_different_ethernet_macs(self, client):
        response = client.post(path=self.endpoint, **self.extra)
        assert response.status_code == 204, response.content

        extra = self.extra.copy()
        extra['HTTP_X_ETHERNET_MAC'] = ETHERNET_MAC_2
        response = client.post(path=self.endpoint, **extra)
        assert response.status_code == 403, response.content


@mock.patch('smarttv.droideka.proxy.views.setup_wizard.BaseSUWView.load_memento_configs', mock.Mock(return_value=None))
@pytest.mark.django_db
@mock.patch('smarttv.droideka.proxy.views.base.APIView._user_info', UserInfo(SOME_PUID))
class TestGiftsPostV4:
    endpoint = '/api/v4/suw/gift'
    extra = {'HTTP_SERIAL': SERIAL_1, 'HTTP_X_WIFI_MAC': WIFI_MAC_1, 'HTTP_X_ETHERNET_MAC': ETHERNET_MAC_1}
    clone_promo_code_404_response = \
        {'invocationInfo': {'req-id': 'de8712f699503cb3d48a295a9e409cef',
                            'hostname': 'mediabilling-test-api-man-7.man.yp-c.yandex.net',
                            'exec-duration-millis': 27,
                            'action': 'POST_PromoCodeActionContainer.clonePromoCode/clone',
                            'app-name': 'mediabilling-api',
                            'app-version': '2020-06-15.stable-58.6972747 (6972747; 2020-06-15T12:55:05)'},
         'result': {'id': 81201, 'description': 'Promo code not found.'}}
    hardware_id = calculate_hardware_id(ETHERNET_MAC_1, WIFI_MAC_1)

    @mock.patch('smarttv.droideka.proxy.views.setup_wizard.mediabilling_api', MediaBillingMock())
    def test_ok(self, client):
        Device(serial_number=SERIAL_1, wifi_mac=WIFI_MAC_1, ethernet_mac=ETHERNET_MAC_1).save()
        response = client.post(path=self.endpoint, **self.extra)
        assert response.status_code == 204, response.content
        device = Device.objects.get(hardware_id=self.hardware_id)
        assert device.subscription_puid == SOME_PUID

    def test_with_missing_puid(self, client):
        with mock.patch('smarttv.droideka.proxy.views.base.APIView._user_info', None):
            response = client.post(path=self.endpoint, **self.extra)
            assert response.status_code == 403, response.content

    def test_with_missing_device(self, client):
        response = client.post(path=self.endpoint, **self.extra)
        assert response.status_code == 404, response.content

    @mock.patch('smarttv.droideka.proxy.views.setup_wizard.mediabilling_api', MediaBillingHttpMock())
    def test_clone_promocode_404(self, client):
        Device(serial_number=SERIAL_1, wifi_mac=WIFI_MAC_1, ethernet_mac=ETHERNET_MAC_1).save()
        headers = {
            'Content-Type': 'application/json;charset=utf-8'
        }
        with requests_mock.Mocker(real_http=True) as m:
            m.register_uri('POST',
                           f'{MediaBillingHttpMock.base_url}/billing/promo-code/clone',
                           headers=headers, status_code=404,
                           json=self.clone_promo_code_404_response)
            response = client.post(path=self.endpoint, **self.extra)
            assert response.status_code == 404, response.content

    @mock.patch('smarttv.droideka.proxy.views.setup_wizard.mediabilling_api',
                MediaBillingMock(fail=['clone_promocode']))
    def test_fail_on_clone_promocode(self, client):
        Device(serial_number=SERIAL_1, wifi_mac=WIFI_MAC_1, ethernet_mac=ETHERNET_MAC_1).save()
        response = client.post(path=self.endpoint, **self.extra)
        assert response.status_code == 503, response.content
        device = Device.objects.get(hardware_id=self.hardware_id)
        assert device.subscription_puid is None

    @mock.patch('smarttv.droideka.proxy.views.setup_wizard.mediabilling_api',
                MediaBillingMock(fail=['consume_promocode']))
    def test_fail_on_consuming_promocode(self, client):
        Device(serial_number=SERIAL_1, wifi_mac=WIFI_MAC_1, ethernet_mac=ETHERNET_MAC_1).save()
        response = client.post(path=self.endpoint, **self.extra)
        assert response.status_code == 503, response.content
        device = Device.objects.get(hardware_id=self.hardware_id)
        assert device.subscription_puid is None

    @mock.patch('smarttv.droideka.proxy.views.setup_wizard.mediabilling_api', MediaBillingMock())
    def test_gift_again(self, client):
        Device(serial_number=SERIAL_1, wifi_mac=WIFI_MAC_1, ethernet_mac=ETHERNET_MAC_1,
               subscription_puid=SOME_PUID).save()
        response = client.post(path=self.endpoint, **self.extra)
        assert response.status_code == 204, response.content
        device = Device.objects.get(hardware_id=self.hardware_id)
        assert device.subscription_puid == SOME_PUID

    def test_gift_another_puid(self, client):
        Device(serial_number=SERIAL_1, wifi_mac=WIFI_MAC_1, ethernet_mac=ETHERNET_MAC_1,
               subscription_puid=OTHER_PUID).save()
        response = client.post(path=self.endpoint, **self.extra)
        assert response.status_code == 403, response.content
        device = Device.objects.get(hardware_id=self.hardware_id)
        assert device.subscription_puid == OTHER_PUID


@mock.patch('smarttv.droideka.proxy.views.setup_wizard.BaseSUWView.load_memento_configs', mock.Mock(return_value=None))
@pytest.mark.django_db
@mock.patch('smarttv.droideka.proxy.views.base.APIView._user_info', UserInfo(SOME_PUID))
@mock.patch('smarttv.droideka.proxy.views.setup_wizard.GiveGift.load_memento_configs',
            mock.Mock(return_value=None))
class TestGiftsGetV4:
    endpoint = '/api/v4/suw/gift'
    extra = {'HTTP_SERIAL': SERIAL_1, 'HTTP_X_WIFI_MAC': WIFI_MAC_1, 'HTTP_X_ETHERNET_MAC': ETHERNET_MAC_1}

    def test_ok(self, client):
        Device(serial_number=SERIAL_1, wifi_mac=WIFI_MAC_1, ethernet_mac=ETHERNET_MAC_1).save()
        response = client.get(path=self.endpoint, **self.extra)
        assert response.status_code == 200, response.content

    def test_with_missing_device(self, client):
        response = client.get(path=self.endpoint, **self.extra)
        assert response.status_code == 404, response.content

    def test_gifts_given(self, client):
        Device(serial_number=SERIAL_1, wifi_mac=WIFI_MAC_1, ethernet_mac=ETHERNET_MAC_1,
               subscription_puid=SOME_PUID).save()
        response = client.get(path=self.endpoint, **self.extra)
        answer = json.loads(response.content)
        assert answer['gift_available'] is False

    def test_kp_gifts_given(self, client):
        Device(
            serial_number=SERIAL_1,
            wifi_mac=WIFI_MAC_1,
            ethernet_mac=ETHERNET_MAC_1,
            kp_gifts_id=uuid.uuid4(),
            kp_gifts_given=True
        ).save()
        response = client.get(path=self.endpoint, **self.extra)
        answer = json.loads(response.content)
        assert answer['gift_available'] is False

    def test_gifts_not_given(self, client):
        Device(serial_number=SERIAL_1, wifi_mac=WIFI_MAC_1, ethernet_mac=ETHERNET_MAC_1).save()
        response = client.get(path=self.endpoint, **self.extra)
        answer = json.loads(response.content)
        assert answer['gift_available'] is True

    def test_kp_gifts_id_after_activation(self, client):
        Device(
            serial_number=SERIAL_1,
            wifi_mac=WIFI_MAC_1,
            ethernet_mac=ETHERNET_MAC_1,
            kp_gifts_id=uuid.uuid4(),
            kp_gifts_given=False
        ).save()
        response = client.get(path=self.endpoint, **self.extra)
        answer = json.loads(response.content)
        assert answer['gift_available'] is True
        assert answer['kp_gifts_id'] is not None
        assert re.match('^[0-9a-f]{32}$', answer['kp_gifts_id'])


@mock.patch('smarttv.droideka.proxy.views.setup_wizard.BaseSUWView.load_memento_configs', mock.Mock(return_value=None))
@pytest.mark.django_db
class TestKpGiftsPostbackV4:
    endpoint = '/api/v6/suw/kp_gifts_given/'

    def test_kp_gifts_postback(self, client):
        kp_gifts_id = uuid.uuid4()
        Device(
            serial_number=SERIAL_1,
            wifi_mac=WIFI_MAC_1,
            ethernet_mac=ETHERNET_MAC_1,
            kp_gifts_id=kp_gifts_id,
            kp_gifts_given=False
        ).save()
        hardware_id = calculate_hardware_id(ETHERNET_MAC_1, WIFI_MAC_1)
        response = client.post(path=self.endpoint, data={'kp_gifts_id': kp_gifts_id.hex})
        assert response.status_code == 204
        assert Device.objects.get(hardware_id=hardware_id).kp_gifts_given is True

    def test_kp_gifts_not_found(self, client):
        Device(
            serial_number=SERIAL_1,
            wifi_mac=WIFI_MAC_1,
            ethernet_mac=ETHERNET_MAC_1,
            kp_gifts_id=uuid.uuid4(),
            kp_gifts_given=False
        ).save()
        response = client.post(path=self.endpoint, data={'kp_gifts_id': uuid.uuid4().hex})
        assert response.status_code == 404
