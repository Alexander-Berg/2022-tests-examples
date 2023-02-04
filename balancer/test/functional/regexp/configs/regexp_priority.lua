if priority1 == nil then priority1 = 3.0; end
if priority2 == nil then priority2 = 2.0; end
if priority3 == nil then priority3 = 1.0; end
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
                    match1 = {
                        match_fsm = { path = '/very_long_uri'; case_insensitive = true; };
                        priority = priority1;
                        errordocument = {
                            status = 200;
                            content = 'match1';
                        }; -- errordocument
                    }; -- match1
                    match2 = {
                        match_fsm = { path = '/very_.*_uri'; case_insensitive = true; };
                        priority = priority2;
                        errordocument = {
                            status = 200;
                            content = 'match2';
                        }; -- errordocument
                    }; -- match2
                    match3 = {
                        match_fsm = { path = '/.*_uri'; case_insensitive = true; };
                        priority = priority3;
                        errordocument = {
                            status = 200;
                            content = 'match3';
                        }; -- errordocument
                    }; -- match3
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
