default_ciphers = "kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2"


default_ciphers_ecdsa = "ECDHE-ECDSA-AES128-GCM-SHA256:kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2"


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
  dns_ttl = get_random_timedelta(600, 900, "s");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", 8180, "/place/db/www/logs/");
  config_check = {
    quorums_file = "./controls/xxx";
  }; -- config_check
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
      port = 16100;
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
      port = 4443;
      ip = "127.0.0.1";
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
        16100;
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
          errordocument = {
            status = 200;
            force_conn_close = false;
          }; -- errordocument
        }; -- http
      }; -- report
    }; -- stats_storage
    remote_ips_80 = {
      ips = {
        "2a02:6b8:0:3400::1:16";
        "2a02:6b8:0:3400::1:17";
      }; -- ips
      ports = {
        80;
      }; -- ports
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
              hasher = {
                mode = "subnet";
                subnet_v4_mask = 32;
                subnet_v6_mask = 128;
                regexp = {
                  upstream_search = {
                    priority = 2;
                    match_fsm = {
                      URI = "/prefetch";
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
                            uuid = "5547488799812893677";
                            exp_getter = {
                              trusted = false;
                              file_switch = "./controls/expgetter.switch";
                              service_name = "web";
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
                                    http2_backend = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
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
                            uuid = "5547488799812893677";
                          }; -- shared
                        }; -- headers
                      }; -- default
                    }; -- regexp
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
                                    attempts = 3;
                                    hashing = {
                                      unpack(gen_proxy_backends({
                                        { "ws14-011.search.yandex.net"; 13512; 683.000; "2a02:6b8:0:160b::8d08:b330"; };
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
                            }; -- checker
                            module = {
                              geobase = {
                                trusted = false;
                                geo_host = "laas.yandex.ru";
                                take_ip_from = "X-Forwarded-For-Y";
                                laas_answer_header = "X-LaaS-Answered";
                                file_switch = "./controls/disable_geobase.switch";
                                geo_path = "/region?response_format=header&version=1&service=balancer";
                                geo = {
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
                                        attempts = 2;
                                        rr = {
                                          unpack(gen_proxy_backends({
                                            { "laas2.yandex.net"; 80; 1.000; "2a02:6b8:0:3400::1022"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "15ms";
                                            backend_timeout = "20ms";
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
                                        uuid = "7573238096116950156";
                                        exp_getter = {
                                          trusted = false;
                                          file_switch = "./controls/expgetter.switch";
                                          uaas = {
                                            shared = {
                                              uuid = "2699981958704336253";
                                            }; -- shared
                                          }; -- uaas
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
                                        uuid = "7573238096116950156";
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
              }; -- hasher
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- remote_ips_80
    https_1 = {
      ips = {
        "2a02:6b8:0:3400::1:16";
      }; -- ips
      ports = {
        443;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 8180, "/place/db/www/logs/");
        ssl_sni = {
          force_ssl = true;
          events = {
            stats = "report";
            reload_ocsp_response = "reload_ocsp";
            reload_ticket_keys = "reload_ticket";
          }; -- events
          contexts = {
            default = {
              priority = 1;
              timeout = "100800s";
              ciphers = get_str_var("default_ciphers_ecdsa");
              log = get_log_path("ssl_sni", 8180, "/place/db/www/logs/");
              priv = get_private_cert_path("beta.mobsearch.yandex.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-beta.mobsearch.yandex.ru.pem", "/dev/shm/balancer");
              secondary = {
                priv = get_private_cert_path("beta.mobsearch.yandex.ru_secondary.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-beta.mobsearch.yandex.ru_secondary.pem", "/dev/shm/balancer");
              }; -- secondary
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.beta.mobsearch.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.beta.mobsearch.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.beta.mobsearch.yandex.ru.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- default
          }; -- contexts
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
                uuid = "https";
                refers = "service_total";
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
            }; -- accesslog
          }; -- http
        }; -- ssl_sni
      }; -- errorlog
    }; -- https_1
    https_2 = {
      ips = {
        "2a02:6b8:0:3400::1:17";
      }; -- ips
      ports = {
        443;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 8180, "/place/db/www/logs/");
        ssl_sni = {
          force_ssl = true;
          events = {
            stats = "report";
            reload_ocsp_response = "reload_ocsp";
            reload_ticket_keys = "reload_ticket";
          }; -- events
          contexts = {
            default = {
              priority = 1;
              timeout = "100800s";
              disable_sslv3 = true;
              ocsp_file_switch = "./controls/disable_ocsp";
              ciphers = get_str_var("default_ciphers_ecdsa");
              ocsp = "./ocsp/allCAs-beta.mobsearch.yandex.ru.der";
              log = get_log_path("ssl_sni", 8180, "/place/db/www/logs/");
              secrets_log = get_log_path("secrets_log", 8180, "/place/db/www/logs/");
              priv = get_private_cert_path("beta.mobsearch.yandex.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-beta.mobsearch.yandex.ru.pem", "/dev/shm/balancer");
              secondary = {
                priv = get_private_cert_path("beta.mobsearch.yandex.ru_secondary.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-beta.mobsearch.yandex.ru_secondary.pem", "/dev/shm/balancer");
              }; -- secondary
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = "/dev/shm/balancer/priv/1st.beta.mobsearch.yandex.ru.key";
                };
                {
                  priority = 2;
                  keyfile = "/dev/shm/balancer/priv/2nd.beta.mobsearch.yandex.ru.key";
                };
                {
                  priority = 1;
                  keyfile = "/dev/shm/balancer/priv/3rd.beta.mobsearch.yandex.ru.key";
                };
              }; -- ticket_keys_list
            }; -- default
          }; -- contexts
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
                refers = "https";
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- ssl_sni
      }; -- errorlog
    }; -- https_2
    https_3 = {
      ips = {
        "127.0.0.1";
      }; -- ips
      ports = {
        4443;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 4443, "/place/db/www/logs/");
        ssl_sni = {
          force_ssl = false;
          events = {
            stats = "report";
            reload_ocsp_response = "reload_ocsp";
            reload_ticket_keys = "reload_ticket";
          }; -- events
          http2_alpn_file = "http2_enable.ratefile";
          http2_alpn_freq = 0.500;
          contexts = {
            default = {
              priority = 1;
              timeout = "100800s";
              disable_sslv3 = true;
              ciphers = get_str_var("default_ciphers");
              ocsp_file_switch = "./controls/disable_ocsp";
              ocsp = "./ocsp/allCAs-beta.mobsearch.yandex.ru.der";
              secrets_log = get_log_path("secrets_log", 4443, "./");
              log = get_log_path("ssl_sni", 4443, "/place/db/www/logs/");
              priv = get_private_cert_path("beta.mobsearch.yandex.ru.pem", "./env/");
              cert = get_public_cert_path("allCAs-beta.mobsearch.yandex.ru.pem", "./env/");
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = "/dev/shm/balancer/priv/1st.beta.mobsearch.yandex.ru.key";
                };
                {
                  priority = 2;
                  keyfile = "/dev/shm/balancer/priv/2nd.beta.mobsearch.yandex.ru.key";
                };
                {
                  priority = 1;
                  keyfile = "/dev/shm/balancer/priv/3rd.beta.mobsearch.yandex.ru.key";
                };
              }; -- ticket_keys_list
            }; -- default
          }; -- contexts
          http2 = {
            goaway_debug_data_enabled = false;
            debug_log_enabled = false;
            allow_http2_without_ssl = true;
            allow_sending_trailers = false;
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
                log = get_log_path("access_log", 4443, "/place/db/www/logs/");
                errordocument = {
                  status = 200;
                  force_conn_close = false;
                }; -- errordocument
              }; -- accesslog
            }; -- http
          }; -- http2
        }; -- ssl_sni
      }; -- errorlog
    }; -- https_3
  }; -- ipdispatch
}