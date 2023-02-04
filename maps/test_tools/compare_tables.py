import math

from yt.wrapper import YsonFormat
import maps.pylibs.utils.lib.traverse as traverse


def dump_table(ytc, table, float_columns=[], bytestrings=False):
    """
        Behaves all kinds of wierd if you have floats in values,
        due to mixture of float string representation, precision and ordering
    """
    table = list(ytc.read_table(table, format=YsonFormat(encoding=None) if bytestrings else YsonFormat()))
    for row in table:
        for key in row:
            if key in float_columns:
                row[key] = cast_float(row[key], "can't cast '{0}' to float: {1}".format(key, row[key]))

    return table


def sort_table(recs, keys=[]):
    # `keys` used for key columns to sort by them; this produces nicer compare result
    return sorted(recs, key=lambda x: [(k, x.get(k) is not None, x.get(k)) for k in keys] + sorted((k, v is not None, v) for k, v in x.items()))


def almost_equal(lhs, rhs, prec, err):
    lhs_trav = traverse.is_traversable(lhs)
    rhs_trav = traverse.is_traversable(rhs)

    if not lhs_trav and not rhs_trav:
        lhs_float = isinstance(lhs, float)
        rhs_float = isinstance(rhs, float)

        if lhs_float and rhs_float:
            return abs(lhs - rhs) < 0.5 * 10**(-prec)

        return lhs == rhs

    if lhs_trav and rhs_trav and len(lhs) == len(rhs):
        lhs_dict = isinstance(lhs, dict)
        rhs_dict = isinstance(rhs, dict)

        if not lhs_dict and not rhs_dict:
            return all(almost_equal(l, r, prec, err) for l, r in zip(lhs, rhs))

        if lhs_dict and rhs_dict and set(lhs.keys()) == set(rhs.keys()):
            return all(almost_equal(lhs[elem], rhs[elem], prec, err) for elem in lhs)

    raise AssertionError(err)


def cast_float(entity, err):
    if not traverse.is_traversable(entity):
        try:
            return float(entity) if not isinstance(entity, str) else entity
        except:
            return entity
    else:
        return traverse.map_values(entity, lambda v: cast_float(v, err))


def assert_equal_tables_contents(
    expected_rows, actual_rows, precision=7,
    ignored_columns=None, float_columns=[],
    null_equals_unexistant=False, nans_equal=False,
    unordered=False, keys=[],
):
    if unordered:
        expected_rows = sort_table(expected_rows, keys=keys)
        actual_rows = sort_table(actual_rows, keys=keys)

    assert len(expected_rows) == len(actual_rows), "Tables have different row count: {0} != {1}".format(len(expected_rows), len(actual_rows))

    for i, (expected_rec, actual_rec) in enumerate(zip(expected_rows, actual_rows)):
        expected_keys = set(expected_rec.keys()) - set(ignored_columns or [])
        actual_keys = set(actual_rec.keys()) - set(ignored_columns or [])
        if not null_equals_unexistant:
            assert expected_keys == actual_keys, "\n".join([
                "Tables have different set of keys in row {4}",
                "\tfirst: {0}",
                "\tsecond: {1}",
                "\tfirst extra keys: {2}",
                "\tsecond extra keys: {3}",
            ]).format(expected_keys, actual_keys, expected_keys - actual_keys, actual_keys - expected_keys, i)

        keys = expected_keys | actual_keys
        for column in keys:
            expected_value = expected_rec.get(column)
            actual_value = actual_rec.get(column)

            err = "record {0} differs in column '{1}' (expected:{2}, actual:{3}, expected_type:{4}, actual_type:{5})".format(
                i, column, expected_value, actual_value, type(expected_value), type(actual_value)
            )

            if ignored_columns and column in ignored_columns:
                continue

            if column in float_columns:
                expected_value = cast_float(expected_value, "can't cast expected '{0}' to float at row {1}, value: {2}".format(
                    column,
                    i,
                    expected_value
                ))
                actual_value = cast_float(actual_value, "can't cast actual '{0}' to float at row {1}, value: {2}".format(
                    column,
                    i,
                    actual_value
                ))

            if column in float_columns and expected_value is not None and actual_value is not None:
                if isinstance(expected_value, float) and isinstance(actual_value, float):
                    if math.isnan(expected_value) and math.isnan(actual_value) and nans_equal:
                        continue
                assert almost_equal(expected_value, actual_value, precision, err), err
            else:
                assert expected_value == actual_value, err


def assert_equal_tables(
    ytc, expected, actual, precision=7,
    ignored_columns=None, float_columns=[], null_equals_unexistant=False,
    unordered=False, nans_equal=False,
    bytestrings=False, keys=[],
):
    # reading table with no encoding will produce keys as bytes, which matters for py3
    def maybe_to_bytes(cols):
        return [col.encode('utf-8') for col in cols] if cols and bytestrings else cols

    expected_recs = dump_table(ytc, expected, float_columns, bytestrings=bytestrings)
    actual_recs = dump_table(ytc, actual, float_columns, bytestrings=bytestrings)

    assert_equal_tables_contents(
        expected_recs,
        actual_recs,
        precision=precision,
        ignored_columns=maybe_to_bytes(ignored_columns),
        float_columns=maybe_to_bytes(float_columns),
        null_equals_unexistant=null_equals_unexistant,
        nans_equal=nans_equal,
        unordered=unordered, keys=keys,
    )
