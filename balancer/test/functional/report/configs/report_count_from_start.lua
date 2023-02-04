if ranges == nil then ranges = "10s"; end
if backend_timeout == nil then backend_timeout = "5s"; end
if keepalive == nil then keepalive = 1; end

function convert_bool_or_nil(val)
    if val == nil then
        return val
    elseif val == "false" then
        return false
    else
        return true
    end
end

function create_router(options)
    return {
        route = "/" .. options.name;
        report = {
            uuid = options.name;
            ranges = legacy_ranges;
            refers = options.refers;
            count_start_from_module_entrance = options.count_start_from_module_entrance;
            events = {
                stats = "report";
            }; -- events
            errordocument = {
                status = 200;
                content = options.name;
            };
        };
    }
end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs

    admin_addrs = {
         { ip = "localhost"; port = admin_port; };
    };

    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port };
        };
    };

    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;
            http = {
                maxlen = 65536; maxreq = 65536;
                admin = {}; -- admin
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
                    keepalive = keepalive;
                    accesslog = {
                        log = accesslog;
                        cutter = {
                            timeout = cutter_timeout or "500ms";
                            bytes = cutter_bytes or 512;
                            prefix_path_router = {
                                referent = create_router {
                                    name = "referent";
                                    count_start_from_module_entrance = convert_bool_or_nil(referent_count_start);
                                }; -- referent
                                default = {
                                    report = {
                                        uuid = "default";
                                        ranges = legacy_ranges;
                                        backend_time_ranges = backend_time_ranges;
                                        refers = default_refers;
                                        count_start_from_module_entrance = convert_bool_or_nil(default_count_start);
                                        events = {
                                            stats = "report";
                                        }; -- events
                                        errordocument = {
                                            status = 200;
                                            content = "default";
                                        };
                                    };
                                }; -- default
                            }; -- prefix_path_router
                        }; -- cutter
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
