import os
from infra.ya_salt.lib import modutil


def test_get_mod_info():
    assert modutil.get_mod_info('nvidia', './') == (None, None)
    os.makedirs('module/nvidia')
    info, err = modutil.get_mod_info('nvidia', './')
    assert info.version == 'unknown'
    assert err is None
    with open('module/nvidia/version', 'w') as f:
        f.write('0x8233\n')
    info, err = modutil.get_mod_info('nvidia', './')
    assert info.version == '0x8233'
    assert err is None
