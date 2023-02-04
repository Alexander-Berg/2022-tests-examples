import logging
import os
import requests
import random
import statistics
import time
from dataclasses import dataclass
from math import ceil
from multiprocessing import Process, Manager
from threading import Thread, Event
from typing import Optional, List

from .parametrization import RequestsProvider

requests.packages.urllib3.disable_warnings(requests.packages.urllib3.exceptions.InsecureRequestWarning)


@dataclass(frozen=True)
class Response:
    status_code: int
    query_duration_s: float
    text: Optional[str] = None


class _RPSLimitedClient(Thread):
    def __init__(self, rps, requests_, stop_signal, responses, exceptions):
        self._requests = requests_
        self._t_delta = (1 / rps)
        self._stop_signal: Event = stop_signal
        self.responses = responses
        self.exceptions = exceptions
        Thread.__init__(self)

    def run(self):
        session = requests.Session()
        t_next_request = time.time() + random.random()  # randomize the start time

        for request_data in self._requests:
            wait_time = t_next_request - time.time()
            if wait_time > 0:
                time.sleep(wait_time)
            if self._stop_signal.is_set():
                return

            t_request = time.time()
            try:
                r = session.request(**request_data)
                t_request_complete = time.time()
                self.responses.append(Response(
                    status_code=r.status_code,
                    query_duration_s=t_request_complete - t_request,
                    text=r.text[:100] if r.status_code >= 300 else None))
            except Exception as e:
                self.exceptions.append(e)
                logging.debug(f"Exception: {e}")
            t_next_request = t_request + self._t_delta


@dataclass(frozen=True)
class Config:
    rps: float
    requests_provider: RequestsProvider
    settings: dict
    duration_s: int
    mean_response_time_s: float
    threads_per_process: int


class LoadTester:
    def __init__(self, config):
        assert isinstance(config, Config)
        self._config = config
        self._logger = logging.getLogger()

        max_rps_per_thread = 1 / config.mean_response_time_s
        self.num_threads = ceil(config.rps / (max_rps_per_thread * 0.9))
        self.rps_per_thread = config.rps / self.num_threads

        self._logger.info(f"Num threads: {self.num_threads}, target rps per thread: {self.rps_per_thread:.2f}. "
                          f"Num processes: {(self.num_threads + self._config.threads_per_process - 1) // self._config.threads_per_process}, "
                          f"max threads per process: {self._config.threads_per_process}. "
                          f"Total RPS: {self.rps_per_thread * self.num_threads:.2f}")

        self._stop_signal = Event()

    def start_clients(self, number_of_threads, responses, exceptions) :
        self._logger.debug(f"Started process {os.getpid()}.")
        default_timeout = 3 * self._config.mean_response_time_s if self._config.mean_response_time_s else 10.
        def enrich_with_timeout(request):
            return request if 'timeout' in request else {**{'timeout': default_timeout}, **{request}}
        clients = [
            _RPSLimitedClient(
                rps=self.rps_per_thread,
                requests_=[enrich_with_timeout(self._config.requests_provider.next_request(self._config.settings))
                           for _ in range(ceil(self.rps_per_thread * (self._config.duration_s + 1)))],
                stop_signal=self._stop_signal,
                responses=responses,
                exceptions=exceptions)
            for _ in range(number_of_threads)
        ]
        for client in clients:
            client.start()
        self._logger.debug(f"Started {number_of_threads} threads in process {os.getpid()}.")
        try:
            t_now = time.time()
            t_stop = t_now + self._config.duration_s
            while t_now < t_stop:
                time.sleep(max((t_stop - t_now) * 0.95, 0.01))
                t_now = time.time()
        except KeyboardInterrupt:
            self._logger.info("The job was aborted")
            self._stop_signal.set()
            raise

        self._stop_signal.set()

        self._logger.debug(f"Waiting for workers being stopped for process {os.getpid()} ...")
        for client in clients:
            stopped = False
            while not stopped:
                client.join(timeout=1)
                stopped = not client.is_alive()

    def run(self):
        with Manager() as manager:
            self.exceptions = manager.list()
            self.responses = manager.list()
            process_count = (self.num_threads + self._config.threads_per_process - 1) // self._config.threads_per_process
            processes = []
            for process_idx in range(process_count):
                p = Process(target=self.start_clients,
                            args=(min((process_idx + 1) * self._config.threads_per_process, self.num_threads) - process_idx * self._config.threads_per_process,
                                  self.responses,
                                  self.exceptions))
                p.start()
                processes.append(p)
            self._logger.info(f"Started {process_count} processes.")

            for p in processes:
                p.join()
            self._logger.info("Finished.")

            return self.stats()

    @dataclass(frozen=True)
    class TestingResults:
        status_code_hist: dict
        exceptions_hist: dict
        response_time_hist: dict
        response_time_traits: dict
        observed_rps: float

    def stats(self) -> TestingResults:
        status_code_hist = {}
        exceptions = {}
        response_times = []
        for response in self.responses:
            response_times.append(response.query_duration_s)

            if response.status_code not in status_code_hist:
                status_code_hist[response.status_code] = {
                    'amount': 1,
                    'msg': response.text if response.text else '<omitted>'
                }
            else:
                status_code_hist[response.status_code]['amount'] += 1

        for exception in self.exceptions:
            assert isinstance(exception, Exception)
            exception_cls_name = exception.__class__.__name__
            if exception_cls_name not in exceptions:
                exceptions[exception_cls_name] = {
                    'amount': 1,
                    'msg': str(exception)
                }
            else:
                exceptions[exception_cls_name]['amount'] += 1

        response_time_hist = {}
        response_time_traits = {}
        if response_times:
            response_time_traits = {
                'min': int(min(response_times) * 1000) / 1000,
                'max': int(max(response_times) * 1000) / 1000,
                'mean': int(statistics.mean(response_times) * 1000) / 1000,
                'median': int(statistics.median(response_times) * 1000) / 1000,
                'p99': int(sorted(response_times)[(len(response_times) * 99 // 100)] * 1000) / 1000
            }
            num_buckets = 4
            bucket_len_s = (response_time_traits['max'] * 1.05) / num_buckets
            response_time_hist = {
                f"[{(idx * bucket_len_s):.2f} - {((idx + 1) * bucket_len_s):.2f})": 0 for idx in range(num_buckets)
            }
            bucket_keys = list(response_time_hist.keys())
            for time_s in response_times:
                bucket_idx = int(time_s // bucket_len_s)
                response_time_hist[bucket_keys[bucket_idx]] += 1

        return LoadTester.TestingResults(
            status_code_hist=status_code_hist,
            exceptions_hist=exceptions,
            response_time_hist=response_time_hist,
            response_time_traits=response_time_traits,
            observed_rps=int(len(response_times) / (self._config.duration_s - 0.5) * 100) / 100)
