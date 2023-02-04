import datetime
import random
import sqlalchemy as sa
import string
from balance import scheme
from balance.payments import scheme_payments
from balance.mapper import (
    TVMACLApp,
    TVMACLPermissionTable,
    ContractPrefix,
    Service,
    Config,
    PartnerBalanceSource,
    PartnerProcessingSource,
)
from balance.sqs_processors import configshop, trust


class TestConfigshopProcessor(object):
    @staticmethod
    def _body(params):
        return {"params": params}

    def test_integration(self, session):
        processor = configshop.Processor(None, None, None, 0)
        uniq_suffix = datetime.datetime.now().isoformat()
        cc = "test_cc_" + uniq_suffix
        display_name = "test_display_name_" + uniq_suffix
        processor.processor_integration(self._body({"cc": cc, "display_name": display_name}))

        tbl = scheme.integration
        result = session.query(tbl).filter(tbl.c.cc == cc).all()
        assert len(result) == 1
        assert result[0].display_name == display_name

        display_name += '_updated'
        processor.processor_integration(self._body({"cc": cc, "display_name": display_name}))

        result = session.query(tbl).filter(tbl.c.cc == cc).all()
        assert len(result) == 1
        assert result[0].display_name == display_name

    @staticmethod
    def _create_integration(session, cc, display_name):
        tbl = scheme.integration

        result = session.execute(tbl.insert(), {"cc": cc, "display_name": display_name})
        assert result.rowcount == 1

    def test_integrations_configuration(self, session):
        processor = configshop.Processor(None, None, None, 1)
        uniq_suffix = datetime.datetime.now().isoformat()
        cc = "test_cc_" + uniq_suffix
        display_name = "test_display_name_" + uniq_suffix
        integration_cc = "test_integration_cc_" + uniq_suffix
        self._create_integration(session, integration_cc, "test_display")

        schema = {}
        processor.processor_integrations_configuration(self._body({
            "cc": cc, "display_name": display_name,
            "integration_cc": integration_cc, "scheme": schema,
        }))

        tbl = scheme.integrations_configuration

        result = session.query(tbl).filter(tbl.c.cc == cc).all()
        assert len(result) == 1
        assert result[0].display_name == display_name
        assert result[0].integration_cc == integration_cc
        assert result[0].scheme == str(schema)

        display_name += '_updated'
        processor.processor_integrations_configuration(self._body(
            {"cc": cc, "display_name": display_name},
        ))

        result = session.query(tbl).filter(tbl.c.cc == cc).all()
        assert len(result) == 1
        assert result[0].display_name == display_name
        assert result[0].integration_cc == integration_cc
        assert result[0].scheme == str(schema)

    def test_completion_source(self, session):
        processor = configshop.Processor(None, None, None, -1)

        uniq_suffix = datetime.datetime.now().isoformat()
        params = {
            "code": "unique_code_" + uniq_suffix,
            "queue": "TEST_QUEUE",
            "src_id": 0,
            "url": "https://anythin.com",
            "config": {"123": 45},
            "services": [345, 678],
        }
        processor.processor_completion_source(self._body(params.copy()))

        tbl = scheme.completion_source
        result = session.query(tbl).filter(tbl.c.code == params["code"]).all()
        assert len(result) == 1
        assert result[0].queue == params["queue"]
        assert result[0].config == params["config"]
        assert result[0].enabled == 1

        params["url"] = "https://kak-developit-na-pitone-i-ne-umeret.wtf"
        params["enabled"] = False

        processor.processor_completion_source(self._body(params.copy()))

        tbl = scheme.completion_source
        result = session.query(tbl).filter(tbl.c.code == params["code"]).all()
        assert len(result) == 1
        assert result[0].url == params["url"]
        assert result[0].enabled == 0

    def _setup_page_data(self, processor):
        uniq_suffix = datetime.datetime.now().isoformat()
        service_id = random.randint(1, 1000000000000)
        desc = "unique_desc_" + uniq_suffix

        page_id = processor.processor_page_data(self._body({"service_id": service_id, "desc": desc}))
        return page_id["id"], service_id, desc

    def test_page_data(self, session):
        processor = configshop.Processor(None, None, None, -1)
        page_id, service_id, desc = self._setup_page_data(processor)
        assert page_id == service_id * 1000 + 1

        tbl = scheme.partner_pages
        result = session.query(tbl).filter(sa.and_(
            tbl.c.service_id == service_id,
            tbl.c.DESC == desc,
        )).all()

        assert len(result) == 1
        assert result[0].page_id == page_id

        new_page_id = processor.processor_page_data(self._body({"service_id": service_id, "desc": desc}))
        assert page_id == new_page_id["id"]

        uniq_suffix = datetime.datetime.now().isoformat()
        desc = "unique_desc_" + uniq_suffix
        page_id = processor.processor_page_data(self._body({"service_id": service_id, "desc": desc}))

        # service id didn't change
        assert page_id["id"] == new_page_id["id"] + 1

    @staticmethod
    def _setup_service(session):
        service = scheme.services
        service_id = random.randint(1, 1000000000000)
        mdh_id = ''.join([random.choice(string.lowercase) for _ in range(36)])

        name = "magazinchek"
        cc = "megazinchek_x"
        display_name = "valentinki ot marinki"
        params = {
            "id": service_id,
            "mdh_id": mdh_id,
            "name": name,
            "cc": cc,
            "display_name": display_name,
        }
        session.execute(service.insert(), params)
        return params

    def test_balance_service(self, meta_session):
        processor = configshop.Processor(None, None, None, 0)
        params = {
            "unilateral": True,
            "is_spendable": True,
        }
        service_params = self._setup_service(meta_session)

        params_copy = params.copy()
        params_copy["service_id"] = service_params["id"]

        result = processor.processor_balance_service(self._body(params_copy))

        params_copy = params.copy()
        params_copy["service_mdh_id"] = service_params["mdh_id"]
        result2 = processor.processor_balance_service(self._body(params_copy))

        assert result == result2
        assert result == service_params

        tbl = scheme.balance_services
        result = meta_session.query(tbl).filter(tbl.c.id == service_params["id"]).all()

        assert len(result) == 1
        assert result[0].unilateral == 1
        assert result[0].is_spendable == 1

        params_copy = params.copy()
        params_copy["service_id"] = service_params["id"]
        params_copy["is_spendable"] = False
        result3 = processor.processor_balance_service(self._body(params_copy))
        assert result2 == result3

        tbl = scheme.balance_services
        result = meta_session.query(tbl).filter(tbl.c.id == service_params["id"]).all()
        assert len(result) == 1
        assert result[0].is_spendable == 0

    def test_thirdparty_service(self, session):
        processor = configshop.Processor(None, None, None, 0)
        params = {
            "skip_from_trust": False,
            "use_product_mapping": False,
        }
        service_params = self._setup_service(session)

        params_copy = params.copy()
        params_copy["service_id"] = service_params["id"]

        result = processor.process_thirdparty_service(self._body(params_copy))

        params_copy = params.copy()
        params_copy["service_mdh_id"] = service_params["mdh_id"]
        result2 = processor.process_thirdparty_service(self._body(params_copy))

        assert result == result2
        assert result == service_params

        tbl = scheme.thirdparty_service
        result = session.query(tbl).filter(tbl.c.id == service_params["id"]).all()

        assert len(result) == 1
        assert result[0].skip_from_trust == 0
        assert result[0].use_product_mapping == 0
        assert result[0].enabled == 1

        params_copy = params.copy()
        params_copy.update({
            "service_id": service_params["id"],
            "use_product_mapping": True,
            "enabled": False
        })
        result3 = processor.process_thirdparty_service(self._body(params_copy))
        assert result2 == result3

        tbl = scheme.thirdparty_service
        result = session.query(tbl).filter(tbl.c.id == service_params["id"]).all()
        assert len(result) == 1
        assert result[0].use_product_mapping == 1
        assert result[0].enabled == 0

    def test_partner_page(self, session):
        processor = configshop.Processor(None, None, None, -1)
        page_id, service_id, _ = self._setup_page_data(processor)
        payment_type = "u menya promocody"
        params = {
            "page_id": page_id,
            "service_id": service_id,
            "payment_type": payment_type,
        }
        processor.processor_partner_page(self._body(params))

        tbl = scheme.partner_product_pages
        result = session.query(tbl).filter(tbl.c.page_id == page_id).all()
        assert len(result) == 1
        assert result[0].service_id == service_id
        assert result[0].payment_type == payment_type

        # idempotency, no errors and chill
        processor.processor_partner_page(self._body(params))

        result = session.query(tbl).filter(tbl.c.page_id == page_id).all()
        assert len(result) == 1
        assert result[0].service_id == service_id
        assert result[0].payment_type == payment_type

    def _insert_acl_app(self, session, records):
        table = TVMACLApp
        for record in records:
            session.execute(sa.insert(table), {
                'tvm_id': record['tvm_id'],
                'env': record['env'],
            })

    def test_tvm_acl_app(self, session):
        processor = configshop.Processor(None, None, None, -1)
        tvm_ids = [100525, 100526]
        params = {
            "apps": [
                {
                    "tvm_id": tvm_ids[0],
                    "env": "test",
                },
                {
                    "tvm_id": tvm_ids[1],
                    "env": "prod",
                }
            ]
        }
        processor.processor_tvm_acl_app(self._body(params))

        tbl = TVMACLApp
        result = session.query(tbl).filter(tbl.tvm_id.in_(tvm_ids)).all()
        assert len(result) == 2

        # idempotency and chill
        processor.processor_tvm_acl_app(self._body(params))

        tbl = TVMACLApp
        result = session.query(tbl).filter(tbl.tvm_id.in_(tvm_ids)).all()
        assert len(result) == 2

    def test_tvm_acl_permission(self, session):
        tvm_ids = [100523, 100524]
        self._insert_acl_app(session, [
            {
                'tvm_id': tvm_ids[0],
                'env': 'test',
            },
            {
                'tvm_id': tvm_ids[1],
                'env': 'prod',
            },
        ])
        processor = configshop.Processor(None, None, None, -1)
        handlers = [{"endpoint": "httpapi", "method": "get_payment_headers"},
                    {"endpoint": "xmlrpc", "method": "LinkIntegrationToClient"}]
        params = {
            "tvm_ids": tvm_ids,
            "handlers": handlers,
        }
        processor.processor_tvm_acl_permission(self._body(params))

        tbl = TVMACLPermissionTable
        result = session.query(tbl).filter(tbl.tvm_id.in_(tvm_ids)).all()
        assert len(result) == 4
        assert sorted(set([(row.endpoint, row.method_name) for row in result])) == \
               sorted([(handler["endpoint"], handler["method"]) for handler in handlers])

        # partial insert
        handlers.append({"endpoint": "xmlrpc", "method": "Fuflo"})
        processor.processor_tvm_acl_permission(self._body(params))

        result = session.query(tbl).filter(tbl.tvm_id.in_(tvm_ids)).all()
        assert len(result) == 6
        assert sorted(set([(row.endpoint, row.method_name) for row in result])) == \
               sorted([(handler["endpoint"], handler["method"]) for handler in handlers])

    def test_oebs_spendable_service_type_map(self, session):
        processor = configshop.Processor(None, None, None, -1)
        service_id = random.randint(1, 1000000000000)
        oebs_contract_type = 110
        params = {
            "service_id": service_id,
            "oebs_contract_type": oebs_contract_type,
        }
        processor.process_oebs_spendable_service_type_map(self._body(params))

        tbl = scheme.config
        result = session.query(tbl).filter(tbl.c.item == 'OEBS_CONFIG__SPENDABLE_SERVICE_TYPE_MAP').all()
        assert len(result) == 1
        json_value = result[0].value_json_clob
        inserted_value = json_value[-1]
        json_len = len(json_value)
        assert inserted_value == params

        # idempotency, no errors and chill
        processor.process_oebs_spendable_service_type_map(self._body(params))
        result = session.query(tbl).filter(tbl.c.item == 'OEBS_CONFIG__SPENDABLE_SERVICE_TYPE_MAP').all()
        assert len(result) == 1
        json_value = result[0].value_json_clob
        assert json_len == len(json_value)
        assert json_value[-1] == params

        # test update value
        params["oebs_contract_type"] = 112
        processor.process_oebs_spendable_service_type_map(self._body(params))
        result = session.query(tbl).filter(tbl.c.item == 'OEBS_CONFIG__SPENDABLE_SERVICE_TYPE_MAP').all()
        assert len(result) == 1
        json_value = result[0].value_json_clob
        assert json_len == len(json_value)
        assert json_value[-1] == params

        # test insert key
        params["service_id"] = random.randint(1, 1000000000000)
        processor.process_oebs_spendable_service_type_map(self._body(params))
        result = session.query(tbl).filter(tbl.c.item == 'OEBS_CONFIG__SPENDABLE_SERVICE_TYPE_MAP').all()
        assert len(result) == 1
        json_value = result[0].value_json_clob
        assert json_len + 1 == len(json_value)
        assert json_value[-1] == params

    def test_process_contract_prefix(self, session):
        processor = configshop.Processor(None, None, None, -1)
        service_id = random.randint(1, 1000000000000)

        params = {
            'prefix': 'test',
            'service_id': service_id,
            'firm_id': 666,
            'is_offer': True,
            'contract_type': 'SPENDABLE',
        }
        processor.process_contract_prefix(self._body(params))

        result = session.query(ContractPrefix).filter(
            ContractPrefix.service_id == service_id,
            ContractPrefix.firm_id == 666
        ).all()

        assert len(result) == 1, 'Not found prefix'

        new_prefix = result[0]
        assert new_prefix.is_offer == 1
        assert new_prefix.prefix == 'test'
        assert new_prefix.contract_type == 'SPENDABLE'

        # idempotency
        processor.process_contract_prefix(self._body(params))
        result = session.query(ContractPrefix).filter(
            ContractPrefix.service_id == service_id,
            ContractPrefix.firm_id == 666
        ).all()
        assert len(result) == 1, 'Not found prefix'

        new_prefix = result[0]
        assert new_prefix.is_offer == 1
        assert new_prefix.prefix == 'test'
        assert new_prefix.contract_type == 'SPENDABLE'

        # test update value
        params['is_offer'] = False
        processor.process_contract_prefix(self._body(params))

        result = session.query(ContractPrefix).filter(
            ContractPrefix.service_id == service_id,
            ContractPrefix.firm_id == 666
        ).all()
        assert len(result) == 1, 'Not found prefix'

        new_prefix = result[0]
        assert new_prefix.is_offer == 0
        assert new_prefix.prefix == 'test'
        assert new_prefix.contract_type == 'SPENDABLE'

    def test_logbroker_common_fast_balance(self, session):
        processor = configshop.Processor(None, None, None, -1)

        service = session.query(Service).filter(Service.cc != None).first()  # noqa: E711
        logbroker_common = session.query(Config).get('LOGBROKER_COMMON')

        if logbroker_common is None:
            logbroker_common = Config(
                item='LOGBROKER_COMMON',
                value_json_clob={'dev': {'Topic': {}}, 'test': {'Topic': {}}, 'prod': {'Topic': {}}}
            )
            session.add(logbroker_common)
            session.flush()

        params = {
            'service_id': service.id,
            'dev': 'dev_topic',
            'test': 'test_topic',
            'prod': 'prod_topic',
        }
        state = processor.process_logbroker_common_fast_balance(self._body(params))

        logbroker_common = session.query(Config).get('LOGBROKER_COMMON')
        topic_key = '%s-fast-balance' % service.cc
        assert topic_key == state['topic_key']

        for env in ('dev', 'test', 'prod'):
            assert logbroker_common.value_json_clob[env]['Topic'][topic_key] == params[env]

    def test_process_partner_balance_source(self, session):
        processor = configshop.Processor(None, None, None, -1)

        params = {
            'service_group': 666,
            'services': [666, 999],
            'use_cache': 1,
            'enabled': 1,
            'processing_units_metadata': {},
            'balance_type': 'FAST_BALANCE',
        }
        processor.process_partner_balance_source(self._body(params))

        partner = session.query(PartnerBalanceSource).get((params['service_group'], params['balance_type']))

        for field, value in params.items():
            assert getattr(partner, field) == value

        # update
        params['enabled'] = 0
        processor.process_partner_balance_source(self._body(params))

        partner = session.query(PartnerBalanceSource).get((params['service_group'], params['balance_type']))

        for field, value in params.items():
            assert getattr(partner, field) == value

    def test_process_partner_processing_source_fast_balance(self, session):
        processor = configshop.Processor(None, None, None, -1)

        service = session.query(Service).filter(Service.cc != None).first()  # noqa: E711

        params = {
            'service_id': service.id,
            'code': 'finops_config_fast_balance',
        }

        partner = session.query(PartnerProcessingSource).get(('PARTNER_PROCESSING', params['code']))

        if partner is None:
            session.add(PartnerProcessingSource(
                queue='PARTNER_PROCESSING',
                code=params['code'],
                process_payments_enqueue_params=[{
                    "mquery": {"current_signed.services": {"$in": [135, 650, 668, 672, 718, 1181]}},
                    "queue":"PARTNER_FAST_BALANCE",
                    "enqueue_object":"contract",
                    "priority":1
                }],
                personal_account_enqueue_params=[{
                    "mquery": {"current_signed.services": {"$in": [135, 650, 668, 672, 718]}},
                    "queue":"PARTNER_FAST_BALANCE",
                    "enqueue_object":"contract",
                    "priority":1
                }]

            ))
            session.flush()

        processor.process_partner_processing_source_fast_balance(self._body(params))

        partner = session.query(PartnerProcessingSource).filter(PartnerProcessingSource.code == params['code']).one()
        for process_params in (partner.process_payments_enqueue_params, partner.personal_account_enqueue_params):
            for param in process_params:
                if param.get('queue') == 'PARTNER_FAST_BALANCE':
                    assert param['mquery']['current_signed.services']['$in'].count(service.id) == 1
                    break

        # dublicate service.id
        processor.process_partner_processing_source_fast_balance(self._body(params))

        partner = session.query(PartnerProcessingSource).filter(PartnerProcessingSource.code == params['code']).one()
        for params in (partner.process_payments_enqueue_params, partner.personal_account_enqueue_params):
            for param in params:
                if param.get('queue') == 'PARTNER_FAST_BALANCE':
                    assert param['mquery']['current_signed.services']['$in'].count(service.id) == 1
                    break


