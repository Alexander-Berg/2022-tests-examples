default_ranges = "1ms,4ms,7ms,11ms,17ms,26ms,39ms,58ms,87ms,131ms,197ms,296ms,444ms,666ms,1000ms,1500ms,2250ms,3375ms,5062ms,7593ms,11390ms,17085ms,30000ms,60000ms,150000ms"


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


instance = {
  buffer = 1;
  maxconn = 2;
  workers = 3;
  tcp_fastopen = 0;
  enable_reuse_port = true;
  private_address = "127.0.0.10";
  default_tcp_rst_on_error = true;
  events = {
    stats = "report";
  }; -- events
  dns_ttl = get_random_timedelta(300, 360, "ms");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", 13250, "/usr/local/www/logs");
  addrs = {
    {
      port = 16100;
      ip = "127.0.0.4";
    };
    {
      port = 13250;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 13250;
      ip = get_ip_by_iproute("v6");
    };
  }; -- addrs
  admin_addrs = {
    {
      ip = "::1";
      port = 13250;
    };
    {
      ip = "::1";
      port = 13251;
    };
    {
      ip = "::1";
      port = 13252;
    };
    {
      port = 13251;
      ip = "127.0.0.1";
    };
  }; -- admin_addrs
  ipdispatch = {
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
        just_storage = false;
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
    admin_0 = {
      ips = {
        "::1";
      }; -- ips
      ports = {
        13250;
      }; -- ports
      shared = {
        uuid = "5301909631393893223";
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
      }; -- shared
    }; -- admin_0
    admin_1 = {
      ips = {
        "::1";
      }; -- ips
      ports = {
        13251;
      }; -- ports
      shared = {
        uuid = "5301909631393893223";
      }; -- shared
    }; -- admin_1
    admin_2 = {
      ips = {
        "::1";
      }; -- ips
      ports = {
        13252;
      }; -- ports
      shared = {
        uuid = "5301909631393893223";
      }; -- shared
    }; -- admin_2
    admin_3 = {
      ips = {
        "127.0.0.1";
      }; -- ips
      ports = {
        13251;
      }; -- ports
      shared = {
        uuid = "5301909631393893223";
      }; -- shared
    }; -- admin_3
    addr_2 = {
      ips = {
        get_ip_by_iproute("v4");
      }; -- ips
      ports = {
        13250;
      }; -- ports
      shared = {
        uuid = "7934112671786495329";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 13250, "/usr/local/www/logs");
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            accesslog = {
              log = get_log_path("access_log", 13250, "/usr/local/www/logs");
              report = {
                uuid = "balancer";
                refers = "service_total";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                regexp = {
                  gobabygo = {
                    priority = 2;
                    match_fsm = {
                      host = "gobabygo\\.n\\.yandex-team\\.ru";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    report = {
                      uuid = "gobabygo";
                      ranges = get_str_var("default_ranges");
                      just_storage = false;
                      disable_robotness = true;
                      disable_sslness = true;
                      events = {
                        stats = "report";
                      }; -- events
                      headers = {
                        create_func = {
                          ["X-Real-Ip"] = "realip";
                        }; -- create_func
                        create_func_weak = {
                          ["X-Forwarded-For"] = "realip";
                          ["X-Req-Id"] = "reqid";
                          ["X-Scheme"] = "scheme";
                          ["X-Source-Port"] = "realport";
                        }; -- create_func_weak
                        hasher = {
                          mode = "text";
                          icookie = {
                            use_default_keys = true;
                            domains = ".yandex.ru,.yandex.tr";
                            trust_parent = false;
                            trust_children = false;
                            enable_set_cookie = true;
                            enable_decrypting = true;
                            decrypted_uid_header = "X-Yandex-ICookie";
                            error_header = "X-Yandex-ICookie-Error";
                            encrypted_header = "X-Yandex-ICookie-Encrypted";
                            report = {
                              uuid = "xxx";
                              ranges = get_str_var("default_ranges");
                              matcher_map = {
                                xxx = {
                                  match_fsm = {
                                    URI = "/(.*)/xxx";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                }; -- xxx
                              }; -- matcher_map
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              shared = {
                                uuid = "7261425686399453008";
                              }; -- shared
                            }; -- report
                          }; -- icookie
                        }; -- hasher
                      }; -- headers
                    }; -- report
                  }; -- gobabygo
                  gobabygo2 = {
                    priority = 1;
                    match_fsm = {
                      host = ".*\\.n\\.yandex-team\\.ru";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    report = {
                      ranges = get_str_var("default_ranges");
                      just_storage = false;
                      disable_robotness = true;
                      disable_sslness = true;
                      events = {
                        stats = "report";
                      }; -- events
                      headers = {
                        create_func = {
                          ["X-Real-Ip"] = "realip";
                        }; -- create_func
                        create_func_weak = {
                          ["X-Forwarded-For"] = "realip";
                          ["X-Req-Id"] = "reqid";
                          ["X-Scheme"] = "scheme";
                          ["X-Source-Port"] = "realport";
                        }; -- create_func_weak
                        hasher = {
                          mode = "text";
                          report = {
                            uuid = "xxx";
                            ranges = get_str_var("default_ranges");
                            matcher_map = {
                              xxx = {
                                match_fsm = {
                                  URI = "/(.*)/xxx";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                              }; -- xxx
                            }; -- matcher_map
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            shared = {
                              uuid = "7261425686399453008";
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
                                        attempts = 100;
                                        rr = {
                                          unpack(gen_proxy_backends({
                                            { "laas.yandex-team.ru"; 80; 1.000; "2a02:6b8:0:2502::2509:1234"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100s";
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
                                srcrwr_ext = {
                                  remove_prefix = "m";
                                  domains = "yp-c.yandex.net";
                                  balancer2 = {
                                    active_policy = {
                                      skip_attempts = 3;
                                      unique_policy = {};
                                    }; -- active_policy
                                    attempts = 1;
                                    hashing = {
                                      unpack(gen_proxy_backends({
                                        { "ws39-272.search.yandex.net"; 911; 1.000; "2a02:6b8:0:2502::2509:508a"; };
                                        { "ws39-386.search.yandex.net"; 911; 1.000; "2a02:6b8:0:2502::2509:50c3"; };
                                        { "ws39-438.search.yandex.net"; 911; 1.000; "2a02:6b8:0:2502::2509:50dd"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "150ms";
                                        backend_timeout = "5s";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 3;
                                        need_resolve = true;
                                      }))
                                    }; -- hashing
                                    on_error = {
                                      errordocument = {
                                        status = 504;
                                        force_conn_close = false;
                                      }; -- errordocument
                                    }; -- on_error
                                  }; -- balancer2
                                }; -- srcrwr_ext
                              }; -- geobase
                            }; -- shared
                          }; -- report
                        }; -- hasher
                      }; -- headers
                      refers = "gobabygo";
                    }; -- report
                  }; -- gobabygo2
                }; -- regexp
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- errorlog
      }; -- shared
    }; -- addr_2
    addr_1 = {
      ips = {
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        13250;
      }; -- ports
      shared = {
        uuid = "7934112671786495329";
      }; -- shared
    }; -- addr_1
  }; -- ipdispatch
}