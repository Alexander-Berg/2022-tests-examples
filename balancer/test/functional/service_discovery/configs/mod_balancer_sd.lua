sd_template = {
    proxy_options = {
        connect_timeout = "0.3s"; backend_timeout = 10;
        resolve_timeout = "0.3s";
        fail_on_5xx = 0;
    };

    termination_delay = termination_delay;
};

if backends_file ~= "" then
    sd_template.endpoint_sets = {
        { backends_file = backends_file; };
    };
elseif tonumber(endpoint_sets_count) > 0 then
    sd_template.endpoint_sets = {};
    for i = 1, endpoint_sets_count do
        sd_template.endpoint_sets[i] = {
            cluster_name = "test-cluster" .. tostring(i);
            endpoint_set_id = "test-service" .. tostring(i);
        };
    end
else
    sd_template.backends = {
        { host = "localhost"; cached_ip = "127.0.0.1"; port = backend1_port; };
        { host = "localhost"; cached_ip = "127.0.0.1"; port = backend2_port; };
    };
end

sd_template[algo] = algo_options or {};

if algo == "active" then
    sd_template.active = {
        request = "GET /check HTTP/1.1\r\nHost: localhost\r\n\r\n";
        delay = "50ms";
        steady = true;
        quorum = active_quorum;
        hysteresis = active_hysteresis;
    };
elseif algo == "dynamic" and dynamic_active then
    sd_template.dynamic.active = {
        request = "GET /not-a-check HTTP/1.1\r\nHost: localhost\r\n\r\n";
        delay = "50ms";
    }
end

if max_pessimized_share then
    sd_template[algo].max_pessimized_share = max_pessimized_share
end


instance = {
    dynamic_balancing_log = dynamiclog;
    thread_mode = thread_mode; set_no_file = false;
    workers = 10;
    worker_start_delay = "10ms";
    dns_ttl = "5h";
    sd = {
        update_frequency = "10ms";
        request_timeout = "10s";
        connect_timeout = "1s";
        cache_dir = cache_dir;
        client_name = "balancer_functional_test";
        log = sd_log;
        allow_empty_endpoint_sets = allow_empty_endpoint_sets;
    };
    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port; };
        };
    };

    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;
            http = {
                maxlen = 65536; maxreq = 65536;
                admin = {};
            }; -- http
        }; -- admin
        test = {
            http = {
                maxlen = 65536; maxreq = 65536;
                errorlog = {
                    log = errorlog;
                    accesslog = {
                        log = accesslog;
                        report = {
                            uuid = "default";
                            hasher = {
                                mode = "request";

                                balancer2 = {
                                    attempts = attempts;
                                    connection_attempts = connection_attempts;
                                    sd = sd_template;
                                };
                            };
                        }; -- report
                    }; -- accesslog
                }; -- errorlog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

if sd_port then
    instance.sd.host = sd_host;
    instance.sd.cached_ip = sd_cached_ip;
    instance.sd.port = sd_port;
else
    instance.sd.host = "";
    instance.sd.port = 0;
end

