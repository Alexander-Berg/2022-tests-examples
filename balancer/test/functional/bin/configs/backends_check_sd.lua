function gen_algo()
    if algo == "active" then
        return { tcp_check = 1; delay = "1s"; };
    else
        return {};
    end
end

function gen_endpoint_sets()
    if endpoint_sets then
        res = {};
        for i = 1, endpoint_sets or 0 do
            res[i] = {
                cluster_name = "cluster" .. tostring(i);
                endpoint_set_id = "service" .. tostring(i);
            };
        end
        return res
    elseif backends_file then
        return {{ backends_file = backends_file; }};
    end
    return nil
end

function gen_backends()
    res = {};
    for i = 1, real or 0 do
        res[i] = { host = 'localhost'; port = _G['real' .. i .. '_port']; };
    end
    for i = 1, fake or 0 do
        res[real + i] = { host = 'localhost'; port = _G['fake' .. i .. '_port']; };
    end
    if algo == "active" then
        res.tcp_check = 1;
        res.delay = "1s";
    end
    return res
end

instance = {
    thread_mode = thread_mode;
    set_no_file = false;
    workers = 10;
    worker_start_delay = "10ms";
    state_directory = "./state";
    dns_ttl = "5h";

    config_check = {};

    sd = {
        update_frequency = "10ms";
        request_timeout = "10s";
        connect_timeout = "1s";
        cache_dir = sd_cache;
        client_name = "balancer_functional_test";
        log = sdlog;
        host = "localhost";
        port = sd_port_override or sd_port;
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
        test = {
            balancer2 = {
                check_backends = {
                    quorum = quorum;
                    name = "sd-group"
                };
                sd = {
                    proxy_options = {
                        connect_timeout = "50ms";
                        backend_timeout = "100s";
                        resolve_timeout = "1s";
                    };
                    endpoint_sets = gen_endpoint_sets();
                    backends = gen_backends();
                    [algo] = gen_algo();
                }; -- sd
            }; -- balancer2
        }; -- test
    }; -- ipdispatch
}; -- instance
