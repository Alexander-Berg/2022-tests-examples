# -*- coding: utf-8 -*-
from datetime import datetime, timedelta
import uuid
import xmlrpclib

import pytest

from balance import mapper, exc
from billing.contract_iface import ContractTypeId
from balance.constants import *
from balance.contractpage import ContractPage

from tests import object_builder as ob


@pytest.mark.usefixtures("session")
@pytest.mark.usefixtures("xmlrpcserver")
class ContractUtils(object):

    MANAGER_UID = 342884841

    CURRENT_DT = datetime.today()
    START_DT_FUTURE = CURRENT_DT + timedelta(days=5)
    START_DT_PAST = CURRENT_DT + timedelta(days=-5)

    def offer_params(self, manager):
        person = ob.PersonBuilder().build(self.session).obj
        client = person.client
        return {
            'client_id': client.id,
            'currency': 'RUB',
            'firm_id': FirmId.CLOUD_TECHNOLOGIES,
            'manager_uid': manager.domain_passport_id,
            'payment_type': PREPAY_PAYMENT_TYPE,
            'person_id': person.id,
            'start_dt': self.START_DT_FUTURE,
            'service_start_dt': self.START_DT_PAST,
            'services': {ServiceId.CLOUD}
        }

    def contract_params(self, manager):
        person = ob.PersonBuilder().build(self.session).obj
        client = person.client
        return {
            'commission': ContractTypeId.OFFER,
            'firm': FirmId.CLOUD_TECHNOLOGIES,
            'manager-code': manager.manager_code,
            'client-id': client.id,
            'person-id': person.id,
            'services': {ServiceId.CLOUD},
            ('services-%s' % ServiceId.CLOUD): ServiceId.CLOUD,
            'dt': self.START_DT_FUTURE,
            'is-signed-dt': None,
            'finish-dt': self.START_DT_FUTURE + timedelta(days=30),
            'payment-type': PREPAY_PAYMENT_TYPE,  # предоплата
        }

    def new_offer(self, manager, additional_params=None):
        params = self.offer_params(manager)
        if additional_params is not None:
            params.update(additional_params)
        return self.xmlrpcserver.CreateOffer(self.session.oper_id, params)

    def new_contract(self, manager):
        params = self.contract_params(manager)
        return self.xmlrpcserver.CreateContract(self.session.oper_id, params)

    def update_projects(self, instructions=None, start_dt=None):
        return self.xmlrpcserver.UpdateProjects(
            self.session.oper_id,
            {
                'start_dt': start_dt or self.START_DT_FUTURE,
                'instructions': instructions,
            }
        )

    def gen_cloud_project_uuid(self):
        while True:
            yield self.create_cloud_project_uuid()

    def create_cloud_project_uuid(self):
        query = "SELECT S_CLOUD_PROJECT_TEST.nextval id FROM dual"
        project_id = self.session.execute(query).first()['id']
        project_id_str = str(project_id).zfill(32)
        return '{}-{}-{}-{}-{}'.format(project_id_str[:8], project_id_str[8:12], project_id_str[12:16],
                                       project_id_str[16:20], project_id_str[20:])

    def check_create_contract_res(self, res):
        assert isinstance(res, dict)
        assert set(res.keys()) == {'ID', 'EXTERNAL_ID'}

        contract_obj = self.session.query(mapper.Contract).get(res['ID'])
        assert contract_obj.external_id == res['EXTERNAL_ID']

    def check_create_contract_res_projects(self, res, projects_expected):
        assert isinstance(res, dict)
        assert set(res.keys()) == {'ID', 'EXTERNAL_ID'}
        contract_obj = self.session.query(mapper.Contract).get(res['ID'])
        projects_actual = contract_obj.current_state().contract_projects
        assert contract_obj.external_id == res['EXTERNAL_ID']
        assert sorted(projects_actual.values()) == sorted(projects_expected['projects'])


