# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import json
import logging
import six

from sepelib.util.log.formatters import TabSeparatedKeyValueFormatter


def test_tskv_formatter():

    result = six.StringIO()
    max_len = 100

    formatter = TabSeparatedKeyValueFormatter(fmt='prefix\t%(message)s', max_value_length=max_len)
    handler = logging.StreamHandler(result)
    handler.setFormatter(formatter)
    logger = logging.Logger('some.logger', level=logging.DEBUG)
    logger.addHandler(handler)

    class CustomClass(object):
        def __str__(self):
            return 'custom\trepresentation\\'

    json_value = [1, 2.0, 'thing']
    long_value = 'word' * max_len * 2
    logger.info({
        'random_key': 'random\tvalue\n',
        'json_key': json_value,
        'object_key': CustomClass(),
        'long_key': long_value,
    })

    expected_result = '\t'.join([
        'prefix',
        'json_key={}'.format(json.dumps(json_value)),
        'long_key={}'.format(long_value[:max_len]),
        r'object_key=custom\trepresentation\\',
        r'random_key=random\tvalue\n'
    ])

    assert expected_result == result.getvalue().rstrip()
