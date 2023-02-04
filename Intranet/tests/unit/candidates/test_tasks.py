import pytest
import uuid

from unittest.mock import patch

from intranet.femida.src.candidates.bulk_upload.choices import CANDIDATE_UPLOAD_MODES
from intranet.femida.src.candidates.tasks import upload_from_beamery_task

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import ContainsDict


# Используем заготовленный словарь, чтобы тесты были предсказуемыми
BEAMERY_IDS = {
    'known': str(uuid.uuid4()),
    'unknown': str(uuid.uuid4()),
}


@pytest.mark.parametrize('beamery_id, femida_id, expected_original, expected_mode', (
    # Создание нового кандидата через Бимери. Указан неизвестный beamery id, femida id отсутствует
    pytest.param('unknown', None, None, CANDIDATE_UPLOAD_MODES.create, id='create'),
    # Изменение существующего кандидата через Бимери. Бимери уже знает femida id
    pytest.param('unknown', 100500, 100500, CANDIDATE_UPLOAD_MODES.merge, id='merge'),
    # Изменение существующего кандидата через Бимери. Бимери ещё не знает femida id.
    # Например, если после создания кандидата на стороне Бимери
    # его отредактировали до того, как Фемида прислала ответное сообщение со своим id.
    pytest.param('known', None, 100500, CANDIDATE_UPLOAD_MODES.merge, id='merge-by-beamery-id'),
))
@patch('intranet.femida.src.candidates.bulk_upload.uploaders.CandidateBeameryUploader')
def test_upload_from_beamery_task(mocked_uploader, beamery_id, femida_id,
                                  expected_original, expected_mode):
    f.CandidateFactory(id=100500, beamery_id=BEAMERY_IDS['known'])
    beamery_id = BEAMERY_IDS[beamery_id]
    raw_data = {
        'id': beamery_id,
        'integrations': {
            'brassring': {'id': femida_id},
        },
    }
    expected_serialized_data = {
        'beamery_id': beamery_id,
        'original': expected_original,
    }

    upload_from_beamery_task(data=[raw_data])

    mocked_uploader.assert_called_once_with(expected_mode, [ContainsDict(expected_serialized_data)])
    mocked_uploader().upload.assert_called_once_with()
