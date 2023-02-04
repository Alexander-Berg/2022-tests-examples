function gen_backends(n)
    res = {
        max_pessimized_share = max_pessimized_share;
        min_pessimization_coeff = min_pessimization_coeff;
        increase_weight_step = increase_weight_step;
        max_skew = max_skew;
    };

    for i=0,n-1 do
        res["backend" .. i] = {
            proxy = {
                host = "localhost";
                port = _G["backend" .. i .. "_port"];
                connect_timeout = "0.3s";
                backend_timeout = "10s";
                resolve_timeout = "0.3s";
                fail_on_5xx = 0;
            };
        }
    end

    return res
end

instance = {
    backends_blacklist = backends_blacklist;
    workers = workers;
    thread_mode = thread_mode; set_no_file = false;
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
                        parameters = {"text"};
                        balancer2 = {
                            attempts = 2;
                            dynamic_hashing = gen_backends(backend_count);
                        }; -- balancer2
                    }; -- cgi_hasher
                }; -- accesslog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
