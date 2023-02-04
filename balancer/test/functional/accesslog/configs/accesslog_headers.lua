instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs
    ipdispatch = {
        admin = {
            ip = "localhost"; port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            }; -- http
        }; -- ipdispatch/admin
        test = {
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                accesslog = {
                    log = accesslog;
                    additional_ip_header = additional_ip_header;
                    additional_port_header = additional_port_header;
                    headers = {
                        create = {
                            additional_ip_header = "created_ip";
                            additional_port_header = "created_port";
                        };
                        proxy = {
                            host = "localhost"; port = backend_port;
                            connect_timeout = "0.3s"; backend_timeout = "10s";
                            resolve_timeout = "1s";
                        }; -- proxy
                    };
                }; -- accesslog
            }; -- http
        }; -- ipdispatch/test
    }; -- ipdispatch
}; -- instance
