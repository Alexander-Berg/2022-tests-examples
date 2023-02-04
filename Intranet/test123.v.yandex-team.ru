upstream test123-v-upstream {
server yandex.ru;
}

server { 
    server_name test123.v.*;
    include includes/server-section-with-acl;

    location / {
         proxy_pass http://test123-v-upstream;
         include includes/location-section;
    }
}
