import pytest
from sepelib.util.log import setup_logging_to_stdout


@pytest.fixture(scope="session", autouse=True)
def set_terminal_logging():
    setup_logging_to_stdout()
