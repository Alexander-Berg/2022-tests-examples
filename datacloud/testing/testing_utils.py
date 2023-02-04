# -*- coding: utf-8 -*-
from library.python import resource
from collections import namedtuple
from datacloud.dev_utils.json.json_utils import json_loads_byteified


__all__ = [
    'FakeContext',
    'RecordsGenerator',
    'data_by_name',
    'Table2Spawn',
    'TablesSpawnerCase'
]


class FakeContext:
    def __init__(self):
        self.table_index = 0

    def next_table(self):
        self.table_index += 1


class RecordsGenerator:
    def __init__(self, input_tables, context=None):
        self.input_tables = input_tables
        self.context = context

    def __iter__(self):
        for table in self.input_tables:
            for rec in table:
                yield rec
            if self.context:
                self.context.next_table()


def data_by_name(name):
    return [
        json_loads_byteified(line)
        for line in resource.find(name).splitlines()
    ]


Table2Spawn = namedtuple('Table2Spawn', ['yt_table', 'data'])


class TablesSpawnerCase():
    @property
    def _tables2spawn(self):
        raise NotImplementedError()

    def test(self, yt_client, yql_client):
        for table2spawn in self._tables2spawn:
            yt_client.write_table(table2spawn.yt_table, table2spawn.data)

        self._logic(yt_client=yt_client, yql_client=yql_client)

        for table2spawn in self._tables2spawn:
            yt_client.remove(table2spawn.yt_table)
