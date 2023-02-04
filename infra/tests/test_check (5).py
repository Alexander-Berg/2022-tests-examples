import unittest.mock as mock


def test_incorrect_args(manifest):
    data = manifest.execute('is_file', arguments=('--unknown-opt', '/dev/null'))

    expected = {'events': [{'description': 'one of the arguments -e/--exists is required',
                            'service': 'is_file',
                            'status': 'CRIT'}]}
    assert expected == data


def test_shit_happens(manifest):
    with mock.patch('os.path.exists', mock.mock_open(), create=True) as m:
        m.side_effect = IOError('IO err happened')
        data = manifest.execute('is_file', arguments=('--exists', '/dev/null'))

    expected = {'events': [{'description': 'IO err happened',
                            'service': 'is_file',
                            'status': 'CRIT'}]}
    assert expected == data


def test_file_exists(manifest):
    with mock.patch('os.path.exists', return_value=True):
        data = manifest.execute('is_file', arguments=(
            '--exists',
            '--true-message', 'yep, exists',
            '--true-status', 'WARN',
            '/dev/null',
        ))

    expected = {'events': [{'description': 'yep, exists',
                            'service': 'is_file',
                            'status': 'WARN'}]}
    assert expected == data


def test_file_not_exists(manifest):
    with mock.patch('os.path.exists', return_value=False):
        data = manifest.execute('is_file', arguments=('--exists', '/dev/null'))

    expected = {'events': [{'description': 'False', 'service': 'is_file', 'status': 'CRIT'}]}
    assert expected == data
