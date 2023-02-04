if delay == nil then delay = "1s"; end
if attempts == nil then attempts = 1; end
if pinger == nil then pinger = false; end

if weight1 == nil then weight1 = 1; end
if weight2 == nil then weight2 = 1; end

function gen_active()
    return {
        uuid = "default";
        request = request;
        tcp_check = tcp_check;
        delay = delay;
        steady = steady;
        quorum = quorum;
        hysteresis = hysteresis;
        use_backend_weight = use_backend_weight;
        backend_weight_disable_file = backend_weight_disable_file;
        {
            weight = weight1;
            proxy = {
                host = "localhost"; port = backend_port1;
                connect_timeout = "0.3s"; backend_timeout = "0.5s";
                resolve_timeout = "0.3s";
                keepalive_count = keepalive_count;
            }; -- proxy
        };
        {
            weight = weight2;
            proxy = {
                host = "localhost"; port = backend_port2;
                connect_timeout = "0.3s"; backend_timeout = "0.5s";
                resolve_timeout = "0.3s";
                keepalive_count = keepalive_count;
            }; -- proxy
        };
    };
end

function gen_http()
    return {
        maxlen = 64 * 1024; maxreq = 64 * 1024;
        balancer2 = {
            attempts = attempts;

            active_policy = {
                skip_attempts = active_skip_attempts;
                unique_policy = {};
            };

            active = gen_active();
        }; -- balancer2
    };
end

instance = {
    workers = workers;
    set_no_file = false;
    worker_start_duration = "1s";
    worker_start_delay = "10ms";
    pinger_required = pinger;
    pinger_state_directory = "pingerfolder";
    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
        --    { ip = "localhost"; port = admin_port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs
    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port };
        };
    };

    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;
            http = {
                maxlen = 64 * 1024; maxreq = 64 * 1024;
                admin = {};
            }; -- http
        }; -- admin
        test = {
            ip = "localhost";
            port = port;
            http = gen_http(); -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

if connection_manager_required == 'true' then instance.connection_manager = {}; end
