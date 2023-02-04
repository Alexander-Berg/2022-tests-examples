if delay == nil then delay = "1s"; end
if request == nil then request = "GET /test.html HTTP/1.1\r\n\r\n"; end
if attempts == nil then attempts = 1; end


function gen_hashing()
    return {
        request = request;
        delay = delay;
        steady = steady;
        {
            weight = 1;
            proxy = {
                host = "localhost"; port = backend_port1;
                connect_timeout = "0.3s"; backend_timeout = "0.5s";
                resolve_timeout = "0.3s";
            }; -- proxy
        };
        {
            weight = 1;
            proxy = {
                host = "localhost"; port = backend_port2;
                connect_timeout = "0.3s"; backend_timeout = "0.5s";
                resolve_timeout = "0.3s";
            }; -- proxy
        };
    }; -- hashing
end

function gen_hasher()
    return {
        mode = "request";
        balancer2 = {
            attempts = attempts;

            active_policy = {
                skip_attempts = active_skip_attempts;
                unique_policy = {};
            };

            hashing = gen_hashing();
        };
    }; -- hasher
end

instance = {
    workers = 1;
    worker_start_duration = "1s";

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
        --    { ip = "localhost"; port = admin_port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs
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
            http = {
                maxlen = 64 * 1024; maxreq = 64 * 1024;
                hasher = gen_hasher(); -- hasher
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
