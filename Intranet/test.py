from intranet.crt.core.ca.base import BaseCA
from intranet.crt.utils.test_ca import create_pem_certificate


class TestCA(BaseCA):
    """Тестовый CA для создания самоподписанных сертификатов.
    """
    IS_EXTERNAL = False
    IS_ASYNC = False

    chain_filename = 'TestCA.pem'

    @classmethod
    def find_non_auto_hosts(cls, fqdns):
        return set()

    def _issue(self, cert):
        cert.used_template = cert.controller.get_internal_ca_template()

        private_key_data = cert.private_key.data if cert.private_key is not None else None
        return create_pem_certificate(cert.request, private_key_data)

    def revoke(self, cert):
        pass

    def hold(self, cert):
        pass


class ApprovableTestCA(TestCA):
    """Класс, чтобы тестировать подтверждение запросов"""
    IS_EXTERNAL = True
    IS_ASYNC = True

    chain_filename = 'TestCA.pem'
