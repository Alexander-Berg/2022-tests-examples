import pytest

from unittest.mock import patch, MagicMock

from src.vacation.main import flow_api, get_approvers, main


@pytest.mark.parametrize('heads_logins, expected', (
    pytest.param([], [], id='no-heads'),
    pytest.param(['head'], ['head', 'robot-ok-tracker'], id='single-head'),
    pytest.param(
        ['replace-head', 'top'],
        ['new-head', 'replace-head', 'top', 'robot-ok-tracker'],
        id='first-head-has-replace',
    ),
    pytest.param(
        ['head', 'replace-head'],
        ['head', 'replace-head', 'robot-ok-tracker'],
        id='second-head-has-replace',
    ),
    pytest.param(
        ['head', 'exc-head-2', 'top'],
        ['head', 'top', 'robot-ok-tracker'],
        id='head-is-excluded',
    ),
    pytest.param(
        ['head1', 'head2', 'head1', 'top'],
        ['head2', 'head1', 'top', 'robot-ok-tracker'],
        id='head-is-duplicated',
    ),
    # Пока непонятно, будет ли в реальности такой кейс, и как такое обрабатывать :)
    pytest.param(
        ['replace-head', 'top', 'replace-head', 'new-head'],
        ['top', 'replace-head', 'new-head', 'robot-ok-tracker'],
        id='head-with-replace-is-duplicated',
    ),
))
@patch('src.vacation.main.replace_heads_map', {'replace-head': 'new-head'})
@patch('src.vacation.main.exclude_heads', {'exc-head-1', 'exc-head-2'})
def test_vacation_get_approvers(heads_logins, expected):
    result = get_approvers(heads_logins)
    assert result == expected


@patch('src.vacation.main.set_approvement_field')
@patch('src.vacation.main.add_approvers_group')
def test_main_without_approvers(mocked_add_approvers_group, mocked_set_approvement_field):
    flow_api.Person().heads_chain = []
    main({'employee': 'employee-login'})

    assert not mocked_add_approvers_group.called
    mocked_set_approvement_field.assert_called_once_with(
        approve_if_no_approvers=True,
        text='Отпуск согласован автоматически.\nThe vacation was automatically approved.',
    )


@patch('src.vacation.main.set_approvement_field')
@patch('src.vacation.main.add_approvers_group')
def test_main_with_approvers(mocked_add_approvers_group, mocked_set_approvement_field):
    heads = ['head', 'top']
    expected_approvers = ['head', 'top', 'robot-ok-tracker']
    flow_api.Person().heads_chain = [MagicMock(login=login) for login in heads]
    main({'employee': 'employee-login'})

    mocked_add_approvers_group.assert_called_once_with(
        expected_approvers,
        is_with_deputies=True,
    )
    mocked_set_approvement_field.assert_called_once_with(
        text=(
            'У укого:employee-login запланирован отпуск. '
            'Подтвердите, пожалуйста.\nPlease confirm the vacation for кто:employee-login.'
        ),
        is_reject_allowed=False,
    )
