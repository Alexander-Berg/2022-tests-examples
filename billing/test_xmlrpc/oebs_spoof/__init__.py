# -*- coding: utf-8 -*-
"""Пример.
Поддерживаются изменение простых атрибутов (в том числе доступных через getitem).
Для изменения атрибутов доп. соглашений специальный тип, на сегодня возможно только изменение списка сервисов
 и только в количестве одного.
patches = [
        {
            "type": "object",
            "target": ["firm", "region_id"],
            "value": 159
        },
        {
            "type": "object",
            "target": ["firm", "firm_exports", {"type": "getitem", "key": "OEBS"}, "oebs_org_id"],
            "value": 666
        },
        {
            "type": "contract_attribute",
            "target": ["col0", "attributes", {"type": "contract_attribute", "code": "services"}],
            "value": [644]
        }
    ]
"""

import datetime
import contextlib
import operator
import itertools
import functools
import decimal
import copy

import sqlalchemy.orm as orm


from butils.execution_context import set_context, gen_ctx_id, get_context

from balance.muzzle_util import CursorWithLog

SQL_EXECS_CTX = 'sql_execs'


class SqlExecutionSaverCursor(CursorWithLog):
    def execute(self, sql, *args, **kwargs):
        def now_():
            return datetime.datetime.now().isoformat(sep=' ')
        ctx = get_context()
        sql_execs = ctx and ctx.get(SQL_EXECS_CTX, None)
        sql_exec = None
        if sql_execs is not None:
            sql_exec = dict(sql=sql, args=args, kwargs=kwargs, start_dt=now_())
            sql_execs.append(sql_exec)
        super(SqlExecutionSaverCursor, self).execute(sql, *args, **kwargs)
        if sql_exec is not None:
            sql_exec.update(dict(after_args=args, after_kwargs=kwargs, finish_dt=now_()))


def get_cursor_proxy_class():
    return SqlExecutionSaverCursor


def get_target(obj, target_spec):
    cur_o = obj
    for i, t in enumerate(target_spec):
        if isinstance(t, basestring):
            cur_o = getattr(cur_o, t)
        elif isinstance(t, dict):
            if t['type'] == 'getitem':
                cur_o = operator.getitem(cur_o, t['key'])
            if t['type'] == 'contract_attribute':
                if i + 1 != len(target_spec):
                    raise ValueError('contract_attribute must me last in target_spec')
                return cur_o
    return cur_o


def set_contract_attribute(c_attrs, code, value):
    if code != 'services':
        raise ValueError('Currently cant set that code for contract attributes')
    if not isinstance(value, list) or len(value) != 1:
        raise ValueError('Currently cant set code services for that value')

    for a in [a for a in c_attrs if a.code.lower() == code]:
        a.key_num = value[0]


def prepare_target(target_spec):
    return list(itertools.chain.from_iterable(t.split('.') if isinstance(t, basestring) else [t] for t in target_spec))


def set_target(obj, patch_spec):
    target_spec = patch_spec['target']
    value = patch_spec['value']
    cur_o = get_target(obj, target_spec[:-1])
    t = target_spec[-1]
    if isinstance(t, basestring):
        setattr(cur_o, t, value)
    elif isinstance(t, dict):
        if t['type'] == 'getitem':
            operator.setitem(cur_o, t['key'], value)
        if t['type'] == 'contract_attribute':
            set_contract_attribute(cur_o, t['code'], value)


def prepare_patch_specs(patch_specs):
    patch_specs = copy.deepcopy(patch_specs)
    for patch_spec in patch_specs:
        patch_spec['target'] = prepare_target(patch_spec['target'])

    return patch_specs


@contextlib.contextmanager
def obj_transform(patch_specs, obj):
    patch_specs = prepare_patch_specs(patch_specs)
    for patch_spec in patch_specs:
        get_target(obj, patch_spec['target'])
    for patch_spec in patch_specs:
        set_target(obj, patch_spec)
    # raise 6666
    try:
        yield
    finally:
        oebs_export = obj.exports['OEBS']
        changed_insts = itertools.chain(obj.session.dirty, obj.session.new, obj.session.deleted)
        for inst in (inst for inst in changed_insts if inst != oebs_export):
            orm.session.make_transient(inst)


def object_transform(patches):
    return functools.partial(obj_transform, patches)


def transform_arg(obj):
    def transform_cxo(obj):
        val = obj.getvalue()
        if val is None:
            return None
        return str(val)

    import cx_Oracle
    module = obj.__class__.__module__
    if isinstance(obj, decimal.Decimal):
        return dict(type='decimal.Decimal', value=str(obj))
    elif isinstance(obj, datetime.datetime):
        obj = obj.replace(microsecond=0)
        return dict(type='datetime.datetime', value=obj.isoformat(sep=' '))
    elif isinstance(obj, datetime.timedelta):
        return dict(type='datetime.timedelta', value=dict(days=obj.days,
                                                          seconds=obj.seconds, microseconds=obj.microseconds))
    elif module == cx_Oracle.__name__:
        return dict(type='.'.join((module, obj.__class__.__name__)), value=transform_cxo(obj))
    elif isinstance(obj, (list, tuple)):
        return [transform_arg(o) for o in obj]
    else:
        return obj


def transform_sql_exec(sql_exec):
    res = sql_exec.copy()

    def transform_params(args_name, kwargs_name, params_name):
        args = res[args_name]
        kwargs = res[kwargs_name]
        if len(args) > 0:
            res[params_name] = [transform_arg(a) for a in args[0]]
        del res[args_name]

        if len(kwargs) > 0:
            res[params_name] = {key: transform_arg(arg) for key, arg in kwargs.items()}
        del res[kwargs_name]
    transform_params('args', 'kwargs', 'params')
    transform_params('after_args', 'after_kwargs', 'after_params')
    return res


def transform_sql_execs(sql_execs):
    return [transform_sql_exec(sql_exec) for sql_exec in sql_execs]


class Spoofer(object):
    def __init__(self, patches):
        self.object_transform = object_transform(patches)
        self.sql_execs = []

    @property
    def transformed_result(self):
        return transform_sql_execs(self.sql_execs)


@contextlib.contextmanager
def spoof(patches):
    import balance.processors.oebs.utils as oebs_utils
    old_get_cursor_proxy_class = oebs_utils.get_cursor_proxy_class
    oebs_utils.get_cursor_proxy_class = get_cursor_proxy_class
    spoofer = Spoofer(patches)
    ctx = {'ctx_id': gen_ctx_id(), SQL_EXECS_CTX: spoofer.sql_execs}
    old_ctx = get_context()
    set_context(ctx)
    try:
        yield spoofer
    finally:
        oebs_utils.get_cursor_proxy_class = old_get_cursor_proxy_class
        set_context(old_ctx)
