import contextlib
import logging
import time
import psycopg2 as pg
import yatest
import os

logger = logging.getLogger("db")

CONNECTION_STRING = "host=127.0.0.1 port=9001 dbname=test user=test password=test sslmode=disable"

IS_INITIALIZED = False


@contextlib.contextmanager
def get_connection():
    conn = pg.connect(CONNECTION_STRING)
    try:
        with conn:
            yield conn
    finally:
        conn.close()


def initialize(sql_paths, extensions=[]):
    global IS_INITIALIZED

    if IS_INITIALIZED:
        logging.info("DB is already initialized. Skip.")
        return

    if not isinstance(sql_paths, list):
        sql_paths = [sql_paths]

    start_time = time.time()
    while time.time() - start_time < 60:
        try:
            with get_connection():
                pass
            break
        except:
            time.sleep(2)

    with get_connection() as conn:
        cur = conn.cursor()

        for extension in extensions:
            cur.execute(f'CREATE EXTENSION IF NOT EXISTS "{extension}"')

        for path in sql_paths:
            sql_file = os.path.join(yatest.common.source_path(), path)
            with open(sql_file) as sql:
                response = cur.execute(sql.read())
                logger.info(str(response))

        cur.execute("""
            CREATE OR REPLACE FUNCTION truncate_all()
                RETURNS void AS $$
            DECLARE curs CURSOR FOR SELECT tablename as name FROM pg_tables WHERE schemaname = 'public';
            BEGIN
                FOR tbl IN curs LOOP
                    EXECUTE 'DELETE FROM ' || tbl.name;
                END LOOP;
            END
            $$ LANGUAGE plpgsql;
        """)

        conn.commit()

    IS_INITIALIZED = True


def reset():
    logging.info("Resetting database...")
    with get_connection() as conn:
        conn.set_isolation_level(0)
        conn.autocommit = True

        with conn.cursor() as cur:
            cur.execute("SELECT truncate_all()")
        logging.info("Database was reset")
