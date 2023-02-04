take_ip_from = take_ip_from or "X-Forwarded-For-Y";
laas_answer_header = laas_answer_header or "X-LaaS-Answered";
geobase_stats = geobase_stats or 'geobase'

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs
    unistat = {
        addrs = {
            { ip = "localhost"; port = unistat_port; };
        }; -- addrs
    }; -- unistat

    http = {
        maxlen = 64 * 1024; maxreq = 64 * 1024;
        accesslog = {
            log = accesslog;
            errorlog = {
                log = errorlog;
                    geobase = {
                        take_ip_from = take_ip_from;
                        laas_answer_header = laas_answer_header;
                        geo_host = geo_host;
                        geo_path = geo_path;
                        file_switch = file_switch;
                        trusted = trusted;
                        processing_time_header = true;
                        geo = {
                            proxy = {
                                host = "localhost"; port = geo_backend_port;
                                connect_timeout = "1s"; resolve_timeout = "1s";
                                backend_timeout = "5s";
                            }; -- proxy
                        }; -- geo
                        cookie_policy = {
                            uuid = "service_total";
                            is_gdpr_b_cookie = {
                                is_gdpr_b = {
                                    mode = "fix";
                                };
                            };
                            proxy = {
                                host = "localhost"; port = backend_port;
                                connect_timeout = "1s"; resolve_timeout = "1s";
                                backend_timeout = "5s";
                            }; -- proxy
                        }; -- cookie_policy
                    }; -- geobase
            }; -- errorlog
        }; -- accesslog
    }; -- http
}; -- instance


function replace_geo_with_dummy()
    geo = instance.http.accesslog.errorlog.geobase.geo;
    geo.proxy = nil;
    geo.errordocument = {
        status = 200;
        content = "geo response";
        headers = {
            ["X-Region-Id"] = dummy_region_id
        };
    };
end

if dummy_in_geo then
    replace_geo_with_dummy()
end
