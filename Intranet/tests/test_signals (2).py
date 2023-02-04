from mock.mock import Mock

from kelvin.result_stats.signals import recalculate_stat_by_courselessonresult
from kelvin.result_stats.tasks import recalculate_courselessonstat


def test_recalculate_stat_by_courselessonresult(mocker):
    """
    Тест запуска пересчета статистики по курсозанятию
    """
    mocked_delay = mocker.patch.object(recalculate_courselessonstat, 'delay')
    mocked_result_instance = Mock(clesson_id=234)
    recalculate_stat_by_courselessonresult(Mock(), mocked_result_instance)
    assert mocked_delay.called_once_with(234), (
        u'Неправильный вызов celery-задачи')
