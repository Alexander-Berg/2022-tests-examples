instance = {
    thread_mode = thread_mode; set_no_file = false;

    events = {
        stats = "report";
    }; -- events

    log = instance_log;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; };
    }; -- admin_addrs
    unistat = {
        addrs = {
            ip = "localhost"; port = stats_port;
        };
    }; -- unistat

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
                errordocument = {
                    status = 200;
                    content = "ok";
                }; -- errordocument
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
