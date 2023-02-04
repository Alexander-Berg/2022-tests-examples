function prepare_blacklist(str)
    if not str then
        return nil
    end

    local retval = {}
    for w in string.gmatch(str, ",?([^,]+)") do
        retval[#retval + 1] = w
    end
    return retval
end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    log = log;

    buffer = buffer;
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
                    errorlog = {
                        log = errorlog;
                        accesslog = {
                            log = accesslog;
                            proxy = {
                                host = "localhost"; port = backend_port;
                                status_code_blacklist = prepare_blacklist(status_code_blacklist);
                                status_code_blacklist_exceptions = prepare_blacklist(status_code_blacklist_exceptions);
                                watch_client_close = watch_client_close;
                            }; -- proxy
                        }; -- accesslog
                    }; -- errorlog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
