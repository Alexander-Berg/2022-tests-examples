# coding: utf-8
import random
import string
from contextlib import contextmanager

import datetime
import pytest
from mock.mock import call
from sqlalchemy import inspect

from balance import mapper
from balance.application import getApplication
from cluster_tools import mdh_import
from tests import object_builder


def generate_unique_id(session, obj_mapper, field='id'):
    ob = object_builder.ObjectBuilder()
    ob._class = obj_mapper
    return ob.generate_unique_id(session, field)


def generate_mdh_id():
    return ''.join([random.choice(string.lowercase) for _ in range(36)])


@pytest.fixture()
def mock_importer(mock_logbroker_app_base):
    @contextmanager
    def mock_mdh_importer(*args, **kwargs):
        with mock_logbroker_app_base("cluster_tools.mdh_import", "MdhImport", *args, **kwargs) as importer:
            # replacing the MdhImport.create_object method to account for the fact
            # that dev database does not have insert triggers that create PKs

            # this is a static method
            orig_create_object = importer.create_object

            # so the replacement does not need the argument for 'self'
            def create_object_add_id_for_dev_db(session, model, attrs):
                pk = inspect(model).primary_key
                if len(pk) == 1 and pk[0].name not in attrs:
                    attrs[pk[0].name] = generate_unique_id(session, model, pk[0].name)
                return orig_create_object(session, model, attrs)

            importer.create_object = create_object_add_id_for_dev_db
            yield importer

    return mock_mdh_importer


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_discount_type(meta_session, update_mode, mock_importer):
    """
    Здесь и дальше в параметризациях update_mode
    None - insert новой записи
    with_pk - update записи в таблице, из mdh приезжает запись, содержащая primary_key, в БД нет mdh_id
    without_pk - update записи в таблице, из mdh приезжает запись, не содержащая primary_key
    """
    mdh_id = generate_mdh_id()
    created_discount_type_id = None
    if update_mode:
        created_discount_type_id = generate_unique_id(meta_session, mapper.DiscountType)
        created_discount_type = mapper.DiscountType(
            id=created_discount_type_id,
            name='created_discount_type',
            tag=9999,
            mdh_id=None if update_mode == 'with_pk' else mdh_id
        )
        meta_session.add(created_discount_type)
        meta_session.flush()
    messages = [
        dict(
            master_uid=mdh_id,
            attrs=dict(
                id=created_discount_type_id if update_mode == 'with_pk' else None,
                name='imported_discount_type',
                tag=123
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_discount_type': messages}) as importer:
        importer.main()

    imported_discount_type_list = meta_session.query(mapper.DiscountType).filter_by(mdh_id=mdh_id).all()
    assert len(imported_discount_type_list) == 1
    imported_discount_type = imported_discount_type_list[0]
    assert imported_discount_type.name == 'imported_discount_type'
    assert imported_discount_type.tag == 123
    if update_mode:
        assert imported_discount_type.id == created_discount_type_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_activity_type(session, update_mode, mock_importer):
    parent_activity_type_mdh_id = generate_mdh_id()
    parent_activity_type_id = generate_unique_id(session, mdh_import.NonExportableActivityType)
    parent_activity_type = mdh_import.NonExportableActivityType(
        id=parent_activity_type_id,
        name='parent_activity_type',
        mdh_id=parent_activity_type_mdh_id,
        hidden=0
    )
    session.add(parent_activity_type)
    session.flush()

    mdh_id = generate_mdh_id()
    created_activity_type_id = None
    if update_mode:
        created_activity_type_id = generate_unique_id(session, mdh_import.NonExportableActivityType)
        created_activity_type = mdh_import.NonExportableActivityType(
            id=created_activity_type_id,
            name='created_activity_type',
            mdh_id=None if update_mode == 'with_pk' else mdh_id,
            hidden=0
        )
        session.add(created_activity_type)
        session.flush()
    messages = [
        dict(
            master_uid=mdh_id,
            attrs=dict(
                id=created_activity_type_id if update_mode == 'with_pk' else None,
                name='imported_activity_type',
                parent_id=parent_activity_type_mdh_id
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_activity_type': messages}) as importer:
        importer.main()

    imported_activity_type_list = session.query(mdh_import.NonExportableActivityType).filter_by(mdh_id=mdh_id).all()
    assert len(imported_activity_type_list) == 1
    imported_activity_type = imported_activity_type_list[0]
    assert imported_activity_type.name == 'imported_activity_type'
    assert imported_activity_type.parent_id == parent_activity_type_id
    if update_mode:
        assert imported_activity_type.id == created_activity_type_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_product_group(session, meta_session, update_mode, mock_importer):
    discount_type_mdh_id = generate_mdh_id()
    discount_type_id = generate_unique_id(meta_session, mapper.DiscountType)
    discount_type = mapper.DiscountType(
        id=discount_type_id,
        name='created_discount_type',
        tag=9999,
        mdh_id=discount_type_mdh_id
    )
    meta_session.add(discount_type)
    meta_session.flush()

    parent_product_group_mdh_id = generate_mdh_id()
    parent_product_group_id = generate_unique_id(session, mdh_import.NonExportableProductGroup)
    parent_product_group = mdh_import.NonExportableProductGroup(
        id=parent_product_group_id,
        name='parent_product_group',
        mdh_id=parent_product_group_mdh_id
    )
    session.add(parent_product_group)
    session.flush()

    product_group_mdh_id = generate_mdh_id()
    created_product_group_id = None
    if update_mode:
        created_product_group_id = generate_unique_id(session, mdh_import.NonExportableProductGroup)
        created_product_group = mdh_import.NonExportableProductGroup(
            id=created_product_group_id,
            name='created_product_group',
            mdh_id=None if update_mode == 'with_pk' else product_group_mdh_id
        )
        session.add(created_product_group)
        session.flush()
    messages = [
        dict(
            master_uid=product_group_mdh_id,
            attrs=dict(
                id=created_product_group_id if update_mode == 'with_pk' else None,
                name='imported_product_group',
                parent_id=parent_product_group_mdh_id,
                discount_id=discount_type_mdh_id
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_product_group': messages}) as importer:
        importer.main()

    imported_product_group_list = session.query(
        mdh_import.NonExportableProductGroup).filter_by(mdh_id=product_group_mdh_id).all()
    assert len(imported_product_group_list) == 1
    imported_product_group = imported_product_group_list[0]
    assert imported_product_group.name == 'imported_product_group'
    assert imported_product_group.parent_id == parent_product_group_id
    assert imported_product_group.discount_id == discount_type_id
    if update_mode:
        assert imported_product_group.id == created_product_group_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_product_type(session, update_mode, mock_importer):
    created_product_type_id = None
    mdh_id = generate_mdh_id()
    if update_mode:
        created_product_type_id = generate_unique_id(session, mapper.ProductType)
        created_product_type = mapper.ProductType(
            id=created_product_type_id,
            cc='created_cc',
            name='created_name',
            mdh_id=None if update_mode == 'with_pk' else mdh_id
        )
        session.add(created_product_type)
        session.flush()
    messages = [
        dict(
            master_uid=mdh_id,
            attrs=dict(
                id=created_product_type_id if update_mode == 'with_pk' else None,
                cc='imported_cc',
                name='imported_name'
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_product_type': messages}) as importer:
        importer.main()

    imported_product_type_list = session.query(mapper.ProductType).filter_by(mdh_id=mdh_id).all()
    assert len(imported_product_type_list) == 1
    imported_product_type = imported_product_type_list[0]
    assert imported_product_type.cc == 'imported_cc'
    assert imported_product_type.name == 'imported_name'
    if update_mode:
        assert imported_product_type.id == created_product_type_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk'])
def test_nom_iso_currency(session, update_mode, mock_importer):
    mdh_id = generate_mdh_id()
    alpha_code = ''.join([random.choice(string.uppercase) for _ in range(10)])
    num_code = generate_unique_id(session, mapper.IsoCurrency, 'num_code')
    if update_mode:
        created_iso_currency = mapper.IsoCurrency(
            alpha_code=alpha_code,
            num_code=num_code,
            name='created_name',
            mdh_id=None if update_mode == 'with_pk' else mdh_id
        )
        session.add(created_iso_currency)
        session.flush()
    messages = [
        dict(
            master_uid=mdh_id,
            attrs=dict(
                alpha_code=alpha_code,
                num_code=num_code,
                name='imported_name'
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_iso_currency': messages}) as importer:
        importer.main()

    imported_iso_currency_list = session.query(mapper.IsoCurrency).filter_by(mdh_id=mdh_id).all()
    assert len(imported_iso_currency_list) == 1
    imported_iso_currency = imported_iso_currency_list[0]
    assert imported_iso_currency.alpha_code == alpha_code
    assert imported_iso_currency.num_code == num_code
    assert imported_iso_currency.name == 'imported_name'


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_product_unit(session, update_mode, mock_importer):
    product_type_mdh_id = generate_mdh_id()
    created_product_type_id = generate_unique_id(session, mapper.ProductType)
    created_product_type = mapper.ProductType(
        id=created_product_type_id,
        cc='created_cc',
        name='created_name',
        mdh_id=product_type_mdh_id
    )
    session.add(created_product_type)

    iso_currency_mdh_id = generate_mdh_id()
    alpha_code = ''.join([random.choice(string.uppercase) for _ in range(10)])
    num_code = generate_unique_id(session, mapper.IsoCurrency, 'num_code')
    iso_currency = mapper.IsoCurrency(
        alpha_code=alpha_code,
        num_code=num_code,
        name='created_name',
        mdh_id=iso_currency_mdh_id
    )
    session.add(iso_currency)
    session.flush()

    product_unit_mdh_id = generate_mdh_id()
    created_product_unit_id = None
    if update_mode:
        created_product_unit_id = generate_unique_id(session, mapper.ProductUnit)
        created_product_unit = mapper.ProductUnit(
            id=created_product_unit_id,
            name='created_product_unit',
            englishname='eng_created_product_unit',
            product_type_id=created_product_type.id,
            type_rate=333,
            precision=0,
            mdh_id=None if update_mode == 'with_pk' else product_unit_mdh_id
        )
        session.add(created_product_unit)
        session.flush()
    messages = [
        dict(
            master_uid=product_unit_mdh_id,
            attrs=dict(
                id=created_product_unit_id if update_mode == 'with_pk' else None,
                name='imported_product_unit',
                englishname='eng_imported_product_unit',
                product_type_id=product_type_mdh_id,
                iso_currency=iso_currency_mdh_id,
                type_rate=333,
                precision=6
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_product_unit': messages}) as importer:
        importer.main()

    imported_product_unit_list = session.query(mapper.ProductUnit).filter_by(mdh_id=product_unit_mdh_id).all()
    assert len(imported_product_unit_list) == 1
    imported_product_unit = imported_product_unit_list[0]
    assert imported_product_unit.name == 'imported_product_unit'
    assert imported_product_unit.englishname == 'eng_imported_product_unit'
    assert imported_product_unit.type_rate == 333
    assert imported_product_unit.product_type_id == created_product_type_id
    assert imported_product_unit.iso_currency == iso_currency.alpha_code
    assert imported_product_unit.precision == 6
    if update_mode:
        assert imported_product_unit.id == created_product_unit_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk'])
def test_nom_country(meta_session, update_mode, mock_importer):
    mdh_id = generate_mdh_id()
    region_id = generate_unique_id(meta_session, mapper.Country, 'region_id')
    if update_mode:
        created_country = mapper.Country(
            region_id=region_id,
            region_name='created_region_name',
            region_name_en='created_region_name_en',
            iso_code=123,
            mdh_id=None if update_mode == 'with_pk' else mdh_id
        )
        meta_session.add(created_country)
        meta_session.flush()
    messages = [
        dict(
            master_uid=mdh_id,
            attrs=dict(
                region_id=region_id,
                region_name='imported_region_name',
                region_name_en='imported_region_name_en',
                iso_code=321
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_country': messages}) as importer:
        importer.main()

    imported_country_list = meta_session.query(mapper.Country).filter_by(mdh_id=mdh_id).all()
    assert len(imported_country_list) == 1
    imported_country = imported_country_list[0]
    assert imported_country.region_id == region_id
    assert imported_country.region_name == 'imported_region_name'
    assert imported_country.region_name_en == 'imported_region_name_en'
    assert imported_country.iso_code == 321


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_tax_policy(session, meta_session, update_mode, mock_importer):
    country = meta_session.query(mapper.Country).filter(mapper.Country.mdh_id is not None).first()
    country_mdh_id = country.mdh_id
    region_id = country.region_id

    tax_policy_mdh_id = generate_mdh_id()
    created_tax_policy_id = None
    if update_mode:
        created_tax_policy_id = generate_unique_id(session, mapper.TaxPolicy)
        created_tax_policy = mapper.TaxPolicy(
            id=created_tax_policy_id,
            name='created_tax_policy',
            region_id=region_id,
            resident=1,
            hidden=0,
            mdh_id=None if update_mode == 'with_pk' else tax_policy_mdh_id
        )
        session.add(created_tax_policy)
        session.flush()
    messages = [
        dict(
            master_uid=tax_policy_mdh_id,
            attrs=dict(
                id=created_tax_policy_id if update_mode == 'with_pk' else None,
                name='imported_tax_policy',
                region_id=country_mdh_id,
                resident=0
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_tax_policy': messages}) as importer:
        importer.main()

    imported_tax_policy_list = session.query(mapper.TaxPolicy).filter_by(mdh_id=tax_policy_mdh_id).all()
    assert len(imported_tax_policy_list) == 1
    imported_tax_policy = imported_tax_policy_list[0]
    assert imported_tax_policy.name == 'imported_tax_policy'
    assert imported_tax_policy.region_id == region_id
    assert imported_tax_policy.resident == 0
    if update_mode:
        assert imported_tax_policy.id == created_tax_policy_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_tax_policy_pct(session, meta_session, update_mode, mock_importer):
    country = meta_session.query(mapper.Country).first()
    region_id = country.region_id

    tax_policy_mdh_id = generate_mdh_id()
    tax_policy_id = generate_unique_id(session, mapper.TaxPolicy)
    tax_policy = mapper.TaxPolicy(
        id=tax_policy_id,
        name='created_tax_policy',
        region_id=region_id,
        resident=1,
        hidden=0,
        mdh_id=tax_policy_mdh_id
    )
    session.add(tax_policy)
    session.flush()

    tax_policy_pct_mdh_id = generate_mdh_id()
    created_tax_policy_pct_id = None
    if update_mode:
        created_tax_policy_pct_id = generate_unique_id(session, mapper.TaxPolicyPct)
        created_tax_policy_pct = mapper.TaxPolicyPct(
            id=created_tax_policy_pct_id,
            tax_policy_id=tax_policy_id,
            mdh_id=None if update_mode == 'with_pk' else tax_policy_pct_mdh_id
        )
        session.add(created_tax_policy_pct)
        session.flush()
    messages = [
        dict(
            master_uid=tax_policy_pct_mdh_id,
            attrs=dict(
                id=created_tax_policy_pct_id if update_mode == 'with_pk' else None,
                tax_policy_id=tax_policy.mdh_id,
                nds_pct=20
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_tax_policy_pct': messages}) as importer:
        importer.main()

    imported_tax_policy_pct_list = session.query(mapper.TaxPolicyPct).filter_by(mdh_id=tax_policy_pct_mdh_id).all()
    assert len(imported_tax_policy_pct_list) == 1
    imported_tax_policy_pct = imported_tax_policy_pct_list[0]
    assert imported_tax_policy_pct.tax_policy_id == tax_policy_id
    assert imported_tax_policy_pct.nds_pct == 20
    if update_mode:
        assert imported_tax_policy_pct.id == created_tax_policy_pct_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_firm(session, meta_session, update_mode, mock_importer):
    country_mdh_id = generate_mdh_id()
    region_id = generate_unique_id(meta_session, mapper.Country, 'region_id')
    created_country = mapper.Country(
        region_id=region_id,
        region_name='created_region_name',
        region_name_en='created_region_name_en',
        iso_code=123,
        mdh_id=country_mdh_id
    )
    meta_session.add(created_country)
    meta_session.flush()

    iso_currency_mdh_id = generate_mdh_id()
    alpha_code = ''.join([random.choice(string.uppercase) for _ in range(10)])
    num_code = generate_unique_id(session, mapper.IsoCurrency, 'num_code')
    created_iso_currency = mapper.IsoCurrency(
        alpha_code=alpha_code,
        num_code=num_code,
        name='created_name',
        mdh_id=iso_currency_mdh_id
    )
    session.add(created_iso_currency)
    created_currency = mapper.Currency(
        char_code=alpha_code,
        iso_code=alpha_code,
        num_code=num_code,
        iso_num_code=num_code,
        is_internal=0,
        auto_rate=0,
        for_price=1,
    )
    session.add(created_currency)
    session.flush()

    firm_mdh_id = generate_mdh_id()
    created_firm_id = None
    if update_mode:
        created_firm_id = generate_unique_id(meta_session, mdh_import.NonExportableFirm)
        created_firm = mdh_import.NonExportableFirm(
            id=created_firm_id,
            default_iso_currency=alpha_code,
            email='created@mdh.import',
            phone='+70001112233',
            payment_invoice_email='created@mdh.import',
            mnclose_email='created@mdh.import',
            mdh_id=None if update_mode == 'with_pk' else firm_mdh_id
        )
        meta_session.add(created_firm)
        meta_session.flush()
    messages = [
        dict(
            master_uid=firm_mdh_id,
            attrs=dict(
                id=created_firm_id if update_mode == 'with_pk' else None,
                region_id=country_mdh_id,
                default_iso_currency=iso_currency_mdh_id,
                title='mdh_import_test_firm',
                email='imported@mdh.import',
                phone='+70001112233',
                payment_invoice_email='imported@mdh.import',
                mnclose_email='imported@mdh.import',
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_firm': messages}) as importer:
        importer.main()

    imported_firm_list = meta_session.query(mdh_import.NonExportableFirm).filter_by(mdh_id=firm_mdh_id).all()
    assert len(imported_firm_list) == 1
    imported_firm = imported_firm_list[0]
    assert imported_firm.region_id == region_id
    assert imported_firm.default_iso_currency == alpha_code
    assert imported_firm.title == 'mdh_import_test_firm'
    assert imported_firm.email == 'imported@mdh.import'
    assert imported_firm.phone == '+70001112233'
    assert imported_firm.payment_invoice_email == 'imported@mdh.import'
    assert imported_firm.mnclose_email == 'imported@mdh.import'
    if update_mode:
        assert imported_firm.id == created_firm_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_adv_kind(session, update_mode, mock_importer):
    mdh_id = generate_mdh_id()
    created_adv_kind_id = None
    if update_mode:
        created_adv_kind_id = generate_unique_id(session, mapper.AdvKind)
        created_adv_kind = mapper.AdvKind(
            id=created_adv_kind_id,
            name='created_adv_kind',
            mdh_id=None if update_mode == 'with_pk' else mdh_id
        )
        session.add(created_adv_kind)
        session.flush()
    messages = [
        dict(
            master_uid=mdh_id,
            attrs=dict(
                id=created_adv_kind_id if update_mode == 'with_pk' else None,
                name='imported_adv_kind'
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_adv_kind': messages}) as importer:
        importer.main()

    imported_adv_kind_list = session.query(mapper.AdvKind).filter_by(mdh_id=mdh_id).all()
    assert len(imported_adv_kind_list) == 1
    imported_adv_kind = imported_adv_kind_list[0]
    assert imported_adv_kind.name == 'imported_adv_kind'
    if update_mode:
        assert imported_adv_kind.id == created_adv_kind_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk'])
def test_nom_service_code(session, update_mode, mock_importer):
    mdh_id = generate_mdh_id()
    service_code = ''.join([random.choice(string.uppercase) for _ in range(10)])
    if update_mode:
        created_service_code = mapper.ServiceCode(
            service_code=service_code,
            descr='created_service_code',
            mdh_id=None if update_mode == 'with_pk' else mdh_id
        )
        session.add(created_service_code)
        session.flush()
    messages = [
        dict(
            master_uid=mdh_id,
            attrs=dict(
                service_code=service_code,
                descr='imported_service_code',
                additional_config='{"imported_additional_config": 1}'
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_service_code': messages}) as importer:
        importer.main()

    imported_service_code_list = session.query(mapper.ServiceCode).filter_by(mdh_id=mdh_id).all()
    assert len(imported_service_code_list) == 1
    imported_service_code = imported_service_code_list[0]
    assert imported_service_code.service_code == service_code
    assert imported_service_code.descr == 'imported_service_code'
    assert imported_service_code.additional_config == {"imported_additional_config": 1}


@pytest.mark.parametrize('code_map', [{}, {"IMP": "ALT"}])
@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_language(session, meta_session, code_map, update_mode, mock_importer):
    mdh_id = generate_mdh_id()
    created_language_id = None
    if update_mode:
        created_language_id = generate_unique_id(meta_session, mapper.Language)
        created_language = mapper.Language(
            id=created_language_id,
            code='CRT',
            lang='created_language',
            balance_code='BLN',
            mdh_id=None if update_mode == 'with_pk' else mdh_id
        )
        meta_session.add(created_language)
        meta_session.flush()
    messages = [
        dict(
            master_uid=mdh_id,
            attrs=dict(
                id=created_language_id,
                code='IMP',
                lang='imported_language',
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    session.config.set('LANGUAGE_CODE_MAP', code_map, column_name='value_json', can_create=True)
    session.flush()

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_language': messages}) as importer:
        importer.main()

    imported_language_list = meta_session.query(mapper.Language).filter_by(mdh_id=mdh_id).all()
    assert len(imported_language_list) == 1
    imported_language = imported_language_list[0]
    assert imported_language.code == code_map.get('IMP', 'IMP')
    assert imported_language.lang == 'imported_language'
    if update_mode:
        assert imported_language.id == created_language_id
        assert imported_language.balance_code == 'IMP'


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_markup(session, update_mode, mock_importer):
    mdh_id = generate_mdh_id()
    created_markup_id = None
    if update_mode:
        created_markup_id = generate_unique_id(session, mapper.Markup)
        created_markup = mapper.Markup(
            id=created_markup_id,
            code='created_markup',
            mdh_id=None if update_mode == 'with_pk' else mdh_id
        )
        session.add(created_markup)
        session.flush()
    messages = [
        dict(
            master_uid=mdh_id,
            attrs=dict(
                id=created_markup_id if update_mode == 'with_pk' else None,
                code='imported_markup',
                description='imported_markup_descr'
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_markup': messages}) as importer:
        importer.main()

    imported_markup_list = session.query(mapper.Markup).filter_by(mdh_id=mdh_id).all()
    assert len(imported_markup_list) == 1
    imported_markup = imported_markup_list[0]
    assert imported_markup.code == 'imported_markup'
    assert imported_markup.description == 'imported_markup_descr'
    if update_mode:
        assert imported_markup.id == created_markup_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_service_group(meta_session, update_mode, mock_importer):
    mdh_id = generate_mdh_id()
    created_service_group_id = None
    if update_mode:
        created_service_group_id = generate_unique_id(meta_session, mapper.ServiceGroup)
        created_service_group = mapper.ServiceGroup(
            id=created_service_group_id,
            name='created_service_group',
            mdh_id=None if update_mode == 'with_pk' else mdh_id
        )
        meta_session.add(created_service_group)
        meta_session.flush()
    messages = [
        dict(
            master_uid=mdh_id,
            attrs=dict(
                id=created_service_group_id if update_mode == 'with_pk' else None,
                name='imported_service_group'
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_service_group': messages}) as importer:
        importer.main()

    imported_service_group_list = meta_session.query(mapper.ServiceGroup).filter_by(mdh_id=mdh_id).all()
    assert len(imported_service_group_list) == 1
    imported_service_group = imported_service_group_list[0]
    assert imported_service_group.name == 'imported_service_group'
    if update_mode:
        assert imported_service_group.id == created_service_group_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_service(session, meta_session, update_mode, mock_importer):
    service_group_mdh_id = generate_mdh_id()
    service_group_id = generate_unique_id(meta_session, mapper.ServiceGroup)
    service_group = mapper.ServiceGroup(
        id=service_group_id,
        name='created_service_group',
        mdh_id=service_group_mdh_id
    )
    meta_session.add(service_group)
    meta_session.flush()

    service_mdh_id = generate_mdh_id()
    created_service_id = None
    if update_mode:
        created_service_id = generate_unique_id(meta_session, mapper.Service)
        created_service = mapper.Service(
            id=created_service_id,
            service_group_id=service_group_id,
            name='created_service',
            mdh_id=None if update_mode == 'with_pk' else service_mdh_id
        )
        meta_session.add(created_service)
        meta_session.flush()
    messages = [
        dict(
            master_uid=service_mdh_id,
            attrs=dict(
                id=created_service_id if update_mode == 'with_pk' else None,
                service_group_id=service_group_mdh_id,
                name='imported_service'
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_service': messages}) as importer:
        importer.main()

    imported_service_list = meta_session.query(mapper.Service).filter_by(mdh_id=service_mdh_id).all()
    assert len(imported_service_list) == 1
    imported_service = imported_service_list[0]
    assert imported_service.service_group_id == service_group_id
    assert imported_service.name == 'imported_service'
    if update_mode:
        assert imported_service.id == created_service_id

    else:
        for export_type in imported_service.export_ng_types:
            assert session.query(mapper.ExportNg).get((export_type, imported_service.id)), "MDH export didn't work"


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_balance_transfer_group(meta_session, update_mode, mock_importer):
    mdh_id = generate_mdh_id()
    created_balance_transfer_group_id = None
    if update_mode:
        created_balance_transfer_group_id = generate_unique_id(meta_session, mapper.BalanceTransferGroup)
        created_balance_transfer_group = mapper.BalanceTransferGroup(
            id=created_balance_transfer_group_id,
            code='created_balance_transfer_group',
            mdh_id=None if update_mode == 'with_pk' else mdh_id
        )
        meta_session.add(created_balance_transfer_group)
        meta_session.flush()
    messages = [
        dict(
            master_uid=mdh_id,
            attrs=dict(
                id=created_balance_transfer_group_id if update_mode == 'with_pk' else None,
                code='imported_balance_transfer_group',
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_balance_transfer_group': messages}) as importer:
        importer.main()

    balance_transfer_group_list = meta_session.query(mapper.BalanceTransferGroup).filter_by(mdh_id=mdh_id).all()
    assert len(balance_transfer_group_list) == 1
    balance_transfer_group = balance_transfer_group_list[0]
    assert balance_transfer_group.code == 'imported_balance_transfer_group'
    if update_mode:
        assert balance_transfer_group.id == created_balance_transfer_group_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_balance_service(meta_session, update_mode, mock_importer):
    service_group_mdh_id = generate_mdh_id()
    service_group_id = generate_unique_id(meta_session, mapper.ServiceGroup)
    service_group = mapper.ServiceGroup(
        id=service_group_id,
        name='created_service_group',
        mdh_id=service_group_mdh_id
    )
    meta_session.add(service_group)

    service_mdh_id = generate_mdh_id()
    service_id = generate_unique_id(meta_session, mapper.Service)
    service = mapper.Service(
        id=service_id,
        service_group_id=service_group_id,
        name='created_service',
        mdh_id=service_mdh_id
    )
    meta_session.add(service)

    balance_transfer_group_mdh_id = generate_mdh_id()
    balance_transfer_group_id = generate_unique_id(meta_session, mapper.BalanceTransferGroup)
    balance_transfer_group = mapper.BalanceTransferGroup(
        id=balance_transfer_group_id,
        code='created_balance_transfer_group',
        mdh_id=balance_transfer_group_mdh_id
    )
    meta_session.add(balance_transfer_group)
    meta_session.flush()

    mdh_id = generate_mdh_id()
    if update_mode:
        created_balance_service = mapper.BalanceService(
            id=service_id,
            mdh_id=None if update_mode == 'with_pk' else mdh_id
        )
        meta_session.add(created_balance_service)
        meta_session.flush()
    messages = [
        dict(
            master_uid=mdh_id,
            attrs=dict(
                service_uid=service_mdh_id,
                url_orders='imported_url_orders',
                client_only=True,
                transfer_group_id=balance_transfer_group_mdh_id,
                test_env=0
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_balance_service': messages}) as importer:
        importer.main()

    balance_service_list = meta_session.query(mapper.BalanceService).filter_by(mdh_id=mdh_id).all()
    assert len(balance_service_list) == 1
    balance_service = balance_service_list[0]
    assert balance_service.id == service_id
    assert balance_service.url_orders == 'imported_url_orders'
    assert balance_service.client_only == 1
    assert balance_service.transfer_group_id == balance_transfer_group_id


# @pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
# def test_nom_thirdparty_service(meta_session, session, update_mode, mock_importer):
#     service_group_mdh_id = generate_mdh_id()
#     service_group_id = generate_unique_id(meta_session, mapper.ServiceGroup)
#     service_group = mapper.ServiceGroup(
#         id=service_group_id,
#         name='created_service_group',
#         mdh_id=service_group_mdh_id
#     )
#     meta_session.add(service_group)
#
#     service_mdh_id = generate_mdh_id()
#     service_id = generate_unique_id(meta_session, mapper.Service)
#     service = mapper.Service(
#         id=service_id,
#         service_group_id=service_group_id,
#         name='created_service',
#         mdh_id=service_mdh_id
#     )
#     meta_session.add(service)
#     meta_session.flush()
#
#     mdh_id = generate_mdh_id()
#     if update_mode:
#         created_thirdparty_service = mapper.ThirdPartyService(
#             id=service_id,
#             enabled=0,
#             mdh_id=None if update_mode == 'with_pk' else mdh_id
#         )
#         session.add(created_thirdparty_service)
#         session.flush()
#     messages = [
#         dict(
#             master_uid=mdh_id,
#             attrs=dict(
#                 service_uid=service_mdh_id,
#                 enabled=1,
#                 product_mapping_config='{"imported_thirdparty_service": 1}'
#             ),
#             status=mdh_import.Status.PUBLISHED,
#             record_uid='record_uid'
#         ),
#     ]
#
#     with mock_importer(messages={'mdh/test/domains/nomenclature/nom_thirdparty_service': messages}) as importer:
#         importer.main()
#
#     thirdparty_service_list = session.query(mapper.ThirdPartyService).filter_by(mdh_id=mdh_id).all()
#     assert len(thirdparty_service_list) == 1
#     thirdparty_service = thirdparty_service_list[0]
#     assert thirdparty_service.id == service_id
#     assert thirdparty_service.enabled == 1
#     assert thirdparty_service.product_mapping_config == '{"imported_thirdparty_service": 1}'


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_product(meta_session, session, update_mode, mock_importer):
    product_group_mdh_id = generate_mdh_id()
    product_group_id = generate_unique_id(session, mdh_import.NonExportableProductGroup)
    product_group = mdh_import.NonExportableProductGroup(
        id=product_group_id,
        name='product_group',
        mdh_id=product_group_mdh_id
    )
    session.add(product_group)

    iso_currency_mdh_id = generate_mdh_id()
    alpha_code = ''.join([random.choice(string.uppercase) for _ in range(10)])
    num_code = generate_unique_id(session, mapper.IsoCurrency, 'num_code')
    iso_currency = mapper.IsoCurrency(
        alpha_code=alpha_code,
        num_code=num_code,
        name='name',
        mdh_id=iso_currency_mdh_id
    )
    session.add(iso_currency)
    currency = mapper.Currency(
        char_code=alpha_code,
        iso_code=alpha_code,
        num_code=num_code,
        iso_num_code=num_code,
        is_internal=0,
        auto_rate=0,
        for_price=1,
    )
    session.add(currency)
    session.flush()

    firm_mdh_id = generate_mdh_id()
    firm_id = generate_unique_id(meta_session, mdh_import.NonExportableFirm)
    firm = mdh_import.NonExportableFirm(
        id=firm_id,
        default_iso_currency=alpha_code,
        email='created@mdh.import',
        phone='+70001112233',
        payment_invoice_email='created@mdh.import',
        mnclose_email='created@mdh.import',
        mdh_id=firm_mdh_id
    )
    meta_session.add(firm)
    meta_session.flush()

    product_mdh_id = generate_mdh_id()
    created_product_id = None
    if update_mode:
        created_product_id = generate_unique_id(session, mdh_import.NonExportableProduct)
        created_product = mdh_import.NonExportableProduct(
            id=created_product_id,
            product_group_id=product_group_id,
            name='created_product',
            mdh_id=None if update_mode == 'with_pk' else product_mdh_id
        )
        session.add(created_product)
        session.flush()
    messages = [
        dict(
            master_uid=product_mdh_id,
            attrs=dict(
                id=created_product_id if update_mode == 'with_pk' else None,
                name='imported_product',
                main_product_id=None,
                reference_price_iso_currency=iso_currency_mdh_id,
                commission_type=None,
                media_discount=None,
                product_group_id=product_group_mdh_id,
                activity_type_id=None,
                adv_kind_id=None,
                service_code=None,
                unit_id=None,
                engine_id=None,
                firm_id=firm_mdh_id
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_product': messages}) as importer:
        importer.main()

    product_list = session.query(mdh_import.NonExportableProduct).filter_by(mdh_id=product_mdh_id).all()
    assert len(product_list) == 1
    product = product_list[0]
    assert product.name == 'imported_product'
    assert product.reference_price_iso_currency == alpha_code
    assert product.reference_price_currency == alpha_code
    assert product.product_group_id == product_group_id
    assert product.firm_id == firm_id

    if update_mode:
        assert product.id == created_product_id

    else:
        for export_type in product.export_ng_types:
            assert session.query(mapper.ExportNg).get((export_type, product.id)), "MDH export didn't work"


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_product_name(meta_session, session, update_mode, mock_importer):
    product = object_builder.ProductBuilder().build(session).obj
    product.mdh_id = generate_mdh_id()
    session.flush()

    language_mdh_id = generate_mdh_id()
    language_id = generate_unique_id(meta_session, mapper.Language)
    language = mapper.Language(
        id=language_id,
        code='CRT',
        lang='created_language',
        balance_code='BLN',
        mdh_id=language_mdh_id
    )
    meta_session.add(language)
    meta_session.flush()

    product_name_mdh_id = generate_mdh_id()
    if update_mode:
        created_product_name = mapper.ProductName(
            product_id=product.id,
            lang_id=language_id,
            product_name='created_product_name',
            mdh_id=None if update_mode == 'with_pk' else product_name_mdh_id
        )
        session.add(created_product_name)
        session.flush()
    messages = [
        dict(
            master_uid=product_name_mdh_id,
            attrs=dict(
                product_id=product.mdh_id,
                lang_id=language_mdh_id,
                product_name='imported_product_name',
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_product_name': messages}) as importer:
        importer.main()

    product_name_list = session.query(mapper.ProductName).filter_by(mdh_id=product_name_mdh_id).all()
    assert len(product_name_list) == 1
    product_name = product_name_list[0]
    assert product_name.product_name == 'imported_product_name'
    assert product_name.product_id == product.id
    assert product_name.lang_id == language_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_prod_season_coeff(session, update_mode, mock_importer):
    product = object_builder.ProductBuilder().build(session).obj
    product.product_group.mdh_id = generate_mdh_id()
    session.flush()

    prod_season_coeff_mdh_id = generate_mdh_id()
    created_prod_season_coeff_id = None
    if update_mode:
        created_prod_season_coeff_id = generate_unique_id(session, mapper.ProdSeasonCoeff)
        created_prod_season_coeff = mapper.ProdSeasonCoeff(
            id=created_prod_season_coeff_id,
            mdh_id=None if update_mode == 'with_pk' else prod_season_coeff_mdh_id
        )
        session.add(created_prod_season_coeff)
        session.flush()
    messages = [
        dict(
            master_uid=prod_season_coeff_mdh_id,
            attrs=dict(
                id=created_prod_season_coeff_id if update_mode == 'with_pk' else None,
                target_id=None,
                group_id=product.product_group.mdh_id,
                dt='2021-07-29',
                finish_dt='2022-07-29'
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_prod_season_coeff': messages}) as importer:
        importer.main()

    prod_season_coeff_list = session.query(mapper.ProdSeasonCoeff).filter_by(mdh_id=prod_season_coeff_mdh_id).all()
    assert len(prod_season_coeff_list) == 1
    prod_season_coeff = prod_season_coeff_list[0]
    assert prod_season_coeff.target_id == product.product_group.id
    assert prod_season_coeff.dt == datetime.datetime.strptime('2021-07-29', '%Y-%m-%d')
    assert prod_season_coeff.finish_dt == datetime.datetime.strptime('2022-07-29', '%Y-%m-%d')
    if update_mode:
        assert prod_season_coeff.id == created_prod_season_coeff_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_product_markup(session, update_mode, mock_importer):
    product = object_builder.ProductBuilder().build(session).obj
    product.mdh_id = generate_mdh_id()

    markup_mdh_id = generate_mdh_id()
    markup_id = generate_unique_id(session, mapper.Markup)
    markup = mapper.Markup(
        id=markup_id,
        code='created_markup',
        mdh_id=markup_mdh_id
    )
    session.add(markup)
    session.flush()

    product_markup_mdh_id = generate_mdh_id()
    created_product_markup_id = None
    if update_mode:
        created_product_markup_id = generate_unique_id(session, mapper.ProductMarkup)
        created_product_markup = mapper.ProductMarkup(
            id=created_product_markup_id,
            pct=10,
            mdh_id=None if update_mode == 'with_pk' else product_markup_mdh_id
        )
        session.add(created_product_markup)
        session.flush()
    messages = [
        dict(
            master_uid=product_markup_mdh_id,
            attrs=dict(
                id=created_product_markup_id if update_mode == 'with_pk' else None,
                product_id=product.mdh_id,
                markup_id=markup_mdh_id,
                dt='2021-07-29',
                pct=12
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_product_markup': messages}) as importer:
        importer.main()

    product_markup_list = session.query(mapper.ProductMarkup).filter_by(mdh_id=product_markup_mdh_id).all()
    assert len(product_markup_list) == 1
    product_markup = product_markup_list[0]
    assert product_markup.product_id == product.id
    assert product_markup.markup_id == markup_id
    assert product_markup.dt == datetime.datetime.strptime('2021-07-29', '%Y-%m-%d')
    if update_mode:
        assert product_markup.id == created_product_markup_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_tax(session, update_mode, mock_importer):
    session.config.set('IMPORT_PRODUCT_TO_OEBS_WITH_NDS_OPERATION_CODE', 1, column_name='value_num', can_create=True)
    session.flush()

    product = object_builder.ProductBuilder().build(session).obj
    product.mdh_id = generate_mdh_id()

    alpha_code = ''.join([random.choice(string.uppercase) for _ in range(10)])
    num_code = generate_unique_id(session, mapper.IsoCurrency, 'num_code')
    iso_currency_mdh_id = generate_mdh_id()
    iso_currency = mapper.IsoCurrency(
        alpha_code=alpha_code,
        num_code=num_code,
        name='created_name',
        mdh_id=iso_currency_mdh_id
    )
    session.add(iso_currency)
    currency = mapper.Currency(
        char_code=alpha_code,
        iso_code=alpha_code,
        num_code=num_code,
        iso_num_code=num_code,
        is_internal=0,
        auto_rate=0,
        for_price=1,
    )
    session.add(currency)
    iso_currency.mdh_id = generate_mdh_id()

    tax_policy = object_builder.TaxPolicyBuilder().build(session).obj
    tax_policy.mdh_id = generate_mdh_id()
    session.flush()

    nds_operation_mdh_id = generate_mdh_id()
    tax_mdh_id = generate_mdh_id()
    created_tax_id = None
    if update_mode:
        created_nds_operation_code_id = generate_unique_id(session, mapper.NDSOperationCode)
        created_nds_operation_code = mapper.NDSOperationCode(
            id=created_nds_operation_code_id,
            code=666,
            description='test description',
            mdh_id=nds_operation_mdh_id,
        )
        session.add(created_nds_operation_code)

        created_tax_id = generate_unique_id(session, mapper.Tax)
        created_tax = mapper.Tax(
            id=created_tax_id,
            dt=datetime.datetime.strptime('2021-07-01', '%Y-%m-%d'),
            iso_currency=alpha_code,
            currency_id=num_code,
            product_id=product.id,
            hidden=0,
            mdh_id=None if update_mode == 'with_pk' else tax_mdh_id,
            nds_operation_code_id=created_nds_operation_code_id
        )
        session.add(created_tax)
        session.flush()

    tax_messages = [
        dict(
            master_uid=tax_mdh_id,
            attrs=dict(
                id=created_tax_id if update_mode == 'with_pk' else None,
                product_id=product.mdh_id,
                iso_currency=iso_currency.mdh_id,
                tax_policy_id=tax_policy.mdh_id,
                dt='2021-07-29',
                nds_operation_code=nds_operation_mdh_id,
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    nds_messages = [
        dict(
            master_uid=nds_operation_mdh_id,
            attrs=dict(
                id=None,
                code=666,
                description='test description',
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={
        'mdh/test/domains/nomenclature/nom_tax': tax_messages,
        'mdh/test/domains/nomenclature/nom_nds_operation_code': nds_messages,
    }) as importer:
        importer.main()

    tax_list = session.query(mapper.Tax).filter_by(mdh_id=tax_mdh_id).all()
    assert len(tax_list) == 1
    tax = tax_list[0]
    assert tax.product_id == product.id
    assert tax.iso_currency == alpha_code
    assert tax.currency_id == num_code
    assert tax.tax_policy_id == tax_policy.id
    assert tax.dt == datetime.datetime.strptime('2021-07-29', '%Y-%m-%d')
    if update_mode:
        assert tax.id == created_tax_id

    if not update_mode:
        exports = session.query(mapper.Export).filter_by(object_id=product.id).first()
        assert exports is not None, "Product with new Tax didn't export to OeBS"


def test_nom_tax_not_save_without_nds_code(session, mock_importer):
    session.config.set('IMPORT_PRODUCT_TO_OEBS_WITH_NDS_OPERATION_CODE', 1, column_name='value_num', can_create=True)
    session.flush()

    product = object_builder.ProductBuilder().build(session).obj
    product.mdh_id = generate_mdh_id()

    alpha_code = ''.join([random.choice(string.uppercase) for _ in range(10)])
    num_code = generate_unique_id(session, mapper.IsoCurrency, 'num_code')
    iso_currency_mdh_id = generate_mdh_id()
    iso_currency = mapper.IsoCurrency(
        alpha_code=alpha_code,
        num_code=num_code,
        name='created_name',
        mdh_id=iso_currency_mdh_id
    )
    session.add(iso_currency)
    currency = mapper.Currency(
        char_code=alpha_code,
        iso_code=alpha_code,
        num_code=num_code,
        iso_num_code=num_code,
        is_internal=0,
        auto_rate=0,
        for_price=1,
    )
    session.add(currency)
    iso_currency.mdh_id = generate_mdh_id()

    tax_policy_id = mapper.TaxPolicy.IDS_WITH_NDS_CODE[0]
    tax_policy = session.query(mapper.TaxPolicy).filter_by(id=tax_policy_id).first()
    if tax_policy is None:
        tax_policy = object_builder.TaxPolicyBuilder(id=tax_policy_id).build(session).obj
        tax_policy.mdh_id = generate_mdh_id()
        session.flush()

    tax_mdh_id = generate_mdh_id()

    messages = [
        dict(
            master_uid=tax_mdh_id,
            attrs=dict(
                id=None,
                product_id=product.mdh_id,
                iso_currency=iso_currency.mdh_id,
                tax_policy_id=tax_policy.mdh_id,
                dt='2021-07-29',
                nds_operation_code=None,
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_tax': messages}) as importer:
        importer.main()
        tax_list = session.query(mapper.Tax).filter_by(mdh_id=tax_mdh_id).all()
        assert len(tax_list) == 0, 'Tax saved with wrong nds_operation_code'
        assert (call().consumer.commit('mdh/test/domains/nomenclature/nom_tax')
                not in importer.lb.get_consumer.mock_calls), \
            'Tax message committed in LogBroker without saved/update object in DB'

    exports = session.query(mapper.Export).filter_by(object_id=product.id).first()
    assert exports is None, 'Export Product to OeBS without saved Tax'


def test_nom_tax_with_only_one_nds_code(session, mock_importer):
    session.config.set('IMPORT_PRODUCT_TO_OEBS_WITH_NDS_OPERATION_CODE', 1, column_name='value_num', can_create=True)
    session.flush()

    product = object_builder.ProductBuilder().build(session).obj
    product.mdh_id = generate_mdh_id()

    alpha_code = ''.join([random.choice(string.uppercase) for _ in range(10)])
    num_code = generate_unique_id(session, mapper.IsoCurrency, 'num_code')
    iso_currency_mdh_id = generate_mdh_id()
    iso_currency = mapper.IsoCurrency(
        alpha_code=alpha_code,
        num_code=num_code,
        name='created_name',
        mdh_id=iso_currency_mdh_id
    )
    session.add(iso_currency)
    currency = mapper.Currency(
        char_code=alpha_code,
        iso_code=alpha_code,
        num_code=num_code,
        iso_num_code=num_code,
        is_internal=0,
        auto_rate=0,
        for_price=1,
    )
    session.add(currency)
    iso_currency.mdh_id = generate_mdh_id()

    was_tax_policy_id = mapper.TaxPolicy.IDS_WITH_NDS_CODE[0]
    tax_policy = session.query(mapper.TaxPolicy).filter_by(id=was_tax_policy_id).first()
    if tax_policy is None:
        tax_policy = object_builder.TaxPolicyBuilder(id=was_tax_policy_id).build(session).obj
        tax_policy.mdh_id = generate_mdh_id()
        session.flush()

    nds_mdh_id = generate_mdh_id()
    nds_operation_code = object_builder.NDSOperationCodeBuilder(mdh_id=nds_mdh_id).build(session).obj
    session.add(nds_operation_code)

    was_tax = mapper.Tax(
        dt=datetime.datetime.strptime('2021-07-01', '%Y-%m-%d'),
        product_id=product.id,
        tax_policy_id=was_tax_policy_id,
        iso_currency=alpha_code,
        currency_id=num_code,
        hidden=0,
        mdh_id=generate_mdh_id(),
        nds_operation_code_id=nds_operation_code.id
    )
    session.add(was_tax)
    session.flush()

    tax_mdh_id = generate_mdh_id()

    messages = [
        dict(
            master_uid=tax_mdh_id,
            attrs=dict(
                id=None,
                product_id=product.mdh_id,
                iso_currency=iso_currency.mdh_id,
                tax_policy_id=tax_policy.mdh_id,
                dt='2021-07-29',
                nds_operation_code=nds_mdh_id,
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_tax': messages}) as importer:
        importer.main()
        tax_list = (session.query(mapper.Tax)
                    .filter(mapper.Tax.product_id == product.id,
                            mapper.Tax.nds_operation_code != None)
                    .all())
        assert len(tax_list) == 1, 'Tax saved with second nds_operation_code for one Product'
        assert (call().consumer.commit('mdh/test/domains/nomenclature/nom_tax')
                not in importer.lb.get_consumer.mock_calls), \
            'Tax message committed in LogBroker without saved/update object in DB'


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_price(session, update_mode, mock_importer):
    product = object_builder.ProductBuilder().build(session).obj
    product.mdh_id = generate_mdh_id()

    alpha_code = ''.join([random.choice(string.uppercase) for _ in range(10)])
    num_code = generate_unique_id(session, mapper.IsoCurrency, 'num_code')
    iso_currency_mdh_id = generate_mdh_id()
    iso_currency = mapper.IsoCurrency(
        alpha_code=alpha_code,
        num_code=num_code,
        name='created_name',
        mdh_id=iso_currency_mdh_id
    )
    session.add(iso_currency)
    currency = mapper.Currency(
        char_code=alpha_code,
        iso_code=alpha_code,
        num_code=num_code,
        iso_num_code=num_code,
        is_internal=0,
        auto_rate=0,
        for_price=1,
    )
    session.add(currency)
    iso_currency.mdh_id = generate_mdh_id()

    tax_policy_pct = object_builder.TaxPolicyPctBuilder().build(session).obj
    tax_policy_pct.mdh_id = generate_mdh_id()
    session.flush()

    price_mdh_id = generate_mdh_id()
    created_price_id = None
    if update_mode:
        created_price_id = generate_unique_id(session, mapper.Price)
        created_price = mapper.Price(
            id=created_price_id,
            dt=datetime.datetime.strptime('2021-07-01', '%Y-%m-%d'),
            iso_currency=alpha_code,
            currency_id=num_code,
            product_id=product.id,
            mdh_id=None if update_mode == 'with_pk' else price_mdh_id
        )
        session.add(created_price)
        session.flush()
    messages = [
        dict(
            master_uid=price_mdh_id,
            attrs=dict(
                id=created_price_id if update_mode == 'with_pk' else None,
                product_id=product.mdh_id,
                iso_currency=iso_currency.mdh_id,
                tax_policy_pct_id=tax_policy_pct.mdh_id,
                dt='2021-07-29'
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_price': messages}) as importer:
        importer.main()

    price_list = session.query(mapper.Price).filter_by(mdh_id=price_mdh_id).all()
    assert len(price_list) == 1
    price = price_list[0]
    assert price.product_id == product.id
    assert price.iso_currency == alpha_code
    assert price.currency_id == num_code
    assert price.tax_policy_pct_id == tax_policy_pct.id
    assert price.dt == datetime.datetime.strptime('2021-07-29', '%Y-%m-%d')
    if update_mode:
        assert price.id == created_price_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_price_with_currency_from_iso_currency(session, update_mode, mock_importer):
    product = object_builder.ProductBuilder().build(session).obj
    product.mdh_id = generate_mdh_id()

    alpha_code = ''.join([random.choice(string.uppercase) for _ in range(10)])
    num_code = generate_unique_id(session, mapper.IsoCurrency, 'num_code')
    iso_currency_mdh_id = generate_mdh_id()
    iso_currency = mapper.IsoCurrency(
        alpha_code=alpha_code,
        num_code=num_code,
        name='created_name',
        mdh_id=iso_currency_mdh_id
    )
    session.add(iso_currency)

    iso_currency.mdh_id = generate_mdh_id()

    tax_policy_pct = object_builder.TaxPolicyPctBuilder().build(session).obj
    tax_policy_pct.mdh_id = generate_mdh_id()
    session.flush()

    price_mdh_id = generate_mdh_id()
    created_price_id = None
    if update_mode:
        created_price_id = generate_unique_id(session, mapper.Price)
        created_price = mapper.Price(
            id=created_price_id,
            dt=datetime.datetime.strptime('2021-07-01', '%Y-%m-%d'),
            iso_currency=alpha_code,
            currency_id=num_code,
            product_id=product.id,
            mdh_id=None if update_mode == 'with_pk' else price_mdh_id
        )
        session.add(created_price)
        session.flush()

    messages = [
        dict(
            master_uid=price_mdh_id,
            attrs=dict(
                id=created_price_id if update_mode == 'with_pk' else None,
                product_id=product.mdh_id,
                iso_currency=iso_currency.mdh_id,
                tax_policy_pct_id=tax_policy_pct.mdh_id,
                dt='2021-07-29'
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_price': messages}) as importer:
        importer.main()

    price_list = session.query(mapper.Price).filter_by(mdh_id=price_mdh_id).all()
    assert len(price_list) == 1
    price = price_list[0]
    assert price.product_id == product.id
    assert price.iso_currency == alpha_code
    assert price.currency_id == num_code


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_nds_operation_code(session, update_mode, mock_importer):
    mdh_id = generate_mdh_id()
    nds_operation_code = None
    if update_mode:
        nds_operation_code = object_builder.NDSOperationCodeBuilder(mdh_id=mdh_id).build(session).obj
        session.add(nds_operation_code)
        session.flush()

    messages = [
        dict(
            master_uid=mdh_id,
            attrs=dict(
                id=nds_operation_code.id if update_mode == 'with_pk' else None,
                description='test description',
                code=666,
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_nds_operation_code': messages}) as importer:
        importer.main()
        nds_operation_code_list = session.query(mapper.NDSOperationCode).filter_by(mdh_id=mdh_id).all()
        assert len(nds_operation_code_list) == 1, 'Not save or duplicate nds_operation_code'
        nds_operation_code = nds_operation_code_list[0]
        assert nds_operation_code.description == 'test description'
        assert nds_operation_code.code == 666
        assert (call().consumer.commit('mdh/test/domains/nomenclature/nom_nds_operation_code')
                in importer.lb.get_consumer.mock_calls), \
            "Didn't commit LogBroker message after save changes"


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_payment_method(meta_session, update_mode, mock_importer):
    mdh_id = generate_mdh_id()
    if update_mode:
        created_payment_method_id = generate_unique_id(meta_session, mapper.PaymentMethod)
        created_payment_method = mapper.PaymentMethod(
            id=created_payment_method_id,
            cc='created_cc',
            name='created_name',
            mdh_id=None if update_mode == 'with_pk' else mdh_id
        )
        meta_session.add(created_payment_method)
        meta_session.flush()
    messages = [
        dict(
            master_uid=mdh_id,
            attrs=dict(
                id=created_payment_method_id if update_mode == 'with_pk' else None,
                cc='imported_cc',
                name='imported_name',
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_payment_method': messages}) as importer:
        importer.main()
        nom_payment_method_list = meta_session.query(mapper.PaymentMethod).filter_by(mdh_id=mdh_id).all()
        assert len(nom_payment_method_list) == 1
        nom_payment_method = nom_payment_method_list[0]
        assert nom_payment_method.cc == 'imported_cc'
        assert nom_payment_method.name == 'imported_name'
        if update_mode:
            assert nom_payment_method.id == created_payment_method_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_pay_policy_region_group_name(session, update_mode, mock_importer):
    mdh_id = generate_mdh_id()
    region_group_id = generate_unique_id(session, mapper.RegionGroupName, field='region_group_id')
    if update_mode:
        created_pay_policy_region_group_name = mapper.RegionGroupName(
            region_group_id=region_group_id,
            region_group_name='created_region_group_name',
            mdh_id=None if update_mode == 'with_pk' else mdh_id
        )
        session.add(created_pay_policy_region_group_name)
        session.flush()
    messages = [
        dict(
            master_uid=mdh_id,
            attrs=dict(
                region_group_id=region_group_id if update_mode == 'with_pk' else None,
                region_group_name='imported_region_group_name'
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_pay_policy_region_group_name': messages}) as importer:
        importer.main()
        pay_policy_region_group_name_list = session.query(mapper.RegionGroupName).filter_by(mdh_id=mdh_id).all()
        assert len(pay_policy_region_group_name_list) == 1
        pay_policy_region_group_name = pay_policy_region_group_name_list[0]
        assert pay_policy_region_group_name.region_group_name == 'imported_region_group_name'
        if update_mode:
            assert pay_policy_region_group_name.region_group_id == region_group_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_pay_policy_region_group(session, meta_session, update_mode, mock_importer):
    country_1 = meta_session.query(mapper.Country).filter(mapper.Country.mdh_id is not None).first()
    country_2 = meta_session.query(mapper.Country).filter(
        mapper.Country.region_id != country_1.region_id and mapper.Country.mdh_id is not None
    ).first()

    region_group_name_mdh_id = generate_mdh_id()
    region_group_id = generate_unique_id(session, mapper.RegionGroupName, field='region_group_id')
    created_pay_policy_region_group_name = mapper.RegionGroupName(
        region_group_id=region_group_id,
        region_group_name='created_region_group_name',
        mdh_id=region_group_name_mdh_id
    )
    session.add(created_pay_policy_region_group_name)
    session.flush()

    mdh_id = generate_mdh_id()
    if update_mode:
        created_pay_policy_region_group_id = generate_unique_id(session, mapper.RegionGroup)
        created_pay_policy_region_group = mapper.RegionGroup(
            id=created_pay_policy_region_group_id,
            region_group_id=region_group_id,
            region_id=country_1.region_id,
            mdh_id=None if update_mode == 'with_pk' else mdh_id
        )
        session.add(created_pay_policy_region_group)
        session.flush()
    messages = [
        dict(
            master_uid=mdh_id,
            attrs=dict(
                id=created_pay_policy_region_group_id if update_mode == 'with_pk' else None,
                region_group_id=region_group_name_mdh_id,
                region_id=country_2.mdh_id
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_pay_policy_region_group': messages}) as importer:
        importer.main()
        pay_policy_region_group_list = session.query(mapper.RegionGroup).filter_by(mdh_id=mdh_id).all()
        assert len(pay_policy_region_group_list) == 1
        pay_policy_region_group = pay_policy_region_group_list[0]
        assert pay_policy_region_group.region_group_id == region_group_id
        assert pay_policy_region_group.region_id == country_2.region_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk'])
def test_nom_pay_policy_service(meta_session, session, update_mode, mock_importer):
    service = meta_session.query(mapper.Service).filter(mapper.Service.mdh_id is not None).first()
    firm = meta_session.query(mapper.Firm).filter(mapper.Firm.mdh_id is not None).first()

    mdh_id = generate_mdh_id()
    if update_mode:
        created_pay_policy_service_id = generate_unique_id(session, mapper.PayPolicyServiceImport)
        created_pay_policy_service = mapper.PayPolicyServiceImport(
            id=created_pay_policy_service_id,
            service_id=service.id,
            firm_id=firm.id,
            description='created',
            mdh_id=None if update_mode == 'with_pk' else mdh_id
        )
        session.add(created_pay_policy_service)
        session.flush()
    messages = [
        dict(
            master_uid=mdh_id,
            attrs=dict(
                id=created_pay_policy_service_id if update_mode == 'with_pk' else None,
                service_id=service.mdh_id,
                firm_id=firm.mdh_id,
                is_atypical=True,
                description='imported',
            ),
            status=mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_pay_policy_service': messages}) as importer:
        importer.main()
        pay_policy_service_list = session.query(mapper.PayPolicyServiceImport).filter_by(mdh_id=mdh_id).all()
        assert len(pay_policy_service_list) == 1
        pay_policy_service = pay_policy_service_list[0]
        assert pay_policy_service.service_id == service.id
        assert pay_policy_service.firm_id == firm.id
        assert pay_policy_service.legal_entity is None
        assert pay_policy_service.category is None
        assert pay_policy_service.is_atypical == 1
        assert pay_policy_service.description == 'imported'
        if update_mode:
            assert pay_policy_service.id == created_pay_policy_service_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk', 'hidden'])
def test_nom_pay_policy_region(meta_session, session, update_mode, mock_importer):
    country = meta_session.query(mapper.Country).first()
    region_group = session.query(mapper.RegionGroupName).first()
    pay_policy_service = session.query(mapper.PayPolicyServiceImport).first()
    if region_group.mdh_id is None:
        region_group.mdh_id = generate_mdh_id()
    if pay_policy_service.mdh_id is None:
        pay_policy_service.mdh_id = generate_mdh_id()
    session.flush()

    mdh_id = generate_mdh_id()
    if update_mode:
        created_pay_policy_region_id = generate_unique_id(session, mapper.PayPolicyRegionImport)
        created_pay_policy_region = mapper.PayPolicyRegionImport(
            id=created_pay_policy_region_id,
            pay_policy_service_id=pay_policy_service.id,
            region_id=country.region_id,
            region_group_id=None,
            mdh_id=None if update_mode == 'with_pk' else mdh_id
        )
        session.add(created_pay_policy_region)
        session.flush()

    messages = [
        dict(
            master_uid=mdh_id,
            attrs=dict(
                id=created_pay_policy_region_id if update_mode == 'with_pk' else None,
                pay_policy_service_id=pay_policy_service.mdh_id,
                region_id=None,
                region_group_id=region_group.mdh_id,
                is_contract=False
            ),
            status=mdh_import.Status.ARCHIVED if update_mode == 'hidden' else mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_pay_policy_region': messages}) as importer:
        importer.main()
        pay_policy_region_list = session.query(mapper.PayPolicyRegionImport).filter_by(mdh_id=mdh_id).all()
        assert len(pay_policy_region_list) == 1
        pay_policy_region = pay_policy_region_list[0]
        assert pay_policy_region.pay_policy_service_id == pay_policy_service.id
        assert pay_policy_region.region_group_id == region_group.region_group_id
        assert pay_policy_region.region_id is None
        assert pay_policy_region.is_agency is None
        assert pay_policy_region.is_contract == 0
        assert pay_policy_region.hidden == (1 if update_mode == 'hidden' else 0)
        if update_mode:
            pay_policy_region.id = created_pay_policy_region_id


@pytest.mark.parametrize('update_mode', [None, 'with_pk', 'without_pk', 'hidden'])
def test_nom_pay_policy_payment_method(meta_session, session, update_mode, mock_importer):
    payment_method = meta_session.query(mapper.PaymentMethod).first()
    pay_policy_service = session.query(mapper.PayPolicyServiceImport).first()
    iso_currency = session.query(mapper.IsoCurrency).first()
    if payment_method.mdh_id is None:
        payment_method.mdh_id = generate_mdh_id()
    meta_session.flush()
    if pay_policy_service.mdh_id is None:
        pay_policy_service.mdh_id = generate_mdh_id()
    session.flush()

    mdh_id = generate_mdh_id()
    if update_mode:
        created_pay_policy_payment_method_id = generate_unique_id(session, mapper.PayPolicyPaymentMethod)
        created_pay_policy_payment_method = mapper.PayPolicyPaymentMethod(
            id=created_pay_policy_payment_method_id,
            pay_policy_service_id=pay_policy_service.id,
            payment_method_id=payment_method.id,
            iso_currency=iso_currency.alpha_code,
            paysys_group_id=0,
            mdh_id=None if update_mode == 'with_pk' else mdh_id
        )
        session.add(created_pay_policy_payment_method)
        session.flush()

    messages = [
        dict(
            master_uid=mdh_id,
            attrs=dict(
                id=created_pay_policy_payment_method_id if update_mode == 'with_pk' else None,
                pay_policy_service_id=pay_policy_service.mdh_id,
                payment_method_id=payment_method.mdh_id,
                iso_currency=iso_currency.mdh_id,
                paysys_group_id=2,
            ),
            status=mdh_import.Status.ARCHIVED if update_mode == 'hidden' else mdh_import.Status.PUBLISHED,
            record_uid='record_uid'
        ),
    ]

    with mock_importer(messages={'mdh/test/domains/nomenclature/nom_pay_policy_payment_method': messages}) as importer:
        importer.main()
        pay_policy_payment_method_list = session.query(mapper.PayPolicyPaymentMethod).filter_by(mdh_id=mdh_id).all()
        assert len(pay_policy_payment_method_list) == 1
        pay_policy_payment_method = pay_policy_payment_method_list[0]
        assert pay_policy_payment_method.pay_policy_service_id == pay_policy_service.id
        assert pay_policy_payment_method.payment_method_id == payment_method.id
        assert pay_policy_payment_method.iso_currency == iso_currency.alpha_code
        assert pay_policy_payment_method.paysys_group_id == 2
        assert pay_policy_payment_method.hidden == (1 if update_mode == 'hidden' else 0)
        if update_mode:
            pay_policy_payment_method.id = created_pay_policy_payment_method_id
