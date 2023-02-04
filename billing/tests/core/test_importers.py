import pytest

from refs.core.importers import ImporterBase


def test_importer_basics(mocker):
    Importer = type('Importer', (ImporterBase,), {})

    with pytest.raises(NotImplementedError):
        # run_ not implemented
        Importer(mocker.Mock()).run()

    synchronizer = mocker.Mock()
    synchronizer.fetcher_result = 33

    Importer = type('Importer', (ImporterBase,), {})

    importer = Importer(synchronizer)

    assert importer.fetcher_result == 33

    synchronizer.bootstrap = False
    assert not importer.bootstrap

    synchronizer.bootstrap = True
    assert importer.bootstrap