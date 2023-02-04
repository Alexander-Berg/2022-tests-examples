import pytest

from smarttv.plant.plant.macs import MacsImporter


class TestImporter:
    @pytest.fixture
    def bulk_create(self, mocker):
        yield mocker.patch('smarttv.plant.plant.macs.MacAddress.objects.bulk_create')

    def test_addresses_saved_normalized(self, bulk_create):
        """
        Проверяет, что в базу данных адреса сохраняются в нормализованном виде
        """
        importer = MacsImporter()
        post_str = "AABBCCAABBCC"
        importer.run(None, None, post_str)

        bulk_create.assert_called_once()

        insertions = bulk_create.call_args.args[0]
        assert insertions[0].macstr == 'aa:bb:cc:aa:bb:cc'
