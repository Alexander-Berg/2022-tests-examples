@../deployment/sql/header.sql

declare
  user_name varchar2(100 char);
  schema_name varchar2(100 char);
  wrong_user exception;
begin
  select lower(user)
  into user_name
  from dual;

  select lower(sys_context('USERENV', 'CURRENT_SCHEMA'))
  into schema_name
  from dual;

  if user_name != 'cmp' or schema_name != 'cmp' then
    raise wrong_user;
  end if;
end;
/

create or replace procedure cmp.safe_drop (
  statement in varchar2,
  expected_code in number
)
as
begin
   execute immediate statement;
exception
   when others then
      if sqlcode != -expected_code then
         raise;
      end if;
end;
/

-- zmb

begin
  safe_drop('drop table market_orders', 942);
end;
/

create table cmp.market_orders as
select
  1 as campaign_id,
  11 as eleven,
  100 as sum_paid,
  0 as test_shop,
  date '2001-01-01' as start_date
from dual
;


-- omb

begin
  safe_drop('drop table t_market_consumption', 942);
  safe_drop('drop function market_consumption', 4043);
  safe_drop('drop type market_consumption_table', 4043);
  safe_drop('drop type market_consumption_row', 4043);
end;
/

create table cmp.t_market_consumption as
select
  1 as campaign_id,
  100 as summ
from dual
;

create type cmp.market_consumption_row as object (
  campaign_id number,
  summ number
);
/

create type cmp.market_consumption_table
  is table of cmp.market_consumption_row;
/

-- noinspection SqlUnused
create function
  cmp.market_consumption (on_date in date)
    return cmp.market_consumption_table
  as
    result cmp.market_consumption_table := cmp.market_consumption_table();
    cursor cursor_ is
      select * from cmp.t_market_consumption;
begin
  for row_ in cursor_ loop
    result.extend;
    result(result.last) := cmp.market_consumption_row(row_.campaign_id, row_.summ);
  end loop;
  return result;
end;
/


-- zkmb

begin
  safe_drop('drop table cs_billing_orders', 942);
end;
/

create table cmp.cs_billing_orders as
select
  1 as campaign_id,
  112 as cs_id,
  100 as sum_paid,
  date '2001-01-01' as start_date
from dual
;

-- okmb

begin
  safe_drop('drop table t_billing_consumption', 942);
  safe_drop('drop function cs_billing_consumption', 4043);
  safe_drop('drop type cs_billing_consumption_table', 4043);
  safe_drop('drop type cs_billing_consumption_row', 4043);
end;
/

create table cmp.t_billing_consumption as
select
  111 + 1 as cs_id,
  1 as campaign_id,
  1 * 100 as sum
from dual
;

create type cmp.cs_billing_consumption_row as object (
  cs_id number,
  campaign_id number,
  sum number
);
/

create type cmp.cs_billing_consumption_table
  is table of cmp.cs_billing_consumption_row;
/

create function
  cmp.cs_billing_consumption (on_date in date)
    return cmp.cs_billing_consumption_table
  as
    result cmp.cs_billing_consumption_table := cmp.cs_billing_consumption_table();
    cursor cursor_ is
      select * from cmp.t_billing_consumption;
begin
  for row_ in cursor_ loop
    result.extend;
    result(result.last) := cmp.cs_billing_consumption_row(
      row_.cs_id, row_.campaign_id, row_.sum
    );
  end loop;
  return result;
end;
/

@../deployment/sql/footer.sql
