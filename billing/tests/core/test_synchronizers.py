import pytest

from refs.core.exceptions import ConfigurationError
from refs.core.fetchers import FetcherBase
from refs.core.importers import ImporterBase
from refs.core.synchronizers import SynchronizerBase


@pytest.fixture
def get_syncer(mocker):

    def wrapped(fetcher=None, importer=None):
        Syncer = type('Syncer', (SynchronizerBase,), {
            'alias': 'test',
            'fetcher': fetcher or mocker.Mock(),
            'importer': importer or mocker.Mock(),
        })
        return Syncer

    return wrapped


def test_syncer_basics(get_syncer, mocker):

    with pytest.raises(ConfigurationError):
        # Не определены обяхательные атрибуты
        Syncer = type('Syncer', (SynchronizerBase,), {})
        Syncer()

    syncer: SynchronizerBase = get_syncer()()
    syncer._log_model = mocker.Mock()
    syncer.log('some')

    assert len(syncer._log_entries) == 1
    assert 'test' in syncer.get_media_path('test')

    assert not syncer.bootstrap

    syncer.get_latest_log_record()
    assert syncer._log_model.get_latest_record.call_count == 1


@pytest.mark.django_db
def test_syncer_run(get_syncer, mocker):

    fetcher_run_mock = mocker.Mock()
    fetcher_run_mock.get_repr = lambda: 'some'
    fetcher_run_mock.has_changed = False

    fetcher = type('Fetcher', (FetcherBase,), {'run': lambda *args: fetcher_run_mock})
    importer = type('Importer', (ImporterBase,), {'run': lambda *args: 'imported'})

    syncer: SynchronizerBase = get_syncer(fetcher, importer)()
    result = syncer.run()

    assert result is None  # До импорта дело не дошло из-за fetcher_run_mock.has_changed
    assert syncer._success

    fetcher_run_mock.has_changed = True
    result = syncer.run()

    assert result == 'imported'
    assert syncer._success

    # Проверим обработку исключений.
    def raiser():
        raise Exception('rise and shine')

    importer = type('Importer', (ImporterBase,), {'run': raiser})
    syncer: SynchronizerBase = get_syncer(fetcher, importer)()

    with pytest.raises(Exception):
        syncer.run()

    assert not syncer._success
    assert 'Traceback' in syncer._log_entries[0]
