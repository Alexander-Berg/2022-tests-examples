import logging as log

from django.urls import reverse

from rest_framework.status import (
    HTTP_403_FORBIDDEN,
    HTTP_200_OK,
)
from django_yauth.settings import YAUTH_TVM2_SERVICE_HEADER

from billing.dcsaap.backend.project.settings import YAUTH_TVM2_CLIENT_ID

from billing.dcsaap.backend.tests.utils.tvm import get_fake_keys, get_fake_ticket


class TestRunAuth:
    """
    Тестирование GET запросов для описаний запусков
    """

    def test_not_auth(self, api_client, non_yolo):
        with non_yolo:
            response = api_client.get(reverse('who'))
        assert response.status_code == HTTP_403_FORBIDDEN

    def test_auth(self, api_client, non_yolo):
        serv_id = int(YAUTH_TVM2_CLIENT_ID)
        log.info(serv_id)
        ticket = get_fake_ticket(serv_id, serv_id)
        log.info(ticket)
        log.info(get_fake_keys())
        headers = {YAUTH_TVM2_SERVICE_HEADER: ticket}
        with non_yolo:
            response = api_client.get(reverse('who'), **headers)
        assert response.status_code == HTTP_200_OK
