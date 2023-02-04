from ya_courier_backend.resources.route_details import _filter_route_state_orders


def test_filtering():
    test_cases = [
        {
            'orders_to_keep': set(),
            'state_in': {},
            'state_out': {},
        },
        {
            'orders_to_keep': set(),
            'state_in': {
                'routed_orders': [],
                'fixed_orders': [],
                'finished_orders': [],
                'next_orders': []
            },
            'state_out': {
                'routed_orders': [],
                'fixed_orders': [],
                'finished_orders': [],
                'next_orders': []
            },
        },
        {
            'orders_to_keep': set([1, 3]),
            'state_in': {
                'next_order': {'id': 2}
            },
            'state_out': {
            },
        },
        {
            'orders_to_keep': set([1, 2, 3]),
            'state_in': {
                'next_order': {'id': 2}
            },
            'state_out': {
                'next_order': {'id': 2}
            },
        },
        {
            'orders_to_keep': set([2, 4]),
            'state_in': {
                'routed_orders': [{'id': 1}, {'id': 2}, {'id': 3}],
                'fixed_orders': [1, 2, 3],
                'finished_orders': [1, 2, 3],
                'next_orders': [1, 2, 3],
            },
            'state_out': {
                'routed_orders': [{'id': 2}],
                'fixed_orders': [2],
                'finished_orders': [2],
                'next_orders': [2],
            },
        }
    ]

    for test_case in test_cases:
        state = test_case['state_in'].copy()
        _filter_route_state_orders(test_case['orders_to_keep'], state)
        assert state == test_case['state_out']
