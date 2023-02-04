import pytest
import datetime
from billing.apikeys.apikeys import mapper

from .utils import ReasonFabric, ServiceFabric, UnitFabric


@pytest.fixture
def unit_fabric():
    return UnitFabric()


@pytest.fixture
def reason_fabric():
    return ReasonFabric()


@pytest.fixture
def service_fabric(unit_fabric, reason_fabric):
    return ServiceFabric(unit_fabric, reason_fabric)


@pytest.fixture
def ph_person():
    person = mapper.BalancePerson(id='1', client_id='1', type='ph').save()
    yield person
    person.delete()


@pytest.fixture
def ur_person():
    person = mapper.BalancePerson(id='2', client_id='1', type='ur').save()
    yield person
    person.delete()


@pytest.fixture
def balance_contract_config():
    return mapper.BalanceContractConfig(manager_code=1)


@pytest.fixture
def balance_config(balance_contract_config):
    return mapper.BalanceConfig(contract_config=balance_contract_config)


@pytest.fixture()
def hits_unit(request):
    """:rtype : mapper.Unit"""
    unit = mapper.Unit(id=1, cc='hits').save()
    yield unit
    unit.delete()


@pytest.fixture()
def hits_delayed_unit():
    unit = mapper.Unit(id=2, cc="hits_delayed", type='delayed', max_delay=6 * 60 * 60).save()
    yield unit
    unit.delete()


@pytest.fixture()
def simple_service(request, service_fabric, hits_unit):
    """:rtype : mapper.Service"""
    service = service_fabric(units=['hits'])
    yield service
    service.delete()


@pytest.fixture()
def simple_service_delayed(request, service_fabric, hits_unit, hits_delayed_unit):
    """:rtype : mapper.Service"""
    service = service_fabric(units=['hits', 'hits_delayed'])
    yield service
    service.delete()


@pytest.fixture
def empty_tariff(simple_service):
    """:rtype : mapper.Tariff"""
    tariff = mapper.Tariff(
        cc=simple_service.cc + '_tariff_cc',
        name='Empty contracted tariff',
        service_id=simple_service.id
    ).save()
    yield tariff
    tariff.delete()


@pytest.fixture
def empty_tariff_delayed(simple_service_delayed):
    """:rtype : mapper.Tariff"""
    tariff = mapper.Tariff(
        cc=simple_service_delayed.cc + '_tariff_cc',
        name='Empty contracted tariff',
        service_id=simple_service_delayed.id,
        tariffication_delay=6 * 60 * 60
    ).save()
    yield tariff
    tariff.delete()


@pytest.fixture
def empty_contractless_tariff(simple_service):
    """:rtype : mapper.Tariff"""
    tariff = mapper.Tariff(
        cc=simple_service.cc + '_tariff_cc_contractless',
        name='Empty contractless tariff',
        service_id=simple_service.id,
        contractless=True,
        personal_account=mapper.TariffPersonalAccountSettings(product='42', default_replenishment_amount="1000")
    ).save()
    yield tariff
    tariff.delete()


@pytest.fixture
def empty_contractless_tariff_more_expensive(simple_service):
    """:rtype : mapper.Tariff"""
    tariff = mapper.Tariff(
        cc=simple_service.cc + '_tariff_cc_contractless_more_expensive',
        name='Empty contractless tariff',
        service_id=simple_service.id,
        contractless=True,
        personal_account=mapper.TariffPersonalAccountSettings(product='42', default_replenishment_amount="2000")
    ).save()
    yield tariff
    tariff.delete()


@pytest.fixture
def empty_contractless_tariff_delayed(simple_service_delayed):
    """:rtype : mapper.Tariff"""
    tariff = mapper.Tariff(
        cc=simple_service_delayed.cc + '_tariff_cc_contractless',
        name='Empty contractless tariff',
        service_id=simple_service_delayed.id,
        contractless=True,
        tariffication_delay=6*60*60,
    ).save()
    yield tariff
    tariff.delete()


