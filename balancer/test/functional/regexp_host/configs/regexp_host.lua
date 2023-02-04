function val_or(val, default)
    if val ~= nil then
        return val
    else
        return default
    end
end

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
                regexp_host = {
                    yandex_ru = {
                        pattern = "yandex\\.ru"; case_insensitive = case_insensitive;
                        errordocument = {
                            status = 200;
                            content = "yandex.ru";
                        };
                    };
                    kub = {
                        pattern = ".*\\.(by|kz|ua)"; case_insensitive = case_insensitive;
                        errordocument = {
                            status = 200;
                            content = "kub";
                        };
                    };
                    com = {
                        pattern = ".*\\.com"; case_insensitive = case_insensitive;
                        errordocument = {
                            status = 200;
                            content = "com";
                        };
                    };
                    com_tr = {
                        pattern = ".*\\.com\\.tr"; case_insensitive = case_insensitive;
                        errordocument = {
                            status = 200;
                            content = "com.tr";
                        };
                    };
                    rock = {
                        pattern = "rock.*"; case_insensitive = case_insensitive;
                        priority = rock_prio;
                        errordocument = {
                            status = 200;
                            content = "rock";
                        };
                    };
                    roll = {
                        pattern = ".*roll"; case_insensitive = case_insensitive;
                        priority = roll_prio;
                        errordocument = {
                            status = 200;
                            content = "roll";
                        };
                    };
                    default = {
                        errordocument = {
                            status = 200;
                            content = "default";
                        };
                    };
                }; -- regexp_path
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
