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
  buffer = 65536;
  maxconn = 1000;
  tcp_fastopen = 0;
  workers = get_workers();
  enable_reuse_port = true;
  private_address = "127.0.0.10";
  default_tcp_rst_on_error = true;
  events = {
    stats = "report";
  }; -- events
  dns_ttl = get_random_timedelta(600, 900, "s");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", 15630, "/place/db/www/logs/");
  admin_addrs = {
    {
      port = 15630;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 15630;
    };
  }; -- admin_addrs
  addrs = {
    {
      port = 80;
      ip = "2a02:6b8:0:3400:0:780:0:1";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 15630;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 15630;
      ip = get_ip_by_iproute("v6");
    };
  }; -- addrs
  ipdispatch = {
    admin = {
      ips = {
        "127.0.0.1";
        "::1";
      }; -- ips
      ports = {
        15630;
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
    http_section_80 = {
      ips = {
        "2a02:6b8:0:3400:0:780:0:1";
      }; -- ips
      ports = {
        80;
      }; -- ports
      shared = {
        uuid = "949665606653003194";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 15630, "/place/db/www/logs/");
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            accesslog = {
              log = get_log_path("access_log", 15630, "/place/db/www/logs/");
              report = {
                uuid = "service_total";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                headers = {
                  create_func_weak = {
                    ["X-Req-Id"] = "reqid";
                  }; -- create_func_weak
                  regexp = {
                    ["awacs-balancer-health-check"] = {
                      priority = 11;
                      match_fsm = {
                        URI = "/awacs-balancer-health-check";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                      errordocument = {
                        status = 200;
                        force_conn_close = false;
                      }; -- errordocument
                    }; -- ["awacs-balancer-health-check"]
                    slbping = {
                      priority = 10;
                      match_fsm = {
                        url = "/health";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                      stats_eater = {
                        balancer2 = {
                          unique_policy = {};
                          attempts = 1;
                          rr = {
                            weights_file = "./controls/slb_check.weights";
                            to_upstream = {
                              weight = 1.000;
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
                                    { "man1-3970-man-answers-nodejs-prod-0df-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c0b:3721:10c:e297:0:2c33"; };
                                    { "man1-6344-man-answers-nodejs-prod-0df-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c0b:b1a:10c:e297:0:2c33"; };
                                    { "man1-6911-man-answers-nodejs-prod-0df-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c0b:1029:10c:e297:0:2c33"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "500ms";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- to_upstream
                            switch_off = {
                              weight = -1.000;
                              errordocument = {
                                status = 503;
                                force_conn_close = false;
                              }; -- errordocument
                            }; -- switch_off
                          }; -- rr
                        }; -- balancer2
                      }; -- stats_eater
                    }; -- slbping
                    api_tags = {
                      priority = 9;
                      match_fsm = {
                        url = "/znatoki/api-tags.*";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                      threshold = {
                        lo_bytes = 500;
                        hi_bytes = 1024;
                        recv_timeout = "1s";
                        pass_timeout = "9s";
                        rewrite = {
                          actions = {
                            {
                              global = false;
                              literal = false;
                              case_insensitive = false;
                              regexp = "/znatoki/api-tags(.*)";
                              rewrite = "/suggest-answers-tags%1";
                            };
                          }; -- actions
                          balancer2 = {
                            by_name_policy = {
                              name = get_geo("bygeo_", "random");
                              unique_policy = {};
                            }; -- by_name_policy
                            attempts = 1;
                            rr = {
                              bygeo_man = {
                                weight = 1.000;
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
                                        { "man1-3955.search.yandex.net"; 26900; 300.000; "2a02:6b8:b000:6074:92e2:baff:fea1:7a44"; };
                                        { "man1-7436.search.yandex.net"; 26900; 300.000; "2a02:6b8:b000:6068:e61d:2dff:fe04:1910"; };
                                        { "man1-7440.search.yandex.net"; 26900; 300.000; "2a02:6b8:b000:6069:e61d:2dff:fe04:3200"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "40ms";
                                        backend_timeout = "200ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 0;
                                        need_resolve = true;
                                      }))
                                    }; -- weighted2
                                  }; -- balancer2
                                }; -- stats_eater
                              }; -- bygeo_man
                              bygeo_sas = {
                                weight = 1.000;
                                shared = {
                                  uuid = "2529656300764442936";
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
                                          { "sas1-4314.search.yandex.net"; 26900; 300.000; "2a02:6b8:b000:638:96de:80ff:fe8c:b838"; };
                                          { "sas1-5957.search.yandex.net"; 26900; 300.000; "2a02:6b8:b000:604:225:90ff:feec:2c2a"; };
                                          { "sas1-6011.search.yandex.net"; 26900; 300.000; "2a02:6b8:b000:672:feaa:14ff:fe1d:faa8"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "40ms";
                                          backend_timeout = "200ms";
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
                              }; -- bygeo_sas
                              bygeo_vla = {
                                weight = 1.000;
                                shared = {
                                  uuid = "2529656300764442936";
                                }; -- shared
                              }; -- bygeo_vla
                            }; -- rr
                          }; -- balancer2
                        }; -- rewrite
                      }; -- threshold
                    }; -- api_tags
                    toloka_form = {
                      priority = 8;
                      match_fsm = {
                        url = "/collections/toloka_form/api/(.*)?";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                      threshold = {
                        lo_bytes = 262144;
                        hi_bytes = 419430;
                        recv_timeout = "2s";
                        pass_timeout = "10s";
                        regexp = {
                          post_method = {
                            priority = 2;
                            match_fsm = {
                              match = "POST.*";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control.weights";
                                answers_man = {
                                  weight = 1.000;
                                  shared = {
                                    uuid = "7583417082730865584";
                                    report = {
                                      uuid = "requests_api_to_man";
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 1;
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
                                            { "man1-1174-man-answers-backend-pro-310-16590.gencfg-c.yandex.net"; 16590; 160.000; "2a02:6b8:c0b:1d85:10c:d04c:0:40ce"; };
                                            { "man1-7814-man-answers-backend-pro-310-16590.gencfg-c.yandex.net"; 16590; 160.000; "2a02:6b8:c0b:1a80:10c:d04c:0:40ce"; };
                                            { "man1-8005-man-answers-backend-pro-310-16590.gencfg-c.yandex.net"; 16590; 160.000; "2a02:6b8:c0b:18a5:10c:d04c:0:40ce"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- report
                                  }; -- shared
                                }; -- answers_man
                                answers_vla = {
                                  weight = 1.000;
                                  shared = {
                                    uuid = "7847648202549820217";
                                    report = {
                                      uuid = "requests_api_to_vla";
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 1;
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
                                            { "vla1-0578-vla-answers-backend-pro-116-25830.gencfg-c.yandex.net"; 25830; 160.000; "2a02:6b8:c0d:b92:10c:d04f:0:64e6"; };
                                            { "vla1-2384-vla-answers-backend-pro-116-25830.gencfg-c.yandex.net"; 25830; 160.000; "2a02:6b8:c0d:4515:10c:d04f:0:64e6"; };
                                            { "vla2-1015-vla-answers-backend--116-25830.gencfg-c.yandex.net"; 25830; 160.000; "2a02:6b8:c15:1ba1:10c:d04f:0:64e6"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- report
                                  }; -- shared
                                }; -- answers_vla
                                answers_sas = {
                                  weight = 1.000;
                                  shared = {
                                    uuid = "7748359804134938302";
                                    report = {
                                      uuid = "requests_api_to_sas";
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 1;
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
                                            { "sas1-0676-sas-answers-backend-pro-d04-15910.gencfg-c.yandex.net"; 15910; 160.000; "2a02:6b8:c08:6ea1:10c:d04d:0:3e26"; };
                                            { "sas1-1323-sas-answers-backend-pro-d04-15910.gencfg-c.yandex.net"; 15910; 160.000; "2a02:6b8:c08:372f:10c:d04d:0:3e26"; };
                                            { "sas1-5918-sas-answers-backend-pro-d04-15910.gencfg-c.yandex.net"; 15910; 160.000; "2a02:6b8:c08:80e:10c:d04d:0:3e26"; };
                                            { "sas1-6512-sas-answers-backend-pro-d04-15910.gencfg-c.yandex.net"; 15910; 160.000; "2a02:6b8:c08:649a:10c:d04d:0:3e26"; };
                                            { "sas1-8015-sas-answers-backend-pro-d04-15910.gencfg-c.yandex.net"; 15910; 160.000; "2a02:6b8:c08:5529:10c:d04d:0:3e26"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- report
                                  }; -- shared
                                }; -- answers_sas
                              }; -- rr
                            }; -- balancer2
                          }; -- post_method
                          default = {
                            priority = 1;
                            balancer2 = {
                              unique_policy = {};
                              attempts = 2;
                              rr = {
                                weights_file = "./controls/traffic_control.weights";
                                answers_man = {
                                  weight = 1.000;
                                  shared = {
                                    uuid = "1090015758557444598";
                                    report = {
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 3;
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
                                            { "man1-1174-man-answers-backend-pro-310-16590.gencfg-c.yandex.net"; 16590; 160.000; "2a02:6b8:c0b:1d85:10c:d04c:0:40ce"; };
                                            { "man1-7814-man-answers-backend-pro-310-16590.gencfg-c.yandex.net"; 16590; 160.000; "2a02:6b8:c0b:1a80:10c:d04c:0:40ce"; };
                                            { "man1-8005-man-answers-backend-pro-310-16590.gencfg-c.yandex.net"; 16590; 160.000; "2a02:6b8:c0b:18a5:10c:d04c:0:40ce"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                      refers = "requests_api_to_man";
                                    }; -- report
                                  }; -- shared
                                }; -- answers_man
                                answers_vla = {
                                  weight = 1.000;
                                  shared = {
                                    uuid = "1386975030217201315";
                                    report = {
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 3;
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
                                            { "vla1-0578-vla-answers-backend-pro-116-25830.gencfg-c.yandex.net"; 25830; 160.000; "2a02:6b8:c0d:b92:10c:d04f:0:64e6"; };
                                            { "vla1-2384-vla-answers-backend-pro-116-25830.gencfg-c.yandex.net"; 25830; 160.000; "2a02:6b8:c0d:4515:10c:d04f:0:64e6"; };
                                            { "vla2-1015-vla-answers-backend--116-25830.gencfg-c.yandex.net"; 25830; 160.000; "2a02:6b8:c15:1ba1:10c:d04f:0:64e6"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                      refers = "requests_api_to_vla";
                                    }; -- report
                                  }; -- shared
                                }; -- answers_vla
                                answers_sas = {
                                  weight = 1.000;
                                  shared = {
                                    uuid = "5344623655770193832";
                                    report = {
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 3;
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
                                            { "sas1-0676-sas-answers-backend-pro-d04-15910.gencfg-c.yandex.net"; 15910; 160.000; "2a02:6b8:c08:6ea1:10c:d04d:0:3e26"; };
                                            { "sas1-1323-sas-answers-backend-pro-d04-15910.gencfg-c.yandex.net"; 15910; 160.000; "2a02:6b8:c08:372f:10c:d04d:0:3e26"; };
                                            { "sas1-5918-sas-answers-backend-pro-d04-15910.gencfg-c.yandex.net"; 15910; 160.000; "2a02:6b8:c08:80e:10c:d04d:0:3e26"; };
                                            { "sas1-6512-sas-answers-backend-pro-d04-15910.gencfg-c.yandex.net"; 15910; 160.000; "2a02:6b8:c08:649a:10c:d04d:0:3e26"; };
                                            { "sas1-8015-sas-answers-backend-pro-d04-15910.gencfg-c.yandex.net"; 15910; 160.000; "2a02:6b8:c08:5529:10c:d04d:0:3e26"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                      refers = "requests_api_to_sas";
                                    }; -- report
                                  }; -- shared
                                }; -- answers_sas
                              }; -- rr
                            }; -- balancer2
                          }; -- default
                        }; -- regexp
                      }; -- threshold
                    }; -- toloka_form
                    https_api = {
                      priority = 7;
                      match_fsm = {
                        url = "(/collections/toloka_form)?(/znatoki)?/api/(.*)?";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                      threshold = {
                        lo_bytes = 262144;
                        hi_bytes = 419430;
                        recv_timeout = "2s";
                        pass_timeout = "10s";
                        regexp = {
                          post_method = {
                            priority = 2;
                            match_fsm = {
                              match = "POST.*";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control.weights";
                                answers_man = {
                                  weight = 1.000;
                                  shared = {
                                    uuid = "7583417082730865584";
                                  }; -- shared
                                }; -- answers_man
                                answers_vla = {
                                  weight = 1.000;
                                  shared = {
                                    uuid = "7847648202549820217";
                                  }; -- shared
                                }; -- answers_vla
                                answers_sas = {
                                  weight = 1.000;
                                  shared = {
                                    uuid = "7748359804134938302";
                                  }; -- shared
                                }; -- answers_sas
                              }; -- rr
                              on_error = {
                                errordocument = {
                                  status = 504;
                                  force_conn_close = false;
                                  content = "Gateway Timeout";
                                }; -- errordocument
                              }; -- on_error
                            }; -- balancer2
                          }; -- post_method
                          default = {
                            priority = 1;
                            balancer2 = {
                              unique_policy = {};
                              attempts = 2;
                              rr = {
                                weights_file = "./controls/traffic_control.weights";
                                answers_man = {
                                  weight = 1.000;
                                  shared = {
                                    uuid = "1090015758557444598";
                                  }; -- shared
                                }; -- answers_man
                                answers_vla = {
                                  weight = 1.000;
                                  shared = {
                                    uuid = "1386975030217201315";
                                  }; -- shared
                                }; -- answers_vla
                                answers_sas = {
                                  weight = 1.000;
                                  shared = {
                                    uuid = "5344623655770193832";
                                  }; -- shared
                                }; -- answers_sas
                              }; -- rr
                              on_error = {
                                errordocument = {
                                  status = 504;
                                  force_conn_close = false;
                                  content = "Gateway Timeout";
                                }; -- errordocument
                              }; -- on_error
                            }; -- balancer2
                          }; -- default
                        }; -- regexp
                      }; -- threshold
                    }; -- https_api
                    sitemap = {
                      priority = 6;
                      match_fsm = {
                        URI = "(/znatoki)?/sitemap(/.*)?";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                      report = {
                        uuid = "sitemap";
                        ranges = get_str_var("default_ranges");
                        just_storage = false;
                        disable_robotness = true;
                        disable_sslness = true;
                        events = {
                          stats = "report";
                        }; -- events
                        regexp = {
                          post_method = {
                            priority = 2;
                            match_fsm = {
                              match = "POST.*";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control.weights";
                                sitemap_sas = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_sitemap_to_sas";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      active = {
                                        delay = "10s";
                                        request = "GET /health/ HTTP/1.1\nHost: znatoki.yandex.ru\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "sas1-0432.search.yandex.net"; 18149; 40.000; "2a02:6b8:b000:159:225:90ff:fe83:3a0"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "5s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 1;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                    }; -- balancer2
                                  }; -- report
                                }; -- sitemap_sas
                                sitemap_man = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_sitemap_to_man";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      active = {
                                        delay = "10s";
                                        request = "GET /health/ HTTP/1.1\nHost: znatoki.yandex.ru\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "man2-0389.search.yandex.net"; 14861; 40.000; "2a02:6b8:b000:6092:92e2:baff:fea2:305e"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "5s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 1;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                    }; -- balancer2
                                  }; -- report
                                }; -- sitemap_man
                                sitemap_vla = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_sitemap_to_vla";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      active = {
                                        delay = "10s";
                                        request = "GET /health/ HTTP/1.1\nHost: znatoki.yandex.ru\n\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "vla1-6008.search.yandex.net"; 14630; 40.000; "2a02:6b8:c0e:103:0:604:dbc:a4e2"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "5s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 1;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                    }; -- balancer2
                                  }; -- report
                                }; -- sitemap_vla
                                sitemap_devnull = {
                                  weight = -1.000;
                                  shared = {
                                    uuid = "6188594998980046344";
                                    report = {
                                      uuid = "requests_sitemap_to_devnull";
                                      ranges = "1ms";
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      errordocument = {
                                        status = 204;
                                        force_conn_close = false;
                                      }; -- errordocument
                                    }; -- report
                                  }; -- shared
                                }; -- sitemap_devnull
                              }; -- rr
                              on_error = {
                                errordocument = {
                                  status = 504;
                                  force_conn_close = false;
                                  content = "Gateway Timeout";
                                }; -- errordocument
                              }; -- on_error
                            }; -- balancer2
                          }; -- post_method
                          default = {
                            priority = 1;
                            balancer2 = {
                              unique_policy = {};
                              attempts = 3;
                              attempts_file = "./controls/sitemap.attempts";
                              rr = {
                                weights_file = "./controls/traffic_control.weights";
                                sitemap_sas = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
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
                                          { "sas1-0432.search.yandex.net"; 18149; 40.000; "2a02:6b8:b000:159:225:90ff:fe83:3a0"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "5s";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 1;
                                          need_resolve = true;
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                    refers = "requests_sitemap_to_sas";
                                  }; -- report
                                }; -- sitemap_sas
                                sitemap_man = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
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
                                          { "man2-0389.search.yandex.net"; 14861; 40.000; "2a02:6b8:b000:6092:92e2:baff:fea2:305e"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "5s";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 1;
                                          need_resolve = true;
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                    refers = "requests_sitemap_to_man";
                                  }; -- report
                                }; -- sitemap_man
                                sitemap_vla = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
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
                                          { "vla1-6008.search.yandex.net"; 14630; 40.000; "2a02:6b8:c0e:103:0:604:dbc:a4e2"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "5s";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 1;
                                          need_resolve = true;
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                    refers = "requests_sitemap_to_vla";
                                  }; -- report
                                }; -- sitemap_vla
                                sitemap_devnull = {
                                  weight = -1.000;
                                  shared = {
                                    uuid = "6188594998980046344";
                                  }; -- shared
                                }; -- sitemap_devnull
                              }; -- rr
                              on_error = {
                                errordocument = {
                                  status = 504;
                                  force_conn_close = false;
                                  content = "Gateway Timeout";
                                }; -- errordocument
                              }; -- on_error
                            }; -- balancer2
                          }; -- default
                        }; -- regexp
                      }; -- report
                    }; -- sitemap
                    toloka_form_default = {
                      priority = 5;
                      match_fsm = {
                        URI = "/collections/toloka_form/(.*)?";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                      threshold = {
                        lo_bytes = 262144;
                        hi_bytes = 419430;
                        recv_timeout = "2s";
                        pass_timeout = "10s";
                        balancer2 = {
                          unique_policy = {};
                          attempts = 2;
                          rr = {
                            weights_file = "./controls/traffic_control.weights";
                            answers_man = {
                              weight = 1.000;
                              report = {
                                uuid = "requests_nodejs_to_man";
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 3;
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
                                      { "man1-3970-man-answers-nodejs-prod-0df-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c0b:3721:10c:e297:0:2c33"; };
                                      { "man1-6344-man-answers-nodejs-prod-0df-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c0b:b1a:10c:e297:0:2c33"; };
                                      { "man1-6911-man-answers-nodejs-prod-0df-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c0b:1029:10c:e297:0:2c33"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "100ms";
                                      backend_timeout = "5s";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                }; -- balancer2
                              }; -- report
                            }; -- answers_man
                            answers_sas = {
                              weight = 1.000;
                              report = {
                                uuid = "requests_nodejs_to_sas";
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 3;
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
                                      { "sas1-3282-sas-answers-nodejs-prod-074-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c08:2a2b:10c:e299:0:2c33"; };
                                      { "sas1-9096-sas-answers-nodejs-p-074-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c08:df24:10c:e299:0:2c33"; };
                                      { "sas2-5935-sas-answers-nodejs-p-074-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c14:1d23:10c:e299:0:2c33"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "100ms";
                                      backend_timeout = "5s";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                }; -- balancer2
                              }; -- report
                            }; -- answers_sas
                            answers_vla = {
                              weight = 1.000;
                              report = {
                                uuid = "requests_nodejs_to_vla";
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 3;
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
                                      { "vla1-0150-vla-answers-nodejs-prod-11e-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c0d:1f9f:10c:e29a:0:2c33"; };
                                      { "vla1-1122-vla-answers-nodejs-prod-11e-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c0d:3c13:10c:e29a:0:2c33"; };
                                      { "vla1-3476-vla-answers-nodejs-prod-11e-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c0d:5000:10c:e29a:0:2c33"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "100ms";
                                      backend_timeout = "5s";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                }; -- balancer2
                              }; -- report
                            }; -- answers_vla
                          }; -- rr
                        }; -- balancer2
                      }; -- threshold
                    }; -- toloka_form_default
                    stats = {
                      priority = 4;
                      match_fsm = {
                        url = "(/znatoki)?/stats.*";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                      threshold = {
                        lo_bytes = 262144;
                        hi_bytes = 419430;
                        recv_timeout = "2s";
                        pass_timeout = "10s";
                        balancer2 = {
                          unique_policy = {};
                          attempts = 1;
                          rr = {
                            weights_file = "./controls/traffic_control.weights";
                            answers_man = {
                              weight = 1.000;
                              balancer2 = {
                                unique_policy = {};
                                attempts = 1;
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
                                    { "man1-1174-man-answers-backend-pro-310-16590.gencfg-c.yandex.net"; 16590; 160.000; "2a02:6b8:c0b:1d85:10c:d04c:0:40ce"; };
                                    { "man1-7814-man-answers-backend-pro-310-16590.gencfg-c.yandex.net"; 16590; 160.000; "2a02:6b8:c0b:1a80:10c:d04c:0:40ce"; };
                                    { "man1-8005-man-answers-backend-pro-310-16590.gencfg-c.yandex.net"; 16590; 160.000; "2a02:6b8:c0b:18a5:10c:d04c:0:40ce"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "600s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- answers_man
                            answers_vla = {
                              weight = 1.000;
                              balancer2 = {
                                unique_policy = {};
                                attempts = 1;
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
                                    { "vla1-0578-vla-answers-backend-pro-116-25830.gencfg-c.yandex.net"; 25830; 160.000; "2a02:6b8:c0d:b92:10c:d04f:0:64e6"; };
                                    { "vla1-2384-vla-answers-backend-pro-116-25830.gencfg-c.yandex.net"; 25830; 160.000; "2a02:6b8:c0d:4515:10c:d04f:0:64e6"; };
                                    { "vla2-1015-vla-answers-backend--116-25830.gencfg-c.yandex.net"; 25830; 160.000; "2a02:6b8:c15:1ba1:10c:d04f:0:64e6"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "600s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- answers_vla
                            answers_sas = {
                              weight = 1.000;
                              balancer2 = {
                                unique_policy = {};
                                attempts = 1;
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
                                    { "sas1-0676-sas-answers-backend-pro-d04-15910.gencfg-c.yandex.net"; 15910; 160.000; "2a02:6b8:c08:6ea1:10c:d04d:0:3e26"; };
                                    { "sas1-1323-sas-answers-backend-pro-d04-15910.gencfg-c.yandex.net"; 15910; 160.000; "2a02:6b8:c08:372f:10c:d04d:0:3e26"; };
                                    { "sas1-5918-sas-answers-backend-pro-d04-15910.gencfg-c.yandex.net"; 15910; 160.000; "2a02:6b8:c08:80e:10c:d04d:0:3e26"; };
                                    { "sas1-6512-sas-answers-backend-pro-d04-15910.gencfg-c.yandex.net"; 15910; 160.000; "2a02:6b8:c08:649a:10c:d04d:0:3e26"; };
                                    { "sas1-8015-sas-answers-backend-pro-d04-15910.gencfg-c.yandex.net"; 15910; 160.000; "2a02:6b8:c08:5529:10c:d04d:0:3e26"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "600s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- answers_sas
                          }; -- rr
                        }; -- balancer2
                      }; -- threshold
                    }; -- stats
                    feed_reader = {
                      priority = 3;
                      match_fsm = {
                        url = "(/znatoki)?/feed.*";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                      threshold = {
                        lo_bytes = 262144;
                        hi_bytes = 419430;
                        recv_timeout = "2s";
                        pass_timeout = "10s";
                        balancer2 = {
                          unique_policy = {};
                          attempts = 1;
                          weighted2 = {
                            weights_file = "./controls/traffic_control.weights";
                            slow_reply_time = "1s";
                            correction_params = {
                              max_weight = 5.000;
                              min_weight = 0.050;
                              history_time = "100s";
                              feedback_time = "300s";
                              plus_diff_per_sec = 0.050;
                              minus_diff_per_sec = 0.100;
                            }; -- correction_params
                            feed_man = {
                              weight = 1.000;
                              report = {
                                uuid = "balancer_requests_feedreader_to_man";
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
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
                                      { "man2-0392-man-answers-feed-reader-bf8-21135.gencfg-c.yandex.net"; 21135; 40.000; "2a02:6b8:c0b:4d02:10d:6:0:528f"; };
                                      { "man2-0444-man-answers-feed-reader-bf8-21135.gencfg-c.yandex.net"; 21135; 40.000; "2a02:6b8:c0b:440a:10d:6:0:528f"; };
                                      { "man2-0445-man-answers-feed-reader-bf8-21135.gencfg-c.yandex.net"; 21135; 40.000; "2a02:6b8:c0b:4c43:10d:6:0:528f"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "100ms";
                                      backend_timeout = "500ms";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                }; -- balancer2
                              }; -- report
                            }; -- feed_man
                            feed_vla = {
                              weight = 1.000;
                              report = {
                                uuid = "balancer_requests_feedreader_to_vla";
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
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
                                      { "vla1-0101-vla-answers-feed-reader-a1e-21175.gencfg-c.yandex.net"; 21175; 41.000; "2a02:6b8:c0d:169d:10d:a:0:52b7"; };
                                      { "vla1-1416-vla-answers-feed-reader-a1e-21175.gencfg-c.yandex.net"; 21175; 41.000; "2a02:6b8:c0d:219a:10d:a:0:52b7"; };
                                      { "vla1-1835-vla-answers-feed-reader-a1e-21175.gencfg-c.yandex.net"; 21175; 41.000; "2a02:6b8:c0d:2b82:10d:a:0:52b7"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "100ms";
                                      backend_timeout = "500ms";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                }; -- balancer2
                              }; -- report
                            }; -- feed_vla
                            feed_sas = {
                              weight = 1.000;
                              report = {
                                uuid = "balancer_requests_feedreader_to_sas";
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
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
                                      { "sas1-6001-sas-answers-feed-reader-562-21165.gencfg-c.yandex.net"; 21165; 40.000; "2a02:6b8:c08:6a1e:10d:8:0:52ad"; };
                                      { "sas1-6157-sas-answers-feed-reader-562-21165.gencfg-c.yandex.net"; 21165; 40.000; "2a02:6b8:c08:730a:10d:8:0:52ad"; };
                                      { "sas1-9298-sas-answers-feed-reader-562-21165.gencfg-c.yandex.net"; 21165; 40.000; "2a02:6b8:c08:6f14:10d:8:0:52ad"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "100ms";
                                      backend_timeout = "500ms";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                }; -- balancer2
                              }; -- report
                            }; -- feed_sas
                          }; -- weighted2
                        }; -- balancer2
                      }; -- threshold
                    }; -- feed_reader
                    recommender = {
                      priority = 2;
                      match_fsm = {
                        url = "(/znatoki)?/recommender.*";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                      threshold = {
                        lo_bytes = 262144;
                        hi_bytes = 419430;
                        recv_timeout = "2s";
                        pass_timeout = "10s";
                        balancer2 = {
                          unique_policy = {};
                          attempts = 1;
                          rr = {
                            weights_file = "./controls/traffic_control.weights";
                            recommender_man = {
                              weight = 1.000;
                              report = {
                                uuid = "balancer_requests_recommender_to_man";
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
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
                                      { "man1-5636-man-answers-recommender-f35-17037.gencfg-c.yandex.net"; 17037; 160.000; "2a02:6b8:c0b:5fb:10d:6c6b:0:428d"; };
                                      { "man2-0402-man-answers-recommender-f35-17037.gencfg-c.yandex.net"; 17037; 160.000; "2a02:6b8:c0b:4427:10d:6c6b:0:428d"; };
                                      { "man2-0419-man-answers-recommender-f35-17037.gencfg-c.yandex.net"; 17037; 160.000; "2a02:6b8:c0b:4c10:10d:6c6b:0:428d"; };
                                      { "man2-0440-man-answers-recommender-f35-17037.gencfg-c.yandex.net"; 17037; 160.000; "2a02:6b8:c0b:411a:10d:6c6b:0:428d"; };
                                      { "man2-0489-man-answers-recommender-f35-17037.gencfg-c.yandex.net"; 17037; 160.000; "2a02:6b8:c0b:4f1f:10d:6c6b:0:428d"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "500ms";
                                      backend_timeout = "2000ms";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                }; -- balancer2
                              }; -- report
                            }; -- recommender_man
                            recommender_vla = {
                              weight = 1.000;
                              report = {
                                uuid = "balancer_requests_recommender_to_vla";
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
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
                                      { "vla1-3306-vla-answers-recommender-0a8-27252.gencfg-c.yandex.net"; 27252; 160.000; "2a02:6b8:c0d:399b:10d:6c68:0:6a74"; };
                                      { "vla2-1027-vla-answers-recommen-0a8-27252.gencfg-c.yandex.net"; 27252; 160.000; "2a02:6b8:c17:497:10d:6c68:0:6a74"; };
                                      { "vla2-5844-624-vla-answers-reco-0a8-27252.gencfg-c.yandex.net"; 27252; 160.000; "2a02:6b8:c15:3623:10d:6c68:0:6a74"; };
                                      { "vla2-5889-9af-vla-answers-reco-0a8-27252.gencfg-c.yandex.net"; 27252; 160.000; "2a02:6b8:c15:361b:10d:6c68:0:6a74"; };
                                      { "vla2-7979-33b-vla-answers-reco-0a8-27252.gencfg-c.yandex.net"; 27252; 160.000; "2a02:6b8:c15:3989:10d:6c68:0:6a74"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "500ms";
                                      backend_timeout = "2000ms";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                }; -- balancer2
                              }; -- report
                            }; -- recommender_vla
                            recommender_sas = {
                              weight = 1.000;
                              report = {
                                uuid = "balancer_requests_recommender_to_sas";
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
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
                                      { "sas1-0537-sas-answers-recommender-d0b-22410.gencfg-c.yandex.net"; 22410; 160.000; "2a02:6b8:c08:1a0c:10d:6c69:0:578a"; };
                                      { "sas1-3848-sas-answers-recommender-d0b-22410.gencfg-c.yandex.net"; 22410; 160.000; "2a02:6b8:c08:2a1b:10d:6c69:0:578a"; };
                                      { "sas1-4167-sas-answers-recommender-d0b-22410.gencfg-c.yandex.net"; 22410; 160.000; "2a02:6b8:c08:230e:10d:6c69:0:578a"; };
                                      { "sas1-4264-sas-answers-recommender-d0b-22410.gencfg-c.yandex.net"; 22410; 160.000; "2a02:6b8:c08:300a:10d:6c69:0:578a"; };
                                      { "sas1-5579-sas-answers-recommender-d0b-22410.gencfg-c.yandex.net"; 22410; 160.000; "2a02:6b8:c08:3c1d:10d:6c69:0:578a"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "500ms";
                                      backend_timeout = "2000ms";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                }; -- balancer2
                              }; -- report
                            }; -- recommender_sas
                          }; -- rr
                        }; -- balancer2
                      }; -- threshold
                    }; -- recommender
                    default = {
                      priority = 1;
                      threshold = {
                        lo_bytes = 262144;
                        hi_bytes = 419430;
                        recv_timeout = "2s";
                        pass_timeout = "10s";
                        shared = {
                          uuid = "backends";
                          regexp = {
                            default = {
                              priority = 1;
                              balancer2 = {
                                unique_policy = {};
                                attempts = 2;
                                rr = {
                                  weights_file = "./controls/traffic_control.weights";
                                  answers_man = {
                                    weight = 1.000;
                                    report = {
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 1;
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
                                            { "man1-3970-man-answers-nodejs-prod-0df-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c0b:3721:10c:e297:0:2c33"; };
                                            { "man1-6344-man-answers-nodejs-prod-0df-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c0b:b1a:10c:e297:0:2c33"; };
                                            { "man1-6911-man-answers-nodejs-prod-0df-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c0b:1029:10c:e297:0:2c33"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "2s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                      refers = "requests_nodejs_to_man";
                                    }; -- report
                                  }; -- answers_man
                                  answers_vla = {
                                    weight = 1.000;
                                    report = {
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 1;
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
                                            { "vla1-0150-vla-answers-nodejs-prod-11e-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c0d:1f9f:10c:e29a:0:2c33"; };
                                            { "vla1-1122-vla-answers-nodejs-prod-11e-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c0d:3c13:10c:e29a:0:2c33"; };
                                            { "vla1-3476-vla-answers-nodejs-prod-11e-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c0d:5000:10c:e29a:0:2c33"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "2s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                      refers = "requests_nodejs_to_vla";
                                    }; -- report
                                  }; -- answers_vla
                                  answers_sas = {
                                    weight = 1.000;
                                    report = {
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 1;
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
                                            { "sas1-3282-sas-answers-nodejs-prod-074-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c08:2a2b:10c:e299:0:2c33"; };
                                            { "sas1-9096-sas-answers-nodejs-p-074-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c08:df24:10c:e299:0:2c33"; };
                                            { "sas2-5935-sas-answers-nodejs-p-074-11315.gencfg-c.yandex.net"; 11315; 160.000; "2a02:6b8:c14:1d23:10c:e299:0:2c33"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "2s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                      refers = "requests_nodejs_to_sas";
                                    }; -- report
                                  }; -- answers_sas
                                }; -- rr
                                on_error = {
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 3;
                                    rr = {
                                      weights_file = "./controls/traffic_control.weights";
                                      answers_man = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_nodejs_offline_to_man";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
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
                                                { "man1-5537-man-answers-nodejs-offl-7e8-14441.gencfg-c.yandex.net"; 14441; 40.000; "2a02:6b8:c0b:36d:10d:37f2:0:3869"; };
                                                { "man1-6670-man-answers-nodejs-offl-7e8-14441.gencfg-c.yandex.net"; 14441; 40.000; "2a02:6b8:c0b:172:10d:37f2:0:3869"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "500ms";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- weighted2
                                            on_error = {
                                              errordocument = {
                                                status = 504;
                                                force_conn_close = false;
                                                content = "Service unavailable!";
                                              }; -- errordocument
                                            }; -- on_error
                                          }; -- balancer2
                                        }; -- report
                                      }; -- answers_man
                                      answers_vla = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_nodejs_offline_to_vla";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
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
                                                { "vla1-1690-vla-answers-nodejs-offl-a0f-15875.gencfg-c.yandex.net"; 15875; 40.000; "2a02:6b8:c0d:2c93:10d:37f0:0:3e03"; };
                                                { "vla1-3766-vla-answers-nodejs-offl-a0f-15875.gencfg-c.yandex.net"; 15875; 40.000; "2a02:6b8:c0d:2d04:10d:37f0:0:3e03"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "500ms";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- weighted2
                                            on_error = {
                                              errordocument = {
                                                status = 504;
                                                force_conn_close = false;
                                                content = "Service unavailable!";
                                              }; -- errordocument
                                            }; -- on_error
                                          }; -- balancer2
                                        }; -- report
                                      }; -- answers_vla
                                      answers_sas = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_nodejs_offline_to_sas";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
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
                                                { "sas1-3002-sas-answers-nodejs-offl-61d-14317.gencfg-c.yandex.net"; 14317; 40.000; "2a02:6b8:c08:2b97:10d:37f1:0:37ed"; };
                                                { "sas1-8154-sas-answers-nodejs-offl-61d-14317.gencfg-c.yandex.net"; 14317; 40.000; "2a02:6b8:c08:5d94:10d:37f1:0:37ed"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "500ms";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- weighted2
                                            on_error = {
                                              errordocument = {
                                                status = 504;
                                                force_conn_close = false;
                                                content = "Service unavailable!";
                                              }; -- errordocument
                                            }; -- on_error
                                          }; -- balancer2
                                        }; -- report
                                      }; -- answers_sas
                                    }; -- rr
                                  }; -- balancer2
                                }; -- on_error
                              }; -- balancer2
                            }; -- default
                          }; -- regexp
                        }; -- shared
                      }; -- threshold
                    }; -- default
                  }; -- regexp
                }; -- headers
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- errorlog
      }; -- shared
    }; -- http_section_80
    http_section_15630 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        15630;
      }; -- ports
      shared = {
        uuid = "949665606653003194";
      }; -- shared
    }; -- http_section_15630
  }; -- ipdispatch
}