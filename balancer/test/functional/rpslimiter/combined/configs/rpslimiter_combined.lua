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
            sync_interval = "200ms";
            ["self"] = {
                proxy = {
                    host = "localhost"; port = limiter_port;
                    connect_timeout = "1s"; backend_timeout = "1s"; resolve_timeout = "1s";
                };
            };
        };

        module = {
            errorlog = {
                log = error_log;
                ipdispatch = {
                    limiter = {
                        ip = "localhost"; port = limiter_port;
                        http = {
                            maxlen = 65536; maxreq = 65536;
                            accesslog = {
                                log = access_log;
                                regexp = {
                                    post = {
                                        match_method = { methods = {"post"} };
                                        prefix_path_router = {
                                            sync = {
                                                route = "/state.sync";
                                                quota_sync = {};
                                            };
                                            acquire = {
                                                route = "/quota.acquire";
                                                shared = {
                                                    uuid = "limiter_router";
                                                    unpack = {
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
                                                    }; -- unpack
                                                }; -- shared
                                            }; -- acquire
                                        }; -- prefix_path_router
                                    }; -- post
                                    default = {
                                        errordocument = { status = 405; };
                                    }; -- default
                                }; -- regexp
                            }; -- accesslog
                        }; -- http
                    }; -- limiter
                    default = {
                        ip = "localhost"; port = port;
                        http = {
                            maxlen = 65536; maxreq = 65536;
                            accesslog = {
                                log = access_log;
                                prefix_path_router = {
                                    ns1 = {
                                        route = "/ns1";
                                        rps_limiter = {
                                            namespace = "namespace1";
                                            checker = {
                                                -- TODO(velavokr): RPS-119 the local case should not require packing
                                                shared = { uuid = "limiter_router"; };
                                            };
                                            module = {
                                                errordocument = { status = 200; content = "/ns1"; };
                                            }; --module
                                            on_error = {
                                                errordocument = { status = 500; };
                                            }; -- on_error
                                        }; -- rps_limiter
                                    }; -- ns1
                                    ns2 = {
                                        route = "/ns2";
                                        rps_limiter = {
                                            namespace = "namespace2";
                                            checker = {
                                                -- TODO(velavokr): RPS-119 the local case should not require packing
                                                shared = { uuid = "limiter_router"; };
                                            };
                                            module = {
                                                errordocument = { status = 200; content = "/ns2"; };
                                            }; --module
                                            on_error = {
                                                errordocument = { status = 500; };
                                            }; -- on_error
                                        }; -- rps_limiter
                                    }; -- ns2
                                    default = {
                                        errordocument = { status = 404; }
                                    }; -- default
                                }; -- prefix_path_router
                            }; -- accesslog
                        }; -- http
                    }; -- default
                }; -- ipdispatch
            }; -- errorlog
        }; -- module
    }; -- rpslimiter_instance
}; -- instance