class TestCreateOfferForClouds(ContractUtils):

    # Набор тестов, которые не должны вызывать исключения
    ## предоплата без payment_term
    ## уникальные projects в рамках договора (делается в первом тесте)
    ## договор без projects (делается в первом тесте)
    ## постоплата
    ## start_dt в прошлом

    @pytest.mark.parametrize('additional_params',
                             [{'projects:': [str(uuid.uuid4())]},
                              {'payment_term': 15, 'payment_type': POSTPAY_PAYMENT_TYPE},
                              {'start_dt': datetime.today() + timedelta(days=-5)}
                              ],
                             ids=['Check prepay without payment_term',
                                  'Check postpay payment type',
                                  'Check if START_DT in the past'])
    def test_create_partners_offer_params(self, some_manager, additional_params):
        projects = {'projects': [str(uuid.uuid4())]}
        res = self.new_offer(some_manager, additional_params=dict(additional_params, **projects))
        self.check_create_contract_res_projects(res, projects)

    # Набор тестов, которые должны вызвать исключение
    ## плательщика нет в t_person
    ## менеджера нет в t_manager
    ## невалидный payment_type
    ## невалидный payment_term
    ## невалидная валюта
    ## одинаковые projects в рамках договора
    ## для постоплаты payment_term обязателен
    ## projects нельзя задать для других сервисов
    @pytest.mark.parametrize('additional_params, failure_msg',
                             [({'person_id': -1}, "Person with ID -1 not found in DB"),
                              ({'manager_uid': -1}, "Invalid parameter for function: Manager uid=-1 with manager_type=1 not found"),
                              ({'payment_type': 0},
                               'Invalid parameter for function: PAYMENT_TYPE. Value must be in (2, 3)'),
                              ({'payment_type': POSTPAY_PAYMENT_TYPE, 'payment_term': 3},
                               'Invalid parameter for function: PAYMENT_TERM. Value is not allowed. Allowed values: ['),
                              ({'currency': 'QWE'},
                               'Invalid parameter for function: Currency with iso_code QWE not found'),
                              # ({'projects': [str(uuid.uuid4())] * 2},
                              #  'Invalid parameter for function: PROJECTS. List must contain unique values'),
                              ({'payment_type': POSTPAY_PAYMENT_TYPE},
                               'Invalid parameter for function: PAYMENT_TERM. You must specify it when using POSTPAY payment type.'),
                              ({'services': [DIRECT_SERVICE_ID], 'projects': []},
                               'Invalid parameter for function: PROJECTS. These services do not need projects.'),
                              ({'projects': [6666, 7777]},
                               u'''<error><msg>Rule violation: 'Список проектов договоров должен содержать не более одного проекта'</msg><wo-rollback>0</wo-rollback><rule>Список проектов договоров должен содержать не более одного проекта</rule><method>Balance.CreateOffer</method><code>CONTRACT_RULE_VIOLATION</code><parent-codes><code>EXCEPTION</code></parent-codes><contents>Rule violation: 'Список проектов договоров должен содержать не более одного проекта'</contents></error>''')
                              ],
                             ids=['Person with ID -1 not found in t_person',
                                  'Manager with ID -1 not found in t_manager',
                                  'Invalid parameter for function: PAYMENT_TYPE. Value must be in (2, 3)',
                                  'Invalid parameter for function: PAYMENT_TERM. Value is not allowed.',
                                  'Invalid parameter for function: Currency with iso_code QWE not found',
                                  # 'Invalid parameter for function: PROJECTS. List must contain unique values',
                                  'Invalid parameter for function: PAYMENT_TERM. You must specify it when using POSTPAY payment type.',
                                  'Invalid parameter for function: PROJECTS. These services do not need projects.',
                                  '2 or more projects'])
    def test_create_partners_offer_params_exc(self, some_manager, additional_params, failure_msg):
        if 'projects' in additional_params:
            additional_params['projects'] = [self.create_cloud_project_uuid()] * 2
        with pytest.raises(xmlrpclib.Fault) as exc_info:
            self.new_offer(some_manager, additional_params=additional_params)
        assert failure_msg in exc_info.value.faultString

    # одинаковые projects в разных договорах
    def test_create_partners_offer_params_not_unique_projects_within_two_contract(self, some_manager):
        project = self.create_cloud_project_uuid()
        additional_params = {'projects': [project]}
        contract_id_1 = self.new_offer(some_manager, additional_params=additional_params)
        failure_msg = 'Invalid parameter for function: PROJECT'

        with pytest.raises(Exception) as exc:
            self.new_offer(some_manager, additional_params=additional_params)
        assert failure_msg in exc.value.faultString and \
               str(contract_id_1['ID']) in exc.value.faultString and \
               project in exc.value.faultString

    # для сервисов <>143 создается оферта без projects
    def test_create_partners_offer_another_service_without_projects(self, some_manager):
        additional_params = {'services': [DIRECT_SERVICE_ID]}
        res = self.new_offer(some_manager, additional_params=additional_params)
        self.check_create_contract_res(res)

    # перенос проекта из одного договора в другой без конфликтов
    def test_update_projects_move_without_conflicts(self, some_manager):
        project = self.create_cloud_project_uuid()
        additional_params = {'projects': [project]}
        contract_id_1 = self.new_offer(some_manager, additional_params=additional_params)
        contract_id_2 = self.new_offer(some_manager)
        self.update_projects([{'action': 'move_projects', 'source': contract_id_1['ID'], 'target': contract_id_2['ID'],
                               'projects': [project]}])
        self.check_create_contract_res_projects(contract_id_1, {'projects': []})
        self.check_create_contract_res_projects(contract_id_2, {'projects': [project]})

    # добавление проекта в договор без конфликтов
    def test_update_projects_add_without_conflicts(self, some_manager):
        projects = (project for project in self.gen_cloud_project_uuid())
        project_2 = next(projects)
        contract_id = self.new_offer(some_manager)
        self.update_projects([{
            'action': 'add_projects',
            'target': contract_id['ID'],
            'projects': [project_2]
        }])
        self.check_create_contract_res_projects(contract_id, {'projects': [project_2]})

    # добавление проекта в договор с ожидаемым конфликтом
    # Тест отключен, т.к. конфликтует с проверкой правила "Запрещено создавать два доп. соглашения на одну дату"
    # Возможная причина: тест выполняет _предварительные_ (тестовые) коммиты создаваемых допсоглашений
    # С одной стороны такой коммит необходим для проверки pojects, с другой - мешает проверке указанного правила
    @pytest.mark.skip()
    def test_update_projects_add_with_conflict(self, some_manager):
        projects = (project for project in self.gen_cloud_project_uuid())
        project = next(projects)

        contract_id_1 = self.new_offer(some_manager, additional_params={'projects': [project]})
        contract_id_2 = self.new_offer(some_manager, additional_params={'projects': []})

        with pytest.raises(Exception) as exc:
            self.update_projects([{
                'action': 'add_projects',
                'target': contract_id_2['ID'],
                'projects': [project]
            }])

        failure_msg = 'Invalid parameter for function: PROJECT'

        assert failure_msg in exc.value.faultString and \
               str(contract_id_1['ID']) in exc.value.faultString and \
               project in exc.value.faultString

    def test_move_add_project_without_conflict(self, some_manager):
        pg = self.gen_cloud_project_uuid()
        project_1 = next(pg)

        contract_id_1 = self.new_offer(
            some_manager,
            additional_params={
                'projects': [project_1],
                'sign_dt': datetime.strptime('2019-01-01', '%Y-%m-%d'),
            })

        contract_id_2 = self.new_offer(
            some_manager,
            additional_params={
                'sign_dt': datetime.strptime('2019-05-05', '%Y-%m-%d')
            })

        contract_2 = self.session.query(mapper.Contract).get(contract_id_2['ID'])

        self.update_projects([{
            'action': 'move_projects',
            'source': contract_id_1['ID'],
            'target': contract_id_2['ID'],
            'projects': [project_1],
        }], start_dt=contract_2.col0.dt + timedelta(days=1))

        self.check_create_contract_res_projects(contract_id_2, {'projects': [project_1]})

    def test_move_add_project_unsigned(self, some_manager):
        pg = self.gen_cloud_project_uuid()
        project_2 = next(pg)

        contract_id_1 = self.new_offer(some_manager)
        contract = self.session.query(mapper.Contract).get(contract_id_1['ID'])
        self.update_projects([{
            'action': 'add_projects',
            'target': contract_id_1['ID'],
            'projects': [project_2],
        }], start_dt=contract.col0.dt + timedelta(days=1))

        self.check_create_contract_res_projects(contract_id_1, {'projects': [project_2]})

    def test_duplicate_project_to_unsigned_from_signed(self, some_manager):
        pg = self.gen_cloud_project_uuid()
        project_id = next(pg)

        contract_signed = self.new_offer(
            some_manager,
            additional_params={
                'sign_dt': datetime.strptime('2019-05-05', '%Y-%m-%d'),
                'projects': [project_id],
            })

        # проверим что к новому неподписанному договору добавится дубль проекта
        contract_unsigned = self.new_contract(some_manager)
        self.update_projects([{
            'action': 'add_projects',
            'target': contract_unsigned['ID'],
            'projects': [project_id],
        }])

        self.check_create_contract_res_projects(contract_unsigned, {'projects': [project_id]})

        # проверим что нельзя подписать второй контракт
        cp = ContractPage(self.session, contract_unsigned['ID'])
        with pytest.raises(exc.INVALID_PARAM) as e:
            cp.sign()
            self.session.commit()
        expected_msg = 'Project "{p_id}" already in use in contract "{c_id}"'.format(p_id=project_id,
                                                                                     c_id=contract_signed['ID'])
        assert str(e.value).endswith(expected_msg)
