import uuid
from datetime import datetime, timezone
from typing import Iterable, List, Optional

import pytest
from motor.motor_asyncio import AsyncIOMotorCollection, AsyncIOMotorDatabase

from maps_adv.geosmb.harmonist.server.lib.enums import (
    InputDataType,
    PipelineStep,
    StepStatus,
)

_default_parsed_input = (
    ("Lorem", "ipsum dolor", "sit amet"),
    ("consectetur", "adipiscing", " elit.", "Nullam"),
    ("accumsan", "orci sit", "amet commodo" "consectetur"),
)


class Factory:
    _coll: AsyncIOMotorCollection

    def __init__(self, motor_db: AsyncIOMotorDatabase):
        self._coll = motor_db.clients_creation_logs

    async def find_creation_log(self, session_id: str) -> dict:
        return await self._coll.find_one({"_id": session_id})

    async def create_log(
        self,
        *,
        biz_id: int = 123,
        input_data: str = "Literally anything",
        input_data_type: InputDataType = InputDataType.TEXT,
        parsed_input: Iterable[Iterable[str]] = _default_parsed_input,
        markup: Optional[dict] = None,
        valid_clients: Optional[List[dict]] = None,
        invalid_clients: Optional[List[dict]] = None,
        validation_errors_file_link: Optional[str] = None,
        import_result: Optional[dict] = None,
        created_at: Optional[datetime] = None,
    ) -> str:
        now = datetime.now(tz=timezone.utc)
        document = {
            "_id": str(uuid.uuid4()),
            "biz_id": biz_id,
            "input_data": input_data,
            "input_data_type": input_data_type,
            "parsed_input": parsed_input,
            "created_at": created_at or now,
            "log_history": [
                {
                    "step": PipelineStep.PARSING_DATA,
                    "status": StepStatus.FINISHED,
                    "created_at": created_at or now,
                },
            ],
        }

        log = await self._coll.insert_one(document)
        _id = str(log.inserted_id)

        if markup:
            await self.add_markup(session_id=_id, markup=markup)

        if valid_clients is not None:
            await self.add_validation_result(
                session_id=_id,
                valid_clients=valid_clients,
                invalid_clients=invalid_clients,
                validation_errors_file_link=validation_errors_file_link,
            )

        if import_result:
            await self.add_import_result(session_id=_id, import_result=import_result)

        return _id

    async def add_markup(self, session_id: str, markup: dict) -> None:
        await self._coll.update_one(
            {"_id": session_id},
            {
                "$set": {"markup": markup},
                "$push": {
                    "log_history": {
                        "step": PipelineStep.VALIDATING_DATA,
                        "status": StepStatus.IN_PROGRESS,
                        "created_at": datetime.now(tz=timezone.utc),
                    },
                },
            },
        )

    async def add_validation_result(
        self,
        session_id: str,
        valid_clients: List[dict],
        invalid_clients: Optional[List[dict]] = None,
        validation_errors_file_link: Optional[str] = None,
    ) -> None:
        validation_result = {
            "valid_clients": valid_clients,
            "invalid_clients": invalid_clients if invalid_clients else [],
        }
        if validation_errors_file_link:
            validation_result[
                "validation_errors_file_link"
            ] = validation_errors_file_link

        await self._coll.update_one(
            {"_id": session_id},
            {
                "$set": validation_result,
                "$push": {
                    "log_history": {
                        "step": PipelineStep.VALIDATING_DATA,
                        "status": StepStatus.FINISHED,
                        "created_at": datetime.now(tz=timezone.utc),
                    },
                },
            },
        )

    async def add_import_result(self, session_id: str, import_result: dict) -> None:
        await self._coll.update_one(
            {"_id": session_id},
            {
                "$set": {"import_result": import_result},
                "$push": {
                    "log_history": {
                        "step": PipelineStep.IMPORTING_CLIENTS,
                        "status": StepStatus.FINISHED,
                        "created_at": datetime.now(tz=timezone.utc),
                    },
                },
            },
        )

    async def add_history_record(
        self,
        *,
        session_id: str,
        step: PipelineStep,
        status: StepStatus,
        failed_reason: Optional[str] = None,
    ) -> None:
        log_record = {
            "step": step,
            "status": status,
            "created_at": datetime.now(tz=timezone.utc),
        }
        if failed_reason:
            log_record["failed_reason"] = failed_reason

        await self._coll.update_one(
            {"_id": session_id},
            {"$push": {"log_history": log_record}},
        )


@pytest.fixture
def factory(motor_db):
    return Factory(motor_db)
