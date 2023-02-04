import pytest

from billing.library.python.calculator.models.product import ProductModel
from billing.library.python.calculator.schemas.product import MDHProductRowSchema

PRODUCT_ID = 506508


@pytest.fixture(scope="module")
def product_model() -> ProductModel:
    currency = {
        "status": 6,
        "status_alias": "published",
        "attrs": {
            "name": "Russian Ruble",
            "num_code": 643,
            "alpha_code": "RUB",
            "minor_unit": 2,
        },
        "master_uid": "38b206d6-6cc8-403a-8933-7de9b8ce7a52",
        "record_uid": "38b206d6-6cc8-403a-8933-7de9b8ce7a52",
        "version": 1,
        "version_master": 1,
    }
    region = {
        "attrs": {
            "iso_code": 643,
            "region_id": 225,
            "region_name": "Россия",
            "region_name_en": "Russia",
        },
        "status": 6,
        "status_alias": "published",
        "master_uid": "84009ba2-0144-4e8e-a29b-0cdcd51f1c3a",
        "record_uid": "84009ba2-0144-4e8e-a29b-0cdcd51f1c3a",
        "version": 1,
        "version_master": 1,
    }
    data = {
        "attrs": {
            "id": PRODUCT_ID,
            "name": "Доступ к службам индивидуальной технической поддержки",
            "common": False,
            "firm_id": "aa4bf616-de27-40b8-8ac3-4d33abfc97d1",
            "unit_id": "4bbde53b-d2af-4b4e-b4f1-d8b2ed26e78c",
            "activ_dt": "2015-09-04T00:00:00",
            "comments": None,
            "fullname": "Доступ к службам индивидуальной технической поддержки",
            "engine_id": "2e2c5b47-456e-400e-a523-b571cf6af2d6",
            "adv_kind_id": None,
            "englishname": None,
            "service_code": None,
            "show_in_shop": True,
            "only_test_env": False,
            "media_discount": "88d9541c-8310-4581-b1ab-484453191533",
            "commission_type": "88d9541c-8310-4581-b1ab-484453191533",
            "main_product_id": None,
            "manual_discount": False,
            "activity_type_id": "c99935a6-55ab-44ad-9441-d7211cac15a9",
            "product_group_id": "cce9361d-8c62-4280-bfc8-922ad6042cc0",
            "reference_price_iso_currency": "38b206d6-6cc8-403a-8933-7de9b8ce7a52",
        },
        "master_uid": "20472ebc-ff9c-46eb-a512-158940cd41e0",
        "record_uid": "20472ebc-ff9c-46eb-a512-158940cd41e0",
        "status": 6,
        "status_alias": "published",
        "version": 1,
        "version_master": 1,
        "version_composite": 1,
        "resync": False,
        "foreign": {
            "reference_price_iso_currency": currency,
            "unit_id": {
                "attrs": {
                    "id": 796,
                    "name": "шт",
                    "precision": 0,
                    "type_rate": 1,
                    "englishname": "p.",
                    "iso_currency": None,
                    "product_type_id": "3407fdad-5912-4e94-86bd-7393c7285bfa",
                },
                "master_uid": "4bbde53b-d2af-4b4e-b4f1-d8b2ed26e78c",
                "record_uid": "4bbde53b-d2af-4b4e-b4f1-d8b2ed26e78c",
                "status": 6,
                "status_alias": "published",
                "version": 1,
                "version_master": 1,
            },
        },
        "nested": {
            "nom_price": [
                {
                    "attrs": {
                        "dt": "2015-09-04T00:00:00",
                        "id": 25011,
                        "1244": "4a16cb9d-4b63-419d-858c-9e303706a251",
                        "price": "0.01",
                        "internal": False,
                        "product_id": "20472ebc-ff9c-46eb-a512-158940cd41e0",
                        "iso_currency": "38b206d6-6cc8-403a-8933-7de9b8ce7a52",
                        "only_test_env": False,
                        "tax_policy_pct_id": None,
                    },
                    "master_uid": "33464467-078a-4fa9-97e9-de33c9bf8dcd",
                    "record_uid": "33464467-078a-4fa9-97e9-de33c9bf8dcd",
                    "status": 6,
                    "status_alias": "published",
                    "version": 1,
                    "version_master": 1,
                    "foreign": {"iso_currency": currency},
                }
            ],
            "nom_tax": [
                {
                    "attrs": {
                        "dt": "2015-12-28T00:00:00",
                        "id": 27307,
                        "test904": None,
                        "product_id": "20472ebc-ff9c-46eb-a512-158940cd41e0",
                        "iso_currency": "38b206d6-6cc8-403a-8933-7de9b8ce7a52",
                        "only_test_env": False,
                        "tax_policy_id": "15d6bb2b-fb0b-4cab-8dd4-4b528e994b12",
                    },
                    "master_uid": "bb75295c-a924-4f8e-a36f-e19ceab7a27a",
                    "record_uid": "bb75295c-a924-4f8e-a36f-e19ceab7a27a",
                    "status": 6,
                    "status_alias": "published",
                    "version": 1,
                    "version_master": 1,
                    "foreign": {
                        "iso_currency": currency,
                        "tax_policy_id": {
                            "attrs": {
                                "id": 10,
                                "name": "Нерезидент России, НДС облагается",
                                "resident": False,
                                "region_id": "84009ba2-0144-4e8e-a29b-0cdcd51f1c3a",
                                "default_tax": False,
                            },
                            "status": 6,
                            "status_alias": "published",
                            "master_uid": "15d6bb2b-fb0b-4cab-8dd4-4b528e994b12",
                            "record_uid": "15d6bb2b-fb0b-4cab-8dd4-4b528e994b12",
                            "version": 1,
                            "version_master": 1,
                            "foreign": {"region_id": region},
                        },
                    },
                },
                {
                    "attrs": {
                        "dt": "2015-09-04T00:00:00",
                        "id": 19380,
                        "test904": None,
                        "product_id": "20472ebc-ff9c-46eb-a512-158940cd41e0",
                        "iso_currency": "38b206d6-6cc8-403a-8933-7de9b8ce7a52",
                        "only_test_env": False,
                        "tax_policy_id": "93772f35-aca3-4a5e-bebc-742c5eb4499f",
                    },
                    "status": 6,
                    "status_alias": "published",
                    "master_uid": "1d946f57-209f-4119-b0b0-a308ff867e19",
                    "record_uid": "1d946f57-209f-4119-b0b0-a308ff867e19",
                    "version": 1,
                    "version_master": 1,
                    "foreign": {
                        "iso_currency": currency,
                        "tax_policy_id": {
                            "attrs": {
                                "id": 1,
                                "name": "Стандартный НДС",
                                "resident": True,
                                "region_id": "84009ba2-0144-4e8e-a29b-0cdcd51f1c3a",
                                "default_tax": True,
                            },
                            "status": 6,
                            "status_alias": "published",
                            "master_uid": "93772f35-aca3-4a5e-bebc-742c5eb4499f",
                            "record_uid": "93772f35-aca3-4a5e-bebc-742c5eb4499f",
                            "version": 1,
                            "version_master": 1,
                            "foreign": {"region_id": region},
                        }
                    },
                },
            ],
        },
    }
    schema = MDHProductRowSchema()
    loaded, _ = schema.load(
        {"id": PRODUCT_ID, "master_uid": data["master_uid"], "version": data["version"], "obj": data}
    )
    return ProductModel(**loaded["obj"])  # type: ignore
