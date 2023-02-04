instance = {
    set_no_file = false;
    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
    }; -- admin_addrs

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
            http = {
                maxlen = 65536; maxreq = 65536;
                errorlog = {
                    log = errorlog;
                    balancer2 = {
                        weighted2 = {
                            weights_file = weights_file;
                            slow_reply_time = slow_reply_time;
                            {
                                weight = 1;
                                errordocument = {
                                    status = 200;
                                    content = "Led";
                                }; -- errordocument
                            };
                            {
                                weight = 1;
                                errordocument = {
                                    status = 200;
                                    content = "Zeppelin";
                                }; -- errordocument
                            };
                        }; -- weighted2
                    }; -- balancer2
                }; -- errorlog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
