if ranges == nil then ranges = "10s"; end
if backend_timeout == nil then backend_timeout = "5s"; end
if keepalive == nil then keepalive = 1; end
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

    ipdispatch = {
        test = {
            ip = "localhost";
            port = port;
            errorlog = {
                log = errorlog;
                log_level = "DEBUG";
                http = {
                    maxlen = 65536; maxreq = 65536;
                    keepalive = keepalive;
                    accesslog = {
                        log = accesslog;
                        antirobot = {
                            checker = {
                                proxy = {
                                    host = "localhost"; port = checker_port;
                                    connect_timeout = "0.3s"; backend_timeout = "10s";
                                    resolve_timeout = "1s";
                                    fail_on_5xx = 0;
                                }; -- proxy
                            }; -- checker
                            module = {
                                report = {
                                    uuid = "default";
                                    ranges = ranges;
                                    events = {
                                        stats = "report";
                                    }; -- events
                                    proxy = {
                                        host = "localhost"; port = backend_port;
                                        connect_timeout = "0.3s"; backend_timeout = backend_timeout;
                                        resolve_timeout = "1s";
                                        fail_on_5xx = 0;
                                    }; -- proxy
                                }; -- report
                            }; -- module
                        }; -- antirobot
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
