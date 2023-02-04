if ranges == nil then ranges = "10s"; end
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
                        regexp = {
                            scorpions = {
                                match_fsm = { path = "/scorpions"; };
                                report = {
                                    uuid = "scorpions";
                                    ranges = ranges;
                                    disable_sslness = scorpions_disable_sslness;
                                    disable_robotness = scorpions_disable_robotness;
                                    events = {
                                        stats = "report";
                                    }; -- events
                                    errordocument = {
                                        status = 200; content = "second";
                                    }; -- errordocument
                                }; -- report
                            }; -- scorpions
                            default = {
                                report = {
                                    uuid = "default";
                                    refers = "scorpions";
                                    ranges = ranges;
                                    disable_sslness = default_disable_sslness;
                                    disable_robotness = default_disable_robotness;
                                    events = {
                                        stats = "report";
                                    }; -- events
                                    errordocument = {
                                        status = 200; content = "first";
                                    }; -- errordocument
                                }; -- report
                            }; -- default
                        }; -- regexp
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
