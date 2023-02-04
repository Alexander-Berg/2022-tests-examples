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
                    allow_webdav = allow_webdav;
                    allow_webdav_file = allow_webdav_file;
                    multiple_hosts_enabled = multiple_hosts_enabled;
                    stats_attr = stats_attr;
                    allow_client_hints_restore = allow_client_hints_restore;
                    client_hints_ua_header = client_hints_ua_header;
                    client_hints_ua_proto_header = client_hints_ua_proto_header;
                    disable_client_hints_restore_file = disable_client_hints_restore_file;
                    events = {
                        stats = "report";
                    };
                    accesslog = {
                        log = accesslog;
                        proxy = {
                            host = 'localhost'; port = backend_port;
                            keepalive_count = keepalive_count;
                            connect_timeout = "1s"; resolve_timeout = "1s";
                            backend_read_timeout = "5s"; client_read_timeout="5s";
                            allow_connection_upgrade = true;
                            allow_connection_upgrade_without_connection_header = allow_connection_upgrade_without_connection_header;
                        }; -- proxy
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
