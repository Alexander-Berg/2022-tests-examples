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
  maxconn = 5000;
  buffer = 3145728;
  tcp_fastopen = 0;
  workers = get_workers();
  enable_reuse_port = true;
  private_address = "127.0.0.10";
  tcp_congestion_control = "bbr";
  default_tcp_rst_on_error = true;
  events = {
    stats = "report";
  }; -- events
  dns_ttl = get_random_timedelta(600, 900, "s");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", 15760, "/place/db/www/logs");
  admin_addrs = {
    {
      port = 15760;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 15760;
    };
  }; -- admin_addrs
  addrs = {
    {
      port = 15760;
      ip = "127.0.0.4";
    };
    {
      port = 80;
      ip = "2a02:6b8::232";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "77.88.21.232";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "87.250.250.232";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "87.250.251.232";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "93.158.134.232";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "213.180.193.232";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "213.180.204.232";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 15760;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 15760;
      ip = get_ip_by_iproute("v6");
    };
    {
      port = 443;
      ip = "2a02:6b8::232";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "77.88.21.232";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "87.250.250.232";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "87.250.251.232";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "93.158.134.232";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "213.180.193.232";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "213.180.204.232";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 15761;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 15761;
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
        15760;
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
        15760;
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
        "2a02:6b8::232";
        "77.88.21.232";
        "87.250.250.232";
        "87.250.251.232";
        "93.158.134.232";
        "213.180.193.232";
        "213.180.204.232";
      }; -- ips
      ports = {
        80;
      }; -- ports
      shared = {
        uuid = "5007946103763043428";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 15760, "/place/db/www/logs");
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            accesslog = {
              log = get_log_path("access_log", 15760, "/place/db/www/logs");
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
                regexp = {
                  ["awacs-balancer-health-check"] = {
                    priority = 4;
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
                    priority = 3;
                    match_fsm = {
                      url = "/monitor/monitor\\?param=check";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    shared = {
                      uuid = "1683180570280210434";
                      stats_eater = {
                        balancer2 = {
                          unique_policy = {};
                          attempts = 1;
                          rr = {
                            weights_file = "./controls/slb_check.weights";
                            to_upstream = {
                              weight = 1.000;
                              errordocument = {
                                status = 200;
                                force_conn_close = false;
                              }; -- errordocument
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
                    }; -- shared
                  }; -- slbping
                  http_internal_nets = {
                    priority = 2;
                    match_source_ip = {
                      source_mask = "5.45.192.0/18,5.255.192.0/18,37.9.64.0/18,37.140.128.0/18,77.88.0.0/18,84.201.128.0/18,87.250.224.0/19,93.158.128.0/18,95.108.128.0/17,100.43.64.0/19,130.193.32.0/19,141.8.128.0/18,178.154.128.0/17,199.21.96.0/22,199.36.240.0/22,213.180.192.0/19,2620:10f:d000::/44,2a02:6b8::/32";
                    }; -- match_source_ip
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
                        regexp = {
                          chunks = {
                            priority = 3;
                            match_fsm = {
                              URI = "/chunks(/.*)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            shared = {
                              uuid = "chunks";
                            }; -- shared
                          }; -- chunks
                          sbapiv4 = {
                            priority = 2;
                            match_fsm = {
                              url = "/v4/(.*)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            shared = {
                              uuid = "sbapiv4";
                            }; -- shared
                          }; -- sbapiv4
                          default = {
                            priority = 1;
                            shared = {
                              uuid = "1829982042268258749";
                              shared = {
                                uuid = "default";
                              }; -- shared
                            }; -- shared
                          }; -- default
                        }; -- regexp
                      }; -- response_headers
                    }; -- headers
                  }; -- http_internal_nets
                  default = {
                    priority = 1;
                    headers = {
                      create = {
                        Location = "1";
                      }; -- create
                      rewrite = {
                        actions = {
                          {
                            regexp = ".*";
                            global = false;
                            literal = false;
                            case_insensitive = false;
                            header_name = "Location";
                            rewrite = "https://%{host}%{url}";
                          };
                        }; -- actions
                        regexp = {
                          unsafe_methods = {
                            priority = 2;
                            match_fsm = {
                              match = "(DELETE|PATCH|POST|PUT).*";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            errordocument = {
                              status = 307;
                              force_conn_close = false;
                              remain_headers = "Location";
                            }; -- errordocument
                          }; -- unsafe_methods
                          default = {
                            priority = 1;
                            errordocument = {
                              status = 302;
                              force_conn_close = false;
                              remain_headers = "Location";
                            }; -- errordocument
                          }; -- default
                        }; -- regexp
                      }; -- rewrite
                    }; -- headers
                  }; -- default
                }; -- regexp
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- errorlog
      }; -- shared
    }; -- http_section_80
    http_section_15760 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        15760;
      }; -- ports
      shared = {
        uuid = "5007946103763043428";
      }; -- shared
    }; -- http_section_15760
    https_section_443 = {
      ips = {
        "2a02:6b8::232";
        "77.88.21.232";
        "87.250.250.232";
        "87.250.251.232";
        "93.158.134.232";
        "213.180.193.232";
        "213.180.204.232";
      }; -- ips
      ports = {
        443;
      }; -- ports
      shared = {
        uuid = "7784282916520349067";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 15761, "/place/db/www/logs");
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
                log = get_log_path("ssl_sni", 15761, "/place/db/www/logs");
                priv = get_private_cert_path("sba.yandex.ru.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-sba.yandex.ru.pem", "/dev/shm/balancer");
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.sba.yandex.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.sba.yandex.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.sba.yandex.ru.key", "/dev/shm/balancer/priv");
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
                log = get_log_path("access_log", 15761, "/place/db/www/logs");
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
                    create = {
                      ["X-Yandex-HTTPS"] = "yes";
                    }; -- create
                    response_headers = {
                      create = {
                        ["Strict-Transport-Security"] = "max-age=3600; includeSubDomains";
                      }; -- create
                      create_weak = {
                        ["X-Content-Type-Options"] = "nosniff";
                        ["X-XSS-Protection"] = "1; mode=block";
                      }; -- create_weak
                      regexp = {
                        ["awacs-balancer-health-check"] = {
                          priority = 16;
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
                          priority = 15;
                          match_fsm = {
                            url = "/monitor/monitor\\?param=check";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          shared = {
                            uuid = "1683180570280210434";
                          }; -- shared
                        }; -- slbping
                        https_clientreport = {
                          priority = 14;
                          match_fsm = {
                            url = "/clientreport/download.*";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          report = {
                            uuid = "clientreport";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            threshold = {
                              lo_bytes = 768000;
                              hi_bytes = 2097152;
                              recv_timeout = "1s";
                              pass_timeout = "1s";
                              on_pass_timeout_failure = {
                                shared = {
                                  uuid = "on_failure";
                                  errordocument = {
                                    status = 200;
                                    base64 = "CAI=";
                                    force_conn_close = false;
                                  }; -- errordocument
                                }; -- shared
                              }; -- on_pass_timeout_failure
                              balancer2 = {
                                by_name_policy = {
                                  name = get_geo("sba_", "random");
                                  unique_policy = {};
                                }; -- by_name_policy
                                attempts = 1;
                                rr = {
                                  weights_file = "./controls/traffic_control.weights";
                                  sba_sas = {
                                    weight = 1.000;
                                    report = {
                                      uuid = "requests_to_sas";
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
                                            { "sas1-0280.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:151:225:90ff:fe83:914"; };
                                            { "sas1-1492.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:179:215:b2ff:fea8:6cfc"; };
                                            { "sas1-1497.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:179:215:b2ff:fea8:cc6"; };
                                            { "sas1-1717.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:17a:feaa:14ff:fea9:7a68"; };
                                            { "sas1-2268.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:627:922b:34ff:fecf:3d70"; };
                                            { "sas1-2794.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:605:225:90ff:fe83:1a14"; };
                                            { "sas1-2819.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:60d:225:90ff:fe83:1662"; };
                                            { "sas1-3462.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe83:200"; };
                                            { "sas1-3613.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe88:b326"; };
                                            { "sas1-3899.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe88:cb82"; };
                                            { "sas1-4255.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63e:96de:80ff:fe8c:dd3a"; };
                                            { "sas1-4287.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:637:96de:80ff:fe8c:baf6"; };
                                            { "sas1-4288.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:638:96de:80ff:fe8c:dec4"; };
                                            { "sas1-4407.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:646:96de:80ff:fe8c:d864"; };
                                            { "sas1-4926.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:65c:96de:80ff:fe81:7f8"; };
                                            { "sas1-5329.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:1252"; };
                                            { "sas1-5336.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:11ca"; };
                                            { "sas1-5395.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:1896"; };
                                            { "sas1-5893.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:635:225:90ff:feec:2e24"; };
                                            { "sas1-5894.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:636:225:90ff:feec:2c62"; };
                                            { "sas1-5897.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:636:225:90ff:feec:3006"; };
                                            { "sas1-6326.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:11a:215:b2ff:fea7:7cb8"; };
                                            { "sas1-6637.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:119:215:b2ff:fea7:7a84"; };
                                            { "sas1-7823.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b67c"; };
                                            { "sas1-9603.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:199:215:b2ff:fea9:7642"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "3s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 1;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- report
                                  }; -- sba_sas
                                  sba_man = {
                                    weight = 1.000;
                                    report = {
                                      uuid = "requests_to_man";
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
                                            { "man1-0109.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6030:92e2:baff:fe6e:b964"; };
                                            { "man1-0139.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2a80"; };
                                            { "man1-0194.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:8094"; };
                                            { "man1-0347.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6036:92e2:baff:fe6f:7f08"; };
                                            { "man1-0404.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601e:92e2:baff:fe6f:7dd0"; };
                                            { "man1-0409.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600d:92e2:baff:fe74:795a"; };
                                            { "man1-0412.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6036:92e2:baff:fe74:7d60"; };
                                            { "man1-0730.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6034:92e2:baff:fe6e:bdf0"; };
                                            { "man1-0947.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600e:92e2:baff:fe55:f228"; };
                                            { "man1-0979.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f6a2"; };
                                            { "man1-1463.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:b350"; };
                                            { "man1-1501.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:d680"; };
                                            { "man1-1657.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601b:f652:14ff:fe8b:af10"; };
                                            { "man1-1762.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b750"; };
                                            { "man1-2941.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:eb20"; };
                                            { "man1-3232.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6000:f652:14ff:fe55:19f0"; };
                                            { "man1-3923.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6074:92e2:baff:fea1:78d2"; };
                                            { "man1-3978.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6074:92e2:baff:fea1:738e"; };
                                            { "man1-4518.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7d58"; };
                                            { "man1-4602.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601d:f652:14ff:fe8c:1e30"; };
                                            { "man1-4723.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6046:e61d:2dff:fe00:9860"; };
                                            { "man1-6606.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6048:e61d:2dff:fe01:f7f0"; };
                                            { "man1-6907.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6060:e61d:2dff:fe04:3490"; };
                                            { "man1-7122.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:38e0"; };
                                            { "man1-7234.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6066:e61d:2dff:fe04:23c0"; };
                                            { "man1-7335.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6067:e61d:2dff:fe6c:e610"; };
                                            { "man1-7368.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6069:e61d:2dff:fe6e:ea0"; };
                                            { "man1-7793.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:606e:e61d:2dff:fe6e:dd0"; };
                                            { "man1-7794.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:606e:e61d:2dff:fe6e:eb0"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "3s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 1;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- report
                                  }; -- sba_man
                                  sba_vla = {
                                    weight = 1.000;
                                    report = {
                                      uuid = "requests_to_vla";
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
                                            { "vla1-0046.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4f:0:604:5cf4:8caa"; };
                                            { "vla1-0089.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:2d:0:604:db7:9d36"; };
                                            { "vla1-0142.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a65b"; };
                                            { "vla1-0145.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a611"; };
                                            { "vla1-0149.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a77a"; };
                                            { "vla1-0169.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:13:0:604:db7:9a7c"; };
                                            { "vla1-0369.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3e:0:604:db6:17d7"; };
                                            { "vla1-0555.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:1b:0:604:db7:9986"; };
                                            { "vla1-0577.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:17:0:604:db7:992c"; };
                                            { "vla1-0656.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:13:0:604:db7:99a7"; };
                                            { "vla1-0832.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4e:0:604:db7:a1c5"; };
                                            { "vla1-0836.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:45:0:604:db7:a702"; };
                                            { "vla1-0848.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:48:0:604:db7:a208"; };
                                            { "vla1-0917.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:61:0:604:db7:9b0e"; };
                                            { "vla1-0924.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:71:0:604:db7:a408"; };
                                            { "vla1-0932.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:98:0:604:db7:a04f"; };
                                            { "vla1-0934.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:5e:0:604:db7:a180"; };
                                            { "vla1-0959.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:22:0:604:db7:9924"; };
                                            { "vla1-0994.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:42:0:604:db7:a4e6"; };
                                            { "vla1-2719.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:2f:0:604:db7:9fd3"; };
                                            { "vla1-4438.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:a0:0:604:db7:a683"; };
                                            { "vla1-4564.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:aa2e"; };
                                            { "vla1-4565.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:a938"; };
                                            { "vla1-4566.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:a7b6"; };
                                            { "vla1-4689.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4e:0:604:db7:a446"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "3s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 1;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- report
                                  }; -- sba_vla
                                }; -- rr
                                on_error = {
                                  shared = {
                                    uuid = "on_failure";
                                  }; -- shared
                                }; -- on_error
                              }; -- balancer2
                            }; -- threshold
                          }; -- report
                        }; -- https_clientreport
                        https_cp = {
                          priority = 13;
                          match_fsm = {
                            URI = "/cp(/.*)?";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          report = {
                            uuid = "cp";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            threshold = {
                              lo_bytes = 500;
                              hi_bytes = 1024;
                              recv_timeout = "1s";
                              pass_timeout = "9s";
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                rr = {
                                  weights_file = "./controls/traffic_control.weights";
                                  sba_sas = {
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
                                            { "sas1-0280.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:151:225:90ff:fe83:914"; };
                                            { "sas1-1492.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:179:215:b2ff:fea8:6cfc"; };
                                            { "sas1-1497.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:179:215:b2ff:fea8:cc6"; };
                                            { "sas1-1717.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:17a:feaa:14ff:fea9:7a68"; };
                                            { "sas1-2268.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:627:922b:34ff:fecf:3d70"; };
                                            { "sas1-2794.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:605:225:90ff:fe83:1a14"; };
                                            { "sas1-2819.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:60d:225:90ff:fe83:1662"; };
                                            { "sas1-3462.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe83:200"; };
                                            { "sas1-3613.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe88:b326"; };
                                            { "sas1-3899.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe88:cb82"; };
                                            { "sas1-4255.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63e:96de:80ff:fe8c:dd3a"; };
                                            { "sas1-4287.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:637:96de:80ff:fe8c:baf6"; };
                                            { "sas1-4288.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:638:96de:80ff:fe8c:dec4"; };
                                            { "sas1-4407.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:646:96de:80ff:fe8c:d864"; };
                                            { "sas1-4926.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:65c:96de:80ff:fe81:7f8"; };
                                            { "sas1-5329.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:1252"; };
                                            { "sas1-5336.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:11ca"; };
                                            { "sas1-5395.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:1896"; };
                                            { "sas1-5893.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:635:225:90ff:feec:2e24"; };
                                            { "sas1-5894.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:636:225:90ff:feec:2c62"; };
                                            { "sas1-5897.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:636:225:90ff:feec:3006"; };
                                            { "sas1-6326.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:11a:215:b2ff:fea7:7cb8"; };
                                            { "sas1-6637.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:119:215:b2ff:fea7:7a84"; };
                                            { "sas1-7823.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b67c"; };
                                            { "sas1-9603.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:199:215:b2ff:fea9:7642"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "40ms";
                                            backend_timeout = "60ms";
                                            fail_on_5xx = false;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 1;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                      refers = "requests_to_sas";
                                    }; -- report
                                  }; -- sba_sas
                                  sba_man = {
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
                                            { "man1-0109.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6030:92e2:baff:fe6e:b964"; };
                                            { "man1-0139.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2a80"; };
                                            { "man1-0194.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:8094"; };
                                            { "man1-0347.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6036:92e2:baff:fe6f:7f08"; };
                                            { "man1-0404.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601e:92e2:baff:fe6f:7dd0"; };
                                            { "man1-0409.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600d:92e2:baff:fe74:795a"; };
                                            { "man1-0412.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6036:92e2:baff:fe74:7d60"; };
                                            { "man1-0730.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6034:92e2:baff:fe6e:bdf0"; };
                                            { "man1-0947.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600e:92e2:baff:fe55:f228"; };
                                            { "man1-0979.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f6a2"; };
                                            { "man1-1463.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:b350"; };
                                            { "man1-1501.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:d680"; };
                                            { "man1-1657.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601b:f652:14ff:fe8b:af10"; };
                                            { "man1-1762.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b750"; };
                                            { "man1-2941.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:eb20"; };
                                            { "man1-3232.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6000:f652:14ff:fe55:19f0"; };
                                            { "man1-3923.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6074:92e2:baff:fea1:78d2"; };
                                            { "man1-3978.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6074:92e2:baff:fea1:738e"; };
                                            { "man1-4518.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7d58"; };
                                            { "man1-4602.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601d:f652:14ff:fe8c:1e30"; };
                                            { "man1-4723.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6046:e61d:2dff:fe00:9860"; };
                                            { "man1-6606.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6048:e61d:2dff:fe01:f7f0"; };
                                            { "man1-6907.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6060:e61d:2dff:fe04:3490"; };
                                            { "man1-7122.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:38e0"; };
                                            { "man1-7234.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6066:e61d:2dff:fe04:23c0"; };
                                            { "man1-7335.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6067:e61d:2dff:fe6c:e610"; };
                                            { "man1-7368.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6069:e61d:2dff:fe6e:ea0"; };
                                            { "man1-7793.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:606e:e61d:2dff:fe6e:dd0"; };
                                            { "man1-7794.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:606e:e61d:2dff:fe6e:eb0"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "40ms";
                                            backend_timeout = "60ms";
                                            fail_on_5xx = false;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 1;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                      refers = "requests_to_man";
                                    }; -- report
                                  }; -- sba_man
                                  sba_vla = {
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
                                            { "vla1-0046.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4f:0:604:5cf4:8caa"; };
                                            { "vla1-0089.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:2d:0:604:db7:9d36"; };
                                            { "vla1-0142.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a65b"; };
                                            { "vla1-0145.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a611"; };
                                            { "vla1-0149.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a77a"; };
                                            { "vla1-0169.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:13:0:604:db7:9a7c"; };
                                            { "vla1-0369.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3e:0:604:db6:17d7"; };
                                            { "vla1-0555.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:1b:0:604:db7:9986"; };
                                            { "vla1-0577.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:17:0:604:db7:992c"; };
                                            { "vla1-0656.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:13:0:604:db7:99a7"; };
                                            { "vla1-0832.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4e:0:604:db7:a1c5"; };
                                            { "vla1-0836.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:45:0:604:db7:a702"; };
                                            { "vla1-0848.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:48:0:604:db7:a208"; };
                                            { "vla1-0917.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:61:0:604:db7:9b0e"; };
                                            { "vla1-0924.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:71:0:604:db7:a408"; };
                                            { "vla1-0932.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:98:0:604:db7:a04f"; };
                                            { "vla1-0934.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:5e:0:604:db7:a180"; };
                                            { "vla1-0959.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:22:0:604:db7:9924"; };
                                            { "vla1-0994.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:42:0:604:db7:a4e6"; };
                                            { "vla1-2719.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:2f:0:604:db7:9fd3"; };
                                            { "vla1-4438.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:a0:0:604:db7:a683"; };
                                            { "vla1-4564.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:aa2e"; };
                                            { "vla1-4565.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:a938"; };
                                            { "vla1-4566.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:a7b6"; };
                                            { "vla1-4689.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4e:0:604:db7:a446"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "40ms";
                                            backend_timeout = "60ms";
                                            fail_on_5xx = false;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 1;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                      refers = "requests_to_vla";
                                    }; -- report
                                  }; -- sba_vla
                                }; -- rr
                                on_error = {
                                  shared = {
                                    uuid = "4521384360058968294";
                                  }; -- shared
                                }; -- on_error
                              }; -- balancer2
                            }; -- threshold
                          }; -- report
                        }; -- https_cp
                        https_downloads = {
                          priority = 12;
                          match_fsm = {
                            URI = "/downloads(/.*)?";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          report = {
                            uuid = "downloads";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            threshold = {
                              lo_bytes = 30720;
                              hi_bytes = 51200;
                              recv_timeout = "1s";
                              pass_timeout = "9s";
                              shared = {
                                uuid = "1654740932832296895";
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 3;
                                  rr = {
                                    weights_file = "./controls/traffic_control.weights";
                                    sba_sas = {
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
                                              { "sas1-0280.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:151:225:90ff:fe83:914"; };
                                              { "sas1-1492.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:179:215:b2ff:fea8:6cfc"; };
                                              { "sas1-1497.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:179:215:b2ff:fea8:cc6"; };
                                              { "sas1-1717.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:17a:feaa:14ff:fea9:7a68"; };
                                              { "sas1-2268.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:627:922b:34ff:fecf:3d70"; };
                                              { "sas1-2794.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:605:225:90ff:fe83:1a14"; };
                                              { "sas1-2819.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:60d:225:90ff:fe83:1662"; };
                                              { "sas1-3462.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe83:200"; };
                                              { "sas1-3613.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe88:b326"; };
                                              { "sas1-3899.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe88:cb82"; };
                                              { "sas1-4255.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63e:96de:80ff:fe8c:dd3a"; };
                                              { "sas1-4287.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:637:96de:80ff:fe8c:baf6"; };
                                              { "sas1-4288.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:638:96de:80ff:fe8c:dec4"; };
                                              { "sas1-4407.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:646:96de:80ff:fe8c:d864"; };
                                              { "sas1-4926.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:65c:96de:80ff:fe81:7f8"; };
                                              { "sas1-5329.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:1252"; };
                                              { "sas1-5336.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:11ca"; };
                                              { "sas1-5395.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:1896"; };
                                              { "sas1-5893.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:635:225:90ff:feec:2e24"; };
                                              { "sas1-5894.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:636:225:90ff:feec:2c62"; };
                                              { "sas1-5897.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:636:225:90ff:feec:3006"; };
                                              { "sas1-6326.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:11a:215:b2ff:fea7:7cb8"; };
                                              { "sas1-6637.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:119:215:b2ff:fea7:7a84"; };
                                              { "sas1-7823.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b67c"; };
                                              { "sas1-9603.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:199:215:b2ff:fea9:7642"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "40ms";
                                              backend_timeout = "150ms";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                        }; -- balancer2
                                        refers = "requests_to_sas";
                                      }; -- report
                                    }; -- sba_sas
                                    sba_man = {
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
                                              { "man1-0109.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6030:92e2:baff:fe6e:b964"; };
                                              { "man1-0139.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2a80"; };
                                              { "man1-0194.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:8094"; };
                                              { "man1-0347.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6036:92e2:baff:fe6f:7f08"; };
                                              { "man1-0404.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601e:92e2:baff:fe6f:7dd0"; };
                                              { "man1-0409.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600d:92e2:baff:fe74:795a"; };
                                              { "man1-0412.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6036:92e2:baff:fe74:7d60"; };
                                              { "man1-0730.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6034:92e2:baff:fe6e:bdf0"; };
                                              { "man1-0947.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600e:92e2:baff:fe55:f228"; };
                                              { "man1-0979.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f6a2"; };
                                              { "man1-1463.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:b350"; };
                                              { "man1-1501.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:d680"; };
                                              { "man1-1657.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601b:f652:14ff:fe8b:af10"; };
                                              { "man1-1762.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b750"; };
                                              { "man1-2941.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:eb20"; };
                                              { "man1-3232.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6000:f652:14ff:fe55:19f0"; };
                                              { "man1-3923.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6074:92e2:baff:fea1:78d2"; };
                                              { "man1-3978.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6074:92e2:baff:fea1:738e"; };
                                              { "man1-4518.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7d58"; };
                                              { "man1-4602.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601d:f652:14ff:fe8c:1e30"; };
                                              { "man1-4723.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6046:e61d:2dff:fe00:9860"; };
                                              { "man1-6606.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6048:e61d:2dff:fe01:f7f0"; };
                                              { "man1-6907.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6060:e61d:2dff:fe04:3490"; };
                                              { "man1-7122.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:38e0"; };
                                              { "man1-7234.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6066:e61d:2dff:fe04:23c0"; };
                                              { "man1-7335.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6067:e61d:2dff:fe6c:e610"; };
                                              { "man1-7368.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6069:e61d:2dff:fe6e:ea0"; };
                                              { "man1-7793.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:606e:e61d:2dff:fe6e:dd0"; };
                                              { "man1-7794.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:606e:e61d:2dff:fe6e:eb0"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "40ms";
                                              backend_timeout = "150ms";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                        }; -- balancer2
                                        refers = "requests_to_man";
                                      }; -- report
                                    }; -- sba_man
                                    sba_vla = {
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
                                              { "vla1-0046.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4f:0:604:5cf4:8caa"; };
                                              { "vla1-0089.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:2d:0:604:db7:9d36"; };
                                              { "vla1-0142.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a65b"; };
                                              { "vla1-0145.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a611"; };
                                              { "vla1-0149.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a77a"; };
                                              { "vla1-0169.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:13:0:604:db7:9a7c"; };
                                              { "vla1-0369.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3e:0:604:db6:17d7"; };
                                              { "vla1-0555.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:1b:0:604:db7:9986"; };
                                              { "vla1-0577.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:17:0:604:db7:992c"; };
                                              { "vla1-0656.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:13:0:604:db7:99a7"; };
                                              { "vla1-0832.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4e:0:604:db7:a1c5"; };
                                              { "vla1-0836.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:45:0:604:db7:a702"; };
                                              { "vla1-0848.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:48:0:604:db7:a208"; };
                                              { "vla1-0917.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:61:0:604:db7:9b0e"; };
                                              { "vla1-0924.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:71:0:604:db7:a408"; };
                                              { "vla1-0932.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:98:0:604:db7:a04f"; };
                                              { "vla1-0934.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:5e:0:604:db7:a180"; };
                                              { "vla1-0959.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:22:0:604:db7:9924"; };
                                              { "vla1-0994.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:42:0:604:db7:a4e6"; };
                                              { "vla1-2719.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:2f:0:604:db7:9fd3"; };
                                              { "vla1-4438.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:a0:0:604:db7:a683"; };
                                              { "vla1-4564.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:aa2e"; };
                                              { "vla1-4565.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:a938"; };
                                              { "vla1-4566.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:a7b6"; };
                                              { "vla1-4689.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4e:0:604:db7:a446"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "40ms";
                                              backend_timeout = "150ms";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                        }; -- balancer2
                                        refers = "requests_to_vla";
                                      }; -- report
                                    }; -- sba_vla
                                  }; -- rr
                                  on_error = {
                                    shared = {
                                      uuid = "4521384360058968294";
                                    }; -- shared
                                  }; -- on_error
                                }; -- balancer2
                              }; -- shared
                            }; -- threshold
                          }; -- report
                        }; -- https_downloads
                        https_malware = {
                          priority = 11;
                          match_fsm = {
                            URI = "/clientreport/malware(/.*)?";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          report = {
                            uuid = "malware";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            shared = {
                              uuid = "malware";
                              threshold = {
                                lo_bytes = 500;
                                hi_bytes = 1024;
                                recv_timeout = "1s";
                                pass_timeout = "9s";
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 3;
                                  rr = {
                                    weights_file = "./controls/traffic_control.weights";
                                    sba_sas = {
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
                                              { "sas1-0280.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:151:225:90ff:fe83:914"; };
                                              { "sas1-1492.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:179:215:b2ff:fea8:6cfc"; };
                                              { "sas1-1497.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:179:215:b2ff:fea8:cc6"; };
                                              { "sas1-1717.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:17a:feaa:14ff:fea9:7a68"; };
                                              { "sas1-2268.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:627:922b:34ff:fecf:3d70"; };
                                              { "sas1-2794.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:605:225:90ff:fe83:1a14"; };
                                              { "sas1-2819.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:60d:225:90ff:fe83:1662"; };
                                              { "sas1-3462.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe83:200"; };
                                              { "sas1-3613.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe88:b326"; };
                                              { "sas1-3899.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe88:cb82"; };
                                              { "sas1-4255.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63e:96de:80ff:fe8c:dd3a"; };
                                              { "sas1-4287.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:637:96de:80ff:fe8c:baf6"; };
                                              { "sas1-4288.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:638:96de:80ff:fe8c:dec4"; };
                                              { "sas1-4407.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:646:96de:80ff:fe8c:d864"; };
                                              { "sas1-4926.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:65c:96de:80ff:fe81:7f8"; };
                                              { "sas1-5329.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:1252"; };
                                              { "sas1-5336.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:11ca"; };
                                              { "sas1-5395.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:1896"; };
                                              { "sas1-5893.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:635:225:90ff:feec:2e24"; };
                                              { "sas1-5894.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:636:225:90ff:feec:2c62"; };
                                              { "sas1-5897.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:636:225:90ff:feec:3006"; };
                                              { "sas1-6326.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:11a:215:b2ff:fea7:7cb8"; };
                                              { "sas1-6637.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:119:215:b2ff:fea7:7a84"; };
                                              { "sas1-7823.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b67c"; };
                                              { "sas1-9603.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:199:215:b2ff:fea9:7642"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "40ms";
                                              backend_timeout = "80ms";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                        }; -- balancer2
                                        refers = "requests_to_sas";
                                      }; -- report
                                    }; -- sba_sas
                                    sba_man = {
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
                                              { "man1-0109.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6030:92e2:baff:fe6e:b964"; };
                                              { "man1-0139.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2a80"; };
                                              { "man1-0194.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:8094"; };
                                              { "man1-0347.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6036:92e2:baff:fe6f:7f08"; };
                                              { "man1-0404.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601e:92e2:baff:fe6f:7dd0"; };
                                              { "man1-0409.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600d:92e2:baff:fe74:795a"; };
                                              { "man1-0412.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6036:92e2:baff:fe74:7d60"; };
                                              { "man1-0730.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6034:92e2:baff:fe6e:bdf0"; };
                                              { "man1-0947.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600e:92e2:baff:fe55:f228"; };
                                              { "man1-0979.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f6a2"; };
                                              { "man1-1463.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:b350"; };
                                              { "man1-1501.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:d680"; };
                                              { "man1-1657.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601b:f652:14ff:fe8b:af10"; };
                                              { "man1-1762.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b750"; };
                                              { "man1-2941.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:eb20"; };
                                              { "man1-3232.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6000:f652:14ff:fe55:19f0"; };
                                              { "man1-3923.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6074:92e2:baff:fea1:78d2"; };
                                              { "man1-3978.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6074:92e2:baff:fea1:738e"; };
                                              { "man1-4518.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7d58"; };
                                              { "man1-4602.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601d:f652:14ff:fe8c:1e30"; };
                                              { "man1-4723.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6046:e61d:2dff:fe00:9860"; };
                                              { "man1-6606.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6048:e61d:2dff:fe01:f7f0"; };
                                              { "man1-6907.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6060:e61d:2dff:fe04:3490"; };
                                              { "man1-7122.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:38e0"; };
                                              { "man1-7234.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6066:e61d:2dff:fe04:23c0"; };
                                              { "man1-7335.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6067:e61d:2dff:fe6c:e610"; };
                                              { "man1-7368.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6069:e61d:2dff:fe6e:ea0"; };
                                              { "man1-7793.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:606e:e61d:2dff:fe6e:dd0"; };
                                              { "man1-7794.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:606e:e61d:2dff:fe6e:eb0"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "40ms";
                                              backend_timeout = "80ms";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                        }; -- balancer2
                                        refers = "requests_to_man";
                                      }; -- report
                                    }; -- sba_man
                                    sba_vla = {
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
                                              { "vla1-0046.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4f:0:604:5cf4:8caa"; };
                                              { "vla1-0089.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:2d:0:604:db7:9d36"; };
                                              { "vla1-0142.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a65b"; };
                                              { "vla1-0145.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a611"; };
                                              { "vla1-0149.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a77a"; };
                                              { "vla1-0169.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:13:0:604:db7:9a7c"; };
                                              { "vla1-0369.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3e:0:604:db6:17d7"; };
                                              { "vla1-0555.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:1b:0:604:db7:9986"; };
                                              { "vla1-0577.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:17:0:604:db7:992c"; };
                                              { "vla1-0656.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:13:0:604:db7:99a7"; };
                                              { "vla1-0832.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4e:0:604:db7:a1c5"; };
                                              { "vla1-0836.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:45:0:604:db7:a702"; };
                                              { "vla1-0848.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:48:0:604:db7:a208"; };
                                              { "vla1-0917.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:61:0:604:db7:9b0e"; };
                                              { "vla1-0924.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:71:0:604:db7:a408"; };
                                              { "vla1-0932.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:98:0:604:db7:a04f"; };
                                              { "vla1-0934.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:5e:0:604:db7:a180"; };
                                              { "vla1-0959.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:22:0:604:db7:9924"; };
                                              { "vla1-0994.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:42:0:604:db7:a4e6"; };
                                              { "vla1-2719.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:2f:0:604:db7:9fd3"; };
                                              { "vla1-4438.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:a0:0:604:db7:a683"; };
                                              { "vla1-4564.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:aa2e"; };
                                              { "vla1-4565.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:a938"; };
                                              { "vla1-4566.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:a7b6"; };
                                              { "vla1-4689.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4e:0:604:db7:a446"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "40ms";
                                              backend_timeout = "80ms";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                        }; -- balancer2
                                        refers = "requests_to_vla";
                                      }; -- report
                                    }; -- sba_vla
                                  }; -- rr
                                  on_error = {
                                    shared = {
                                      uuid = "4521384360058968294";
                                    }; -- shared
                                  }; -- on_error
                                }; -- balancer2
                              }; -- threshold
                            }; -- shared
                          }; -- report
                        }; -- https_malware
                        https_gethash = {
                          priority = 10;
                          match_fsm = {
                            URI = "/(gethash|get_nm_hash)(/.*)?";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          report = {
                            uuid = "gethash";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            shared = {
                              uuid = "2153604649003956927";
                              shared = {
                                uuid = "malware";
                              }; -- shared
                            }; -- shared
                          }; -- report
                        }; -- https_gethash
                        https_lookup = {
                          priority = 9;
                          match_fsm = {
                            URI = "/lookup(/.*)?";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          report = {
                            uuid = "lookup";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            shared = {
                              uuid = "1829982042268258749";
                            }; -- shared
                          }; -- report
                        }; -- https_lookup
                        https_urlinfo = {
                          priority = 8;
                          match_fsm = {
                            URI = "/urlinfo(/.*)?";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          report = {
                            uuid = "urlinfo";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            shared = {
                              uuid = "2153604649003956927";
                            }; -- shared
                          }; -- report
                        }; -- https_urlinfo
                        https_chunks = {
                          priority = 7;
                          match_fsm = {
                            URI = "/chunks(/.*)?";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          shared = {
                            uuid = "chunks";
                            report = {
                              uuid = "chunks";
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
                                rr = {
                                  weights_file = "./controls/traffic_control.weights";
                                  sba_sas = {
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
                                            { "sas1-0280.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:151:225:90ff:fe83:914"; };
                                            { "sas1-1492.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:179:215:b2ff:fea8:6cfc"; };
                                            { "sas1-1497.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:179:215:b2ff:fea8:cc6"; };
                                            { "sas1-1717.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:17a:feaa:14ff:fea9:7a68"; };
                                            { "sas1-2268.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:627:922b:34ff:fecf:3d70"; };
                                            { "sas1-2794.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:605:225:90ff:fe83:1a14"; };
                                            { "sas1-2819.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:60d:225:90ff:fe83:1662"; };
                                            { "sas1-3462.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe83:200"; };
                                            { "sas1-3613.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe88:b326"; };
                                            { "sas1-3899.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe88:cb82"; };
                                            { "sas1-4255.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63e:96de:80ff:fe8c:dd3a"; };
                                            { "sas1-4287.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:637:96de:80ff:fe8c:baf6"; };
                                            { "sas1-4288.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:638:96de:80ff:fe8c:dec4"; };
                                            { "sas1-4407.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:646:96de:80ff:fe8c:d864"; };
                                            { "sas1-4926.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:65c:96de:80ff:fe81:7f8"; };
                                            { "sas1-5329.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:1252"; };
                                            { "sas1-5336.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:11ca"; };
                                            { "sas1-5395.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:1896"; };
                                            { "sas1-5893.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:635:225:90ff:feec:2e24"; };
                                            { "sas1-5894.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:636:225:90ff:feec:2c62"; };
                                            { "sas1-5897.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:636:225:90ff:feec:3006"; };
                                            { "sas1-6326.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:11a:215:b2ff:fea7:7cb8"; };
                                            { "sas1-6637.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:119:215:b2ff:fea7:7a84"; };
                                            { "sas1-7823.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b67c"; };
                                            { "sas1-9603.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:199:215:b2ff:fea9:7642"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "40ms";
                                            backend_timeout = "10s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 1;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                      refers = "requests_to_sas";
                                    }; -- report
                                  }; -- sba_sas
                                  sba_man = {
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
                                            { "man1-0109.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6030:92e2:baff:fe6e:b964"; };
                                            { "man1-0139.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2a80"; };
                                            { "man1-0194.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:8094"; };
                                            { "man1-0347.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6036:92e2:baff:fe6f:7f08"; };
                                            { "man1-0404.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601e:92e2:baff:fe6f:7dd0"; };
                                            { "man1-0409.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600d:92e2:baff:fe74:795a"; };
                                            { "man1-0412.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6036:92e2:baff:fe74:7d60"; };
                                            { "man1-0730.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6034:92e2:baff:fe6e:bdf0"; };
                                            { "man1-0947.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600e:92e2:baff:fe55:f228"; };
                                            { "man1-0979.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f6a2"; };
                                            { "man1-1463.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:b350"; };
                                            { "man1-1501.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:d680"; };
                                            { "man1-1657.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601b:f652:14ff:fe8b:af10"; };
                                            { "man1-1762.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b750"; };
                                            { "man1-2941.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:eb20"; };
                                            { "man1-3232.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6000:f652:14ff:fe55:19f0"; };
                                            { "man1-3923.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6074:92e2:baff:fea1:78d2"; };
                                            { "man1-3978.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6074:92e2:baff:fea1:738e"; };
                                            { "man1-4518.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7d58"; };
                                            { "man1-4602.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601d:f652:14ff:fe8c:1e30"; };
                                            { "man1-4723.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6046:e61d:2dff:fe00:9860"; };
                                            { "man1-6606.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6048:e61d:2dff:fe01:f7f0"; };
                                            { "man1-6907.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6060:e61d:2dff:fe04:3490"; };
                                            { "man1-7122.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:38e0"; };
                                            { "man1-7234.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6066:e61d:2dff:fe04:23c0"; };
                                            { "man1-7335.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6067:e61d:2dff:fe6c:e610"; };
                                            { "man1-7368.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6069:e61d:2dff:fe6e:ea0"; };
                                            { "man1-7793.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:606e:e61d:2dff:fe6e:dd0"; };
                                            { "man1-7794.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:606e:e61d:2dff:fe6e:eb0"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "40ms";
                                            backend_timeout = "10s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 1;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                      refers = "requests_to_man";
                                    }; -- report
                                  }; -- sba_man
                                  sba_vla = {
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
                                            { "vla1-0046.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4f:0:604:5cf4:8caa"; };
                                            { "vla1-0089.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:2d:0:604:db7:9d36"; };
                                            { "vla1-0142.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a65b"; };
                                            { "vla1-0145.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a611"; };
                                            { "vla1-0149.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a77a"; };
                                            { "vla1-0169.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:13:0:604:db7:9a7c"; };
                                            { "vla1-0369.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3e:0:604:db6:17d7"; };
                                            { "vla1-0555.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:1b:0:604:db7:9986"; };
                                            { "vla1-0577.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:17:0:604:db7:992c"; };
                                            { "vla1-0656.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:13:0:604:db7:99a7"; };
                                            { "vla1-0832.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4e:0:604:db7:a1c5"; };
                                            { "vla1-0836.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:45:0:604:db7:a702"; };
                                            { "vla1-0848.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:48:0:604:db7:a208"; };
                                            { "vla1-0917.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:61:0:604:db7:9b0e"; };
                                            { "vla1-0924.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:71:0:604:db7:a408"; };
                                            { "vla1-0932.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:98:0:604:db7:a04f"; };
                                            { "vla1-0934.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:5e:0:604:db7:a180"; };
                                            { "vla1-0959.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:22:0:604:db7:9924"; };
                                            { "vla1-0994.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:42:0:604:db7:a4e6"; };
                                            { "vla1-2719.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:2f:0:604:db7:9fd3"; };
                                            { "vla1-4438.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:a0:0:604:db7:a683"; };
                                            { "vla1-4564.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:aa2e"; };
                                            { "vla1-4565.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:a938"; };
                                            { "vla1-4566.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:a7b6"; };
                                            { "vla1-4689.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4e:0:604:db7:a446"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "40ms";
                                            backend_timeout = "10s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 1;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                      refers = "requests_to_vla";
                                    }; -- report
                                  }; -- sba_vla
                                }; -- rr
                                on_error = {
                                  shared = {
                                    uuid = "4521384360058968294";
                                  }; -- shared
                                }; -- on_error
                              }; -- balancer2
                            }; -- report
                          }; -- shared
                        }; -- https_chunks
                        https_safesearch = {
                          priority = 6;
                          match_fsm = {
                            url = "/(infected_check|redirect|safety)(.*)?";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          report = {
                            uuid = "safesearch";
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
                              rr = {
                                weights_file = "./controls/traffic_control.weights";
                                safesearch_sas = {
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
                                          { "sas1-0667.search.yandex.net"; 8370; 200.000; "2a02:6b8:b000:144:225:90ff:fe83:214"; };
                                          { "sas1-5393.search.yandex.net"; 8370; 200.000; "2a02:6b8:b000:63d:96de:80ff:fe81:1086"; };
                                          { "sas1-5579.search.yandex.net"; 8370; 200.000; "2a02:6b8:b000:649:225:90ff:fe95:8108"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "40ms";
                                          backend_timeout = "400ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 1;
                                          need_resolve = true;
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                    refers = "requests_to_sas";
                                  }; -- report
                                }; -- safesearch_sas
                                safesearch_man = {
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
                                          { "man1-2679.search.yandex.net"; 18610; 200.000; "2a02:6b8:b000:6026:f652:14ff:fe8b:f5e0"; };
                                          { "man1-4173.search.yandex.net"; 18610; 200.000; "2a02:6b8:b000:6040:92e2:baff:fe6f:7fe4"; };
                                          { "man1-7861.search.yandex.net"; 18610; 200.000; "2a02:6b8:b000:606f:e61d:2dff:fe6d:a860"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "40ms";
                                          backend_timeout = "400ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 1;
                                          need_resolve = true;
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                    refers = "requests_to_man";
                                  }; -- report
                                }; -- safesearch_man
                                safesearch_vla = {
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
                                          { "vla1-1339.search.yandex.net"; 14510; 200.000; "2a02:6b8:c0e:5d:0:604:db7:9e09"; };
                                          { "vla1-1431.search.yandex.net"; 14510; 200.000; "2a02:6b8:c0e:83:0:604:db7:a7f6"; };
                                          { "vla1-2233.search.yandex.net"; 14510; 200.000; "2a02:6b8:c0e:5a:0:604:db7:a242"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "40ms";
                                          backend_timeout = "400ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 1;
                                          need_resolve = true;
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                    refers = "requests_to_vla";
                                  }; -- report
                                }; -- safesearch_vla
                              }; -- rr
                              on_error = {
                                shared = {
                                  uuid = "4521384360058968294";
                                  report = {
                                    uuid = "onerror";
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
                              }; -- on_error
                            }; -- balancer2
                          }; -- report
                        }; -- https_safesearch
                        https_threat_matches = {
                          priority = 5;
                          match_fsm = {
                            url = "/v4/threatMatches(.*)?";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          shared = {
                            uuid = "threatmatches";
                            report = {
                              uuid = "threatmatches";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              shared = {
                                uuid = "1654740932832296895";
                              }; -- shared
                            }; -- report
                          }; -- shared
                        }; -- https_threat_matches
                        https_full_hashes = {
                          priority = 4;
                          match_fsm = {
                            url = "/v4/fullHashes(.*)?";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          shared = {
                            uuid = "fullhashes";
                            report = {
                              uuid = "fullhashes";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              shared = {
                                uuid = "1654740932832296895";
                              }; -- shared
                            }; -- report
                          }; -- shared
                        }; -- https_full_hashes
                        https_threat_list_updates = {
                          priority = 3;
                          match_fsm = {
                            url = "/v4/threatListUpdates(.*)?";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          shared = {
                            uuid = "threatlistupdates";
                            report = {
                              uuid = "threatlistupdates";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              shared = {
                                uuid = "1654740932832296895";
                              }; -- shared
                            }; -- report
                          }; -- shared
                        }; -- https_threat_list_updates
                        https_sbapiv4 = {
                          priority = 2;
                          match_fsm = {
                            url = "/v4/(.*)?";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          shared = {
                            uuid = "sbapiv4";
                            report = {
                              uuid = "sbapiv4";
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
                                rr = {
                                  weights_file = "./controls/traffic_control.weights";
                                  sba_sas = {
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
                                            { "sas1-0280.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:151:225:90ff:fe83:914"; };
                                            { "sas1-1492.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:179:215:b2ff:fea8:6cfc"; };
                                            { "sas1-1497.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:179:215:b2ff:fea8:cc6"; };
                                            { "sas1-1717.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:17a:feaa:14ff:fea9:7a68"; };
                                            { "sas1-2268.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:627:922b:34ff:fecf:3d70"; };
                                            { "sas1-2794.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:605:225:90ff:fe83:1a14"; };
                                            { "sas1-2819.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:60d:225:90ff:fe83:1662"; };
                                            { "sas1-3462.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe83:200"; };
                                            { "sas1-3613.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe88:b326"; };
                                            { "sas1-3899.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe88:cb82"; };
                                            { "sas1-4255.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63e:96de:80ff:fe8c:dd3a"; };
                                            { "sas1-4287.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:637:96de:80ff:fe8c:baf6"; };
                                            { "sas1-4288.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:638:96de:80ff:fe8c:dec4"; };
                                            { "sas1-4407.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:646:96de:80ff:fe8c:d864"; };
                                            { "sas1-4926.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:65c:96de:80ff:fe81:7f8"; };
                                            { "sas1-5329.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:1252"; };
                                            { "sas1-5336.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:11ca"; };
                                            { "sas1-5395.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:1896"; };
                                            { "sas1-5893.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:635:225:90ff:feec:2e24"; };
                                            { "sas1-5894.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:636:225:90ff:feec:2c62"; };
                                            { "sas1-5897.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:636:225:90ff:feec:3006"; };
                                            { "sas1-6326.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:11a:215:b2ff:fea7:7cb8"; };
                                            { "sas1-6637.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:119:215:b2ff:fea7:7a84"; };
                                            { "sas1-7823.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b67c"; };
                                            { "sas1-9603.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:199:215:b2ff:fea9:7642"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "40ms";
                                            backend_timeout = "1s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 1;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                      refers = "requests_to_sas";
                                    }; -- report
                                  }; -- sba_sas
                                  sba_man = {
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
                                            { "man1-0109.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6030:92e2:baff:fe6e:b964"; };
                                            { "man1-0139.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2a80"; };
                                            { "man1-0194.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:8094"; };
                                            { "man1-0347.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6036:92e2:baff:fe6f:7f08"; };
                                            { "man1-0404.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601e:92e2:baff:fe6f:7dd0"; };
                                            { "man1-0409.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600d:92e2:baff:fe74:795a"; };
                                            { "man1-0412.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6036:92e2:baff:fe74:7d60"; };
                                            { "man1-0730.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6034:92e2:baff:fe6e:bdf0"; };
                                            { "man1-0947.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600e:92e2:baff:fe55:f228"; };
                                            { "man1-0979.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f6a2"; };
                                            { "man1-1463.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:b350"; };
                                            { "man1-1501.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:d680"; };
                                            { "man1-1657.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601b:f652:14ff:fe8b:af10"; };
                                            { "man1-1762.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b750"; };
                                            { "man1-2941.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:eb20"; };
                                            { "man1-3232.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6000:f652:14ff:fe55:19f0"; };
                                            { "man1-3923.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6074:92e2:baff:fea1:78d2"; };
                                            { "man1-3978.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6074:92e2:baff:fea1:738e"; };
                                            { "man1-4518.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7d58"; };
                                            { "man1-4602.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601d:f652:14ff:fe8c:1e30"; };
                                            { "man1-4723.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6046:e61d:2dff:fe00:9860"; };
                                            { "man1-6606.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6048:e61d:2dff:fe01:f7f0"; };
                                            { "man1-6907.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6060:e61d:2dff:fe04:3490"; };
                                            { "man1-7122.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:38e0"; };
                                            { "man1-7234.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6066:e61d:2dff:fe04:23c0"; };
                                            { "man1-7335.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6067:e61d:2dff:fe6c:e610"; };
                                            { "man1-7368.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6069:e61d:2dff:fe6e:ea0"; };
                                            { "man1-7793.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:606e:e61d:2dff:fe6e:dd0"; };
                                            { "man1-7794.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:606e:e61d:2dff:fe6e:eb0"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "40ms";
                                            backend_timeout = "1s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 1;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                      refers = "requests_to_man";
                                    }; -- report
                                  }; -- sba_man
                                  sba_vla = {
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
                                            { "vla1-0046.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4f:0:604:5cf4:8caa"; };
                                            { "vla1-0089.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:2d:0:604:db7:9d36"; };
                                            { "vla1-0142.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a65b"; };
                                            { "vla1-0145.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a611"; };
                                            { "vla1-0149.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a77a"; };
                                            { "vla1-0169.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:13:0:604:db7:9a7c"; };
                                            { "vla1-0369.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3e:0:604:db6:17d7"; };
                                            { "vla1-0555.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:1b:0:604:db7:9986"; };
                                            { "vla1-0577.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:17:0:604:db7:992c"; };
                                            { "vla1-0656.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:13:0:604:db7:99a7"; };
                                            { "vla1-0832.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4e:0:604:db7:a1c5"; };
                                            { "vla1-0836.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:45:0:604:db7:a702"; };
                                            { "vla1-0848.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:48:0:604:db7:a208"; };
                                            { "vla1-0917.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:61:0:604:db7:9b0e"; };
                                            { "vla1-0924.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:71:0:604:db7:a408"; };
                                            { "vla1-0932.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:98:0:604:db7:a04f"; };
                                            { "vla1-0934.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:5e:0:604:db7:a180"; };
                                            { "vla1-0959.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:22:0:604:db7:9924"; };
                                            { "vla1-0994.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:42:0:604:db7:a4e6"; };
                                            { "vla1-2719.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:2f:0:604:db7:9fd3"; };
                                            { "vla1-4438.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:a0:0:604:db7:a683"; };
                                            { "vla1-4564.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:aa2e"; };
                                            { "vla1-4565.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:a938"; };
                                            { "vla1-4566.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:a7b6"; };
                                            { "vla1-4689.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4e:0:604:db7:a446"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "40ms";
                                            backend_timeout = "1s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 1;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                      refers = "requests_to_vla";
                                    }; -- report
                                  }; -- sba_vla
                                }; -- rr
                                on_error = {
                                  shared = {
                                    uuid = "4521384360058968294";
                                  }; -- shared
                                }; -- on_error
                              }; -- balancer2
                            }; -- report
                          }; -- shared
                        }; -- https_sbapiv4
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
                            shared = {
                              uuid = "default";
                              threshold = {
                                lo_bytes = 500;
                                hi_bytes = 1024;
                                recv_timeout = "1s";
                                pass_timeout = "9s";
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 3;
                                  rr = {
                                    weights_file = "./controls/traffic_control.weights";
                                    sba_sas = {
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
                                              { "sas1-0280.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:151:225:90ff:fe83:914"; };
                                              { "sas1-1492.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:179:215:b2ff:fea8:6cfc"; };
                                              { "sas1-1497.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:179:215:b2ff:fea8:cc6"; };
                                              { "sas1-1717.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:17a:feaa:14ff:fea9:7a68"; };
                                              { "sas1-2268.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:627:922b:34ff:fecf:3d70"; };
                                              { "sas1-2794.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:605:225:90ff:fe83:1a14"; };
                                              { "sas1-2819.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:60d:225:90ff:fe83:1662"; };
                                              { "sas1-3462.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe83:200"; };
                                              { "sas1-3613.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe88:b326"; };
                                              { "sas1-3899.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:634:225:90ff:fe88:cb82"; };
                                              { "sas1-4255.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63e:96de:80ff:fe8c:dd3a"; };
                                              { "sas1-4287.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:637:96de:80ff:fe8c:baf6"; };
                                              { "sas1-4288.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:638:96de:80ff:fe8c:dec4"; };
                                              { "sas1-4407.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:646:96de:80ff:fe8c:d864"; };
                                              { "sas1-4926.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:65c:96de:80ff:fe81:7f8"; };
                                              { "sas1-5329.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:1252"; };
                                              { "sas1-5336.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:11ca"; };
                                              { "sas1-5395.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:63d:96de:80ff:fe81:1896"; };
                                              { "sas1-5893.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:635:225:90ff:feec:2e24"; };
                                              { "sas1-5894.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:636:225:90ff:feec:2c62"; };
                                              { "sas1-5897.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:636:225:90ff:feec:3006"; };
                                              { "sas1-6326.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:11a:215:b2ff:fea7:7cb8"; };
                                              { "sas1-6637.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:119:215:b2ff:fea7:7a84"; };
                                              { "sas1-7823.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b67c"; };
                                              { "sas1-9603.search.yandex.net"; 14080; 150.000; "2a02:6b8:b000:199:215:b2ff:fea9:7642"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "40ms";
                                              backend_timeout = "60ms";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                        }; -- balancer2
                                        refers = "requests_to_sas";
                                      }; -- report
                                    }; -- sba_sas
                                    sba_man = {
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
                                              { "man1-0109.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6030:92e2:baff:fe6e:b964"; };
                                              { "man1-0139.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2a80"; };
                                              { "man1-0194.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:8094"; };
                                              { "man1-0347.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6036:92e2:baff:fe6f:7f08"; };
                                              { "man1-0404.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601e:92e2:baff:fe6f:7dd0"; };
                                              { "man1-0409.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600d:92e2:baff:fe74:795a"; };
                                              { "man1-0412.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6036:92e2:baff:fe74:7d60"; };
                                              { "man1-0730.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6034:92e2:baff:fe6e:bdf0"; };
                                              { "man1-0947.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600e:92e2:baff:fe55:f228"; };
                                              { "man1-0979.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f6a2"; };
                                              { "man1-1463.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:b350"; };
                                              { "man1-1501.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:d680"; };
                                              { "man1-1657.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601b:f652:14ff:fe8b:af10"; };
                                              { "man1-1762.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b750"; };
                                              { "man1-2941.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:eb20"; };
                                              { "man1-3232.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6000:f652:14ff:fe55:19f0"; };
                                              { "man1-3923.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6074:92e2:baff:fea1:78d2"; };
                                              { "man1-3978.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6074:92e2:baff:fea1:738e"; };
                                              { "man1-4518.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7d58"; };
                                              { "man1-4602.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:601d:f652:14ff:fe8c:1e30"; };
                                              { "man1-4723.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6046:e61d:2dff:fe00:9860"; };
                                              { "man1-6606.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6048:e61d:2dff:fe01:f7f0"; };
                                              { "man1-6907.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6060:e61d:2dff:fe04:3490"; };
                                              { "man1-7122.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:38e0"; };
                                              { "man1-7234.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6066:e61d:2dff:fe04:23c0"; };
                                              { "man1-7335.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6067:e61d:2dff:fe6c:e610"; };
                                              { "man1-7368.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:6069:e61d:2dff:fe6e:ea0"; };
                                              { "man1-7793.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:606e:e61d:2dff:fe6e:dd0"; };
                                              { "man1-7794.search.yandex.net"; 11900; 250.000; "2a02:6b8:b000:606e:e61d:2dff:fe6e:eb0"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "40ms";
                                              backend_timeout = "60ms";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                        }; -- balancer2
                                        refers = "requests_to_man";
                                      }; -- report
                                    }; -- sba_man
                                    sba_vla = {
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
                                              { "vla1-0046.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4f:0:604:5cf4:8caa"; };
                                              { "vla1-0089.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:2d:0:604:db7:9d36"; };
                                              { "vla1-0142.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a65b"; };
                                              { "vla1-0145.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a611"; };
                                              { "vla1-0149.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3f:0:604:db7:a77a"; };
                                              { "vla1-0169.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:13:0:604:db7:9a7c"; };
                                              { "vla1-0369.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:3e:0:604:db6:17d7"; };
                                              { "vla1-0555.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:1b:0:604:db7:9986"; };
                                              { "vla1-0577.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:17:0:604:db7:992c"; };
                                              { "vla1-0656.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:13:0:604:db7:99a7"; };
                                              { "vla1-0832.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4e:0:604:db7:a1c5"; };
                                              { "vla1-0836.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:45:0:604:db7:a702"; };
                                              { "vla1-0848.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:48:0:604:db7:a208"; };
                                              { "vla1-0917.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:61:0:604:db7:9b0e"; };
                                              { "vla1-0924.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:71:0:604:db7:a408"; };
                                              { "vla1-0932.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:98:0:604:db7:a04f"; };
                                              { "vla1-0934.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:5e:0:604:db7:a180"; };
                                              { "vla1-0959.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:22:0:604:db7:9924"; };
                                              { "vla1-0994.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:42:0:604:db7:a4e6"; };
                                              { "vla1-2719.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:2f:0:604:db7:9fd3"; };
                                              { "vla1-4438.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:a0:0:604:db7:a683"; };
                                              { "vla1-4564.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:aa2e"; };
                                              { "vla1-4565.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:a938"; };
                                              { "vla1-4566.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:77:0:604:db7:a7b6"; };
                                              { "vla1-4689.search.yandex.net"; 14080; 250.000; "2a02:6b8:c0e:4e:0:604:db7:a446"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "40ms";
                                              backend_timeout = "60ms";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                        }; -- balancer2
                                        refers = "requests_to_vla";
                                      }; -- report
                                    }; -- sba_vla
                                  }; -- rr
                                  on_error = {
                                    shared = {
                                      uuid = "4521384360058968294";
                                    }; -- shared
                                  }; -- on_error
                                }; -- balancer2
                              }; -- threshold
                            }; -- shared
                          }; -- report
                        }; -- default
                      }; -- regexp
                    }; -- response_headers
                  }; -- headers
                }; -- report
              }; -- accesslog
            }; -- http
          }; -- ssl_sni
        }; -- errorlog
      }; -- shared
    }; -- https_section_443
    https_section_15761 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        15761;
      }; -- ports
      shared = {
        uuid = "7784282916520349067";
      }; -- shared
    }; -- https_section_15761
  }; -- ipdispatch
}