instance = {
    thread_mode = thread_mode; set_no_file = false;
    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs
    buffer = buffer;
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
            errorlog = {
                log = errorlog;
                accesslog = {
                    log = accesslog;
                    report = {
                        ranges = "7ms,11ms,17ms,26ms,39ms,58ms,87ms,131ms,197ms,296ms,444ms,666ms,1000ms,1500ms,2250ms,3375ms,5062ms,7593ms,11390ms,17085ms";
                        input_size_ranges = "1,5,10,500,1000"; output_size_ranges = "1,5,10,500,1000";
                        disable_robotness = true; disable_sslness = true;
                        events = {
                            stats = "report";
                        }; -- events
                        proxy = {
                            host = "::1"; port = backend_port;
                        }; -- proxy
                    }; -- report
                }; -- accesslog
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
