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
                    errors_only = errors_only;
                    logged_share = logged_share;
                    prefix_path_router = {
                        case_insensitive = case_insensitive;
                        ok = {
                            route = "/200";
                            errordocument = {
                                status = 200;
                                content = "200";
                            };
                        };
                        fail = {
                            route = "/404";
                            errordocument = {
                                status = 404;
                                content = "404";
                            };
                        };
                        default = {
                            errordocument = {
                                status = 200;
                                content = "default";
                            };
                        };
                    }; -- prefix_path_router
                }; -- accesslog
            }; -- http
        }; -- ipdispatch/test
    }; -- ipdispatch
}; -- instance
