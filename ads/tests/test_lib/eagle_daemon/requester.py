import logging
import time
from multiprocessing import Pool

import requests
import ujson
from ads.bsyeti.tests.test_lib.eagle_answers_proto.answers_pb2 import TTests


class RequestToEagleOld:
    def __init__(self, request_base):
        self.request_base = request_base

    def __call__(self, request):
        request = self.request_base.format(uid=request)
        resp = None
        for i in range(15):
            try:
                resp = requests.get(request)
                assert resp.ok, resp.ok
                assert resp, "empty response"
                assert int(ujson.loads(resp.content)["data"][0]["is_full"]) == 1, resp.content
                break
            except Exception:
                logging.error("request #%d %s failed", i, request, exc_info=True)
                resp = None
            time.sleep(min(10, 2**i))
        return (request, resp)


def get_eagle_answers(uniqs, request_base, request_key):
    answers = {}

    n_processes = 8
    pool = Pool(n_processes)
    chunksize = len(uniqs) // n_processes + 1
    logging.info(
        "starting proc pool, %d processes, %d requests, %d chunksize",
        n_processes,
        len(uniqs),
        chunksize,
    )
    logging.debug("request_base %s , request_key %s", request_base, request_key)
    logging.debug("first five uniqs: %s", uniqs[:5])
    responses = pool.map(RequestToEagleOld(request_base), uniqs, chunksize)
    pool.close()
    pool.join()

    for uniq, (_, resp) in zip(uniqs, responses):
        if resp is not None and resp.ok:
            answers[(uniq, request_key)] = (resp.content, resp.headers)
        else:
            raise Exception("eagle failed to answer")
    return answers


class RequestToEagleJson:
    def __init__(self, prefix):
        self.prefix = prefix

    def __call__(self, request):
        final_request = self.prefix + request
        logging.debug("requests: `%s`", final_request)
        resp = None
        limit = 30
        for i in range(limit):
            try:
                resp = requests.get(final_request)
                assert resp.ok, resp.ok
                assert resp, "empty response"
                assert int(ujson.loads(resp.content)["data"][0]["is_full"]) == 1, resp.content
                return (request, resp)
            except Exception:
                logging.error("request #%d %s failed", i, final_request, exc_info=True)
                resp = None
                if limit - i <= 1:
                    raise Exception("eagle failed to answer correctly")
            time.sleep(min(2, (i + 1) / 4.0))


def get_eagle_responses(uniq_requests, prefix, test_id):
    answers = [None] * len(uniq_requests)

    n_processes = 8
    pool = Pool(n_processes)
    chunksize = len(uniq_requests) // n_processes + 1
    logging.info(
        "starting proc pool, %d processes, %d requests, %d chunksize",
        n_processes,
        len(uniq_requests),
        chunksize,
    )
    responses = pool.map(RequestToEagleJson(prefix), uniq_requests, chunksize)
    pool.close()
    pool.join()

    for ind, (req, resp) in enumerate(responses):
        if resp is not None and resp.ok:
            answers[ind] = (
                (req.encode(), test_id.encode()),
                (resp.content, dict(resp.headers)),
            )
        else:
            raise Exception("eagle failed to answer")
    return answers


class RequestToEagleProtos:
    def __init__(self, prefix):
        self.prefix = prefix

    def __call__(self, request):
        final_request = self.prefix + request
        logging.debug("requests: `%s`", final_request)
        resp = None
        content = None
        limit = 30
        for i in range(limit):
            try:
                resp = requests.get(final_request)
                assert resp, "empty response"
                assert resp.ok, resp.content
                content = resp.content
                assert content, "empty content"
                # we dont want to parse proto here.  # assert int(ujson.loads(content)["data"][0]["is_full"]) == 1, content
                return request, content
            except Exception:
                logging.error("request #%d %s failed", i, final_request, exc_info=True)
                resp = None
                if limit - i <= 1:
                    raise
            time.sleep(min(2, 0.5 * (i + 1)))


def get_eagle_protos(uniq_requests, prefix, test_id):

    n_processes = 8
    pool = Pool(n_processes)
    chunksize = len(uniq_requests) // n_processes + 1
    logging.info(
        "starting proc pool, %d processes, %d requests, %d chunksize",
        n_processes,
        len(uniq_requests),
        chunksize,
    )
    responses = pool.imap_unordered(RequestToEagleProtos(prefix), uniq_requests, chunksize)
    pool.close()
    pool.join()

    requests_to_final = [TTests.TTest.TRequest() for _ in range(len(uniq_requests))]
    for ind, (req, content) in enumerate(sorted(responses)):  # sorted is important
        requests_to_final[ind].Key = req
        requests_to_final[ind].Answer = content

    proto_obj = TTests.TTest()
    proto_obj.TestName = test_id
    proto_obj.Requests.extend(requests_to_final)
    proto_res = TTests()
    proto_res.Tests.extend([proto_obj])
    return proto_res.SerializeToString()
