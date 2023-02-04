instance = {
    set_no_file = false;
    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs
    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port; };
        };
    };

    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;
            http = {
                maxlen = 65536; maxreq = 65536;
                admin = {};
            }; -- http
        }; -- admin
        test = {
            ip = "localhost";
            port = port;
            http = {
                maxlen = 65536; maxreq = 65536;
                errorlog = {
                    log = errorlog;
                    balancer2 = {
                        attempts = 1;
                        rr = {
                            randomize_initial_state = false;
                            weights_file = weights_file;
                            {
                                weight = 2.0;
                                errordocument = {
                                    status = 200;
                                    content = "id 0";
                                }; -- errordocument
                            }, -- backend 0
                            {
                                weight = 1.0;
                                errordocument = {
                                    status = 200;
                                    content = "id 1";
                                }; -- errordocument
                            }, -- backend 1
                            {
                                weight = 0.0;
                                errordocument = {
                                    status = 200;
                                    content = "id 2";
                                }; -- errordocument
                            }, -- backend 2
                        }; -- rr
                    }; -- balancer2
                }; -- errorlog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
