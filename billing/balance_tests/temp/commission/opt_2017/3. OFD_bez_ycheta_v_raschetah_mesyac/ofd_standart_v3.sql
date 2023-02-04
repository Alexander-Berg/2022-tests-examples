with

s_attrs_src as (
    select key_num   as value_num,
           contract_id                                         as contract_id,
           code,
           start_dt                                                as from_dt,
           nvl(
                lead(start_dt) over(partition by code, contract_id
                                     order by start_dt),
                add_months(trunc(sysdate, 'MM'), 11)
           ) - 1/24/60/60                                       as till_dt
      from XXXX_CONTRACT_SIGNED_ATTR
)
--select  *from s_attrs_src;
,

s_src as (  
   select  
      xxxx.contract_eid                                      as contract_eid,
      xxxx.contract_id                                       as contract_id,
		  xxxx.invoice_eid                                       as invoice_eid,
      xxxx.invoice_id                                        as invoice_id,
      xxxx.invoice_dt                                        as invoice_dt,
      trunc(xxxx.act_dt, 'MM')                               as from_dt,
       add_months(trunc(xxxx.act_dt, 'MM'), 1) - 1/84600      as till_dt,
		   xxxx.act_id                                                as act_id,
       xxxx.act_eid                                               as act_eid,
       xxxx.act_dt                                                as act_dt,
		   xxxx.kkm_id                                                as kkm_id,
       xxxx.kkm_eid                                                as kkm_eid
from   xxxx_new_comm_contract_basic  xxxx
-- join XXXX_EXTPROPS                       p  on p.classname = 'Order'
--                                                  and p.attrname = 'act_text'
--                                                  and p.object_id = xxxx.kkm_id
--                                                   -- BALANCE-25644
--                                                  and p.value_num is not null
                                                  
  join s_attrs_src                           ca_ct on ca_ct.contract_id = xxxx.contract_id
                                                  and ca_ct.code = 'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE'
                                                    -- bo.t_enums_tree.parent_id = 3000:
                                                    --  15: ОФД: Без уч. в расчетах
                                                  and ca_ct.value_num in (15)
                                                  and xxxx.act_dt between ca_ct.from_dt
                                                               and ca_ct.till_dt
                                                  and xxxx.kkm_eid is not null
      -- BALANCE-25648
      -- тип оплаты по договору — только предоплата (2)
      join s_attrs_src                              pt on pt.contract_id = xxxx.contract_id
                                                  and pt.code = 'PAYMENT_TYPE'
                                                  and pt.value_num = 2
                                                  and xxxx.act_dt between pt.from_dt
                                                               and pt.till_dt
		   where 
            xxxx.hidden <4 
		   and xxxx.act_dt >= date'2017-06-01'
       and xxxx.act_id  is not null -- чтобы не было пустых строк, которые ломают расчет, в реальном расчете такого нет
     
    )   

--select  * from s_src;
,
s_kv_src as (
  select d.*,
          -- кол-во активированных касс
          count( kkm_id)
          over(partition by contract_id
             -- BALANCE-25643: в каждом периоде — своё кол-во
             --                касс нарастающим итогом
                  order by from_dt)                      as kkm_cnt
   from (
            select d.*,
                   -- кол-во активированных касс (для шкалы)
                  
                   -- первый акт по заказу
                   first_value(act_id)
                   --https://st.yandex-team.ru/BALANCE-25732
                          over(partition by contract_id, kkm_id order by act_dt ) as first_act
              from s_src d
           ) d
           
           
     where act_id = first_act
     
)
--select  *from s_kv_src;

,

-- КВ с контролем, что оплат не более, чем актов
s_kv as (
    select contract_id, contract_eid,
           from_dt, till_dt, kkm_eid, kkm_id,
           act_id, act_eid, act_dt,
           case                          
            when kkm_cnt >= 8 then 1780
            when kkm_cnt >=  6 then 1653
            when kkm_cnt >=  4 then 1525
            when kkm_cnt >=  2 then 1398
            when kkm_cnt >=  1 then 1271
                                 else 0
           end                                  as reward
      from s_kv_src
),
-- результирующий запрос
s_opt_2017_ofd as (
select contract_id, contract_eid,
       from_dt, till_dt, kkm_eid as kkm, kkm_id,
       act_id, act_eid, act_dt,
       1            as reward_type,
       reward       as reward_to_charge,
       reward       as reward_to_pay
  from s_kv)

	
select s.contract_id,
       s.contract_eid,
       s.from_dt,
       s.till_dt,
       s.kkm,
       s.kkm_id,
       s.act_id,
       s.act_eid,
       s.act_dt,
       s.reward_type,
       s.reward_to_charge,
       s.reward_to_pay
  from (
    select contract_id, contract_eid,
           from_dt, till_dt,
           kkm,kkm_id,
           act_id, act_eid, act_dt,
           400 + reward_type as reward_type,
           reward_to_charge,
           reward_to_pay
      from s_opt_2017_ofd
     )       s
     order by contract_id, from_dt;