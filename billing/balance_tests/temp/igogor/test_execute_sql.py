# coding: utf-8
from balance import balance_db
from btestlib.environments import TrustDbNames


def test_enable_export_xg():
    xg_db = balance_db.BalanceBS(dbname=TrustDbNames.BS_XG)
    res = xg_db.execute("update t_config set value_num = 0 where item='BATCH_EXPORT_ACTIVE'")

    res = xg_db.execute("select * from t_config where item='BATCH_EXPORT_ACTIVE'")
    # "update t_config set value_num = 0 where item='BATCH_EXPORT_ACTIVE'")

    pass


def test_enable_expoert_pg():
    pg_db = balance_db.BalanceBS(dbname=TrustDbNames.BS_PG)
    res = pg_db.execute("update t_config set value_num = 0 where item='BATCH_EXPORT_ACTIVE'")

    res = pg_db.execute("select * from t_config where item='BATCH_EXPORT_ACTIVE'")

    pass


def test_get_data():
    xg_db = balance_db.BalanceBS(dbname=TrustDbNames.BS_XG)
    # res = xg_db.execute("select * from t_export_pack")
    #
    # res = xg_db.execute("select * from t_export where classname = 'BalancePack' order by EXPORT_DT")

    res = xg_db.execute("select * from t_payment where trust_payment_id = '5bc454bf910d3925f79b212e'")
    pass
