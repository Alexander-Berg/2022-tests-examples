# -*- coding: utf-8 -*-
import datetime
import pytest
import mock

from balance import mapper
from billing.contract_iface import ContractTypeId, CollateralType
from balance import constants as cst

from balance import contract_notify_email

from tests import object_builder as ob


class SubClass(object):

    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)

    def __getattr__(self, key):
        if key in self.__dict__:
            return super(SubClass, self).__getattr__(key)


class MockContract(object):
    type = 'COMMISSION'
    commission = ContractTypeId.COMMISSION
    firm = SubClass(id=cst.FirmId.YANDEX_OOO)
    person = None
    col0 = SubClass(services={})
    col = SubClass(collateral_type=SubClass(id=0))

    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)

    def has_ability(self, key):
        return getattr(self, 'commission', None) in mapper.Contract.abilities[key]


CONTRACTS_GROUP_PARAMS = [
    (1, u'дистрибуция',
     MockContract(
         type='DISTRIBUTION',
         col=SubClass(collateral_type=SubClass(id=CollateralType.DISTRIBUTION))
     ),
     []),
    (2.1, u'агентские продажи', MockContract(), []),
    (2.2, u'агентские продажи', MockContract(firm=SubClass(id=cst.FirmId.GAS_STATIONS)), ['zapravki']),
    (2.3, u'агентские продажи', MockContract(commission=ContractTypeId.WHOLESALE_AGENCY), []),
    (2.4, u'агентские продажи', MockContract(commission=ContractTypeId.DIRECT_AGENCY), []),
    (2.5, u'агентские продажи', MockContract(commission=ContractTypeId.WHOLESALE_AGENCY_AWARD), []),
    (2.6, u'агентские продажи', MockContract(commission=ContractTypeId.YANDEX_ADS_WHOLESALE_AGENCY_AWARD), []),
    (3, u'агентские продажи. ОФД', MockContract(firm=SubClass(id=cst.FirmId.OFD)), []),
    (4, u'агентские продажи. Реклама',
     MockContract(
         firm=SubClass(id=cst.FirmId.YANDEX_ADS),
         commission=ContractTypeId.YANDEX_ADS_WHOLESALE_AGENCY_AWARD),
     []
     ),
    (5, u'индивидуальные продажи. Реклама', MockContract(firm=SubClass(id=cst.FirmId.YANDEX_ADS)), []),
    (6, u'агентские продажи. Казахстан', MockContract(firm=SubClass(id=cst.FirmId.YANDEX_KZ), commission=ContractTypeId.KZ_COMMISSION), []),
    (7, u'индивидуальные продажи. Казахстан', MockContract(firm=SubClass(id=cst.FirmId.YANDEX_KZ)), []),
    (8, u'клиентские продажи',
     MockContract(
         commission=ContractTypeId.NON_AGENCY,
         person=SubClass(vip=False)
     ),
     ['PPC', 'bk', 'mkb', 'mediaselling', 'geocontext'],
     ),
    (9, u'проектные сделки',
     MockContract(
         commission=ContractTypeId.NON_AGENCY,
         person=SubClass(vip=True)
     ),
     ['adfox', 'apikeys', 'addapter_dev', 'addapter_ret', 'ticket_to_ride', 'mediana', 'connect', 'tours'],
     ),
    (10, None,
     MockContract(
         commission=ContractTypeId.NON_AGENCY,
         person=SubClass(vip=True),
     ),
     ['onetime']
     ),
    (11, u'индивидуальные продажи. VIP',
     MockContract(
         commission=ContractTypeId.NON_AGENCY,
         person=SubClass(vip=True)
     ),
     []
     ),
    (12, u'проектные сделки',
     MockContract(commission=ContractTypeId.NON_AGENCY),
     ['adfox', 'apikeys', 'addapter_dev', 'addapter_ret', 'ticket_to_ride', 'mediana', 'connect', 'tours']
     ),
    (13, u'проектные сделки. Медиа',
     MockContract(
         commission=ContractTypeId.NON_AGENCY,
         firm=SubClass(id=cst.FirmId.MEDIASERVICES)
     ),
     ['tickets', 'events_tickets', 'events_tickets_new', 'afisha_moviepass', 'music', 'music-promo'],
     ),
    (14.1, u'проектные сделки', MockContract(firm=SubClass(id=cst.FirmId.CLOUD)), []),
    (14.2, u'проектные сделки', MockContract(firm=SubClass(id=cst.FirmId.HEALTH_CLINIC)), []),
    (14.3, u'проектные сделки', MockContract(firm=SubClass(id=cst.FirmId.BUS)), []),
    (15, u'вертикаль', MockContract(firm=SubClass(id=cst.FirmId.VERTIKALI)), []),
    (16, u'маркет', MockContract(firm=SubClass(id=cst.FirmId.MARKET)), []),
    (17, u'такси', MockContract(firm=SubClass(id=cst.FirmId.TAXI)), ['taxi', 'taxifee', 'taxicomsn']),
    (17.1, u'такси', MockContract(firm=SubClass(id=cst.FirmId.YA_LOGISTICS)), ['deliverycomsn', 'deliveryfee', 'delivery']),
    (18.1, u'корпоративное такси', MockContract(firm=SubClass(id=cst.FirmId.TAXI)), ['taxi_corp']),
    (18.2, u'корпоративное такси', MockContract(firm=SubClass(id=cst.FirmId.TAXI)), ['taxi_corp_clients']),
    (18.3, u'корпоративное такси', MockContract(firm=SubClass(id=cst.FirmId.YA_LOGISTICS)), ['logistics_clients']),
    (19.1, u'такси Казахстан', MockContract(firm=SubClass(id=cst.FirmId.TAXI_KZ)), []),
    (19.2, u'Убер Казахстан', MockContract(firm=SubClass(id=cst.FirmId.UBER_KZ)), []),
    (19.3, u'такси Казахстан', MockContract(firm=SubClass(id=cst.FirmId.TAXI_AM)), []),
    (20, u'корпоративное такси БелГо корп', MockContract(firm=SubClass(id=cst.FirmId.BELGO_CORP)), []),
    (21, u'корпоративное такси ОсОО «Яндекс.Такси Корп»', MockContract(firm=SubClass(id=cst.FirmId.TAXI_CORP_KG)), []),
]


