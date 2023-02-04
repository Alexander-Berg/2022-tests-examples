from datetime import datetime

from flask import Flask, g
from ya_courier_backend.util.errors import (
    SchemaValidatorError,
    ResponseSchemaViolation,
    CgiSchemaValidatorError)
from ya_courier_backend.util.validate_schema import schema_validation, get_cgi_parameters
import pytest
from ya_courier_backend.models.user import UserRole
from ya_courier_backend.util.oauth import UserAccount
from collections import namedtuple
from flask import request


def test_order_property_type(schema_validator):
    with pytest.raises(SchemaValidatorError, match="Json schema validation failed: OrderGet: '5' is not of type 'integer'"):
        schema_validator.validate_object("OrderGet", {"route_id": "5"})


def test_order_additional_fields(schema_validator):
    with pytest.raises(SchemaValidatorError, match=r"Json schema validation failed: OrderGet: Additional properties are not allowed \('dummy_property' was unexpected\)"):
        schema_validator.validate_object("OrderGet", {"route_id": 5, "dummy_property": "dummy_value"})


def test_order_modify_additional_fields(schema_validator):
    with pytest.raises(SchemaValidatorError, match=r"Json schema validation failed: OrderModify: Additional properties are not allowed \('history' was unexpected\)"):
        schema_validator.validate_object("OrderModify", {"number": "ABC123", "history": []})


@pytest.fixture(scope="session")
def schema_validation_mock_app(schema_validator):
    app = Flask(__name__)
    app.schema_validator = schema_validator
    with app.app_context():
        g.user = UserAccount(
            id=None,
            login="schema_validation_mock_app_test_user",
            uid=None,
            company_ids=[1],
            is_super=False,
            confirmed_at=datetime.now(),
            role=UserRole.admin.value
        )
        yield app


def test_response_validation(schema_validation_mock_app):
    app = schema_validation_mock_app

    @schema_validation(response_schema_name=None)
    def dummy_handler():
        return {"key": "value"}

    with app.test_request_context(json={}):
        dummy_handler()

    @schema_validation(response_schema_name="OrderGet", throw_on_response=False)
    def malfunctioning_handler_1():
        # note: here and below returned data violates the expected response schema
        return {"route_id": "5", "dummy_property": "dummy_value"}

    with app.test_request_context(json={}):
        malfunctioning_handler_1()

    @schema_validation(response_schema_name="OrderGet", throw_on_response=True)
    def malfunctioning_handler_2():
        return {"route_id": "5", "dummy_property": "dummy_value"}

    with pytest.raises(ResponseSchemaViolation,
                       match='Server is unable to form a proper response due to an internal error'):
        with app.test_request_context(json={}):
            malfunctioning_handler_2()


def test_request_schema(schema_validation_mock_app):
    app = schema_validation_mock_app

    @schema_validation()
    def dummy_handler():
        return 42

    with app.test_request_context(json={"key": "value"}):
        dummy_handler()

    @schema_validation(request_schema_name="OrderGet", throw_on_request=True)
    def some_handler():
        return 42

    with pytest.raises(SchemaValidatorError,
                       match=r"Json schema validation failed: OrderGet: Additional properties are not allowed \('dummy_property' was unexpected\)"):
        with app.test_request_context(json={"route_id": 5, "dummy_property": "dummy_value"}):
            some_handler()


def test_order_get_cgi_parameters(schema_validator):
    path = "/api/v1/companies/{company_id}/orders"
    request_method = "get"

    cgi = schema_validator.get_cgi_parameters(
        path, request_method, {"route_id": "1", "number": "order_number", "page": "2"})
    assert cgi["route_id"] == 1
    assert cgi["number"] == "order_number"
    assert cgi["page"] == 2

    cgi = schema_validator.get_cgi_parameters(
        path, request_method, {"route_id": "1", "number": "order_number"})
    assert cgi["route_id"] == 1
    assert cgi["number"] == "order_number"
    assert cgi["page"] == 1

    with pytest.raises(
            CgiSchemaValidatorError,
            match=r"CGI parameters validation failed: get /api/v1/companies/{company_id}/orders: {'page': \['Must be greater than or equal to 1\.'\]}"):
        schema_validator.get_cgi_parameters(
            path, request_method, {"route_id": "1", "number": "order_number", "page": "0"})

    with pytest.raises(
            CgiSchemaValidatorError,
            match=r"CGI parameters validation failed: get /api/v1/companies/{company_id}/orders: {'route_id': \[\"Not a valid integer 'non_valid_id'.\"\]}"):
        schema_validator.get_cgi_parameters(
            path, request_method, {"route_id": "non_valid_id", "number": "order_number", "page": "1"})


def test_required_cgi(schema_validator):
    path = "/api/v1/companies/{company_id}/order-notifications"
    request_method = "get"
    with pytest.raises(
            CgiSchemaValidatorError,
            match=r"CGI parameters validation failed: get /api/v1/companies/{company_id}/order-notifications: {'to': \['Missing data for required field.'\]}"):
        schema_validator.get_cgi_parameters(
            path, request_method, {"from": "2020-09-06T10:15:00+03:00", "page": "1"})


