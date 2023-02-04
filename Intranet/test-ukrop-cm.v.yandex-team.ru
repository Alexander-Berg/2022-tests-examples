upstream test-ukrop-cm-v-upstream {
server tukrop000.search.yandex.net:3130;
}

server { 
    server_name test-ukrop-cm.v.*;
    include includes/server-section-with-acl;

    location / {
         proxy_pass http://test-ukrop-cm-v-upstream;
         include includes/location-section;
    }
}
