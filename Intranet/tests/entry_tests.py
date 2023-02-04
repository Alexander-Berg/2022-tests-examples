from staff.budget_position.entry import entry


def test_entry_can_give_non_default_value():
    test_dict = {3: 7}

    result = entry(test_dict, 3).then(lambda v: v).collect()
    assert result == 7


def test_entry_can_give_default_value():
    test_dict = {3: 7}

    result = entry(test_dict, 4).then(lambda v: v).collect()
    assert not result


def test_entry_can_give_default_value_with_or_expr():
    test_dict = {3: 7}

    result = entry(test_dict, 4).then(lambda v: v).or_value(5).collect()
    assert result == 5


def test_entry_node_give_value():
    test_dict = {3: 7}

    result = entry(test_dict, 3).collect()
    assert result == 7


def test_entry_node_give_non_for_absent_key():
    test_dict = {3: 7}

    result = entry(test_dict, 4).collect()
    assert not result


def test_and_value_correct_result_for_none():
    test_dict = {3: None}

    result = entry(test_dict, 3).and_value().or_value('ok').collect()
    assert result == 'ok'


def test_and_value_correct_result_for_some_value():
    test_dict = {3: 5}

    result = entry(test_dict, 3).and_value().or_value('ok').collect()
    assert result == 5


def test_and_value_correct_result_for_some_value_and_then():
    test_dict = {3: 5}

    result = entry(test_dict, 3).and_value().then(lambda v: 'ok').collect()
    assert result == 'ok'


def test_and_value_correct_result_for_none_and_then():
    test_dict = {3: None}

    result = entry(test_dict, 3).and_value().then(lambda v: 'ok').collect()
    assert result is None
