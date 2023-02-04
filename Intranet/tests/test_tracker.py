import pytest

from mock import patch

from ids.exceptions import BackendError

from intranet.compositor_processors.src.processors.tracker import (
    create_queue,
    create_ticket,
)


def test_create_queue_success(test_vcr):
    with patch('intranet.compositor_processors.src.processors.tracker.get_service_ticket') as mock_service_ticket:
        mock_service_ticket.return_value = 'some_ticket'
        with test_vcr.use_cassette('tracker_create_queue_success.yaml'):
            create_queue(
                uid='1120000000016772',
                org_id=123,
                queue_name='dogma',
                queue_key='DOGMA',
            )


def test_create_queue_fail(test_vcr):
    with patch('intranet.compositor_processors.src.processors.tracker.get_service_ticket') as mock_service_ticket:
        mock_service_ticket.return_value = 'some_ticket'
        with patch('intranet.compositor_processors.src.processors.tracker.TRACKER_RETRY') as tracker_retry:
            tracker_retry.return_value = 1
            with test_vcr.use_cassette('tracker_create_queue_fail.yaml'):
                with pytest.raises(BackendError):
                    create_queue(
                        uid='1120000000016772',
                        org_id=123,
                        queue_name='dogma',
                        queue_key='DOGMA',
                    )


def test_create_queue_after_error_success(test_vcr):
    with patch('intranet.compositor_processors.src.processors.tracker.get_service_ticket') as mock_service_ticket:
        mock_service_ticket.return_value = 'some_ticket'
        with test_vcr.use_cassette('tracker_create_queue_after_error_success.yaml'):
            create_queue(
                uid='1120000000016772',
                org_id=123,
                queue_name='dogma',
                queue_key='DOGMA',
            )


def test_create_queue_on_conflict_success(test_vcr):
    with patch('intranet.compositor_processors.src.processors.tracker.get_service_ticket') as mock_service_ticket:
        mock_service_ticket.return_value = 'some_ticket'
        with test_vcr.use_cassette('tracker_create_queue_on_conflict_success.yaml'):
            create_queue(
                uid='1120000000016772',
                org_id=123,
                queue_name='dogma',
                queue_key='DOGMA',
            )


def test_create_ticket_success(test_vcr):
    with patch('intranet.compositor_processors.src.processors.tracker.get_service_ticket') as mock_service_ticket:
        mock_service_ticket.return_value = 'some_ticket'
        with test_vcr.use_cassette('tracker_create_ticket_success.yaml'):
            result = create_ticket(
                uid='4034386806',
                website='yandex.ru',
                org_id=103432,
                queue_key='DOGMA',
            )
            assert result == {'ticket_key': 'DOGMA-156'}


def test_create_ticket_fail(test_vcr):
    with patch('intranet.compositor_processors.src.processors.tracker.get_service_ticket') as mock_service_ticket:
        mock_service_ticket.return_value = 'some_ticket'
        with patch('intranet.compositor_processors.src.processors.tracker.TRACKER_RETRY') as tracker_retry:
            tracker_retry.return_value = 1
            with test_vcr.use_cassette('tracker_create_ticket_fail.yaml'):
                with pytest.raises(BackendError):
                    create_ticket(
                        uid='4034386806',
                        website='yandex.ru',
                        org_id=103432,
                        queue_key='DOGMA',
                    )
