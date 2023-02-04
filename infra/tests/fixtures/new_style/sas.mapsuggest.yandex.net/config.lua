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


function prefix_with_dc(name, default_dc, separator)
  dc = DC or default_dc or "unknown";
  separator = separator or "_";
  return dc .. separator .. name;
end


_call_func_providers({
  "get_workers";
})


instance = {
  buffer = 65536;
  maxconn = 5000;
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
  log = get_log_path("childs_log", 15870, "/place/db/www/logs");
  admin_addrs = {
    {
      port = 15870;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 15870;
    };
  }; -- admin_addrs
  addrs = {
    {
      port = 15870;
      ip = "127.0.0.4";
    };
    {
      port = 443;
      ip = "2a02:6b8::126";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "87.250.250.126";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 15871;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 15871;
      ip = get_ip_by_iproute("v6");
    };
    {
      port = 80;
      ip = "2a02:6b8::126";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "87.250.250.126";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 15870;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 15870;
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
        15870;
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
        15870;
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
            status = 204;
            force_conn_close = false;
          }; -- errordocument
        }; -- http
      }; -- report
    }; -- stats_storage
    https_section_443 = {
      ips = {
        "2a02:6b8::126";
        "87.250.250.126";
      }; -- ips
      ports = {
        443;
      }; -- ports
      shared = {
        uuid = "1307879810447188393";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 15871, "/place/db/www/logs");
          ssl_sni = {
            force_ssl = true;
            events = {
              stats = "report";
              reload_ocsp_response = "reload_ocsp";
              reload_ticket_keys = "reload_ticket";
            }; -- events
            contexts = {
              ["suggest-maps.yandex.az"] = {
                priority = 2;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 15871, "/place/db/www/logs");
                priv = get_private_cert_path("suggest-maps.yandex.az.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-suggest-maps.yandex.az.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "suggest-maps\\.yandex\\.(az|com.am|co.il|kg|lv|lt|md|tj|tm|fr|ee)";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.suggest-maps.yandex.az.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.suggest-maps.yandex.az.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.suggest-maps.yandex.az.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["suggest-maps.yandex.az"]
              default = {
                priority = 1;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 15871, "/place/db/www/logs");
                priv = get_private_cert_path("suggest-maps.yandex.ru.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-suggest-maps.yandex.ru.pem", "/dev/shm/balancer");
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.suggest-maps.yandex.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.suggest-maps.yandex.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.suggest-maps.yandex.ru.key", "/dev/shm/balancer/priv");
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
                log = get_log_path("access_log", 15871, "/place/db/www/logs");
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
                  headers = {
                    create_func = {
                      ["X-Forwarded-For-Y"] = "realip";
                      ["X-Req-Id"] = "reqid";
                      ["X-Source-Port-Y"] = "realport";
                      ["X-Start-Time"] = "starttime";
                    }; -- create_func
                    response_headers = {
                      create_weak = {
                        ["X-Content-Type-Options"] = "nosniff";
                        ["X-XSS-Protection"] = "1; mode=block";
                      }; -- create_weak
                      hasher = {
                        mode = "subnet";
                        subnet_v4_mask = 32;
                        subnet_v6_mask = 128;
                        shared = {
                          uuid = "upstreams";
                        }; -- shared
                      }; -- hasher
                    }; -- response_headers
                  }; -- headers
                }; -- report
              }; -- accesslog
            }; -- http
          }; -- ssl_sni
        }; -- errorlog
      }; -- shared
    }; -- https_section_443
    https_section_15871 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        15871;
      }; -- ports
      shared = {
        uuid = "1307879810447188393";
      }; -- shared
    }; -- https_section_15871
    http_section_80 = {
      ips = {
        "2a02:6b8::126";
        "87.250.250.126";
      }; -- ips
      ports = {
        80;
      }; -- ports
      shared = {
        uuid = "5944919745762798076";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 15870, "/place/db/www/logs");
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            accesslog = {
              log = get_log_path("access_log", 15870, "/place/db/www/logs");
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
                headers = {
                  create_func = {
                    ["X-Forwarded-For-Y"] = "realip";
                    ["X-Req-Id"] = "reqid";
                    ["X-Source-Port-Y"] = "realport";
                    ["X-Start-Time"] = "starttime";
                  }; -- create_func
                  response_headers = {
                    create_weak = {
                      ["X-Content-Type-Options"] = "nosniff";
                      ["X-XSS-Protection"] = "1; mode=block";
                    }; -- create_weak
                    hasher = {
                      mode = "subnet";
                      subnet_v4_mask = 32;
                      subnet_v6_mask = 128;
                      shared = {
                        uuid = "upstreams";
                        regexp = {
                          ["awacs-balancer-health-check"] = {
                            priority = 6;
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
                          experiment = {
                            priority = 5;
                            match_fsm = {
                              cgi = "exprt=5";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                            report = {
                              uuid = "exprt5";
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
                                rr = {
                                  weights_file = "./controls/traffic_control.weights";
                                  [prefix_with_dc("man")] = {
                                    weight = 1.000;
                                    balancer2 = {
                                      active_policy = {
                                        skip_attempts = 3;
                                        unique_policy = {};
                                      }; -- active_policy
                                      attempts = 4;
                                      hashing = {
                                        delay = "5s";
                                        request = "GET /ping HTTP/1.1\nHost: mapsuggest.yandex.net\n\n";
                                        unpack(gen_proxy_backends({
                                          { "man1-2586.search.yandex.net"; 11800; 100.000; "2a02:6b8:b000:6022:f652:14ff:fe8b:c7b0"; };
                                          { "man1-2923.search.yandex.net"; 11800; 100.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:d900"; };
                                          { "man1-3923.search.yandex.net"; 11800; 100.000; "2a02:6b8:b000:6074:92e2:baff:fea1:78d2"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "40ms";
                                          backend_timeout = "1s";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 10;
                                          need_resolve = true;
                                        }))
                                      }; -- hashing
                                    }; -- balancer2
                                  }; -- [prefix_with_dc("man")]
                                  [prefix_with_dc("sas")] = {
                                    weight = 1.000;
                                    balancer2 = {
                                      active_policy = {
                                        skip_attempts = 3;
                                        unique_policy = {};
                                      }; -- active_policy
                                      attempts = 4;
                                      hashing = {
                                        delay = "5s";
                                        request = "GET /ping HTTP/1.1\nHost: mapsuggest.yandex.net\n\n";
                                        unpack(gen_proxy_backends({
                                          { "sas1-5945.search.yandex.net"; 13200; 100.000; "2a02:6b8:b000:174:225:90ff:feec:2f4e"; };
                                          { "sas1-5948.search.yandex.net"; 13200; 100.000; "2a02:6b8:b000:635:225:90ff:feeb:fb74"; };
                                          { "sas2-0759.search.yandex.net"; 13200; 100.000; "2a02:6b8:c02:409:0:604:dde:f586"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "40ms";
                                          backend_timeout = "1s";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 10;
                                          need_resolve = true;
                                        }))
                                      }; -- hashing
                                    }; -- balancer2
                                  }; -- [prefix_with_dc("sas")]
                                  [prefix_with_dc("vla")] = {
                                    weight = 1.000;
                                    balancer2 = {
                                      active_policy = {
                                        skip_attempts = 3;
                                        unique_policy = {};
                                      }; -- active_policy
                                      attempts = 4;
                                      hashing = {
                                        delay = "5s";
                                        request = "GET /ping HTTP/1.1\nHost: mapsuggest.yandex.net\n\n";
                                        unpack(gen_proxy_backends({
                                          { "vla1-0133.search.yandex.net"; 13200; 100.000; "2a02:6b8:c0e:3f:0:604:db7:a781"; };
                                          { "vla1-0848.search.yandex.net"; 13200; 100.000; "2a02:6b8:c0e:48:0:604:db7:a208"; };
                                          { "vla1-4438.search.yandex.net"; 13200; 100.000; "2a02:6b8:c0e:a0:0:604:db7:a683"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "40ms";
                                          backend_timeout = "1s";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 10;
                                          need_resolve = true;
                                        }))
                                      }; -- hashing
                                    }; -- balancer2
                                  }; -- [prefix_with_dc("vla")]
                                }; -- rr
                                on_error = {
                                  errordocument = {
                                    status = 504;
                                    force_conn_close = false;
                                  }; -- errordocument
                                }; -- on_error
                              }; -- balancer2
                            }; -- report
                          }; -- experiment
                          ["saas-prestable"] = {
                            priority = 4;
                            match_fsm = {
                              cgi = "source=saas-prestable";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                            report = {
                              uuid = "saas-prestable";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
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
                                    { "saas-searchproxy-maps-prestable.yandex.net"; 80; 1.000; "2a02:6b8:0:3400::1097"; };
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
                            }; -- report
                          }; -- ["saas-prestable"]
                          ["get-history"] = {
                            priority = 3;
                            match_fsm = {
                              URI = "/suggest-get-history.*";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "get_history";
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
                                  attempts = 1;
                                  rr = {
                                    weights_file = "./controls/traffic_control.weights";
                                    [prefix_with_dc("man")] = {
                                      weight = 1.000;
                                      balancer2 = {
                                        active_policy = {
                                          skip_attempts = 12;
                                          unique_policy = {};
                                        }; -- active_policy
                                        attempts = 4;
                                        hashing = {
                                          delay = "5s";
                                          request = "GET /ping HTTP/1.1\nHost: mapsuggest.yandex.net\n\n";
                                          unpack(gen_proxy_backends({
                                            { "man1-1625.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:6018:f652:14ff:fe8b:b8b0"; };
                                            { "man1-1762.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b750"; };
                                            { "man1-1930.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:bdc0"; };
                                            { "man1-2020.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:1500"; };
                                            { "man1-2145.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:601e:f652:14ff:fe55:4540"; };
                                            { "man1-2226.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:601a:92e2:baff:fe56:e98c"; };
                                            { "man1-2405.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:6014:f652:14ff:fe8b:fee0"; };
                                            { "man1-5263.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:604e:e61d:2dff:fe01:f140"; };
                                            { "man1-6823.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:605d:e61d:2dff:fe04:2c50"; };
                                            { "man1-8383.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:6080:e61d:2dff:fe6d:a5f0"; };
                                            { "man1-8397.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:6082:e61d:2dff:fe6c:fa10"; };
                                            { "man1-8962.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:651e:e61d:2dff:fe6d:d510"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "40ms";
                                            backend_timeout = "200ms";
                                            fail_on_5xx = false;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- hashing
                                      }; -- balancer2
                                    }; -- [prefix_with_dc("man")]
                                    [prefix_with_dc("sas")] = {
                                      weight = 1.000;
                                      balancer2 = {
                                        active_policy = {
                                          skip_attempts = 29;
                                          unique_policy = {};
                                        }; -- active_policy
                                        attempts = 4;
                                        hashing = {
                                          delay = "5s";
                                          request = "GET /ping HTTP/1.1\nHost: mapsuggest.yandex.net\n\n";
                                          unpack(gen_proxy_backends({
                                            { "sas1-2912.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:10a:225:90ff:fe83:1dba"; };
                                            { "sas1-2982.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:10e:225:90ff:fe83:c72"; };
                                            { "sas1-6274.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:166:feaa:14ff:fe1d:f840"; };
                                            { "sas1-6818.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:11d:215:b2ff:fea7:6f0c"; };
                                            { "sas1-6967.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:123:215:b2ff:fea7:adbc"; };
                                            { "sas1-8530.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:135:215:b2ff:fea7:bb3c"; };
                                            { "sas1-9217.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:143:feaa:14ff:fede:43ac"; };
                                            { "sas1-9267.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:698:428d:5cff:fef4:8c3b"; };
                                            { "sas1-9272.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:69a:428d:5cff:fef4:793e"; };
                                            { "sas1-9298.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:698:428d:5cff:fef4:922b"; };
                                            { "sas1-9397.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:198:428d:5cff:fef4:8869"; };
                                            { "sas1-9561.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:198:428d:5cff:fef4:9431"; };
                                            { "sas1-9562.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:698:428d:5cff:fef4:9179"; };
                                            { "sas1-9573.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:199:215:b2ff:fea9:72f6"; };
                                            { "sas2-0199.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:414:0:604:df5:d8d5"; };
                                            { "sas2-0569.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:41d:0:604:df5:d891"; };
                                            { "sas2-0612.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:412:0:604:df5:d76f"; };
                                            { "sas2-0682.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:40b:0:604:dde:f5b8"; };
                                            { "sas2-0705.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:405:0:604:dde:f3f3"; };
                                            { "sas2-0727.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:40a:0:604:dde:f5ea"; };
                                            { "sas2-0735.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:407:0:604:dde:f604"; };
                                            { "sas2-0743.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:40c:0:604:dde:f71a"; };
                                            { "sas2-0745.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:40f:0:604:dde:f3ff"; };
                                            { "sas2-0746.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:408:0:604:dde:fba2"; };
                                            { "sas2-0759.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:409:0:604:dde:f586"; };
                                            { "sas2-0772.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:40d:0:604:dde:fc0b"; };
                                            { "sas2-1217.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:6ad:f652:14ff:fe7a:e5b0"; };
                                            { "sas2-6207.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:6fd:76d4:35ff:fec6:333a"; };
                                            { "sas2-6258.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:1f8:96de:80ff:fe8c:b886"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "40ms";
                                            backend_timeout = "200ms";
                                            fail_on_5xx = false;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- hashing
                                      }; -- balancer2
                                    }; -- [prefix_with_dc("sas")]
                                    [prefix_with_dc("vla")] = {
                                      weight = 1.000;
                                      balancer2 = {
                                        active_policy = {
                                          skip_attempts = 22;
                                          unique_policy = {};
                                        }; -- active_policy
                                        attempts = 4;
                                        hashing = {
                                          delay = "5s";
                                          request = "GET /ping HTTP/1.1\nHost: mapsuggest.yandex.net\n\n";
                                          unpack(gen_proxy_backends({
                                            { "vla1-0069.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:4f:0:604:5cf4:8e24"; };
                                            { "vla1-0091.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:2d:0:604:db7:9c22"; };
                                            { "vla1-0104.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:2d:0:604:db7:9c2f"; };
                                            { "vla1-0193.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:88:0:604:db7:a871"; };
                                            { "vla1-0383.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:9d:0:604:d8f:eb87"; };
                                            { "vla1-0487.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:34:0:604:db7:9ecf"; };
                                            { "vla1-0722.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:52:0:604:db7:a457"; };
                                            { "vla1-0771.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:6a:0:604:db7:a451"; };
                                            { "vla1-0924.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:71:0:604:db7:a408"; };
                                            { "vla1-0969.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:43:0:604:db7:a112"; };
                                            { "vla1-1064.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:8d:0:604:db7:ab3f"; };
                                            { "vla1-1375.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:88:0:604:db7:a9e4"; };
                                            { "vla1-1387.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:54:0:604:db7:a6bd"; };
                                            { "vla1-1495.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:71:0:604:db7:a3f8"; };
                                            { "vla1-1848.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:45:0:604:db7:a78f"; };
                                            { "vla1-2526.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:95:0:604:d8f:eb3e"; };
                                            { "vla1-2688.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:a2:0:604:db7:9f80"; };
                                            { "vla1-2870.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:2f:0:604:db7:9f7f"; };
                                            { "vla1-3010.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:a3:0:604:db7:a554"; };
                                            { "vla1-3616.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:a0:0:604:db7:a502"; };
                                            { "vla1-3813.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:a3:0:604:db7:a662"; };
                                            { "vla1-4275.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:90:0:604:db7:a99c"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "40ms";
                                            backend_timeout = "200ms";
                                            fail_on_5xx = false;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- hashing
                                      }; -- balancer2
                                    }; -- [prefix_with_dc("vla")]
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- stats_eater
                            }; -- report
                          }; -- ["get-history"]
                          personalization = {
                            priority = 2;
                            match_fsm = {
                              URI = "/suggest-personal.*";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "suggest_personal";
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
                                  attempts = 1;
                                  rr = {
                                    weights_file = "./controls/traffic_control.weights";
                                    [prefix_with_dc("man")] = {
                                      weight = 1.000;
                                      balancer2 = {
                                        active_policy = {
                                          skip_attempts = 3;
                                          unique_policy = {};
                                        }; -- active_policy
                                        attempts = 4;
                                        hashing = {
                                          delay = "5s";
                                          request = "GET /ping HTTP/1.1\nHost: mapsuggest.yandex.net\n\n";
                                          unpack(gen_proxy_backends({
                                            { "man1-0198.search.yandex.net"; 13490; 100.000; "2a02:6b8:b000:6030:92e2:baff:fe56:e95e"; };
                                            { "man1-0508.search.yandex.net"; 13490; 100.000; "2a02:6b8:b000:602b:92e2:baff:fe74:7c70"; };
                                            { "man1-5404.search.yandex.net"; 13490; 100.000; "2a02:6b8:b000:6050:e61d:2dff:fe03:49b0"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "40ms";
                                            backend_timeout = "1000ms";
                                            fail_on_5xx = false;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- hashing
                                      }; -- balancer2
                                    }; -- [prefix_with_dc("man")]
                                    [prefix_with_dc("sas")] = {
                                      weight = 1.000;
                                      balancer2 = {
                                        active_policy = {
                                          skip_attempts = 3;
                                          unique_policy = {};
                                        }; -- active_policy
                                        attempts = 4;
                                        hashing = {
                                          delay = "5s";
                                          request = "GET /ping HTTP/1.1\nHost: mapsuggest.yandex.net\n\n";
                                          unpack(gen_proxy_backends({
                                            { "sas1-2255.search.yandex.net"; 13490; 100.000; "2a02:6b8:b000:624:922b:34ff:fecf:319e"; };
                                            { "sas1-2679.search.yandex.net"; 13490; 100.000; "2a02:6b8:b000:607:225:90ff:fe83:19fe"; };
                                            { "sas1-4159.search.yandex.net"; 13490; 100.000; "2a02:6b8:b000:674:96de:80ff:fe81:1372"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "40ms";
                                            backend_timeout = "1000ms";
                                            fail_on_5xx = false;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- hashing
                                      }; -- balancer2
                                    }; -- [prefix_with_dc("sas")]
                                    [prefix_with_dc("vla")] = {
                                      weight = 1.000;
                                      balancer2 = {
                                        active_policy = {
                                          skip_attempts = 3;
                                          unique_policy = {};
                                        }; -- active_policy
                                        attempts = 4;
                                        hashing = {
                                          delay = "5s";
                                          request = "GET /ping HTTP/1.1\nHost: mapsuggest.yandex.net\n\n";
                                          unpack(gen_proxy_backends({
                                            { "vla1-0230.search.yandex.net"; 13490; 100.000; "2a02:6b8:c0e:9e:0:604:db7:a834"; };
                                            { "vla1-0826.search.yandex.net"; 13490; 100.000; "2a02:6b8:c0e:57:0:604:db7:a55e"; };
                                            { "vla1-2349.search.yandex.net"; 13490; 100.000; "2a02:6b8:c0e:86:0:604:db7:ab9b"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "40ms";
                                            backend_timeout = "1000ms";
                                            fail_on_5xx = false;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- hashing
                                      }; -- balancer2
                                    }; -- [prefix_with_dc("vla")]
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- stats_eater
                            }; -- report
                          }; -- personalization
                          default = {
                            priority = 1;
                            report = {
                              uuid = "default";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              stats_eater = {
                                pinger = {
                                  lo = 0.500;
                                  hi = 0.700;
                                  delay = "1s";
                                  histtime = "3s";
                                  ping_request_data = "GET /ping HTTP/1.1\nHost: mapsuggest.yandex.net\r\n\r\n";
                                  admin_request_uri = "/ping";
                                  admin_ips = "5.45.193.144/29,5.45.208.88/30,5.45.228.160/30,5.45.229.168/30,5.45.232.160/30,5.45.240.168/30,5.45.243.16/30,5.45.247.144/29,5.45.247.176/29,5.255.192.64/30,5.255.194.12/30,5.255.195.144/30,5.255.196.168/30,5.255.197.168/30,5.255.200.160/30,5.255.252.160/30,37.9.73.160/30,37.140.136.88/29,37.140.137.104/29,77.88.35.168/30,77.88.54.168/30,84.201.159.176/28,87.250.226.0/25,87.250.226.128/25,87.250.228.0/24,87.250.234.0/24,95.108.180.40/29,95.108.237.0/25,95.108.237.128/25,100.43.92.144/28,141.8.136.128/25,141.8.154.200/29,141.8.155.104/29,185.32.186.8/29,213.180.202.160/30,213.180.223.16/30,2001:678:384:100::/64,2620:10f:d000:100::/64,2a02:6b8:0:300::/64,2a02:6b8:0:400::/64,2a02:6b8:0:800::/64,2a02:6b8:0:900::/64,2a02:6b8:0:d00::/64,2a02:6b8:0:e00::/64,2a02:6b8:0:1000::/64,2a02:6b8:0:1100::/64,2a02:6b8:0:1200::/64,2a02:6b8:0:1300::/64,2a02:6b8:0:1400::/64,2a02:6b8:0:1500::/64,2a02:6b8:0:1600::/64,2a02:6b8:0:1700::/64,2a02:6b8:0:1800::/64,2a02:6b8:0:1900::/64,2a02:6b8:0:1a00::/64,2a02:6b8:0:1b00::/64,2a02:6b8:0:1d00::/64,2a02:6b8:0:1e00::/64,2a02:6b8:0:1f00::/64,2a02:6b8:0:2000::/64,2a02:6b8:0:2200::/64,2a02:6b8:0:2c00::/64,2a02:6b8:0:3000::/64,2a02:6b8:0:3100::/64,2a02:6b8:0:3401::/64,2a02:6b8:0:3c00::/64,2a02:6b8:0:3d00::/64,2a02:6b8:0:3e00::/64,2a02:6b8:0:3f00::/64,2a02:6b8:0:4000::/64,2a02:6b8:0:4200::/64,2a02:6b8:0:4700::/64,2a02:6b8:b010:b000::/64";
                                  enable_tcp_check_file = "./controls/tcp_check_on";
                                  switch_off_file = "./controls/slb_check.weights";
                                  switch_off_key = "switch_off";
                                  admin_error_replier = {
                                    errordocument = {
                                      status = 503;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- admin_error_replier
                                  module = {
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      rr = {
                                        weights_file = "./controls/traffic_control.weights";
                                        [prefix_with_dc("man")] = {
                                          weight = 1.000;
                                          balancer2 = {
                                            active_policy = {
                                              skip_attempts = 12;
                                              unique_policy = {};
                                            }; -- active_policy
                                            attempts = 2;
                                            connection_attempts = 2;
                                            hashing = {
                                              delay = "5s";
                                              request = "GET /ping HTTP/1.1\nHost: mapsuggest.yandex.net\n\n";
                                              unpack(gen_proxy_backends({
                                                { "man1-1625.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:6018:f652:14ff:fe8b:b8b0"; };
                                                { "man1-1762.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b750"; };
                                                { "man1-1930.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:bdc0"; };
                                                { "man1-2020.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:1500"; };
                                                { "man1-2145.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:601e:f652:14ff:fe55:4540"; };
                                                { "man1-2226.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:601a:92e2:baff:fe56:e98c"; };
                                                { "man1-2405.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:6014:f652:14ff:fe8b:fee0"; };
                                                { "man1-5263.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:604e:e61d:2dff:fe01:f140"; };
                                                { "man1-6823.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:605d:e61d:2dff:fe04:2c50"; };
                                                { "man1-8383.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:6080:e61d:2dff:fe6d:a5f0"; };
                                                { "man1-8397.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:6082:e61d:2dff:fe6c:fa10"; };
                                                { "man1-8962.search.yandex.net"; 9960; 644.000; "2a02:6b8:b000:651e:e61d:2dff:fe6d:d510"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "40ms";
                                                backend_timeout = "400ms";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- hashing
                                          }; -- balancer2
                                        }; -- [prefix_with_dc("man")]
                                        [prefix_with_dc("sas")] = {
                                          weight = 1.000;
                                          balancer2 = {
                                            active_policy = {
                                              skip_attempts = 29;
                                              unique_policy = {};
                                            }; -- active_policy
                                            attempts = 2;
                                            connection_attempts = 2;
                                            hashing = {
                                              delay = "5s";
                                              request = "GET /ping HTTP/1.1\nHost: mapsuggest.yandex.net\n\n";
                                              unpack(gen_proxy_backends({
                                                { "sas1-2912.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:10a:225:90ff:fe83:1dba"; };
                                                { "sas1-2982.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:10e:225:90ff:fe83:c72"; };
                                                { "sas1-6274.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:166:feaa:14ff:fe1d:f840"; };
                                                { "sas1-6818.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:11d:215:b2ff:fea7:6f0c"; };
                                                { "sas1-6967.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:123:215:b2ff:fea7:adbc"; };
                                                { "sas1-8530.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:135:215:b2ff:fea7:bb3c"; };
                                                { "sas1-9217.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:143:feaa:14ff:fede:43ac"; };
                                                { "sas1-9267.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:698:428d:5cff:fef4:8c3b"; };
                                                { "sas1-9272.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:69a:428d:5cff:fef4:793e"; };
                                                { "sas1-9298.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:698:428d:5cff:fef4:922b"; };
                                                { "sas1-9397.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:198:428d:5cff:fef4:8869"; };
                                                { "sas1-9561.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:198:428d:5cff:fef4:9431"; };
                                                { "sas1-9562.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:698:428d:5cff:fef4:9179"; };
                                                { "sas1-9573.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:199:215:b2ff:fea9:72f6"; };
                                                { "sas2-0199.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:414:0:604:df5:d8d5"; };
                                                { "sas2-0569.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:41d:0:604:df5:d891"; };
                                                { "sas2-0612.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:412:0:604:df5:d76f"; };
                                                { "sas2-0682.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:40b:0:604:dde:f5b8"; };
                                                { "sas2-0705.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:405:0:604:dde:f3f3"; };
                                                { "sas2-0727.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:40a:0:604:dde:f5ea"; };
                                                { "sas2-0735.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:407:0:604:dde:f604"; };
                                                { "sas2-0743.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:40c:0:604:dde:f71a"; };
                                                { "sas2-0745.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:40f:0:604:dde:f3ff"; };
                                                { "sas2-0746.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:408:0:604:dde:fba2"; };
                                                { "sas2-0759.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:409:0:604:dde:f586"; };
                                                { "sas2-0772.search.yandex.net"; 9960; 322.000; "2a02:6b8:c02:40d:0:604:dde:fc0b"; };
                                                { "sas2-1217.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:6ad:f652:14ff:fe7a:e5b0"; };
                                                { "sas2-6207.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:6fd:76d4:35ff:fec6:333a"; };
                                                { "sas2-6258.search.yandex.net"; 9960; 322.000; "2a02:6b8:b000:1f8:96de:80ff:fe8c:b886"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "40ms";
                                                backend_timeout = "400ms";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- hashing
                                          }; -- balancer2
                                        }; -- [prefix_with_dc("sas")]
                                        [prefix_with_dc("vla")] = {
                                          weight = 1.000;
                                          balancer2 = {
                                            active_policy = {
                                              skip_attempts = 22;
                                              unique_policy = {};
                                            }; -- active_policy
                                            attempts = 2;
                                            connection_attempts = 2;
                                            hashing = {
                                              delay = "5s";
                                              request = "GET /ping HTTP/1.1\nHost: mapsuggest.yandex.net\n\n";
                                              unpack(gen_proxy_backends({
                                                { "vla1-0069.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:4f:0:604:5cf4:8e24"; };
                                                { "vla1-0091.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:2d:0:604:db7:9c22"; };
                                                { "vla1-0104.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:2d:0:604:db7:9c2f"; };
                                                { "vla1-0193.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:88:0:604:db7:a871"; };
                                                { "vla1-0383.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:9d:0:604:d8f:eb87"; };
                                                { "vla1-0487.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:34:0:604:db7:9ecf"; };
                                                { "vla1-0722.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:52:0:604:db7:a457"; };
                                                { "vla1-0771.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:6a:0:604:db7:a451"; };
                                                { "vla1-0924.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:71:0:604:db7:a408"; };
                                                { "vla1-0969.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:43:0:604:db7:a112"; };
                                                { "vla1-1064.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:8d:0:604:db7:ab3f"; };
                                                { "vla1-1375.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:88:0:604:db7:a9e4"; };
                                                { "vla1-1387.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:54:0:604:db7:a6bd"; };
                                                { "vla1-1495.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:71:0:604:db7:a3f8"; };
                                                { "vla1-1848.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:45:0:604:db7:a78f"; };
                                                { "vla1-2526.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:95:0:604:d8f:eb3e"; };
                                                { "vla1-2688.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:a2:0:604:db7:9f80"; };
                                                { "vla1-2870.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:2f:0:604:db7:9f7f"; };
                                                { "vla1-3010.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:a3:0:604:db7:a554"; };
                                                { "vla1-3616.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:a0:0:604:db7:a502"; };
                                                { "vla1-3813.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:a3:0:604:db7:a662"; };
                                                { "vla1-4275.search.yandex.net"; 9960; 429.000; "2a02:6b8:c0e:90:0:604:db7:a99c"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "40ms";
                                                backend_timeout = "400ms";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- hashing
                                          }; -- balancer2
                                        }; -- [prefix_with_dc("vla")]
                                      }; -- rr
                                      on_error = {
                                        errordocument = {
                                          status = 504;
                                          force_conn_close = false;
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- module
                                }; -- pinger
                              }; -- stats_eater
                            }; -- report
                          }; -- default
                        }; -- regexp
                      }; -- shared
                    }; -- hasher
                  }; -- response_headers
                }; -- headers
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- errorlog
      }; -- shared
    }; -- http_section_80
    http_section_15870 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        15870;
      }; -- ports
      shared = {
        uuid = "5944919745762798076";
      }; -- shared
    }; -- http_section_15870
  }; -- ipdispatch
}