if ranges == nil then ranges = "10s"; end
if backend_timeout == nil then backend_timeout = "5s"; end
if client_read_timeout == nil then client_read_timeout = "1s"; end
if client_write_timeout == nil then client_write_timeout = "1s"; end
if keepalive == nil then keepalive = 1; end
if keepalive_count == nil then keepalive_count = 0; end

function gen_matcher_map()
    common_part = "(www[.]|m[.]|)yandex[.]";
    return {
        ru = { match_fsm = { host = common_part .. "(ru|by|ua|kz)"; case_insensitive = true; }; };
        com = { match_fsm = { host = common_part .. "com"; case_insensitive = true; }; };
        com_tr = { match_fsm = { host = common_part .. "com[.]tr"; case_insensitive = true; }; };
        foo = { match_fsm = { path = "/foo.*"; }; };
        foobar = { match_fsm = { path = "/foo/bar.*"; }; };
        bar = { match_fsm = { path = "/bar.*"; }; };
        upgrade_xxx = { match_fsm = { upgrade = "xxx"; }; };
        upgrade = { match_fsm = { upgrade = ".*"; }; };
    }; -- fsm_map
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
        hide_legacy_signals = hide_legacy_signals;
    };

    buffer = buffer;
    ipdispatch = {
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
                        report = {
                            uuid = "default";
                            all_default_ranges = all_default_ranges;
                            ranges = legacy_ranges;
                            backend_time_ranges = backend_time_ranges;
                            client_fail_time_ranges = client_fail_time_ranges;
                            input_size_ranges = input_size_ranges;
                            output_size_ranges = output_size_ranges;
                            disable_robotness = disable_robotness;
                            disable_sslness = disable_sslness;
                            matcher_map = gen_matcher_map();
                            outgoing_codes = outgoing_codes;
                            disable_signals = disable_signals;
                            enable_signals = enable_signals;
                            signal_set = signal_set;
                            events = {
                                stats = "report";
                            }; -- events
                            proxy = {
                                host = host; port = backend_port;
                                connect_timeout = connect_timeout;
                                backend_timeout = backend_timeout;
                                client_read_timeout = client_read_timeout;
                                client_write_timeout = client_write_timeout;
                                resolve_timeout = "1s";
                                fail_on_5xx = fail_on_5xx;
                                allow_connection_upgrade = true;
                                keepalive_count = keepalive_count;
                            }; -- proxy
                        }; -- report
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance

if connection_manager_required == 'true' then instance.connection_manager = {}; end
