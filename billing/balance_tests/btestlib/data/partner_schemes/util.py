# -*- coding: utf-8 -*-
__all__ = []

import json


def register(table, name, scheme, ignore_duplicate_names=False):
    if not isinstance(scheme, basestring):
        scheme = json.dumps(scheme, encoding='utf8', ensure_ascii=False)
    if not ignore_duplicate_names and name in table:
        raise ValueError('name=%s already registered, ignore_duplicate_names=False' % (name, ))
    table[name] = scheme
