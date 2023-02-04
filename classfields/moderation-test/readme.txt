1. we prepared baseline model for regions
http://vgorovoy-01-sas.dev.vertis.yandex.net:8888/notebooks/projects/realty/scripts/price_estimator/models/prepareFinalModelSellRegions.ipynb#
2. copy models
# scp $vgorovoy:/home/vgorovoy/projects/realty/scripts/price_estimator/models/regions_sell_best_model.factors.pickle.dat .
# scp $vgorovoy:/home/vgorovoy/projects/realty/scripts/price_estimator/models/regions_sell_best_model.dat .

scp $vgorovoy:/home/vgorovoy/projects/realty/scripts/price_estimator/models/regions_sell_best_model.factors.pickle.dat /Users/vgorovoy/Projects/realty/scripts/price_estimator/price-estimator-rest-api/model/
scp $vgorovoy:/home/vgorovoy/projects/realty/scripts/price_estimator/models/regions_sell_best_model.dat /Users/vgorovoy/Projects/realty/scripts/price_estimator/price-estimator-rest-api/model/

3. copy houses df with price data
scp $vgorovoy:/home/vgorovoy/projects/realty/scripts/price_estimator/data/buildings/all.houses.with.sell.price.stat.tsv /Users/vgorovoy/Projects/realty/scripts/price_estimator/price-estimator-rest-api/data/


3. prepare moderaion export with ok and not ok data
3.1. fraud.data.tsv
select user_id, o.id as offer_id, address,
	offer_type, category,
	price, renovation,
	area_value as area,
	kitchen_space_value as kitchen_area, rooms, open_plan, studio
from  offer o, realty_offer r
where o.id = r.id and user_id in (657747662,
                                              635125077,
                                              561539102,
                                              654838904,
                                              654848724,
                                              561539100,
                                              413740270,
                                              411394380,
                                              411395799,
                                              650386938,
                                              654833424)
                                     and address is not NULL
                                     and category = 'APARTMENT'
                                     and offer_type = 'SELL'
                                     and price is not NULL;

5.select user_id, o.id as offer_id,
  country,
  region,
  district,
  sub_locality_name,
  non_admin_sub_locality,
  address,
  	offer_type, category,
  	price, renovation,
  	area_value, kitchen_space_value, rooms, open_plan, studio, create_time

  from  offer o, realty_offer r
  where o.id = r.id and status = 'active' and (create_time >= '2018-06-27' and create_time < '2018-06-28')
                                       and address is not NULL
                                       and category = 'APARTMENT'
                                       and offer_type = 'SELL'
                                       and price is not NULL;

5.1 better to take from r3_offers
select offer_id,
  unified_address,
  	type,
  	category,
  	price, renovation,
  	area, kitchen_area, rooms, open_plan, studio, creation_date
  from  realty3_offers
  where cluster_head = 1 and error_code is null and uid is not null
  and (creation_date >= '2018-06-27' and creation_date < '2018-06-28')
  and category = 2
  and type = 1;

5.2 all vos offers
select offer_id,
  unified_address,
  	type,
  	category,
  	price, renovation,
  	area, kitchen_area, rooms, open_plan, studio, creation_date
  from  realty3_offers
  where cluster_head = 1 and error_code is null and uid is not null
--  and (creation_date >= '2018-06-27' and creation_date < '2018-06-28')
  and category = 2
  and (flat_type = 1 or flat_type =3)
  and (subject_federation_id != 1)
  and (subject_federation_id != 10174)
  and type = 1;


6. run model and check results