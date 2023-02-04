import io

import yatest.common

from infra.ya_salt.lib import grub


def test_parse_grub_default():
    tests = [
        ('set default="0"', True, '0'),
        ('set default="0...', False, None),
    ]
    for buf, ok, default_name in tests:
        grub1, err = grub.parse_file(io.BytesIO(buf))
        if ok:
            assert err is None
            assert grub1.default_name == default_name
        else:
            assert err


def test_parse_grub_precise():
    with open(yatest.common.source_path('infra/ya_salt/lib/tests/test_grub_precise.cfg')) as f:
        grub1, err = grub.parse_file(f)
    assert err is None
    assert grub1.default_name == '0'
    assert len(grub1.menu_entries) == 8
    assert ['Ubuntu, with Linux 4.14.78-30', 'Ubuntu, with Linux 4.14.78-30 (recovery mode)',
            'Previous Linux versions>Ubuntu, with Linux 4.14.78-29',
            'Previous Linux versions>Ubuntu, with Linux 4.14.78-29 (recovery mode)',
            'Previous Linux versions>Ubuntu, with Linux 4.14.69-27',
            'Previous Linux versions>Ubuntu, with Linux 4.14.69-27 (recovery mode)',
            'Previous Linux versions>Ubuntu, with Linux 4.14.69-26',
            'Previous Linux versions>Ubuntu, with Linux 4.14.69-26 (recovery mode)'] == [i.name for i in
                                                                                         grub1.menu_entries]
    assert ['4.14.78-30', '4.14.78-30', '4.14.78-29', '4.14.78-29', '4.14.69-27', '4.14.69-27', '4.14.69-26',
            '4.14.69-26'], [i.kernel_version for i in
                            grub1.menu_entries]
    v, err = grub1.get_boot_version()
    assert err is None
    assert v == '4.14.78-30'


def test_parse_grub_precise2():
    with open(yatest.common.source_path('infra/ya_salt/lib/tests/test_grub_precise2.cfg')) as f:
        grub1, err = grub.parse_file(f)
    assert err is None
    assert grub1.default_name == 'Previous Linux versions>Ubuntu, with Linux 4.4.114-50'
    assert len(grub1.menu_entries) == 10
    assert ['Ubuntu, with Linux 4.14.94-38',
            'Ubuntu, with Linux 4.14.94-38 (recovery mode)',
            'Previous Linux versions>Ubuntu, with Linux 4.14.80-33',
            'Previous Linux versions>Ubuntu, with Linux 4.14.80-33 (recovery mode)',
            'Previous Linux versions>Ubuntu, with Linux 4.14.78-32',
            'Previous Linux versions>Ubuntu, with Linux 4.14.78-32 (recovery mode)',
            'Previous Linux versions>Ubuntu, with Linux 4.14.78-31',
            'Previous Linux versions>Ubuntu, with Linux 4.14.78-31 (recovery mode)',
            'Previous Linux versions>Ubuntu, with Linux 4.4.114-50',
            'Previous Linux versions>Ubuntu, with Linux 4.4.114-50 (recovery mode)',
            ] == [i.name for i in
                  grub1.menu_entries]
    assert ['4.14.78-30', '4.14.78-30', '4.14.78-29', '4.14.78-29', '4.14.69-27', '4.14.69-27', '4.14.69-26',
            '4.14.69-26'], [i.kernel_version for i in
                            grub1.menu_entries]
    v, err = grub1.get_boot_version()
    assert err is None
    assert v == '4.4.114-50'


def test_parse_grub_xenial():
    with open(yatest.common.source_path('infra/ya_salt/lib/tests/test_grub_xenial.cfg')) as f:
        grub1, err = grub.parse_file(f)
    assert err is None
    assert grub1.default_name == '0'
    assert len(grub1.menu_entries) == 13
    assert ['Ubuntu', 'Advanced options for Ubuntu>Ubuntu, with Linux 4.14.78-30',
            'Advanced options for Ubuntu>Ubuntu, with Linux 4.14.78-30 (upstart)',
            'Advanced options for Ubuntu>Ubuntu, with Linux 4.14.78-30 (recovery mode)',
            'Advanced options for Ubuntu>Ubuntu, with Linux 4.14.78-29',
            'Advanced options for Ubuntu>Ubuntu, with Linux 4.14.78-29 (upstart)',
            'Advanced options for Ubuntu>Ubuntu, with Linux 4.14.78-29 (recovery mode)',
            'Advanced options for Ubuntu>Ubuntu, with Linux 4.4.114-50',
            'Advanced options for Ubuntu>Ubuntu, with Linux 4.4.114-50 (upstart)',
            'Advanced options for Ubuntu>Ubuntu, with Linux 4.4.114-50 (recovery mode)',
            'Advanced options for Ubuntu>Ubuntu, with Linux 4.4.0-21-generic',
            'Advanced options for Ubuntu>Ubuntu, with Linux 4.4.0-21-generic (upstart)',
            'Advanced options for Ubuntu>Ubuntu, with Linux 4.4.0-21-generic (recovery mode)'] == [i.name for i in
                                                                                                   grub1.menu_entries]
    assert ['4.14.78-30',
            '4.14.78-30',
            '4.14.78-30',
            '4.14.78-30',
            '4.14.78-29',
            '4.14.78-29',
            '4.14.78-29',
            '4.4.114-50',
            '4.4.114-50',
            '4.4.114-50',
            '4.4.0-21-generic',
            '4.4.0-21-generic',
            '4.4.0-21-generic'], [i.kernel_version for i in grub1.menu_entries]
    v, err = grub1.get_boot_version()
    assert err is None
    assert v == '4.14.78-30'
