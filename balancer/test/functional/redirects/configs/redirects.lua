instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB: "" ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
    }; -- admin_addrs

    log = log;

    ipdispatch = {
        test = {
            ip = "localhost";
            port = port;
            errorlog = {
                log = errorlog;
                log_level = "DEBUG";
                http = {
                    maxlen = 65536; maxreq = 65536;
                    accesslog = {
                        log = accesslog;
                        redirects = {
                            actions = {
                                {src="//maps.yandex.ru/*"; redirect={
                                    dst="https://yandex.ru/maps$request_uri$args";
                                    code=301;
                                    dst_rewrites={
                                        {regexp='/[.](?:/|$)'; rewrite='/'; global=true};
                                        {regexp='[.]([^.]*)[.]xml$'; rewrite='/$1'};
                                        {regexp='/([^?/]*)/*([?].*)'; rewrite='$2&$1'; url={
                                            path=true; query=true
                                        }},
                                    };
                                }};
                                {src="//maps.yandex.ru/.well-known/*"; forward={
                                    dst="//maps.s3.yandex.net/{path}",
                                    dst_rewrites={
                                        {regexp='/[.](?:/|$)'; rewrite='/'; global=true};
                                    },
                                    proxy = {
                                        host = "localhost"; port = forward_port;
                                        connect_timeout = "0.3s"; backend_timeout = "10s";
                                        resolve_timeout = "1s";
                                        fail_on_5xx = 0;
                                    }
                                }},
                                {src="//yandex.ru/things/*"; redirect={
                                    dst="https://store.turbo.site/$request_uri$args";
                                    code=302;
                                }};
                            }; -- actions
                            proxy = {
                                host = "localhost"; port = backend_port;
                                connect_timeout = "0.3s"; backend_timeout = "10s";
                                resolve_timeout = "1s";
                                fail_on_5xx = 0;
                            }; -- proxy
                        }; -- redirects
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
