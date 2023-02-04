--liquibase formatted sql

--changeset spirit-1984:fill_fiscal_storage context:local

insert all
  into ds.t_firm(inn, title, kpp, ogrn, agent, url, oebs_id, sono_initial, sono_destination, hidden) 
       values('7707613666', 'This firm has nothing obsolete', '770413666', '1237746713666',  1, 'yandex.ru', 1, '7704', '7704', 0)
  into ds.t_firm(inn, title, kpp, ogrn, agent, url, oebs_id, sono_initial, sono_destination, hidden) 
       values('7707613642', 'This firm has nearly end service life', '770413642', '1237746713642',  1, 'yandex.ru', 1, '7704', '7704', 0)
  into ds.t_firm(inn, title, kpp, ogrn, agent, url, oebs_id, sono_initial, sono_destination, hidden) 
       values('9907613642', 'This firm has an urgent replacement', '990413642', '1237746713642',  1, 'yandex.ru', 1, '7704', '7704', 0)
  into ds.t_whitespirit(url, version, last_active_dt) values ('http://ws:8080', '1.0.328', current_date)
SELECT * FROM dual;


insert into ds.t_fiscal_storage(id, serial_number, state, state_dt, update_dt, last_document_number, type_code, status, hidden)
       VALUES (s_fiscal_storage_id.nextval, '1111068902013666', 'fiscal', current_date, current_date, 1, 'fn-1', 'good', 0);
insert into ds.t_cash_register(id, serial_number, sw_version, state, state_dt, rack_number, hidden, update_dt, fiscal_storage_id,
                          type_code, whitespirit_url, oebs_address_code, open_shift_dt, shift_documents_count, ofd_queue_size, current_groups)
       values (s_cash_register_id.nextval, '3820034313666','3.5.30', 'OPEN_SHIFT', current_date, '6660666', 0, current_date, s_fiscal_storage_id.currval, 'rp_sistema_1fa', 'http://ws:8080', 'IVA>IVNIT', current_date, 13, 0, 'OEBS');
insert into ds.t_registration(id, start_dt, end_dt, state, state_dt, hidden, cash_register_id, fiscal_storage_id, firm_inn)
        values(s_registration_id.nextval, current_date, current_date, 'REGISTERED', current_date, 0, s_cash_register_id.currval, s_fiscal_storage_id.currval, '7707613666');


insert into ds.t_fiscal_storage(id, serial_number, state, state_dt, update_dt, last_document_number, type_code, status, hidden)
       VALUES (s_fiscal_storage_id.nextval, '1111068902013642', 'fiscal', current_date, current_date, 1, 'fn-1', 'nearly_end_service_life', 0);
insert into ds.t_cash_register(id, serial_number, sw_version, state, state_dt, rack_number, hidden, update_dt, fiscal_storage_id,
                          type_code, whitespirit_url, oebs_address_code, open_shift_dt, shift_documents_count, ofd_queue_size, current_groups)
       values (s_cash_register_id.nextval, '3820034313642','3.5.30', 'OPEN_SHIFT', current_date, '6660666', 0, current_date, s_fiscal_storage_id.currval, 'rp_sistema_1fa', 'http://ws:8080', 'IVA>IVNIT', current_date, 13, 0, 'OEBS');
insert into ds.t_registration(id, start_dt, end_dt, state, state_dt, hidden, cash_register_id, fiscal_storage_id, firm_inn)
        values(s_registration_id.nextval, current_date, current_date, 'REGISTERED', current_date, 0, s_cash_register_id.currval, s_fiscal_storage_id.currval, '7707613642');

insert into ds.t_fiscal_storage(id, serial_number, state, state_dt, update_dt, last_document_number, type_code, status, hidden)
       VALUES (s_fiscal_storage_id.nextval, '2222068902013642', 'fiscal', current_date, current_date, 1, 'fn-1', 'urgent_replacement', 0);
insert into ds.t_cash_register(id, serial_number, sw_version, state, state_dt, rack_number, hidden, update_dt, fiscal_storage_id,
                          type_code, whitespirit_url, oebs_address_code, open_shift_dt, shift_documents_count, ofd_queue_size, current_groups)
       values (s_cash_register_id.nextval, '5920034313642','3.5.30', 'OPEN_SHIFT', current_date, '6660666', 0, current_date, s_fiscal_storage_id.currval, 'rp_sistema_1fa', 'http://ws:8080', 'IVA>IVNIT', current_date, 13, 0, 'RZD');
insert into ds.t_registration(id, start_dt, end_dt, state, state_dt, hidden, cash_register_id, fiscal_storage_id, firm_inn)
        values(s_registration_id.nextval, current_date, current_date, 'REGISTERED', current_date, 0, s_cash_register_id.currval, s_fiscal_storage_id.currval, '9907613642');

