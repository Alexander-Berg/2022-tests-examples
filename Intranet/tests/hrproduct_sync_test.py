from datetime import date

import pytest
from mock import Mock, patch

from staff.lib.utils.date import parse_date

from staff.oebs.controllers.datasources import HRProductDatasource
from staff.oebs.models import HRProduct


json_data = {
    'products': [
        {
            'productID': 100004521,
            'productName': 'Accounting, planning and control',
            'endDate': None,
            'serviceCode': 'FI104',
            'serviceName': 'Учет',
            'serviceABC': '1742',
        },
        {
            'productID': 100000512,
            'productName': 'ADFOX',
            'endDate': '2018-11-14',
            'serviceCode': 'AD101',
            'serviceName': 'Adfox',
            'serviceABC': '737',
        },
        {
            'productID': 100000511,
            'productName': 'AppMetrica',
            'endDate': '2018-11-14',
            'serviceCode': 'DP101',
            'serviceName': 'Мобильная метрика (сервис)',
            'serviceABC': None,
        },
        {
            'productID': 100000102,
            'productName': 'Billing and Payment',
            'endDate': None,
            'serviceCode': 'IN104',
            'serviceName': 'Биллинг',
            'serviceABC': '1044',
        },
    ],
}


@pytest.mark.django_db
@patch('staff.oebs.controllers.datasources.oebs_datasource.OEBSDatasource._save_to_s3')
def test_hrproduct_sync(_save_mock, build_updater):
    datasource = HRProductDatasource(HRProduct.oebs_type, HRProduct.method, Mock())
    datasource._data = json_data
    updater = build_updater(model=HRProduct, datasource=datasource)
    updater.run_sync()
    assert HRProduct.objects.count() == 4

    for raw_data in json_data['products']:
        product = HRProduct.objects.get(product_id=raw_data['productID'])
        assert product.end_date == parse_date(raw_data['endDate'])
        assert product.product_name == raw_data['productName']
        assert product.service_code == raw_data['serviceCode']
        assert product.service_name == raw_data['serviceName']
        assert product.service_abc == raw_data['serviceABC']


@pytest.mark.django_db
@patch('staff.oebs.controllers.datasources.oebs_datasource.OEBSDatasource._save_to_s3')
def test_hrproduct_update(_save_mock, build_updater):
    HRProduct.objects.create(
        product_id=100000102,
        end_date=None,
        product_name='Accounting, planning and control',
        service_code='FI104',
        service_name='Учет',
        service_abc='1742',
    )

    HRProduct.objects.create(
        product_id=100004521,
        end_date=None,
        product_name='Billing and Payment',
        service_code='IN104',
        service_name='Биллинг',
        service_abc='1044',
    )

    json_data = {
        'products': [
            {
                'productID': 100004521,
                'productName': 'New product name',
                'endDate': '2021-06-18',
                'serviceCode': 'IN109',
                'serviceName': 'Сквизинг',
                'serviceABC': '1045',
            },
        ],
    }

    datasource = HRProductDatasource(HRProduct.oebs_type, HRProduct.method, Mock())
    datasource._data = json_data

    updater = build_updater(model=HRProduct, datasource=datasource)
    updater.run_sync()
    assert HRProduct.objects.count() == 1

    product = HRProduct.objects.get(product_id=100004521)
    assert product.end_date == date(year=2021, month=6, day=18)
    assert product.product_name == 'New product name'
    assert product.service_code == 'IN109'
    assert product.service_name == 'Сквизинг'
    assert product.service_abc == '1045'
