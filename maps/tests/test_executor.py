import logging
import typing as tp
from collections.abc import Callable
from io import StringIO
from pathlib import Path
from unittest.mock import Mock, create_autospec, call

import pytest

from maps.infra.sedem.sandbox.config_applier.lib.executor import (
    Applier,
    ApplierFailure,
    Context,
    Executor,
    ExecutionStatus,
    ServiceConfigProxy,
)
from maps.infra.sedem.sandbox.config_applier.tests.mock_service_config import mock_config


ApplierCallable = Callable[[ServiceConfigProxy, Context], Context]


@pytest.fixture
def applier_cls_factory() -> Callable[[], Mock]:
    def factory(applier: tp.Optional[Applier] = None) -> Mock:
        mock_applier_cls = create_autospec(Applier)
        if applier:
            mock_applier_cls.return_value = applier
        return mock_applier_cls
    return factory


@pytest.fixture
def applier_factory() -> Callable[[], Mock]:
    def factory(is_applicable: bool = True,
                side_effect: tp.Union[None, ApplierCallable, Exception] = None) -> Mock:
        mock_applier = create_autospec(Applier, instance=True)
        mock_applier.is_applicable_for.return_value = is_applicable
        if side_effect:
            mock_applier.side_effect = side_effect
        return mock_applier
    return factory


def test_create(applier_cls_factory) -> None:
    mock_applier_cls1 = applier_cls_factory()
    mock_applier_cls2 = applier_cls_factory()

    Executor(
        oauth_token='token',
        arcadia_root=Path('/arcadia'),
        arcadia_revision=42,
        work_dirs_root=Path('/tmp'),
        applier_classes=[mock_applier_cls1, mock_applier_cls2],
    )

    for mock_applier_cls in (mock_applier_cls1, mock_applier_cls2):
        assert mock_applier_cls.call_args == call(
            oauth_token='token',
            arcadia_root=Path('/arcadia'),
            arcadia_revision=42,
            work_dirs_root=Path('/tmp'),
        )


@pytest.mark.parametrize('exception_type,expected_status', [(Exception, ExecutionStatus.EXCEPTION),
                                                            (ApplierFailure, ExecutionStatus.FAILURE)])
def test_execute_one(applier_cls_factory, applier_factory, exception_type, expected_status) -> None:
    mock_applier1 = applier_factory(side_effect=lambda _, context: context | {'applier1_passed': True})
    mock_applier2 = applier_factory(is_applicable=False,
                                    side_effect=lambda _, context: context | {'applier2_passed': True})
    mock_applier3 = applier_factory(side_effect=lambda _, context: context | {'applier3_passed': True})
    mock_applier4 = applier_factory(side_effect=exception_type('smth went wrong!'))
    mock_applier5 = applier_factory(side_effect=lambda _, context: context | {'applier5_passed': True})

    executor = Executor(
        oauth_token='token',
        arcadia_root=Path('/arcadia'),
        arcadia_revision=42,
        work_dirs_root=Path('/tmp'),
        applier_classes=[applier_cls_factory(mock_applier)
                         for mock_applier in (mock_applier1,
                                              mock_applier2,
                                              mock_applier3,
                                              mock_applier4,
                                              mock_applier5)],
    )
    config = mock_config('maps/mock')

    result = executor.execute_one(config)

    assert result.service_name == 'maps-core-mock'
    assert result.context == Context(
        applier1_passed=True,
        applier3_passed=True,
    )
    assert result.status == expected_status

    assert mock_applier1.mock_calls == [
        call.is_applicable_for(config),
        call(config, context=Context()),
    ]
    assert mock_applier2.mock_calls == [
        call.is_applicable_for(config),
    ]
    assert mock_applier3.mock_calls == [
        call.is_applicable_for(config),
        call(config, context=Context(applier1_passed=True)),
    ]
    assert mock_applier4.mock_calls == [
        call.is_applicable_for(config),
        call(config, context=Context(applier1_passed=True, applier3_passed=True)),
    ]
    assert mock_applier5.mock_calls == []


@pytest.mark.parametrize('in_parallel', (False, True), ids=('in_series', 'in_parallel'))
def test_execute(applier_cls_factory, applier_factory, in_parallel: bool) -> None:
    logger = logging.getLogger('test-execute')
    logger.propagate = False
    log_stream = StringIO()
    logger.addHandler(logging.StreamHandler(log_stream))

    def apply_config(config: ServiceConfigProxy, context: Context) -> Context:
        if config.service_name() == 'maps-core-mock2':
            logger.info('applier failed')
            raise Exception('smth went wrong!')
        logger.info('applier passed')
        return context | {'applier_passed': True}

    mock_applier = applier_factory(side_effect=apply_config)

    executor = Executor(
        oauth_token='token',
        arcadia_root=Path('/arcadia'),
        arcadia_revision=42,
        work_dirs_root=Path('/tmp'),
        applier_classes=[applier_cls_factory(mock_applier)],
    )

    configs = [mock_config('maps/mock1'), mock_config('maps/mock2'), mock_config('maps/mock3')]

    if in_parallel:
        results = executor.execute_in_parallel(configs)
    else:
        results = executor.execute(configs)

    processed_services, failed_services = [], []
    for result in results:
        if result.status != ExecutionStatus.SUCCESS:
            failed_services.append(result.service_name)
            assert result.context == Context()
        else:
            processed_services.append(result.service_name)
            assert result.context == Context(applier_passed=True)

    assert sorted(processed_services) == [
        'maps-core-mock1',
        'maps-core-mock3',
    ]

    assert sorted(failed_services) == [
        'maps-core-mock2',
    ]

    assert sorted(log_stream.getvalue().splitlines()) == [
        '[maps-core-mock1] applier passed',
        '[maps-core-mock2] applier failed',
        '[maps-core-mock3] applier passed',
    ]
