backend_timeout = backend_timeout or "10s";

if default_tcp_rst_on_error ~= nil then
    if default_tcp_rst_on_error == "false" or default_tcp_rst_on_error == "0" then
        default_tcp_rst_on_error = false;
    else
        default_tcp_rst_on_error = true;
    end
end

if send_rst ~= nil then
    if send_rst == "false" or default_tcp_rst_on_error == "0" then
        send_rst = false;
    else
        send_rst = true;
    end
end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    log = instance_log;
    default_tcp_rst_on_error = default_tcp_rt_on_error;

    ipdispatch = {
        admin = {
            ip = "localhost"; port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {}
            }; -- http
        }; -- admin
        test = {
            errorlog = {
                log = errorlog;
                log_level = "DEBUG";
                http = {
                    maxreq = 64 * 1024; maxlen = 64 * 1024;
                    tcp_rst_on_error = {
                        send_rst = send_rst;
                        accesslog = {
                            log = accesslog;
                            proxy = {
                                host = "localhost"; port = backend_port;
                                connect_timeout = connect_timeout or "500ms";
                                backend_timeout = backend_timeout;
                                resolve_timeout = "1s";
                            }; -- proxy
                        }; -- accesslog
                    }; -- tcp_rst_on_error
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
