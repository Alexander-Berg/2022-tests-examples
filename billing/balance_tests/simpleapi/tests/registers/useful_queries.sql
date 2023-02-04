----- реестры, которые обрабатываются в тестах ----
SELECT *
FROM T_INCOMING_MAIL
WHERE MSG_PATH LIKE '%greed-ts1f'
      AND dt BETWEEN TIMESTAMP '2017-08-28 00:00:00' AND TIMESTAMP '2017-08-29 00:00:00'; --greed-tm1f

---- изменить период запуска разборщика ----
UPDATE bo.t_pycron_schedule
SET CRONTAB = '*/15 * * * *'
WHERE name = 'paysys-report-proc';

select * from bo.t_pycron_schedule
WHERE name = 'paysys-report-proc';

---- все платежи по реестру ----
SELECT p.id
FROM bo.t_payment p
  JOIN bo.t_payment_register pr ON p.register_id = pr.id
WHERE pr.incoming_mail_id = 233530;

---- pycron inserts for greed-tm1f ----
insert into bo.t_pycron_descr (name, command, timeout, description, total_count, terminate, owner_login)
values ('paysys-report-proc-tm', 'YANDEX_XML_CONFIG=/etc/yandex/balance-paysys/run_rep_proc.cfg.xml yb-python -pysupport paysys/run_rep_proc.py -t',
        3600, 'paysys-report-proc-tm', null, 0, 'vaclav');

---- разборщик ----
SELECT *
FROM bo.t_pycron_schedule
WHERE name = 'paysys-report-proc';

insert into bo.t_pycron_schedule (name, crontab, host, enabled)
values ('paysys-report-proc-tm', '*/5 * * * *', 'greed-tm1f', 1);

---- recent pycron job working time ----
SELECT
  l.name,
  s.host,
  (finished - started) * 24 * 60 * 60,
  exit_code
from bo.t_pycron_lock l
join bo.t_pycron_state s on l.id = s.id
where (l.name = 'paysys-report-proc' and s.host = 'greed-ts1f')
   or (l.name = 'paysys-report-proc-tm' and s.host = 'greed-tm1f');

---- в постпереналивочном скрипте ----
INSERT INTO t_incoming_mail (id, dt, msg_path, src, status)
  SELECT
    bo.s_incoming_mail_id.nextval                                                            AS id,
    sysdate                                                                                  AS dt,
    '/large/test_samples/' || regexp_substr(im.msg_path, '[A-Za-z0-9.]+[.]') || 'greed-tm1f' AS msg_path,
    im.src                                                                                   AS src,
    0                                                                                        AS status
  FROM bo.t_incoming_mail im
  WHERE im.dt BETWEEN TIMESTAMP '2016-10-28 00:00:00' AND TIMESTAMP '2016-10-29 00:00:00'
        AND exists(SELECT *
                   FROM bo.t_payment_register pr
                   WHERE pr.incoming_mail_id = im.id)
        AND status = 2;

INSERT INTO t_incoming_mail (id, dt, msg_path, src, status)
  SELECT
    bo.s_incoming_mail_id.nextval                                                            AS id,
    sysdate                                                                                  AS dt,
    '/large/test_samples/' || regexp_substr(im.msg_path, '[A-Za-z0-9.]+[.]') || 'greed-ts1f' AS msg_path,
    im.src                                                                                   AS src,
    0                                                                                        AS status
  FROM bo.t_incoming_mail im
  WHERE im.dt BETWEEN TIMESTAMP '2016-10-28 00:00:00' AND TIMESTAMP '2016-10-29 00:00:00'
        AND exists(SELECT *
                   FROM bo.t_payment_register pr
                   WHERE pr.incoming_mail_id = im.id)
        AND status = 2;
