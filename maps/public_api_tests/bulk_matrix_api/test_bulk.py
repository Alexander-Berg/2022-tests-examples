#!/usr/bin/env python3
from argparse import ArgumentParser
import requests
import retry
import json
import time
import logging
import os
from multiprocessing.pool import ThreadPool
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from requests import ConnectionError
requests.packages.urllib3.disable_warnings((InsecureRequestWarning))


DEFAULT_ROUTING_MATRIX_API = "https://api.routing.yandex.net"
# prestable: http://prestable.api.routing.n.yandex-team.ru
ROUTING_MATRIX_URL = os.environ.get('ROUTING_MATRIX_API', DEFAULT_ROUTING_MATRIX_API) + '/routing_matrix'
CURRENT_STATE_URL = os.environ.get('ROUTING_MATRIX_API', DEFAULT_ROUTING_MATRIX_API) + '/current_state'

logger = logging.getLogger()
logger.addHandler(logging.FileHandler("bulk_matrix.log"))

session = requests.Session()
adapter = requests.adapters.HTTPAdapter(max_retries=3, pool_connections=64, pool_maxsize=128)
session.mount('https://', adapter)


def timestamps():
    return [
        int(time.time()) + t
        for t in [7200, 10800, 14400, 18000, 21600, 25200, 28800, 36000, 46800, 50400, 54000, 61200, 93600]]


# GeoSaaS Bulk frontend
@retry.retry(exceptions=(RuntimeError, ConnectionError), tries=5, delay=2, backoff=2, logger=logger, jitter=(0, 2))
def start_slice(locations, timestamp):
    locs = "|".join("{lat},{lon}".format(**loc["point"]) for loc in locations)
    url = ROUTING_MATRIX_URL
    data = "&origins=" + locs + "&destinations=" + locs + "&departure_time=" + str(timestamp)
    if len(locations) < 100:
        data += "&pool_name=INF"
    start = time.time()
    resp = session.post(url, data=data, verify=False)
    msg = "Url: {url}, Status: {stat}, Response: {response},  Data: `{data}`".format(
        url=resp.url, data=data, stat=resp.status_code, response=resp.text)
    if not resp.ok:
        msg += "req duration: " + str(time.time() - start) + "Starting failed: " + str(
            (resp.status_code, resp.text, resp.headers))
        logger.error(msg)
        raise RuntimeError(msg)
    logger.error("Task started:" + msg + " req duration: " + str(time.time() - start) + " Response: " + resp.text)
    task_id = resp.json()["id"]
    return task_id


@retry.retry(exceptions=(RuntimeError, ConnectionError), tries=5, delay=1, backoff=2, logger=logger, jitter=(0, 2))
def get(url):
    start = time.time()
    resp = session.get(url, verify=False)
    if not resp.ok:
        raise RuntimeError(
            "GET failed: " + str((resp.status_code, resp.text, resp.headers, resp.url)) +
            " It took: " + str(time.time() - start)
        )
    return resp


@retry.retry(exceptions=(RuntimeError, ConnectionError), tries=1, delay=2, backoff=2, logger=logger, jitter=(0, 2))
def wait_slice(task_id):
    done = False
    url = CURRENT_STATE_URL + "?data_id=" + task_id
    router_timeout = 1000000
    timeout = 0.1
    while not done:
        try:
            resp = get(url + "&timeout=%d" % router_timeout)
            done = resp.json()["state"]["data"]["ready"]
            timeouted = resp.json()["state"].get("timeouted", False)
            logger.error("Response: " + resp.text)
            if not done:
                logger.error("{url} not ready. Sleeping. state data: {data}".format(
                    url=url, data=resp.json()["state"]["data"]))
                time.sleep(timeout)
                if timeout < 5 and not timeouted:
                    timeout *= 2
                if timeouted and router_timeout < 20000000:
                    router_timeout *= 2
        except ConnectionError:
            print("Connection eror, retrying...")
    resp = get(url + "&pron=need_result")
    # logger.error(resp.url + " Response: " + resp.text)


def get_matrix(args):
    locations, timestamp = args
    task_start = time.time()

    try:
        task_id = start_slice(locations, timestamp)
        wait_slice(task_id)
        return time.time() - task_start
    except Exception as e:
        logger.error("Failed to download matrix:" + str(e))
        return task_start - time.time()


def router_stats(query_cb, locations, min_locations):
    slice_sizes = []
    slice_times = []
    wall_sizes = []
    wall_times = []
    for p in range(2, 20, 1):
        locs = locations[:2**p]
        size = len(locs)
        if (size < min_locations):
            continue
        pool = ThreadPool(16)
        start = time.time()
        tt = timestamps()
        times = pool.map(query_cb, [(locs, t) for t in tt])
        elpassed = time.time() - start
        wall_sizes.append(size)
        wall_times.append(elpassed)
        for t in times:
            slice_sizes.append(size)
            slice_times.append(t)
        print("<{{Total time for {count} of {size}x{size}: {t:.2f}\nSlice times {slices}\n}}>".format(
            count=len(tt), size=size, t=elpassed, slices=times))

        if size >= len(locations):
            break
    return slice_sizes, slice_times, wall_sizes, wall_times


def main():
    parser = ArgumentParser()
    parser.add_argument('--source-task', help="Mvrp task used as a locations source", required=True)
    parser.add_argument('--max-locations', help="Limit maximum size of requested matrix", type=int, default=2500)
    parser.add_argument('--min-locations', help="Minimum size of requested matrix", type=int, default=1)
    args = parser.parse_args()

    with open(args.source_task) as f:
        task = json.load(f)
        locations = task["locations"][:args.max_locations]

    router_stats(get_matrix, locations, args.min_locations)


if __name__ == "__main__":
    main()
