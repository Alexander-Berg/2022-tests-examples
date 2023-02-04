import pytest


@pytest.fixture()
def some_firm_export():
    return [
        {
            "export_type": "OEBS",
            "oebs_org_id": 123,
        }
    ]