@pytest.fixture
def empty_contractless_accessable_tariff(simple_service):
    """:rtype : mapper.Tariff"""
    tariff = mapper.Tariff(
        cc=simple_service.cc + 'accessable_tariff_cc_contractless',
        name='Empty contractless tariff',
        service_id=simple_service.id,
        contractless=True,
        client_access=True,
    ).save()
    yield tariff
    tariff.delete()


@pytest.fixture
def empty_contractless_accessable_tariff_delayed(simple_service_delayed):
    """:rtype : mapper.Tariff"""
    tariff = mapper.Tariff(
        cc=simple_service_delayed.cc + 'accessable_tariff_cc_contractless',
        name='Empty contractless tariff',
        service_id=simple_service_delayed.id,
        contractless=True,
        client_access=True,
        tariffication_delay=6*60*60,
    ).save()
    yield tariff
    tariff.delete()


@pytest.fixture
def empty_tariffless_tariff(simple_service):
    """:rtype : mapper.Tariff"""
    tariff = mapper.Tariff(
        cc=simple_service.cc + '_tariff_cc_tariffless',
        name='Empty tariff for tariffless contract',
        service_id=simple_service.id,
        flow_type=mapper.ContractFlowTypes.tariffless,
        client_access=True,
    ).save()
    yield tariff
    tariff.delete()


@pytest.fixture()
def user(request):
    """:rtype : mapper.User"""
    user = mapper.User(uid=1, balance_client_id=1).save()
    yield user
    user.delete()


@pytest.fixture()
def manager_permission_set(request):
    service_read_permission = mapper.Permission(id="service_access-read", name="service_access", action="read").save()
    service_write_permission = mapper.Permission(id="service_access-write", name="service_access", action="write").save()
    service_perform_permission = mapper.Permission(id="service_access-perform", name="service_access", action="perform").save()
    perm_set = mapper.PermissionSet(name='manager_perm_set',
                                    permissions=[
                                        service_read_permission.pk,
                                        service_write_permission.pk,
                                        service_perform_permission.pk
                                    ]).save()
    yield perm_set
    perm_set.delete()
    service_read_permission.delete()
    service_write_permission.delete()
    service_perform_permission.delete()


@pytest.fixture()
def user_manager(request, simple_service, manager_permission_set):
    """:rtype : mapper.User"""
    role = mapper.Role(id='manager_of_simple_service', perm_sets=[manager_permission_set.pk], constraints={
        'service_access': {
            'read': {'service': [simple_service.id]},
            'write': {'service': [simple_service.id]},
            'perform': {'service': [simple_service.id]},
        }
    }).save()
    manager = mapper.User(uid=1120000000000001, roles=[role.pk]).save()
    yield manager
    manager.delete()
    role.delete()


@pytest.fixture()
def user_support_ro(request, manager_permission_set):
    """:rtype : mapper.User"""
    role = mapper.Role(id='manager_of_simple_service', perm_sets=[manager_permission_set.pk], constraints={
        'service_access': {
            'read': {'service': '*'},
            'write': None,
            'perform': None,
        }
    }).save()
    manager = mapper.User(uid=1120000000000002, roles=[role.pk]).save()
    yield manager
    manager.delete()
    role.delete()


@pytest.fixture()
def user_admin(request, manager_permission_set):
    """:rtype : mapper.User"""
    role = mapper.Role(id='manager_of_simple_service', perm_sets=[manager_permission_set.pk], constraints={
        'service_access': {
            'read': {'service': '*'},
            'write': {'service': '*'},
            'perform': {'service': '*'},
        }
    }).save()
    manager = mapper.User(uid=1120000000000003, roles=[role.pk]).save()
    yield manager
    manager.delete()
    role.delete()


@pytest.fixture()
def project(request, user):
    """:rtype : mapper.Project"""
    project = mapper.Project.create(user)
    yield project
    project.delete()


@pytest.fixture()
def simple_link(request, project, simple_service):
    """:rtype : mapper.ProjectServiceLink"""
    link = project.attach_to_service(simple_service)
    yield link
    link.delete()


