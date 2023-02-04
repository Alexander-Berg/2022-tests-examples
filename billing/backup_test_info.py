# -*- coding: utf-8 -*-
import sqlalchemy
from butils import logger

import balance
from balance import mapper
from balance.application import Application
import sys


log = logger.get_logger()


def fix_col_value(col_type, value):
    if not value:
        return 'null'
    if col_type.startswith('VARCHAR'):
        return "'%s'" % value
    if col_type == 'DATETIME':
        return "to_date('%s','YYYY-MM-DD HH24:MI:SS')" % value
    if col_type.startswith('OracleNumber'):
        return str(value)
    return repr(value)


def serialize_result_proxy(result_proxy):
    pass

def serialize_query(query):
    table_name, columns = None, None
    values = []
    for row in query.all():
        if not table_name:
            table_name = row.__table__.name.upper()
        values = []
        for col in row.__table__.columns:
            column_type = repr(col.type)
            if not issubclass(balance.scheme.OracleNumber, col.type.__class__):
                column_type = str(col.type)
            values.append((col.name, fix_col_value(column_type, getattr(row, col.name))))
        if not columns:
            columns = ', '.join([t[0].upper() for t in values])
        values.append(', '.join([t[1] for t in values]))
    return (table_name, columns, values)


def insert_serialize(scheme="", output=sys.stdout, close_output=False):
    def wrap(func):
        log.debug('Wrap serializer with params: [%s, %s]' % (scheme, output))
        insert_template = "INSERT INTO %s.%s (%s) VALUES (%s);\n"

        def serializer(*args, **kwargs):
            result = func(*args, **kwargs)
            if isinstance(result, balance.application.plugins.dbhelper.Query):
                (t_name, columns, values) = serialize_query(result)
            elif isinstance(result, sqlalchemy.engine.result.ResultProxy):
                serialize_result_proxy(result)
                (t_name, columns, values) = serialize_result_proxy(result)

            for value in values:
                output.write(insert_template % (scheme, t_name, columns, value))
            output.flush()
            if close_output and output is not sys.stdout:
                output.close()
        return serializer
    if scheme and not isinstance(scheme, basestring):
        raise RuntimeError("Scheme argument must be string")
    if not isinstance(output, file):
        raise RuntimeError("Output argument must be file descriptor")
    return wrap


class Backuper(Application):
    name = 'backuper'
    __version__ = '0.0.1'

    def __init__(self):
        super(Backuper, self).__init__()
        self.session = self.new_session()

    #@insert_serialize(scheme='BO', output=open('/dev/null', 'w'))
    @insert_serialize(scheme='BO')
    def export_processing(self):
        return self.session.query(mapper.Processing)\
            .filter(mapper.Processing.env_type == 'test')


    @insert_serialize(scheme='BO')
    def export_terminals(self):
        return self.session.query(mapper.Terminal)\
            .join(mapper.Processing)\
            .filter(mapper.Processing.env_type == 'test')

    #@insert_serialize(scheme='BO')
    #def export_service_terminals(self):
    #    return self.session.execute("select * from bo.t_service_terminal")

    def process(self):
        for name, val in self.__class__.__dict__.items():
            if name.startswith('export') and callable(val):
                print 'Calling [%s]...' % name
                print getattr(self, name)()


if __name__ == '__main__':
    Backuper().process()
