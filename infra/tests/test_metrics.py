from infra.orly.lib import metrics


def test_histogram():
    name = 'handle_start_operation'
    tags = [('tier', 'salt-state-apply')]
    h = metrics.Histogram(name)
    h.observe(100.0)
    h.observe(0.001)
    h.observe(0.764)
    assert h.fmt() == ['handle_start_operation_hgram',
                       [[0.01, 0], [0.025, 0], [0.05, 0], [0.1, 0], [0.25, 0], [0.5, 0], [0.75, 1], [1.0, 0], [2.5, 0],
                        [5.0, 0], [7.5, 0], [10.0, 1]]]
    h = metrics.Histogram(name, buckets=[10, 20, 30], tags=tags)
    h.observe(1)
    h.observe(23)
    assert h.fmt() == ['tier=salt-state-apply;handle_start_operation_hgram', [[10.0, 0], [20.0, 1], [30.0, 0]]]


def test_counter():
    name = 'handle_allow_request'
    tags = [('tier', 'salt-state-apply')]
    c = metrics.Counter(name, tags)
    assert c.fmt() == ['tier=salt-state-apply;handle_allow_request_dhhh', 0]
    c.inc(42)
    assert c.fmt() == ['tier=salt-state-apply;handle_allow_request_dhhh', 42]
    tags = ()
    c = metrics.Counter(name, tags)
    assert c.fmt() == ['handle_allow_request_dhhh', 0]
    c.inc(42)
    assert c.fmt() == ['handle_allow_request_dhhh', 42]


def test_get_histogram_regarding_tags():
    reg = metrics.Registry()
    reg.get_histogram('queue-wait-time', tags=(('tier', 'salt-state-apply'),))
    reg.get_histogram('queue-wait-time', tags=(('tier', 'hostman-kernel'),))
    assert len(reg._histograms) == 2
