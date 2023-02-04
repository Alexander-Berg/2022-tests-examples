import requests

from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    add_comment_for_order_status_update, create_route_env,
    delete_comment_from_order_status_update_event,
    get_order, get_order_details, get_courier_quality,
    patch_order_by_order_id
)


def _check_order_status_comments(system_env_with_db, order_id, depot_id, date, expected_order_status_update_event):
    order = get_order(system_env_with_db, order_id)
    assert order['order_status_comments'] == [expected_order_status_update_event]

    order_details = get_order_details(system_env_with_db, order['number'])
    assert order_details['order_status_comments'] == [expected_order_status_update_event]

    courier_report = get_courier_quality(system_env_with_db, date=date, depot_id=depot_id)
    assert courier_report[0]['order_status_comments'] == [expected_order_status_update_event]


def test_comment_for_status_update_event(system_env_with_db):
    with create_route_env(system_env_with_db, 'test_order_status_comment') as route_env:
        order_id = route_env['orders'][0]['id']
        depot_id = route_env['depot']['id']
        date = route_env['route']['date']

        new_status = 'confirmed'
        _, order = patch_order_by_order_id(system_env_with_db, order_id, {'status': new_status})
        expected_order_status_update_event_no_comment = {
            'id': 1,
            'status': new_status
        }
        assert order['order_status_comments'] == [expected_order_status_update_event_no_comment]

        comment = f'Comment for {new_status} status.'
        code, order_status_update_comment = add_comment_for_order_status_update(system_env_with_db, order_id, expected_order_status_update_event_no_comment["id"], comment)
        assert code == requests.codes.ok
        expected_order_status_update_event = {
            **expected_order_status_update_event_no_comment,
            'comment': comment
        }
        assert order_status_update_comment == expected_order_status_update_event
        _check_order_status_comments(system_env_with_db, order_id, depot_id, date, expected_order_status_update_event)

        comment = ''
        code, order_status_update_comment = add_comment_for_order_status_update(system_env_with_db, order_id, expected_order_status_update_event_no_comment["id"], comment)
        assert code == requests.codes.ok

        comment = None
        code, order_status_update_comment = add_comment_for_order_status_update(system_env_with_db, order_id, expected_order_status_update_event_no_comment["id"], comment)
        assert code == requests.codes.ok

        code, order_status_update_comment = delete_comment_from_order_status_update_event(system_env_with_db, order_id, expected_order_status_update_event["id"])
        assert code == requests.codes.ok
        assert order_status_update_comment == expected_order_status_update_event_no_comment
        _check_order_status_comments(system_env_with_db, order_id, depot_id, date, expected_order_status_update_event_no_comment)


def test_invalid_comment_for_status_update_event(system_env_with_db):
    with create_route_env(system_env_with_db, 'test_invalid_order_status_comment') as route_env:
        order_id = route_env['orders'][0]['id']
        comment = 'Some comment'

        incorrect_status_update_event_id = 0

        code, order_status_update_comment = add_comment_for_order_status_update(system_env_with_db, order_id, incorrect_status_update_event_id, comment)
        assert code == requests.codes.unprocessable
        assert order_status_update_comment['message'] == f'Event {incorrect_status_update_event_id} from order {order_id} must be of STATUS_UPDATE type, while it is ORDER_CREATED.'

        code, order_status_update_comment = delete_comment_from_order_status_update_event(system_env_with_db, order_id, incorrect_status_update_event_id)
        assert code == requests.codes.unprocessable
        assert order_status_update_comment['message'] == f'Event {incorrect_status_update_event_id} from order {order_id} must be of STATUS_UPDATE type, while it is ORDER_CREATED.'

        incorrect_status_update_event_id = 2

        code, order_status_update_comment = add_comment_for_order_status_update(system_env_with_db, order_id, incorrect_status_update_event_id, comment)
        assert code == requests.codes.not_found
        assert order_status_update_comment['message'] == f'Event {incorrect_status_update_event_id} was not found for order {order_id}.'

        code, order_status_update_comment = delete_comment_from_order_status_update_event(system_env_with_db, order_id, incorrect_status_update_event_id)
        assert code == requests.codes.not_found
        assert order_status_update_comment['message'] == f'Event {incorrect_status_update_event_id} was not found for order {order_id}.'


def test_delete_non_existing_comment(system_env_with_db):
    with create_route_env(system_env_with_db, 'test_delete_non_existing_comment') as route_env:
        order_id = route_env['orders'][0]['id']

        patch_order_by_order_id(system_env_with_db, order_id, {'status': 'finished'})
        status_update_event_id = 1

        code, order_status_update_comment = delete_comment_from_order_status_update_event(system_env_with_db, order_id, status_update_event_id)
        assert code == requests.codes.not_found
        assert order_status_update_comment['message'] == f'No comment in event {status_update_event_id} from order {order_id}.'


def test_comment_for_confirmed_order(system_env_with_db):
    with create_route_env(system_env_with_db, 'test_comment_for_confirmed_status', order_status='confirmed') as route_env:
        order_id = route_env['orders'][0]['id']
        depot_id = route_env['depot']['id']
        date = route_env['route']['date']

        order = get_order(system_env_with_db, order_id)
        expected_order_status_update_event_no_comment = {
            'id': 1,
            'status': 'confirmed'
        }
        assert order['order_status_comments'] == [expected_order_status_update_event_no_comment]

        comment = 'Comment for `confirmed` status.'
        code, order_status_update_comment = add_comment_for_order_status_update(system_env_with_db, order_id, expected_order_status_update_event_no_comment["id"], comment)
        assert code == requests.codes.ok
        expected_order_status_update_event = {
            **expected_order_status_update_event_no_comment,
            'comment': comment
        }
        assert order_status_update_comment == expected_order_status_update_event
        _check_order_status_comments(system_env_with_db, order_id, depot_id, date, expected_order_status_update_event)
