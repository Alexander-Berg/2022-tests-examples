# -*- coding: utf-8 -*-
import types


import sqlparse
from sqlparse import tokens


def convert_value(ph_id, value):
    if isinstance(value, basestring):
        return u"'%s'" % (value.decode('utf8'), )
    elif isinstance(value, (int, float)):
        return str(value)
    elif isinstance(value, types.NoneType):
        return 'NULL'
    elif isinstance(value, dict):
        if value['type'] == 'decimal.Decimal':
            return value['value']
        elif value['type'].startswith('cx_Oracle'):
            return ':%s' % (ph_id, )
        elif value['type'] == 'datetime.datetime':
            return "TO_DATE('%s', 'YYYY-MM-DD HH24:MI:SS')" % (value['value'], )
        else:
            return '%s:%s' % (value['type'], value['value'])
    return str(value)


def format_sql_exec(sql_exec):
    result = []
    statements = sqlparse.parse(sql_exec['sql'] ,encoding='utf8')
    params = sql_exec.get('params', None)
    is_positional = isinstance(params, list)
    result.append('-- ' + str(params))
    for statement in statements:
        if params is not None:
            for token in statement.flatten():
                if not token.match(tokens.Token.Name.Placeholder, [r'(?<!\w)[$:?]\w+'], regex=True):
                    continue
                ph_id = token.value[1:]
                if is_positional:
                    ph_id = int(ph_id) - 1
                token.value = convert_value(ph_id, params[ph_id])

        result.append(str(statement))
    return result


def format(sql_execs):
    res = []
    for i, sql_exec in enumerate(sql_execs):
        res.append('-- ' + 'sql_exec: ' + str(i))
        res += format_sql_exec(sql_exec)

    return '\n'.join(res)
