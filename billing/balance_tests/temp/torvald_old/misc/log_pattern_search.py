# -*- coding: utf-8 -*-

import re
from collections import deque

pre_deque_len = 5
post_deque_len = 5


def main():
    pre_chunk = deque(maxlen=pre_deque_len)
    post_chunk = deque(maxlen=post_deque_len)
    service_order_id = 123456

    res = []
    f = open('balance-notifier.log', 'r')
    post_f = open('balance-notifier.log', 'r')
    post_iter = iter(post_f)
    patt = re.compile('([\t]*<[a-zA-Z]*>{0}</[a-zA-Z]*>)'.format(service_order_id))
    for line in f.readlines():
        pre_chunk.append(line)
        post_iter.next()
        if patt.search(line):
            for post_line in range(post_deque_len):
                post_chunk.append(post_iter.next())
            res.append(pre_chunk + post_chunk)
            print patt.search(line).groups()

    pass


if __name__ == '__main__':
    main()
