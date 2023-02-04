import pytest

from unittest.mock import patch, Mock

from intranet.femida.src.candidates.scorings.uploaders import CandidateScoringYTUploader
from intranet.femida.src.candidates.models import CandidateScoring

from intranet.femida.tests import factories as f


@pytest.fixture
def scoring_dataset():
    return [
        {'candidate': f.CandidateFactory().id, 'scoring_value': 0.1},
        {'candidate': f.CandidateFactory().id, 'scoring_value': 1},
    ]


def _upload_scoring_data(data):
    uploader = CandidateScoringYTUploader(
        table='//home/table',
        version='1',
        scoring_category=f.ScoringCategoryFactory().id,
    )
    mock = Mock(return_value=data)
    with patch('intranet.femida.src.candidates.scorings.uploaders.SheetReaderYTAdapter', mock):
        uploader.upload()


@patch('intranet.femida.src.candidates.scorings.uploaders.ScoringUploadResultsYTTable', Mock())
def test_scoring_yt_upload(scoring_dataset):
    _upload_scoring_data(scoring_dataset)

    scoring_value_by_candidate = dict(
        CandidateScoring.objects.values_list('candidate', 'scoring_value'),
    )
    assert len(scoring_value_by_candidate) == len(scoring_dataset)
    for item in scoring_dataset:
        assert scoring_value_by_candidate[item['candidate']] == item['scoring_value']


@patch('intranet.femida.src.candidates.scorings.uploaders.ScoringUploadResultsYTTable', Mock())
@pytest.mark.parametrize('data', (
    {},
    {'scoring_value': -1},
))
def test_scoring_yt_upload_invalid_data(data):
    data['candidate'] = f.CandidateFactory().id
    _upload_scoring_data([data])
    assert not CandidateScoring.objects.exists()
