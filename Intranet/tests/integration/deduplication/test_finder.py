import pytest

from intranet.femida.src.candidates import deduplication

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


test_data = [
    {  # 0
        'target_cand': 'ivan',
        'strategy': deduplication.strategies.base_strategy,
        'threshold': deduplication.MAYBE_DUPLICATE,
        'expected_duplicates': ['ivan_too'],
    },
    {  # 1
        'target_cand': 'ivan',
        'strategy': deduplication.strategies.base_strategy,
        'threshold': deduplication.DEFINITELY_DUPLICATE,
        'expected_duplicates': [],
    },
    {  # 2
        'target_cand': 'ivan',
        'strategy': deduplication.strategies.new_strategy,
        'threshold': deduplication.MAYBE_DUPLICATE,
        'expected_duplicates': ['ivan_too'],
    },
    {  # 3
        'target_cand': 'ivan_submission',
        'strategy': deduplication.strategies.new_strategy,
        'threshold': deduplication.MAYBE_DUPLICATE,
        'expected_duplicates': ['ivan', 'ivan_too'],
    },
    {  # 4
        'target_cand': 'ivan_dict',
        'strategy': deduplication.strategies.new_strategy,
        'threshold': deduplication.MAYBE_DUPLICATE,
        'expected_duplicates': ['ivan', 'ivan_too'],
    },
]


@pytest.mark.parametrize('test_data', test_data)
def test_finder(dd_dataset, test_data):
    target_cand = dd_dataset[test_data['target_cand']]
    expected_duplicates = [dd_dataset[i] for i in test_data['expected_duplicates']]

    results = list(deduplication.DuplicatesFinder().find(
        candidate=target_cand,
        strategy=test_data['strategy'],
        threshold=test_data['threshold'],
    ))
    duplicates = [i[0] for i in results]
    assert len(duplicates) == len(expected_duplicates)
    for duplicate in expected_duplicates:
        assert duplicate in duplicates


def test_rotation_finder():
    submission = f.create_submission()
    candidate = f.create_candidate_with_consideration(
        login=submission.rotation.created_by.username,
    )
    results = list(deduplication.RotationDuplicatesFinder().find(
        candidate=submission,
        strategy=deduplication.strategies.rotation_strategy,
    ))
    duplicates = [i[0] for i in results]
    assert duplicates == [candidate]
