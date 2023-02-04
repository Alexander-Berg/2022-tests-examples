import os

from kelvin.settings import get_setting


def test_get_setting(mocker):
    # string
    mocker.patch('os.getenv', lambda x: 'string')
    assert get_setting('A', 'hello') == 'string'

    # string default value
    mocker.patch('os.getenv', lambda x: None)
    assert get_setting('A', 'hello') == 'hello'
    assert get_setting('A', 'hello', str) == 'hello'

    # int
    mocker.patch('os.getenv', lambda x: '15')
    assert get_setting('A', 20) == 15
    assert get_setting('A', '20', int) == 15

    # int default value
    mocker.patch('os.getenv', lambda x: None)
    assert get_setting('A', 20) == 20
    assert get_setting('A', 20, int) == 20

    # bool
    mocker.patch('os.getenv', lambda x: 'true')
    assert get_setting('A', False) is True

    mocker.patch('os.getenv', lambda x: 'false')
    assert get_setting('A', True) is False

    # bool default
    mocker.patch('os.getenv', lambda x: None)
    assert get_setting('A', True) is True
    assert get_setting('A', False) is False

    # str many
    mocker.patch('os.getenv', lambda x: 'alpha, beta')
    try:
        get_setting('A', 'gamma', many=True)
    except Exception as err:
        assert isinstance(err, RuntimeError)

    assert get_setting('A', ['gamma'], many=True) == ['alpha', 'beta']
    assert get_setting('A', ['gamma']) == ['alpha', 'beta']
    assert get_setting('A', []) == ['alpha', 'beta']

    mocker.patch('os.getenv', lambda x: ' 1, 2, 3  ')
    assert get_setting('A', [4, 5], many=True) == [1, 2, 3]
    assert get_setting('A', [4, 5]) == [1, 2, 3]
    assert get_setting('A', [], int) == [1, 2, 3]
    assert get_setting('A', []) == ['1', '2', '3']

    mocker.patch('os.getenv', lambda x: ' 1, 0, true, false, yes, no')
    assert get_setting('A', [True, False]) == [True, False, True, False, True, False]
