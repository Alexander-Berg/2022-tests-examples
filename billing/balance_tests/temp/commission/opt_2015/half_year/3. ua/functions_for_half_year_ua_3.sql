create or replace function xx_calc_ua_skv(p_amt in number, p_from_dt in date)
      return number parallel_enable
    as
        l_res   number;
        l_M     number := 1000000;
        l_K     number := 1000;
    begin
        -- первое полугодие
        if to_char(p_from_dt, 'MM-DD') in ('03-01','04-01') then
            if    p_amt >=     9*l_M then l_res := 763450 + (p_amt -    9*l_M)*0.105;
            elsif p_amt >=   4.5*l_M then l_res := 335950 + (p_amt -  4.5*l_M)*0.095;
            elsif p_amt >=  2.25*l_M then l_res := 144700 + (p_amt - 2.25*l_M)*0.085;
            elsif p_amt >= 1.125*l_M then l_res :=  60325 + (p_amt -1.125*l_M)*0.075;
            elsif p_amt >=   400*l_K then l_res :=  13200 + (p_amt -  400*l_K)*0.065;
            elsif p_amt >=   180*l_K then l_res :=          (p_amt -  180*l_K)*0.06;
            else l_res := 0;
            end if;
        -- второе полугодие
        elsif to_char(p_from_dt, 'MM-DD') = '09-01' then
            if    p_amt >=    11*l_M then l_res := 932800 + (p_amt -   11*l_M)*0.105;
            elsif p_amt >=   5.5*l_M then l_res := 410300 + (p_amt -  5.5*l_M)*0.095;
            elsif p_amt >=  2.75*l_M then l_res := 176550 + (p_amt - 2.75*l_M)*0.085;
            elsif p_amt >=   1.4*l_M then l_res :=  75300 + (p_amt -  1.4*l_M)*0.075;
            elsif p_amt >=   500*l_K then l_res :=  16800 + (p_amt -  500*l_K)*0.065;
            elsif p_amt >=   220*l_K then l_res :=          (p_amt -  220*l_K)*0.06;
            else l_res := 0;
            end if;
        else
            l_res := -1;
        end if;

        return l_res;
    end;
