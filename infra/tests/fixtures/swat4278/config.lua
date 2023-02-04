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


function get_port_var(name, offset, default)
  value = get_int_var(name, default)
  if value == nil then
    error("Neither port variable \"" .. name .. "\" nor default port is specified.")
  end
  if value < 0 or value > 65535 then
    error("Variable \"" .. name .. "\" is not a valid port: " .. value)
  end
  if offset ~= nil then
    value = value + offset
  end
  return value
end


function get_random_timedelta(start, end_, unit)
  return math.random(start, end_) .. unit;
end


instance = {
  workers = 0;
  maxconn = 4000;
  buffer = 1048576;
  tcp_fastopen = 0;
  enable_reuse_port = true;
  private_address = "127.0.0.10";
  default_tcp_rst_on_error = true;
  events = {
    stats = "report";
  }; -- events
  dns_ttl = get_random_timedelta(600, 900, "s");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", get_port_var("admin_port"), "/place/db/www/logs/");
  admin_addrs = {
    {
      ip = "127.0.0.1";
      port = get_port_var("admin_port");
    };
  }; -- admin_addrs
  addrs = {
    {
      ip = "2a02:6b8:0:3400::1:1";
      port = get_port_var("port");
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "2a02:6b8:0:3400::1:2";
      port = get_port_var("port");
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "2a02:6b8:0:3400::1:3";
      port = get_port_var("port");
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "2a02:6b8:0:3400::1:4";
      port = get_port_var("port");
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "2a02:6b8:0:3400::1:5";
      port = get_port_var("port");
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "2a02:6b8:0:3400::1:6";
      port = get_port_var("port");
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "2a02:6b8:0:3400::1:7";
      port = get_port_var("port");
      disabled = get_int_var("disable_external", 0);
    };
  }; -- addrs
  ipdispatch = {
    admin = {
      ips = {
        "127.0.0.1";
      }; -- ips
      ports = {
        get_port_var("admin_port");
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
    section_1 = {
      ips = {
        "2a02:6b8:0:3400::1:1";
      }; -- ips
      ports = {
        get_port_var("port");
      }; -- ports
      regexp = {
        muse = {
          priority = 2;
          match_fsm = {
            URI = "/muse(/.*)?";
            case_insensitive = true;
            surround = false;
          }; -- match_fsm
          errordocument = {
            status = 200;
            force_conn_close = false;
          }; -- errordocument
        }; -- muse
        nirvana = {
          priority = 1;
          match_fsm = {
            URI = "/nirvana(/.*)?";
            case_insensitive = true;
            surround = false;
          }; -- match_fsm
          errordocument = {
            status = 200;
            force_conn_close = false;
          }; -- errordocument
        }; -- nirvana
      }; -- regexp
    }; -- section_1
    section_2 = {
      ips = {
        "2a02:6b8:0:3400::1:2";
      }; -- ips
      ports = {
        get_port_var("port");
      }; -- ports
      regexp = {
        beatles = {
          priority = 2;
          match_fsm = {
            URI = "/beatles(/.*)?";
            case_insensitive = true;
            surround = false;
          }; -- match_fsm
          errordocument = {
            status = 200;
            force_conn_close = false;
          }; -- errordocument
        }; -- beatles
        libertines = {
          priority = 1;
          match_fsm = {
            URI = "/libertines(/.*)?";
            case_insensitive = true;
            surround = false;
          }; -- match_fsm
          errordocument = {
            status = 200;
            force_conn_close = false;
          }; -- errordocument
        }; -- libertines
      }; -- regexp
    }; -- section_2
    section_3 = {
      ips = {
        "2a02:6b8:0:3400::1:3";
      }; -- ips
      ports = {
        get_port_var("port");
      }; -- ports
      shared = {
        uuid = "8625809092784230567";
        regexp = {
          beatles = {
            priority = 4;
            match_fsm = {
              URI = "/beatles(/.*)?";
              case_insensitive = true;
              surround = false;
            }; -- match_fsm
            errordocument = {
              status = 200;
              force_conn_close = false;
            }; -- errordocument
          }; -- beatles
          libertines = {
            priority = 3;
            match_fsm = {
              URI = "/libertines(/.*)?";
              case_insensitive = true;
              surround = false;
            }; -- match_fsm
            errordocument = {
              status = 200;
              force_conn_close = false;
            }; -- errordocument
          }; -- libertines
          muse = {
            priority = 2;
            match_fsm = {
              URI = "/muse(/.*)?";
              case_insensitive = true;
              surround = false;
            }; -- match_fsm
            errordocument = {
              status = 200;
              force_conn_close = false;
            }; -- errordocument
          }; -- muse
          nirvana = {
            priority = 1;
            match_fsm = {
              URI = "/nirvana(/.*)?";
              case_insensitive = true;
              surround = false;
            }; -- match_fsm
            errordocument = {
              status = 200;
              force_conn_close = false;
            }; -- errordocument
          }; -- nirvana
        }; -- regexp
      }; -- shared
    }; -- section_3
    section_4 = {
      ips = {
        "2a02:6b8:0:3400::1:4";
      }; -- ips
      ports = {
        get_port_var("port");
      }; -- ports
      regexp = {
        nirvana = {
          priority = 4;
          match_fsm = {
            URI = "/nirvana(/.*)?";
            case_insensitive = true;
            surround = false;
          }; -- match_fsm
          errordocument = {
            status = 200;
            force_conn_close = false;
          }; -- errordocument
        }; -- nirvana
        muse = {
          priority = 3;
          match_fsm = {
            URI = "/muse(/.*)?";
            case_insensitive = true;
            surround = false;
          }; -- match_fsm
          errordocument = {
            status = 200;
            force_conn_close = false;
          }; -- errordocument
        }; -- muse
        libertines = {
          priority = 2;
          match_fsm = {
            URI = "/libertines(/.*)?";
            case_insensitive = true;
            surround = false;
          }; -- match_fsm
          errordocument = {
            status = 200;
            force_conn_close = false;
          }; -- errordocument
        }; -- libertines
        beatles = {
          priority = 1;
          match_fsm = {
            URI = "/beatles(/.*)?";
            case_insensitive = true;
            surround = false;
          }; -- match_fsm
          errordocument = {
            status = 200;
            force_conn_close = false;
          }; -- errordocument
        }; -- beatles
      }; -- regexp
    }; -- section_4
    section_5 = {
      ips = {
        "2a02:6b8:0:3400::1:5";
      }; -- ips
      ports = {
        get_port_var("port");
      }; -- ports
      shared = {
        uuid = "8625809092784230567";
      }; -- shared
    }; -- section_5
    section_6 = {
      ips = {
        "2a02:6b8:0:3400::1:6";
      }; -- ips
      ports = {
        get_port_var("port");
      }; -- ports
      regexp = {
        beatles = {
          priority = 1;
          match_fsm = {
            URI = "/beatles(/.*)?";
            case_insensitive = true;
            surround = false;
          }; -- match_fsm
          errordocument = {
            status = 200;
            force_conn_close = false;
          }; -- errordocument
        }; -- beatles
      }; -- regexp
    }; -- section_6
    section_7 = {
      ips = {
        "2a02:6b8:0:3400::1:7";
      }; -- ips
      ports = {
        get_port_var("port");
      }; -- ports
      regexp = {
        muse = {
          priority = 4;
          match_fsm = {
            URI = "/muse(/.*)?";
            case_insensitive = true;
            surround = false;
          }; -- match_fsm
          errordocument = {
            status = 200;
            force_conn_close = false;
          }; -- errordocument
        }; -- muse
        beatles = {
          priority = 3;
          match_fsm = {
            URI = "/beatles(/.*)?";
            case_insensitive = true;
            surround = false;
          }; -- match_fsm
          errordocument = {
            status = 200;
            force_conn_close = false;
          }; -- errordocument
        }; -- beatles
        libertines = {
          priority = 2;
          match_fsm = {
            URI = "/libertines(/.*)?";
            case_insensitive = true;
            surround = false;
          }; -- match_fsm
          errordocument = {
            status = 200;
            force_conn_close = false;
          }; -- errordocument
        }; -- libertines
        nirvana = {
          priority = 1;
          match_fsm = {
            URI = "/nirvana(/.*)?";
            case_insensitive = true;
            surround = false;
          }; -- match_fsm
          errordocument = {
            status = 200;
            force_conn_close = false;
          }; -- errordocument
        }; -- nirvana
      }; -- regexp
    }; -- section_7
  }; -- ipdispatch
}