
  CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."V_AR_CONSOLIDATIONS_HY" ("CONTRACT_ID", "CONTRACT_EID", "AGENCY_ID", "COMMISSION_TYPE", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "START_DT", "FINISH_DT", "SIGN_DT", "LINKED_CONTRACT_ID", "LINKED_AGENCY_ID", "CONS_TYPE") AS 
  with
s_hy AS (
    -- ���������� ������ ��������� ����������� � �������� ����.
    -- ��� ���, ��������, 2019-01-01 �� ������ �������� ��������� 2018-09-01 -- 2019-02-28
    -- ������� ���������� ��������� ���. ���� - �����������
    select d.dt from_dt, add_months(d.dt, 6) - 1/24/60/60 as till_dt
      from (
         select add_months(add_months(trunc(sysdate, 'Y'), -10), 6*(level-1)) as dt
           from dual
        connect by level <= 4
           ) d
)
, s_consolidate_cls_src as (
    select cp.contract2_id                          as contract_id,
           cp.collateral_id,
           cp.cl_dt                                 as start_dt,
           fd.value_dt                              as finish_dt,
           nvl(s.is_signed, s.is_faxed)             as sign_dt
      from bo.v_contract_signed_attr_history     cp
      join bo.v_contract_signed_attr_history     fd on fd.code = 'CONSOLIDATION_FINISH_DT'
                                                 and fd.collateral_id = cp.collateral_id
      join bo.t_contract_collateral             s on s.id = cp.collateral_id
      -- ����������� ������ �� �������������� ����������, ������� ����������� ��
      -- ��������� ���� ��������� �������.
      -- BALANCE-28305: ���� ��������� ������� �� ����������� (CONSOLIDATION_FINISH_DT)
      -- �������� ��� ������ (� ���������� ����). ��������� ������� ����, �����
      -- �������� �� ��������� � �������� ���������
    join s_hy                                   q on q.till_dt between cp.cl_dt
                                                                   and trunc(fd.value_dt) + 1 - 1/24/60/60
                                                  -- ������� ������ ������� ���������
                                                 and trunc(sysdate, 'MM')-1 between q.from_dt and q.till_dt
     where cp.code = 'REWARD_CONSOLIDATION_PERIOD'
        -- ������ ������������ - ���������
       and cp.value_num = 3
)
, s_consolidations as (
    select *
      from (
        select d.*,
               max(d.sign_dt) over (partition by d.contract_id) as sign_dt_last
          from s_consolidate_cls_src     d
           ) d
        -- ������������ ��������� ����������� �������������� ���������� �
        -- ������������ ����� ���������� ��������������� ���������� ���������� ��
        -- ����, ������ ���� ���������� � �������� ������ ��� ���
     where sign_dt = sign_dt_last
)
-- ������� �������� ����������� ���������
, s_attrs_src as (
    select value_num,
           update_dt,
           contract2_id                                         as contract_id,
           code,
           cl_dt                                                as from_dt,
           nvl(
            lead(cl_dt) over(partition by code, contract2_id
                                 order by stamp),
            add_months(trunc(sysdate, 'MM'), 3)
           ) -1/24/60/60                                        as till_dt
      from bo.mv_contract_signed_attr_hist
)
-- ����������� �������� �� ����� ����. ������
, s_attrs as (
    select *
      from s_attrs_src
        -- BALANCE-25145: ������� �������� �������� �� ����� ����.������
     where trunc(sysdate, 'MM') - 1 between from_dt and till_dt
)
select cp.contract_id,
       -- ����� �������� ��� �������� ��������:
       -- ��-��, ��� ������, ����� ������� ������ �������
       c.external_id    as contract_eid,
       c.client_id      as agency_id,
       rt.value_num     as commission_type,
       fr.dt            as contract_from_dt,
       tl.value_dt      as contract_till_dt,
       cp.start_dt,
       cp.finish_dt,
       cp.sign_dt,
       lc.key_num       as linked_contract_id,
       cl.client_id     as linked_agency_id,
       -- 1 - ���������������
       -- 2 - �� �������
       ct.value_num     as cons_type
  from s_consolidations                    cp
  join bo.mv_contract_signed_attr_hist     lc on lc.code = 'LINKED_REWARD_CONTRACTS'
                                             and lc.collateral_id = cp.collateral_id
                                             and lc.value_num = 1
  join bo.mv_contract_signed_attr_hist     ct on ct.code = 'REWARD_CONSOLIDATION_TYPE'
                                             and ct.collateral_id = cp.collateral_id
  join bo.t_contract2                      c  on c.id = cp.contract_id
  join bo.t_contract2                      cl on cl.id = lc.key_num
  join s_attrs                             rt on rt.contract_id = c.id
                                             and rt.code = 'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE'
  -- ������ ��������
  join bo.t_contract_collateral            fr on fr.contract2_id = c.id
                                             and fr.num is null
  -- ����� ��������
  join bo.mv_contract_signed_attr          tl on tl.contract_id = c.id
                                             and tl.code = 'FINISH_DT';
