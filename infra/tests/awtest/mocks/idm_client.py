from awacs.lib import idmclient


class MockIDMClient(idmclient.IDMClient):
    def __init__(self, url, token, req_timeout=None, verify_ssl=None, max_retries=None):
        super(MockIDMClient, self).__init__(url, token, req_timeout=req_timeout, verify_ssl=verify_ssl,
                                            max_retries=max_retries)
        self.last_update_call_args = None
        self.last_remove_call_args = None
        self.last_batch_call_args = None
        self.total_update_calls_count = 0
        self.total_remove_calls_count = 0
        self.total_batch_calls_count = 0

    def iterate_system_roles(self, system_id):
        return []

    def update_role(self, system_id, role_slug_path, responsibilities=None):
        self.total_update_calls_count += 1
        self.last_update_call_args = (system_id, role_slug_path, responsibilities)

    def remove_role(self, system_id, role_slug_path):
        self.total_remove_calls_count += 1
        self.last_remove_call_args = (system_id, role_slug_path)

    def batch(self, subrequests):
        self.total_batch_calls_count += 1
        self.last_batch_call_args = (subrequests,)
