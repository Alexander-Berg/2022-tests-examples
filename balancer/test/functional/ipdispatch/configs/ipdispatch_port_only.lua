instance = {
    addrs = {
        { ip = "localhost"; port = led_port; };
        { ip = "localhost"; port = zeppelin_port; };
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    thread_mode = thread_mode; set_no_file = false;

    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;
            http = {
                maxlen = 65536; maxreq = 65536;
                admin = {};
            }; -- http
        }; -- admin
        led = {
            ip = "localhost";
            port = led_port;
            http = {
                maxlen = 65536; maxreq = 65536;
                errordocument = {
                    status = 200;
                    content = "Led";
                }; -- errordocument
            }; -- http
        }; -- localhost1
        zeppelin = {
            port = zeppelin_port;
            http = {
                maxlen = 65536; maxreq = 65536;
                errordocument = {
                    status = 200;
                    content = "Zeppelin";
                }; -- errordocument
            }; -- http
        }; -- localhost2
    }; -- ipdispatch
}; -- instance

