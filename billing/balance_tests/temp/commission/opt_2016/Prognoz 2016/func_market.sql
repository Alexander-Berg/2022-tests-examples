 create function xx_calc_market_skv(p_amt in number, p_from_dt in date)
      return number parallel_enable
    as
        l_M     number := 1000000;
        l_res   number;
    begin
        if p_from_dt = date'2016-03-01' then
            if    p_amt >= 155.00*l_M then l_res := p_amt*0.04;
            elsif p_amt >=  67.50*l_M then l_res := p_amt*0.035;
            elsif p_amt >=  22.50*l_M then l_res := p_amt*0.03;
            elsif p_amt >=   9.00*l_M then l_res := p_amt*0.025;
            elsif p_amt >=   4.50*l_M then l_res := p_amt*0.02;
            elsif p_amt >=   2.25*l_M then l_res := p_amt*0.015;
            elsif p_amt >=   0.90*l_M then l_res := p_amt*0.01;
            elsif p_amt >=   0.45*l_M then l_res := p_amt*0.005;
            else l_res := 0;
            end if;
        elsif p_from_dt = date'2016-09-01' then
            if    p_amt >= 200.00*l_M then l_res := p_amt*0.04;
            elsif p_amt >=  82.50*l_M then l_res := p_amt*0.035;
            elsif p_amt >=  27.50*l_M then l_res := p_amt*0.03;
            elsif p_amt >=  11.00*l_M then l_res := p_amt*0.025;
            elsif p_amt >=   5.50*l_M then l_res := p_amt*0.02;
            elsif p_amt >=   2.75*l_M then l_res := p_amt*0.015;
            elsif p_amt >=   1.10*l_M then l_res := p_amt*0.01;
            elsif p_amt >=   0.55*l_M then l_res := p_amt*0.005;
            else l_res := 0;
            end if;
        else
            l_res := -1;
        end if;

        return l_res;
    end;