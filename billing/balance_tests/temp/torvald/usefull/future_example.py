__author__ = 'torvald'

import time

# from balance import balance_api as api
# 'futures' module should by installed
# pip install futures
from concurrent import futures

# Example of using executor.map
def common_method(kwargs):
    start = time.time()
    time.sleep(1)
    print ('{} run: {}'.format(kwargs['a'], (time.time() - start)))
    return kwargs['a']

def map_example():
    start = time.time()
    executor = futures.ThreadPoolExecutor(max_workers=3)
    results = executor.map(common_method, [{'a': 1, 'b': 2}, {'a': 2, 'b': 3}])
    print (results)
    for item in results:
        print item
    print ('global: {}'.format((time.time() - start)))
# ---------------------------------------------------------

# Example of using 'as_complete'
def run_check(type):
    # Code to launch check
    # sleep = random.choice(xrange(10))
    # time.sleep(sleep)
    # for it in range(3):
    #     print (api.Medium().server.CreateClient(defaults.PASSPORT_UID, defaults.client()))
    # print('{} runtime: {} sec'.format(type, sleep))

    # result = api.test_balance().DCSRunCheck({'code': type, 'objects': '1,2'})

    return result

def as_complete_example():
    check_list = ['aob', 'aob_tr', 'aob_sw', 'aob_ua', 'aob_us']

    # from multiprocessing import dummy as Pool
    # Pool.pool()
    # result = imap_unordered(run_check, check_list)


    with futures.ThreadPoolExecutor(max_workers=5) as executor:
        to_do = []
        for check in check_list:
            future = executor.submit(run_check, check)
            to_do.append(future)
            msg = 'Scheduled for {}: {}'
            print(msg.format(check, future))
        results = []
        for future in futures.as_completed(to_do):
            res = future.result()
            print time.time()
            msg = '{} result: {!r}'
            print(msg.format(future, res))
            results.append(res)

        return len(results)


if __name__ == "__main__":
    map_example()
    # start = time.time()
    # as_complete_example()
    # print ('GLOBAL: {}'.format((time.time() - start)))
    pass