from skynet_keychain.server import Server


def test_parse_keys():
    # create server response
    records = {
        'COMMON': [
            {
                'name': 'common_key',
                'keybody': 'common body'
            },
            {
                'name': 'common_key_2',
                'keybody': 'common body 2'
            },
        ],
        'PSEUDO': [
            {
                'name': 'pseudo_user',
                'keys': ['pseudo body 1'],
            },
            {
                'name': 'pseudo_user_2',
                'keys': ['pseudo body 2'],
            },
        ],
        'STAFF': [
            {
                'name': 'staff_user',
                'keys': ['staff body 1.1', 'staff body 1.2'],
            },
            {
                'name': 'staff_user_2',
                'keys': ['staff body 2.1', 'staff body 2.2'],
            },
        ],
    }

    # parse records
    ret = Server.parseRecords(records)
    assert(ret)
    commonKeys, userKeys = ret

    # check test key in keys
    assert(commonKeys['common_key.common.public'].body == 'common body\n')
    assert(commonKeys['common_key_2.common.public'].body == 'common body 2\n')
    assert(userKeys['pseudo_user']['pseudo_user.pseudo.public'].body == 'pseudo body 1\n')
    assert(userKeys['pseudo_user_2']['pseudo_user_2.pseudo.public'].body == 'pseudo body 2\n')
    assert(userKeys['staff_user']['staff_user.staff.public'].body == 'staff body 1.1\n')
    assert(userKeys['staff_user']['staff_user.staff.1.public'].body == 'staff body 1.2\n')
    assert(userKeys['staff_user_2']['staff_user_2.staff.public'].body == 'staff body 2.1\n')
    assert(userKeys['staff_user_2']['staff_user_2.staff.1.public'].body == 'staff body 2.2\n')


def test_common_keys(tmpdir):
    # create test path
    keysPath = tmpdir.mkdir('keys_path')

    # create non-listed key
    nonLisedKey = keysPath.join('non_listed.public')
    nonLisedKey.write('non lised body')

    # create old key
    oldKey = keysPath.join('old.public')
    oldKey.write('old body')

    # crete new key record
    keys = {}
    new_key_name = 'new.public'
    new_key_body = 'new key body'
    keys[new_key_name] = Server.Key(new_key_name, new_key_body)

    # create old key record
    oldKey.key_new_content = 'new content'
    keys[oldKey.basename] = Server.Key(oldKey.basename, oldKey.key_new_content)

    # update keys in path
    ret = Server.updateCommonKeys(keysPath.strpath, keys)

    # non listed key backuped
    assert(nonLisedKey.check(exists=0))
    assert(keysPath.join(nonLisedKey.basename + Server.BACKUP_KEY_SUFFIX).check(exists=1))

    # new key exists
    assert(keysPath.join(new_key_name).check(exists=1))

    # key key content
    assert(keysPath.join(new_key_name).read() == new_key_body)

    # old key content changed
    assert(oldKey.read() == oldKey.key_new_content)


def test_user_keys(tmpdir):
    # create test path
    keysPath = tmpdir.mkdir('keys_path')

    # create non-listed dir
    nonListedDir = keysPath.mkdir('non_listed_user')

    user_name = 'user'
    userKeysPath = keysPath.mkdir(user_name)

    # create old key
    oldKey = userKeysPath.join('old.public')
    oldKey.write('old body')

    # crete new key record
    keys = {}
    new_key_name = 'new.public'
    new_key_body = 'new key body'
    keys.setdefault(user_name, {})[new_key_name] = Server.Key(new_key_name, new_key_body)

    # create old key record
    oldKey.key_new_content = 'new content'
    keys.setdefault(user_name, {})[oldKey.basename] = Server.Key(oldKey.basename, oldKey.key_new_content)

    # update keys in path
    ret = Server.updateUserKeys(keysPath.strpath, keys)

    # non listed key backuped
    assert(nonListedDir.check(exists=0))
    assert(keysPath.join(nonListedDir.basename + Server.BACKUP_KEY_SUFFIX).check(exists=1))

    # new key exists
    assert(userKeysPath.join(new_key_name).check(exists=1))

    # key key content
    assert(userKeysPath.join(new_key_name).read() == new_key_body)

    # old key content changed
    assert(oldKey.read() == oldKey.key_new_content)
