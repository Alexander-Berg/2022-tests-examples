# -*- coding: utf-8 -*-

import datetime
import json

import mock
import httpretty
import pytest
import requests
from hamcrest import (
    assert_that,
    contains_inanyorder,
    has_entries,
    has_properties,
)

from balance import mapper
from balance.actions.invoice_turnon import InvoiceTurnOn
from balance import exc
from balance.constants import (
    RegionId,
    ExportState,
    TVM2_SERVICE_TICKET_HEADER
)
from balance.queue_processor import QueueProcessor
from balance.processors.takeout import (
    get_persons_by_passport,
    prepare_data,
    send_upload_done,
    upload_file,
)

import tests.object_builder as ob
from tests.base import httpretty_ensure_enabled


pytestmark = [
    pytest.mark.takeout
]


class TakeoutBase(object):
    datetime_format = '%Y-%m-%dT%H:%M:%s'
    order_format = '{service_id}-{service_order_id}'
    job_id = 'test-job_id'
    queue_name = 'TAKEOUT'
    consumer = 'balance'
    empty_response = {
        'invoices': [],
        'contracts': [],
        'orders': [],
    }


@pytest.fixture
def client(session):
    return ob.ClientBuilder.construct(session)


@pytest.fixture
def passport(session, client):
    return ob.PassportBuilder.construct(session, client=client)


class TestPersonFilter(object):
    @pytest.mark.parametrize(
        'hidden',
        [0, 1]
    )
    def test_hidden_person(self, session, client, passport, hidden):
        country = ob.Getter(mapper.Country, RegionId.RUSSIA)
        category = ob.PersonCategoryBuilder.construct(session, resident=0, country=country)
        person = ob.PersonBuilder.construct(
            session,
            client=client,
            person_category=category,
            hidden=hidden,
        )
        ob.InvoiceBuilder.construct(session, person=person)

        persons = get_persons_by_passport(passport)
        assert persons == [person]

    @pytest.mark.parametrize('legal_entity', [pytest.param(0, id='ph'), pytest.param(1, id='ur')])
    @pytest.mark.parametrize(
        'resident, region_id, is_ok',
        [
            pytest.param(0, RegionId.RUSSIA, 1, id='ru_nonres'),
            pytest.param(0, RegionId.SWITZERLAND, 1, id='sw_nonres'),
            pytest.param(1, RegionId.RUSSIA, 0, id='ru_res'),
            pytest.param(1, RegionId.SWITZERLAND, 1, id='sw_res'),
        ]
    )
    def test_categories(self, session, client, passport, resident, legal_entity, region_id, is_ok):
        country = ob.Getter(mapper.Country, region_id)
        category = ob.PersonCategoryBuilder.construct(
            session,
            resident=resident,
            ur=legal_entity,
            country=country
        )
        person = ob.PersonBuilder.construct(
            session,
            client=client,
            person_category=category,
        )
        ob.InvoiceBuilder.construct(session, person=person)

        persons = get_persons_by_passport(passport)
        if is_ok:
            assert persons == [person]
        else:
            assert persons == []

    @pytest.mark.parametrize(
        'hidden, is_ok',
        [
            (0, True),
            (1, True),
            (2, False),
        ]
    )
    def test_invoice(self, session, client, passport, hidden, is_ok):
        person = ob.PersonBuilder.construct(session, client=client, type='sw_ur')
        invoice = ob.InvoiceBuilder.construct(session, person=person)
        invoice.hidden = hidden
        session.flush()

        persons = get_persons_by_passport(passport)
        if is_ok:
            assert persons == [person]
        else:
            assert persons == []

    @pytest.mark.parametrize(
        'is_cancelled, is_ok',
        [
            pytest.param(None, True, id='not_cancelled'),
            pytest.param(datetime.datetime.now(), False, id='cancelled'),
        ]
    )
    def test_contract(self, session, client, passport, is_cancelled, is_ok):
        person = ob.PersonBuilder.construct(session, client=client, type='sw_ur')
        contract = ob.ContractBuilder.construct(session, person=person)
        contract.col0.is_cancelled = is_cancelled
        session.flush()

        persons = get_persons_by_passport(passport)
        if is_ok:
            assert persons == [person]
        else:
            assert persons == []


