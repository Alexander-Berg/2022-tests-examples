# -*- coding: utf-8 -*-

def execute_sql(session, sql_query, sql_params=None, dict_list=True, key=None):
    '''
    Примеры вызова:
    1. С параметрами - execute_sql('select sysdate, :xxx from dual', {'xxx': '666'})
    2. Без параметров и в сыром виде - execute_sql('select sysdate from dual', dict_list=False)
    3. Без возвращаемых значений - execute_sql('update t_pycron_state set failed=0 where id = 100000000000')
    '''

    def normalize(value):
        if isinstance(value, basestring):
            value = value.strip('\x00')
        return value

    def normalize_dict(row):
        row = dict(row)
        for k, v in row.iteritems():
            row[k] = normalize(v)
        return row

    def normalize_list(row):
        row = list(row)
        for i, v in enumerate(row):
            row[i] = normalize(v)
        return tuple(row)

    with session.begin():
        result_proxy = session.execute(sql_query, sql_params)
        header = result_proxy.keys()

        if header:
            if dict_list:
                if key:
                    result = {unicode(nrow[key]): nrow for nrow in (normalize_dict(row) for row in result_proxy)}
                else:
                    result = map(normalize_dict, result_proxy)
                return result
            else:
                rows = result_proxy.fetchall()
                if key:
                    rows = {unicode(normalize(row[key])): normalize_list(row) for row in rows}
                else:
                    rows = map(normalize_list, rows)
                return {'header': header, 'rows': rows,
                        'count': result_proxy.rowcount}
        else:
            return result_proxy.rowcount
