if ip == nil then ip = '127.0.0.1'; end
instance = {
    thread_mode = thread_mode; set_no_file = false;

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
            ip = "localhost";
            port = port;
            http = {
                maxlen = 65536; maxreq = 65536;
                regexp = {
                    match = {
                        match_and = {
                            {
                                match_fsm = { path = '/'; };
                            };
                            {
                                match_source_ip = { source_mask = ip; };
                            };
                        }; -- match_and
                        errordocument = {
                            status = 200;
                            content = 'match';
                        }; -- errordocument
                    }; -- match
                    default = {
                        errordocument = {
                            status = 200;
                            content = 'default';
                        }; -- errordocument
                    }; -- default
                }; -- regexp
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
