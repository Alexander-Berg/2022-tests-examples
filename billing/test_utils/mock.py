from datetime import datetime
from typing import Any
from unittest.mock import Mock

from billing.library.python.calculator.actions import Action
from billing.library.python.calculator.models.event import EventModel
from billing.library.python.calculator.models.transaction import TransactionBatchModel


def action_mock(
    event: EventModel,
    references: Any,
    on_dt: datetime = None,
    dry_run: bool = True,
    migrated: bool = True,
    **kwargs,
) -> Action:
    return Mock(
        run=lambda: TransactionBatchModel(event=event, client_transactions=[]),
        event=event,
        references=references,
        on_dt=on_dt,
        dry_run=dry_run,
        migrated=migrated,
        **kwargs,
    )
