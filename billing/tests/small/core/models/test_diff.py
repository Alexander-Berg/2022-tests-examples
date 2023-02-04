from billing.dcsaap.backend.core.models import Run, Check, Diff

from billing.dcsaap.backend.tests.utils.models import create_run, create_diff


class TestDiffs:
    """
    Тестирование логики модели Diffs
    """

    def test_count_diffs_for_run(self, some_check: Check):
        """
        Проверяем, что подсчет расхождений работает верно
        """
        run = create_run(some_check, status=Run.STATUS_FINISHED)
        create_diff(run, 'k1', 'k1_value1', 'column', '1', '2')
        create_diff(run, 'k1', 'k1_value2', 'column', '3', '4')
        assert Diff.count_for_run(run) == 2

        create_diff(run, 'k1', 'k1_value2', 'column', '5', '6', status=Diff.STATUS_CLOSED)
        assert Diff.count_for_run(run) == 2

    def test_count_diffs_stat_for_run(self, some_check: Check):
        """
        Проверяем подсчет статистики расхождений
        Ожидаем: 3 расхождения каждого типа
        """

        run = create_run(some_check, status=Run.STATUS_FINISHED)
        create_diff(run, 'k1', 'k1_value1', 'column1', '1', '2', diff_type=Diff.TYPE_DIFF)
        create_diff(run, 'k1', 'k1_value1', 'column2', '2', '3', diff_type=Diff.TYPE_DIFF)
        needed = [
            {'type': Diff.TYPE_DIFF, 'total': 1, 'type_str': dict(Diff.TYPES)[Diff.TYPE_DIFF]},
            {'type': None, 'total': 1, 'type_str': 'Итого расхождений'},
        ]
        assert Diff.count_stat_for_run(run) == needed

        create_diff(run, 'k1', 'k1_value2', 'column1', '1', None, diff_type=Diff.TYPE_NOT_IN_T2)
        create_diff(run, 'k1', 'k1_value2', 'column2', '3', None, diff_type=Diff.TYPE_NOT_IN_T2)
        create_diff(run, 'k1', 'k1_value3', 'column2', '1', None, diff_type=Diff.TYPE_NOT_IN_T2)
        needed = [
            {
                'type': Diff.TYPE_NOT_IN_T2,
                'total': 2,
                'type_str': dict(Diff.TYPES)[Diff.TYPE_NOT_IN_T2] + f"(##{some_check.table2}##)",
            },
            {'type': Diff.TYPE_DIFF, 'total': 1, 'type_str': dict(Diff.TYPES)[Diff.TYPE_DIFF]},
            {'type': None, 'total': 3, 'type_str': 'Итого расхождений'},
        ]
        assert Diff.count_stat_for_run(run) == needed

        create_diff(run, 'k1', 'k1_value4', 'column1', None, '2', diff_type=Diff.TYPE_NOT_IN_T1)
        create_diff(run, 'k1', 'k1_value5', 'column1', None, '2', diff_type=Diff.TYPE_NOT_IN_T1)
        create_diff(run, 'k1', 'k1_value6', 'column1', None, '2', diff_type=Diff.TYPE_NOT_IN_T1)
        # закрытое расхождение не должно быть учтено в статистике
        create_diff(
            run, 'k1', 'k1_value7', 'column1', None, '2', diff_type=Diff.TYPE_NOT_IN_T1, status=Diff.STATUS_CLOSED
        )
        needed = [
            {
                'type': Diff.TYPE_NOT_IN_T1,
                'total': 3,
                'type_str': dict(Diff.TYPES)[Diff.TYPE_NOT_IN_T1] + f"(##{some_check.table1}##)",
            },
            {
                'type': Diff.TYPE_NOT_IN_T2,
                'total': 2,
                'type_str': dict(Diff.TYPES)[Diff.TYPE_NOT_IN_T2] + f"(##{some_check.table2}##)",
            },
            {'type': Diff.TYPE_DIFF, 'total': 1, 'type_str': dict(Diff.TYPES)[Diff.TYPE_DIFF]},
            {'type': None, 'total': 6, 'type_str': 'Итого расхождений'},
        ]
        assert Diff.count_stat_for_run(run) == needed