REJECT_REASONS = [
    (u'non-resident contract', MockContract(
        firm=SubClass(region_id=cst.RegionId.RUSSIA),
        person=SubClass(person_category=SubClass(resident=False))
    )),
    # соответсвует группе COMMITMENT_LETTER
    (u'commitment letter contract', MockContract(commission=ContractTypeId.RU_LETTER_OF_GUARANTEE)),
    (u'agreement on the advertising brand contract', MockContract(commission=ContractTypeId.ADVERTISING_BRAND)),
    (u'letter of attorney contract', MockContract(commission=ContractTypeId.POWER_OF_ATTORNEY)),
    (u'offer contract', MockContract(commission=ContractTypeId.OFFER)),
    (u'Yandex Ukraine contract', MockContract(firm=SubClass(id=cst.FirmId.YANDEX_UA))),
    (u'Yandex Inc contract', MockContract(firm=SubClass(id=cst.FirmId.YANDEX_INC))),
    (u'Yandex Turkey contract', MockContract(firm=SubClass(id=cst.FirmId.YANDEX_TR))),
    (u'Yandex AG contract', MockContract(firm=SubClass(id=cst.FirmId.YANDEX_EU_AG))),
    (u'excepted firm contract', MockContract(firm=SubClass(id=cst.FirmId.KINOPOISK))),
    (u'excepted firm contract', MockContract(firm=SubClass(id=cst.FirmId.TAXI_UA))),
    (u'excepted firm contract', MockContract(firm=SubClass(id=cst.FirmId.AUTORU_EU_AG))),
    (u'excepted firm contract', MockContract(firm=SubClass(id=cst.FirmId.ZEN))),
    (u'excepted firm contract', MockContract(firm=SubClass(id=cst.FirmId.AUTORU))),
    (u'excepted firm contract', MockContract(firm=SubClass(id=cst.FirmId.DRIVE))),
    (u'excepted firm contract', MockContract(firm=SubClass(id=cst.FirmId.PROBKI))),
    (u'excepted firm contract', MockContract(firm=SubClass(id=cst.FirmId.BELFACTA))),
    (u'excepted firm contract', MockContract(firm=SubClass(id=cst.FirmId.UBER_ML_BV))),
    (u'excepted firm contract', MockContract(firm=SubClass(id=cst.FirmId.UBER_ML_BV_BYN))),
    (u'excepted firm contract', MockContract(firm=SubClass(id=cst.FirmId.SERVICES_EU_AG))),
    (u'excepted firm contract', MockContract(firm=SubClass(id=cst.FirmId.YANDEX_EU_NV))),
    (u'excepted firm contract', MockContract(firm=SubClass(id=cst.FirmId.CLOUD_TECHNOLOGIES))),
    (u'excepted firm contract', MockContract(firm=SubClass(id=cst.FirmId.YANDEX_EU_BV))),
    (u'excepted firm contract', MockContract(firm=SubClass(id=cst.FirmId.KAZNET_MEDIA))),
    (u'excepted firm contract', MockContract(firm=SubClass(id=cst.FirmId.UBER_AZERBAYCAN_MMC))),
    (u'excepted firm contract', MockContract(firm=SubClass(id=cst.FirmId.TAXI_EU_BV))),
]


