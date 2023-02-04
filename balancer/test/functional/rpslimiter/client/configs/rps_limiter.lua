instance = {
    thread_mode = thread_mode; set_no_file = false;
    addrs = {
        { ip = "localhost"; port = port; }
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; }
    };
    ipdispatch = {
        default = {
            http = {
                maxlen = 65536; maxreq = 65536;
                rps_limiter = {
                    disable_file = disable_file;
                    skip_on_error = skip_on_error;
                    quota_name = quota_name;
                    namespace = namespace;
                    log_quota = log_quota;
                    checker = {
                        proxy = {
                            host = "localhost"; port = checker_port;
                            connect_timeout = "10s"; backend_timeout = "10s"; resolve_timeout = "10s";
                        }; -- proxy
                    }; --checker
                    module = {
                        errordocument = {
                            status = 200;
                            content = "module";
                        };
                    }; --module
                }; -- rps_limiter
            }; -- http
        }; --default
    }; --ipdispatch
}; --instance
