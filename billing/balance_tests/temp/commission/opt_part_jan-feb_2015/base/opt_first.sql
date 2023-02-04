  with
s_pre as (
    select opt.contract_id,
           opt.contract_eid,
           opt.from_dt,
           opt.till_dt,
           opt.nds,
           opt.currency,
           opt.discount_type,
           200 + opt.reward_type                                        as reward_type,
           -- � ����������
           opt.turnover_to_charge   - nvl(com.turnover_to_charge, 0)    as turnover_to_charge,
           opt.reward_to_charge     - nvl(com.reward_to_charge, 0)      as reward_to_charge,
           opt.delkredere_to_charge - nvl(com.delkredere_to_charge, 0)  as delkredere_to_charge,
           opt.dkv_to_charge        - nvl(com.dkv_to_charge, 0)         as dkv_to_charge,
           -- � ������������
           opt.turnover_to_pay_w_nds - nvl(com.turnover_to_pay_w_nds,0 ) as turnover_to_pay_w_nds,
           opt.turnover_to_pay       - nvl(com.turnover_to_pay, 0)      as turnover_to_pay,
           opt.reward_to_pay     - nvl(com.reward_to_pay, 0)            as reward_to_pay,
           opt.delkredere_to_pay - nvl(com.delkredere_to_pay, 0)        as delkredere_to_pay,
           opt.dkv_to_pay        - nvl(com.dkv_to_pay, 0)               as dkv_to_pay
      from bo.v_comm_old_base_src               opt
      left outer
        -- �������, ��� ���� ��������� � ���.�����
        -- ����� �������
      join bo.v_comm_base_src     com on com.contract_id = opt.contract_id
                                                   and com.from_dt = opt.from_dt
                                                   and com.till_dt = opt.till_dt
                                                   and com.nds = opt.nds
                                                   and com.currency = opt.currency
                                                   and com.reward_type = opt.reward_type
                                                   and nvl(com.discount_type, -1) = nvl(opt.discount_type, -1)
--                                                   and opt.contract_id = 46673
        -- BALANCE-19554: �������� �� ������� ������� ������ �� ���������,
        -- ���� �������� 2015-{���,���}. ��� �����:
            -- * �� �� ���, ���
            -- * ������ �� ������� (�� ������, ���� �� �������)
            -- * ��� �� ���������
     where date'2015-01-01' between opt.from_dt and opt.till_dt
        or date'2015-02-01' between opt.from_dt and opt.till_dt
        or date'2015-03-01' between opt.from_dt and opt.till_dt
--        and opt.contract_id = 46673

)

select * from s_pre;
select contract_id,     contract_eid,
       date'2015-01-01'                         as from_dt,
       date'2015-04-01' - 1/24/60/60            as till_dt,
       nds,             currency,
       discount_type,
       reward_type,
       -- � ����������
       sum(turnover_to_charge)                  as turnover_to_charge,
       sum(reward_to_charge)                    as reward_to_charge,
       sum(delkredere_to_charge)                as delkredere_to_charge,
       sum(dkv_to_charge)                       as dkv_to_charge,
       -- � ������������
       sum(turnover_to_pay_w_nds)               as turnover_to_pay_w_nds,
       sum(turnover_to_pay)                     as turnover_to_pay,
       sum(reward_to_pay)                       as reward_to_pay,
       sum(delkredere_to_pay)                   as delkredere_to_pay,
       sum(dkv_to_pay)                          as dkv_to_pay
  from s_pre
 group by contract_id, contract_eid,
          nds, currency, discount_type, reward_type
          order by  contract_id  , from_dt, discount_type, reward_type, currency,nds,turnover_to_charge, reward_to_charge, turnover_to_pay,reward_to_pay;
