import pytest

from ok.approvements.models import ApprovementHistory
from ok.approvements.yt.tables import YTApprovementHistoryTable
from tests import factories as f


pytestmark = pytest.mark.django_db


def test_approvement_history_queries_count(django_assert_num_queries):
    """
    Проверяет кол-во запросов при записи истории в YT
    """
    approvements = f.ApprovementFactory.create_batch(5)
    for approvement in approvements:
        f.ApprovementHistoryFactory(content_object=approvement)

    stages = f.ApprovementStageFactory.create_batch(5)
    for stage in stages:
        f.ApprovementHistoryFactory(content_object=stage)

    # Ожидается 5 запросов:
    # 1. Получение истории
    # 2. Префетч согласований
    # 3. Префетч стадий
    # 4. Префетч согласований для каждой стадии
    # 5. Получение согласующих для всех согласований
    with django_assert_num_queries(5):
        history_entries = ApprovementHistory.objects.all()
        table = YTApprovementHistoryTable()
        table.write(history_entries)
