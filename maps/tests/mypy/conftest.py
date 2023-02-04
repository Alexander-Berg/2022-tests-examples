def mypy_check_root() -> str:
    return 'maps/b2bgeo/identity/libs/pytest_lib/'


def mypy_config_resource() -> tuple[str, str]:
    return '__tests__', 'config/mypy_strict.ini'
