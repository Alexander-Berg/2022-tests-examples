import ads.multik.pylib.key_value_store as kv


DATA = [
    ((True,), (1, 'super_uniq_hash'), {'title': 'title', 'body': 'body', 'href': 'href'}),
    ((True, 1), (1, 'super_uniq_hash'), {'title': 'user_1_title'}),
    ((True, 1, 'title'), (1, 'super_uniq_hash'), {'title': 'user_branch_title'}),
    ((True, 1, 'body'), (1, 'super_uniq_hash'), {'body': 'user_branch_body'}),
    ((True, 2), (1, 'super_uniq_hash'), {'title': 'user_2_title'}),
    ((True, 3), (1, 'super_uniq_hash'), {'body': 'user_3_body', 'href': 'user_3_href'})
]


def test_save_load():
    mem_store = kv.MemoryKeyValueStore(('prod', 'user', 'branch'), ('bid', 'key'), ('title', 'body', 'href'))
    mem_store.create_store((bool, int, str), (int, str), (str, str, str))
    mem_store.connect_store()

    for ns, key, d in DATA:
        mem_store.upsert(ns, key, d)

    backup_data = mem_store.dict()

    mem_store_new = kv.MemoryKeyValueStore(('prod', 'user', 'branch'), ('bid', 'key'), ('title', 'body', 'href'))
    mem_store_new.create_store((bool, int, str), (int, str), (str, str, str))
    mem_store_new.connect_store()
    mem_store_new.from_dict(backup_data)

    for ns, key, d in DATA:
        res = mem_store.read(ns, key)
        assert {key: res[key] for key in d} == d
