function gen_backend(port)
    return {
        proxy = {
            host = "localhost"; port = port;
            connect_timeout = "0.5s"; backend_timeout = "1s";
            resolve_timeout = "1s";
        };
    };
end

function gen_balancer(port1, port2)
    return {
        balancer2 = {
            unique_policy = {};

            attempts = 1;

            fast_attempts = fast_attempts;
            fast_503 = fast_503;

            connection_attempts = connection_attempts;

            rr = {
                randomize_initial_state = false;
                b1 = gen_backend(port1);
                b2 = gen_backend(port2);
            };
        };
    };
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
            default = {
                balancer2 = {
                    unique_policy = {};

                    fast_attempts = fast_attempts;
                    fast_503 = fast_503;

                    connection_attempts = connection_attempts;
                    attempts = 1;

                    rr = {
                        randomize_initial_state = false;
                        b1 = gen_balancer(backend_timeouted_port, backend_503_port);
                        b2 = gen_balancer(backend_timeouted_port, backend_fake_port);
                        b3 = gen_balancer(backend_fake_port, backend_503_port);
                    }; -- balancer2/rr

                    on_error = {
                        errordocument = {
                            status = 200;
                        };
                    };
                }; -- balancer2
            }; -- ipdispatch/defaul
        }; -- ipdispatch
    }; -- http
}; -- instance
