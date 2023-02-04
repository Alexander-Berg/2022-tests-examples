from test_generic import request_without_rps_control, TestDistanceMatrixApi, SIMPLE_ONE_TO_ONE_QUERY
import threading
import time


THREAD_COUNT = 10
QUERIES_PER_THREAD = 10


class QueryThread(threading.Thread):
    def __init__(self):
        self.error = None
        self.resps = []
        threading.Thread.__init__(self)

    def run(self):
        try:
            for _ in range(QUERIES_PER_THREAD):
                self.resps.append(request_without_rps_control(SIMPLE_ONE_TO_ONE_QUERY))
        except Exception as e:
            self.error = e
            raise


class TestDistanceMatrixApiRpsLimit(TestDistanceMatrixApi):
    def test_large_rps_limit(self):
        try:
            threads = []
            for _ in range(THREAD_COUNT):
                t = QueryThread()
                threads.append(t)
                t.start()

            for t in threads:
                t.join()
            for t in threads:
                self.assertTrue(t.error is None)
                for resp in t.resps:
                    self.assert_code(resp, 200)
        finally:
            print("Sleep after rps test for 1 sec")
            time.sleep(1)
