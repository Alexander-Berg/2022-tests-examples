instance = {
    thread_mode = thread_mode; set_no_file = false;

    workers = workers;

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
                allow_trace = true;
                regexp = {
                    get = {
                        match_if_file_exists = {
                            path = path
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
            }; -- test
        }; -- http
    }; -- ipdispatch
}; -- instance
