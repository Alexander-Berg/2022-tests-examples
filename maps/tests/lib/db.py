import logging
import time
import psycopg2 as pg
import yatest
import os

logger = logging.getLogger("db")

CONNECTION_STRING = "host=127.0.0.1 port=9001 dbname=test user=test password=test sslmode=disable"

IS_INITIALIZED = False


def initialize():
    global IS_INITIALIZED

    if IS_INITIALIZED:
        logging.info("DB is already initialized. Skip.")
        return

    start_time = time.time()
    while time.time() - start_time < 60:
        try:
            with pg.connect(CONNECTION_STRING):
                pass
            break
        except:
            time.sleep(2)

    with pg.connect(CONNECTION_STRING) as conn:
        cur = conn.cursor()

        cur.execute('CREATE EXTENSION IF NOT EXISTS "pgcrypto"')
        cur.execute('CREATE EXTENSION IF NOT EXISTS "uuid-ossp"')

        sql_file = os.path.join(yatest.common.source_path(), "maps/automotive/remote_access/docs/db.sql")
        with open(sql_file) as sql:
            response = cur.execute(sql.read())
            logger.info(str(response))

        cur.execute("""
            CREATE OR REPLACE FUNCTION truncate_all()
                RETURNS void AS $$
            DECLARE curs CURSOR FOR SELECT tablename as name FROM pg_tables WHERE schemaname = 'public';
            BEGIN
                FOR tbl IN curs LOOP
                    EXECUTE 'TRUNCATE TABLE ' || tbl.name || ' CASCADE';
                END LOOP;
            END
            $$ LANGUAGE plpgsql;
        """)

        conn.commit()

    IS_INITIALIZED = True


def reset():
    with pg.connect(CONNECTION_STRING) as conn:
        conn.set_isolation_level(0)

        cur = conn.cursor()
        cur.execute("SELECT truncate_all()")
        conn.commit()


def query(query_body):
    logger.debug(query_body)
    with pg.connect(CONNECTION_STRING) as conn:
        with conn.cursor() as cursor:
            cursor.execute(query_body)
            response = cursor.fetchall()
            logger.debug(response)
            return response
