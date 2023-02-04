instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
    }; -- admin_addrs
    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port; disabled = 0; }
        };
    }; -- unistat

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
                report = {
                    uuid = "total";
                    regexp_host = {
                        yandex_ru = {
                            pattern = "yandex\\.ru";
                            errordocument = {
                                status = 200;
                                content = "yandex.ru";
                            }; -- errordocument
                        }; -- regexp_host/yandex.ru
                    }; -- regexp_host
                }; -- report
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
