if msg == nil then msg = "Not Found"; end
if match == nil then match = "/?x=(\\d+)"; end
if checker_timeout == nil then checker_timeout = "10s"; end
instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;

            http = {
                maxlen = 64 * 1024; maxreq = 64 * 1024;
                admin = {};
            }; -- http
        }; -- admin

        test = {
            ip = "localhost";
            port = port;
            http = {
                maxlen = 64 * 1024; maxreq = 64 * 1024;
                thumbsban = {
                    id_regexp = match;
                    checker = {
                        proxy = {
                            host = "localhost"; cached_ip = "127.0.0.1"; port = checker_port;
                            connect_timeout = "0.3s"; backend_timeout = checker_timeout;
                            resolve_timeout = "1s";
                            keepalive_count = "0";
                            fail_on_5xx = 0;
                        }; -- proxy
                    }; -- checker
                    ban_handler = {
                        errordocument = {
                            status = 404;
                            content = msg;
                        }; -- errordocument
                    }; -- ban_handler
                    module = {
                        proxy = {
                            host = "localhost"; port = backend_port;
                            connect_timeout = "9s"; backend_timeout = "9s";
                            resolve_timeout = "1s";
                            fail_on_5xx = 0;
                        }; -- proxy
                    }; -- module
                }; -- thumbsban
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
