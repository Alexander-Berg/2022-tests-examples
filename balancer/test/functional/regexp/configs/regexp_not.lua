instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
    }; -- admin_addrs

    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;
            http = {
                maxlen = 65536; maxreq = 65536;
                admin = {};
            }; -- http
        }; -- admin
        test = {
            ip = "localhost";
            port = port;
            http = {
                maxlen = 65536; maxreq = 65536;
                regexp = {
                    yandex_ru = {
                        priority = 3;
                        match_fsm = {
                            host = "yandex\\.ru"; case_insensitive = true;
                        }; -- match_fsm
                        errordocument = {
                            status = 200;
                            content = "yandex.ru";
                        }; --errordocument
                    }; -- yandex_ru
                    not_yandex = {
                        priority = 2;
                        match_not = {
                            match_fsm = {
                                host = "yandex"; case_insensitive = true; surround = true;
                            }; -- match_fsm
                        }; -- match_not
                        errordocument = {
                            status = 200;
                            content = "not yandex";
                        }; --errordocument
                    };
                    default = {
                        errordocument = {
                            status = 200;
                            content = "some yandex";
                        }; -- errordocument
                    }; -- default
                }; -- regexp
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

