backend_host = backend_host or "localhost"
instance = {
    state_directory = state_directory;
    workers = workers;
    thread_mode = thread_mode; set_no_file = false;
    backends_blacklist = backends_blacklist;
    dynamic_balancing_log = dynamic_balancing_log;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs
    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port; }
        };
    }; -- unistat

    ipdispatch = {
        default = {
            http = {
                maxlen = 65536; maxreq = 65536;
                accesslog = {
                    log = access_log;
                    balancer2 = {
                        attempts = 2;
                        dynamic = {
                            max_pessimized_share = max_pessimized_share;
                            min_pessimization_coeff = min_pessimization_coeff;
                            weight_increase_step = weight_increase_step;
                            disable_defaults = disable_defaults;
                            active = {
                                request = request;
                                tcp_check = tcp_check;
                                delay = delay;
                                use_backend_weight = use_backend_weight;
                                weight_normalization_coeff = weight_normalization_coeff;
                            };

                            first = {
                                proxy = {
                                    host = "localhost"; port = backend0_port;
                                    connect_timeout = "0.3s"; backend_timeout = 10;
                                    resolve_timeout = "0.3s";
                                    fail_on_5xx = 0;
                                    keepalive_count = keepalive_count;
                                }; -- proxy
                            };

                            second = {
                                proxy = {
                                    host = "localhost"; port = backend1_port;
                                    connect_timeout = "0.3s"; backend_timeout = 10;
                                    resolve_timeout = "0.3s";
                                    fail_on_5xx = 0;
                                    keepalive_count = keepalive_count;
                                }; -- proxy
                            };
                        }; -- rr
                    }; -- balancer2
                }; -- accesslog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
