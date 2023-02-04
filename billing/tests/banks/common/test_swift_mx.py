import pytest

from bcl.banks.common.client import GenericAssociateClient
from bcl.banks.common.connector import ConnectorCaller
from bcl.banks.common.swift_mx import SwiftMxSftpConnector


@pytest.fixture
def get_connector_sftp():
    """Создаёт подключатель, имитирующий работу с SFTP."""

    def get_connector_sftp_(client_mock):

        class TestConnector(SwiftMxSftpConnector):

            party_alias = 'ing'
            settings_aliases = ['ya']
            settings_alias_default = 'ya'

            def __init__(self, *, caller: ConnectorCaller, alias: str = None):
                super().__init__(caller=caller, alias=alias)
                self.bundle_filename = 'dummybundle.txt'

            @classmethod
            def get_client(cls, alias: str = None) -> GenericAssociateClient:
                return client_mock

            def bundle_filename_generate(self) -> str:
                return self.bundle_filename

        connector = TestConnector(caller=None)

        client_mock.path_outbox = 'out/'
        client_mock.path_inbox = 'in/'
        client_mock.settings_alias = 'ya'

        return connector

    return get_connector_sftp_


class TestSwiftMxSftpConnector:

    def test_status_get_files(self, sftp_client, get_connector_sftp):

        connector = get_connector_sftp(sftp_client(files_contents={
            'out/file1.txt': '12345',
            'out/file2.txt': '67890',
        }))

        result = connector.status_get()
        assert result == ['12345', '67890']

    def test_fetch_accounts_data(self, sftp_client, get_connector_sftp):

        connector = get_connector_sftp(sftp_client(files_contents={
            'out/file1.txt': '12345',
            'out/file2.txt': '67890',
        }))

        def get_data(fname, contents, settings_alias):
            return f'accnum-{settings_alias}-{fname}', contents

        result = connector.fetch_accounts_data(
            func_filter_name=lambda name: name.endswith('file2.txt'),
            func_get_data_for_account=get_data
        )
        assert result == {'accnum-ya-out/file2.txt': ['67890']}

    def test_bundle_register(self, sftp_client, get_connector_sftp):

        client_mock = sftp_client()
        connector = get_connector_sftp(client_mock)

        result = connector.bundle_register('bundlecontents')
        assert result is None
        assert client_mock.files_contents == {'in/dummybundle.txt': 'bundlecontents'}