def test_get_cgi_parameters(schema_validation_mock_app):
    app = schema_validation_mock_app
    UrlRuleMock = namedtuple('UrlRule', ['rule', 'endpoint'])
    with app.test_request_context("/?route_id=123"):
        request.url_rule = UrlRuleMock(rule="/api/v1/companies/{company_id}/orders", endpoint="")
        cgi = get_cgi_parameters(request)
        assert len(cgi) == 2
        assert cgi["page"] == 1
        assert cgi["route_id"] == 123

    with app.test_request_context("/?incorrect=123"):
        request.url_rule = UrlRuleMock(rule="/api/v1/companies/{company_id}/orders", endpoint="")
        pytest.raises(CgiSchemaValidatorError, get_cgi_parameters, request)


@pytest.mark.parametrize(
    "path, method, request_body, expected_error",
    [
        ("/api/v1/create-company", "post", {"utm_source": 5}, r"{'utm_source': \['Not a valid string\.'\]}"),
        ("/api/v1/companies/{company_id}/order-notifications", "get", {"to": "invalid_timestamp", "from": "2020-09-06T10:15:00+03:00", "page": "1"},
            r"{'to': \[\"Not a valid datetime 'invalid_timestamp'\.\"\]}"),
        ("/api/v1/couriers/{courier_id}/routes", "get", {"date": "2020-01-32"}, r"{'date': \[\"Not a valid date '2020-01-32'\.\"\]}"),
        ("/api/v1/couriers/{courier_id}/routes/{route_id}/predict-eta", "get", {"find-optimal": "non-boolean", "lat": "30", "lon": "50", "time": "2020-09-06T10:15:00+03:00"},
            r"{'find\-optimal': \[\"Not a valid boolean 'non\-boolean'\.\"\]}"),
        ("/api/v1/couriers/{courier_id}/routes/{route_id}/predict-eta", "get", {"find-optimal": True, "lat": "non-float", "lon": "50", "time": "2020-09-06T10:15:00+03:00"},
            r"{'lat': \[\"Not a valid float 'non\-float'\.\"\]}"),
        ("/api/v1/tracking/{track_id}/track", "get", {"after": "non-integer"}, r"{'after': \[\"Not a valid integer 'non\-integer'\.\"\]}")
    ])
def test_cgi_parameters(schema_validator, path, method, request_body, expected_error):
    with pytest.raises(
            CgiSchemaValidatorError,
            match=f"CGI parameters validation failed: {method} {path}: {expected_error}"):
        schema_validator.get_cgi_parameters(path, method, request_body)


def test_cgi_minimum_id(schema_validator):
    path = "/api/v1/companies/{company_id}/courier-position"
    request_method = "get"

    for depot_id in [-1, 0]:
        with pytest.raises(
                CgiSchemaValidatorError,
                match=r"CGI parameters validation failed: get /api/v1/companies/{company_id}/courier-position: {'depot_id': \['Must be greater than or equal to 1.'\]}"):
            schema_validator.get_cgi_parameters(
                path, request_method, {"depot_id": str(depot_id)})

    cgi = schema_validator.get_cgi_parameters(
        path, request_method, {"depot_id": "1"})
    assert len(cgi) == 2
    assert cgi["depot_id"] == 1


def test_cgi_enum(schema_validator):
    path = "/api/v1/companies/{company_id}/mvrp_task"
    request_method = "post"

    with pytest.raises(
            CgiSchemaValidatorError,
            match=r"CGI parameters validation failed: post /api/v1/companies/{company_id}/mvrp_task: {'initial-status': \['Must be one of: new, confirmed.'\]}"):
        schema_validator.get_cgi_parameters(
            path, request_method, {"task_id": "1234", "initial-status": "finished"})

    cgi = schema_validator.get_cgi_parameters(
        path, request_method, {"task_id": "1234", "initial-status": "confirmed"})
    assert len(cgi) == 4  # 'order-update' is also accounted as they have default values
    assert cgi["initial-status"] == "confirmed"


def test_cgi_lat_lon(schema_validator):
    path = "/api/v1/couriers/{courier_id}/routes/{route_id}/predict-eta"
    request_method = "get"

    for item in [
            {"lat": -91, "lon": 0, "message": r"'lat': \['Must be greater than or equal to -90.'\]"},
            {"lat": 91, "lon": 0, "message": r"'lat': \['Must be less than or equal to 90.'\]"},
            {"lat": 0, "lon": -181, "message": r"'lon': \['Must be greater than or equal to -180.'\]"},
            {"lat": 0, "lon": 181, "message": r"'lon': \['Must be less than or equal to 180.'\]"},
            ]:
        with pytest.raises(
                CgiSchemaValidatorError,
                match=item["message"]):
            schema_validator.get_cgi_parameters(
                path, request_method, {"time": "2020-09-06T10:15:00+03:00", "lat": item["lat"], "lon": item["lon"]})

    cgi = schema_validator.get_cgi_parameters(
        path, request_method, {"time": "2020-09-06T10:15:00+03:00", "lat": 90, "lon": 180})
    assert len(cgi) == 4
    assert cgi["lat"] == 90
    assert cgi["lon"] == 180
