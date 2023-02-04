from balance import balance_db as db


def test_unterminate():
    db.balance().execute('''UPDATE (SELECT *
            FROM BO.t_pycron_descr
            WHERE name LIKE 'oebs-processor')
    SET terminate = 0''')
