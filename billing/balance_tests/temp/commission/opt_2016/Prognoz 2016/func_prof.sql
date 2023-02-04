create function xx_calc_prof_skv(p_amt in number, p_from_dt in date)
      return number parallel_enable
    as
        l_res   number;
        l_M     number := 1000000;
    begin
        -- первое полугодие 2015
        if to_char(p_from_dt, 'YYYY-MM-DD') = '2015-03-01' then
            -- (26-11)*1000000*0.04 + (60-26)*1000000*0.05 +
            -- (115-60)*1000000*0.06 + (320-115)*1000000*0.07 +
            -- (610-320)*1000000*0.08
            if    p_amt >= 610*l_M then l_res := 43150000 + (p_amt - 610*l_M)*0.09;
            elsif p_amt >= 320*l_M then l_res := 19950000 + (p_amt - 320*l_M)*0.08;
            elsif p_amt >= 115*l_M then l_res :=  5600000 + (p_amt - 115*l_M)*0.07;
            elsif p_amt >=  60*l_M then l_res :=  2300000 + (p_amt -  60*l_M)*0.06;
            elsif p_amt >=  26*l_M then l_res :=   600000 + (p_amt -  26*l_M)*0.05;
            elsif p_amt >=  11*l_M then l_res :=            (p_amt -  11*l_M)*0.04;
            else l_res := 0;
            end if;
        -- второе полугодие 2015
        elsif to_char(p_from_dt, 'YYYY-MM-DD') = '2015-09-01' then
            -- (28-12)*1000000*0.04 + (62-28)*1000000*0.05 +
            -- (125-62)*1000000*0.06 + (340-125)*1000000*0.07 +
            -- (650-340)*1000000*0.08
            if    p_amt >= 650*l_M then l_res := 45970000 + (p_amt - 650*l_M)*0.09;
            elsif p_amt >= 340*l_M then l_res := 21170000 + (p_amt - 340*l_M)*0.08;
            elsif p_amt >= 125*l_M then l_res :=  6120000 + (p_amt - 125*l_M)*0.07;
            elsif p_amt >=  62*l_M then l_res :=  2340000 + (p_amt -  62*l_M)*0.06;
            elsif p_amt >=  28*l_M then l_res :=   640000 + (p_amt -  28*l_M)*0.05;
            elsif p_amt >=  12*l_M then l_res :=            (p_amt -  12*l_M)*0.04;
            else l_res := 0;
            end if;
        -- BALANCE-22387
        -- https://wiki.yandex-team.ru/Balance/TZ/Opt2016/#polugodovajapremija
        -- первое полугодие 2016
        elsif to_char(p_from_dt, 'YYYY-MM-DD') = '2016-03-01' then
            if    p_amt >= 823.50*l_M then l_res := 58657500 + (p_amt - 823.50*l_M)*0.09;
            elsif p_amt >= 416.00*l_M then l_res := 26057500 + (p_amt - 416.00*l_M)*0.08;
            elsif p_amt >= 143.75*l_M then l_res :=  7000000 + (p_amt - 143.75*l_M)*0.07;
            elsif p_amt >=  75.00*l_M then l_res :=  2875000 + (p_amt -  75.00*l_M)*0.06;
            elsif p_amt >=  32.50*l_M then l_res :=   750000 + (p_amt -  32.50*l_M)*0.05;
            elsif p_amt >=  13.75*l_M then l_res :=            (p_amt -  13.75*l_M)*0.04;
            else l_res := 0;
            end if;
        -- второе полугодие 2016
        elsif to_char(p_from_dt, 'YYYY-MM-DD') = '2016-09-01' then
            if    p_amt >= 877.50*l_M then l_res := 62492500 + (p_amt - 877.50*l_M)*0.09;
            elsif p_amt >= 442.00*l_M then l_res := 27652500 + (p_amt - 442.00*l_M)*0.08;
            elsif p_amt >= 156.25*l_M then l_res :=  7650000 + (p_amt - 156.25*l_M)*0.07;
            elsif p_amt >=  77.50*l_M then l_res :=  2925000 + (p_amt -  77.50*l_M)*0.06;
            elsif p_amt >=  35.00*l_M then l_res :=   800000 + (p_amt -  35.00*l_M)*0.05;
            elsif p_amt >=  15.00*l_M then l_res :=            (p_amt -  15.00*l_M)*0.04;
            else l_res := 0;
            end if;
        else
            l_res := -1;
        end if;

        return l_res;
    end;