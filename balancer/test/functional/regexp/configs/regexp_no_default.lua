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
            { ip = "localhost"; port = stats_port; disabled = 0; }
        };
    };

    ipdispatch = {
        default = {
            errorlog = {
                log = errorlog;
                http = {
                    maxlen = 65536; maxreq = 65536;
                    accesslog = {
                        log = accesslog;
                        report = {
                            uuid = "total";
                            regexp = {
                                root_section = {
                                    match_fsm = {
                                        url = "/"; case_insensitive = false; surround = false;
                                    }; -- match_fsm
                                    errordocument = {
                                        status = 200;
                                        content = "root_section";
                                    }; -- errordocument
                                }; -- root_section
                            }; -- regexp
                        }; -- report
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- default
    }; -- ipdispatch
}; --instance
