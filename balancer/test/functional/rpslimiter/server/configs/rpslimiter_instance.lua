instance = {
    unistat = {
        addrs = { { ip = "localhost"; port = stats_port; }; };
        hide_legacy_signals = true;
    }; -- unistat

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs

    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    log = instance_log;

    -- TODO(velavokr): RPS-118 rpslimiter_instance should be a section in instance, not a module
    rpslimiter_instance = {
        quotas = {
            --[[intervals are in ms]]--
            ["namespace1-quota"] = { quota = quota1; interval = interval1; };
            ["namespace2-quota"] = { quota = quota2; interval = interval2; };
        };

        remote = {
            local_host_id = "self";
            sync_path = "/state.sync";
            sync_interval = sync_interval;
            ["self"] = {
                proxy = {
                    host = "localhost"; port = self_port;
                    connect_timeout = "1s"; backend_timeout = "1s"; resolve_timeout = "1s";
                };
            };
            ["peer1"] = {
                proxy = {
                    host = "localhost"; port = peer1_port;
                    connect_timeout = "1s"; backend_timeout = "1s"; resolve_timeout = "1s";
                };
            };
            ["peer2"] = {
                proxy = {
                    host = "localhost"; port = peer2_port;
                    connect_timeout = "1s"; backend_timeout = "1s"; resolve_timeout = "1s";
                };
            };
        };

        module = {
            errorlog = {
                log = error_log;
                http = {
                    maxlen = 65536; maxreq = 65536;
                    accesslog = {
                        log = access_log;
                        regexp = {
                            get = {
                                match_method = { methods = {"get"}; };
                                shared = {
                                    uuid = "limiter_router";
                                    regexp = {
                                        ["quota1-route"] = {
                                            match_fsm = { header = {
                                                name = "x-rpslimiter-balancer";
                                                value = "namespace1";
                                            }; };
                                            quota = { name = "namespace1-quota"; };
                                        };
                                        ["quota2-route"] = {
                                            match_fsm = { header = {
                                                name = "x-rpslimiter-balancer";
                                                value = "namespace2";
                                            }; };
                                            quota = { name = "namespace2-quota"; };
                                        };
                                        default = {
                                            errordocument = { status = 404; };
                                        };
                                    }; -- regexp
                                }; -- shared
                            }; -- get
                            post = {
                                match_method = { methods = {"post"} };
                                prefix_path_router = {
                                    sync = {
                                        route = "/state.sync";
                                        quota_sync = {};
                                    };
                                    acquire = {
                                        route = "/quota.acquire";
                                        unpack = {
                                            shared = { uuid = "limiter_router"; };
                                        }; -- unpack
                                    };
                                }; -- prefix_path_router
                            }; -- post
                            default = {
                                errordocument = { status = 405; };
                            }; -- default
                        }; -- regexp
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- module
    }; -- rpslimiter_instance
}; -- instance
