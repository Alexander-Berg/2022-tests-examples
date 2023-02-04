import json
from mock import patch, Mock

import pytest

from staff.oebs.controllers.datasources import OrganizationDatasource
from staff.oebs.models import Organization


json_data = '''
{
    "TaxUnits": [
        {
            "code": 12345,
            "taxUnitNameRus": "Yandex 1",
            "taxUnitNameEng": "Yandex 1 en",
            "countryCode": "RU",
            "staffUsage": "N",
            "taxUnitCategory": "300",
            "startDate": "2019-01-01",
            "endDate": null
        },
        {
            "code": 54321,
            "taxUnitNameRus": "Yandex 2",
            "taxUnitNameEng": "Yandex 2 en",
            "countryCode": "RU",
            "staffUsage": "Y",
            "taxUnitCategory": "300",
            "startDate": "2020-11-01",
            "endDate": null
        }
    ]
}
'''


@pytest.mark.django_db
@patch('staff.oebs.controllers.datasources.oebs_datasource.OEBSDatasource._save_to_s3')
def test_organization_sync(_save_mock, build_updater):
    datasource = OrganizationDatasource(Organization.oebs_type, Organization.method, Mock())
    datasource._data = json.loads(json_data)

    updater = build_updater(model=Organization, datasource=datasource)
    updater.run_sync()

    assert Organization.objects.count() == 2

    organization = Organization.objects.get(org_id=12345)
    assert organization.name_ru == 'Yandex 1'
    assert organization.name_en == 'Yandex 1 en'
    assert organization.country_code == 'RU'
    assert organization.staff_usage == 'N'

    organization = Organization.objects.get(org_id=54321)
    assert organization.name_ru == 'Yandex 2'
    assert organization.name_en == 'Yandex 2 en'
    assert organization.country_code == 'RU'
    assert organization.staff_usage == 'Y'


@pytest.mark.django_db
@patch('staff.oebs.controllers.datasources.oebs_datasource.OEBSDatasource._save_to_s3')
def test_organization_sync_update(_save_mock, build_updater):
    update_json_data = '''
    {"TaxUnits": [
            {
                "code": 12345,
                "taxUnitNameRus": "Yandex 1 ru",
                "taxUnitNameEng": "Yandex 1",
                "countryCode": "RU",
                "staffUsage": "Y",
                "taxUnitCategory": "300",
                "startDate": "2019-01-01",
                "endDate": null
            },
            {
                "code": 11111,
                "taxUnitNameRus": "Yandex 3 ru",
                "taxUnitNameEng": "Yandex 3",
                "countryCode": "RU",
                "staffUsage": "Y",
                "taxUnitCategory": "300",
                "startDate": "2020-11-01",
                "endDate": null
            }
        ]
    }'''

    datasource = OrganizationDatasource(Organization.oebs_type, Organization.method, Mock())
    datasource._data = json.loads(update_json_data)

    Organization.objects.create(org_id=12345, staff_usage='N')
    Organization.objects.create(org_id=54321, staff_usage='N')

    updater = build_updater(model=Organization, datasource=datasource)
    updater.run_sync()
    assert Organization.objects.count() == 2

    organization = Organization.objects.get(org_id=12345)
    assert organization.name_ru == 'Yandex 1 ru'
    assert organization.name_en == 'Yandex 1'
    assert organization.country_code == 'RU'
    assert organization.staff_usage == 'Y'

    organization = Organization.objects.get(org_id=11111)
    assert organization.name_ru == 'Yandex 3 ru'
    assert organization.name_en == 'Yandex 3'
    assert organization.country_code == 'RU'
    assert organization.staff_usage == 'Y'
