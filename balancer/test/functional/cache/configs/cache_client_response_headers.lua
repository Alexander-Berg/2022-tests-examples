check = check and check == "true"
keepalive = keepalive and keepalive == "true"
instance = {
    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    thread_mode = thread_mode; set_no_file = false;

    ipdispatch = {
        admin = {
            ip = "localhost"; port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            }; -- http
        }; -- ipdispatch/admin
        default = {
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                keepalive = keepalive;
                errorlog = {
                    log = errorlog;
                    log_level = "DEBUG";

                    regexp = {
                        first = {
                            -- First request is meant to initialize the cache WITHOUT the x-led header
                            match_fsm = {
                                url = "[^m]*m=1.*";
                            }; -- match_fsm
                            shared = {
                                uuid = "cache_client";
                                cache_client = {
                                    id_regexp = match;
                                    server = {
                                        proxy = {
                                            host = "localhost"; port = server_port;
                                            connect_timeout = "5s"; backend_timeout = timeout;
                                            resolve_timeout = "1s";
                                            fail_on_5xx = 0;
                                            keepalive_count = 0;
                                        }; -- proxy
                                    }; -- server
                                    module = {
                                        proxy = {
                                            host = "localhost"; port = backend_port;
                                            connect_timeout = "5s"; backend_timeout = timeout;
                                            resolve_timeout = "1s";
                                            fail_on_5xx = 0;
                                        }; -- proxy
                                    }; -- module
                                }; -- cache_client
                            }; -- shared
                        }; -- first_request
                        second_request = {
                            -- Second request. Response has to come from the cache WITHOUT the x-led header.
                            -- But nevertheless, balancer has to add the x-led header in the outer scope of cache_client (which is response_headers).
                            -- For more info, see BALANCER-992
                            match_fsm = {
                                url = "[^m]*m=2.*";
                            }; -- match_fsm
                            rewrite = {
                                actions = {
                                    {
                                        regexp = "/\\?m=2([^\\s]*)";
                                        split = "url";
                                        rewrite = "/?m=1%1";
                                    };
                                }; -- actions
                                response_headers = {
                                    create = {
                                        ["x-led"] = "zeppelin";
                                    }; -- create
                                    shared = {
                                        uuid = "cache_client";
                                    };
                                }; -- response_headers
                            }; -- rewrite
                        }; -- second_request
                        default = {
                            errordocument = {
                                status = 404;
                                content = "not found\r\n";
                            }; -- errordocument
                        }; -- default
                    }; -- regexp
                }; -- errorlog
            }; -- http
        }; -- ipdispatch/default
    }; -- ipdispatch
}; -- instance
