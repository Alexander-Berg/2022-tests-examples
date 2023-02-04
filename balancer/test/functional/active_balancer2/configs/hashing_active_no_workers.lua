if request == nil then request = "GET /test.html HTTP/1.1\r\n\r\n"; end


function gen_hashing()
    return {
        request = request;
        delay = "1s";
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
            attempts = 1;

            active_policy = {
                unique_policy = {};
            };

            hashing = gen_hashing();
        };
    };
end

instance = {
    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs

    admin_addrs = {
         { ip = "localhost"; port = admin_port; };
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
            http = {
                maxlen = 64 * 1024; maxreq = 64 * 1024;
                hasher = gen_hasher();
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
