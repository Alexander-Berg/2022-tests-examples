create function xx_calc_prof_skv(p_amt in number, p_from_dt in date)
      return number parallel_enable
    as
        l_res   number;
        l_M     number := 1000000;
    begin
        -- первое полугодие
        if to_char(p_from_dt, 'MM-DD') = '03-01' then
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
        -- второе полугодие
        elsif to_char(p_from_dt, 'MM-DD') = '09-01' then
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
        else
            l_res := -1;
        end if;

        return l_res;
    end;
    
    
    
    
    
    create function xx_calc_base_skv(p_amt in number, p_from_dt in date)
      return number parallel_enable
    as
        l_res   number;
        l_M     number := 1000000;
    begin
        -- первое полугодие
        -- в 2015 первое полугодие начинается в апреле.
        -- далее будет, как у всех, начинаться с 03-01.
        if to_char(p_from_dt, 'MM-DD') in ('03-01', '04-01') then
            -- (9200000-4000000)*0.03 + (21600000-9200000)*.04 +
            -- (48800000-21600000)*.05
            if    p_amt >= 48.8*l_M then l_res := 2012000 + (p_amt - 48.8*l_M)*0.06;
            elsif p_amt >= 21.6*l_M then l_res :=  652000 + (p_amt - 21.6*l_M)*0.05;
            elsif p_amt >=  9.2*l_M then l_res :=  156000 + (p_amt -  9.2*l_M)*0.04;
            elsif p_amt >=    4*l_M then l_res :=           (p_amt -    4*l_M)*0.03;
            else l_res := 0;
            end if;
        -- второе полугодие
        elsif to_char(p_from_dt, 'MM-DD') = '09-01' then
            -- (11.5-4.8)*1000000*0.03 + (27-11.5)*1000000*.04 +
            -- (61-27)*1000000*.05
            if    p_amt >= 61.0*l_M then l_res := 2521000 + (p_amt - 61.0*l_M)*0.06;
            elsif p_amt >= 27.0*l_M then l_res :=  821000 + (p_amt - 27.0*l_M)*0.05;
            elsif p_amt >= 11.5*l_M then l_res :=  201000 + (p_amt - 11.5*l_M)*0.04;
            elsif p_amt >=  4.8*l_M then l_res :=           (p_amt -  4.8*l_M)*0.03;
            else l_res := 0;
            end if;
        else
            l_res := -1;
        end if;

        return l_res;

    end;