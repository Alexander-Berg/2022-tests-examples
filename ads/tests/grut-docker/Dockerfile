FROM registry.yandex.net/yt/yt:stable

RUN apt-get update && apt-get install -y supervisor

COPY grut-admin /usr/bin/grut-admin
COPY grut-orm /usr/bin/grut_orm
COPY object_api /usr/bin/grut_object_api

COPY supervisord/supervisord.conf /etc/supervisor/supervisord.conf
COPY supervisord/*.sv.conf /etc/supervisor/conf.d/
COPY start_yt_local.sh /usr/bin/start_yt_local.sh
COPY init_db.sh /usr/bin/init_db.sh
COPY ports.py /tmp/ports.py
COPY run.sh /usr/bin/run.sh

COPY grut_orm/config.yson /etc/grut_orm/config.yson
COPY grut_object_api/config.yson /etc/grut_object_api/config.yson

# When grut db is initialized the flag file will be created by init_db.sh script
# Strictly speaking, it is not sufficient to say that object_api is ready to process requests right now
# but in practice db initialization is the most time consuming process and when db is initialized other services are ready too
HEALTHCHECK --interval=1s --timeout=3s --retries=300 \
  CMD cat /tmp/db_initialized || exit 1

ENTRYPOINT ["bash", "run.sh"]
