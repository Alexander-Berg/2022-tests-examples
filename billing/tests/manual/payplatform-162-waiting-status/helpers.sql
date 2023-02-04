-- Файл содержит скрипты для ручного тестирования задачи PAYPLATFORM-162


-- проверить, нет ли задачи
select *
from bo.T_NIRVANA_MNCLOSE_SYNC
where task_id = 't-162';

-- создать задачу в NirvanaMNCloseSync
insert into BO.T_NIRVANA_MNCLOSE_SYNC(task_id, dt, status)
values ('t-162', to_date('2019-09-01', 'yyyy-mm-dd'), 'new_unopenable');
;

-- или сделать её new_unopenable, если задача уже есть
update bo.T_NIRVANA_MNCLOSE_SYNC
set status = 'new_unopenable'
where TASK_ID = 't-162';

-- получить блок, соответствующий кубику
select *
from bo.T_NIRVANA_BLOCK
where REQUEST like '%t-162%';

-- удалить блоки для этой задачи если нужно
delete
from bo.T_NIRVANA_BLOCK
where REQUEST like '%t-162%';

-- меняем статус на stalled
update bo.T_NIRVANA_MNCLOSE_SYNC
set STATUS = 'stalled'
where TASK_ID = 't-162';

-- меняем статус на opened
update bo.T_NIRVANA_MNCLOSE_SYNC
set STATUS = 'opened'
where TASK_ID = 't-162';

-- меняем статус на resolved
update bo.T_NIRVANA_MNCLOSE_SYNC
set STATUS = 'resolved'
where TASK_ID = 't-162';
