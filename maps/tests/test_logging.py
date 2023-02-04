from dataclasses import dataclass
from io import StringIO
import logging
import typing as tp

import pytest

from maps.infra.sedem.sandbox.config_applier.lib.logging import (
    ThreadLocals,
    patch_log_format,
    set_in_scope,
)


@dataclass
class Context:
    value: tp.Optional[int] = None


def test_set_in_scope() -> None:
    ctx = Context()

    assert ctx.value is None

    with set_in_scope(ctx, value=1):
        assert ctx.value == 1

    assert ctx.value is None


def test_set_in_scope_not_an_attr() -> None:
    ctx = Context()

    assert not hasattr(ctx, 'not_an_attr')

    with set_in_scope(ctx, not_an_attr=None):
        assert ctx.not_an_attr is None

    assert not hasattr(ctx, 'not_an_attr')


def test_set_in_scope_with_exception() -> None:
    ctx = Context()

    assert ctx.value is None

    with pytest.raises(Exception):
        with set_in_scope(ctx, value=1):
            assert ctx.value == 1
            raise Exception()

    assert ctx.value is None


def test_patch_log_format() -> None:
    logger = logging.getLogger('test-logging')
    logger.propagate = False
    stream = StringIO()
    handler = logging.StreamHandler(stream)
    handler.setFormatter(logging.Formatter('%(message)s'))
    logger.addHandler(handler)

    thread_locals = ThreadLocals()
    thread_locals.service_name = 'test-service'

    logger.error('test old format')
    with patch_log_format(thread_locals):
        logger.error('test new format')
    logger.error('test old format')

    assert stream.getvalue().splitlines() == [
        'test old format',
        '[test-service] test new format',
        'test old format',
    ]
