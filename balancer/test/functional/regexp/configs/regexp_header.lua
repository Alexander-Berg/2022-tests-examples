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
                        match_fsm = {
                            header = {
                                name = "Led"; value = "Zeppelin";
                                name_surround = name_surround;
                            };
                            case_insensitive = case_insensitive;
                            surround = surround;
                        };
                        priority = 2;
                        errordocument = {
                            status = 200;
                            content = 'match';
                        }; -- errordocument
                    }; -- match
                    led_anyway = {
                        match_fsm = {
                            header = {
                                name = name;
                                value = ".*"
                            };
                            case_insensitive = true;
                        };
                        priority = 1;
                        errordocument = {
                            status = 200;
                            content = "Stairway";
                        };
                    }; -- led_anyway
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
