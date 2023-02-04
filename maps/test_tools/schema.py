from collections import defaultdict
from functools import reduce
from typing import List, Dict, Optional, Iterable, Set, Collection

from yt.wrapper.yson import YsonUint64, YsonUnicode

from maps.pylibs.yt.lib import YtContext

from maps.analyzer.pylibs.envkit.yt import copy_user_attributes
from maps.analyzer.pylibs.schema import TypeV3
from maps.analyzer.pylibs.schema.column import Column
from maps.analyzer.pylibs.schema.table import Table
import maps.analyzer.pylibs.schema as pyschema


_TYPES_MAP = {
    bool: pyschema.Boolean,
    float: pyschema.Double,
    list: pyschema.Yson,
    int: pyschema.Int64,
    str: pyschema.String,
    type(None): None,  # value not set, type is unknown
    YsonUint64: pyschema.Uint64,
    YsonUnicode: pyschema.String,
}


def infer_common_type(types: Collection[Optional[TypeV3]], hint: Optional[TypeV3] = None) -> TypeV3:
    """ Infer common type for column values: Any if any of types differs """
    types_set = set(filter(lambda t: t is not None, types))
    if len(types_set) == 1:
        return types_set.pop()
    if len(types_set) == 0:
        return hint or pyschema.Yson
    return pyschema.Yson


def infer_required(types: Collection[Optional[TypeV3]]) -> bool:
    """ Infer if column can be required: all types should be the same and not None (i.e. undetected type due to no value) """
    types_set = set(types)
    return len(types_set) == 1 and types_set.pop() is not None


def infer_column(name: str, types: Collection[Optional[TypeV3]], hint: Optional[Column] = None, narrow: bool = False) -> Column:
    hint_ty = hint.type.unwrapped if hint is not None else None
    ty = infer_common_type(types, hint=hint_ty)

    # narrow yson to complex-type hint
    if narrow and ty == pyschema.Yson and hint_ty is not None and not hint_ty.primitive:
        ty = hint_ty

    if narrow and ty.primitive and hint_ty is not None and hint_ty.primitive and ty.yson_type == hint_ty.yson_type:
        ty = hint_ty

    # special case for string -> utf8
    if narrow and ty == pyschema.String and hint_ty == pyschema.Utf8:
        ty = hint_ty

    req = infer_required(types) and (hint is None or hint.required)
    if not req or ty == pyschema.Yson:  # yson always optional
        ty = pyschema.Optional(ty)
    return pyschema.column(name, ty, None)


def infer_schema(rows: Iterable[dict], hints: List[Column] = None, strict: bool = False, narrow: bool = False) -> Table:
    rows = list(rows)

    complex_hints: List[Column] = [h for h in (hints or []) if not h.type.unwrapped.primitive]
    if complex_hints and strict and not narrow:
        raise RuntimeError(
            'There are complex types in hints, but narrow not specified; inferred Any won\'t be narrowed to complex type:\n'
            '  hints: {}'.format(complex_hints)
        )

    hints_dict: Dict[str, Column] = {h.name: h for h in hints} if hints is not None else {}

    columns_names = reduce(lambda l, r: l | r, (set(row.keys()) for row in rows))

    columns_types: Dict[str, Set[Optional[TypeV3]]] = defaultdict(set)
    for row in rows:
        for name in columns_names:
            value = row.get(name)
            columns_types[name].add(_TYPES_MAP.get(type(value), pyschema.Yson))
    inferred = pyschema.table([
        infer_column(name, types, hint=hints_dict.get(name), narrow=narrow)
        for name, types in columns_types.items()
    ], None)

    if strict and hints:
        hint_errors = []
        for col in inferred.columns:
            if col.name in hints_dict and hints_dict[col.name] != col:
                hint_errors.append('    inferred: {}, hint: {}'.format(col, hints_dict[col.name]))
        if hint_errors:
            raise RuntimeError('Inferred types for hinted columns differs from hints: \n{}'.format('\n'.join(hint_errors)))

    return inferred


def schematize_table(
    ytc: YtContext,
    table: str,
    sort_by: List[str] = None,
    unique_keys: bool = False,
    rows_count_for_sample: int = 100,
    schema: Optional[Table] = None,
    hints: List[Column] = None,
    strict_hints: bool = False,
    inherit_user_attrs: bool = True,
    narrow: bool = False
) -> None:
    """
    Assign schema to test table, possibly inferring schema from rows

    Args:
        * ytc - YtContext
        * table - path to table
        * sort_by - sort table by columns
        * unique_keys - are keys unique
        * rows_count_for_sample - rows count to infer schema from
        * schema - `Table` object to set specified schema (won't infer schema in this case)
        * hints - list of `Column` objects - hints for some columns (i.e. when no real values found, so optional of any type may be used in this case)
        * strict_hints - inferred type should equal hint
        * inherit_user_attrs - if True user attributes will be copied to result table
        * narrow - allow narrowing inferred types to hints (this is useful because only limited set may be inferred):
            - primitives narrowed to hint if they have same representation (uint, int, string)
            - any narrowed to complex hint (as long as complex types can't be inferred)
    """
    table_path = ytc.TablePath(table, start_index=0, end_index=rows_count_for_sample)
    table_schema = schema or infer_schema(ytc.read_table(table_path), hints=hints, strict=strict_hints, narrow=narrow)
    if sort_by:
        table_schema = pyschema.sorted_table(table_schema, sort_by, unique_keys)

    result = ytc.create_temp_table(schema=table_schema.schema)
    if sort_by:
        ytc.run_sort(table, result, sort_by=sort_by)
    else:
        ytc.run_merge(table, result, spec={'schema_inference_mode': 'from_output'})

    if inherit_user_attrs:
        copy_user_attributes(ytc, table, result)

    ytc.copy(result, table, force=True)
