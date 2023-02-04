# -*- coding: utf-8 -*-

import json

from balance import balance_api as api

from check import utils


def main():
    # cmp_id = api.test_balance().DCSRunCheckNew([json.dumps({
    #     'code': 'bua',
    #     'objects': '1,2,3',
    # })])

    cmp_id = utils.run_check_new('bua', objects=[1, 2, 3])
    print(cmp_id)


if __name__ == '__main__':
    main()

# vim:ts=4:sts=4:sw=4:tw=79:et:
