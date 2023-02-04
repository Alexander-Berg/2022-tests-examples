import logging
import pytest
import copy

from smarttv.droideka.tests import mock
from smarttv.droideka.tests.helpers import remove_part

from smarttv.droideka.proxy.api.vs import VideoSearchExtendedLogger

logger = logging.getLogger(__name__)

test_data = [
    (mock.VideoSearchMocks.video_search_full_vs_response, mock.VideoSearchMocks.expected_full_response),
    (remove_part([['entity_data']], copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part([['base_info', 'legal_assoc', 'collections', 'assoc']],
                 copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),

    (remove_part(['entity_data', ['base_info']], copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part([['base_info']], copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),

    (remove_part(['entity_data', ['related_object']],
                 copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part([['collections', 'assoc']], copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),

    (remove_part(['entity_data', ['legal_assoc']], copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part([['legal_assoc']], copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),

    (remove_part([['clips']], copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part([['clips']], copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),

    (remove_part(['entity_data', 'base_info', ['id']],
                 copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part(['base_info', ['id']], copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),

    (remove_part(['entity_data', 'base_info', ['title']],
                 copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part(['base_info', ['title']], copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),

    (remove_part(['entity_data', 'base_info', ['legal']],
                 copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part(['base_info', ['uuid']], copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),

    (remove_part(['entity_data', 'base_info', 'legal', ['vh_licenses']],
                 copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part(['base_info', ['uuid']], copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),

    (remove_part(['entity_data', 'base_info', 'legal', 'vh_licenses', ['uuid']],
                 copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part(['base_info', ['uuid']], copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),

    (remove_part(['entity_data', 'related_object', [mock.VideoSearchMocks.COLLECTIONS_INDEX]],
                 copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part([['collections']], copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),

    (remove_part(['entity_data', 'related_object', mock.VideoSearchMocks.COLLECTIONS_INDEX, ['search_request']],
                 copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part(['collections', ['search_request']], copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),

    (remove_part(['entity_data', 'related_object', mock.VideoSearchMocks.COLLECTIONS_INDEX, ['entref']],
                 copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part(['collections', ['entref']], copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),

    (remove_part(['entity_data', 'related_object', [mock.VideoSearchMocks.ASSOC_INDEX]],
                 copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part([['assoc']], copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),

    (remove_part(['entity_data', 'related_object', mock.VideoSearchMocks.ASSOC_INDEX, ['search_request']],
                 copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part(['assoc', ['search_request']], copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),

    (remove_part(['entity_data', 'legal_assoc', 0, ['search_request']],
                 copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part(['legal_assoc', ['search_request']], copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),

    (remove_part(['entity_data', 'legal_assoc', 0, 'object', [mock.VideoSearchMocks.LEGAL_ASSOC_N_1]],
                 copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part(['legal_assoc', 'object', [mock.VideoSearchMocks.LEGAL_ASSOC_N_1]],
                 copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),

    (remove_part(['entity_data', 'legal_assoc', 0, 'object', mock.VideoSearchMocks.LEGAL_ASSOC_N_1, ['name']],
                 copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part(['legal_assoc', 'object', mock.VideoSearchMocks.LEGAL_ASSOC_N_1, ['name']],
                 copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),

    (remove_part(['entity_data', 'legal_assoc', 0, 'object', mock.VideoSearchMocks.LEGAL_ASSOC_N_1, ['id']],
                 copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part(['legal_assoc', 'object', mock.VideoSearchMocks.LEGAL_ASSOC_N_1, ['id']],
                 copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),

    (remove_part(['entity_data', 'legal_assoc', 0, 'object', mock.VideoSearchMocks.LEGAL_ASSOC_N_1, ['name', 'id']],
                 copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part(['legal_assoc', 'object', [mock.VideoSearchMocks.LEGAL_ASSOC_N_1]],
                 copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),

    (remove_part(['entity_data', 'legal_assoc', 0, 'object', [mock.VideoSearchMocks.LEGAL_ASSOC_N_2]],
                 copy.deepcopy(mock.VideoSearchMocks.video_search_full_vs_response)),
     remove_part(['legal_assoc', 'object', [mock.VideoSearchMocks.LEGAL_ASSOC_N_2]],
                 copy.deepcopy(mock.VideoSearchMocks.expected_full_response))),
]


@pytest.mark.parametrize('input_data, expected_result', test_data)
def test_absent_data_is_ok(input_data, expected_result):
    actual_result = VideoSearchExtendedLogger.get_log_content(input_data)
    assert actual_result == expected_result
