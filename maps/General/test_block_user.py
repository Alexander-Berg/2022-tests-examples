from lib.import_file import generate_excel_body
from lib.server import server
import lib.async_processor as async_processor

import maps.automotive.libs.large_tests.lib.db as db

import pytest


def block_user(uid):
    with db.get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute("""
                INSERT INTO blacklisted_users(user_id) VALUES(%s)
            """, (uid,))
            conn.commit()


def unblock_user(uid):
    with db.get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute("""
                DELETE FROM blacklisted_users WHERE user_id=%s
            """, (uid,))
            conn.commit()


def wait_for_import_completed(user, org):
    async_processor.perform_all_work()
    (server.get_import_file_status(user, org) >> 200)["status"] == "Processed"


@pytest.mark.skip(
    reason="This check disabled for now, files will be uploaded but not imported."
        "\nEnable it here: https://nda.ya.ru/t/tmS1wfit4M6PC2")
def test_blocked_user(user, organization):
    server.post_import_file(user, 'test.xls', organization, generate_excel_body()) >> 200
    wait_for_import_completed(user, organization)

    block_user(user.uid)

    resp = server.post_import_file(user, 'test.xls', organization, generate_excel_body()) >> 422
    assert resp["code"] == "BLOCKED_USER"

    unblock_user(user.uid)

    server.post_import_file(user, 'test.xls', organization, generate_excel_body()) >> 200
    wait_for_import_completed(user, organization)
