import pytest
import requests

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    add_company_number,
    create_route_env,
    create_tmp_company,
    delete_company_number,
    get_company_number,
    get_company_numbers,
    get_courier_quality,
    get_order,
    get_order_details,
    patch_order_by_order_id,
)


@pytest.mark.parametrize('is_super_user', [True, False])
def test_company_number(system_env_with_db, is_super_user):
    auth = system_env_with_db.auth_header_super if is_super_user else None

    with create_tmp_company(system_env_with_db, "Test company number.") as contractor_id:
        number = f"Number for company {contractor_id}"
        add_company_number(system_env_with_db, contractor_id, number, auth=auth)

        response = get_company_number(system_env_with_db, contractor_id, auth=auth)
        assert response == {"id": contractor_id, "number": number}, response

        response = get_company_numbers(system_env_with_db, auth=auth)
        assert {"id": contractor_id, "number": number} in response, response

        delete_company_number(system_env_with_db, contractor_id, auth=auth)

        response = get_company_number(system_env_with_db, contractor_id, auth=auth)
        assert response == {}, response

        response = get_company_numbers(system_env_with_db, auth=auth)
        assert {"id": contractor_id, "number": number} not in response, response


@skip_if_remote
def test_modify_existing_number(system_env_with_db):
    with create_tmp_company(system_env_with_db, "Test company number.") as contractor_id:
        number = f"Number for company {contractor_id}"
        add_company_number(system_env_with_db, contractor_id, number)

        response = get_company_number(system_env_with_db, contractor_id)
        assert response == {"id": contractor_id, "number": number}

        new_number = f"New number for company {contractor_id}"
        add_company_number(system_env_with_db, contractor_id, new_number)

        response = get_company_number(system_env_with_db, contractor_id)
        assert response == {"id": contractor_id, "number": new_number}


@skip_if_remote
def test_process_non_existing_number(system_env_with_db):
    with create_tmp_company(system_env_with_db, "Test company number.") as contractor_id:
        response = get_company_number(system_env_with_db, contractor_id)
        assert response == {}

        response = get_company_numbers(system_env_with_db)
        assert response == []

        delete_company_number(system_env_with_db, contractor_id)


@skip_if_remote
def test_same_numbers_for_several_companies(system_env_with_db):
    with create_tmp_company(system_env_with_db, "Test company 1.") as contractor_id:
        with create_tmp_company(system_env_with_db, "Test company 2.") as second_contractor_id:
            number = "Number for company."
            add_company_number(system_env_with_db, contractor_id, number)
            status_code = add_company_number(system_env_with_db, second_contractor_id, number, strict=False)
            assert status_code == requests.codes.unprocessable


@skip_if_remote
def test_company_number_sharing(system_env_with_db):
    contractor_name = "Test company name"
    with create_tmp_company(system_env_with_db, contractor_name) as contractor_id:
        with create_route_env(
            system_env_with_db,
            "company_number_sharing",
            order_locations=[{"lat": 55.733827, "lon": 37.588722}]
        ) as route_env:
            contractor_number = f"Number for company {contractor_id}"

            order_id = route_env["orders"][0]["id"]
            order_number = route_env["orders"][0]["number"]
            depot_id = route_env["depot"]["id"]

            patch_data={"shared_with_company_ids": [contractor_id]}
            _, response = patch_order_by_order_id(system_env_with_db, order_id, patch_data)
            assert response["shared_with_company_ids"] == [contractor_id]
            assert response["shared_with_companies"] == []

            response = get_courier_quality(system_env_with_db, route_env["route"]["date"], depot_id=depot_id)
            assert response[0]["order_shared_with_companies"] == [{"id": contractor_id, "name": contractor_name, "number": None}]

            response = get_order_details(system_env_with_db, order_number)
            assert response["shared_with_companies"] == [{"id": contractor_id, "name": contractor_name, "number": None}]

            add_company_number(system_env_with_db, contractor_id, contractor_number)

            response = get_order(system_env_with_db, order_id)
            assert response["shared_with_company_ids"] == [contractor_id]
            assert response["shared_with_companies"] == [{"id": contractor_id, "number": contractor_number}]

            response = get_courier_quality(system_env_with_db, route_env["route"]["date"], depot_id=depot_id)
            assert response[0]["order_shared_with_companies"] == [{"id": contractor_id, "name": contractor_name, "number": contractor_number}]

            response = get_order_details(system_env_with_db, order_number)
            assert response["shared_with_companies"] == [{"id": contractor_id, "name": contractor_name, "number": contractor_number}]


@skip_if_remote
def test_shared_with_company_numbers(system_env_with_db):
    contractor_name = "Test company name 1"
    second_contractor_name = "Test company name 2"
    with create_tmp_company(system_env_with_db, contractor_name) as contractor_id:
        with create_tmp_company(system_env_with_db, second_contractor_name) as second_contractor_id:
            with create_route_env(
                system_env_with_db,
                "shared_with_company_numbers",
                order_locations=[{"lat": 55.733827, "lon": 37.588722}]
            ) as route_env:
                contractor_number = f"Number for company {contractor_id}"
                second_contractor_number = f"Number for company {second_contractor_id}"

                order_id = route_env["orders"][0]["id"]
                order_number = route_env["orders"][0]["number"]
                depot_id = route_env["depot"]["id"]

                add_company_number(system_env_with_db, contractor_id, contractor_number)

                patch_data={"shared_with_company_numbers": [contractor_number, second_contractor_number]}
                status_code, response = patch_order_by_order_id(system_env_with_db, order_id, patch_data, strict=False)
                assert status_code == requests.codes.ok
                assert response["shared_with_companies"] == [{"id": contractor_id, "number": contractor_number}]

                add_company_number(system_env_with_db, second_contractor_id, second_contractor_number)

                patch_data={"shared_with_company_numbers": [contractor_number, second_contractor_number]}
                status_code, response = patch_order_by_order_id(system_env_with_db, order_id, patch_data)

                response = get_order(system_env_with_db, order_id)
                assert set(response["shared_with_company_ids"]) == {contractor_id, second_contractor_id}
                assert sorted(response["shared_with_companies"], key=lambda elem: elem["id"]) == sorted([
                    {"id": contractor_id, "number": contractor_number},
                    {"id": second_contractor_id, "number": second_contractor_number}
                ], key=lambda elem: elem["id"])

                response = get_courier_quality(system_env_with_db, route_env["route"]["date"], depot_id=depot_id)
                assert sorted(response[0]["order_shared_with_companies"], key=lambda elem: elem["id"]) == sorted([
                    {"id": contractor_id, "name": contractor_name, "number": contractor_number},
                    {"id": second_contractor_id, "name": second_contractor_name, "number": second_contractor_number},
                ], key=lambda elem: elem["id"])

                response = get_order_details(system_env_with_db, order_number)
                assert sorted(response["shared_with_companies"], key=lambda elem: elem["id"]) == sorted([
                    {"id": contractor_id, "name": contractor_name, "number": contractor_number},
                    {"id": second_contractor_id, "name": second_contractor_name, "number": second_contractor_number},
                ], key=lambda elem: elem["id"])
