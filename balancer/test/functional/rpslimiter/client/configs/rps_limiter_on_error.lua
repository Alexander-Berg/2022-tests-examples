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
                    on_error = {
                        errordocument = {
                            status = 400;
                            content = "on_error";
                        };
                    }; -- on_error
                }; -- rps_limiter
            }; -- http
        }; --default
    }; --ipdispatch
}; --instance
