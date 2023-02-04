default_ciphers = "kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2"


default_ranges = "1ms,4ms,7ms,11ms,17ms,26ms,39ms,58ms,87ms,131ms,197ms,296ms,444ms,666ms,1000ms,1500ms,2250ms,3375ms,5062ms,7593ms,11390ms,17085ms,30000ms,60000ms,150000ms"


function _call_func_providers(overridable_func_names)
  for _, func_name in pairs(overridable_func_names) do
    local func_provider_path = _G[func_name .. "_provider"]
    if func_provider_path ~= nil then
      local env = {}
      setmetatable(env, {__index = _G})
      local provider, err = loadfile(func_provider_path, nil, env)
      if provider == nil then
        error(string.format('Failed to import provider "%s": %s', func_provider_path, err))
      end
      ok, rv = pcall(provider)
      if ok then
        if type(rv) ~= 'function' then
          error(string.format('Provider "%s" must return a function, not %s.', func_provider_path, type(rv)))
        end
        _G["do_" .. func_name] = rv
      else
        error(string.format('Provider "%s" failed: %s', func_provider_path, rv))
      end
    end
  end
end


function gen_proxy_backends(backends, proxy_options)
  local result = {}

  for index, backend in pairs(backends) do
    local proxy = {
      host = backend[1] or backend['host'];
      port = backend[2] or backend['port'];
      cached_ip = backend[4] or backend['cached_ip'];
    };

    if proxy_options ~= nil then
      for optname, optvalue in pairs(proxy_options) do
        proxy[optname] = optvalue
      end
    end

    result[index] = {
      weight = backend[3] or backend['weight'];
      proxy = proxy;
    };
  end

  if next(result) == nil then
    error("backends list is empty")
  end

  return result
end


function get_ca_cert_path(name, default_ca_cert_dir)
  default_ca_cert_dir = default_ca_cert_dir or "/dev/shm/balancer/priv"
  return (ca_cert_dir or default_ca_cert_dir) .. "/" .. name;
end


function get_geo(name, default_geo)
  default_geo = default_geo or "random"
  return name .. (DC or default_geo);
end


function check_int(value, var_name)
    return tonumber(value) or error("Could not cast variable \"" .. var_name .. "\" to a number.'")
end

function get_int_var(name, default)
  value = _G[name]
  return value and check_int(value) or default
end


function get_ip_by_iproute(addr_family)
  if disable_external then
    if addr_family == "v4" then
      return "127.1.1.1"
    elseif addr_family == "v6" then
      return "127.2.2.2"
    else
      error("invalid parameter")
    end
  end

  local ipcmd
  if addr_family == "v4" then
    ipcmd = "ip route get 77.88.8.8 2>/dev/null| awk '/src/ {print $NF}'"
  elseif addr_family == "v6" then
    ipcmd = "ip route get 2a00:1450:4010:c05::65 2>/dev/null | grep -oE '2a02[:0-9a-f]+' | tail -1"
  else
    error("invalid parameter")
  end
  local handler = io.popen(ipcmd)
  local ip = handler:read("*l")
  handler:close()
  if ip == nil or ip == "" or ip == "proto" then
    return "127.0.0.2"
  end
  return ip
end


function get_log_path(name, port, default_log_dir)
  default_log_dir = default_log_dir or "/place/db/www/logs"
  rv = (log_dir or default_log_dir) .. "/current-" .. name .. "-balancer";
  if port ~= nil then
    rv = rv .. "-" .. port;
  end
  return rv
end


function get_private_cert_path(name, default_private_cert_dir)
  default_private_cert_dir = default_private_cert_dir or "/dev/shm/balancer/priv"
  return (private_cert_dir or default_private_cert_dir) .. "/" .. name;
end


function get_public_cert_path(name, default_public_cert_dir)
  default_public_cert_dir = default_public_cert_dir or "/dev/shm/balancer"
  return (public_cert_dir or default_public_cert_dir) .. "/" .. name;
end


function get_random_timedelta(start, end_, unit)
  return math.random(start, end_) .. unit;
end


function get_str_env_var(name, default)
  rv = os.getenv(name)
  if rv == nil then
    if default == nil then
      error(string.format('Environment variable "%s" is not set.', name))
    else
      return default
    end
  else
    return rv
  end
end


function get_str_var(name, default)
  return _G[name] or default
end


function do_get_workers()
  -- actual get_workers() implementation, can be overridden
  value = _G["workers"]
  if value == nil then
    error('Variable "workers" is not specified.')
  end
  int_value = tonumber(value)
  if int_value == nil then
    error('Could not cast variable "workers" to a number.')
  end
  return int_value
end


function get_workers()
  value = do_get_workers()
  if type(value) ~= 'number' then
    error(string.format('Provided get_workers() implementation must return a number, not %s.', type(value)))
  end
  if value < 0 or value % 1 ~= 0 then
    error(string.format('Provided get_workers() implementation must return a non-negative integer, not %s', value))
  end
  return value
end


_call_func_providers({
  "get_workers";
})


