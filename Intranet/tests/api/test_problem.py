import pytest


@pytest.mark.parametrize('gap_type', ('duty', 'manual', None))
def test_get_problem(
    client, problem_factory, assert_json_keys_value_equal,
    gap_factory, gap_type, manual_gap_factory, assert_count_queries,
):
    if gap_type == 'duty':
        gap = gap_factory()
        problem = problem_factory(duty_gap=gap)
    elif gap_type == 'manual':
        gap = manual_gap_factory()
        problem = problem_factory(manual_gap=gap)
    else:
        problem = problem_factory()
    problem_id = problem.id
    with assert_count_queries(2):
        response = client.get(
            f'/api/watcher/v1/problem/{problem_id}',
        )
    assert response.status_code == 200, response.text
    data = response.json()
    expected = {
        'id': problem.id,
        'shift_id': problem.shift_id,
        'staff_id': problem.staff_id,
        'reason': problem.reason,
    }
    assert_json_keys_value_equal(data, expected)

    assert data['staff']['login'] == problem.staff.login
    assert data['shift']['schedule_id'] == problem.shift.schedule_id

    if gap_type:
        expected_gap = {
            'id': gap.id,
            'start': gap.start.isoformat(),
            'end': gap.end.isoformat(),
            'staff_id': gap.staff_id,
            'type': None,
            'title': None,
            'comment': None,
            'is_manual': gap_type == 'manual'
        }
        if gap_type == 'duty':
            expected_gap['type'] = gap.type
        else:
            expected_gap['title'] = gap.gap_settings.title
            expected_gap['comment'] = gap.gap_settings.comment
        assert_json_keys_value_equal(data['gap'], expected_gap)
    else:
        assert not data['gap']


def test_list_problem(client, problem_factory):
    expected = [problem_factory() for _ in range(2)]
    response = client.get(
        '/api/watcher/v1/problem/',
    )
    assert response.status_code == 200, response.text
    data = response.json()['result']
    assert len(data) == 2
    assert {obj['id'] for obj in data} == {obj.id for obj in expected}
