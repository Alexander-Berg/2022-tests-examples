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
  log = get_log_path("childs_log", 15600, "/place/db/www/logs/");
  admin_addrs = {
    {
      port = 15600;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 15600;
    };
  }; -- admin_addrs
  addrs = {
    {
      port = 15600;
      ip = "127.0.0.4";
    };
    {
      port = 80;
      ip = "2a02:6b8:0:3400::1092";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 8998;
      ip = "2a02:6b8:0:3400::1092";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "213.180.205.20";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 8998;
      ip = "213.180.205.20";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 15600;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 15600;
      ip = get_ip_by_iproute("v6");
    };
    {
      port = 443;
      ip = "2a02:6b8:0:3400::1092";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "213.180.205.20";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 15601;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 15601;
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
        15600;
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
        15600;
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
    http_section_80 = {
      ips = {
        "2a02:6b8:0:3400::1092";
        "213.180.205.20";
      }; -- ips
      ports = {
        80;
        8998;
      }; -- ports
      shared = {
        uuid = "654512622682957948";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 15600, "/place/db/www/logs/");
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            accesslog = {
              log = get_log_path("access_log", 15600, "/place/db/www/logs/");
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
                shared = {
                  uuid = "6705341324779669174";
                  request_replier = {
                    sink = {
                      balancer2 = {
                        unique_policy = {};
                        attempts = 1;
                        rr = {
                          unpack(gen_proxy_backends({
                            { "juggler-testing-api.search.yandex.net"; 80; 1.000; "2a02:6b8:0:3400:0:1b7:0:2"; };
                          }, {
                            resolve_timeout = "10ms";
                            connect_timeout = "100ms";
                            backend_timeout = "80s";
                            fail_on_5xx = false;
                            http_backend = true;
                            buffering = false;
                            keepalive_count = 0;
                            need_resolve = true;
                          }))
                        }; -- rr
                      }; -- balancer2
                    }; -- sink
                    enable_failed_requests_replication = true;
                    rate = 0.000;
                    rate_file = "./controls/request_repl.ratefile";
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
                      v1_slbping = {
                        priority = 5;
                        match_fsm = {
                          URI = "/ping";
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
                                  attempts = 4;
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
                                      { "iva1-0290.search.yandex.net"; 8998; 100.000; "2a02:6b8:b000:3106:ec4:7aff:fe18:ace"; };
                                      { "man2-2436.search.yandex.net"; 8998; 100.000; "2a02:6b8:c01:737:0:604:b2a9:7b16"; };
                                      { "myt1-1826.search.yandex.net"; 8998; 100.000; "2a02:6b8:b000:a024:428d:5cff:fe36:dbfc"; };
                                      { "sas2-2678.search.yandex.net"; 8998; 100.000; "2a02:6b8:c02:66b:0:604:5e92:ceaf"; };
                                      { "vla1-4118.search.yandex.net"; 8998; 100.000; "2a02:6b8:c0e:9c:0:604:db7:aa67"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "100ms";
                                      backend_timeout = "300ms";
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
                      }; -- v1_slbping
                      v2_slbping = {
                        priority = 4;
                        match_fsm = {
                          URI = "/ping";
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
                                  attempts = 4;
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
                                      { "iva1-0290.search.yandex.net"; 10711; 100.000; "2a02:6b8:b000:3106:ec4:7aff:fe18:ace"; };
                                      { "man2-2436.search.yandex.net"; 10711; 100.000; "2a02:6b8:c01:737:0:604:b2a9:7b16"; };
                                      { "myt1-1826.search.yandex.net"; 10711; 100.000; "2a02:6b8:b000:a024:428d:5cff:fe36:dbfc"; };
                                      { "sas1-1233.search.yandex.net"; 10711; 100.000; "2a02:6b8:b000:171:feaa:14ff:fea9:7a86"; };
                                      { "vla1-4118.search.yandex.net"; 10711; 100.000; "2a02:6b8:c0e:9c:0:604:db7:aa67"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "100ms";
                                      backend_timeout = "300ms";
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
                      }; -- v2_slbping
                      v1_default = {
                        priority = 3;
                        match_fsm = {
                          URI = "/(api(-slb)?|v1)(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        rewrite = {
                          actions = {
                            {
                              global = false;
                              literal = false;
                              rewrite = "/api%1";
                              regexp = "/v1(/.*)?";
                              case_insensitive = false;
                            };
                          }; -- actions
                          threshold = {
                            lo_bytes = 524288;
                            hi_bytes = 1048576;
                            recv_timeout = "1s";
                            pass_timeout = "9s";
                            headers = {
                              create_func_weak = {
                                ["X-Forwarded-For-Y"] = "realip";
                              }; -- create_func_weak
                              stats_eater = {
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 2;
                                  active = {
                                    delay = "10s";
                                    request = "GET /ping HTTP/1.1\r\nHost: juggler-api.search.yandex.net\r\n\r\n";
                                    steady = false;
                                    unpack(gen_proxy_backends({
                                      { "iva1-0290.search.yandex.net"; 8998; 100.000; "2a02:6b8:b000:3106:ec4:7aff:fe18:ace"; };
                                      { "man2-2436.search.yandex.net"; 8998; 100.000; "2a02:6b8:c01:737:0:604:b2a9:7b16"; };
                                      { "myt1-1826.search.yandex.net"; 8998; 100.000; "2a02:6b8:b000:a024:428d:5cff:fe36:dbfc"; };
                                      { "sas2-2678.search.yandex.net"; 8998; 100.000; "2a02:6b8:c02:66b:0:604:5e92:ceaf"; };
                                      { "vla1-4118.search.yandex.net"; 8998; 100.000; "2a02:6b8:c0e:9c:0:604:db7:aa67"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "100ms";
                                      backend_timeout = "75s";
                                      fail_on_5xx = false;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = true;
                                    }))
                                  }; -- active
                                }; -- balancer2
                              }; -- stats_eater
                            }; -- headers
                          }; -- threshold
                        }; -- rewrite
                      }; -- v1_default
                      v2_default = {
                        priority = 2;
                        match_fsm = {
                          URI = "/v2(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        threshold = {
                          lo_bytes = 524288;
                          hi_bytes = 1048576;
                          recv_timeout = "1s";
                          pass_timeout = "9s";
                          headers = {
                            create_func_weak = {
                              ["X-Forwarded-For-Y"] = "realip";
                            }; -- create_func_weak
                            stats_eater = {
                              balancer2 = {
                                unique_policy = {};
                                attempts = 2;
                                active = {
                                  delay = "10s";
                                  request = "GET /ping HTTP/1.1\r\nHost: juggler-api.search.yandex.net\r\n\r\n";
                                  steady = false;
                                  unpack(gen_proxy_backends({
                                    { "iva1-0290.search.yandex.net"; 10711; 100.000; "2a02:6b8:b000:3106:ec4:7aff:fe18:ace"; };
                                    { "man2-2436.search.yandex.net"; 10711; 100.000; "2a02:6b8:c01:737:0:604:b2a9:7b16"; };
                                    { "myt1-1826.search.yandex.net"; 10711; 100.000; "2a02:6b8:b000:a024:428d:5cff:fe36:dbfc"; };
                                    { "sas1-1233.search.yandex.net"; 10711; 100.000; "2a02:6b8:b000:171:feaa:14ff:fea9:7a86"; };
                                    { "vla1-4118.search.yandex.net"; 10711; 100.000; "2a02:6b8:c0e:9c:0:604:db7:aa67"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "75s";
                                    fail_on_5xx = false;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- active
                              }; -- balancer2
                            }; -- stats_eater
                          }; -- headers
                        }; -- threshold
                      }; -- v2_default
                      default = {
                        priority = 1;
                        headers = {
                          create = {
                            Location = "/api";
                          }; -- create
                          errordocument = {
                            status = 303;
                            force_conn_close = false;
                            remain_headers = "Location";
                          }; -- errordocument
                        }; -- headers
                      }; -- default
                    }; -- regexp
                  }; -- request_replier
                }; -- shared
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- errorlog
      }; -- shared
    }; -- http_section_80
    http_section_15600 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        15600;
      }; -- ports
      shared = {
        uuid = "654512622682957948";
      }; -- shared
    }; -- http_section_15600
    https_section_443 = {
      ips = {
        "2a02:6b8:0:3400::1092";
        "213.180.205.20";
      }; -- ips
      ports = {
        443;
      }; -- ports
      shared = {
        uuid = "7763721269529923074";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 15601, "/place/db/www/logs/");
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
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 15601, "/place/db/www/logs/");
                priv = get_private_cert_path("juggler-api.search.yandex.net.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-juggler-api.search.yandex.net.pem", "/dev/shm/balancer");
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.juggler-api.search.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.juggler-api.search.yandex.net.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.juggler-api.search.yandex.net.key", "/dev/shm/balancer/priv");
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
                log = get_log_path("access_log", 15601, "/place/db/www/logs/");
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
                  shared = {
                    uuid = "6705341324779669174";
                  }; -- shared
                }; -- report
              }; -- accesslog
            }; -- http
          }; -- ssl_sni
        }; -- errorlog
      }; -- shared
    }; -- https_section_443
    https_section_15601 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        15601;
      }; -- ports
      shared = {
        uuid = "7763721269529923074";
      }; -- shared
    }; -- https_section_15601
  }; -- ipdispatch
}