 create  function xx_calc_spec_skv(p_amt in number, p_from_dt in date)
      return number parallel_enable
    as
        l_res   number := -1;
        l_M     number := 1000000;
    begin
        if      p_amt >= 40*l_M then l_res := (p_amt - 10*l_M)*.15;
        elsif   p_amt >= 10*l_M then l_res := (p_amt - 10*l_M)*.1;
        else l_res := 0;
        end if;
        return l_res;
    end;