upstream morda-madm-test-viewer-upstream {
server v44.wdevx.yandex.net:1443;
}

server {
    server_name morda-madm-test.viewer.*;

    include includes/server-section-with-acl;

    location / {
         proxy_pass http://morda-madm-test-viewer-upstream;
         include includes/location-section;
    }
}
