create or replace function xxx_test_commission_type(
                p_comm_type     in number,
                p_discount_type in number)
      return number parallel_enable as
    begin
        if (p_comm_type in (2, 8) and p_discount_type in (1, 2, 3, 7, 11, 12)) or
           (p_comm_type = 1 and p_discount_type in (1, 2, 3, 7, 12, 29)) or
           (p_comm_type = 3 and p_discount_type in (17)) or
           (p_comm_type = 4 and p_discount_type in (19)) or
           (p_comm_type = 5 and p_discount_type in (12)) or
           (p_comm_type = 6 and p_discount_type in (1, 2, 3, 7, 12)) or
           (p_comm_type = 7 and p_discount_type in (28))
        then
            return 1;
        else
            return 0;
        end if;
    end xxx_test_commission_type;