class TestContractNotifierEmail(object):

    def _main(self, answer, func_name, func_args, services):
        services = services or ['']

        for service in services:
            def patched_func(self):
                return {service}

            with mock.patch('balance.contract_notify_email.ContractNS.service_cc_set', property(patched_func)):
                assert answer == getattr(contract_notify_email, func_name)(*func_args)

    @pytest.mark.parametrize('num, contract_group, contract, services',
                             CONTRACTS_GROUP_PARAMS)
    def test_get_ya_contacts_group(self, num, contract_group, contract, services, session):
        contract.session = session
        self._main(
            answer=contract_group,
            func_name='get_ya_contacts_group',
            func_args=[contract, contract.col],
            services=services,
        )


    @pytest.mark.parametrize('reject_reason, contract',
                             REJECT_REASONS)
    def test_reject_reasons(self, reject_reason, contract, session):
        contract.session = session
        self._main(
            answer=reject_reason,
            func_name='get_reject_reason',
            func_args=[contract],
            services=None,
        )

    @pytest.mark.email
    @pytest.mark.parametrize('service, email_list',
                             [(102, ['docs-project@yandex-team.ru']),
                              (151, ['b2b-bus@yandex-team.ru']),
                              (205, ['b2b-bus@yandex-team.ru']),
                              (601, ['b2b-bus@yandex-team.ru']),
                              (602, ['b2b-bus@yandex-team.ru']),
                              (118, ['docs-media@yandex-team.ru', 'dpasik@yandex-team.ru', 'kate-parf@yandex-team.ru']),
                              (126, ['docs-media@yandex-team.ru', 'dpasik@yandex-team.ru', 'kate-parf@yandex-team.ru']),
                              (153, ['mikhail@yandex-team.ru', 'emilab@yandex-team.ru']),
                              (170, ['mikhail@yandex-team.ru', 'emilab@yandex-team.ru']),
                              (270, ['mikhail@yandex-team.ru', 'emilab@yandex-team.ru']),
                              (204, ['mikhail@yandex-team.ru', 'emilab@yandex-team.ru']),
                              (135, ['docs_alarm@yandex-team.ru']),
                              (650, ['docs_alarm@yandex-team.ru']),
                              (718, ['docs_alarm@yandex-team.ru']),
                              (111, ['taxi-netoriginala@yandex-team.ru']),
                              (1161, ['taxi-netoriginala@yandex-team.ru']),
                              (203, ['forleasing@yandex-team.ru']),
                              (98, ['partner-travel@yandex-team.ru']),
                              (143, ['docs-project@yandex-team.ru', 'cloud_ops@yandex-team.ru']),
                              (144, ['docs-project@yandex-team.ru', 'cloud_ops@yandex-team.ru']),
                              (621, ['docs-project@yandex-team.ru']),
                              (611, ['docs-project@yandex-team.ru']),
                              (616, ['docs-project@yandex-team.ru']),
                              ])
    def test_get_recipients(self, service, email_list, session):
        client = ob.ClientBuilder()
        person = ob.PersonBuilder(type="ur", client=client)
        manager = ob.ManagerWithChiefsBuilder().build(session).obj
        dt = datetime.datetime.now() - datetime.timedelta(days=60)
        contract = ob.ContractBuilder(
            manager_code=manager.manager_code, person=person, dt=dt,
            commission=1, client=client
        ).build(session).obj
        contract.col0.services = {service: True}
        session.flush()

        recipient = (manager.safe_email, manager.name)
        res_recipients = contract_notify_email.get_recipients(contract, recipient)[1:]
        required_recipients = [(email, manager.name) for email in email_list]

        assert set(required_recipients) == set(res_recipients)
