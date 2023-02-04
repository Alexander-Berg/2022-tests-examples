instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; };
    }; --addrs
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
                maxlen = 65535;
                maxreq = 65535;

                accesslog = {
                    log = common_log;
                    regexp = {
                        led = {
                            match_fsm = { path = "/led/.*"; }; -- match_fsm
                            accesslog = {
                                log = led_log;
                                errordocument = {
                                    status = 200;
                                    content = "Led";
                                }; --errordocument/led
                            }; -- accesslog/led
                        }; -- regexp/led
                        zeppelin = {
                            match_fsm = { path = "/zeppelin/.*"; }; -- match_fsm
                            accesslog = {
                                log = zeppelin_log;
                                errordocument = {
                                    status = 200;
                                    content = "Zeppelin";
                                }; -- errordocument/zeppelin
                            }; -- accesslog/zeppelin
                        }; -- regexp/zeppelin
                    }; -- regexp
                }; -- accesslog/common
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
