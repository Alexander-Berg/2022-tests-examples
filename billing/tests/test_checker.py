import asyncio
import logging
from pathlib import Path
from typing import Any, Dict, List

import pytest

from hamcrest import (
    all_of, assert_that, contains, contains_inanyorder, contains_string, equal_to, greater_than,
    greater_than_or_equal_to, has_length, has_properties, only_contains
)

from billing.yandex_pay.tools.fim.lib.checker import FIMChecker, run_periodic_checks
from billing.yandex_pay.tools.fim.lib.common import dict_to_checksums
from billing.yandex_pay.tools.fim.lib.stats import fim_check


@pytest.fixture
def checksum_dict(dummy_files_dict, dummy_dir) -> Dict[Path, str]:
    return {
        path: f'sha256:{dummy_files_dict[path.name][1]}'
        for path in dummy_dir.iterdir()
    }


class TestFIMChecker:
    @pytest.mark.asyncio
    async def test_check_successful(
        self, checksum_dict, dummy_files_dict, dummy_logger, caplog
    ):
        fim_checker = FIMChecker(checksum_dict, dummy_logger)
        passed, failed = await fim_checker.check()
        assert_that(passed, equal_to(len(dummy_files_dict)))
        assert_that(failed, equal_to(0))

        logs = [r for r in caplog.records if r.name == dummy_logger.logger.name]
        assert_that(logs, has_length(0))

    @pytest.mark.asyncio
    async def test_check_fails_missing_file(
        self, checksum_dict, dummy_files_dict, dummy_dir, dummy_logger, caplog
    ):
        missing = dummy_dir / 'missing'
        checksum_dict[missing] = 'bad'

        fim_checker = FIMChecker(checksum_dict, dummy_logger)
        passed, failed = await fim_checker.check()
        assert_that(passed, equal_to(len(dummy_files_dict)))
        assert_that(failed, equal_to(1))

        logs = [r for r in caplog.records if r.name == dummy_logger.logger.name]
        assert_that(logs, has_length(1))
        log_crit = logs[0]

        assert_that(
            log_crit,
            has_properties(
                levelno=logging.CRITICAL,
                message=all_of(
                    contains_string('INTEGRITY CHECK FAILED'),
                    contains_string(f"{{'{str(missing)}': 'LocationNotFoundError: '}}"),
                ),
            )
        )

    @pytest.mark.asyncio
    async def test_check_fails_hash_mismatch(
        self, checksum_dict, dummy_files_dict, dummy_dir, dummy_logger, caplog
    ):
        hacked: Path = next(f for f in dummy_dir.iterdir() if f.is_file())
        hacked.write_bytes(hacked.read_bytes() + b'\x00')

        fim_checker = FIMChecker(checksum_dict, dummy_logger)
        passed, failed = await fim_checker.check()
        assert_that(passed, equal_to(len(dummy_files_dict) - 1))
        assert_that(failed, equal_to(1))

        logs = [r for r in caplog.records if r.name == dummy_logger.logger.name]
        assert_that(
            logs,
            contains(
                has_properties(
                    levelno=logging.WARNING,
                    message=contains_string(f'Checksum mismatch for {str(hacked)}'),
                ),
                has_properties(
                    levelno=logging.CRITICAL,
                    message=contains_string('INTEGRITY CHECK FAILED'),
                ),
            )
        )


class TestPeriodicChecks:
    async def _wait_for_logs(
        self,
        loop: asyncio.AbstractEventLoop,
        caplog: Any,
        logger_name: str,
        num_logs: int = 1,
        max_wait: float = 6.0,
    ) -> List[logging.LogRecord]:
        start = loop.time()

        while True:
            logs = [r for r in caplog.records if r.name == logger_name]
            if len(logs) >= num_logs:
                return logs
            if loop.time() - start > max_wait:
                raise RuntimeError('Max wait time exhausted')
            await asyncio.sleep(0.05)

    async def _cancel_task(self, task) -> None:
        task.cancel()
        try:
            await task
        except asyncio.CancelledError:
            pass

    @pytest.mark.asyncio
    async def test_periodic_check_works(
        self, loop, checksum_dict, dummy_logger, tmp_path, caplog
    ):
        caplog.set_level(logging.DEBUG, logger=dummy_logger.logger.name)

        checksums = tmp_path / 'checksums.txt'
        checksums.write_text(dict_to_checksums(checksum_dict), 'utf-8')

        task = loop.create_task(
            run_periodic_checks(checksums, sleep=0.1, logger=dummy_logger)
        )

        try:
            logs = await self._wait_for_logs(loop, caplog, dummy_logger.logger.name, 2)
        finally:
            await self._cancel_task(task)

        assert_that(
            logs,
            only_contains(
                has_properties(
                    levelno=logging.DEBUG,
                    message=contains_string('Integrity check passed'),
                )
            )
        )

        assert_that(
            fim_check.get(),
            contains_inanyorder(
                ('fim_check_failed_summ', 0.0),
                ('fim_check_passed_summ', len(checksum_dict) * 2.0)
            )
        )

    @pytest.mark.asyncio
    async def test_periodic_check_fails(
        self, loop, checksum_dict, dummy_dir, dummy_logger, tmp_path, caplog
    ):
        hacked: Path = next(f for f in dummy_dir.iterdir() if f.is_file())
        hacked.write_bytes(hacked.read_bytes() + b'\x00')

        checksums = tmp_path / 'checksums.txt'
        checksums.write_text(dict_to_checksums(checksum_dict), 'utf-8')

        task = loop.create_task(
            run_periodic_checks(checksums, sleep=0.1, logger=dummy_logger)
        )

        try:
            logs = await self._wait_for_logs(loop, caplog, dummy_logger.logger.name, 2)
        finally:
            await self._cancel_task(task)

        assert_that(
            logs,
            contains(
                has_properties(
                    levelno=logging.WARNING,
                    message=contains_string(f'Checksum mismatch for {str(hacked)}'),
                ),
                has_properties(
                    levelno=logging.CRITICAL,
                    message=contains_string('INTEGRITY CHECK FAILED'),
                )
            )
        )

        assert_that(
            fim_check.get(),
            contains_inanyorder(
                contains(
                    'fim_check_failed_summ', greater_than(0)
                ),
                contains(
                    'fim_check_passed_summ',
                    greater_than_or_equal_to(len(checksum_dict) - 1.0),
                ),
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('body', ['', 'bad', 'foo=bar=baz'])
    async def test_periodic_check_unable_to_read_file(
        self, loop, checksum_dict, dummy_dir, dummy_logger, tmp_path, caplog, body
    ):
        checksums = tmp_path / 'checksums.txt'
        checksums.write_text(body, 'utf-8')

        task = loop.create_task(
            run_periodic_checks(checksums, sleep=0.1, logger=dummy_logger)
        )

        try:
            logs = await self._wait_for_logs(loop, caplog, dummy_logger.logger.name)
        finally:
            await self._cancel_task(task)

        assert_that(
            logs,
            only_contains(
                has_properties(
                    levelno=logging.ERROR,
                    message=f'Unable to read checksums from file: {str(checksums)}'
                )
            )
        )

        assert_that(
            fim_check.get(),
            contains(
                contains(
                    'fim_check_failed_summ', greater_than(0)
                )
            )
        )
