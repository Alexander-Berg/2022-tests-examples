import asyncio
import logging
from pathlib import Path
from typing import Any, Dict, List

import pytest

from hamcrest import assert_that, contains, contains_inanyorder, equal_to, greater_than, greater_than_or_equal_to

from billing.yandex_pay.tools.fim.lib.api.app import FIMApplication
from billing.yandex_pay.tools.fim.lib.checker import run_periodic_checks
from billing.yandex_pay.tools.fim.lib.common import dict_to_checksums


@pytest.fixture
def checksum_dict(dummy_files_dict, dummy_dir) -> Dict[Path, str]:
    return {
        path: f'sha256:{dummy_files_dict[path.name][1]}'
        for path in dummy_dir.iterdir()
    }


@pytest.fixture
def checksum_file(checksum_dict, tmp_path) -> Path:
    checksums = tmp_path / 'checksums.txt'
    checksums.write_text(dict_to_checksums(checksum_dict), 'utf-8')
    return checksums


async def _maybe_cancel_task(task: asyncio.Task) -> None:
    if not task.done():
        task.cancel()
        try:
            await task
        except asyncio.CancelledError:
            pass


@pytest.fixture
async def checker_task(loop, checksum_file, dummy_logger, caplog) -> asyncio.Task:
    caplog.set_level(logging.DEBUG, logger=dummy_logger.logger.name)

    task: asyncio.Task = loop.create_task(
        run_periodic_checks(checksum_file, sleep=0.1, logger=dummy_logger)
    )

    yield task

    await _maybe_cancel_task(task)


@pytest.fixture
async def failing_task(loop) -> asyncio.Task:
    async def _failing_coro():
        raise Exception('boom')

    task = loop.create_task(_failing_coro())

    yield task

    await _maybe_cancel_task(task)


@pytest.fixture
async def app(aiohttp_client, checker_task):
    return await aiohttp_client(FIMApplication(checker_task))


class TestHealthCheck:
    @pytest.mark.asyncio
    async def test_healthcheck_works(self, app):
        resp = await app.get('/healthcheck')

        assert_that(resp.status, equal_to(200))
        assert_that(await resp.text(), equal_to('ok'))

    @pytest.mark.asyncio
    async def test_healthcheck_task_cancelled(self, app, checker_task):
        checker_task.cancel()
        try:
            await checker_task
        except asyncio.CancelledError:
            pass

        resp = await app.get('/healthcheck')
        assert_that(resp.status, equal_to(400))
        assert_that(await resp.text(), equal_to('FIM task cancelled'))

    @pytest.mark.asyncio
    async def test_healthcheck_task_failed(self, failing_task, aiohttp_client):
        app = await aiohttp_client(FIMApplication(failing_task))

        resp = await app.get('/healthcheck')
        assert_that(resp.status, equal_to(400))
        assert_that(await resp.text(), equal_to('FIM task exited'))


@pytest.mark.asyncio
async def test_ping(app, checker_task):
    resp = await app.get('/ping')
    assert_that(resp.status, equal_to(200))
    assert_that(await resp.text(), equal_to('pong'))

    checker_task.cancel()
    try:
        await checker_task
    except asyncio.CancelledError:
        pass

    resp = await app.get('/ping')
    assert_that(resp.status, equal_to(200))
    assert_that(await resp.text(), equal_to('pong'))


class TestStats:
    async def _wait_for_logs(
        self,
        loop: asyncio.AbstractEventLoop,
        caplog: Any,
        logger_name: str,
        num_logs: int = 1,
        max_wait: float = 6.0,
        levelno: int = logging.DEBUG,
    ) -> List[logging.LogRecord]:
        start = loop.time()

        while True:
            logs = [
                r for r in caplog.records
                if r.name == logger_name and r.levelno >= levelno
            ]
            if len(logs) >= num_logs:
                return logs
            if loop.time() - start > max_wait:
                raise RuntimeError('Max wait time exhausted')
            await asyncio.sleep(0.05)

    @pytest.mark.asyncio
    async def test_unistat_success(
        self, caplog, loop, dummy_logger, app, checksum_dict, checker_task
    ):
        # wait for one check to complete
        await self._wait_for_logs(loop, caplog, dummy_logger.logger.name)

        resp = await app.get('/unistat')
        assert_that(resp.status, equal_to(200))
        resp_body = await resp.json()

        assert_that(
            resp_body,
            contains_inanyorder(
                ['fim_check_failed_summ', 0.0],
                contains(
                    'fim_check_passed_summ',
                    greater_than_or_equal_to(len(checksum_dict)),
                )
            )
        )

        await _maybe_cancel_task(checker_task)

        # unistat works if task exits
        resp = await app.get('/unistat')
        assert_that(resp.status, equal_to(200))

    @pytest.mark.asyncio
    async def test_unistat_failure(
        self, caplog, loop, dummy_logger, dummy_dir, app, checksum_dict, checker_task
    ):
        hacked: Path = next(f for f in dummy_dir.iterdir() if f.is_file())
        hacked.write_bytes(hacked.read_bytes() + b'\x00')
        caplog.clear()

        await self._wait_for_logs(
            loop, caplog, dummy_logger.logger.name, levelno=logging.CRITICAL
        )
        resp = await app.get('/unistat')
        assert_that(resp.status, equal_to(200))
        resp_body = await resp.json()

        assert_that(
            resp_body,
            contains_inanyorder(
                contains(
                    'fim_check_failed_summ',
                    greater_than(0),
                ),
                contains(
                    'fim_check_passed_summ',
                    greater_than_or_equal_to(len(checksum_dict) - 1),
                ),
            )
        )

    @pytest.mark.asyncio
    async def test_unistat_reports_if_checksum_file_unreadable(
        self, loop, checksum_file, dummy_logger, aiohttp_client, caplog
    ):
        checksum_file.unlink()
        task: asyncio.Task = loop.create_task(
            run_periodic_checks(checksum_file, sleep=0.1, logger=dummy_logger)
        )
        app = await aiohttp_client(FIMApplication(task))

        await self._wait_for_logs(
            loop, caplog, dummy_logger.logger.name, levelno=logging.ERROR
        )
        resp = await app.get('/unistat')
        assert_that(resp.status, equal_to(200))
        resp_body = await resp.json()

        assert_that(
            resp_body,
            contains_inanyorder(
                ['fim_check_failed_summ', 1],
                ['fim_check_passed_summ', 0],
            )
        )
