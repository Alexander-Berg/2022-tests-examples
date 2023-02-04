upstream kiwimon-test-viewer-upstream {
server kiwimon01.search.yandex.net;
}

server { 
    server_name kiwimon-test.viewer.*;
    include includes/server-section-with-acl;

    location / {
         proxy_pass http://kiwimon-test-viewer-upstream;
         include includes/location-section;
    }
}
