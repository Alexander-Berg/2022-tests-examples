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
                        regexp = {
                            a = {
                                match_fsm = {
                                    path = "/a.*"; surround = false; case_insensitive = true;
                                };
                                report = {
                                    uuid = "a";
                                    all_default_ranges = true;
                                    matcher_map = {
                                        foo = { match_fsm = { path = ".*foo.*"; }; };
                                        bar = { match_fsm = { path = ".*bar.*"; }; };
                                    };
                                    events = {
                                        stats = "report";
                                    }; -- events
                                    proxy = {
                                        host = "localhost"; port = backend_port;
                                        connect_timeout = "0.3s"; backend_timeout = "10s";
                                        resolve_timeout = "1s";
                                        fail_on_5xx = 0;
                                    }; -- proxy
                                }; -- report
                            };

                            b = {
                                match_fsm = {
                                    path = "/b.*"; surround = false; case_insensitive = true;
                                };
                                report = {
                                    uuid = "a,b";
                                    events = {
                                        stats = "report";
                                    }; -- events
                                    proxy = {
                                        host = "localhost"; port = backend_port;
                                        connect_timeout = "0.3s"; backend_timeout = "10s";
                                        resolve_timeout = "1s";
                                        fail_on_5xx = 0;
                                    }; -- proxy
                                }; -- report
                            };

                            c = {
                                match_fsm = {
                                    path = "/c.*"; surround = false; case_insensitive = true;
                                };
                                report = {
                                    uuid = "a,b,c";
                                    all_default_ranges = true;
                                    matcher_map = {
                                        foo = { match_fsm = { path = ".*foo.*"; }; };
                                    };
                                    events = {
                                        stats = "report";
                                    }; -- events
                                    proxy = {
                                        host = "localhost"; port = backend_port;
                                        connect_timeout = "0.3s"; backend_timeout = "10s";
                                        resolve_timeout = "1s";
                                        fail_on_5xx = 0;
                                    }; -- proxy
                                }; -- report
                            };
                        }; -- regexp
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
