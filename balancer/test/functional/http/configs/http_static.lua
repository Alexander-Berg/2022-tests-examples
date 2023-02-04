instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs

    admin_addrs = {
         { ip = "localhost"; port = admin_port; };
    };

    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port };
        };
    };

    log = log;

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
                log_level = "DEBUG";
                http = {
                    maxlen = maxlen;
                    maxreq = maxreq;
                    maxheaders = maxheaders;
                    keepalive = keepalive;
                    keepalive_requests = keepalive_requests;
                    keepalive_timeout = keepalive_timeout;
                    keepalive_drop_probability = keepalive_drop_probability;
                    no_keepalive_file = no_keepalive_file;
                    allow_trace = allow_trace;
                    multiple_hosts_enabled = multiple_hosts_enabled;
                    stats_attr = stats_attr;
                    ban_requests_file = ban_requests_file;
                    events = {
                        stats = "report";
                    };
                    accesslog = {
                        log = accesslog;
                        errordocument = {
                            status = 200;
                        }; -- errordocument
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
