maxreq = maxreq or 64 * 1024
maxlen = maxlen or 64 * 1024

if custom_accesslog ~= nil then
    accesslog = custom_accesslog
end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    unistat = {
        addrs = {
            {
                ip = "localhost";
                port = stats_port;
            };
        };
        hide_legacy_signals = true;
    }; -- unistat
    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs
    ipdispatch = {
        admin = {
            ip = "localhost"; port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            }; -- http
        }; -- ipdispatch/admin
        test = {
            http = {
                maxreq = maxreq; maxlen = maxlen;
                accesslog = {
                    log = accesslog;
                    additional_ip_header = additional_ip_header;
                    additional_port_header = additional_port_header;
                    log_instance_dir = log_instance_dir;
                    proxy = {
                        host = "localhost"; port = backend_port;
                        connect_timeout = "0.3s"; backend_timeout = "10s";
                        resolve_timeout = "0.3s";
                    }; -- proxy
                }; -- accesslog
            }; -- http
        }; -- ipdispatch/test
    }; -- ipdispatch
}; -- instance
