import pytest
from ads_pytorch.core.badcode_validator import register_badcode_validator, BadCodeValidator


@pytest.fixture(scope="function", autouse=True)
def clear_callbacks():
    lst = []
    prev_val = BadCodeValidator.CALLBACKS[:]
    BadCodeValidator.CALLBACKS = lst
    try:
        yield
    finally:
        BadCodeValidator.CALLBACKS = prev_val


@pytest.mark.asyncio
async def test_register():
    called = False

    @register_badcode_validator
    def foo(*args, **kwargs):
        nonlocal called
        called = True

    await BadCodeValidator()(None, None, None, None)

    assert called


@pytest.mark.asyncio
async def test_register_async():
    called = False

    @register_badcode_validator
    async def foo(*args, **kwargs):
        nonlocal called
        called = True

    await BadCodeValidator()(None, None, None, None)

    assert called


@pytest.mark.asyncio
async def test_called_once():
    call_count = 0

    @register_badcode_validator
    def foo(*args, **kwargs):
        nonlocal call_count
        call_count += 1

    validator = BadCodeValidator()
    for _ in range(10):
        await validator(None, None, None, None)

    assert call_count == 1


def test_properties():
    x = BadCodeValidator()
    assert x.call_before_train
    assert not x.call_after_train
