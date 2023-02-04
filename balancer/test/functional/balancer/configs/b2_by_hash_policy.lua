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

                        rr = {
                            randomize_initial_state = false;
                            first = {
                                errordocument = {
                                    status = 200;
                                    content = "first";
                                };
                            };
                            second = {
                                errordocument = {
                                    status = 200;
                                    content = "second";
                                };
                            };
                        }; -- balancer2/rr
                    }; -- balancer2
                }; -- hasher
            }; -- ipdispatch/default
        }; -- ipdispatch
    }; -- http
}; -- instance
