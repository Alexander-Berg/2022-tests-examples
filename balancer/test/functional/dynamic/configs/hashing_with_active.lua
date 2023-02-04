function gen_backends(n)
    res = {
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
            use_backends_grouping = use_backends_grouping;
        };
    };

    for i=0,n-1 do
        res["backend" .. i] = {
            proxy = {
                host = "localhost";
                port = _G["backend" .. i .. "_port"];
                connect_timeout = "0.3s";
                backend_timeout = 10;
                resolve_timeout = "0.3s";
                fail_on_5xx = 0;
                keepalive_count = keepalive_count;
            };
        }
    end

    return res
end

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
                    cgi_hasher = {
                        parameters = {"text"},
                        balancer2 = {
                            attempts = 2;
                            dynamic_hashing = gen_backends(backend_count);
                            by_group_name_from_header_policy = {
                                unique_policy = {
                                };
                                header_name = 'X-Group';
                            };
                        }; -- balancer2
                    }; -- cgi_hasher
                }; -- accesslog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
