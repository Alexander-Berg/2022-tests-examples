if delay == nil then delay = "1s"; end
if request == nil then request = "GET /test.html HTTP/1.1\r\n\r\n"; end


function gen_active()
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

function gen_http()
    return {
        maxlen = 64 * 1024; maxreq = 64 * 1024;
        balancer2 = {
            attempts = 1;
            unique_policy = {};
            active = gen_active();
            on_error = gen_on_error();
        };
    };
end

instance = {
    workers = 1;
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
            http = gen_http(); -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
