if regexp == nil then regexp = ".*"; end
if rewrite == nil then rewrite = "%0"; end
if literal == nil then literal = 0; end;
if case == nil then case = 0; end
if glob == nil then glob = 0; end
instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB: "" ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
    }; -- admin_addrs

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
                    maxlen = 65536; maxreq = 65536;
                    rewrite = {
                        actions = {
                            {
                                regexp = regexp;
                                rewrite = rewrite;
                                literal = literal;
                                case_insensitive = case;
                                header_name = header_name;
                                global = glob;
                                split = split;
                            };
                        }; -- actions

                        accesslog = {
                            log = accesslog;
                            proxy = {
                                host = "localhost"; port = backend_port;
                                connect_timeout = "0.3s"; backend_timeout = "10s";
                                resolve_timeout = "1s";
                                fail_on_5xx = 0;
                            }; -- proxy
                        }; -- accesslog
                    }; -- rewrite
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
