import pytest

from intranet.femida.src.candidates.deduplication import SimilarityInfo


pytestmark = pytest.mark.django_db


def test_different_candidates(dd_dataset):
    si = SimilarityInfo(dd_dataset['ivan'], dd_dataset['petr'])
    assert si.score == 0.0
    assert si.conflict_fields == {'first_name', 'middle_name', 'last_name', 'city', 'country'}


def test_by_itself(dd_dataset):
    si = SimilarityInfo(dd_dataset['ivan'], dd_dataset['ivan'])
    assert si.score == 5.5
    assert si.score_details['first_name'] == 0.5
    assert si.score_details['last_name'] == 1.0
    assert si.score_details['contacts'] == 4.0
    assert si.match_details['first_name'] == 1
    assert si.match_details['last_name'] == 1
    assert si.match_details['contacts'] == 2
    assert si.conflict_fields == set()


def test_with_duplicate_instance(dd_dataset):
    si = SimilarityInfo(dd_dataset['ivan'], dd_dataset['ivan_too'])
    assert si.score == 3.0
    assert si.score_details['last_name'] == 1.0
    assert si.score_details['contacts'] == 2.0
    assert si.match_details['last_name'] == 1
    assert si.match_details['contacts'] == 1
    assert si.conflict_fields == {'first_name', 'country'}


def test_with_duplicate_submission(dd_dataset):
    si = SimilarityInfo(dd_dataset['ivan_submission'], dd_dataset['ivan'])
    assert si.score == 3.5
    assert si.score_details['first_name'] == 0.5
    assert si.score_details['last_name'] == 1.0
    assert si.score_details['contacts'] == 2.0
    assert si.conflict_fields == set()


def test_with_duplicate_dict(dd_dataset):
    si = SimilarityInfo(dd_dataset['ivan_dict'], dd_dataset['ivan'])
    assert si.score == 5.5
    assert si.score_details['first_name'] == 0.5
    assert si.score_details['last_name'] == 1.0
    assert si.score_details['contacts'] == 4.0
    assert si.conflict_fields == set()
