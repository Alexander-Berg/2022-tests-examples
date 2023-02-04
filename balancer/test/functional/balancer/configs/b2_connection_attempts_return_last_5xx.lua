function gen_backend(port)
    return {
        proxy = {
            host = "localhost"; port = port;
            connect_timeout = "5s"; backend_timeout = "5s";
            resolve_timeout = "1s";
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
                    by_name_policy = {
                        name = "backend";
                        unique_policy = {};
                    };

                    status_code_blacklist = {
                        "5xx";
                    };
                    return_last_5xx = return_last_5xx;

                    fast_attempts = fast_attempts;
                    fast_503 = fast_503;

                    connection_attempts = connection_attempts;

                    attempts = attempts or 1;

                    rr = {
                        randomize_initial_state = false;
                        backend_503_1 = gen_backend(backend_503_1_port);
                        backend_503_1 = gen_backend(backend_503_2_port);
                        backend_503_1 = gen_backend(backend_503_3_port);
                        backend_last_200 = gen_backend(backend_last_200_port);
                    }; -- balancer2/rr
                }; -- balancer2
            }; -- ipdispatch/defaul
        }; -- ipdispatch
    }; -- http
}; -- instance
