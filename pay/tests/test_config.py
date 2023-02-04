from mongo_idm.config import DEFAULT_DB_ROLES, DEFAULT_ROLES_INFO


def test_default_db_roles():
    assert DEFAULT_DB_ROLES == {
        "read": {
            "set": "read",
            "name": {
                "en": u"Read only",
                "ru": u"Только чтение",
            },
        },
        "readWrite": {
            "set": "readWrite",
            "name": {
                "en": u"Read write",
                "ru": u"Чтение и запись",
            },
        },
        "dbAdmin": {
            "set": "dbAdmin",
            "name": {
                "en": u"Administration",
                "ru": u"Администрирование",
            },
        },
    }


def test_default_roles_info():
    assert DEFAULT_ROLES_INFO == {
        "slug": "database",
        "name": {
                "en": u"Database",
                "ru": u"База",
        },
        "values": {},
    }
