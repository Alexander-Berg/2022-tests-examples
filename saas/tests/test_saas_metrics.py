import unittest

from saas.library.python.yasm import YasmSignalIterator
from saas.library.python.yasm.saas_service_metrics import SaasServiceMetrics
from saas.library.python.deploy_manager_api import SaasService


class TestSaasMetrics(unittest.TestCase):
    def test_proxy_rps(self):
        srv = ['stable', 'mtdict']
        signals = ['proxy_rps', 'proxy_ram_avg', 'proxy_cpu_avg', 'backend_rps', 'backend_ram_avg', 'backend_cpu_avg',
                   'proxy_ram_quant99', 'proxy_cpu_quant99', 'backend_ram_quant99', 'backend_cpu_quant99']
        service_metrics = SaasServiceMetrics.for_saas_service(SaasService(srv[0], srv[1]))
        for sig in signals:
            self.assertIsInstance(getattr(service_metrics, sig)(), YasmSignalIterator)
