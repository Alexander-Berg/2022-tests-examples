import logging

from helpers import load_test_config, perform_test, PASS


def test_case(postgres, validator, test_name):
    test_data = load_test_config(test_name)
    status, info = perform_test(test_data, postgres, validator)
    if info:
        logging.error(info)
    assert status == PASS
