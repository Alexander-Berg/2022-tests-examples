from unittest import mock
import datetime
import pytz
from billing.apikeys.apikeys.mapper import keys, roles, limits
from ..utils import (
    mock_datetime,
)


def test_project_service_link_export_custom_fields_values(mongomock):
    service = create_service()
    project = create_project()
    link = create_link(project, service)
    link.custom_params.rps = 600
    link.save()
    key_config_0 = create_key(link)
    key_config_0.custom_params.rps = 1000
    key_config_0.save()
    key_config_1 = create_key(link)
    key_config_1.custom_params.ref = 'qwerty'
    key_config_1.save()

    export_link = keys.ProjectServiceLinkExport.copy_from(link)
    assert export_link.custom_params['rps'] == 600
    assert export_link.keys[key_config_0.key].custom_params['rps'] == 1000
    assert export_link.keys[key_config_0.key].custom_params['ref'] is None
    assert export_link.keys[key_config_1.key].custom_params['rps'] == 500
    assert export_link.keys[key_config_1.key].custom_params['ref'] == 'qwerty'


def test_project_service_link_export_custom_fields_update_dt(mongomock):
    service = create_service()
    project = create_project()
    link = create_link(project, service)

    export_link = keys.ProjectServiceLinkExport.copy_from(link)
    export_link.reload()  # MongoMock теперь отрезает микросекунды, прям как Mongo
    init_update_dt = export_link.update_dt

    link.custom_params.rps = 500
    link.save()
    export_link = keys.ProjectServiceLinkExport.copy_from(link)
    not_update_dt = export_link.update_dt

    link.custom_params.rps = 5000
    link.save()
    export_link = keys.ProjectServiceLinkExport.copy_from(link)
    true_update_dt = export_link.update_dt

    assert init_update_dt == not_update_dt
    assert init_update_dt < true_update_dt


def test_project_service_link_copy_from_update_mode(mongomock):
    # проверка корректности обновления вложенных полей в методе copy_from через update
    service = create_service()
    project = create_project()
    link = create_link(project, service)

    export_link = keys.ProjectServiceLinkExport.copy_from(link)

    link.custom_params.rps = 600
    link.save()
    key_config_0 = create_key(link)
    key_config_0.custom_params.rps = 1000
    key_config_0.save()
    key_config_1 = create_key(link)
    key_config_1.custom_params.ref = 'qwerty'
    key_config_1.save()

    export_link = keys.ProjectServiceLinkExport.copy_from(link, update_mode=True)
    assert export_link.custom_params['rps'] == 600
    assert export_link.keys[key_config_0.key].custom_params['rps'] == 1000
    assert export_link.keys[key_config_0.key].custom_params['ref'] is None
    assert export_link.keys[key_config_1.key].custom_params['rps'] == 500
    assert export_link.keys[key_config_1.key].custom_params['ref'] == 'qwerty'

    key_config_0.custom_params.rps = 1010
    key_config_0.save()
    export_link = keys.ProjectServiceLinkExport.copy_from(link, update_mode=True)
    assert export_link.keys[key_config_0.key].custom_params['rps'] == 1010


def test_project_service_link_export_limits_values(mongomock):
    service = create_service()
    project = create_project()
    link = create_link(project, service)
    create_key(link)
    for counter in link.get_counters():
        keys.HourlyStat(dt=datetime.datetime(2021, 1, 1, 12, tzinfo=pytz.utc), counter_id=counter.id, value=1).save()
    limit_checker = limits.ProjectLinkLimitChecker.getone(link_id=link.id)
    limit_checker.last_check = datetime.datetime(2021, 1, 1, 20, tzinfo=pytz.utc)
    limit_checker.save()
    with mock.patch('billing.apikeys.apikeys.mapper.keys.datetime', new=mock_datetime(datetime.datetime(2020, 1, 1, 20, tzinfo=pytz.UTC))):
        export_link = keys.ProjectServiceLinkExport.copy_from(link)
    assert export_link.limits == {
        'test_hits': keys.ProjectLimitStatusExport(limit=10, value=1, reset_dt=datetime.datetime(2021, 1, 1, 21, tzinfo=pytz.utc))
    }

    for counter in link.get_counters():
        keys.HourlyStat(dt=datetime.datetime(2021, 1, 1, 13, tzinfo=pytz.utc), counter_id=counter.id, value=1).save()

    with mock.patch('billing.apikeys.apikeys.mapper.keys.datetime', new=mock_datetime(datetime.datetime(2020, 1, 1, 20, tzinfo=pytz.UTC))):
        export_link = keys.ProjectServiceLinkExport.copy_from(link)
    assert export_link.limits == {
        'test_hits': keys.ProjectLimitStatusExport(limit=10, value=2, reset_dt=datetime.datetime(2021, 1, 1, 21, tzinfo=pytz.utc))
    }


def create_service():
    unit = keys.Unit(id=1, cc='hits')
    unit.save()
    service = keys.Service(
        id=1,
        cc='test',
        token='test_token',
        name='Test',
        units=[unit.cc],
        lock_reasons=[1],
        unlock_reasons=[2],
        default_link_config=keys.LinkConfig(),
        link_custom_fields=[keys.CustomFieldConfig(name='rps', _type='int', value=500)],
        key_custom_fields=[
            keys.CustomFieldConfig(name='rps', _type='int', value=500),
            keys.CustomFieldConfig(name='ref', _type='str', value=None),
        ],
    )
    limits.LimitConfig(
        name=f'{service.cc}_{unit.cc}',
        service_id=service.id,
        unit_id=unit.id,
        cron_string='0 0 * * * *',
        cron_timezone='+0300',
        limit=10,
        min_checks_for_period=42,
    ).save()
    return service.save()


def create_project():
    user = roles.User(uid=1, login='vasya')
    user.save()
    return user.get_default_project()


def create_link(project, service):
    return project.attach_to_service(service)


def create_key(link):
    key = keys.Key.create(link.project)
    return key.attach_to_service(link.service)
