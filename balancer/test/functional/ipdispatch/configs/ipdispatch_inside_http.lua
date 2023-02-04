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

    http = {
        maxlen = 65536; maxreq = 65536;
        ipdispatch = {
            admin = {
                ip = "localhost";
                port = admin_port;
                admin = {};
            }; -- admin
            led = {
                ip = "localhost";
                port = led_port;
                errordocument = {
                    status = 200;
                    content = "Led";
                }; -- errordocument
            }; -- localhost1
        }; -- ipdispatch
    }; -- http
}; -- instance
