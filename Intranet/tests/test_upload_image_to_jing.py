# coding: utf-8

from __future__ import unicode_literals

from mock import patch

from utils import handle_utterance, create_req_info


def test_upload_image_to_jing_no_start_message(uid, tg_app):
    with patch('uhura.lib.vins.connectors.tg_connector.TelegramConnector.download_file') as m,\
            patch('uhura.external.jing.JingClient.upload') as m1,\
            patch('os.remove') as m2:
        m.return_value = 'file_name'
        m2.return_value = None
        req_info = create_req_info(uid, '')
        req_info.additional_options['message'] = {
            'photo': [{'file_id': 'abcdef'}]
        }
        m1.return_value = 'link'
        response = tg_app.handle_request(req_info)
        assert response.messages == ['link']


def test_upload_image_to_jing_success(uid, tg_app):
    with patch('uhura.lib.vins.connectors.tg_connector.TelegramConnector.download_file') as m,\
            patch('uhura.external.jing.JingClient.upload') as m1,\
            patch('os.remove') as m2:
        m.return_value = 'file_name'
        m2.return_value = None
        req_info = create_req_info(uid, '')
        req_info.additional_options['message'] = {
            'photo': [{'file_id': 'abcdef'}]
        }
        m1.return_value = 'link'
        handle_utterance(
            tg_app, uid, 'Залей скриншот', 'Отправь мне, пожалуйста, картинку🌆 или gif', cancel_button=True
        )
        response = tg_app.handle_request(req_info)
        assert response.messages == ['link']
