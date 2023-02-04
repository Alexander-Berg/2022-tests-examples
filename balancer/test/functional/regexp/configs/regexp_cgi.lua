if name == nil then name = "Led"; end
instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
    }; -- admin_addrs

    ipdispatch = {
        test = {
            ip = "localhost";
            port = port;
            http = {
                maxlen = 65536; maxreq = 65536;
                regexp = {
                    match = {
                        match_fsm = {
                            cgi = cgi;
                        };
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
