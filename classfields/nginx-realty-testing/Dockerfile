#
FROM registry.yandex.net/vertis-base/ubuntu:trusty

ARG VERSION

    # change hostname for postinst nginx script
RUN cp /bin/hostname{,.bkp} && echo "echo docker-13-sas.test.vertis.yandex.net" > /bin/hostname  \
    && apt-get update \
    # install packages
    && apt-get install -y nginx=1.12.1-5 nginx-common=1.12.1-5 nginx-full=1.12.1-5 openssl \
    && apt-get install -y yandex-vertis-config-nginx-front \ 
                          yandex-vertis-realty-rbc-www-nginx-static \
                          yandex-vertis-realty2-api-nginx-static \
                          yandex-vertis-realty2-www-nginx-static \
    && rm /etc/nginx/sites-enabled/default \

    # change server_name and map's
    # realty.test.vertis.yandex.ru -> docker-13-sas.test.vertis.yandex.ru
    # realty.test.vertis.yandex.net -> docker-13-sas.test.vertis.yandex.net
    && sed -i 's/realty.test.vertis.yandex.ru/docker-13-sas.test.vertis.yandex.ru/g'  \
               /etc/nginx/include/realty.yandex.ru/* \
               /etc/nginx/sites-enabled/* \

    && sed -i 's/realty.test.vertis.yandex.net/docker-13-sas.test.vertis.yandex.net/g'  \
               /etc/nginx/sites-enabled/* \

    # configure upstream's
    && sed -i 's/nodejs-01-myt.test.vertis.yandex.net/localhost/g' /etc/nginx/include/realty.yandex.ru/* \

    #remove not realty configs
    && find /etc/nginx/sites-enabled/ ! -name "*realty.yandex*"  -type f -delete \
    && rm /etc/nginx/sites-enabled/partner.realty.yandex.ru.conf \

    ## revert original hostname bin
    && mv /bin/hostname{.bkp,} \


	## rm after https://st.yandex-team.ru/VERTISADMIN-16917
	&& sed -i -e '1 s/^/include \/etc\/nginx\/include\/realty.yandex.ru\/map-cookie-dc.inc;\n/;' /etc/nginx/sites-enabled/realty.yandex.ru.conf


COPY etc/nginx /etc/nginx

CMD ["nginx", "-g", "daemon off;"]
