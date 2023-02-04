set hive.tez.container.size=6000;
set hive.execution.engine=tez;
set hive.cli.print.header=true;

select offer_id,
collect_set(offer_transaction_whole__in__rubles) as total_price_in_rubbles_set,
max(day) as last_day_exposition
from holocron.realty3_offers_2017_10_23
where
offer_offertypeint = 1 and
offer_categorytypeint = 2 and
offer_offerstate_error_1_errortypeint is null and
offer_offerstate_error_errortypeint is null and
offer_location_subjectfederationid = 1 and
(offer_apartmentinfo_flattypeint is  NULL or offer_apartmentinfo_flattypeint = 1 or offer_apartmentinfo_flattypeint = 3)
and (day = '2017-12-15' or day = '2018-01-08' or day = '2018-02-10')
group by offer_id


set hive.tez.container.size=6000;
set hive.execution.engine=tez;
set hive.cli.print.header=true;

select offer_id,
collect_set(offer_transaction_whole__in__rubles) as total_price_in_rubbles_set,
collect_list(offer_transaction_whole__in__rubles) as total_price_in_rubbles_set,
max(day) as last_day_exposition
from holocron.realty3_offers_2017_10_23
where
offer_offertypeint = 1 and
offer_categorytypeint = 2 and
offer_offerstate_error_1_errortypeint is null and
offer_offerstate_error_errortypeint is null and
offer_location_subjectfederationid = 1 and
(offer_apartmentinfo_flattypeint is  NULL or offer_apartmentinfo_flattypeint = 1 or offer_apartmentinfo_flattypeint = 3)
and (day = '2017-12-15' or day = '2018-01-08' or day = '2018-02-10')
group by offer_id

--
set hive.tez.container.size=6000;
set hive.execution.engine=tez;
set hive.cli.print.header=true;

select offer_id,
-- tested that it keeps order
collect_set(offer_transaction_whole__in__rubles) as total_price_in_rubbles_set,
max(day) as last_day_exposition
from
    (
    select offer_id,
        offer_transaction_whole__in__rubles,
        day
    from holocron.realty3_offers_2017_10_23
    where
        offer_offertypeint = 1 and
        offer_categorytypeint = 2 and
        offer_offerstate_error_1_errortypeint is null and
        offer_offerstate_error_errortypeint is null and
        offer_location_subjectfederationid = 1 and
        (offer_apartmentinfo_flattypeint is  NULL or offer_apartmentinfo_flattypeint = 1 or offer_apartmentinfo_flattypeint = 3)
        and (day = '2017-12-15' or day = '2018-01-08' or day = '2018-02-10')
    order by day desc
    ) t1
group by offer_id


-- use first_value and last_value
set hive.tez.container.size=6000;
set hive.execution.engine=tez;
set hive.cli.print.header=true;

select offer_id,  max(last_day_exposition) , max(last_price), max(first_price) from (
select offer_id,
-- tested that it keeps order
    last_value(day) over (partition by offer_id order by day asc) as last_day_exposition,
    last_value(offer_transaction_whole__in__rubles) over (partition by offer_id order by day asc) as last_price,
    first_value(offer_transaction_whole__in__rubles) over (partition by offer_id order by day asc) as first_price
from holocron.realty3_offers_2017_10_23
where
    offer_offertypeint = 1 and
    offer_categorytypeint = 2 and
    offer_offerstate_error_1_errortypeint is null and
    offer_offerstate_error_errortypeint is null and
    offer_location_subjectfederationid = 1 and
    (offer_apartmentinfo_flattypeint is  NULL or offer_apartmentinfo_flattypeint = 1 or offer_apartmentinfo_flattypeint = 3)
    and (day = '2017-12-15' or day = '2018-01-08' or day = '2018-02-10')) t
group by offer_id;