class TestPrepareData(TakeoutBase):
    @pytest.mark.parametrize('invoices_amount, persons_amount', [
        pytest.param(0, 1, id='without_invoices'),
        pytest.param(2, 2, id='with_invoices'),
    ])
    def test_invoices(self, session, client, invoices_amount, persons_amount):
        persons = [ob.PersonBuilder.construct(
            session=session,
            client=client,
            verified_docs=True,
            hidden=0,
            type='sw_ur',
        ) for _ in xrange(persons_amount)]
        invoices = [
            ob.InvoiceBuilder.construct(session=session, client=client, person=person)
            for _ in xrange(invoices_amount)
            for person in persons
        ]
        expected_data = self.empty_response
        expected_data['invoices'] = [{
                'date': invoice.dt.strftime(self.datetime_format),
                'number': invoice.external_id,
                'acts': [],
            } for invoice in invoices
        ]
        prepared_data = prepare_data(persons)

        assert_that(
            prepared_data,
            contains_inanyorder(*expected_data),
        )

    @pytest.mark.parametrize('contracts_amount, persons_amount', [
        pytest.param(0, 1, id='without_contracts'),
        pytest.param(2, 2, id='with_contracts'),
    ])
    def test_contracts(self, session, client, contracts_amount, persons_amount):
        persons = [ob.PersonBuilder.construct(
            session=session,
            client=client,
            type='sw_ur',
            verified_docs=True,
        ) for _ in xrange(persons_amount)]
        contracts = [
            ob.ContractBuilder.construct(session=session, client=client, person=person)
            for _ in xrange(contracts_amount)
            for person in persons
        ]
        expected_data = self.empty_response
        expected_data['contracts'] = contains_inanyorder([{
            'date': contract.update_dt.strftime(self.datetime_format),
            'number': contract.external_id,
        } for contract in contracts])
        prepared_data = prepare_data(persons)

        assert_that(prepared_data, expected_data)

    def test_orders_acts(self, session, client):
        persons = [ob.PersonBuilder.construct(
            session=session,
            client=client,
            type='sw_ur',
            verified_docs=True,
        ) for _ in xrange(2)]
        invoices = [
            ob.InvoiceBuilder.construct(session=session, client=client, person=person)
            for person in persons
        ]
        for invoice in invoices:
            InvoiceTurnOn(invoice, manual=True).do()
            invoice.close_invoice(act_dt=datetime.datetime.now())
            a, = invoice.acts
            a.factura = str(ob.get_big_number())

        ob.OrderBuilder.construct(session, client=client)   # extra order that should not be reported
        orders = [io.order for i in invoices for io in i.invoice_orders]

        expected_data = self.empty_response
        expected_data['invoices'] = contains_inanyorder([{
            'date': invoice.dt.strftime(self.datetime_format),
            'number': invoice.external_id,
            'acts': [{
                'number': act.external_id,
                'date': act.dt.strftime(self.datetime_format),
                'vat_invoice': act.factura,
            } for act in invoice.acts],
        } for invoice in invoices])
        expected_data['orders'] = contains_inanyorder([
            self.order_format.format(service_id=order.service_id, service_order_id=order.service_order_id)
            for order in orders
        ])
        prepared_data = prepare_data(persons)

        assert_that(prepared_data, expected_data)