class TestTrustProcessor(object):
    @staticmethod
    def _body(params):
        return {"params": params}

    def test_trust_service(self, meta_session):
        processor = trust.Processor(None, None, None, 0)

        service = meta_session.query(Service).filter(Service.cc != None).first()

        params = {
            'service_id': service.id,
            'allow_anonymous': 1,
            'schema_name': 'trust_as_processing',
            'db_comp_id': 'db:bs',
            'api_comp_id': 'yb_trust_payments',
            'paymethod_markup_url': 'url'
        }

        processor.processor_trust_service(self._body(params))

        tbl = scheme_payments.trust_services
        s = meta_session.query(tbl).filter(tbl.c.id == service.id).first()
        for key in ['allow_anonymous', 'schema_name', 'db_comp_id', 'api_comp_id', 'paymethod_markup_url']:
            assert params[key] == getattr(s, key)

        params['paymethod_markup_url'] = 'another'

        processor.processor_trust_service(self._body(params))

        tbl = scheme_payments.trust_services
        s = meta_session.query(tbl).filter(tbl.c.id == service.id).first()
        for key in ['allow_anonymous', 'schema_name', 'db_comp_id', 'api_comp_id', 'paymethod_markup_url']:
            assert params[key] == getattr(s, key)

        # idempotency
        processor.processor_trust_service(self._body(params))

        tbl = scheme_payments.trust_services
        s = meta_session.query(tbl).filter(tbl.c.id == service.id).first()
        for key in ['allow_anonymous', 'schema_name', 'db_comp_id', 'api_comp_id', 'paymethod_markup_url']:
            assert params[key] == getattr(s, key)

    def test_fiscal_service(self, meta_session):
        processor = trust.Processor(None, None, None, 0)

        service = meta_session.query(Service).filter(Service.cc != None).first()

        params = {
            'service_id': service.id,
            'url': 'some_url',
            'fiscal_agent_type': 'agent',
            'email': 'some_email',
            'fiscal_auto_send': 1,
            'balance_allowed': 1,
            'allowed_types': ['full_prepayment_wo_delivery', 'partial_payment_w_delivery'],
            'default_allowed_type': 'partial_payment_w_delivery'
        }

        processor.processor_fiscal_service(self._body(params))

        tbl = scheme.fiscal_services
        f = meta_session.query(tbl).filter(tbl.c.id == service.id).first()
        for key in ['url', 'fiscal_agent_type', 'email', 'fiscal_auto_send', 'balance_allowed']:
            assert params[key] == getattr(f, key)
        assert 'full_prepayment_wo_delivery,partial_payment_w_delivery:default' == f.allowed_types

        # idempotency
        processor.processor_fiscal_service(self._body(params))

        tbl = scheme.fiscal_services
        f = meta_session.query(tbl).filter(tbl.c.id == service.id).first()
        for key in ['url', 'fiscal_agent_type', 'email', 'fiscal_auto_send', 'balance_allowed']:
            assert params[key] == getattr(f, key)
        assert 'full_prepayment_wo_delivery,partial_payment_w_delivery:default' == f.allowed_types

        params['default_allowed_type'] = 'full_prepayment_wo_delivery'

        processor.processor_fiscal_service(self._body(params))

        tbl = scheme.fiscal_services
        f = meta_session.query(tbl).filter(tbl.c.id == service.id).first()
        for key in ['url', 'fiscal_agent_type', 'email', 'fiscal_auto_send', 'balance_allowed']:
            assert params[key] == getattr(f, key)
        assert 'full_prepayment_wo_delivery:default,partial_payment_w_delivery' == f.allowed_types

    def test_tvm_application(self, meta_session):
        processor = trust.Processor(None, None, None, 0)

        service = meta_session.query(Service).filter(Service.cc != None).first()

        tvm_ids = [100620, 100621]
        params = {
            'apps': [
                {
                    'id': tvm_ids[0],
                    'alias': 'lala-test',
                    'service_id': service.id,
                    'internal': 0,
                },
                {
                    'id': tvm_ids[1],
                    'alias': 'lala-prod',
                    'service_id': service.id,
                }
            ]
        }

        processor.processor_tvm_application(self._body(params))

        tbl = scheme.tvm_applications
        tvm_test = meta_session.query(tbl).filter(tbl.c.id == tvm_ids[0]).first()
        assert 'lala-test' == tvm_test.alias
        assert service.id == tvm_test.service_id
        assert 0 == tvm_test.internal

        tvm_prod = meta_session.query(tbl).filter(tbl.c.id == tvm_ids[1]).first()
        assert 'lala-prod' == tvm_prod.alias
        assert service.id == tvm_prod.service_id
        assert 0 == tvm_prod.internal

        params = {
            'apps': [
                {
                    'id': tvm_ids[0],
                    'alias': 'lala-changed-test',
                    'service_id': service.id,
                    'internal': 0,
                },
                {
                    'id': tvm_ids[1],
                    'alias': 'lala-changed-prod',
                    'service_id': service.id,
                }
            ]
        }

        processor.processor_tvm_application(self._body(params))

        tvm_test = meta_session.query(tbl).filter(tbl.c.id == tvm_ids[0]).first()
        assert 'lala-changed-test' == tvm_test.alias
        assert service.id == tvm_test.service_id
        assert 0 == tvm_test.internal

        tvm_prod = meta_session.query(tbl).filter(tbl.c.id == tvm_ids[1]).first()
        assert 'lala-changed-prod' == tvm_prod.alias
        assert service.id == tvm_prod.service_id
        assert 0 == tvm_prod.internal

        # idempotency
        processor.processor_tvm_application(self._body(params))

        tvm_test = meta_session.query(tbl).filter(tbl.c.id == tvm_ids[0]).first()
        assert 'lala-changed-test' == tvm_test.alias
        assert service.id == tvm_test.service_id
        assert 0 == tvm_test.internal

        tvm_prod = meta_session.query(tbl).filter(tbl.c.id == tvm_ids[1]).first()
        assert 'lala-changed-prod' == tvm_prod.alias
        assert service.id == tvm_prod.service_id
        assert 0 == tvm_prod.internal

    def test_tvm_application_map(self, meta_session):
        processor = trust.Processor(None, None, None, 0)

        tvm_ids = [100630, 100631]
        params = {
            'apps': [
                {
                    'id': tvm_ids[0],
                    'alias': 'lala2-test',
                    'internal': 0,
                },
                {
                    'id': tvm_ids[1],
                    'alias': 'lala2-prod',
                }
            ]
        }
        processor.processor_tvm_application(self._body(params))


        params = {
            'mapping': [
                {
                    'src_id': tvm_ids[0],
                    'dst_id': tvm_ids[1],
                },
                {
                    'src_id': tvm_ids[1],
                    'dst_id': tvm_ids[0],
                },
            ]
        }

        processor.processor_tvm_application_map(self._body(params))

        tbl = scheme.tvm_application_map
        mapping1 = meta_session.query(tbl).filter(tbl.c.src_id == tvm_ids[0]).first()
        assert tvm_ids[1] == mapping1.dst_id

        mapping2 = meta_session.query(tbl).filter(tbl.c.src_id == tvm_ids[1]).first()
        assert tvm_ids[0] == mapping2.dst_id

        # idempotency
        processor.processor_tvm_application_map(self._body(params))

        tbl = scheme.tvm_application_map
        mapping1 = meta_session.query(tbl).filter(tbl.c.src_id == tvm_ids[0]).first()
        assert tvm_ids[1] == mapping1.dst_id

        mapping2 = meta_session.query(tbl).filter(tbl.c.src_id == tvm_ids[1]).first()
        assert tvm_ids[0] == mapping2.dst_id
