if delay == nil then delay = "1s"; end
if request == nil then request = "GET /test.html HTTP/1.1\r\n\r\n"; end

if use_balancer2 and use_balancer2 == "true" then
    use_balancer2 = true;
else
    use_balancer2 = false;
end


function gen_hashing()
    return {
        request = request;
        delay = delay;
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
    };
end

function gen_on_error()
    return {
        errordocument = {
            status = 503;
            content = 'Error';
        }; -- errordocument
    };
end

function gen_hasher()
    return {
        mode = "request";
        balancer2 = {
            attempts = 1;
            active_policy = {
                unique_policy = {};
            };
            hashing = gen_hashing();
            on_error = gen_on_error();
        }; -- balancer2
    };
end

instance = {
    workers = 1;
    worker_start_duration = "1s";

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
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
            http = {
                maxlen = 64 * 1024; maxreq = 64 * 1024;
                hasher = gen_hasher();
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