class TestUploadFile(TakeoutBase):
    takeout_cfg = {
        'URL': 'http://takeout.yandex.ru',
        'Consumer': TakeoutBase.consumer,
    }
    tvm_service_ticket = '1234567890'
    data = {'data': 'data'}
    file_name = 'balance_data.json'

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_happy_path(self):
        httpretty.register_uri(
            httpretty.POST,
            self.takeout_cfg['URL'] + '/1/upload/',
            json.dumps({u'status': u'ok'})
        )

        upload_file(
            takeout_cfg=self.takeout_cfg,
            tvm_service_ticket=self.tvm_service_ticket,
            job_id=self.job_id,
            data=self.data,
            file_name=self.file_name,
        )

        request, = httpretty.httpretty.latest_requests
        assert_that(
            request,
            has_properties(
                headers=has_entries({TVM2_SERVICE_TICKET_HEADER: self.tvm_service_ticket}),
                querystring=has_entries({'consumer': [self.consumer]})
            )
        )

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_invalid_job_id(self):
        httpretty.register_uri(
            httpretty.POST,
            self.takeout_cfg['URL'] + '/1/upload/',
            json.dumps({u'status': u'job_id.invalid'})
        )

        with pytest.raises(exc.TAKEOUT_INVALID_JOB_ID):
            upload_file(
                takeout_cfg=self.takeout_cfg,
                tvm_service_ticket=self.tvm_service_ticket,
                job_id=self.job_id,
                data=self.data,
                file_name=self.file_name,
            )

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_takeout_server_500(self):
        httpretty.register_uri(
            httpretty.POST,
            self.takeout_cfg['URL'] + '/1/upload/',
            'PERSONAL DATA FOR THE GDPR GOD',
            status=500
        )

        with pytest.raises(requests.HTTPError):
            upload_file(
                takeout_cfg=self.takeout_cfg,
                tvm_service_ticket=self.tvm_service_ticket,
                job_id=self.job_id,
                data=self.data,
                file_name=self.file_name,
            )

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_takeout_unexpected_status(self):
        httpretty.register_uri(
            httpretty.POST,
            self.takeout_cfg['URL'] + '/1/upload/',
            json.dumps({u'status': u'not_ok'})
        )

        with pytest.raises(exc.TAKEOUT_UNEXPECTED_RESPONSE):
            upload_file(
                takeout_cfg=self.takeout_cfg,
                tvm_service_ticket=self.tvm_service_ticket,
                job_id=self.job_id,
                data=self.data,
                file_name=self.file_name,
            )

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_takeout_invalid_body(self):
        httpretty.register_uri(
            httpretty.POST,
            self.takeout_cfg['URL'] + '/1/upload/',
            'PERSONAL DATA FOR THE GDPR GOD',
        )

        with pytest.raises(exc.TAKEOUT_UNEXPECTED_RESPONSE):
            upload_file(
                takeout_cfg=self.takeout_cfg,
                tvm_service_ticket=self.tvm_service_ticket,
                job_id=self.job_id,
                data=self.data,
                file_name=self.file_name,
            )


