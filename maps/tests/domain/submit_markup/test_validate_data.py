import asyncio
from datetime import timedelta
from unittest.mock import Mock

import pytest
from smb.common.testing_utils import dt

from maps_adv.common.helpers import AsyncContextManagerMock
from maps_adv.geosmb.harmonist.server.lib.enums import (
    ColumnType,
    PipelineStep,
    StepStatus,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture(autouse=True)
def fetch_creation_log(dm):
    dm.fetch_clients_creation_log.coro.return_value = {
        "parsed_input": [
            [
                "Василий",
                "Кеков",
                "+7 (800) 200-06-00",
                "пёс какой-то",
                "kekov@ya.ru",
                "do not import this1",
            ],
            [
                "Пётр",
                "Винигретов",
                "8202 063 00 22",
                "не пёс",
                "vinigretov",
                "do not import this2",
            ],
            [
                "Иван",
                "Чебуреков",
                "434 58-23",
                "",
                "cheburekov@ya.ru",
                "do not import this3",
            ],
        ]
    }


async def test_saves_validation_result_in_db(domain, dm):
    await domain.submit_markup(
        session_id="f8d049c53e04",
        biz_id=123,
        markup={
            "ignore_first_line": False,
            "column_type_map": [
                {"column_type": ColumnType.FIRST_NAME, "column_number": 0},
                {"column_type": ColumnType.LAST_NAME, "column_number": 1},
                {"column_type": ColumnType.PHONE, "column_number": 2},
                {"column_type": ColumnType.COMMENT, "column_number": 3},
                {"column_type": ColumnType.EMAIL, "column_number": 4},
                {"column_type": ColumnType.DO_NOT_IMPORT, "column_number": 5},
            ],
        },
    )
    await asyncio.sleep(0.1)

    dm.submit_validated_clients.assert_called_with(
        session_id="f8d049c53e04",
        biz_id=123,
        validation_step_status=StepStatus.IN_PROGRESS,
        valid_clients=[
            {
                "first_name": "Василий",
                "last_name": "Кеков",
                "phone": 78002000600,
                "comment": "пёс какой-то",
                "email": "kekov@ya.ru",
            },
            {
                "first_name": "Иван",
                "last_name": "Чебуреков",
                "phone": 4345823,
                "comment": None,
                "email": "cheburekov@ya.ru",
            },
        ],
        invalid_clients=[
            {
                "row": "Пётр;Винигретов;8202 063 00 22;не пёс;vinigretov;do not import this2",  # noqa
                "reason": {"email": ["Некорректный адрес электронной почты."]},
            }
        ],
    )


async def test_locks_creation_log(mocker, domain, lock_manager):
    mocker.patch.object(
        lock_manager,
        "try_lock_creation_entry",
        Mock(return_value=AsyncContextManagerMock()),
    )

    await domain.submit_markup(
        session_id="f8d049c53e04",
        biz_id=123,
        markup={
            "ignore_first_line": False,
            "column_type_map": [
                {"column_type": ColumnType.FIRST_NAME, "column_number": 0},
                {"column_type": ColumnType.LAST_NAME, "column_number": 1},
                {"column_type": ColumnType.PHONE, "column_number": 2},
                {"column_type": ColumnType.COMMENT, "column_number": 3},
                {"column_type": ColumnType.EMAIL, "column_number": 4},
                {"column_type": ColumnType.DO_NOT_IMPORT, "column_number": 5},
            ],
        },
    )
    await asyncio.sleep(0.1)

    lock_manager.try_lock_creation_entry.assert_called_with("f8d049c53e04")


async def test_ignores_first_line_if_ignore_first_line_is_true(domain, dm):
    await domain.submit_markup(
        session_id="f8d049c53e04",
        biz_id=123,
        markup={
            "ignore_first_line": True,
            "column_type_map": [
                {"column_type": ColumnType.FIRST_NAME, "column_number": 0},
                {"column_type": ColumnType.EMAIL, "column_number": 4},
            ],
        },
    )
    await asyncio.sleep(0.1)

    dm.submit_validated_clients.assert_called_with(
        session_id="f8d049c53e04",
        biz_id=123,
        validation_step_status=StepStatus.IN_PROGRESS,
        valid_clients=[
            {"first_name": "Иван", "email": "cheburekov@ya.ru"},
        ],
        invalid_clients=[
            {
                "row": "Пётр;Винигретов;8202 063 00 22;не пёс;vinigretov;do not import this2",  # noqa
                "reason": {
                    "email": ["Некорректный адрес электронной почты."],
                    "contacts": ["Нужен как минимум один контакт: почта или телефон."],
                },
            }
        ],
    )


async def test_ignores_do_not_import_columns(domain, dm):
    await domain.submit_markup(
        session_id="f8d049c53e04",
        biz_id=123,
        markup={
            "ignore_first_line": False,
            "column_type_map": [
                {"column_type": ColumnType.FIRST_NAME, "column_number": 0},
                {"column_type": ColumnType.DO_NOT_IMPORT, "column_number": 1},
                {"column_type": ColumnType.PHONE, "column_number": 2},
                {"column_type": ColumnType.DO_NOT_IMPORT, "column_number": 3},
                {"column_type": ColumnType.DO_NOT_IMPORT, "column_number": 4},
                {"column_type": ColumnType.DO_NOT_IMPORT, "column_number": 5},
            ],
        },
    )
    await asyncio.sleep(0.1)

    dm.submit_validated_clients.assert_called_with(
        session_id="f8d049c53e04",
        biz_id=123,
        validation_step_status=StepStatus.FINISHED,
        valid_clients=[
            {"phone": 78002000600, "first_name": "Василий"},
            {"phone": 82020630022, "first_name": "Пётр"},
            {"phone": 4345823, "first_name": "Иван"},
        ],
        invalid_clients=[],
    )


async def test_marks_process_finished_on_submitting_validation_if_has_no_invalid_clients(  # noqa
    domain, dm
):
    await domain.submit_markup(
        session_id="f8d049c53e04",
        biz_id=123,
        markup={
            "ignore_first_line": False,
            "column_type_map": [
                {"column_type": ColumnType.FIRST_NAME, "column_number": 0},
                {"column_type": ColumnType.PHONE, "column_number": 2},
            ],
        },
    )
    await asyncio.sleep(0.1)

    call_args = dm.submit_validated_clients.call_args
    assert call_args[1]["invalid_clients"] == []
    assert call_args[1]["validation_step_status"] == StepStatus.FINISHED


async def test_marks_process_in_progress_on_submitting_validation_if_has_invalid_clients(  # noqa
    domain, dm
):
    await domain.submit_markup(
        session_id="f8d049c53e04",
        biz_id=123,
        markup={
            "ignore_first_line": False,
            "column_type_map": [
                {"column_type": ColumnType.FIRST_NAME, "column_number": 0},
                {"column_type": ColumnType.EMAIL, "column_number": 2},
            ],
        },
    )
    await asyncio.sleep(0.1)

    call_args = dm.submit_validated_clients.call_args
    assert call_args[1]["invalid_clients"] != []
    assert call_args[1]["validation_step_status"] == StepStatus.IN_PROGRESS


@pytest.mark.freeze_time(dt("2020-01-05 18:00:00"))
async def test_uploads_error_file_to_mds(domain, dm, mds):
    await domain.submit_markup(
        session_id="f8d049c53e04",
        biz_id=123,
        markup={
            "ignore_first_line": True,
            "column_type_map": [
                {"column_type": ColumnType.FIRST_NAME, "column_number": 0},
                {"column_type": ColumnType.EMAIL, "column_number": 2},
            ],
        },
    )
    await asyncio.sleep(0.1)

    mds.upload_file.assert_called_with(
        file_content=expected_file_content,
        file_name="1578247200.csv",
        expire=timedelta(days=1),
    )


async def test_saves_file_download_link(domain, dm):
    await domain.submit_markup(
        session_id="f8d049c53e04",
        biz_id=123,
        markup={
            "ignore_first_line": True,
            "column_type_map": [
                {"column_type": ColumnType.FIRST_NAME, "column_number": 0},
                {"column_type": ColumnType.EMAIL, "column_number": 2},
            ],
        },
    )
    await asyncio.sleep(0.1)

    dm.submit_error_file.assert_called_with(
        session_id="f8d049c53e04",
        biz_id=123,
        validation_errors_file_link="http://mds-inner-read.server/get-business/603/errors_file.csv?disposition=1", # noqa
    )


async def test_does_not_call_mds_if_no_invalid_clients(domain, dm, mds):
    await domain.submit_markup(
        session_id="f8d049c53e04",
        biz_id=123,
        markup={
            "ignore_first_line": False,
            "column_type_map": [
                {"column_type": ColumnType.FIRST_NAME, "column_number": 0},
                {"column_type": ColumnType.PHONE, "column_number": 2},
            ],
        },
    )
    await asyncio.sleep(0.1)

    mds.upload_file.assert_not_called()


async def test_logs_mds_exceptions(domain, dm, mds, caplog):
    mds.upload_file.coro.side_effect = Exception("boom!")

    await domain.submit_markup(
        session_id="f8d049c53e04",
        biz_id=123,
        markup={
            "ignore_first_line": False,
            "column_type_map": [
                {"column_type": ColumnType.FIRST_NAME, "column_number": 0},
                {"column_type": ColumnType.EMAIL, "column_number": 4},
            ],
        },
    )
    await asyncio.sleep(0.1)

    errors = [r for r in caplog.records if r.levelname == "ERROR"]
    assert {error.message for error in errors} == {
        "Failed to upload errors file for session_id f8d049c53e04"
    }


async def test_marks_creation_log_failed_if_failed_to_upload_document(domain, dm, mds):
    mds.upload_file.coro.side_effect = Exception("boom!")

    await domain.submit_markup(
        session_id="f8d049c53e04",
        biz_id=123,
        markup={
            "ignore_first_line": False,
            "column_type_map": [
                {"column_type": ColumnType.FIRST_NAME, "column_number": 0},
                {"column_type": ColumnType.EMAIL, "column_number": 4},
            ],
        },
    )
    await asyncio.sleep(0.1)

    dm.update_log_status.assert_called_with(
        session_id="f8d049c53e04",
        biz_id=123,
        pipeline_step=PipelineStep.VALIDATING_DATA,
        status=StepStatus.FAILED,
        failed_reason="Failed to create errors file.",
    )


expected_file_content = (
    b"\xd0\x9f\xd1\x91\xd1\x82\xd1\x80;\xd0\x92\xd0\xb8\xd0\xbd\xd0"
    b"\xb8\xd0\xb3\xd1\x80\xd0\xb5\xd1\x82\xd0\xbe\xd0\xb2;8202 063 00 22"
    b";\xd0\xbd\xd0\xb5 \xd0\xbf\xd1\x91\xd1\x81;vinigretov;do not import this2;"
    b"{'email': ['\xd0\x9d\xd0\xb5\xd0\xba\xd0\xbe\xd1\x80\xd1\x80\xd0\xb5\xd0\xba"
    b"\xd1\x82\xd0\xbd\xd1\x8b\xd0\xb9 \xd0\xb0\xd0\xb4\xd1\x80\xd0\xb5\xd1\x81 "
    b"\xd1\x8d\xd0\xbb\xd0\xb5\xd0\xba\xd1\x82\xd1\x80\xd0\xbe\xd0\xbd"
    b"\xd0\xbd\xd0\xbe\xd0\xb9 \xd0\xbf\xd0\xbe\xd1\x87\xd1\x82\xd1\x8b.'], 'conta"
    b"cts': ['\xd0\x9d\xd1\x83\xd0\xb6\xd0\xb5\xd0\xbd \xd0\xba\xd0\xb0\xd0"
    b"\xba \xd0\xbc\xd0\xb8\xd0\xbd\xd0\xb8\xd0\xbc\xd1\x83\xd0\xbc \xd0\xbe\xd0"
    b"\xb4\xd0\xb8\xd0\xbd \xd0\xba\xd0\xbe\xd0\xbd\xd1\x82\xd0\xb0"
    b"\xd0\xba\xd1\x82: \xd0\xbf\xd0\xbe\xd1\x87\xd1\x82\xd0\xb0 \xd0\xb8\xd0"
    b"\xbb\xd0\xb8 \xd1\x82\xd0\xb5\xd0\xbb\xd0\xb5\xd1\x84\xd0\xbe\xd0\xbd.'"
    b"]}\n\xd0\x98\xd0\xb2\xd0\xb0\xd0\xbd;\xd0\xa7\xd0\xb5\xd0\xb1\xd1\x83"
    b"\xd1\x80\xd0\xb5\xd0\xba\xd0\xbe\xd0\xb2;434 58-23;;cheburekov@ya.ru;do no"
    b"t import this3;{'email': ['\xd0\x9d\xd0\xb5\xd0\xba\xd0\xbe\xd1"
    b"\x80\xd1\x80\xd0\xb5\xd0\xba\xd1\x82\xd0\xbd\xd1\x8b\xd0\xb9 "
    b"\xd0\xb0\xd0\xb4\xd1\x80\xd0\xb5\xd1\x81 \xd1\x8d\xd0\xbb\xd0"
    b"\xb5\xd0\xba\xd1\x82\xd1\x80\xd0\xbe\xd0\xbd\xd0\xbd\xd0\xbe\xd0"
    b"\xb9 \xd0\xbf\xd0\xbe\xd1\x87\xd1\x82\xd1\x8b.'], 'contacts': ['\xd0"
    b"\x9d\xd1\x83\xd0\xb6\xd0\xb5\xd0\xbd \xd0\xba\xd0\xb0\xd0\xba \xd0\xbc\xd0"
    b"\xb8\xd0\xbd\xd0\xb8\xd0\xbc\xd1\x83\xd0\xbc \xd0\xbe\xd0\xb4"
    b"\xd0\xb8\xd0\xbd \xd0\xba\xd0\xbe\xd0\xbd\xd1\x82\xd0\xb0\xd0\xba\xd1\x82:"
    b" \xd0\xbf\xd0\xbe\xd1\x87\xd1\x82\xd0\xb0 \xd0\xb8\xd0\xbb\xd0\xb8 \xd1"
    b"\x82\xd0\xb5\xd0\xbb\xd0\xb5\xd1\x84\xd0\xbe\xd0\xbd.']}"
)
