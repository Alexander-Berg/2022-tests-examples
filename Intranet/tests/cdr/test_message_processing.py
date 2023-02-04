from unittest.mock import patch, Mock
import pytest

from intranet.vconf.src.cdr.views import process_record, process_xml_input

pytestmark = pytest.mark.django_db


def test_process_xml_input_no_exceptions(messages):
    for message in messages:
        process_xml_input(message)


def test_process_record_cms_participant_join(cms_participant_join_record):
    join_participant_mock = Mock()
    with patch('intranet.vconf.src.cdr.views.join_participant', join_participant_mock):
        process_record(cms_participant_join_record)

    join_participant_mock.assert_called_once_with(
        call_id="b8a81da5-c24c-43db-ba58-742f587faec8",
        participant_cms_id="fc9c85ca-8c41-4a1a-9252-b16977d1e4e1",
        name='perfect display name',
    )
