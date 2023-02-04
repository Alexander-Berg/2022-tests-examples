import pytest

from intranet.femida.tests import factories as f


@pytest.fixture
def populate_db_duplicates():
    original = f.CandidateFactory()
    duplicate = f.CandidateFactory(
        is_duplicate=True,
        original=original,
    )
    return {
        'original': original,
        'duplicate': duplicate,
    }
