"""Tests MongoEngine models."""

import pytest
from bson.int64 import Int64
from mongoengine import IntField, StringField, EmbeddedDocumentField
from mongoengine.base.fields import BaseField

from infra.walle.server.tests.lib.util import mock_task, monkeypatch_audit_log, AUDIT_LOG_ID, mock_uuid_for_inv
from sepelib.mongo.util import get_registered_models
from walle.hosts import Host, HostState
from walle.models import Document, DocumentPostprocessor
from walle.util.api import get_object_result, get_query_result
from walle.util.mongo import MongoDocument


def _assert_equal(host, fields, expected):
    api_obj = host.to_api_obj(fields)
    if "uuid" in api_obj:
        api_obj["uuid"] = str(api_obj["uuid"])
    assert api_obj == expected


@pytest.mark.parametrize("use_pymongo", [False, True])
def test_to_api_obj_mongoengine(monkeypatch, walle_test, use_pymongo):
    monkeypatch_audit_log(monkeypatch)
    host = walle_test.mock_host(
        {
            "inv": 1,
            "name": "host-name",
            "state": HostState.ASSIGNED,
            "task": mock_task(),
            "status_reason": "reason-mock",
        }
    )

    if use_pymongo:
        host_obj = MongoDocument.for_model(Host)
        host = host_obj(Host.get_collection().find_one({"inv": 1}))

    _assert_equal(
        host,
        None,
        {
            "uuid": mock_uuid_for_inv(1),
            "inv": Int64(1),
            "name": "host-name",
            "task": {"status": "status-mock"},
            "state": HostState.ASSIGNED,
            "config": "config-mock",
            "status": "ready",
            "status_author": walle_test.api_issuer,
            "status_reason": "reason-mock",
        },
    )

    _assert_equal(host, ["uuid"], {"uuid": mock_uuid_for_inv(1)})
    _assert_equal(host, ["name"], {"name": "host-name"})

    assert host.health is None and "health.reasons" in Host.api_fields
    _assert_equal(host, ["name", "health"], {"name": "host-name"})

    _assert_equal(host, ["name.invalid_field"], {})
    _assert_equal(host, ["inv", "name.invalid_field"], {"inv": Int64(1)})

    _assert_equal(
        host,
        ["inv", "task"],
        {"inv": Int64(1), "task": {"owner": "wall-e", "status": "status-mock", "audit_log_id": AUDIT_LOG_ID}},
    )

    _assert_equal(
        host, ["inv", "task.invalid", "task.type", "task.owner"], {"inv": Int64(1), "task": {"owner": "wall-e"}}
    )

    _assert_equal(host, ["inv", "task.invalid", "task.type", "task.invalid_doc.invalid"], {"inv": Int64(1)})


def test_postprocessing(database):
    class Doc(Document):
        id = IntField(primary_key=True, required=True)
        value = StringField()
        private_value = StringField()
        api_fields = ["id", "value"]

    class Postprocessor(DocumentPostprocessor):
        def __init__(self, set_field=False):
            super().__init__(["private_value"], ["postprocessor_field"])
            self._processed = False
            self._set_field = set_field

        def process(self, objects, requested_fields):
            self._processed = True
            extra_fields = {"postprocessor_field": "calculated-value"} if self._set_field else {}

            res = []
            for obj in objects:
                assert obj.private_value == "some-private-value"
                res.append(obj.to_api_obj(requested_fields, extra_fields=extra_fields))

            return res

    def check(fields, host_obj, postprocessor=None):
        id_field = Doc.id
        query, query_args = {id_field.db_field: 1}, {"fields": fields}

        assert get_object_result(Doc, query, query_args, postprocessor=postprocessor) == host_obj

        query_result = get_query_result(
            Doc, query, cursor_field=id_field, query_args=query_args, postprocessor=postprocessor
        )
        host_obj_copy = host_obj.copy()
        host_obj_copy.update({id_field.name: 1})
        assert query_result == {"result": [host_obj_copy], "total": 1}

    doc = Doc(id=1, value="some-value", private_value="some-private-value")
    doc.save(validate=False)

    check(["value"], {"value": doc.value})
    check(["id", "value", "private_value"], {"id": doc.id, "value": doc.value})

    postprocessor = Postprocessor()
    check(["value"], {"value": doc.value}, postprocessor)
    assert not postprocessor._processed

    postprocessor = Postprocessor()
    check(["value", "postprocessor_field", "private_value"], {"value": doc.value}, postprocessor)
    assert postprocessor._processed

    postprocessor = Postprocessor(set_field=True)
    check(
        ["value", "postprocessor_field"], {"value": doc.value, "postprocessor_field": "calculated-value"}, postprocessor
    )
    assert postprocessor._processed


def get_models_with_api_fields():
    return [m for m in get_registered_models() if getattr(m, "api_fields", None)]


def get_model_simple_fields():
    for model in get_models_with_api_fields():
        for field_name in model.api_fields:
            if "." not in field_name:
                yield model, field_name


def get_model_embedded_docs():
    for model in get_models_with_api_fields():
        for field_name in model.api_fields:
            if "." in field_name:
                yield model, field_name


@pytest.mark.parametrize("model, field_name", get_model_simple_fields())
def test_simple_api_field_exists(model, field_name):
    field_obj = getattr(model, field_name, None)
    assert isinstance(field_obj, BaseField)


@pytest.mark.parametrize("model, field_name", get_model_embedded_docs())
def test_embedded_doc_exists(model, field_name):
    field_path = field_name.split(".")
    cur_component = model
    for embedded_doc_name in field_path[:-1]:
        cur_component = getattr(cur_component, embedded_doc_name, None)
        assert isinstance(cur_component, EmbeddedDocumentField)
        cur_component = cur_component.document_type
    # check field
    field_obj = getattr(cur_component, field_path[-1], None)
    assert isinstance(field_obj, BaseField)
