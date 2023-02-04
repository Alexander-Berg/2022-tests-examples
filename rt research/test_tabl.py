#!/usr/bin/python
# -*- coding: utf-8 -*-

import yt.wrapper as yt


def reducer(key, recs):
    S = set()
    for rec in recs:
        S.add(rec['text'])

    L = list(S)
    L.sort()

    text = '\t'.join(L)

    yield {'url': key['url'], 'text': text}


def main():
    input = '//tmp/yuryz/links'
    output = '//tmp/yuryz/links_test'

    yt.run_reduce(reducer, input, output, reduce_by = 'url')
    yt.run_sort(output, sort_by='url')

    print yt.row_count(input)
    print yt.row_count(output)


if __name__ == '__main__':
    main()
