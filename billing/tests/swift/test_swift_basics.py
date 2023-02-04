import pytest

from refs.core.fetchers import FetcherFileResult
from refs.swift.synchronizers import SwiftSynchronizer


@pytest.mark.django_db
def test_importer_basics(extract_fixture):

    def do_import(filename, boostrap):
        filepath = extract_fixture(filename)

        synchronizer = SwiftSynchronizer(bootstrap=boostrap)
        result = FetcherFileResult()
        result.add_item(filename, filepath)
        synchronizer.fetcher_result = result
        synchronizer._run_importer()

        return synchronizer._log_entries

    # Базовые данные.
    result = do_import('swift_base.zip', boostrap=True)
    assert result[0] == 'EventImporterFull. Created: 2 Updated: 0 Deleted: 0'
    assert result[1] == 'HolidayImporter. Created: 16 Updated: 0 Deleted: 8'

    # Инкрементальное обновление.
    result = do_import('swift_update.zip', boostrap=False)
    assert result[0] == 'EventImporterDelta. Created: 2 Updated: 2 Deleted: 1'
    assert result[1] == 'HolidayImporter. Created: 0 Updated: 0 Deleted: 9'

    # Проверяем, что не ломаемся при получении обновления по архивной записи
    result = do_import('swift_new_update.zip', boostrap=False)
    assert len(result) == 1
    assert result[0] == 'HolidayImporter. Created: 1 Updated: 0 Deleted: 8'


@pytest.mark.django_db
def test_importer_custom(extract_fixture):
    """Тест на обновление отдельных источников."""

    def do_import(filename, boostrap, sources='event,holiday'):
        filepath = extract_fixture(filename)

        synchronizer = SwiftSynchronizer(
            bootstrap=boostrap,
            params={'sources': sources}
        )
        result = FetcherFileResult()
        result.add_item(filename, filepath)
        synchronizer.fetcher_result = result
        synchronizer._run_importer()

        return synchronizer._log_entries

    # Импорт всех данных
    result = do_import('swift_base.zip', boostrap=True)
    assert result[0] == 'EventImporterFull. Created: 2 Updated: 0 Deleted: 0'
    assert result[1] == 'HolidayImporter. Created: 16 Updated: 0 Deleted: 8'

    # Импорт только событий
    result = do_import('swift_update.zip', boostrap=False, sources='event')
    assert len(result) == 1
    assert result[0] == 'EventImporterDelta. Created: 2 Updated: 2 Deleted: 1'

    # Импорт только праздников
    result = do_import('swift_update.zip', boostrap=False, sources='holiday')
    assert len(result) == 1
    assert result[0] == 'HolidayImporter. Created: 0 Updated: 0 Deleted: 9'
