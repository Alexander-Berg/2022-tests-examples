from dwh.core.models import Interruption, Work


def test_basic(init_work, check_work_stop, django_assert_num_queries):

    target_interruped = 'dummyhost1'

    work_1 = init_work({'meta': {'task_name': 'echo'}, 'params': {'a': 7}})
    work_1.target = target_interruped
    work_2 = init_work({'meta': {'task_name': 'echo'}, 'params': {'a': 8}})
    work_2.target = target_interruped
    work_3 = init_work({'meta': {'task_name': 'echo'}, 'params': {'a': 9}})
    work_3.target = 'one.two.three'
    work_4 = init_work({'meta': {'task_name': 'echo'}, 'params': {'a': 9}})
    work_4.target = 'one.two.three'  # для проверки незатронутости в статусе new
    work_4.save()

    for work in (work_1, work_2, work_3):
        assert not work.dt_stop
        assert work.status == work.Status.NEW
        work.status = work.Status.STARTED
        work.save()

    interruption1 = Interruption.objects.create(target=target_interruped)
    assert f'{interruption1}' == target_interruped
    interruption2 = Interruption.objects.create(target='one.two.three')

    def check_still_working(*, expect_count: int, stop_work: Work = None, expect_queries: int):

        if stop_work:
            stop_work.refresh_from_db()
            assert stop_work.dt_stop  # дата остановки для прерывания выставлена
            check_work_stop(stop_work)

        with django_assert_num_queries(expect_queries) as _:
            still_working_count = Work.interruption_schedule(target=target_interruped)
            assert still_working_count == expect_count

    check_still_working(expect_count=2, expect_queries=7)
    check_still_working(expect_count=1, stop_work=work_1, expect_queries=6)
    check_still_working(expect_count=0, stop_work=work_2, expect_queries=6)

    # проверяем наличие прерывания
    interruption1.refresh_from_db()
    assert interruption1.dt_set
    assert not interruption1.dt_drop
    assert interruption1.active

    interruption2.refresh_from_db()
    assert not interruption2.active

    # проверяем флаги прерывания на заданиях
    for work in (work_1, work_2):
        work.refresh_from_db()
        assert not work.dt_stop  # после остановки дата сброшена
        assert work.is_interrupted

    for work in (work_3, work_4):
        work.refresh_from_db()
        assert not work.dt_stop

    # снимаем прерывания
    with django_assert_num_queries(3) as _:
        count_dropped = Work.interruption_drop(target=target_interruped)
        assert count_dropped == 2

    for work in (work_1, work_2):
        work.refresh_from_db()
        assert not work.dt_stop
        assert work.status == work.Status.NEW

    work_3.refresh_from_db()
    assert work_3.is_started  # по-прежнему выполняется на другом хосте
