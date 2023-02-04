if ranges == nil then ranges = "10s"; end
if first_refers == nil then first_refers = "trash"; end
if second_refers == nil then second_refers = "trash"; end
if top_refers == nil then top_refers = "trash"; end
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
                        report = {
                            uuid = "top";
                            refers = top_refers;
                            ranges = ranges;
                            events = {
                                stats = "report";
                            }; -- events
                            regexp = {
                                first = {
                                    match_fsm = { path = "/first"; };
                                    report = {
                                        uuid = "first";
                                        refers = first_refers;
                                        events = {
                                            stats = "report";
                                        }; -- events
                                        errordocument = {
                                            status = 200; content = "first";
                                        }; -- errordocument
                                    }; -- report
                                }; -- first
                                second = {
                                    match_fsm = { path = "/second"; };
                                    report = {
                                        uuid = "second";
                                        refers = second_refers;
                                        ranges = ranges;
                                        events = {
                                            stats = "report";
                                        }; -- events
                                        errordocument = {
                                            status = 200; content = "second";
                                        }; -- errordocument
                                    }; -- report
                                }; -- second
                                trash = {
                                    match_fsm = { path = "/trash"; };
                                    report = {
                                        uuid = "trash";
                                        ranges = ranges;
                                        events = {
                                            stats = "report";
                                        }; -- events
                                        errordocument = {
                                            status = 200; content = "trash";
                                        }; -- errordocument
                                    }; -- report
                                }; -- trash
                            }; -- regexp
                        }; -- report
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
