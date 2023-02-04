from datetime import datetime
import pytz
import pytest
from billing.apikeys.apikeys import mapper


@pytest.fixture()
def hits_units(request):
    """:rtype : mapper.Unit"""
    unit1 = mapper.Unit(id=1, cc='hits1').save()
    unit2 = mapper.Unit(id=2, cc='hits2').save()
    yield unit1, unit2
    unit1.delete()
    unit2.delete()


@pytest.fixture()
def service_with_two_units(request, service_fabric, hits_units) -> mapper.Service:
    service = service_fabric(units=['hits1', 'hits2'])
    yield service
    service.delete()


@pytest.fixture()
def link_with_two_units(request,
                        project: mapper.Project,
                        service_with_two_units) -> mapper.ProjectServiceLink:
    link = project.attach_to_service(service_with_two_units)
    yield link
    link.delete()


@pytest.fixture()
def link_with_two_keys(request, link_with_two_units) -> mapper.ProjectServiceLink:
    key1 = mapper.Key.create(link_with_two_units.project)
    ksc1 = key1.attach_to_service(link_with_two_units.service)
    key2 = mapper.Key.create(link_with_two_units.project)
    ksc2 = key2.attach_to_service(link_with_two_units.service)
    for i, counter in enumerate(mapper.KeyServiceCounter.objects.all()):
        counter.counter_id = i
        counter.save()
    yield link_with_two_units
    mapper.KeyServiceCounter.objects.filter(key=key1.id, service_id=link_with_two_units.service_id).delete()
    mapper.KeyServiceCounter.objects.filter(key=key2.id, service_id=link_with_two_units.service_id).delete()
    ksc1.delete()
    key1.delete()
    ksc2.delete()
    key2.delete()


def test_project_service_link_statistics_calculated_correctly(mongomock, link_with_two_keys: mapper.ProjectServiceLink):
    try:
        counters = list(sorted(link_with_two_keys.get_counters(), key=lambda c: (c.key, c.unit_id)))
        for i, counter in enumerate(counters):
            mapper.HourlyStatArchived(dt=datetime(2021, 1, 1, 23, tzinfo=pytz.utc),
                                      counter_id=counter.counter_id, value=50).save()
            mapper.HourlyStatArchived(dt=datetime(2021, 1, 1, 19, tzinfo=pytz.utc),
                                      counter_id=counter.counter_id, value=500).save()
            mapper.HourlyStat(dt=datetime(2021, 1, 1, 20, tzinfo=pytz.utc), counter_id=counter.id, value=1*i).save()
            mapper.HourlyStat(dt=datetime(2021, 1, 1, 21, tzinfo=pytz.utc), counter_id=counter.id, value=2*i).save()
            mapper.HourlyStat(dt=datetime(2021, 1, 1, 22, tzinfo=pytz.utc), counter_id=counter.id, value=3*i).save()
            mapper.HourlyStat(dt=datetime(2021, 1, 2, tzinfo=pytz.utc), counter_id=counter.id, value=4*i).save()

        stat = link_with_two_keys.get_statistic(datetime(2021, 1, 1, 21, tzinfo=pytz.utc))
        assert stat == {'hits1': 118, 'hits2': 136}

        stat = link_with_two_keys.get_statistic(datetime(2021, 1, 1, 21, tzinfo=pytz.utc),
                                                datetime(2021, 1, 2, tzinfo=pytz.utc))
        assert stat == {'hits1': 110, 'hits2': 120}

        for i, counter in enumerate(counters):
            mapper.HourlyStat(dt=datetime(2021, 1, 2, 1, tzinfo=pytz.utc), counter_id=counter.id, value=i).save()
        stat = link_with_two_keys.get_statistic(datetime(2021, 1, 1, 21, tzinfo=pytz.utc))
        assert stat == {'hits1': 120, 'hits2': 140}
    finally:
        mapper.HourlyStat.objects.all().delete()
        mapper.HourlyStatArchived.objects.all().delete()