class TestSendUploadDone(TakeoutBase):
    takeout_cfg = {
        'URL': 'http://takeout.yandex.ru',
        'Consumer': TakeoutBase.consumer,
    }
    tvm_service_ticket = '1234567890'
    file_name = 'balance_data.json'

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_happy_path(self):
        httpretty.register_uri(
            httpretty.POST,
            self.takeout_cfg['URL'] + '/1/upload/done/',
            json.dumps({u'status': u'ok'})
        )

        send_upload_done(
            takeout_cfg=self.takeout_cfg,
            tvm_service_ticket=self.tvm_service_ticket,
            job_id=self.job_id,
            file_name=self.file_name,
        )

        request, = httpretty.httpretty.latest_requests
        assert_that(
            request,
            has_properties(
                headers=has_entries({TVM2_SERVICE_TICKET_HEADER: self.tvm_service_ticket}),
                querystring={'consumer': [self.consumer]},
                parsed_body={'job_id': [self.job_id], 'filename': [self.file_name]}
            )
        )

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_invalid_response(self):
        httpretty.register_uri(
            httpretty.POST,
            self.takeout_cfg['URL'] + '/1/upload/done/',
            json.dumps({u'status': u'missing'})
        )

        with pytest.raises(exc.TAKEOUT_MISSING_FILE):
            send_upload_done(
                takeout_cfg=self.takeout_cfg,
                tvm_service_ticket=self.tvm_service_ticket,
                job_id=self.job_id,
                file_name=self.file_name,
            )

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_takeout_server_500(self):
        httpretty.register_uri(
            httpretty.POST,
            self.takeout_cfg['URL'] + '/1/upload/done/',
            '12321',
            status=500
        )

        with pytest.raises(requests.HTTPError):
            send_upload_done(
                takeout_cfg=self.takeout_cfg,
                tvm_service_ticket=self.tvm_service_ticket,
                job_id=self.job_id,
                file_name=self.file_name,
            )

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_takeout_unexpected_status(self):
        httpretty.register_uri(
            httpretty.POST,
            self.takeout_cfg['URL'] + '/1/upload/done/',
            json.dumps({u'status': u'meh'})
        )

        with pytest.raises(exc.TAKEOUT_UNEXPECTED_RESPONSE):
            send_upload_done(
                takeout_cfg=self.takeout_cfg,
                tvm_service_ticket=self.tvm_service_ticket,
                job_id=self.job_id,
                file_name=self.file_name,
            )

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_takeout_invalid_body(self):
        httpretty.register_uri(
            httpretty.POST,
            self.takeout_cfg['URL'] + '/1/upload/done/',
            'got some data, dude?'
        )

        with pytest.raises(exc.TAKEOUT_UNEXPECTED_RESPONSE):
            send_upload_done(
                takeout_cfg=self.takeout_cfg,
                tvm_service_ticket=self.tvm_service_ticket,
                job_id=self.job_id,
                file_name=self.file_name,
            )


class TestQueue(TakeoutBase):
    @pytest.fixture(autouse=True)
    def mock_tvm(self, session):
        takeout_component = {
            'URL': 'http://takeout.yandex.ru',
            'TVMId': '123456789',
            'Consumer': 'balance',
        }
        component_mocker = mock.patch(
            'butils.application.plugins.components_cfg.get_component_cfg',
            return_value=takeout_component
        )
        get_service_ticket_mocker = mock.patch('balance.processors.takeout.get_service_ticket')

        with component_mocker, get_service_ticket_mocker as get_service_ticket_mock:
            get_service_ticket_mock.return_value = 'qeroihjohjrgwohrgwoihfbrw'
            yield

    def test_happy_path(self, session, client, passport):
        person = ob.PersonBuilder.construct(session, client=client, type='sw_ur')
        ob.InvoiceBuilder.construct(session, client=client, person=person)
        passport.enqueue('TAKEOUT', input_={'job_id': 6666})
        export_obj = passport.exports['TAKEOUT']

        with httpretty_ensure_enabled():
            httpretty.register_uri(
                httpretty.POST,
                'http://takeout.yandex.ru/1/upload/',
                json.dumps({'status': 'ok'})
            )
            httpretty.register_uri(
                httpretty.POST,
                'http://takeout.yandex.ru/1/upload/done/',
                json.dumps({'status': 'ok'})
            )

            QueueProcessor('TAKEOUT').process_one(export_obj)
            assert export_obj.state == ExportState.exported

    def test_no_data(self, session, client, passport):
        ob.PersonBuilder.construct(session, client=client, type='sw_ur')
        passport.enqueue('TAKEOUT', input_={'job_id': 6666})
        export_obj = passport.exports['TAKEOUT']

        with httpretty_ensure_enabled():
            QueueProcessor('TAKEOUT').process_one(export_obj)

            assert_that(
                export_obj,
                has_properties(
                    state=ExportState.enqueued,
                    rate=1,
                    error='No data for passport_id = %s' % passport.passport_id
                )
            )
