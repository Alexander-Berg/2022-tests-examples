import pytest

import irt.logging


def test_wrong_project():
    with pytest.raises(ValueError):
        irt.logging.getLogger('Wrong_name', __name__)

    with pytest.raises(ValueError):
        irt.logging.getLogger(None, __name__, 'WRONG_LEVEL')


def test_correct_use():
    common_logger = irt.logging.getLogger(None, __name__)
    project_logger = irt.logging.getLogger(irt.logging.MULTIK_PROJECT, __name__)
    project_logger_with_log_level = irt.logging.getLogger('MULTIK', __name__, irt.logging.DEBUG)

    common_logger.info('Info log')
    project_logger.error('Error log')
    project_logger_with_log_level.debug('Debug log')