@pytest.fixture()
def simple_link_delayed(request, project, simple_service_delayed):
    """:rtype : mapper.ProjectServiceLink"""
    link = project.attach_to_service(simple_service_delayed)
    yield link
    link.delete()


@pytest.fixture()
def link_with_fake_tariff(request, project, simple_service, empty_tariff):
    """:rtype : mapper.ProjectServiceLink"""
    link = mapper.ProjectServiceLink.create(project, simple_service)
    link.config.tariff = empty_tariff.cc
    link.save()
    yield link
    link.delete()


@pytest.fixture()
def link_with_fake_tariff_delayed(request, project, simple_service_delayed, empty_tariff_delayed):
    """:rtype : mapper.ProjectServiceLink"""
    link = mapper.ProjectServiceLink.create(project, simple_service_delayed)
    link.config.tariff = empty_tariff_delayed.cc
    link.save()
    yield link
    link.delete()


@pytest.fixture()
def link_with_fake_contractless_tariff(request, project, simple_service, empty_contractless_tariff):
    """:rtype : mapper.ProjectServiceLink"""
    link = mapper.ProjectServiceLink.create(project, simple_service)
    link.config.tariff = empty_contractless_tariff.cc
    link.save()
    yield link
    link.delete()


@pytest.fixture()
def order_for_link_with_empty_contractless_tariff(empty_contractless_tariff, link_with_fake_contractless_tariff):
    order = mapper.BalanceOrder(
        id=1,
        product_id='42',
        project_id=link_with_fake_contractless_tariff.project_id,
        tariff=empty_contractless_tariff.cc
    ).save()
    yield order
    order.delete()


@pytest.fixture()
def order_for_link_with_empty_contractless_tariff_more_expensive(empty_contractless_tariff_more_expensive,
                                                                 link_with_fake_contractless_tariff):
    order = mapper.BalanceOrder(
        id=2,
        product_id='42',
        project_id=link_with_fake_contractless_tariff.project_id,
        tariff=empty_contractless_tariff_more_expensive.cc
    ).save()
    yield order
    order.delete()


@pytest.fixture()
def link_with_fake_contractless_tariff_delayed(request, project, simple_service_delayed, empty_contractless_tariff_delayed):
    """:rtype : mapper.ProjectServiceLink"""
    link = mapper.ProjectServiceLink.create(project, simple_service_delayed)
    link.config.tariff = empty_contractless_tariff_delayed.cc
    link.save()
    yield link
    link.delete()


@pytest.fixture()
def link_without_any_tariff(request, project, simple_service):
    """:rtype : mapper.ProjectServiceLink"""
    link = mapper.ProjectServiceLink.create(project, simple_service)
    link.config.tariff = None
    link.save()
    yield link
    link.delete()


@pytest.fixture()
def link_with_fake_tariffless_tariff(request, project, simple_service, empty_tariffless_tariff):
    """:rtype : mapper.ProjectServiceLink"""
    link = mapper.ProjectServiceLink.create(project, simple_service)
    link.config.tariff = empty_tariffless_tariff.cc
    link.balance_contract_id = '6'
    link.save()
    yield link
    link.delete()


@pytest.fixture()
def simple_key(request, simple_link):
    """:rtype : mapper.KeyServiceConfig"""
    key = mapper.Key.create(simple_link.project)
    ksc = key.attach_to_service(simple_link.service)
    yield ksc
    mapper.KeyServiceCounter.objects.filter(key=key.id, service_id=simple_link.service_id).delete()
    ksc.delete()
    key.delete()


@pytest.fixture()
def simple_key_delayed(request, simple_link_delayed):
    """:rtype : mapper.KeyServiceConfig"""
    key = mapper.Key.create(simple_link_delayed.project)
    ksc = key.attach_to_service(simple_link_delayed.service)
    yield ksc
    mapper.KeyServiceCounter.objects.filter(key=key.id, service_id=simple_link_delayed.service_id).delete()
    ksc.delete()
    key.delete()


