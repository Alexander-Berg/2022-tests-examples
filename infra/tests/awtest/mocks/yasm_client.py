from awacs.lib import yasm_client


class MockYasmClient(yasm_client.YasmClient):
    def get_last_balancer_rps(self, *args, **kwargs):
        return 0
