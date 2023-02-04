import pytest


MIDDLEWARE_CLASS_STRING = 'kelvin.common.middleware.TVMMiddleware'


@pytest.fixture
def enable_tvm_middleware(settings, mocker):
    """
    Принудительно включает TVMMiddleware в тесте
    """
    if MIDDLEWARE_CLASS_STRING in settings.MIDDLEWARE:
        return

    settings.MIDDLEWARE += (MIDDLEWARE_CLASS_STRING, )


@pytest.fixture
def disable_tvm_middleware(settings):
    """
    Принудительно выключает TVMMiddleware в тесте
    """
    settings.MIDDLEWARE = tuple([
        m for m in settings.MIDDLEWARE if
        m != MIDDLEWARE_CLASS_STRING
    ])