@pytest.fixture()
def simple_keys_pair(request, simple_link):
    """:rtype : mapper.KeyServiceConfig"""
    key_old = mapper.Key.create(simple_link.project)
    key_old.name = 'Older key'
    key_old.dt -= datetime.timedelta(minutes=10)
    key_old.save()
    key_new = mapper.Key.create(simple_link.project)
    key_new.name = 'Newer key'
    key_new.save()
    ksc_old = key_old.attach_to_service(simple_link.service)
    ksc_new = key_new.attach_to_service(simple_link.service)
    yield ksc_old, ksc_new
    mapper.KeyServiceCounter.objects.filter(key=key_old.id, service_id=simple_link.service_id).delete()
    mapper.KeyServiceCounter.objects.filter(key=key_new.id, service_id=simple_link.service_id).delete()
    ksc_old.delete()
    ksc_new.delete()
    key_old.delete()
    key_new.delete()


def upgradable_contractless_tariff_factory(simple_service, subs_cost):
    return mapper.Tariff(
        cc=simple_service.cc + '_upgradable_' + subs_cost + '_tariff_cc_contractless',
        name='Empty contractless tariff',
        service_id=simple_service.id,
        contractless=True,
        client_access=True,
        personal_account=mapper.TariffPersonalAccountSettings(product='777',
                                                              default_replenishment_amount=str(subs_cost)),
        tarifficator_config=[
            {"unit": "UnconditionalActivatorUnit", "params": {}},
            {"unit": "PrepayPeriodicallyDiscountedUnit", "params": {
                "period_mask": "0 0 x x * *", "truncate_period_mask": "0 0 * * * *", "time_zone": "+0300",
                "product_id": "777", "product_value": subs_cost, "autocharge_personal_account": True,
                "ban_reason": 110, "unban_reason": 111, "scope": "yearprepay_contractless"
            }},
        ]
    )


@pytest.fixture
def tariff_to_upgrade_from(simple_service):
    tariff = upgradable_contractless_tariff_factory(simple_service, '1000000').save()
    yield tariff
    tariff.delete()


@pytest.fixture
def tariff_to_upgrade_to(simple_service):
    tariff = upgradable_contractless_tariff_factory(simple_service, '2000000').save()
    yield tariff
    tariff.delete()


@pytest.fixture()
def link_upgradable_with_discount(request, project, simple_service, tariff_to_upgrade_from):
    link = mapper.ProjectServiceLink.create(project, simple_service)
    link.config.tariff = tariff_to_upgrade_from.cc
    link.save()
    yield link
    mapper.TarifficatorState.drop_for_link(link)
    link.delete()


@pytest.fixture()
def link_with_most_expensive_tariff(request, project, simple_service, tariff_to_upgrade_to):
    link = mapper.ProjectServiceLink.create(project, simple_service)
    link.config.tariff = tariff_to_upgrade_to.cc
    link.save()
    yield link
    mapper.TarifficatorState.drop_for_link(link)
    link.delete()


@pytest.fixture()
def order_for_link_upgradable_with_discount_with_tariff_tier_1(
    tariff_to_upgrade_from: mapper.Tariff,
    link_upgradable_with_discount: mapper.ProjectServiceLink,
):
    order = mapper.BalanceOrder(
        id=2,
        product_id=tariff_to_upgrade_from.personal_account.product,
        project_id=link_upgradable_with_discount.project_id,
        tariff=tariff_to_upgrade_from.cc
    ).save()
    yield order
    order.delete()


@pytest.fixture()
def order_for_link_upgradable_with_discount_with_tariff_tier_2(
    tariff_to_upgrade_to: mapper.Tariff,
    link_upgradable_with_discount: mapper.ProjectServiceLink,
):
    order = mapper.BalanceOrder(
        id=3,
        product_id=tariff_to_upgrade_to.personal_account.product,
        project_id=link_upgradable_with_discount.project_id,
        tariff=tariff_to_upgrade_to.cc
    ).save()
    yield order
    order.delete()
