function gen_config_check()
    if mode == 'extended' then
        return {
            quorums_file = quorums_file;
            skip_same_groups = skip_same_groups;
        };
    end
    return nil
end

function gen_algo(real_count, fake_count)
    res = {};
    for i = 1, real_count do
        res[i] = {
            proxy = {
                host = real_host or 'localhost';
                port = real_port;
                connect_timeout = "100ms";
                backend_timeout = "1s";
                resolve_timeout = "100ms";
            };
        };
    end
    for i = 1, fake_count do
        res[real_count + i] = {
            proxy = {
                host = fake_host or 'localhost';
                port = fake_port;
                connect_timeout = "1ms";
                backend_timeout = "1s";
                resolve_timeout = "1ms";
            };
        };
    end
    if algo == "active" then
        res.tcp_check = 1;
        res.delay = "1s";
    end
    return res
end


function gen_groups(skip)
    return {
        {
            shared = {
                uuid = "shared";
                balancer2 = {
                    check_backends = {
                        quorum = quorum1;
                        amount_quorum = amount_quorum1;
                        name = group1;
                        skip = skip;
                    };
                    [algo] = gen_algo(real1, fake1);
                };
            }; -- shared
        };
        {
            balancer2 = {
                check_backends = {
                    quorum = quorum2;
                    amount_quorum = amount_quorum2;
                    name = group2;
                    skip = skip;
                };
                [algo] = gen_algo(real2, fake2);
            }; -- balancer2
        };
        {
            shared = {
                uuid = "shared";
            }; -- shared
        };
    };
end


function gen_top_level()
    res = {};
    if root_quorum == nil and root_amount_quorum == nil then
        res = {
            rr = gen_groups(false);
        };
        res.rr.randomize_initial_state = false;
    else
        res = {
            by_location = gen_groups(true);
            check_backends = {
                quorum = root_quorum;
                amount_quorum = root_amount_quorum;
                name = root;
            };
        };
    end
    return res
end


instance = {
    thread_mode = thread_mode;
    set_no_file = false;
    workers = 10;
    worker_start_delay = "10ms";
    state_directory = "./state";

    dns_async_resolve = dns_async_resolve;
    dns_timeout = "100ms";
    dns_ip = dns_ip;
    dns_port = dns_port;
    dns_ttl = "5h";

    log = childlog;

    config_check = gen_config_check();

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
            http = {
                balancer2 = gen_top_level();
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
