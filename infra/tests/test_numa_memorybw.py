import unittest.mock as mock


def blob_time():
    return 1634666358


def empty_output(*args, **kwargs):
    return b""


def test_get_numa_bandwith_empty(manifest):
    expected_result = {'events': [{'description': '{"status": "WARN", "timestamp": 1634666358, "reason": "list index out of range"}', 'service': 'numa_memorybw', 'status': 'WARN'}]}
    with mock.patch('subprocess.check_output', empty_output), mock.patch('builtins.open', mock.mock_open(read_data='model name: AMD EPYC')), mock.patch('time.time', blob_time):
        result = manifest.execute('numa_memorybw')
    print(result)
    assert result == expected_result


def ok_output(*args, **kwargs):
    return b"# function 'x86-64-movsb' (movsb-based memcpy() in arch/x86/lib/memcpy_64.S)\n4802903118.164617\n"


def test_get_numa_bandwith_ok(manifest):
    expected_result = {'events': [{'description': '{"status": "OK", "timestamp": 1634666358, "reason": "4.47 GB/s"}', 'service': 'numa_memorybw', 'status': 'OK'}]}
    with mock.patch('subprocess.check_output', ok_output), mock.patch('builtins.open', mock.mock_open(read_data='model name: AMD EPYC')), mock.patch('time.time', blob_time):
        result = manifest.execute('numa_memorybw')
    print(result)
    assert result == expected_result


def crit_output(*args, **kwargs):
    return b"# function 'x86-64-movsb' (movsb-based memcpy() in arch/x86/lib/memcpy_64.S)\n1302903118.164617\n"


def test_get_numa_bandwith_crit(manifest):
    expected_result = {'events': [{'description': '{"status": "CRIT", "timestamp": 1634666358, "reason": "1.21 GB/s"}', 'service': 'numa_memorybw', 'status': 'CRIT'}]}
    with mock.patch('subprocess.check_output', crit_output), mock.patch('builtins.open', mock.mock_open(read_data='model name: AMD EPYC')), mock.patch('time.time', blob_time):
        result = manifest.execute('numa_memorybw')
    print(result)
    assert result == expected_result
