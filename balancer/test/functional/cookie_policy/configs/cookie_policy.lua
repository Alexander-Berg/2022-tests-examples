function gen_ssn_policy()
    if ssn_policy then
        return {
            mode = ssn_policy_mode;
            name_re = ssn_name_re;
        };
    end
end

function gen_epl_policy()
    if epl_policy then
        return {
            mode = epl_policy_mode;
        };
    end
end

function gen_prc_policy()
    if prc_policy then
        return {
            mode = prc_policy_mode;
            name_re = prc_name_re;
        };
    end
end

function gen_ssn_override()
    if ssn_policy_override then
        return {
            mode = ssn_policy_mode;
        }
    end
end

function gen_epl_override()
    if epl_policy_override then
        return {
            mode = epl_policy_mode;
        }
    end
end

function gen_prc_override()
    if prc_policy_override then
        return {
            mode = prc_policy_mode;
        }
    end
end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };
    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port; }
        };
    };
    ipdispatch = {
        default = {
            errorlog = {
                log = errorlog;
                http = {
                    maxreq = 64 * 1024; maxlen = 64 * 1024;
                    accesslog = {
                        log = accesslog;
                        cookie_policy = {
                            uuid = "xxx";
                            file_switch = file_switch;
                            gdpr_file_switch = gdpr_file_switch;
                            default_yandex_policies = default_yandex_policies;
                            parser_mode = parser_mode;
                            samesite_none = {
                                [ssn_policy_name] = gen_ssn_policy();
                            };
                            gdpr_lifetime = {
                                [epl_policy_name] = gen_epl_policy();
                            };
                            protected_cookie = {
                                [prc_policy_name] = gen_prc_policy();
                            };

                            mode_controls = {
                                policy_modes = {
                                    [ssn_policy_name] = gen_ssn_override();
                                    [epl_policy_name] = gen_epl_override();
                                    [prc_policy_name] = gen_prc_override();
                                };
                            };

                            proxy = {
                                host = "localhost"; port = backend_port;
                                connect_timeout = "0.3s"; backend_timeout = backend_timeout;
                                resolve_timeout = "0.3s";
                                keepalive_count = 5;
                                fail_on_5xx = 0;
                            }; -- proxy
                        }; -- cookie_policy
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- default
    }; -- ipdispatch
}; -- instance
