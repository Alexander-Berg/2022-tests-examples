import maps.analyzer.pylibs.schema as s


CLID = s.column('clid', s.String, None)
UUID = s.column('uuid', s.String, None)
TIME = s.column('time', s.Optional(s.Uint64), None)
OPT_UUID = s.column('uuid', s.Optional(s.String), None)


def test_diff():
    def tbl(*cols):
        return s.table(cols, None)

    def diff_has(line, left, right):
        return line in s.diff_tables(left, right).diff

    def subset_has(line, left, right):
        return line in s.diff_tables(left, right).subset

    # equal tables
    assert diff_has('left is equal to right', tbl(CLID, UUID, TIME), tbl(CLID, UUID, TIME))
    # subset
    assert subset_has('left is subset of right', tbl(CLID, UUID, TIME), tbl(CLID, UUID))
    assert subset_has('left is subset of right', tbl(CLID, UUID, TIME), tbl(TIME))
    # inequal
    assert diff_has('columns only in left table', tbl(CLID, UUID, TIME), tbl(UUID, TIME))
    assert diff_has('columns only in right table', tbl(CLID, TIME), tbl(CLID, UUID, TIME))
    assert diff_has('different columns', tbl(CLID, UUID), tbl(CLID, OPT_UUID))
    # subset
    assert not subset_has('columns only in left table', tbl(CLID, UUID, TIME), tbl(UUID, TIME))
    assert subset_has('columns only in right table', tbl(CLID, TIME), tbl(CLID, UUID, TIME))
    assert subset_has('different columns', tbl(CLID, UUID), tbl(CLID, OPT_UUID))

    # sort orders
    USERS = s.sorted_table(tbl(CLID, UUID, TIME), sort_by=[CLID.name, UUID.name])
    USERS_OPT = s.sorted_table(tbl(CLID, OPT_UUID, TIME), sort_by=[CLID.name, OPT_UUID.name])
    BY_UUIDS = s.sorted_table(tbl(CLID, UUID, TIME), sort_by=[UUID.name])
    BY_CLIDS = s.sorted_table(tbl(CLID, UUID, TIME), sort_by=[CLID.name])

    assert not diff_has('different sort order', USERS, USERS_OPT)
    assert diff_has('different sort order', USERS, BY_UUIDS)
    assert diff_has('different sort order', USERS, BY_CLIDS)
    assert diff_has('different sort order', USERS_OPT, BY_UUIDS)
    assert diff_has('different sort order', USERS_OPT, BY_CLIDS)
    assert not diff_has('different columns', USERS, BY_UUIDS)
    assert not diff_has('different columns', USERS, BY_CLIDS)
    assert subset_has('left sort order is not prefix of right', USERS, BY_UUIDS)
    assert subset_has('left sort order is not prefix of right', USERS, BY_CLIDS)
    assert subset_has('left sort order is not prefix of right', BY_UUIDS, USERS)
    assert not subset_has('left sort order is not prefix of right', BY_CLIDS, USERS)
