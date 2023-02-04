import pytest

from unittest.mock import patch

from constance import config
from django.urls.base import reverse

from intranet.femida.src.core.choices import DRF_THROTTLE_SCOPES
from intranet.femida.src.offers.choices import OFFER_STATUSES
from intranet.femida.src.utils.begemot import MisspellAPI

from intranet.femida.tests import factories as f
from intranet.femida.tests.mock.offers import FakeNewhireAPI
from intranet.femida.tests.utils import use_cache


pytestmark = pytest.mark.django_db

one_req_for_path_per_day = patch(
    target='intranet.femida.src.offers.mixins.ExternalFormRatePerPathThrottle.rate',
    new='1/day',
)
one_req_for_view_and_user_per_day = patch(
    target='intranet.femida.src.offers.mixins.ExternalFormRatePerIPAndViewThrottle.THROTTLE_RATES',
    new={
        DRF_THROTTLE_SCOPES.ext_form_check_login: '1/day',
        DRF_THROTTLE_SCOPES.ext_form_attachment_upload: '1/day',
        DRF_THROTTLE_SCOPES.ext_form_accept: '1/day',
    },
)


@one_req_for_path_per_day
@use_cache
def test_offer_accept_form_throttling(client):
    """
    Проверяет, что форму внеш.анкеты оффера можно открывать с заданным рейтом
    """
    offer = f.create_offer(status=OFFER_STATUSES.sent)
    link = f.OfferLinkFactory.create(offer=offer)
    uid = link.uid.hex
    url = reverse('external-api:offers-accept-form', kwargs={'uid': uid})
    response = client.get(url)
    assert response.status_code == 200

    response = client.get(url)
    assert response.status_code == 429


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
@one_req_for_path_per_day
@use_cache
def test_offer_check_login_external_throttling(client):
    """
    Проверяет, что валидировать логин на внеш.анкете оффера можно только с заданным рейтом
    """
    offer = f.create_offer(status=OFFER_STATUSES.sent)
    link = f.OfferLinkFactory.create(offer=offer)
    uid = link.uid.hex
    url = reverse('external-api:offers-check-login', kwargs={'uid': uid})
    data = {
        'username': 'username',
    }
    response = client.get(url, data)
    assert response.status_code == 200

    response = client.get(url, data)
    assert response.status_code == 429


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
@one_req_for_view_and_user_per_day
@use_cache
def test_offer_check_login_external_throttling_per_ip(client):
    """
    Проверяет, что валидировать логин на ЛЮБОЙ внеш.анкете оффера можно только с заданным рейтом
    """
    for status_code in (200, 429):
        offer = f.create_offer(status=OFFER_STATUSES.sent)
        link = f.OfferLinkFactory.create(offer=offer)
        uid = link.uid.hex
        url = reverse('external-api:offers-check-login', kwargs={'uid': uid})
        data = {
            'username': 'username',
        }
        response = client.get(url, data)
        assert response.status_code == status_code


@patch('intranet.femida.src.offers.controllers.NewhireAPI', FakeNewhireAPI)
@one_req_for_path_per_day
@use_cache
def test_preprofile_accept_form_throttling(client):
    """
    Проверяет, что форму внеш.анкеты Наниматора можно открывать только с заданным рейтом
    """
    preprofile = f.PreprofileFactory()
    link = f.OfferLinkFactory(preprofile=preprofile)
    uid = link.uid.hex
    url = reverse('external-api:preprofiles-accept-form', kwargs={'uid': uid})
    response = client.get(url)
    assert response.status_code == 200

    response = client.get(url)
    assert response.status_code == 429


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
@patch('intranet.femida.src.offers.controllers.NewhireAPI', FakeNewhireAPI)
@one_req_for_path_per_day
@use_cache
def test_preprofile_check_login_throttling(client):
    """
    Проверяет, что валидировать логин на внеш.анкете Наниматора
    можно только с заданным рейтом
    """
    preprofile = f.PreprofileFactory()
    link = f.OfferLinkFactory(preprofile=preprofile)
    uid = link.uid.hex
    url = reverse('external-api:preprofiles-check-login', kwargs={'uid': uid})
    data = {
        'username': 'username',
    }
    response = client.get(url, data)
    assert response.status_code == 200

    response = client.get(url, data)
    assert response.status_code == 429


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
@patch('intranet.femida.src.offers.controllers.NewhireAPI', FakeNewhireAPI)
@one_req_for_view_and_user_per_day
@use_cache
def test_preprofile_check_login_throttling_per_ip(client):
    """
    Проверяет, что валидировать логин на ЛЮБОЙ внеш.анкете Наниматора
    можно только с заданным рейтом
    """
    for status_code in (200, 429):
        preprofile = f.PreprofileFactory()
        link = f.OfferLinkFactory(preprofile=preprofile)
        uid = link.uid.hex
        url = reverse('external-api:preprofiles-check-login', kwargs={'uid': uid})
        data = {
            'username': 'username',
        }
        response = client.get(url, data)
        assert response.status_code == status_code


@patch('intranet.femida.src.api.offers.base_views.NewhireAPI.attach_phone')
@use_cache
def test_attach_eds_phone_throttling(mocked_attach_phone, client):
    config.EDS_SMS_RATE = '1/day'
    mocked_attach_phone.return_value = ({}, 200)
    offer = f.create_offer(status=OFFER_STATUSES.sent)
    link = f.OfferLinkFactory.create(offer=offer)
    uid = link.uid.hex
    phone = '+77777777777'

    url = reverse('external-api:offers-attach-eds-phone', kwargs={'uid': uid})
    response = client.post(url, data={'eds_phone': phone})
    assert response.status_code == 200

    response = client.post(url, data={'eds_phone': phone})
    assert response.status_code == 429


@pytest.mark.parametrize('return_value, correct_response', (
    ({'code': 200}, ('test', False)),
    ({'code': 202}, ('test', False)),
    ({'code': 201, 'r': 10000, 'text': 'text'}, ('text', True)),
))
def test_misspell_api_responses(return_value, correct_response):
    user_text = 'test'
    patched_request = patch(
        target='intranet.femida.src.utils.begemot.MisspellAPI._request',
        return_value=return_value,
    )
    with patched_request:
        response = MisspellAPI.get_spellcheck(user_text)
    assert response == correct_response


@pytest.mark.parametrize('user_text, local_rules, correct_response', (
    ('test', {}, ('test', False)),
    ('test', {'test': 'text'}, ('text', True)),
    ('testing', {'test': 'text'}, ('testing', False)),
    ('a aa a aa a', {'a': 'b', 'something': 'else'}, ('b aa b aa b', True)),
))
def test_misspell_local_fixes(user_text, local_rules, correct_response):
    patched_request = patch(
        target='intranet.femida.src.utils.begemot.MisspellAPI._request',
        return_value={'code': 200},
    )
    patched_rules = patch(
        target='intranet.femida.src.utils.begemot.MisspellAPI.local_rules',
        new=local_rules,
    )
    with patched_request, patched_rules:
        response = MisspellAPI.get_spellcheck(user_text, local_rules=True)
    assert response == correct_response
