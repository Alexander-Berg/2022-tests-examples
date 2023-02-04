instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs

    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
    };

    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port };
        };
    };

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
                accesslog = {
                    flags_getter = {
                        file_switch = file_switch;
                        service_name = service_name;
                        flags_path = flags_path;
                        flags_host = flags_host;
                        flags = {
                            proxy = {
                                host = "localhost"; port = flags_backend_port;
                                backend_timeout = "1s";
                                connect_timeout = "1s"; resolve_timeout = "1s";
                                fail_on_5xx = 1;
                            }; -- proxy
                        }; -- flags
                        proxy = {
                            host = "localhost"; port = backend_port;
                            connect_timeout = "1s"; backend_timeout = "1s";
                            resolve_timeout = "1s";
                            fail_on_5xx = 0;
                        }; -- proxy
                    }; -- flags_getter
                }; -- accesslog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
