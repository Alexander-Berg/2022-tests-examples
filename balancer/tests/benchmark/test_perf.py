import yatest


def test_perf_request(metrics):
    metrics.set_benchmark(yatest.common.execute_benchmark("balancer/kernel/http/parser/tests/benchmark/request/request", budget=10))


def test_perf_response(metrics):
    metrics.set_benchmark(yatest.common.execute_benchmark("balancer/kernel/http/parser/tests/benchmark/response/response", budget=10))


def test_perf_requestline(metrics):
    metrics.set_benchmark(yatest.common.execute_benchmark("balancer/kernel/http/parser/tests/benchmark/requestline/requestline"))
