import copy

import pytest

from smarttv.droideka.tests import mock
from smarttv.droideka.proxy.api.vh import VhIdLogger
from smarttv.droideka.tests.helpers import remove_part

NULLABLE_DOCUMENT_FIELDS = ('content_id', 'parent_id', 'title')

programs_test_data = [
    (mock.ProgramsMocks.programs_full_response, mock.ProgramsMocks.programs_full_expected_result),
    (remove_part(['set', mock.ProgramsMocks.INDEX_FIRST_EPISODE, ['end_time']],
                 mock.ProgramsMocks.programs_full_response),
     remove_part(['aggregated_ids', mock.ProgramsMocks.INDEX_FIRST_EPISODE, ['end_time']],
                 mock.ProgramsMocks.programs_full_expected_result)),
    (remove_part(['set', mock.ProgramsMocks.INDEX_FIRST_EPISODE, ['program_id']],
                 mock.ProgramsMocks.programs_full_response),
     remove_part(['aggregated_ids', mock.ProgramsMocks.INDEX_FIRST_EPISODE, ['program_id']],
                 mock.ProgramsMocks.programs_full_expected_result)),
    (remove_part(['set', mock.ProgramsMocks.INDEX_FIRST_EPISODE, ['start_time']],
                 mock.ProgramsMocks.programs_full_response),
     remove_part(['aggregated_ids', mock.ProgramsMocks.INDEX_FIRST_EPISODE, ['start_time']],
                 mock.ProgramsMocks.programs_full_expected_result)),
    (remove_part(['set', mock.ProgramsMocks.INDEX_FIRST_EPISODE, ['channel_id']],
                 mock.ProgramsMocks.programs_full_response),
     remove_part(['aggregated_ids', mock.ProgramsMocks.INDEX_FIRST_EPISODE, ['channel_id']],
                 mock.ProgramsMocks.programs_full_expected_result)),
    (remove_part(['set', mock.ProgramsMocks.INDEX_FIRST_EPISODE, ['title']],
                 mock.ProgramsMocks.programs_full_response),
     remove_part(['aggregated_ids', mock.ProgramsMocks.INDEX_FIRST_EPISODE, ['title']],
                 mock.ProgramsMocks.programs_full_expected_result)),
    (remove_part(['set', [mock.ProgramsMocks.INDEX_FIRST_EPISODE]],
                 copy.deepcopy(mock.ProgramsMocks.programs_full_response)),
     remove_part(['aggregated_ids', [mock.ProgramsMocks.INDEX_FIRST_EPISODE]],
                 copy.deepcopy(mock.ProgramsMocks.programs_full_expected_result))),
    (remove_part(['set', [mock.ProgramsMocks.INDEX_FIRST_EPISODE, mock.ProgramsMocks.INDEX_SECOND_EPISODE]],
                 copy.deepcopy(mock.ProgramsMocks.programs_full_response)),
     remove_part(['aggregated_ids', [mock.ProgramsMocks.INDEX_FIRST_EPISODE, mock.ProgramsMocks.INDEX_SECOND_EPISODE]],
                 copy.deepcopy(mock.ProgramsMocks.programs_full_expected_result))),
    (remove_part(['set', [mock.ProgramsMocks.INDEX_FIRST_EPISODE, mock.ProgramsMocks.INDEX_SECOND_EPISODE]],
                 copy.deepcopy(mock.ProgramsMocks.programs_full_response)),
     remove_part(['aggregated_ids', [mock.ProgramsMocks.INDEX_FIRST_EPISODE, mock.ProgramsMocks.INDEX_SECOND_EPISODE]],
                 copy.deepcopy(mock.ProgramsMocks.programs_full_expected_result))),
]


def test_log_full_carousel_info():
    actual_result = VhIdLogger.get_log_info(mock.log_id_carousel_full_input, VhIdLogger.KEY_CAROUSEL,
                                            carousel_id='some_carousel_id')

    assert mock.log_id_carousel_full_result == actual_result


def test_log_carousel_without_id():
    actual_result = VhIdLogger.get_log_info(mock.log_id_carousel_full_input, VhIdLogger.KEY_CAROUSEL)

    assert actual_result == mock.log_id_carousel_noid_result


@pytest.mark.parametrize('nullable_field', NULLABLE_DOCUMENT_FIELDS)
def test_log_carousel_without_content_id(nullable_field):
    input_data = mock.log_id_carousel_small_input.copy()
    del input_data['set'][0][nullable_field]
    expected_result = mock.log_id_carousel_small_result.copy()
    expected_result['aggregated_ids']['content'][0][nullable_field] = None

    actual_result = VhIdLogger.get_log_info(input_data, VhIdLogger.KEY_CAROUSEL, carousel_id='some_carousel_id')
    assert expected_result == actual_result


def test_full_doc2doc():
    input_data = mock.log_id_full_doc2doc.copy()
    expected_result = mock.log_id_full_doc2doc_result.copy()

    actual_result = VhIdLogger.get_log_info(input_data, VhIdLogger.KEY_DOC2DOC)

    assert expected_result == actual_result


@pytest.mark.parametrize('nullable_field', NULLABLE_DOCUMENT_FIELDS)
def test_log_doc2doc_without_content_id(nullable_field):
    input_data = mock.log_id_small_doc2doc.copy()
    del input_data['set'][0][nullable_field]
    expected_result = mock.log_id_small_doc2doc_result.copy()
    expected_result['aggregated_ids'][0][nullable_field] = None

    actual_result = VhIdLogger.get_log_info(input_data, VhIdLogger.KEY_DOC2DOC)
    assert expected_result == actual_result


def test_log_full_feed():
    actual_result = VhIdLogger.get_log_info(mock.log_id_feed_full_input, VhIdLogger.KEY_FEED)

    assert actual_result == mock.log_id_feed_result


@pytest.mark.parametrize('input_data, expected_result', programs_test_data)
def test_log_programs(input_data, expected_result):
    assert VhIdLogger.get_log_info(input_data, VhIdLogger.KEY_PROGRAMS) == expected_result
