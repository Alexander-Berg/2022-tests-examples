from maps.b2bgeo.ya_courier.backend.test_lib.util import get_unistat, clean_unistat_signals


def test_pg_stat(system_env_with_db):
    j = get_unistat(system_env_with_db)

    signals = clean_unistat_signals(j)

    assert "db_deadlocks_count_dxxm" in signals
    assert "db_temp_files_count_dxxm" in signals
