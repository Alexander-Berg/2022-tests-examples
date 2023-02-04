from unittest import mock
from dwh.grocery.tools.bunker import get_from_cache, CypressCache


version = "stable"


class FakeYtClient:

    def __init__(self):
        pass

    def exists(self, path):
        return True

    def create(self, type, path, recursive):
        pass

    def get(self, path):
        """
        Когда в метаданных должен быть run_id, но по какой-то причине его нет (DWH-769)
        """
        return {}


class FakeYtClientOlderVersion(FakeYtClient):

    def exists(self, path):
        """
        Когда api_version у YT установлена как v4, а ответ пришел в старом формате (DWH-769)
        ...
        File "yt/python/yt/wrapper/client_impl.py", line 352, in exists
          return client_api.exists(path, client=self, read_from=read_from, cache_sticky_group_size=cache_sticky_group_size, suppress_transaction_coordinator_sync=suppress_transaction_coordinator_sync)
        File "yt/python/yt/wrapper/cypress_commands.py", line 351, in exists
          return apply_function_to_result(_process_result, result)
        File "yt/python/yt/wrapper/batch_response.py", line 44, in apply_function_to_result
          return function(result)
        File "yt/python/yt/wrapper/cypress_commands.py", line 342, in _process_result
          return result["value"] if get_api_version(client) == "v4" else result
        """
        raise KeyError('value')


class TestBunker:

    def test_get_run_id_not_present(self):
        def mock_init(self, path, cluster="locke"):
            self._path = path
            self._client = FakeYtClient

        with mock.patch.object(CypressCache, '__init__', mock_init):
            assert get_from_cache("123/123", version) is None

    def test_get_exists_throws_key_error_due_to_older_version_result(self):
        def mock_init(self, path, cluster="locke"):
            self._path = path
            self._client = FakeYtClientOlderVersion

        with mock.patch.object(CypressCache, '__init__', mock_init):
            assert get_from_cache("123/123", version) is None
