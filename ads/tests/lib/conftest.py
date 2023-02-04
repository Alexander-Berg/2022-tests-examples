import copy
import pytest

import yt.wrapper as yt


class LocalYt(object):
    def __init__(
            self,
            yt_stuff,
            transaction_id=None
            ):
        self._yt_stuff = yt_stuff
        self.transaction = transaction_id

    def _set_transaction(self, transaction_id):
        self.transaction = transaction_id

    def get_client(self):
        yt_stuff_client = self._yt_stuff.get_yt_client()
        yt_client = yt.YtClient(config=copy.deepcopy(yt_stuff_client.config))
        yt_client.config['enable_logging_for_params_changes'] = True

        if self.transaction is not None:
            yt.config.set_command_param("transaction_id", self.transaction, yt_client)

        return yt_client

    def with_transaction(
            self,
            transaction_id
    ):
        local_yt = LocalYt(self._yt_stuff)
        local_yt._set_transaction(transaction_id)
        return local_yt


@pytest.fixture(scope='module')
def local_yt(yt_stuff):
    yield LocalYt(yt_stuff)
