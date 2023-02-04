instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;

            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            }; -- http
        }; -- admin

        test = {
            ip = "localhost";
            port = port;

            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                thumb_consistent_hash = {
                    id_regexp = id_regexp;
                    [first] = {
                        errordocument = {
                            status = 200;
                            content = first;
                        }; -- errordocument
                    }; -- first
                    [second] = {
                        errordocument = {
                            status = 200;
                            content = second;
                        }; -- errordocument
                    }; -- second
                    default = {
                        errordocument = {
                            status = 503;
                            content = 'default';
                        }; -- errordocument
                    }; -- default
                }; -- thumb_consistent_hash
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
