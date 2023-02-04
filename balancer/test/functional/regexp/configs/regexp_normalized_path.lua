instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };

    http = {
        maxlen = 65536; maxreq = 65536;
        response_headers = {
            create_func = {
                ["X-Url"] = "url";
            };

            regexp = {
                test = {
                    match_fsm = {
                        normalized_path = "/test/123";
                    }; -- match_fsm
                    priority = 4;
                    errordocument = {
                        status = 200;
                        content = "match";
                    }; -- errordocument
                }; -- test
                default = {
                    priority = 2;
                    errordocument = {
                        status = 404;
                        content = "default";
                    };
                }; -- default
            }; -- regexp
        };
    }; -- http
}; --instance
