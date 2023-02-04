# Пример конфига приложения для использования в тестах
config_sample = """<?xml version='1.0' encoding='utf8'?>
<Config xmlns:ns0="http://www.w3.org/2001/XInclude">
    <Type>dev</Type>
    <environment>development</environment>

    <Components>
    <Component id="ya_tvm_api">
        <URL>tvm-api.yandex.net</URL>
        <Timeout>10.0</Timeout>
    </Component>
    <Component id="yandex_direct">
        <URL>http://test-direct.yandex.ru:9000</URL>
    </Component>
</Components>

    <MailTemplates>/usr/share/yandex-balance-templates/mako/mail_templates</MailTemplates>

    <DaData>
        <Token>XXXXXX</Token>
    </DaData>

    <PyLog>
    <Logger name="sqlalchemy.engine">DEBUG</Logger>
    <Logger name="sqlalchemy.pool">ERROR</Logger>
    <Logger name="sqlalchemy.orm.unitofwork">ERROR</Logger>
    <Logger name="xmlrpc.dump">DEBUG</Logger>
    <Logger name="db.pool">DEBUG</Logger>
</PyLog>
    <DbBackends>
    <DbBackend id="balance" type="oracle">
        <Host>balance-dev.yandex.ru</Host>
        <User>bo</User>
        <Pass>XXXXXX</Pass>
    </DbBackend>
    <DbBackend id="balance_1" type="oracle">
        <Host>balance-dev.yandex.ru</Host>
        <User>bo</User>
        <Pass>XXXXX</Pass>
    </DbBackend>
</DbBackends>
    <DbBackends>
    <DbBackend id="meta_2" type="oracle">
        <Host>metadb_node2.yandex.ru</Host>
        <User>bo</User>
        <Pass>XXXX</Pass>
    </DbBackend>
    <DbBackend id="meta_ro" type="oracle">
        <Host>metadb_ro.yandex.ru</Host>
        <User>bo</User>
        <Pass>XXXXX</Pass>
    </DbBackend>
</DbBackends>
<DbBackends>
        <DbBackend id="yt_hahn" type="yt">
            <Proxy>
                <Url>hahn.yt.com</Url>
            </Proxy>
            <Token>YT_TOKEN</Token>
        </DbBackend>

        <DbBackend id="yt_freud" type="yt">
            <Proxy>
                <Url>freud.com</Url>
            </Proxy>
            <Token>YT_TOKEN</Token>
        </DbBackend>

        <DbBackend id="yql_hahn" type="yql">
            <Proxy>
                <Url>hahn.yql.com</Url>
            </Proxy>
            <Token>YQL_TOKEN</Token>
        </DbBackend>

        <DbBackend id="yql_freud" type="yql">
            <Proxy>
                <Url>freud.yql.com</Url>
            </Proxy>
            <Token>YQL_TOKEN</Token>
        </DbBackend>
</DbBackends>
    <PyLog>
        <LogFormat>%(asctime)s P%(process)-5s T%(thread)-2.0d %(levelname)-7s %(name)-15s: %(message)s</LogFormat>
    </PyLog>
    <Log level="debug">/var/log/yb/agency_rewards.log</Log>
    <BKDealPath>//home/adfox/deals_v2/deals_dict</BKDealPath>
    <BKDealNotificationPath>//home/direct/db/deal_notifications</BKDealNotificationPath>
    <MNClose>
        <Tasks>
            <Task id="private_deals">ar_import_private_deals</Task>
            <Task id="stats">ar_import_deal_stats</Task>
            <Task id="import_domain_stats">ar_import_domain_stats</Task>
        </Tasks>
    </MNClose>
    <LastMonthActsPath>//home/balance/test/yb-ar/acts/{date}</LastMonthActsPath>
    <YTExportTaskCmd>/usr/bin/dwh/run_with_env.sh</YTExportTaskCmd>
    <Celery>
        <Endpoint>sqs.yandex.net:8771</Endpoint>
        <AccessKey>ybar</AccessKey>
        <Queues>
            <Queue id="calculate">calc</Queue>
            <Queue id="alt">calc-alt</Queue>
        </Queues>
    </Celery>
    <Notifications>
        <Support>test-balance-notify@yandex-team.ru</Support>
        <Facts>test-balance-notify@yandex-team.ru</Facts>
    </Notifications>
    <DomainStat>
        <Source>//home/yabs/stat/tmp/BillingOrderDomains</Source>
        <Dest>//home/balance/{env}/yb-ar/domain-stats/{date}</Dest>
    </DomainStat>
    <Startrek>
        <Host>https://st-api.test.yandex-team.ru</Host>
        <Token>12kadskadjs283hajs12asacas</Token>
    </Startrek>
</Config>
"""

bunker_calc_sample = {
    'login': 'shorrty',
    'email': 'test-balance-notify@yandex-team.ru',
    'title': 'Проф, Директ по доменам',
    'pre_actions': [
        {
            'order': 1,
            'title': 'Выгрузка статистики в YT из БД Баланса',
            'type': 'db_to_yt',
            'query': "...",
            'path': '{agency_stats}',
            'columns': [
                {'name': 'agency_id', 'type': 'int64'},
                {'name': 'client_id', 'type': 'int64'},
                {'name': 'service_id', 'type': 'int64'},
                {'name': 'service_order_id', 'type': 'int64'},
                {'name': 'amt', 'type': 'double'},
            ],
        },
        {
            'order': 2,
            'title': 'Расчет грейда домена',
            'type': 'yql',
            'query': "select * from {domain_stats}",
            'path': '',
            'columns': [],
        },
    ],
    'query': 'select * from [{domain_grades}]',
    'path': '{agency_rewards}',
    'cluster': 'hahn',
    'scale': '1',
    'comm_type': ['direct'],
    'from_dt': '2019-02-01T00:00:00.000Z',
    'till_dt': '2020-02-29T00:00:00.000Z',
    'freq': 'm',
    'calendar': 'f',
    '__version': '21',
    'env': [
        {
            'name': 'acts1m_ago',
            'value': '//home/balance/{env}/yb-ar/acts/{calc_prev_dt}',
        },
        {
            'name': 'agency_rewards',
            'value': '//home/balance/{env}/yb-ar/rewards/{calc_name}/{calc_dt}',
        },
        {'name': 'agency_stats', 'value': '//home/balance/{env}/yb-ar/agency-stats/{calc_dt}'},
        {'name': 'domain_stats', 'value': '//home/balance/{env}/yb-ar/domain-stats/{calc_dt}'},
        {'name': 'domain_grades', 'value': '//home/balance/{env}/yb-ar/domain-grades/{calc_dt}'},
        {'name': 'domain_grades_report', 'value': '//home/balance/{env}/yb-ar/domain-grades-report/{calc_dt}'},
    ],
    "forecast_status": "enabled",
    "forecast_email": "shorrty@yandex-team.ru",
    "forecast_env": [{"env_key": "domain_grades", "columns": ["amt", "is_gray"]}],
    "forecast_dist": [{"month": "2018-07-01T00:00:00.000Z", "pct": 10}],
}
