import pytest
# import os

import irt.utils


def test_sandbox():
    assert irt.utils.sandbox.validate_uri('sbr://123')
    assert not irt.utils.sandbox.validate_uri('sbr://12dlks3')
    assert not irt.utils.sandbox.validate_uri('sbr://')
    assert not irt.utils.sandbox.validate_uri('http://')
    assert not irt.utils.sandbox.validate_uri('http')
    assert not irt.utils.sandbox.validate_uri('123')

    with pytest.raises(ValueError):
        irt.utils.sandbox.download('123')

    # assert irt.utils.sandbox.download('sbr://990691340')
    # assert os.path.exists('categories.json')
    # assert irt.utils.sandbox.download('sbr://990691340', 'new.json')
    # assert os.path.exists('new.json')
    # assert irt.utils.sandbox.download_latest('SANDBOX_UPLOAD_SCRIPT')
    # assert os.path.exists('upload.sfx.py')
    # assert irt.utils.sandbox.download_latest('SANDBOX_UPLOAD_SCRIPT', 'upload.sfx.new.py')
    # assert os.path.exists('upload.sfx.new.py')
