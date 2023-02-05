import logging

from maps.garden.sandbox.server_autotests.lib import actions
from maps.garden.sandbox.server_autotests.lib import helper_functions as hf
from maps.garden.sandbox.server_autotests.lib.constants import EXAMPLE_DEPLOYMENT, TESTING_FUNCTIONS, TEST_CONTOUR_NAME_SUFFIX
from maps.garden.sandbox.server_autotests.lib.server_tester import ServerTester

from typing import Optional


logger = logging.getLogger("garden.server_autotests")


def run(user_name: str, server_hostname: str, *, contour_name: Optional[str] = None, fresh_start: bool = True, test_cases: Optional[tuple[str]] = None):
    if contour_name is None:
        contour_name = f"{user_name}_{TEST_CONTOUR_NAME_SUFFIX}"

    tester = ServerTester(server_hostname, contour_name=contour_name)
    tester.system_contour_name = hf.get_system_contour_name(tester)
    try:
        tester.user_contour = hf.check_if_user_contour(tester)
    except Exception:
        tester.user_contour = True
        # We couldn't find contour, that mean it needs to be created
        fresh_start = True

    # If fresh-start is True that means that either:
    # - we passed parameter fresh-start
    # - such dev contour doesn't exist
    # Thats why here we don't create new contour if fresh-start is False
    if fresh_start:
        if not tester.user_contour:
            raise Exception(f"You can`t delete system contour {tester.contour_name}!")
        # If contour we want to delete doesn't exist script won't fail
        actions.delete_user_contour(tester)
        actions.switch_to_user_contour(tester)

    if not test_cases:
        test_cases = [function.__name__ for function in TESTING_FUNCTIONS]

    if not tester.user_contour:
        actions.disable_autostart(tester, EXAMPLE_DEPLOYMENT)

    test_results = []

    logger.info(f"Starting autotests on {tester.session.base_url}")
    error = None

    for tester_function in TESTING_FUNCTIONS:
        if tester_function.__name__ in test_cases:
            try:
                logger.info(f"Starting test `{tester_function.__name__}`")
                tester_function(tester)
                logger.info(f"Finished test `{tester_function.__name__}`")
                test_results.append((tester_function.__name__, "SUCCESS"))
            except Exception as ex:
                logging.exception("Test `{tester_function.__name__}` failed:")
                test_results.append((tester_function.__name__, "FAILED"))
                error = f"Test `{tester_function.__name__}` failed: {str(ex)}"
                break

    if not tester.user_contour:
        actions.enable_autostart(tester, EXAMPLE_DEPLOYMENT)

    logger.info(f"Finished autotests on {tester.session.base_url}")

    for test_name, result in test_results:
        logger.info(f"Test `{test_name}`: {result}")

    if error:
        raise Exception(error)
