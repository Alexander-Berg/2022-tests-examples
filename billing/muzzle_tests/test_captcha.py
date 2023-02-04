# -*- coding: utf-8 -*-
import pytest
import requests
import mock

from balance import exc
from muzzle.captcha import generate_captcha, check_captcha


def _raise_for_status():
    raise requests.exceptions.HTTPError('Bad status')


def test_generate_captcha(session):
    with mock.patch('requests.get') as get:
        mock_resp = mock.Mock()
        mock_resp.status_code = requests.codes.ok
        mock_resp.content = '''<?xml version="1.0"?>
            <number url='u_r_l'>k_e_y</number>
        '''
        get.return_value = mock_resp
        key, url = generate_captcha()
        assert key == 'k_e_y'
        assert url == 'u_r_l'

    with pytest.raises(exc.CAPTCHA_API_UNAVAILABLE):
        with mock.patch('requests.get') as get:
            mock_resp = mock.Mock()
            mock_resp.raise_for_status = _raise_for_status
            get.return_value = mock_resp
            generate_captcha()

    with pytest.raises(exc.CAPTCHA_API_UNAVAILABLE):
        with mock.patch('requests.get') as get:
            get.side_effect = requests.exceptions.ConnectionError('Could not connect tot Captcha server')
            generate_captcha()


def test_check_captcha(session):
    with mock.patch('requests.get') as get:
        mock_resp = mock.Mock()
        mock_resp.status_code = requests.codes.ok
        mock_resp.content = '''<?xml version="1.0"?>
            <image_check>ok</image_check>
        '''
        get.return_value = mock_resp
        result = check_captcha('k_e_y', 'r_e_p')
        assert True == result

    with mock.patch('requests.get') as get:
        mock_resp = mock.Mock()
        mock_resp.status_code = requests.codes.ok
        mock_resp.content = '''<?xml version="1.0"?>
            <image_check>failed</image_check>
        '''
        get.return_value = mock_resp
        result = check_captcha('k_e_y', 'r_e_p')
        assert False == result

    with pytest.raises(exc.CAPTCHA_API_UNAVAILABLE):
        with mock.patch('requests.get') as get:
            mock_resp = mock.Mock()
            mock_resp.raise_for_status = _raise_for_status
            get.return_value = mock_resp
            check_captcha('k_e_y', 'r_e_p')

    with pytest.raises(exc.CAPTCHA_API_UNAVAILABLE):
        with mock.patch('requests.get') as get:
            get.side_effect = requests.exceptions.ConnectionError('Could not connect tot Captcha server')
            check_captcha('k_e_y', 'r_e_p')