instance = {
  maxconn = 4000;
  buffer = 1048576;
  tcp_fastopen = 0;
  thread_mode = true;
  workers = get_workers();
  enable_reuse_port = true;
  private_address = "127.0.0.10";
  default_tcp_rst_on_error = true;
  events = {
    stats = "report";
  }; -- events
  dns_ttl = get_random_timedelta(300, 360, "s");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", 8180, "/place/db/www/logs/");
  cpu_limiter = {
    active_check_subnet_default = true;
    cpu_usage_coeff = 0.500;
  }; -- cpu_limiter
  admin_addrs = {
    {
      port = 8180;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 8180;
    };
  }; -- admin_addrs
  addrs = {
    {
      port = 8180;
      ip = "127.0.0.4";
    };
    {
      port = 80;
      ip = "2a02:6b8:0:3400::1:16";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "2a02:6b8:0:3400::1:17";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 8180;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 8180;
      ip = get_ip_by_iproute("v6");
    };
    {
      port = 443;
      ip = "2a02:6b8:0:3400::1:16";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "2a02:6b8:0:3400::1:17";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 8181;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 8181;
      ip = get_ip_by_iproute("v6");
    };
    {
      port = 80;
      ip = "2a02:6b8:0:3400::1050";
      disabled = get_int_var("disable_external", 0);
    };
  }; -- addrs
  ipdispatch = {
    admin = {
      ips = {
        "127.0.0.1";
        "::1";
      }; -- ips
      ports = {
        8180;
      }; -- ports
      http = {
        maxlen = 65536;
        maxreq = 65536;
        keepalive = true;
        no_keepalive_file = "./controls/keepalive_disabled";
        events = {
          stats = "report";
        }; -- events
        admin = {};
      }; -- http
    }; -- admin
    stats_storage = {
      ips = {
        "127.0.0.4";
      }; -- ips
      ports = {
        8180;
      }; -- ports
      report = {
        uuid = "service_total";
        ranges = get_str_var("default_ranges");
        just_storage = true;
        disable_robotness = true;
        disable_sslness = true;
        events = {
          stats = "report";
        }; -- events
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          rps_limiter = {
            skip_on_error = true;
            disable_file = "./rps-limiter-file-switch";
            checker = {
              errordocument = {
                status = 200;
                force_conn_close = false;
              }; -- errordocument
            }; -- checker
            module = {
              errordocument = {
                status = 204;
                force_conn_close = false;
              }; -- errordocument
            }; -- module
          }; -- rps_limiter
        }; -- http
      }; -- report
    }; -- stats_storage
    section_1_80 = {
      ips = {
        "2a02:6b8:0:3400::1:16";
        "2a02:6b8:0:3400::1:17";
      }; -- ips
      ports = {
        80;
      }; -- ports
      shared = {
        uuid = "600373725010471674";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 8180, "/place/db/www/logs/");
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            accesslog = {
              log = get_log_path("access_log", 8180, "/place/db/www/logs/");
              report = {
                uuid = "http";
                refers = "service_total";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                webauth = {
                  auth_path = "/check_oauth_token";
                  checker = {
                    shared = {
                      uuid = "1527141021745917176";
                      headers = {
                        create = {
                          Host = "webauth.yandex-team.ru";
                        }; -- create
                        proxy = {
                          host = "webauth.yandex-team.ru";
                          port = 443;
                          resolve_timeout = "10ms";
                          connect_timeout = "100ms";
                          backend_timeout = "5s";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                          https_settings = {
                            ciphers = "kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";
                            ca_file = get_ca_cert_path("allCAs-hamster.yandex.tld.pem", "");
                            sni_on = true;
                            verify_depth = 3;
                            sni_host = "webauth.yandex-team.ru";
                          }; -- https_settings
                        }; -- proxy
                      }; -- headers
                    }; -- shared
                  }; -- checker
                  on_forbidden = {
                    errordocument = {
                      status = 403;
                      force_conn_close = false;
                      content = "Access forbidden";
                    }; -- errordocument
                  }; -- on_forbidden
                  unauthorized_set_cookie = "webauth_csrf_token={csrf_token}; Path=/";
                  unauthorized_redirect = "https://oauth.yandex-team.ru/authorize?response_type=code&client_id={app_id}&state={csrf_state}";
                  role = "/webauth-qloud/qloud-ext/education/yashchenko-klein/envs/crowdtest/user";
                  regexp = {
                    yandex = {
                      priority = 2;
                      match_fsm = {
                        host = "(.*\\.)?(xn----7sbhgfw0a0bcg8l1a\\.xn--p1ai|xn--80aefebu0a0bbh8l\\.xn--p1ai|xn--d1acpjx3f\\.xn--p1ai|2yandex\\.ru|jandeks\\.com\\.tr|jandex\\.com\\.tr|kremlyandex\\.ru|video-yandex\\.ru|videoyandex\\.ru|wwwyandex\\.ru|xyyandex\\.net|ya-plus-plus\\.ru|ya-plusplus\\.ru|ya\\.nu|ya\\.rs|ya\\.ru|ya\\.tel|ya\\.tm|yanclex\\.ru|yandeks\\.com|yandeks\\.com\\.tr|yandes\\.ru|yandesk\\.com|yandesk\\.org|yandesk\\.ru|yandex-plus-plus\\.ru|yandex-plusplus\\.ru|yandex-rambler\\.ru|yandex-video\\.ru|yandex\\.asia|yandex\\.az|yandex\\.biz\\.tr|yandex\\.by|yandex\\.co\\.il|yandex\\.co\\.no|yandex\\.com|yandex\\.com\\.de|yandex\\.com\\.kz|yandex\\.com\\.ru|yandex\\.com\\.tr|yandex\\.com\\.ua|yandex\\.de|yandex\\.dk|yandex\\.do|yandex\\.ee|yandex\\.es|yandex\\.ie|yandex\\.in|yandex\\.info\\.tr|yandex\\.it|yandex\\.jobs|yandex\\.jp\\.net|yandex\\.kg|yandex\\.kz|yandex\\.lt|yandex\\.lu|yandex\\.lv|yandex\\.md|yandex\\.mobi|yandex\\.mx|yandex\\.name|yandex\\.net|yandex\\.net\\.ru|yandex\\.no|yandex\\.nu|yandex\\.org|yandex\\.pl|yandex\\.pt|yandex\\.qa|yandex\\.ro|yandex\\.rs|yandex\\.ru|yandex\\.sk|yandex\\.st|yandex\\.sx|yandex\\.tj|yandex\\.tm|yandex\\.ua|yandex\\.uz|yandex\\.web\\.tr|yandex\\.xxx|yandexbox\\.ru|yandexmedia\\.ru|yandexplusplus\\.ru|yandexvideo\\.ru|yandfex\\.ru|yandx\\.ru|yaplusplus\\.ru|yandex\\.com\\.ge|yandex\\.fr|yandex\\.az|yandex\\.uz|yandex\\.com\\.am|yandex\\.co\\.il|yandex\\.kg|yandex\\.lt|yandex\\.lv|yandex\\.md|yandex\\.tj|yandex\\.tm|yandex\\.ee)(:\\d+|\\.)?";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                      hasher = {
                        mode = "subnet";
                        subnet_v4_mask = 32;
                        subnet_v6_mask = 128;
                        headers = {
                          create_func = {
                            ["X-Forwarded-For-Y"] = "realip";
                            ["X-Req-Id"] = "reqid";
                            ["X-Source-Port-Y"] = "realport";
                            ["X-Start-Time"] = "starttime";
                            ["X-Yandex-Balancer"] = "localip";
                            ["X-Yandex-RandomUID"] = "yuid";
                          }; -- create_func
                          response_headers = {
                            delete = "Strict-Transport-Security|X-Yandex-Report-Type";
                            create_weak = {
                              ["X-Content-Type-Options"] = "nosniff";
                              ["X-XSS-Protection"] = "1; mode=block";
                            }; -- create_weak
                            shared = {
                              uuid = "596626063809110048";
                              rpcrewrite = {
                                url = "/proxy";
                                dry_run = false;
                                host = "bolver.yandex-team.ru";
                                rpc_success_header = "X-Metabalancer-Answered";
                                file_switch = "./controls/disable_rpcrewrite_module";
                                rpc = {
                                  report = {
                                    uuid = "rpcrewrite-backend";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    stats_eater = {
                                      balancer2 = {
                                        simple_policy = {};
                                        attempts = 3;
                                        rr = {
                                          unpack(gen_proxy_backends({
                                            { "bolver.yandex-team.ru"; 80; 1.000; "2a02:6b8:0:3400::32"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "150ms";
                                            backend_timeout = "10s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- rr
                                      }; -- balancer2
                                    }; -- stats_eater
                                  }; -- report
                                }; -- rpc
                                regexp = {
                                  upstream_captcha = {
                                    priority = 12;
                                    match_fsm = {
                                      URI = "/x?(show|check)?captcha.*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    rps_limiter = {
                                      skip_on_error = false;
                                      namespace = "record";
                                      on_error = {
                                        errordocument = {
                                          status = 503;
                                          force_conn_close = false;
                                        }; -- errordocument
                                      }; -- on_error
                                      checker = {
                                        errordocument = {
                                          status = 200;
                                          force_conn_close = false;
                                        }; -- errordocument
                                      }; -- checker
                                      module = {
                                        report = {
                                          uuid = "captchasearch";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          h100 = {
                                            cutter = {
                                              bytes = 512;
                                              timeout = "0.1s";
                                              antirobot = {
                                                cut_request = true;
                                                no_cut_request_file = "./controls/no_cut_request_file";
                                                file_switch = "./controls/do.not.use.it";
                                                cut_request_bytes = 512;
                                                checker = {
                                                  stats_eater = {
                                                    balancer2 = {
                                                      unique_policy = {};
                                                      attempts = 2;
                                                      weighted2 = {
                                                        slow_reply_time = "1s";
                                                        correction_params = {
                                                          max_weight = 5.000;
                                                          min_weight = 0.050;
                                                          history_time = "100s";
                                                          feedback_time = "300s";
                                                          plus_diff_per_sec = 0.050;
                                                          minus_diff_per_sec = 0.100;
                                                        }; -- correction_params
                                                        unpack(gen_proxy_backends({
                                                          { "ws14-011.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::8d08:b330"; };
                                                          { "ws26-201.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d986"; };
                                                          { "ws26-466.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d803"; };
                                                          { "ws26-467.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d804"; };
                                                          { "ws26-468.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d805"; };
                                                          { "ws26-469.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d806"; };
                                                          { "ws26-470.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d8fe"; };
                                                          { "ws26-471.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d8f6"; };
                                                          { "ws26-472.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d8f7"; };
                                                          { "ws26-473.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d8f8"; };
                                                          { "ws26-474.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d8f9"; };
                                                          { "ws26-476.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d8fb"; };
                                                          { "ws27-230.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:da95"; };
                                                          { "ws27-231.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:db95"; };
                                                          { "ws27-449.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:db19"; };
                                                          { "ws28-050.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:903b"; };
                                                          { "ws28-051.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:913b"; };
                                                          { "ws28-052.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:903c"; };
                                                          { "ws28-195.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:9183"; };
                                                          { "ws28-196.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:9084"; };
                                                          { "ws28-199.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:9185"; };
                                                          { "ws28-206.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:9089"; };
                                                          { "ws28-207.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:9189"; };
                                                          { "ws28-208.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:908a"; };
                                                          { "ws28-214.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:908d"; };
                                                          { "ws28-216.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:908e"; };
                                                          { "ws28-217.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:918e"; };
                                                          { "ws29-200.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:9286"; };
                                                          { "ws29-201.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:9386"; };
                                                          { "ws29-202.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:9287"; };
                                                          { "ws30-149.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:f12::54c9:b151"; };
                                                          { "ws31-150.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:f12::54c9:b352"; };
                                                          { "ws35-013.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:8508"; };
                                                          { "ws35-019.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:850b"; };
                                                          { "ws35-022.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:840d"; };
                                                          { "ws35-028.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:8410"; };
                                                          { "ws35-044.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:8418"; };
                                                          { "ws35-045.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:8518"; };
                                                          { "ws35-046.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:8419"; };
                                                          { "ws35-047.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:8519"; };
                                                          { "ws35-048.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:841a"; };
                                                          { "ws35-049.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:851a"; };
                                                          { "ws35-050.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:841b"; };
                                                          { "ws35-051.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:851b"; };
                                                          { "ws35-052.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:841c"; };
                                                          { "ws35-053.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:851c"; };
                                                          { "ws35-054.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:841d"; };
                                                          { "ws36-002.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:2502::2509:5403"; };
                                                          { "ws37-927.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:2502::2509:5fd7"; };
                                                          { "ws37-934.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:2502::2509:5edb"; };
                                                          { "ws37-935.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:2502::2509:5fdb"; };
                                                          { "ws37-936.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:2502::2509:5edc"; };
                                                          { "ws37-937.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:2502::2509:5fdc"; };
                                                          { "ws37-938.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:2502::2509:5edd"; };
                                                          { "ws37-939.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:2502::2509:5fdd"; };
                                                          { "ws37-940.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:2502::2509:5ede"; };
                                                          { "ws37-941.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:2502::2509:5fde"; };
                                                        }, {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "30ms";
                                                          backend_timeout = "10s";
                                                          fail_on_5xx = true;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 0;
                                                          need_resolve = true;
                                                        }))
                                                      }; -- weighted2
                                                    }; -- balancer2
                                                  }; -- stats_eater
                                                }; -- checker
                                                module = {
                                                  errordocument = {
                                                    status = 403;
                                                    force_conn_close = false;
                                                  }; -- errordocument
                                                }; -- module
                                              }; -- antirobot
                                            }; -- cutter
                                          }; -- h100
                                        }; -- report
                                      }; -- module
                                    }; -- rps_limiter
                                  }; -- upstream_captcha
                                  upstream_clck = {
                                    priority = 11;
                                    match_fsm = {
                                      URI = "/clck/(.*)?";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    hasher = {
                                      mode = "subnet";
                                      take_ip_from = "X-Real-IP";
                                      h100 = {
                                        cutter = {
                                          bytes = 512;
                                          timeout = "0.1s";
                                          antirobot = {
                                            cut_request = true;
                                            no_cut_request_file = "./controls/no_cut_request_file";
                                            file_switch = "./controls/disable_antirobot_module";
                                            cut_request_bytes = 512;
                                            checker = {
                                              shared = {
                                                uuid = "1283850100962082161";
                                              }; -- shared
                                            }; -- checker
                                            module = {
                                              rewrite = {
                                                actions = {
                                                  {
                                                    global = false;
                                                    rewrite = "%1";
                                                    literal = false;
                                                    regexp = "/clck(/.*)";
                                                    case_insensitive = false;
                                                  };
                                                }; -- actions
                                                click = {
                                                  keys = "./data/clickdaemon.keys";
                                                  report = {
                                                    uuid = "clcksearch";
                                                    ranges = get_str_var("default_ranges");
                                                    just_storage = false;
                                                    disable_robotness = true;
                                                    disable_sslness = true;
                                                    events = {
                                                      stats = "report";
                                                    }; -- events
                                                    stats_eater = {
                                                      balancer2 = {
                                                        unique_policy = {};
                                                        attempts = 5;
                                                        weighted2 = {
                                                          slow_reply_time = "1s";
                                                          correction_params = {
                                                            max_weight = 5.000;
                                                            min_weight = 0.050;
                                                            history_time = "100s";
                                                            feedback_time = "300s";
                                                            plus_diff_per_sec = 0.050;
                                                            minus_diff_per_sec = 0.100;
                                                          }; -- correction_params
                                                          unpack(gen_proxy_backends({
                                                            { "imgs28-005.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df05"; };
                                                            { "imgs28-047.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df2f"; };
                                                            { "imgs28-051.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df33"; };
                                                            { "imgs28-055.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df37"; };
                                                            { "imgs28-059.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df3b"; };
                                                            { "imgs28-062.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df3e"; };
                                                            { "imgs28-064.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df40"; };
                                                            { "imgs28-066.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df42"; };
                                                            { "imgs28-067.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df43"; };
                                                            { "imgs28-070.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df46"; };
                                                            { "imgs28-123.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df7b"; };
                                                            { "imgs28-125.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df7d"; };
                                                            { "imgs28-128.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df80"; };
                                                            { "imgs28-131.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df83"; };
                                                            { "imgs28-134.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df86"; };
                                                            { "imgs28-137.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df89"; };
                                                            { "imgs28-141.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df8d"; };
                                                            { "imgs28-149.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df95"; };
                                                            { "imgs28-154.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df9a"; };
                                                            { "imgs28-157.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df9d"; };
                                                            { "imgs28-159.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:df9f"; };
                                                            { "imgs28-165.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:dfa5"; };
                                                            { "imgs28-166.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:dfa6"; };
                                                            { "imgs28-169.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:dfa9"; };
                                                            { "imgs28-182.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:dfb6"; };
                                                            { "imgs28-185.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c28::b29a:dfb9"; };
                                                            { "imgs30-015.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:f12::54c9:b611"; };
                                                            { "imgs30-017.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:f12::54c9:b613"; };
                                                            { "imgs30-040.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:f12::54c9:b62a"; };
                                                            { "imgs30-043.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:f12::54c9:b62d"; };
                                                            { "imgs30-047.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:f12::54c9:b631"; };
                                                            { "imgs30-050.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:f12::54c9:b634"; };
                                                            { "imgs30-054.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:f12::54c9:b638"; };
                                                            { "imgs30-055.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:f12::54c9:b639"; };
                                                            { "imgs30-061.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:f12::54c9:b63f"; };
                                                            { "imgs30-066.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:f12::54c9:b644"; };
                                                            { "imgs30-074.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:f12::54c9:b64c"; };
                                                            { "imgs30-075.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:f12::54c9:b64d"; };
                                                            { "imgs30-077.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:f12::54c9:b64f"; };
                                                            { "imgs30-083.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:f12::54c9:b655"; };
                                                            { "imgs30-088.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:f12::54c9:b65a"; };
                                                            { "imgs30-095.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:f12::54c9:b661"; };
                                                            { "imgs30-101.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:f12::54c9:b667"; };
                                                            { "imgs30-102.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:f12::54c9:b668"; };
                                                            { "imgs30-103.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:f12::54c9:b669"; };
                                                            { "imgs30-104.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:f12::54c9:b66a"; };
                                                            { "ws25-184.search.yandex.net"; 18100; 10.102; "2a02:6b8:0:1498::b29a:8c7e"; };
                                                            { "ws25-300.search.yandex.net"; 18100; 10.102; "2a02:6b8:0:1498::b29a:8cb8"; };
                                                            { "ws26-487.search.yandex.net"; 18100; 17.656; "2a02:6b8:0:c22::b29a:d9fa"; };
                                                            { "ws27-101.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c22::b29a:db54"; };
                                                            { "ws27-120.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c22::b29a:da5e"; };
                                                            { "ws27-128.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c22::b29a:da62"; };
                                                            { "ws27-144.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c22::b29a:da6a"; };
                                                            { "ws27-165.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c22::b29a:db74"; };
                                                            { "ws27-245.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c22::b29a:db9c"; };
                                                            { "ws27-326.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c22::b29a:dac5"; };
                                                            { "ws28-062.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c22::b29a:9041"; };
                                                            { "ws28-085.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c22::b29a:914c"; };
                                                            { "ws28-094.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c22::b29a:9051"; };
                                                            { "ws28-243.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c22::b29a:919b"; };
                                                            { "ws28-245.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c22::b29a:919c"; };
                                                            { "ws28-247.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c22::b29a:919d"; };
                                                            { "ws28-272.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c22::b29a:90aa"; };
                                                            { "ws28-467.search.yandex.net"; 18100; 17.656; "2a02:6b8:0:c22::b29a:9004"; };
                                                            { "ws29-011.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c22::b29a:9327"; };
                                                            { "ws29-083.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c22::b29a:934b"; };
                                                            { "ws29-123.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:c22::b29a:935f"; };
                                                            { "ws31-325.search.yandex.net"; 18100; 28.458; "2a02:6b8:0:f12::54c9:b2a9"; };
                                                            { "ws33-063.search.yandex.net"; 18100; 7.115; "2a02:6b8:0:f12::b29a:9d26"; };
                                                            { "ws35-011.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:160b::b29a:8507"; };
                                                            { "ws35-356.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:160b::b29a:84b4"; };
                                                            { "ws35-942.search.yandex.net"; 18100; 14.229; "2a02:6b8:0:160b::b29a:86df"; };
                                                            { "ws38-235.search.yandex.net"; 18100; 17.656; "2a02:6b8:0:c22::52d:ed77"; };
                                                            { "ws38-578.search.yandex.net"; 18100; 17.656; "2a02:6b8:0:c22::52d:ee29"; };
                                                            { "ws39-143.search.yandex.net"; 18100; 17.656; "2a02:6b8:0:2502::2509:5149"; };
                                                            { "ws39-144.search.yandex.net"; 18100; 17.656; "2a02:6b8:0:2502::2509:504a"; };
                                                            { "ws39-145.search.yandex.net"; 18100; 17.656; "2a02:6b8:0:2502::2509:514a"; };
                                                            { "ws39-146.search.yandex.net"; 18100; 17.656; "2a02:6b8:0:2502::2509:504b"; };
                                                            { "ws39-147.search.yandex.net"; 18100; 17.656; "2a02:6b8:0:2502::2509:514b"; };
                                                            { "ws39-149.search.yandex.net"; 18100; 17.656; "2a02:6b8:0:2502::2509:514c"; };
                                                            { "ws39-452.search.yandex.net"; 18100; 17.656; "2a02:6b8:0:2502::2509:50e4"; };
                                                            { "ws39-608.search.yandex.net"; 18100; 17.656; "2a02:6b8:0:2502::2509:5238"; };
                                                          }, {
                                                            resolve_timeout = "10ms";
                                                            connect_timeout = "30ms";
                                                            backend_timeout = "1s";
                                                            fail_on_5xx = true;
                                                            http_backend = true;
                                                            buffering = false;
                                                            keepalive_count = 0;
                                                            need_resolve = true;
                                                          }))
                                                        }; -- weighted2
                                                      }; -- balancer2
                                                    }; -- stats_eater
                                                  }; -- report
                                                }; -- click
                                              }; -- rewrite
                                            }; -- module
                                          }; -- antirobot
                                        }; -- cutter
                                      }; -- h100
                                    }; -- hasher
                                  }; -- upstream_clck
                                  upstream_cycounter = {
                                    priority = 10;
                                    match_fsm = {
                                      URI = "/cycounter(.*)?";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    hasher = {
                                      mode = "subnet";
                                      take_ip_from = "X-Real-IP";
                                      h100 = {
                                        cutter = {
                                          bytes = 512;
                                          timeout = "0.1s";
                                          antirobot = {
                                            cut_request = true;
                                            no_cut_request_file = "./controls/no_cut_request_file";
                                            file_switch = "./controls/disable_antirobot_module";
                                            cut_request_bytes = 512;
                                            checker = {
                                              shared = {
                                                uuid = "1283850100962082161";
                                              }; -- shared
                                            }; -- checker
                                            module = {
                                              report = {
                                                uuid = "cycounter";
                                                ranges = get_str_var("default_ranges");
                                                just_storage = false;
                                                disable_robotness = true;
                                                disable_sslness = true;
                                                events = {
                                                  stats = "report";
                                                }; -- events
                                                stats_eater = {
                                                  balancer2 = {
                                                    unique_policy = {};
                                                    attempts = 5;
                                                    weighted2 = {
                                                      slow_reply_time = "1s";
                                                      correction_params = {
                                                        max_weight = 5.000;
                                                        min_weight = 0.050;
                                                        history_time = "100s";
                                                        feedback_time = "300s";
                                                        plus_diff_per_sec = 0.050;
                                                        minus_diff_per_sec = 0.100;
                                                      }; -- correction_params
                                                      unpack(gen_proxy_backends({
                                                        { "imgs30-001.search.yandex.net"; 8899; 683.000; "2a02:6b8:0:f12::54c9:b603"; };
                                                        { "ws26-108.search.yandex.net"; 8899; 683.000; "2a02:6b8:0:c22::b29a:d858"; };
                                                        { "ws33-396.search.yandex.net"; 8899; 683.000; "2a02:6b8:0:f12::b29a:9ccd"; };
                                                        { "ws35-900.search.yandex.net"; 8899; 683.000; "2a02:6b8:0:160b::b29a:86ca"; };
                                                        { "ws36-190.search.yandex.net"; 8899; 683.000; "2a02:6b8:0:2502::2509:5461"; };
                                                        { "ws37-153.search.yandex.net"; 8899; 683.000; "2a02:6b8:0:2502::2509:5d4e"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "150ms";
                                                        backend_timeout = "10s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = true;
                                                      }))
                                                    }; -- weighted2
                                                  }; -- balancer2
                                                }; -- stats_eater
                                              }; -- report
                                            }; -- module
                                          }; -- antirobot
                                        }; -- cutter
                                      }; -- h100
                                    }; -- hasher
                                  }; -- upstream_cycounter
                                  upstream_suggest = {
                                    priority = 9;
                                    match_fsm = {
                                      URI = "/(suggest|suggest-mobile).*|/jquery\\.crossframeajax\\.html";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    hasher = {
                                      mode = "subnet";
                                      take_ip_from = "X-Real-IP";
                                      h100 = {
                                        cutter = {
                                          bytes = 512;
                                          timeout = "0.1s";
                                          antirobot = {
                                            cut_request = true;
                                            no_cut_request_file = "./controls/no_cut_request_file";
                                            file_switch = "./controls/disable_antirobot_module";
                                            cut_request_bytes = 512;
                                            checker = {
                                              shared = {
                                                uuid = "1283850100962082161";
                                              }; -- shared
                                            }; -- checker
                                            module = {
                                              hasher = {
                                                mode = "subnet";
                                                take_ip_from = "X-Forwarded-For-Y";
                                                subnet_v4_mask = 32;
                                                subnet_v6_mask = 128;
                                                report = {
                                                  uuid = "balancer-suggest";
                                                  ranges = get_str_var("default_ranges");
                                                  just_storage = false;
                                                  disable_robotness = true;
                                                  disable_sslness = true;
                                                  events = {
                                                    stats = "report";
                                                  }; -- events
                                                  stats_eater = {
                                                    balancer2 = {
                                                      unique_policy = {};
                                                      attempts = 2;
                                                      active = {
                                                        delay = "1s";
                                                        request = "GET /ping HTTP/1.1\nHost: beta.mobsearch.yandex.ru\n\n";
                                                        quorum = 8533.800;
                                                        hysteresis = 2586.000;
                                                        unpack(gen_proxy_backends({
                                                          { "iva1-0492.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:310c:ec4:7aff:fe52:c900"; };
                                                          { "iva1-0504.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:310c:ec4:7aff:fe52:c938"; };
                                                          { "iva1-0536.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:3100:ec4:7aff:fe51:524e"; };
                                                          { "iva1-0570.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:310d:ec4:7aff:fe52:ccda"; };
                                                          { "iva1-0576.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:310d:ec4:7aff:fe52:cdf2"; };
                                                          { "iva1-0585.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:310d:ec4:7aff:fe52:c754"; };
                                                          { "iva1-0589.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:3101:ec4:7aff:fe52:ca68"; };
                                                          { "iva1-0603.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:3101:ec4:7aff:fe51:5380"; };
                                                          { "iva1-0616.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:310e:ec4:7aff:fe51:5132"; };
                                                          { "iva1-0646.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:3110:ec4:7aff:fe51:5738"; };
                                                          { "iva1-0700.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:3102:ec4:7aff:fe51:5152"; };
                                                          { "iva1-0704.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:3102:ec4:7aff:fe51:51ee"; };
                                                          { "iva1-0762.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:3115:ec4:7aff:fe51:56b4"; };
                                                          { "iva1-0764.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:3106:ec4:7aff:fe51:5432"; };
                                                          { "iva1-0790.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:3108:ec4:7aff:fe51:52e8"; };
                                                          { "iva1-0792.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:3108:ec4:7aff:fe51:5634"; };
                                                          { "iva1-0806.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:3109:ec4:7aff:fe51:5040"; };
                                                          { "iva1-0808.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:3109:ec4:7aff:fe51:5782"; };
                                                          { "iva1-0811.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:3101:ec4:7aff:fe51:54c2"; };
                                                          { "iva1-0826.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:310a:ec4:7aff:fe51:5268"; };
                                                        }, {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "30ms";
                                                          backend_timeout = "150ms";
                                                          fail_on_5xx = true;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 0;
                                                          need_resolve = true;
                                                        }))
                                                      }; -- active
                                                      on_error = {
                                                        balancer2 = {
                                                          unique_policy = {};
                                                          attempts = 2;
                                                          hashing = {
                                                            unpack(gen_proxy_backends({
                                                              { "sas1-1463.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:179:215:b2ff:fea8:c2e"; };
                                                              { "sas1-1469.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:179:215:b2ff:fea8:71d8"; };
                                                              { "sas1-1473.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:179:215:b2ff:fea8:6d30"; };
                                                              { "sas1-1477.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:67a:215:b2ff:fea8:7064"; };
                                                              { "sas1-1480.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:67a:215:b2ff:fea8:6f34"; };
                                                              { "sas1-1515.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:17a:feaa:14ff:fea9:7ab0"; };
                                                              { "sas1-1545.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:17a:feaa:14ff:fea9:7980"; };
                                                              { "sas1-1546.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:17a:feaa:14ff:fea9:7ab2"; };
                                                              { "sas1-1548.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:17a:feaa:14ff:fea9:7a96"; };
                                                              { "sas1-1566.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:17a:feaa:14ff:fea9:6d66"; };
                                                              { "sas1-1723.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:178:215:b2ff:fea8:a7e"; };
                                                              { "sas1-1725.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:178:215:b2ff:fea8:aaa"; };
                                                              { "sas1-1761.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:17e:76d4:35ff:fe4b:5e83"; };
                                                              { "sas1-6087.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:164:428d:5cff:fe36:8a00"; };
                                                              { "sas1-6091.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:164:428d:5cff:fe36:8a5c"; };
                                                              { "sas1-6092.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:164:428d:5cff:fe36:8a24"; };
                                                              { "sas1-6094.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:164:428d:5cff:fe36:8a5a"; };
                                                              { "sas1-6097.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:164:428d:5cff:fe36:8b66"; };
                                                              { "sas1-6098.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:164:428d:5cff:fe34:f29a"; };
                                                              { "sas1-9408.search.yandex.net"; 8041; 1293.000; "2a02:6b8:b000:13a:feaa:14ff:fea9:798e"; };
                                                            }, {
                                                              resolve_timeout = "10ms";
                                                              connect_timeout = "150ms";
                                                              backend_timeout = "300ms";
                                                              fail_on_5xx = true;
                                                              http_backend = true;
                                                              buffering = false;
                                                              keepalive_count = 0;
                                                              need_resolve = true;
                                                            }))
                                                          }; -- hashing
                                                        }; -- balancer2
                                                      }; -- on_error
                                                    }; -- balancer2
                                                  }; -- stats_eater
                                                }; -- report
                                              }; -- hasher
                                            }; -- module
                                          }; -- antirobot
                                        }; -- cutter
                                      }; -- h100
                                    }; -- hasher
                                  }; -- upstream_suggest
                                  upstream_mediahostsearch = {
                                    priority = 8;
                                    match_fsm = {
                                      host = "(web|zen|.*people|.*ludi|.*peoplesearch|oyun|play|game|games|twitter|video)\\.yandex\\..*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    hasher = {
                                      mode = "subnet";
                                      take_ip_from = "X-Real-IP";
                                      h100 = {
                                        cutter = {
                                          bytes = 512;
                                          timeout = "0.1s";
                                          antirobot = {
                                            cut_request = true;
                                            no_cut_request_file = "./controls/no_cut_request_file";
                                            file_switch = "./controls/disable_antirobot_module";
                                            cut_request_bytes = 512;
                                            checker = {
                                              shared = {
                                                uuid = "1283850100962082161";
                                              }; -- shared
                                            }; -- checker
                                            module = {
                                              report = {
                                                uuid = "mediahostsearch";
                                                ranges = get_str_var("default_ranges");
                                                just_storage = false;
                                                disable_robotness = true;
                                                disable_sslness = true;
                                                events = {
                                                  stats = "report";
                                                }; -- events
                                                shared = {
                                                  uuid = "33145912284926247";
                                                  stats_eater = {
                                                    balancer2 = {
                                                      unique_policy = {};
                                                      attempts = 5;
                                                      weighted2 = {
                                                        slow_reply_time = "1s";
                                                        correction_params = {
                                                          max_weight = 5.000;
                                                          min_weight = 0.050;
                                                          history_time = "100s";
                                                          feedback_time = "300s";
                                                          plus_diff_per_sec = 0.050;
                                                          minus_diff_per_sec = 0.100;
                                                        }; -- correction_params
                                                        unpack(gen_proxy_backends({
                                                          { "iva1-0897.search.yandex.net"; 8080; 1130.000; "2a02:6b8:b000:3116:225:90ff:fec5:1774"; };
                                                          { "sfront6-001.search.yandex.net"; 8080; 819.000; "2a02:6b8:0:888::d5b4:c612"; };
                                                          { "sfront7-001.search.yandex.net"; 8080; 819.000; "2a02:6b8:0:1498::b29a:8c0f"; };
                                                          { "sfront8-001.search.yandex.net"; 8080; 819.000; "2a02:6b8:0:c24::d5b4:d603"; };
                                                          { "ws26-240.search.yandex.net"; 8080; 1130.000; "2a02:6b8:0:c22::b29a:d89a"; };
                                                          { "ws30-490.search.yandex.net"; 8080; 1293.000; "2a02:6b8:0:f12::54c9:b6f3"; };
                                                          { "ws31-387.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:f12::54c9:b2c8"; };
                                                          { "ws34-272.search.yandex.net"; 8080; 718.000; "2a02:6b8:0:f12::b29a:9e8f"; };
                                                          { "ws35-840.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:160b::b29a:86ac"; };
                                                          { "ws36-037.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:2502::2509:5514"; };
                                                          { "ws36-038.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:2502::2509:5415"; };
                                                          { "ws36-109.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:2502::2509:5538"; };
                                                          { "ws36-110.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:2502::2509:5439"; };
                                                          { "ws37-553.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:2502::2509:5f1c"; };
                                                          { "ws37-833.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:2502::2509:5fa8"; };
                                                          { "ws39-064.search.yandex.net"; 8080; 1130.000; "2a02:6b8:0:2502::2509:5022"; };
                                                          { "ws40-142.search.yandex.net"; 8080; 1130.000; "2a02:6b8:0:2502::258c:8049"; };
                                                        }, {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "150ms";
                                                          backend_timeout = "10s";
                                                          fail_on_5xx = true;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 0;
                                                          need_resolve = true;
                                                        }))
                                                      }; -- weighted2
                                                    }; -- balancer2
                                                  }; -- stats_eater
                                                }; -- shared
                                              }; -- report
                                            }; -- module
                                          }; -- antirobot
                                        }; -- cutter
                                      }; -- h100
                                    }; -- hasher
                                  }; -- upstream_mediahostsearch
                                  upstream_imagestoday = {
                                    priority = 7;
                                    match_fsm = {
                                      URI = "/(images|gorsel)/today.*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    hasher = {
                                      mode = "subnet";
                                      take_ip_from = "X-Real-IP";
                                      h100 = {
                                        cutter = {
                                          bytes = 512;
                                          timeout = "0.1s";
                                          antirobot = {
                                            cut_request = true;
                                            no_cut_request_file = "./controls/no_cut_request_file";
                                            file_switch = "./controls/disable_antirobot_module";
                                            cut_request_bytes = 512;
                                            checker = {
                                              shared = {
                                                uuid = "1283850100962082161";
                                              }; -- shared
                                            }; -- checker
                                            module = {
                                              geobase = {
                                                trusted = false;
                                                geo_host = "laas.yandex.ru";
                                                take_ip_from = "X-Forwarded-For-Y";
                                                laas_answer_header = "X-LaaS-Answered";
                                                file_switch = "./controls/disable_geobase.switch";
                                                geo = {
                                                  shared = {
                                                    uuid = "3710244868187230547";
                                                  }; -- shared
                                                }; -- geo
                                                geo_path = "/region?response_format=header&version=1&service=balancer&service=hamster.yandex.com.tr";
                                                regexp = {
                                                  exp_testing = {
                                                    priority = 2;
                                                    match_fsm = {
                                                      cgi = "(exp-testing=da|exp_confs=testing)";
                                                      case_insensitive = true;
                                                      surround = true;
                                                    }; -- match_fsm
                                                    headers = {
                                                      create = {
                                                        ["X-L7-EXP-Testing"] = "true";
                                                      }; -- create
                                                      shared = {
                                                        uuid = "6577303422447989330";
                                                        exp_getter = {
                                                          trusted = false;
                                                          file_switch = "./controls/expgetter.switch";
                                                          service_name = "images";
                                                          service_name_header = "Y-Service";
                                                          uaas = {
                                                            shared = {
                                                              uuid = "2699981958704336253";
                                                            }; -- shared
                                                          }; -- uaas
                                                          report = {
                                                            uuid = "imagessearch";
                                                            ranges = get_str_var("default_ranges");
                                                            just_storage = false;
                                                            disable_robotness = true;
                                                            disable_sslness = true;
                                                            events = {
                                                              stats = "report";
                                                            }; -- events
                                                            request_replier = {
                                                              sink = {
                                                                shared = {
                                                                  uuid = "6068248889932265530";
                                                                }; -- shared
                                                              }; -- sink
                                                              enable_failed_requests_replication = false;
                                                              rate = 0.000;
                                                              rate_file = "./controls/request_replier_images.ratefile";
                                                              stats_eater = {
                                                                balancer2 = {
                                                                  unique_policy = {};
                                                                  attempts = 5;
                                                                  weighted2 = {
                                                                    slow_reply_time = "1s";
                                                                    correction_params = {
                                                                      max_weight = 5.000;
                                                                      min_weight = 0.050;
                                                                      history_time = "100s";
                                                                      feedback_time = "300s";
                                                                      plus_diff_per_sec = 0.050;
                                                                      minus_diff_per_sec = 0.100;
                                                                    }; -- correction_params
                                                                    unpack(gen_proxy_backends({
                                                                      { "iva1-0897.search.yandex.net"; 8080; 1130.000; "2a02:6b8:b000:3116:225:90ff:fec5:1774"; };
                                                                      { "sfront6-001.search.yandex.net"; 8080; 819.000; "2a02:6b8:0:888::d5b4:c612"; };
                                                                      { "sfront7-001.search.yandex.net"; 8080; 819.000; "2a02:6b8:0:1498::b29a:8c0f"; };
                                                                      { "sfront8-001.search.yandex.net"; 8080; 819.000; "2a02:6b8:0:c24::d5b4:d603"; };
                                                                      { "ws26-240.search.yandex.net"; 8080; 1130.000; "2a02:6b8:0:c22::b29a:d89a"; };
                                                                      { "ws30-490.search.yandex.net"; 8080; 1293.000; "2a02:6b8:0:f12::54c9:b6f3"; };
                                                                      { "ws31-387.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:f12::54c9:b2c8"; };
                                                                      { "ws34-272.search.yandex.net"; 8080; 718.000; "2a02:6b8:0:f12::b29a:9e8f"; };
                                                                      { "ws35-840.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:160b::b29a:86ac"; };
                                                                      { "ws36-037.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:2502::2509:5514"; };
                                                                      { "ws36-038.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:2502::2509:5415"; };
                                                                      { "ws36-109.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:2502::2509:5538"; };
                                                                      { "ws36-110.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:2502::2509:5439"; };
                                                                      { "ws37-553.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:2502::2509:5f1c"; };
                                                                      { "ws37-833.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:2502::2509:5fa8"; };
                                                                      { "ws39-064.search.yandex.net"; 8080; 1130.000; "2a02:6b8:0:2502::2509:5022"; };
                                                                      { "ws40-142.search.yandex.net"; 8080; 1130.000; "2a02:6b8:0:2502::258c:8049"; };
                                                                    }, {
                                                                      resolve_timeout = "10ms";
                                                                      connect_timeout = "150ms";
                                                                      backend_timeout = "10s";
                                                                      fail_on_5xx = true;
                                                                      http_backend = true;
                                                                      buffering = true;
                                                                      keepalive_count = 0;
                                                                      need_resolve = true;
                                                                    }))
                                                                  }; -- weighted2
                                                                }; -- balancer2
                                                              }; -- stats_eater
                                                            }; -- request_replier
                                                          }; -- report
                                                        }; -- exp_getter
                                                      }; -- shared
                                                    }; -- headers
                                                  }; -- exp_testing
                                                  default = {
                                                    priority = 1;
                                                    headers = {
                                                      create = {
                                                        ["X-L7-EXP"] = "true";
                                                      }; -- create
                                                      shared = {
                                                        uuid = "6577303422447989330";
                                                      }; -- shared
                                                    }; -- headers
                                                  }; -- default
                                                }; -- regexp
                                              }; -- geobase
                                            }; -- module
                                          }; -- antirobot
                                        }; -- cutter
                                      }; -- h100
                                    }; -- hasher
                                  }; -- upstream_imagestoday
                                  upstream_images = {
                                    priority = 6;
                                    match_or = {
                                      {
                                        match_fsm = {
                                          URI = "/(images|gorsel)(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      };
                                      {
                                        match_fsm = {
                                          host = ".*(images|gorsel)(\\..*)?\\.yandex\\..*";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      };
                                      {
                                        match_and = {
                                          {
                                            match_fsm = {
                                              URI = "/(search/xml|xmlsearch)(.*)?";
                                              case_insensitive = true;
                                              surround = false;
                                            }; -- match_fsm
                                          };
                                          {
                                            match_fsm = {
                                              cgi = "type=(pictures|cbir|cbirlike|picturedups)";
                                              case_insensitive = true;
                                              surround = true;
                                            }; -- match_fsm
                                          };
                                        }; -- match_and
                                      };
                                    }; -- match_or
                                    shared = {
                                      uuid = "3146088339605898263";
                                    }; -- shared
                                  }; -- upstream_images
                                  upstream_searchapp = {
                                    priority = 5;
                                    match_fsm = {
                                      URI = "/searchapp(.*)?";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    hasher = {
                                      mode = "subnet";
                                      take_ip_from = "X-Real-IP";
                                      h100 = {
                                        cutter = {
                                          bytes = 512;
                                          timeout = "0.1s";
                                          antirobot = {
                                            cut_request = true;
                                            no_cut_request_file = "./controls/no_cut_request_file";
                                            file_switch = "./controls/disable_antirobot_module";
                                            cut_request_bytes = 512;
                                            checker = {
                                              shared = {
                                                uuid = "1283850100962082161";
                                              }; -- shared
                                            }; -- checker
                                            module = {
                                              geobase = {
                                                trusted = false;
                                                geo_host = "laas.yandex.ru";
                                                take_ip_from = "X-Forwarded-For-Y";
                                                laas_answer_header = "X-LaaS-Answered";
                                                file_switch = "./controls/disable_geobase.switch";
                                                geo = {
                                                  shared = {
                                                    uuid = "3710244868187230547";
                                                  }; -- shared
                                                }; -- geo
                                                geo_path = "/region?response_format=header&version=1&service=balancer&service=hamster.yandex.com.tr";
                                                regexp = {
                                                  exp_testing = {
                                                    priority = 2;
                                                    match_fsm = {
                                                      cgi = "(exp-testing=da|exp_confs=testing)";
                                                      case_insensitive = true;
                                                      surround = true;
                                                    }; -- match_fsm
                                                    headers = {
                                                      create = {
                                                        ["X-L7-EXP-Testing"] = "true";
                                                      }; -- create
                                                      shared = {
                                                        uuid = "7767510489282304795";
                                                        exp_getter = {
                                                          trusted = false;
                                                          file_switch = "./controls/expgetter.switch";
                                                          service_name = "touch";
                                                          service_name_header = "Y-Service";
                                                          headers_size_limit = 2048;
                                                          uaas = {
                                                            shared = {
                                                              uuid = "2699981958704336253";
                                                            }; -- shared
                                                          }; -- uaas
                                                          report = {
                                                            uuid = "searchapp";
                                                            ranges = get_str_var("default_ranges");
                                                            just_storage = false;
                                                            disable_robotness = true;
                                                            disable_sslness = true;
                                                            events = {
                                                              stats = "report";
                                                            }; -- events
                                                            stats_eater = {
                                                              balancer2 = {
                                                                unique_policy = {};
                                                                attempts = 5;
                                                                weighted2 = {
                                                                  slow_reply_time = "1s";
                                                                  correction_params = {
                                                                    max_weight = 5.000;
                                                                    min_weight = 0.050;
                                                                    history_time = "100s";
                                                                    feedback_time = "300s";
                                                                    plus_diff_per_sec = 0.050;
                                                                    minus_diff_per_sec = 0.100;
                                                                  }; -- correction_params
                                                                  unpack(gen_proxy_backends({
                                                                    { "man1-1381.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:e8d0"; };
                                                                    { "man1-1759.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:6027:f652:14ff:fe8b:bf40"; };
                                                                    { "man1-1930.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:bdc0"; };
                                                                    { "man1-2009.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:2a30"; };
                                                                    { "man1-2254.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:d610"; };
                                                                    { "man1-2468.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:6021:f652:14ff:fe55:3480"; };
                                                                    { "man1-2531.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:6023:f652:14ff:fe8c:ff0"; };
                                                                    { "man1-2978.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:dbd0"; };
                                                                    { "man1-3021.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:6028:f652:14ff:fe55:3fc0"; };
                                                                    { "man1-3825.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:603a:92e2:baff:fe6f:81da"; };
                                                                    { "man1-4352.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:6044:92e2:baff:fe6f:7fba"; };
                                                                    { "man1-4354.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:6041:92e2:baff:fe74:761e"; };
                                                                    { "man1-4410.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7d98"; };
                                                                    { "man1-4487.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:6041:92e2:baff:fe6e:bd86"; };
                                                                    { "man1-5985.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:8830"; };
                                                                    { "man1-6011.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:8220"; };
                                                                    { "man1-6066.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:6056:e61d:2dff:fe00:9570"; };
                                                                    { "man1-6118.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:3e10"; };
                                                                    { "man1-6119.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4660"; };
                                                                    { "man1-6264.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:6057:e61d:2dff:fe03:5360"; };
                                                                    { "sas1-0613.search.yandex.net"; 7345; 1130.000; "2a02:6b8:b000:144:225:90ff:fe82:ffc6"; };
                                                                    { "sas1-0623.search.yandex.net"; 7345; 1130.000; "2a02:6b8:b000:15f:225:90ff:fe4f:f6e6"; };
                                                                    { "sas1-0907.search.yandex.net"; 7345; 1130.000; "2a02:6b8:b000:15d:225:90ff:fe83:1892"; };
                                                                    { "sas1-1297.search.yandex.net"; 7345; 1130.000; "2a02:6b8:b000:163:225:90ff:fe92:4a1a"; };
                                                                    { "sas1-1302.search.yandex.net"; 7345; 1130.000; "2a02:6b8:b000:163:225:90ff:fe94:2ac8"; };
                                                                    { "sas1-1673.search.yandex.net"; 7345; 1130.000; "2a02:6b8:b000:615:922b:34ff:fecf:3fc2"; };
                                                                    { "sas1-1764.search.yandex.net"; 7345; 1130.000; "2a02:6b8:b000:61a:922b:34ff:fecf:22ac"; };
                                                                    { "sas1-2253.search.yandex.net"; 7345; 1130.000; "2a02:6b8:b000:624:922b:34ff:fecf:4176"; };
                                                                    { "sas1-2349.search.yandex.net"; 7345; 1130.000; "2a02:6b8:b000:627:922b:34ff:fecf:3094"; };
                                                                    { "sas1-2522.search.yandex.net"; 7345; 1130.000; "2a02:6b8:b000:60b:225:90ff:fe83:1aca"; };
                                                                    { "sas1-2694.search.yandex.net"; 7345; 1130.000; "2a02:6b8:b000:608:225:90ff:fe83:17d0"; };
                                                                    { "sas1-2801.search.yandex.net"; 7345; 1130.000; "2a02:6b8:b000:60c:225:90ff:fe83:129c"; };
                                                                    { "sas1-4184.search.yandex.net"; 7345; 1130.000; "2a02:6b8:b000:63a:96de:80ff:fe81:ad2"; };
                                                                    { "sas1-4518.search.yandex.net"; 7345; 1130.000; "2a02:6b8:b000:639:96de:80ff:fe81:102e"; };
                                                                    { "sas1-4781.search.yandex.net"; 7345; 1130.000; "2a02:6b8:b000:63b:96de:80ff:fe81:100a"; };
                                                                    { "sas1-5281.search.yandex.net"; 7345; 1130.000; "2a02:6b8:b000:644:96de:80ff:fe81:984"; };
                                                                    { "sas1-5283.search.yandex.net"; 7345; 1130.000; "2a02:6b8:b000:644:96de:80ff:fe81:1600"; };
                                                                    { "sas1-5678.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:660:76d4:35ff:fe62:eb84"; };
                                                                    { "sas1-5713.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:660:76d4:35ff:fe62:ea36"; };
                                                                    { "sas1-5733.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:662:76d4:35ff:fec4:22de"; };
                                                                    { "sas1-5757.search.yandex.net"; 7345; 1293.000; "2a02:6b8:b000:662:76d4:35ff:fe62:eb00"; };
                                                                  }, {
                                                                    resolve_timeout = "10ms";
                                                                    connect_timeout = "150ms";
                                                                    backend_timeout = "60s";
                                                                    fail_on_5xx = true;
                                                                    http_backend = true;
                                                                    buffering = false;
                                                                    keepalive_count = 0;
                                                                    need_resolve = true;
                                                                  }))
                                                                }; -- weighted2
                                                              }; -- balancer2
                                                            }; -- stats_eater
                                                          }; -- report
                                                        }; -- exp_getter
                                                      }; -- shared
                                                    }; -- headers
                                                  }; -- exp_testing
                                                  default = {
                                                    priority = 1;
                                                    headers = {
                                                      create = {
                                                        ["X-L7-EXP"] = "true";
                                                      }; -- create
                                                      shared = {
                                                        uuid = "7767510489282304795";
                                                      }; -- shared
                                                    }; -- headers
                                                  }; -- default
                                                }; -- regexp
                                              }; -- geobase
                                            }; -- module
                                          }; -- antirobot
                                        }; -- cutter
                                      }; -- h100
                                    }; -- hasher
                                  }; -- upstream_searchapp
                                  upstream_expgettermacro = {
                                    priority = 4;
                                    match_fsm = {
                                      host = "test";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    regexp = {
                                      exp_testing = {
                                        priority = 2;
                                        match_fsm = {
                                          cgi = "(exp-testing=da|exp_confs=testing)";
                                          case_insensitive = true;
                                          surround = true;
                                        }; -- match_fsm
                                        headers = {
                                          create = {
                                            ["X-L7-EXP-Testing"] = "true";
                                          }; -- create
                                          shared = {
                                            uuid = "1545129224075080947";
                                            exp_getter = {
                                              trusted = false;
                                              file_switch = "./controls/expgetter.switch";
                                              service_name = "news";
                                              service_name_header = "Y-Service";
                                              uaas = {
                                                shared = {
                                                  uuid = "2699981958704336253";
                                                }; -- shared
                                              }; -- uaas
                                              report = {
                                                uuid = "requests_news_to_sas";
                                                ranges = get_str_var("default_ranges");
                                                just_storage = false;
                                                disable_robotness = true;
                                                disable_sslness = true;
                                                events = {
                                                  stats = "report";
                                                }; -- events
                                                errordocument = {
                                                  status = 200;
                                                  force_conn_close = false;
                                                }; -- errordocument
                                              }; -- report
                                            }; -- exp_getter
                                          }; -- shared
                                        }; -- headers
                                      }; -- exp_testing
                                      default = {
                                        priority = 1;
                                        headers = {
                                          create = {
                                            ["X-L7-EXP"] = "true";
                                          }; -- create
                                          shared = {
                                            uuid = "1545129224075080947";
                                          }; -- shared
                                        }; -- headers
                                      }; -- default
                                    }; -- regexp
                                  }; -- upstream_expgettermacro
                                  upstream_video = {
                                    priority = 3;
                                    match_fsm = {
                                      URI = "/video(.*)?";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    shared = {
                                      uuid = "5403696927846419830";
                                    }; -- shared
                                  }; -- upstream_video
                                  upstream_search = {
                                    priority = 2;
                                    match_fsm = {
                                      URI = "/prefetch\\.txt|/bots|/403\\.html|/404\\.html|/500\\.html|/adresa-segmentator|/all-supported-params|/black\\.html|/cgi-bin/hidereferer|/cgi-bin/set-intl|/cgi-bin/xmlsearch\\.pl|/cgi-bin/yandpage|/cgi-bin/yandsearch|/chrome-add-search-provider-v2\\.html|/chrome-add-search-provider\\.html|/click|/cy|/dzen|/experiments\\.xml|/family|/familysearch|/padsearch|/jsonsearch|/formfeedback|/goto_issue/|/goto_rubric/|/i/yandex-big\\.gaf|/ie3/yandsearch|/images-data|/images\\.html|/index_m|/jsonproxy|/jsonsearch/images|/jsonsearch/video|/largesearch|/lego/blocks-desktop/i-social/closer/i-social__closer\\.html|/map/.+/news\\.html|/more_samples|/msearch|/msearchpart|/norobot|/opensearch\\.xml|/people|/person|/podpiska/login\\.pl|/quotes|/redir|/region_map|/regions_list\\.xml|/rubric2sport|/schoolsearch|/search|/search/advanced|/search/customize|/search/extra-snippet|/search/inforequest|/sitesearch|/sportagent|/storeclick|/storerequest|/telsearch|/toggle-experiment|/touchsearch|/versions|/white\\.html|/wpage|/xmlsearch|/yandpage|/yandsearch|/yca/cy|/v|/viewconfig|/search(/.*)?|/infected|/adult|/redir_warning|/post-indexes|/adresa-segmentator|/st/b-spec-adv/title\\.gif|/yandcache\\.js|/images.*|/video.*|/gorsel.*|/getarhost.*|/safaripromoter*|/search\\.xml|/player\\.xml|/iframe|/sitesearch/opensearch\\.xml|/auto-regions.*|/sport/.*|/tail-log|/csp(/.*)?";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    shared = {
                                      uuid = "5877762356895733154";
                                    }; -- shared
                                  }; -- upstream_search
                                  default = {
                                    priority = 1;
                                    hasher = {
                                      mode = "subnet";
                                      take_ip_from = "X-Real-IP";
                                      h100 = {
                                        cutter = {
                                          bytes = 512;
                                          timeout = "0.1s";
                                          antirobot = {
                                            cut_request = true;
                                            no_cut_request_file = "./controls/no_cut_request_file";
                                            file_switch = "./controls/disable_antirobot_module";
                                            cut_request_bytes = 512;
                                            checker = {
                                              shared = {
                                                uuid = "1283850100962082161";
                                              }; -- shared
                                            }; -- checker
                                            module = {
                                              geobase = {
                                                trusted = false;
                                                geo_host = "laas.yandex.ru";
                                                take_ip_from = "X-Forwarded-For-Y";
                                                laas_answer_header = "X-LaaS-Answered";
                                                file_switch = "./controls/disable_geobase.switch";
                                                geo = {
                                                  shared = {
                                                    uuid = "3710244868187230547";
                                                  }; -- shared
                                                }; -- geo
                                                geo_path = "/region?response_format=header&version=1&service=balancer&service=hamster.yandex.com.tr";
                                                regexp = {
                                                  exp_testing = {
                                                    priority = 2;
                                                    match_fsm = {
                                                      cgi = "(exp-testing=da|exp_confs=testing)";
                                                      case_insensitive = true;
                                                      surround = true;
                                                    }; -- match_fsm
                                                    headers = {
                                                      create = {
                                                        ["X-L7-EXP-Testing"] = "true";
                                                      }; -- create
                                                      shared = {
                                                        uuid = "542772884026284737";
                                                        exp_getter = {
                                                          trusted = false;
                                                          file_switch = "./controls/expgetter.switch";
                                                          uaas = {
                                                            shared = {
                                                              uuid = "2699981958704336253";
                                                            }; -- shared
                                                          }; -- uaas
                                                          report = {
                                                            uuid = "mordasearch";
                                                            ranges = get_str_var("default_ranges");
                                                            just_storage = false;
                                                            disable_robotness = true;
                                                            disable_sslness = true;
                                                            events = {
                                                              stats = "report";
                                                            }; -- events
                                                            shared = {
                                                              uuid = "1387621517456384857";
                                                            }; -- shared
                                                          }; -- report
                                                        }; -- exp_getter
                                                      }; -- shared
                                                    }; -- headers
                                                  }; -- exp_testing
                                                  default = {
                                                    priority = 1;
                                                    headers = {
                                                      create = {
                                                        ["X-L7-EXP"] = "true";
                                                      }; -- create
                                                      shared = {
                                                        uuid = "542772884026284737";
                                                      }; -- shared
                                                    }; -- headers
                                                  }; -- default
                                                }; -- regexp
                                              }; -- geobase
                                            }; -- module
                                          }; -- antirobot
                                        }; -- cutter
                                      }; -- h100
                                    }; -- hasher
                                  }; -- default
                                }; -- regexp
                              }; -- rpcrewrite
                            }; -- shared
                          }; -- response_headers
                        }; -- headers
                      }; -- hasher
                    }; -- yandex
                    default = {
                      priority = 1;
                      errordocument = {
                        status = 406;
                        force_conn_close = true;
                      }; -- errordocument
                    }; -- default
                  }; -- regexp
                }; -- webauth
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- errorlog
      }; -- shared
    }; -- section_1_80
    section_1_8180 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        8180;
      }; -- ports
      shared = {
        uuid = "600373725010471674";
      }; -- shared
    }; -- section_1_8180
    section_2_443 = {
      ips = {
        "2a02:6b8:0:3400::1:16";
        "2a02:6b8:0:3400::1:17";
      }; -- ips
      ports = {
        443;
      }; -- ports
      shared = {
        uuid = "2924368812049100509";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 8181, "/place/db/www/logs/");
          ssl_sni = {
            force_ssl = true;
            events = {
              stats = "report";
              reload_ocsp_response = "reload_ocsp";
              reload_ticket_keys = "reload_ticket";
            }; -- events
            http2_alpn_file = "./controls/http2_enable.ratefile";
            http2_alpn_freq = 0.550;
            contexts = {
              default = {
                priority = 1;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                ca = get_ca_cert_path("InternalYandexCA", "./");
                secrets_log = get_log_path("hamster_secrets", 8181, "./");
                log = get_log_path("ssl_sni", 8181, "/place/db/www/logs/");
                priv = get_private_cert_path("hamster.yandex.tld.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-hamster.yandex.tld.pem", "/dev/shm/balancer");
                client = {
                  verify_peer = true;
                  verify_depth = 3;
                  verify_once = true;
                  fail_if_no_peer_cert = true;
                }; -- client
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.hamster.yandex.tld.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.hamster.yandex.tld.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.hamster.yandex.tld.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- default
            }; -- contexts
            http2 = {
              goaway_debug_data_enabled = false;
              debug_log_enabled = false;
              events = {
                stats = "report";
              }; -- events
              http = {
                maxlen = 65536;
                maxreq = 65536;
                keepalive = true;
                no_keepalive_file = "./controls/keepalive_disabled";
                events = {
                  stats = "report";
                }; -- events
                accesslog = {
                  log = get_log_path("access_log", 8181, "/place/db/www/logs/");
                  report = {
                    uuid = "https";
                    refers = "service_total";
                    ranges = get_str_var("default_ranges");
                    just_storage = false;
                    disable_robotness = true;
                    disable_sslness = true;
                    events = {
                      stats = "report";
                    }; -- events
                    cookie_policy = {
                      uuid = "service_total";
                      default_yandex_policies = "stable";
                      webauth = {
                        auth_path = "/check_oauth_token";
                        checker = {
                          shared = {
                            uuid = "1527141021745917176";
                          }; -- shared
                        }; -- checker
                        on_forbidden = {
                          errordocument = {
                            status = 403;
                            force_conn_close = false;
                            content = "Access forbidden";
                          }; -- errordocument
                        }; -- on_forbidden
                        unauthorized_set_cookie = "webauth_csrf_token={csrf_token}; Path=/";
                        unauthorized_redirect = "https://oauth.yandex-team.ru/authorize?response_type=code&client_id={app_id}&state={csrf_state}";
                        role = "/webauth-qloud/qloud-ext/education/yashchenko-klein/envs/crowdtest/user";
                        regexp = {
                          yandex = {
                            priority = 2;
                            match_fsm = {
                              host = "(.*\\.)?(xn----7sbhgfw0a0bcg8l1a\\.xn--p1ai|xn--80aefebu0a0bbh8l\\.xn--p1ai|xn--d1acpjx3f\\.xn--p1ai|2yandex\\.ru|jandeks\\.com\\.tr|jandex\\.com\\.tr|kremlyandex\\.ru|video-yandex\\.ru|videoyandex\\.ru|wwwyandex\\.ru|xyyandex\\.net|ya-plus-plus\\.ru|ya-plusplus\\.ru|ya\\.nu|ya\\.rs|ya\\.ru|ya\\.tel|ya\\.tm|yanclex\\.ru|yandeks\\.com|yandeks\\.com\\.tr|yandes\\.ru|yandesk\\.com|yandesk\\.org|yandesk\\.ru|yandex-plus-plus\\.ru|yandex-plusplus\\.ru|yandex-rambler\\.ru|yandex-video\\.ru|yandex\\.asia|yandex\\.az|yandex\\.biz\\.tr|yandex\\.by|yandex\\.co\\.il|yandex\\.co\\.no|yandex\\.com|yandex\\.com\\.de|yandex\\.com\\.kz|yandex\\.com\\.ru|yandex\\.com\\.tr|yandex\\.com\\.ua|yandex\\.de|yandex\\.dk|yandex\\.do|yandex\\.ee|yandex\\.es|yandex\\.ie|yandex\\.in|yandex\\.info\\.tr|yandex\\.it|yandex\\.jobs|yandex\\.jp\\.net|yandex\\.kg|yandex\\.kz|yandex\\.lt|yandex\\.lu|yandex\\.lv|yandex\\.md|yandex\\.mobi|yandex\\.mx|yandex\\.name|yandex\\.net|yandex\\.net\\.ru|yandex\\.no|yandex\\.nu|yandex\\.org|yandex\\.pl|yandex\\.pt|yandex\\.qa|yandex\\.ro|yandex\\.rs|yandex\\.ru|yandex\\.sk|yandex\\.st|yandex\\.sx|yandex\\.tj|yandex\\.tm|yandex\\.ua|yandex\\.uz|yandex\\.web\\.tr|yandex\\.xxx|yandexbox\\.ru|yandexmedia\\.ru|yandexplusplus\\.ru|yandexvideo\\.ru|yandfex\\.ru|yandx\\.ru|yaplusplus\\.ru|yandex\\.com\\.ge|yandex\\.fr|yandex\\.az|yandex\\.uz|yandex\\.com\\.am|yandex\\.co\\.il|yandex\\.kg|yandex\\.lt|yandex\\.lv|yandex\\.md|yandex\\.tj|yandex\\.tm|yandex\\.ee)(:\\d+|\\.)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            hasher = {
                              mode = "subnet";
                              subnet_v4_mask = 32;
                              subnet_v6_mask = 128;
                              headers = {
                                create_func = {
                                  ["X-Forwarded-For-Y"] = "realip";
                                  ["X-Req-Id"] = "reqid";
                                  ["X-Source-Port-Y"] = "realport";
                                  ["X-Start-Time"] = "starttime";
                                  ["X-Yandex-RandomUID"] = "yuid";
                                }; -- create_func
                                create = {
                                  ["X-Yandex-HTTPS"] = "yes";
                                }; -- create
                                response_headers = {
                                  delete = "Strict-Transport-Security|X-Yandex-Report-Type";
                                  create = {
                                    Authorization = get_str_env_var("TOKEN");
                                  }; -- create
                                  create_weak = {
                                    ["X-Content-Type-Options"] = "nosniff";
                                    ["X-XSS-Protection"] = "1; mode=block";
                                  }; -- create_weak
                                  shared = {
                                    uuid = "596626063809110048";
                                  }; -- shared
                                }; -- response_headers
                              }; -- headers
                            }; -- hasher
                          }; -- yandex
                          default = {
                            priority = 1;
                            errordocument = {
                              status = 406;
                              force_conn_close = true;
                            }; -- errordocument
                          }; -- default
                        }; -- regexp
                      }; -- webauth
                    }; -- cookie_policy
                  }; -- report
                }; -- accesslog
              }; -- http
            }; -- http2
          }; -- ssl_sni
        }; -- errorlog
      }; -- shared
    }; -- section_2_443
    section_2_8181 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        8181;
      }; -- ports
      shared = {
        uuid = "2924368812049100509";
      }; -- shared
    }; -- section_2_8181
    remote_ips_internal = {
      ips = {
        "2a02:6b8:0:3400::1050";
      }; -- ips
      ports = {
        80;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 8183, "/place/db/www/logs/");
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          accesslog = {
            log = get_log_path("access_log", 8183, "/place/db/www/logs/");
            report = {
              uuid = "internal";
              refers = "http,service_total";
              ranges = get_str_var("default_ranges");
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
              regexp = {
                upstream_images = {
                  priority = 3;
                  match_or = {
                    {
                      match_fsm = {
                        URI = "/(images|gorsel)(/.*)?";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_fsm = {
                        host = ".*(images|gorsel)(\\..*)?\\.yandex\\..*";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_and = {
                        {
                          match_fsm = {
                            URI = "/(search/xml|xmlsearch)(.*)?";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                        };
                        {
                          match_fsm = {
                            cgi = "type=(pictures|cbir|cbirlike|picturedups)";
                            case_insensitive = true;
                            surround = true;
                          }; -- match_fsm
                        };
                      }; -- match_and
                    };
                  }; -- match_or
                  shared = {
                    uuid = "3146088339605898263";
                    icookie = {
                      use_default_keys = true;
                      domains = ".yandex.ru,.yandex.tr";
                      trust_parent = false;
                      trust_children = false;
                      enable_set_cookie = true;
                      enable_decrypting = true;
                      decrypted_uid_header = "X-Yandex-ICookie";
                      error_header = "X-Yandex-ICookie-Error";
                      force_equal_to_yandexuid = false;
                      scheme_bitmask = 0;
                      hasher = {
                        mode = "subnet";
                        take_ip_from = "X-Real-IP";
                        h100 = {
                          cutter = {
                            bytes = 512;
                            timeout = "0.1s";
                            antirobot = {
                              cut_request = true;
                              no_cut_request_file = "./controls/no_cut_request_file";
                              file_switch = "./controls/disable_antirobot_module";
                              cut_request_bytes = 512;
                              checker = {
                                shared = {
                                  uuid = "1283850100962082161";
                                }; -- shared
                              }; -- checker
                              module = {
                                geobase = {
                                  trusted = false;
                                  geo_host = "laas.yandex.ru";
                                  take_ip_from = "X-Forwarded-For-Y";
                                  laas_answer_header = "X-LaaS-Answered";
                                  file_switch = "./controls/disable_geobase.switch";
                                  geo = {
                                    shared = {
                                      uuid = "3710244868187230547";
                                    }; -- shared
                                  }; -- geo
                                  geo_path = "/region?response_format=header&version=1&service=balancer&service=hamster.yandex.com.tr";
                                  regexp = {
                                    exp_testing = {
                                      priority = 2;
                                      match_fsm = {
                                        cgi = "(exp-testing=da|exp_confs=testing)";
                                        case_insensitive = true;
                                        surround = true;
                                      }; -- match_fsm
                                      headers = {
                                        create = {
                                          ["X-L7-EXP-Testing"] = "true";
                                        }; -- create
                                        shared = {
                                          uuid = "162902337883447668";
                                          exp_getter = {
                                            trusted = false;
                                            file_switch = "./controls/expgetter.switch";
                                            service_name = "images";
                                            service_name_header = "Y-Service";
                                            uaas = {
                                              shared = {
                                                uuid = "2699981958704336253";
                                              }; -- shared
                                            }; -- uaas
                                            report = {
                                              ranges = get_str_var("default_ranges");
                                              just_storage = false;
                                              disable_robotness = true;
                                              disable_sslness = true;
                                              events = {
                                                stats = "report";
                                              }; -- events
                                              request_replier = {
                                                sink = {
                                                  shared = {
                                                    uuid = "6068248889932265530";
                                                  }; -- shared
                                                }; -- sink
                                                enable_failed_requests_replication = false;
                                                rate = 0.000;
                                                rate_file = "./controls/request_replier_images.ratefile";
                                                shared = {
                                                  uuid = "33145912284926247";
                                                }; -- shared
                                              }; -- request_replier
                                              refers = "imagessearch";
                                            }; -- report
                                          }; -- exp_getter
                                        }; -- shared
                                      }; -- headers
                                    }; -- exp_testing
                                    default = {
                                      priority = 1;
                                      headers = {
                                        create = {
                                          ["X-L7-EXP"] = "true";
                                        }; -- create
                                        shared = {
                                          uuid = "162902337883447668";
                                        }; -- shared
                                      }; -- headers
                                    }; -- default
                                  }; -- regexp
                                }; -- geobase
                              }; -- module
                            }; -- antirobot
                          }; -- cutter
                        }; -- h100
                      }; -- hasher
                    }; -- icookie
                  }; -- shared
                }; -- upstream_images
                upstream_video = {
                  priority = 2;
                  match_fsm = {
                    URI = "/video(.*)?";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  shared = {
                    uuid = "5403696927846419830";
                    hasher = {
                      mode = "subnet";
                      take_ip_from = "X-Real-IP";
                      h100 = {
                        cutter = {
                          bytes = 512;
                          timeout = "0.1s";
                          antirobot = {
                            cut_request = true;
                            no_cut_request_file = "./controls/no_cut_request_file";
                            file_switch = "./controls/disable_antirobot_module";
                            cut_request_bytes = 512;
                            checker = {
                              shared = {
                                uuid = "1283850100962082161";
                                report = {
                                  uuid = "antirobot";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  stats_eater = {
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 2;
                                      hashing = {
                                        unpack(gen_proxy_backends({
                                          { "ws14-011.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::8d08:b330"; };
                                          { "ws26-201.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d986"; };
                                          { "ws26-466.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d803"; };
                                          { "ws26-467.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d804"; };
                                          { "ws26-468.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d805"; };
                                          { "ws26-469.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d806"; };
                                          { "ws26-470.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d8fe"; };
                                          { "ws26-471.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d8f6"; };
                                          { "ws26-472.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d8f7"; };
                                          { "ws26-473.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d8f8"; };
                                          { "ws26-474.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d8f9"; };
                                          { "ws26-476.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:c22::b29a:d8fb"; };
                                          { "ws27-230.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:da95"; };
                                          { "ws27-231.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:db95"; };
                                          { "ws27-449.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:db19"; };
                                          { "ws28-050.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:903b"; };
                                          { "ws28-051.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:913b"; };
                                          { "ws28-052.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:903c"; };
                                          { "ws28-195.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:9183"; };
                                          { "ws28-196.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:9084"; };
                                          { "ws28-199.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:9185"; };
                                          { "ws28-206.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:9089"; };
                                          { "ws28-207.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:9189"; };
                                          { "ws28-208.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:908a"; };
                                          { "ws28-214.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:908d"; };
                                          { "ws28-216.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:908e"; };
                                          { "ws28-217.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:918e"; };
                                          { "ws29-200.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:9286"; };
                                          { "ws29-201.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:9386"; };
                                          { "ws29-202.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:c22::b29a:9287"; };
                                          { "ws30-149.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:f12::54c9:b151"; };
                                          { "ws31-150.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:f12::54c9:b352"; };
                                          { "ws35-013.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:8508"; };
                                          { "ws35-019.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:850b"; };
                                          { "ws35-022.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:840d"; };
                                          { "ws35-028.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:8410"; };
                                          { "ws35-044.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:8418"; };
                                          { "ws35-045.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:8518"; };
                                          { "ws35-046.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:8419"; };
                                          { "ws35-047.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:8519"; };
                                          { "ws35-048.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:841a"; };
                                          { "ws35-049.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:851a"; };
                                          { "ws35-050.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:841b"; };
                                          { "ws35-051.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:851b"; };
                                          { "ws35-052.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:841c"; };
                                          { "ws35-053.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:851c"; };
                                          { "ws35-054.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::b29a:841d"; };
                                          { "ws36-002.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:2502::2509:5403"; };
                                          { "ws37-927.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:2502::2509:5fd7"; };
                                          { "ws37-934.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:2502::2509:5edb"; };
                                          { "ws37-935.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:2502::2509:5fdb"; };
                                          { "ws37-936.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:2502::2509:5edc"; };
                                          { "ws37-937.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:2502::2509:5fdc"; };
                                          { "ws37-938.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:2502::2509:5edd"; };
                                          { "ws37-939.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:2502::2509:5fdd"; };
                                          { "ws37-940.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:2502::2509:5ede"; };
                                          { "ws37-941.search.yandex.net"; 13512; 1130.000; "2a02:6b8:0:2502::2509:5fde"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "30ms";
                                          backend_timeout = "100ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- hashing
                                    }; -- balancer2
                                  }; -- stats_eater
                                }; -- report
                              }; -- shared
                            }; -- checker
                            module = {
                              geobase = {
                                trusted = true;
                                geo_host = "laas.yandex.ru";
                                take_ip_from = "X-Forwarded-For-Y";
                                laas_answer_header = "X-LaaS-Answered";
                                file_switch = "./controls/disable_geobase.switch";
                                geo_path = "/region?response_format=header&version=1&service=balancer&service=hamster.yandex.com.tr";
                                geo = {
                                  shared = {
                                    uuid = "3710244868187230547";
                                    report = {
                                      uuid = "geobasemodule";
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      stats_eater = {
                                        balancer2 = {
                                          simple_policy = {};
                                          attempts = 1;
                                          rr = {
                                            unpack(gen_proxy_backends({
                                              { "laas.yandex.ru"; 80; 1.000; "2a02:6b8::91"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "25ms";
                                              backend_timeout = "45ms";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 10;
                                              need_resolve = true;
                                            }))
                                          }; -- rr
                                        }; -- balancer2
                                      }; -- stats_eater
                                    }; -- report
                                  }; -- shared
                                }; -- geo
                                regexp = {
                                  exp_testing = {
                                    priority = 2;
                                    match_fsm = {
                                      cgi = "(exp-testing=da|exp_confs=testing)";
                                      case_insensitive = true;
                                      surround = true;
                                    }; -- match_fsm
                                    headers = {
                                      create = {
                                        ["X-L7-EXP-Testing"] = "true";
                                      }; -- create
                                      shared = {
                                        uuid = "9128551868339935139";
                                        exp_getter = {
                                          trusted = false;
                                          file_switch = "./controls/expgetter.switch";
                                          service_name = "video";
                                          service_name_header = "Y-Service";
                                          uaas = {
                                            shared = {
                                              uuid = "2699981958704336253";
                                              report = {
                                                uuid = "expgetter";
                                                ranges = get_str_var("default_ranges");
                                                just_storage = false;
                                                disable_robotness = true;
                                                disable_sslness = true;
                                                events = {
                                                  stats = "report";
                                                }; -- events
                                                stats_eater = {
                                                  balancer2 = {
                                                    by_name_policy = {
                                                      name = get_geo("bygeo_", "random");
                                                      simple_policy = {};
                                                    }; -- by_name_policy
                                                    attempts = 1;
                                                    rr = {
                                                      bygeo_man = {
                                                        weight = 1.000;
                                                        balancer2 = {
                                                          unique_policy = {};
                                                          attempts = 1;
                                                          connection_attempts = 5;
                                                          rr = {
                                                            unpack(gen_proxy_backends({
                                                              { "usersplit-1.gencfg-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:3ae1:100:1101::1111"; };
                                                            }, {
                                                              resolve_timeout = "10ms";
                                                              connect_timeout = "5ms";
                                                              backend_timeout = "10ms";
                                                              fail_on_5xx = true;
                                                              http_backend = true;
                                                              buffering = false;
                                                              keepalive_count = 1;
                                                              need_resolve = true;
                                                            }))
                                                          }; -- rr
                                                        }; -- balancer2
                                                      }; -- bygeo_man
                                                      bygeo_sas = {
                                                        weight = 1.000;
                                                        balancer2 = {
                                                          unique_policy = {};
                                                          attempts = 1;
                                                          connection_attempts = 5;
                                                          rr = {
                                                            unpack(gen_proxy_backends({
                                                              { "usersplit-2.gencfg-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:6a21:100:47b::1111"; };
                                                            }, {
                                                              resolve_timeout = "10ms";
                                                              connect_timeout = "5ms";
                                                              backend_timeout = "10ms";
                                                              fail_on_5xx = true;
                                                              http_backend = true;
                                                              buffering = false;
                                                              keepalive_count = 1;
                                                              need_resolve = true;
                                                            }))
                                                          }; -- rr
                                                        }; -- balancer2
                                                      }; -- bygeo_sas
                                                      bygeo_vla = {
                                                        weight = 1.000;
                                                        balancer2 = {
                                                          unique_policy = {};
                                                          attempts = 1;
                                                          connection_attempts = 5;
                                                          rr = {
                                                            unpack(gen_proxy_backends({
                                                              { "usersplit-3.gencfg-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:4fa5:10b:2909::1111"; };
                                                            }, {
                                                              resolve_timeout = "10ms";
                                                              connect_timeout = "5ms";
                                                              backend_timeout = "10ms";
                                                              fail_on_5xx = true;
                                                              http_backend = true;
                                                              buffering = false;
                                                              keepalive_count = 1;
                                                              need_resolve = true;
                                                            }))
                                                          }; -- rr
                                                        }; -- balancer2
                                                      }; -- bygeo_vla
                                                    }; -- rr
                                                    on_error = {
                                                      balancer2 = {
                                                        unique_policy = {};
                                                        attempts = 1;
                                                        rr = {
                                                          unpack(gen_proxy_backends({
                                                            { "uaas.search.yandex.net"; 80; 1.000; "2a02:6b8:0:3400::120"; };
                                                          }, {
                                                            resolve_timeout = "10ms";
                                                            connect_timeout = "20ms";
                                                            backend_timeout = "30ms";
                                                            fail_on_5xx = true;
                                                            http_backend = true;
                                                            buffering = false;
                                                            keepalive_count = 1;
                                                            need_resolve = true;
                                                          }))
                                                        }; -- rr
                                                      }; -- balancer2
                                                    }; -- on_error
                                                  }; -- balancer2
                                                }; -- stats_eater
                                              }; -- report
                                            }; -- shared
                                          }; -- uaas
                                          report = {
                                            uuid = "videosearch";
                                            ranges = get_str_var("default_ranges");
                                            just_storage = false;
                                            disable_robotness = true;
                                            disable_sslness = true;
                                            events = {
                                              stats = "report";
                                            }; -- events
                                            stats_eater = {
                                              balancer2 = {
                                                unique_policy = {};
                                                attempts = 5;
                                                weighted2 = {
                                                  slow_reply_time = "1s";
                                                  correction_params = {
                                                    max_weight = 5.000;
                                                    min_weight = 0.050;
                                                    history_time = "100s";
                                                    feedback_time = "300s";
                                                    plus_diff_per_sec = 0.050;
                                                    minus_diff_per_sec = 0.100;
                                                  }; -- correction_params
                                                  unpack(gen_proxy_backends({
                                                    { "mmeta27-04.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:c22::b29a:da09"; };
                                                    { "sfront8-000.search.yandex.net"; 8080; 819.000; "2a02:6b8:0:c24::d5b4:d602"; };
                                                    { "ws26-418.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:c22::b29a:d8f3"; };
                                                    { "ws30-120.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:f12::54c9:b043"; };
                                                    { "ws35-043.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:160b::b29a:8517"; };
                                                    { "ws35-122.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:160b::b29a:843f"; };
                                                    { "ws36-114.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:2502::2509:543b"; };
                                                    { "ws37-406.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:2502::2509:5ccd"; };
                                                    { "ws37-962.search.yandex.net"; 8080; 1130.000; "2a02:6b8:0:2502::2509:5ee9"; };
                                                  }, {
                                                    resolve_timeout = "10ms";
                                                    connect_timeout = "150ms";
                                                    backend_timeout = "10s";
                                                    fail_on_5xx = true;
                                                    http_backend = true;
                                                    buffering = true;
                                                    keepalive_count = 0;
                                                    need_resolve = true;
                                                  }))
                                                }; -- weighted2
                                              }; -- balancer2
                                            }; -- stats_eater
                                          }; -- report
                                        }; -- exp_getter
                                      }; -- shared
                                    }; -- headers
                                  }; -- exp_testing
                                  default = {
                                    priority = 1;
                                    headers = {
                                      create = {
                                        ["X-L7-EXP"] = "true";
                                      }; -- create
                                      shared = {
                                        uuid = "9128551868339935139";
                                      }; -- shared
                                    }; -- headers
                                  }; -- default
                                }; -- regexp
                              }; -- geobase
                            }; -- module
                          }; -- antirobot
                        }; -- cutter
                      }; -- h100
                    }; -- hasher
                  }; -- shared
                }; -- upstream_video
                upstream_search = {
                  priority = 1;
                  match_fsm = {
                    URI = "/prefetch\\.txt|/bots|/403\\.html|/404\\.html|/500\\.html|/adresa-segmentator|/all-supported-params|/black\\.html|/cgi-bin/hidereferer|/cgi-bin/set-intl|/cgi-bin/xmlsearch\\.pl|/cgi-bin/yandpage|/cgi-bin/yandsearch|/chrome-add-search-provider-v2\\.html|/chrome-add-search-provider\\.html|/click|/cy|/dzen|/experiments\\.xml|/family|/familysearch|/padsearch|/jsonsearch|/formfeedback|/goto_issue/|/goto_rubric/|/i/yandex-big\\.gaf|/ie3/yandsearch|/images-data|/images\\.html|/index_m|/jsonproxy|/jsonsearch/images|/jsonsearch/video|/largesearch|/lego/blocks-desktop/i-social/closer/i-social__closer\\.html|/map/.+/news\\.html|/more_samples|/msearch|/msearchpart|/norobot|/opensearch\\.xml|/people|/person|/podpiska/login\\.pl|/quotes|/redir|/region_map|/regions_list\\.xml|/rubric2sport|/schoolsearch|/search|/search/advanced|/search/customize|/search/extra-snippet|/search/inforequest|/sitesearch|/sportagent|/storeclick|/storerequest|/telsearch|/toggle-experiment|/touchsearch|/versions|/white\\.html|/wpage|/xmlsearch|/yandpage|/yandsearch|/yca/cy|/v|/viewconfig|/search(/.*)?|/infected|/adult|/redir_warning|/post-indexes|/adresa-segmentator|/st/b-spec-adv/title\\.gif|/yandcache\\.js|/images.*|/video.*|/gorsel.*|/getarhost.*|/safaripromoter*|/search\\.xml|/player\\.xml|/iframe|/sitesearch/opensearch\\.xml|/auto-regions.*|/sport/.*|/tail-log|/csp(/.*)?";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  shared = {
                    uuid = "5877762356895733154";
                    hasher = {
                      mode = "subnet";
                      take_ip_from = "X-Real-IP";
                      h100 = {
                        cutter = {
                          bytes = 512;
                          timeout = "0.1s";
                          antirobot = {
                            cut_request = true;
                            no_cut_request_file = "./controls/no_cut_request_file";
                            file_switch = "./controls/disable_antirobot_module";
                            cut_request_bytes = 512;
                            checker = {
                              shared = {
                                uuid = "1283850100962082161";
                              }; -- shared
                            }; -- checker
                            module = {
                              geobase = {
                                trusted = false;
                                geo_host = "laas.yandex.ru";
                                take_ip_from = "X-Forwarded-For-Y";
                                laas_answer_header = "X-LaaS-Answered";
                                file_switch = "./controls/disable_geobase.switch";
                                geo = {
                                  shared = {
                                    uuid = "3710244868187230547";
                                  }; -- shared
                                }; -- geo
                                geo_path = "/region?response_format=header&version=1&service=balancer&service=hamster.yandex.com.tr";
                                regexp = {
                                  exp_testing = {
                                    priority = 2;
                                    match_fsm = {
                                      cgi = "(exp-testing=da|exp_confs=testing)";
                                      case_insensitive = true;
                                      surround = true;
                                    }; -- match_fsm
                                    headers = {
                                      create = {
                                        ["X-L7-EXP-Testing"] = "true";
                                      }; -- create
                                      shared = {
                                        uuid = "2311644333025867568";
                                        exp_getter = {
                                          trusted = false;
                                          file_switch = "./controls/expgetter.switch";
                                          service_name = "web";
                                          service_name_header = "Y-Service";
                                          uaas = {
                                            shared = {
                                              uuid = "2699981958704336253";
                                            }; -- shared
                                          }; -- uaas
                                          report = {
                                            uuid = "yandsearch";
                                            ranges = get_str_var("default_ranges");
                                            just_storage = false;
                                            disable_robotness = true;
                                            disable_sslness = true;
                                            events = {
                                              stats = "report";
                                            }; -- events
                                            request_replier = {
                                              sink = {
                                                shared = {
                                                  uuid = "6068248889932265530";
                                                  balancer2 = {
                                                    unique_policy = {};
                                                    attempts = 5;
                                                    weighted2 = {
                                                      slow_reply_time = "1s";
                                                      correction_params = {
                                                        max_weight = 5.000;
                                                        min_weight = 0.050;
                                                        history_time = "100s";
                                                        feedback_time = "300s";
                                                        plus_diff_per_sec = 0.050;
                                                        minus_diff_per_sec = 0.100;
                                                      }; -- correction_params
                                                      unpack(gen_proxy_backends({
                                                        { "sinkadm.priemka.yandex.ru"; 80; 1.000; "2a02:6b8:0:3400::eeee:20"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "150ms";
                                                        backend_timeout = "10s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = true;
                                                      }))
                                                    }; -- weighted2
                                                  }; -- balancer2
                                                }; -- shared
                                              }; -- sink
                                              enable_failed_requests_replication = false;
                                              rate = 0.000;
                                              rate_file = "./controls/request_replier_web.ratefile";
                                              shared = {
                                                uuid = "1387621517456384857";
                                                stats_eater = {
                                                  balancer2 = {
                                                    unique_policy = {};
                                                    attempts = 5;
                                                    weighted2 = {
                                                      slow_reply_time = "1s";
                                                      correction_params = {
                                                        max_weight = 5.000;
                                                        min_weight = 0.050;
                                                        history_time = "100s";
                                                        feedback_time = "300s";
                                                        plus_diff_per_sec = 0.050;
                                                        minus_diff_per_sec = 0.100;
                                                      }; -- correction_params
                                                      unpack(gen_proxy_backends({
                                                        { "mmeta33-06.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:f12::b29a:9c05"; };
                                                        { "mmeta34-01.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:f12::b29a:9f02"; };
                                                        { "mnews3-02.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:c24::d5b4:d6d7"; };
                                                        { "ws25-015.search.yandex.net"; 8080; 1293.000; "2a02:6b8:0:1498::b29a:8c52"; };
                                                        { "ws26-419.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:c22::b29a:d9f3"; };
                                                        { "ws27-038.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:c22::b29a:da35"; };
                                                        { "ws27-074.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:c22::b29a:da47"; };
                                                        { "ws27-161.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:c22::b29a:db72"; };
                                                        { "ws27-197.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:c22::b29a:db84"; };
                                                        { "ws29-064.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:c22::b29a:9242"; };
                                                        { "ws29-349.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:c22::b29a:93d0"; };
                                                        { "ws29-453.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:c22::b29a:931b"; };
                                                        { "ws30-411.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:f12::54c9:b1d4"; };
                                                        { "ws31-464.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:f12::54c9:b3ef"; };
                                                        { "ws35-123.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:160b::b29a:853f"; };
                                                        { "ws35-435.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:160b::b29a:85db"; };
                                                        { "ws35-769.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:160b::b29a:8788"; };
                                                        { "ws36-902.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:2502::2509:56cb"; };
                                                        { "ws37-539.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:2502::2509:5f15"; };
                                                        { "ws37-621.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:2502::2509:5f3e"; };
                                                        { "ws37-636.search.yandex.net"; 8080; 683.000; "2a02:6b8:0:2502::2509:5e46"; };
                                                        { "ws39-340.search.yandex.net"; 8080; 1130.000; "2a02:6b8:0:2502::2509:50ac"; };
                                                        { "ws39-782.search.yandex.net"; 8080; 1130.000; "2a02:6b8:0:2502::2509:528f"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "150ms";
                                                        backend_timeout = "10s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = true;
                                                        keepalive_count = 0;
                                                        need_resolve = true;
                                                      }))
                                                    }; -- weighted2
                                                  }; -- balancer2
                                                }; -- stats_eater
                                              }; -- shared
                                            }; -- request_replier
                                          }; -- report
                                        }; -- exp_getter
                                      }; -- shared
                                    }; -- headers
                                  }; -- exp_testing
                                  default = {
                                    priority = 1;
                                    headers = {
                                      create = {
                                        ["X-L7-EXP"] = "true";
                                      }; -- create
                                      shared = {
                                        uuid = "2311644333025867568";
                                      }; -- shared
                                    }; -- headers
                                  }; -- default
                                }; -- regexp
                              }; -- geobase
                            }; -- module
                          }; -- antirobot
                        }; -- cutter
                      }; -- h100
                    }; -- hasher
                  }; -- shared
                }; -- upstream_search
              }; -- regexp
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- remote_ips_internal
  }; -- ipdispatch
}