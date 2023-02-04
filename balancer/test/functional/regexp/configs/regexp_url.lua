instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };

    ipdispatch = {
        admin = {
            ip = "localhost"; port = admin_port;
            http = {
                maxlen = 64 * 1024; maxreq = 64 * 1024;
                admin = {};
            }; -- http
        }; -- ipdispatch/admin
        default = {
            http = {
                maxlen = 65536; maxreq = 65536;
                regexp = {
                    ddos_ban = {
                        match_fsm = {
                            url = "/delurl\\.xml.*antisharlatan\\.net.*";
                            case_insensitive = false;
                            surround = false;
                        }; -- match_fsm
                        priority = 4;
                        errordocument = {
                            status = 200;
                            content = "match1";
                        }; -- errordocument
                    }; -- ddos_ban
                    video_ban = {
                        match_fsm = {
                            url = "/video/search\\?text=сериал%20вне%20игры%20смотреть%20онлайн";
                            case_insensitive = true;
                            surround = false;
                        }; -- match_fsm
                        priority = 4;
                        errordocument = {
                            status = 200;
                            content = "match2";
                        }; -- errordocument
                    }; -- video_ban
                    default = {
                        priority = 2;
                        errordocument = {
                            status = 404;
                            content = "default";
                        };
                    }; -- default
                }; -- regexp
            }; -- http
        }; -- ipdispatch/default
    }; -- ipdispatch
}; --instance
