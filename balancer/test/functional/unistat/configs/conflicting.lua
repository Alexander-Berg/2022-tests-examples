function get_unistat_addrs()
    if conflicting_with_admin then
        return {
            { ip = "localhost"; port = stats_port };
            { ip = "localhost"; port = admin_port };
        };
    else
        return {
            { ip = "localhost"; port = stats_port };
            { ip = "localhost"; port = port };
        };
    end
end


instance = {
    thread_mode = thread_mode; set_no_file = false;

    workers = workers;
    enable_reuse_port = true;

    addrs = {
        { ip = "localhost"; port = port; };
    };

    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };

    unistat = {
        addrs = get_unistat_addrs();
    };

    log = instance_log;

    http = {
        maxreq = 64 * 1024; maxlen = 64 * 1024;

        errorlog = {
            log = error_log;
            log_level = "DEBUG";
            accesslog = {
                log = access_log;

                ipdispatch = {
                    admin = {
                        ip = "localhost"; port = admin_port;
                        admin = {
                            disable_xml_stats = disable_xml_stats;
                        };
                    };
                    default = {
                        report = {
                            uuid = "test";
                            events = {
                                stats = "report";
                            }; -- events
                            balancer2 = {
                                attempts = attempts or 2;

                                rr = {
                                    randomize_initial_state = false;
                                    {
                                        proxy = {
                                            host = "localhost"; port = backend_port;
                                            connect_timeout = "5s"; backend_timeout = backend_timeout or "5s";
                                            resolve_timeout = "1s";
                                        };
                                    };
                                }; -- balancer2/rr
                            }; -- balancer2
                        }; -- report
                    }; -- ipdispatch/default
                }; -- ipdispatch

            }; -- accesslog
        }; -- errorlog
    }; -- http
}; -- instance
