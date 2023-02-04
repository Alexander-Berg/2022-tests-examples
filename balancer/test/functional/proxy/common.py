import pytest

watch_client_close = pytest.mark.parametrize('watch_client_close', [False, True], ids=['', 'watch_client_close'])
connection_manager_required = pytest.mark.parametrize('connection_manager_required', [False, True], ids=['', 'connection_manager_required'])
