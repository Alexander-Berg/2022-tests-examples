instance = {
    thread_mode = thread_mode;

    addrs = {
        { ip = "localhost"; port = port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs
    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port; };
        }; -- addrs
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
            errorlog = {
                log = errorlog;
                log_level = "DEBUG";
                http = {
                    maxlen = 65536; maxreq = 65536;
                    accesslog = {
                        log = accesslog;
                        remote_log = {
                            delay = delay;
                            uaas_mode = uaas_mode;
                            no_remote_log_file = no_remote_log_file;
                            level_compress_file = level_compress_file;
                            queue_limit = queue_limit;
                            remote_log_storage = {
                                proxy = {
                                    host = "localhost";
                                    port = storage_port;
                                    connect_timeout = "1s";
                                    backend_timeout = "1s";
                                    resolve_timeout = "1s";
                                }; -- proxy
                            }; -- remote_log_storage
                            proxy = {
                                host = "localhost";
                                connect_timeout = "1s";
                                backend_timeout = "1s";
                                resolve_timeout = "1s";
                                port = backend_port;
                            }; -- proxy
                        }; -- remote_log
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
