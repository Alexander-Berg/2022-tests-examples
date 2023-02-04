default_ranges = "1ms,4ms,7ms,11ms,17ms,26ms,39ms,58ms,87ms,131ms,197ms,296ms,444ms,666ms,1000ms,1500ms,2250ms,3375ms,5062ms,7593ms,11390ms,17085ms,30000ms,60000ms,150000ms"


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
  workers = 1;
  buffer = 65536;
  maxconn = 5000;
  tcp_fastopen = 0;
  enable_reuse_port = true;
  private_address = "127.0.0.10";
  default_tcp_rst_on_error = false;
  events = {
    stats = "report";
  }; -- events
  dns_ttl = get_random_timedelta(600, 900, "s");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", 16100, "/place/db/www/logs");
  admin_addrs = {
    {
      port = 16100;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 16100;
    };
  }; -- admin_addrs
  addrs = {
    {
      port = 16100;
      ip = "127.0.0.4";
    };
    {
      port = 16100;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 16100;
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
        16100;
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
    local_ips_16100 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        16100;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 16100, "/place/db/www/logs");
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          accesslog = {
            log = get_log_path("access_log", 16100, "/place/db/www/logs");
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
              regexp_path = {
                led = {
                  priority = 3;
                  pattern = "/led/.*";
                  case_insensitive = true;
                  errordocument = {
                    status = 200;
                    content = "Led";
                    force_conn_close = false;
                  }; -- errordocument
                }; -- led
                zeppelin = {
                  priority = 2;
                  pattern = "/zeppelin/.*";
                  case_insensitive = false;
                  errordocument = {
                    status = 200;
                    content = "Zeppelin";
                    force_conn_close = false;
                  }; -- errordocument
                }; -- zeppelin
                default = {
                  errordocument = {
                    status = 200;
                    content = "Zeppelin";
                    force_conn_close = false;
                  }; -- errordocument
                }; -- default
              }; -- regexp_path
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- local_ips_16100
  }; -- ipdispatch
}