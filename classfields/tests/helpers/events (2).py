from datetime import datetime
from typing import Any, Dict, List

from smartagent_realty_converter.models.event import (
    HttpEvent,
    ObjectEvent,
    ObjectEventDetails,
    ObjectEventMessage,
    ObjectEventMetadata,
)
from tests.helpers.random_data import random_lower_string


def build_object_event_message(bucket_id: str, object_id: str) -> ObjectEventMessage:
    return ObjectEventMessage(
        event_metadata=ObjectEventMetadata(
            event_id=random_lower_string(),
            event_type=random_lower_string(),
            created_at=datetime.now(),
            tracing_context={},
            cloud_id=random_lower_string(),
            folder_id=random_lower_string(),
        ),
        details=ObjectEventDetails(bucket_id=bucket_id, object_id=object_id),
    )


def build_object_event(messages: List[ObjectEventMessage]) -> ObjectEvent:
    return ObjectEvent(messages=messages)


def build_http_event(query: Dict[str, Any], body: str = "") -> HttpEvent:
    return HttpEvent(
        http_method="GET",
        headers={},
        multi_value_headers={},
        query_string_parameters=query,
        multi_value_query_string_parameters={},
        request_context={},
        body=body,
        is_base64_encoded=False,
    )
