from unittest import mock
from datetime import datetime, timedelta
import pytz
from billing.apikeys.apikeys import mapper
from billing.apikeys.apikeys.balance2apikeys import Balance2Apikeys
from ..utils import (
    mock_datetime,
)


# TODO либо в оригинальном методе убрать isinstance(date, datetime), который не переваривает mock объекты
def tarifficator_state_save_mock(self, force_insert=False, validate=True, clean=True,
                                 write_concern=None, cascade=None, cascade_kwargs=None,
                                 _refs=None, **kwargs):
    return super(mapper.TarifficatorState, self).save(force_insert, validate, clean, write_concern, cascade,
                                                      cascade_kwargs, _refs, **kwargs)


def tarifficator_state_delete_mock(self, **write_concern):
    return super(mapper.TarifficatorState, self).delete(**write_concern)


def test_tariffication_will_run_with_delay(mongomock, link_with_fake_tariff, link_with_fake_tariff_delayed):
    """
    Между 21 вечера и 3 утра тест проходить не будет, задание без задержки будет запланировано уже на следующий день
    Из-за чего delay_value по факту станет отрицательным
    """
    with mock.patch('billing.apikeys.apikeys.mapper.task.datetime',
                    new=mock_datetime(datetime(2020, 1, 1, 13, 5, 45, tzinfo=pytz.UTC))):
        tarifficator_task = mapper.TarifficatorTask(link=link_with_fake_tariff)
        tarifficator_task_delayed = mapper.TarifficatorTask(link=link_with_fake_tariff_delayed)
        delay_value = tarifficator_task_delayed.get_next_run_time() - tarifficator_task.get_next_run_time()

        assert delay_value == timedelta(seconds=6*60*60)
        """
E   assert datetime.timedelta(days=-1, seconds=21600) == datetime.timedelta(seconds=21600)
E    +  where datetime.timedelta(seconds=21600) = timedelta(seconds=((6 * 60) * 60))
        """


def test_tariff_changing_scheduling_unafected(mongomock,
                                              link_with_fake_contractless_tariff_delayed,
                                              empty_contractless_accessable_tariff_delayed):
    with mock.patch('billing.apikeys.apikeys.mapper.keys.datetime',
                    new=mock_datetime(datetime(2020, 1, 1, 13, 5, 45, tzinfo=pytz.UTC))):
        link_with_fake_contractless_tariff_delayed.schedule_attachable_tariff_changing(
            empty_contractless_accessable_tariff_delayed.cc,
        )
        expected_scheduled_tariff_date = datetime(2020, 1, 1, 21, 0, 0, tzinfo=pytz.UTC)
        assert \
            link_with_fake_contractless_tariff_delayed.config.scheduled_tariff_date \
            == expected_scheduled_tariff_date, \
            'scheduled_tariff_date should be the same whenever tariff is delayed or not'


def test_tariff_shutdown_scheduling_unafected(mongomock, link_with_fake_contractless_tariff_delayed):
    link_with_fake_contractless_tariff_delayed.schedule_shutdown_attachable_tariff(
        date=datetime(2020, 1, 1, 13, 5, 45, tzinfo=pytz.UTC)
    )
    expected_scheduled_tariff_date = datetime(2020, 1, 1, 21, 0, 0, tzinfo=pytz.UTC)
    assert \
        link_with_fake_contractless_tariff_delayed.config.scheduled_tariff_date \
        == expected_scheduled_tariff_date, \
        'scheduled_tariff_date should be the same whenever tariff is delayed or not'


def test_tarifficator_must_not_change_tariff_before_delay_passed(mongomock,
                                                                 link_with_fake_contractless_tariff_delayed,
                                                                 empty_contractless_tariff_delayed,
                                                                 empty_contractless_accessable_tariff_delayed):
    with mock.patch('billing.apikeys.apikeys.mapper.keys.datetime',
                    new=mock_datetime(datetime(2020, 1, 1, 13, 5, 45, tzinfo=pytz.UTC))):
        link_with_fake_contractless_tariff_delayed.schedule_attachable_tariff_changing(
            empty_contractless_accessable_tariff_delayed.cc
        )
    task = mapper.TarifficatorTask.getone(link=link_with_fake_contractless_tariff_delayed)
    with mock.patch('billing.apikeys.apikeys.mapper.contractor.datetime',
                    new=mock_datetime(datetime(2020, 1, 1, 21, 30, tzinfo=pytz.UTC))), \
            mock.patch('billing.apikeys.apikeys.mapper.task.datetime',
                       new=mock_datetime(datetime(2020, 1, 1, 21, 30, tzinfo=pytz.UTC))), \
            mock.patch('billing.apikeys.apikeys.mapper.schedule_tariff.datetime',
                       new=mock_datetime(datetime(2020, 1, 1, 21, 30, tzinfo=pytz.UTC))), \
            mock.patch('billing.apikeys.apikeys.mapper.contractor.TarifficatorState.save',
                       new=tarifficator_state_save_mock), \
            mock.patch('billing.apikeys.apikeys.mapper.contractor.TarifficatorState.delete',
                       new=tarifficator_state_delete_mock), \
            mock.patch.object(Balance2Apikeys, 'get_personal_account', lambda * args: None), \
            mock.patch.object(mapper.User, 'get_client', lambda x: {'NAME': 'Fake'}):
        task.do_task()
    assert link_with_fake_contractless_tariff_delayed.config.tariff == empty_contractless_tariff_delayed.cc
    assert link_with_fake_contractless_tariff_delayed.config.scheduled_tariff is not None
    assert link_with_fake_contractless_tariff_delayed.config.scheduled_tariff_date is not None
    assert task.dt == datetime(2020, 1, 2, 3, tzinfo=pytz.UTC)
