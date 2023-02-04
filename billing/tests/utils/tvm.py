import subprocess
from typing import Union

from retry import retry
from rest_framework.test import APIClient, APITestCase

__all__ = ['TVMAPIClient', 'TVMAPITestCase', 'TVMKnife', 'get_fake_keys', 'get_fake_ticket']


class TVMAPIClient(APIClient):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

        # Данный импорт невозможен, пока Django не инициализирована
        from billing.dcsaap.backend.tools.tvm import get_self_directed_ticket

        self.credentials(HTTP_X_YA_SERVICE_TICKET=get_self_directed_ticket())


class TVMAPITestCase(APITestCase):
    client_class = TVMAPIClient


class TVMKnife:
    """
    Небольшая обертка над tvmknife, который собирается специально для запуска тестов
    """

    def __init__(self):
        self._path = self.get_tvmknife_path()

    @staticmethod
    def get_tvmknife_path():
        import yatest.common

        try:
            return [yatest.common.binary_path('passport/infra/tools/tvmknife/bin/tvmknife')]
        except (AttributeError, NotImplementedError):
            # запуск вне ya make
            return ["ya", "tool", "tvmknife"]

    def keys(self) -> str:
        return subprocess.check_output(self._path + ['unittest', 'public_keys']).decode().strip()

    def service_ticket(self, src_id: Union[int, str], dst_id: Union[int, str]) -> str:
        return (
            subprocess.check_output(self._path + ['unittest', 'service', '-s', str(src_id), '-d', str(dst_id)])
            .decode()
            .strip()
        )

    def user_ticket(self, uid: Union[int, str]):
        return subprocess.check_output(self._path + ['unittest', 'user', '-d', str(uid)]).decode().strip()


@retry(tries=3)
def get_fake_keys():
    return TVMKnife().keys()


@retry(tries=3)
def get_fake_ticket(from_service_id: int, to_service_id: int):
    return TVMKnife().service_ticket(from_service_id, to_service_id)
