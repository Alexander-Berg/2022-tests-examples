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
                hasher = {
                    mode = "request";

                    balancer2 = {
                        by_hash_policy = {
                            unique_policy = {};
                        }; -- by_hash_policy

                        attempts = attempts;

                        rr = {
                            randomize_initial_state = false;
                            first = {
                                proxy = {
                                    host = "localhost"; port = first_backend_port;
                                    resolve_timeout = "1s"; connect_timeout = "1s";
                                    backend_timeout = "5s";
                                };
                            };
                            second = {
                                proxy = {
                                    host = "localhost"; port = second_backend_port;
                                    resolve_timeout = "1s"; connect_timeout = "1s";
                                    backend_timeout = "5s";
                                };
                            };
                        }; -- balancer2/rr
                    }; -- balancer2
                }; -- hasher
            }; -- ipdispatch/default
        }; -- ipdispatch
    }; -- http
}; -- instance