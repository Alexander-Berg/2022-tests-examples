function rps_limiter_section(register_only)
    return {
        register_only = register_only;
        namespace = namespace;
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
end

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
                regexp_path = {
                    async_section = {
                        pattern = "/register";
                        rps_limiter = rps_limiter_section(true);
                    };

                    regular_section = {
                        pattern = "/test";
                        rps_limiter = rps_limiter_section(false);
                    };
                }; -- regexp
            }; -- http
        }; --default
    }; --ipdispatch
}; --instance
