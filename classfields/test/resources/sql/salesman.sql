CREATE TABLE `sales` (
                         `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
                         `new_client_id` int(11) NOT NULL,
                         `category_id` int(11) NOT NULL,
                         `section_id` int(11) NOT NULL,
                         `ya_region_id` int(11) NOT NULL,
                         `ya_city_id` int(11) NOT NULL,
                         `salon_id` int(11) NOT NULL,
                         `folder_id` int(11) NOT NULL,
                         `create_date` timestamp(3) NULL DEFAULT NULL,
                         `year` int(11) NOT NULL,
                         `price_RUR` bigint(11) NOT NULL,
                         `set_date` timestamp(3) NULL DEFAULT NULL,
                         PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `sales` (`id`, `new_client_id`, `category_id`, `section_id`, `ya_region_id`,
                     `ya_city_id`, `salon_id`, `folder_id`, `create_date`, `year`, `price_RUR`, `set_date`)
VALUES (1, 1, 1, 1, 1, 1, 1, 1, '2019-01-17T17:22:06.602', 2019, 100, '2019-01-17T17:22:06.602');


CREATE TABLE `sales_vin` (
                             `sale_id` int(11) NOT NULL,
                             `vin` varchar(64) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `sales_vin` (`sale_id`, `vin`)
VALUES (1, 'vin');


CREATE TABLE `products_apply_schedule` (
                                           `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
                                           `offer_id` varchar(64) NOT NULL DEFAULT '',
                                           `offer_category` varchar(24) NOT NULL DEFAULT '',
                                           `user` varchar(64) NOT NULL,
                                           `product` varchar(32) NOT NULL DEFAULT '',
                                           `schedule_type` enum('UNKNOWN','ONCE_AT_TIME','PERIODICAL','PERIODICAL_IN_DAY','PERIODICAL_SINCE_TIME','PERIODICAL_IN_RANGE') NOT NULL DEFAULT 'UNKNOWN',
                                           `weekdays` varchar(32) NOT NULL,
                                           `timezone` varchar(16) NOT NULL,
                                           `time` time DEFAULT NULL,
                                           `start_time` time DEFAULT NULL,
                                           `end_time` time DEFAULT NULL,
                                           `period_in_minutes` smallint(6) unsigned DEFAULT NULL,
                                           `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                                           `epoch` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                                           `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
                                           `is_visible` tinyint(1) NOT NULL DEFAULT '1',
                                           `expire_date` timestamp(3) NULL DEFAULT NULL,
                                           `custom_price` bigint(20) DEFAULT NULL,
                                           `dates` varchar(256) DEFAULT NULL,
                                           `allow_multiple_reschedule` tinyint(3) NOT NULL DEFAULT '1',
                                           `prev_schedule_id` int(11) unsigned DEFAULT NULL,
                                           PRIMARY KEY (`id`),
                                           KEY `offer_product` (`offer_id`,`product`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `products_apply_schedule` (`offer_id`, `user`, `weekdays`, `timezone`, `is_deleted`, `is_visible`, `epoch`)
VALUES
('1055684040-3df3', 'user:11913489', '1,2,3,4,5,6,7', '+03:00', 1, 1, '2019-01-17 17:22:06.602'),
('1064950674-e6e5629a', 'user:14704800', '1,2,3,4,5,6,7', '+03:00', 1, 1, '2019-01-22 10:17:18.412'),
('1066074070-efbf59c6', 'user:11913489', '1,2,3,4,5,6,7', '+03:00', 1, 1, '2019-01-21 15:49:05.327'),
('1063167088-7a0ade', 'user:27402862', '1,2,3,4,5,6,7', '+03:00', 0, 1, '2019-01-22 00:46:37.656'),
-- this schedule should be ignored by AutoApplyScheduleDao, because is_visible = 0
('1092103102-bcb2a9a9', 'dealer:20101', '1,2,3,4,5,6,7', '+03:00', 0, 0, '2019-10-23 00:46:37.656');