function gen_balancer(uuid, attempts, rr)
    params = {
        uuid = uuid;
        events = {
            stats = "report";
        };

        attempts = 3;

        rr = rr;
    }

    return {
        balancer2 = params;
    }
end

instance = {
    set_no_file = false;
    addrs = {
        { ip = "localhost"; port = port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };

    http = {
        maxreq = 64 * 1024; maxlen = 64 * 1024;
        ipdispatch = {
            admin = {
                ip = "localhost"; port = admin_port;
                admin = {};
            };
            default = gen_balancer(uuid, attempts, {
                randomize_initial_state = false;
                backend = {
                    proxy = {
                        host = "yandex.invalid"; port = backend_port;
                    }; -- balancer2/rr/backend/proxy
                    weight = 3;
                }; -- balancer2/rr/backend
                second_backend = {
                    proxy = {
                        host = "localhost"; port = second_backend_port;
                    }; -- balancer2/rr/backend/proxy
                    weight = 2;
                }; -- balancer2/rr/backend
                third_backend = {
                    proxy = {
                        host = "localhost"; port = third_backend_port;
                    }; -- balancer2/rr/backend/proxy
                    weight = 1;
                }; -- balancer2/rr/backend
            }); -- ipdispatch/default
        }; -- ipdispatch
    }; -- http
}; -- instance
