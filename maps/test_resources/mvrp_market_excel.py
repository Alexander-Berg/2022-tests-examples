import pytest


def mvrp_market_result_excel():
    return {
        'Result Routes': [{
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 0, 'location_id': '888891180',
            'location_ref': 'depot-91180', 'location_title': '', 'type': 'depot', 'multi_order': False, 'transit_distance_m': 0, 'transit_duration': '00:00:00',
            'arrival_time': '09:00:00', 'time_window': '08:30:00-23:59:00', 'is_first_in_route': True, 'is_last_in_route': False, 'stop_number': 0, 'arrival_time_s': 32400,
            'transit_duration_s': 0, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962973, 'lon': 40.947555, 'service_duration_s': 300,
            'shared_service_duration_s': 0, 'total_service_duration_s': 300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:00:00', 'total_service_duration': '00:05:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 1, 'location_id': '57637247',
            'location_ref': 'LO-74061347', 'location_title': 'г. Иваново, проспект Ленина, д. 23, стр. , к. ', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 6655,
            'transit_duration': '00:18:44', 'arrival_time': '09:23:44', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 1,
            'arrival_time_s': 33824, 'transit_duration_s': 1124, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.005309, 'lon': 40.972663, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 2, 'location_id': '58098844',
            'location_ref': '116220188', 'location_title': 'г. Иваново, улица Громобоя, д. 15', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1199,
            'transit_duration': '00:04:05', 'arrival_time': '09:37:49', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 2,
            'arrival_time_s': 34669, 'transit_duration_s': 245, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.011092, 'lon': 40.977415, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 3, 'location_id': '57341525',
            'location_ref': '115239628', 'location_title': 'г. Иваново, Комсомольская улица, д. 17', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 699,
            'transit_duration': '00:02:55', 'arrival_time': '09:50:44', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 3,
            'arrival_time_s': 35444, 'transit_duration_s': 175, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.008293, 'lon': 40.97631, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 4, 'location_id': 'm_57439953_57941164',
            'location_ref': '115367829_116008801', 'location_title': 'г. Иваново, улица Калинина, д. 5', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 484,
            'transit_duration': '00:01:47', 'arrival_time': '10:02:31', 'time_window': '10:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 4,
            'arrival_time_s': 36151, 'transit_duration_s': 107, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.006756, 'lon': 40.979595, 'service_duration_s': 360,
            'shared_service_duration_s': 400, 'total_service_duration_s': 760, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:06:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:12:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 5,
            'location_id': 'm_57098203_57130189_57251334_57426559_57426918_57436592_57437016_57459588_57463951_57464228_57473048',
            'location_ref': '114910800_114955697_115119063_115349731_115349735_115363904_115363905_115390728_115398092_115398093_115409517',
            'location_title': 'г. Иваново, проспект Ленина, д. 108', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1796, 'transit_duration': '00:05:25',
            'arrival_time': '10:20:36', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 5, 'arrival_time_s': 37236,
            'transit_duration_s': 325, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.017315, 'lon': 40.97023, 'service_duration_s': 900,
            'shared_service_duration_s': 400, 'total_service_duration_s': 1300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:15:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:21:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 6, 'location_id': '57436783',
            'location_ref': '115363275', 'location_title': 'г. Иваново, улица Ермака, д. 11', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 741,
            'transit_duration': '00:02:20', 'arrival_time': '10:44:36', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 6,
            'arrival_time_s': 38676, 'transit_duration_s': 140, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.017693, 'lon': 40.96448, 'service_duration_s': 300,
            'shared_service_duration_s': 400, 'total_service_duration_s': 700, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 7, 'location_id': '57718668',
            'location_ref': '115732755', 'location_title': 'г. Иваново, 1-я Минеевская улица, д. 4', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2524,
            'transit_duration': '00:06:20', 'arrival_time': '11:02:36', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 7,
            'arrival_time_s': 39756, 'transit_duration_s': 380, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.032812, 'lon': 40.968306, 'service_duration_s': 300,
            'shared_service_duration_s': 400, 'total_service_duration_s': 700, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 8,
            'location_id': 'm_57097795_57545507_57576284_57582754_57601047_57614422_57619498_57620505_57792771_57799279_57821929_57847938_57912314_57917217_57918118_57961782_58016241_58018881_58023199_58028156_58033140_58055629_58064757_58096594_58104161_58108789_58125578',
            'location_ref': '114911272_115506024_115547762_115555801_115578892_115596486_115603948_115603969_115811534_115820458_115850108_115883243_115969592_115975829_115977598_116036496_116108585_116111736_116118253_116124358_116131228_116161013_116174284_116217275_116226336_116233725_116256056',
            'location_title': 'г. Иваново, улица Карла Маркса, д. 8', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2604, 'transit_duration': '00:06:40',
            'arrival_time': '11:20:56', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 8, 'arrival_time_s': 40856,
            'transit_duration_s': 400, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.016096, 'lon': 40.972564, 'service_duration_s': 1860,
            'shared_service_duration_s': 400, 'total_service_duration_s': 2260, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:31:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:37:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 9, 'location_id': 'm_57929666_58044674',
            'location_ref': '115993753_116147039', 'location_title': 'г. Иваново, улица Громобоя, д. 11а', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1196,
            'transit_duration': '00:03:31', 'arrival_time': '12:02:07', 'time_window': '08:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 9,
            'arrival_time_s': 43327, 'transit_duration_s': 211, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.010857, 'lon': 40.974118, 'service_duration_s': 360,
            'shared_service_duration_s': 400, 'total_service_duration_s': 760, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:06:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:12:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 10, 'location_id': '58126493',
            'location_ref': '116257240', 'location_title': 'г. Иваново, 10-я Минеевская улица, д. 30', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 4897,
            'transit_duration': '00:11:42', 'arrival_time': '12:26:29', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 10,
            'arrival_time_s': 44789, 'transit_duration_s': 702, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.046146, 'lon': 40.955685, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 11, 'location_id': '57793652',
            'location_ref': '115813450', 'location_title': 'г. Иваново, Октябрьская улица, д. 3/70', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 4066,
            'transit_duration': '00:09:28', 'arrival_time': '12:45:57', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 11,
            'arrival_time_s': 45957, 'transit_duration_s': 568, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.014958, 'lon': 40.973031, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 12,
            'location_id': 'm_57281334_57305922_57449350_58044398_58074749_58127145', 'location_ref': '115159212_115191737_115378774_116146073_116187477_116256762',
            'location_title': 'г. Иваново, Революционная улица, д. 24, к. 1', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 4295, 'transit_duration': '00:10:14',
            'arrival_time': '13:06:11', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 12, 'arrival_time_s': 47171,
            'transit_duration_s': 614, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.033654, 'lon': 40.918585, 'service_duration_s': 600,
            'shared_service_duration_s': 400, 'total_service_duration_s': 1000, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:10:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:16:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 13, 'location_id': '58122757',
            'location_ref': '116251477', 'location_title': 'г. Иваново, Революционная улица, д. 24к3', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 74,
            'transit_duration': '00:00:21', 'arrival_time': '13:23:12', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 13,
            'arrival_time_s': 48192, 'transit_duration_s': 21, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.034026, 'lon': 40.919312, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 14,
            'location_id': 'm_57709515_58030981_58036050_58048449', 'location_ref': '115720546_116128417_116130542_116151607',
            'location_title': 'г. Иваново, Дюковская улица, д. 38А', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 692, 'transit_duration': '00:02:19',
            'arrival_time': '13:35:31', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 14, 'arrival_time_s': 48931,
            'transit_duration_s': 139, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.032194, 'lon': 40.92525, 'service_duration_s': 480,
            'shared_service_duration_s': 400, 'total_service_duration_s': 880, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:08:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:14:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 15, 'location_id': '58038177',
            'location_ref': '116138130', 'location_title': 'г. Иваново, улица Дзержинского, д. 13', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3726,
            'transit_duration': '00:07:58', 'arrival_time': '13:58:09', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 15,
            'arrival_time_s': 50289, 'transit_duration_s': 478, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.015547, 'lon': 40.96562, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 16, 'location_id': '57292901',
            'location_ref': '115174324', 'location_title': 'г. Иваново, улица Громобоя, д. 18', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1115,
            'transit_duration': '00:04:06', 'arrival_time': '14:12:15', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 16,
            'arrival_time_s': 51135, 'transit_duration_s': 246, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.010553, 'lon': 40.976373, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 17, 'location_id': '57522985',
            'location_ref': '115477923', 'location_title': 'г. Иваново, Дюковская улица, д. 25', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 5315,
            'transit_duration': '00:13:14', 'arrival_time': '14:35:29', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 17,
            'arrival_time_s': 52529, 'transit_duration_s': 794, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.034702, 'lon': 40.924594, 'service_duration_s': 1100,
            'shared_service_duration_s': 400, 'total_service_duration_s': 1500, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:18:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:25:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 18, 'location_id': '57682836',
            'location_ref': '115687496', 'location_title': 'г. Иваново, улица Люлина, д. 6', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 5470,
            'transit_duration': '00:13:15', 'arrival_time': '15:13:44', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 18,
            'arrival_time_s': 54824, 'transit_duration_s': 795, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.029568, 'lon': 40.977334, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 19, 'location_id': '58051058',
            'location_ref': '116155733', 'location_title': 'г. Иваново, улица Калашникова, д. 26', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 899,
            'transit_duration': '00:03:44', 'arrival_time': '15:27:28', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 19,
            'arrival_time_s': 55648, 'transit_duration_s': 224, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.026874, 'lon': 40.968009, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 20, 'location_id': '57876039',
            'location_ref': '115920992', 'location_title': 'г. Иваново, 2-я Парковская улица, д. 81', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 4664,
            'transit_duration': '00:09:25', 'arrival_time': '15:46:53', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 20,
            'arrival_time_s': 56813, 'transit_duration_s': 565, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.048927, 'lon': 40.929759, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 21, 'location_id': '57893925',
            'location_ref': '115945554', 'location_title': 'г. Иваново, 5-я Кубанская улица, д. 56', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2024,
            'transit_duration': '00:04:51', 'arrival_time': '16:01:44', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 21,
            'arrival_time_s': 57704, 'transit_duration_s': 291, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.045176, 'lon': 40.952954, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 22, 'location_id': '57892853',
            'location_ref': '115944173', 'location_title': 'г. Иваново, 2-я Петрозаводская улица, д. 1а', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 351,
            'transit_duration': '00:01:49', 'arrival_time': '16:13:33', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 22,
            'arrival_time_s': 58413, 'transit_duration_s': 109, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.045269, 'lon': 40.947986, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 23, 'location_id': '56976267',
            'location_ref': 'LO-73362883', 'location_title': 'г. Иваново, улица Академика Мальцева, д. 4, стр. , к.', 'type': 'delivery', 'multi_order': False,
            'transit_distance_m': 3891, 'transit_duration': '00:09:14', 'arrival_time': '16:32:47', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False,
            'is_last_in_route': False, 'stop_number': 23, 'arrival_time_s': 59567, 'transit_duration_s': 554, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.019613,
            'lon': 40.967327, 'service_duration_s': 200, 'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0,
            'waiting_duration': '00:00:00', 'service_duration': '00:03:20', 'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00',
            'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 24, 'location_id': 'm_57919353_57919807',
            'location_ref': '115979427_115979428', 'location_title': 'г. Иваново, Авдотьинская улица, д. 30', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3825,
            'transit_duration': '00:08:51', 'arrival_time': '16:51:38', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 24,
            'arrival_time_s': 60698, 'transit_duration_s': 531, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.029368, 'lon': 40.914694, 'service_duration_s': 280,
            'shared_service_duration_s': 400, 'total_service_duration_s': 680, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:04:40',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:20', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 25, 'location_id': '58071904',
            'location_ref': '116183483', 'location_title': 'г. Иваново, Левобережная улица, д. 12', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 895,
            'transit_duration': '00:03:31', 'arrival_time': '17:06:29', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 25,
            'arrival_time_s': 61589, 'transit_duration_s': 211, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.02636, 'lon': 40.9256, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 26, 'location_id': '57705499',
            'location_ref': '115715880', 'location_title': 'г. Иваново, Революционная, д. 24к4', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1323,
            'transit_duration': '00:04:25', 'arrival_time': '17:20:54', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 26,
            'arrival_time_s': 62454, 'transit_duration_s': 265, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.034785, 'lon': 40.918126, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 27, 'location_id': 'm_57905893_57906156',
            'location_ref': '115962229_115962228', 'location_title': 'г. Иваново, улица Тимирязева, д. 1', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3401,
            'transit_duration': '00:07:30', 'arrival_time': '17:38:24', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 27,
            'arrival_time_s': 63504, 'transit_duration_s': 450, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.01672, 'lon': 40.955379, 'service_duration_s': 280,
            'shared_service_duration_s': 400, 'total_service_duration_s': 680, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:04:40',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:20', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 28, 'location_id': '58069332',
            'location_ref': '116180391', 'location_title': 'г. Иваново, улица Тимирязева, д. 39', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1066,
            'transit_duration': '00:04:35', 'arrival_time': '17:54:19', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 28,
            'arrival_time_s': 64459, 'transit_duration_s': 275, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.023322, 'lon': 40.961551, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 29, 'location_id': '57114523',
            'location_ref': '114933602', 'location_title': 'г. Иваново, Зубчатая улица, д. 21', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 855,
            'transit_duration': '00:03:07', 'arrival_time': '18:07:26', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 29,
            'arrival_time_s': 65246, 'transit_duration_s': 187, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.025733, 'lon': 40.95493, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 30, 'location_id': '58062934',
            'location_ref': '116171257', 'location_title': 'г. Иваново, улица Юрия Гагарина, д. 15', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 962,
            'transit_duration': '00:03:15', 'arrival_time': '18:20:41', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 30,
            'arrival_time_s': 66041, 'transit_duration_s': 195, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.024047, 'lon': 40.961463, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 31050, 'vehicle_ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'route_number': 0, 'location_in_route': 31, 'location_id': '888891180',
            'location_ref': 'depot-91180', 'location_title': '', 'type': 'depot', 'multi_order': False, 'transit_distance_m': 7904, 'transit_duration': '00:21:30',
            'arrival_time': '18:52:11', 'time_window': '08:30:00-23:59:00', 'is_first_in_route': False, 'is_last_in_route': True, 'stop_number': 31, 'arrival_time_s': 67931,
            'transit_duration_s': 1290, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962973, 'lon': 40.947555, 'service_duration_s': 300,
            'shared_service_duration_s': 0, 'total_service_duration_s': 300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:00:00', 'total_service_duration': '00:05:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 0, 'location_id': '888891180',
            'location_ref': 'depot-91180', 'location_title': '', 'type': 'depot', 'multi_order': False, 'transit_distance_m': 0, 'transit_duration': '00:00:00',
            'arrival_time': '09:00:00', 'time_window': '08:30:00-23:59:00', 'is_first_in_route': True, 'is_last_in_route': False, 'stop_number': 0, 'arrival_time_s': 32400,
            'transit_duration_s': 0, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962973, 'lon': 40.947555, 'service_duration_s': 300,
            'shared_service_duration_s': 0, 'total_service_duration_s': 300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:00:00', 'total_service_duration': '00:05:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 1, 'location_id': '57601463',
            'location_ref': '115579509', 'location_title': 'г. Кинешма, Социалистическая улица, д. 35/2', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 107127,
            'transit_duration': '01:35:58', 'arrival_time': '10:40:58', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 1,
            'arrival_time_s': 38458, 'transit_duration_s': 5758, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.476013, 'lon': 42.093446, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 2, 'location_id': '57328527',
            'location_ref': '115221354', 'location_title': 'г. Ивановская область, городской округ Кинешма, Лесозаводская улица, д. 23 в', 'type': 'delivery', 'multi_order': False,
            'transit_distance_m': 3657, 'transit_duration': '00:08:34', 'arrival_time': '10:59:32', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False,
            'is_last_in_route': False, 'stop_number': 2, 'arrival_time_s': 39572, 'transit_duration_s': 514, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.456046,
            'lon': 42.127861, 'service_duration_s': 200, 'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0,
            'waiting_duration': '00:00:00', 'service_duration': '00:03:20', 'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00',
            'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 3, 'location_id': '57911244',
            'location_ref': '115967942', 'location_title': 'г. Кинешма, улица Куйбышева, д. 5', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 4766,
            'transit_duration': '00:10:39', 'arrival_time': '11:20:11', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 3,
            'arrival_time_s': 40811, 'transit_duration_s': 639, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.463133, 'lon': 42.086996, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 4, 'location_id': '57543926',
            'location_ref': 'LO-73963757', 'location_title': 'г. Кинешма, улица Желябова, д. 5, стр. , к. ', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2722,
            'transit_duration': '00:05:41', 'arrival_time': '11:35:52', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 4,
            'arrival_time_s': 41752, 'transit_duration_s': 341, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.464155, 'lon': 42.108502, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 5, 'location_id': '56944075',
            'location_ref': '114702030', 'location_title': 'г. Кинешма, улица Желябова, д. 5', 'type': 'delivery', 'multi_order': True, 'transit_distance_m': 0,
            'transit_duration': '00:00:00', 'arrival_time': '11:35:52', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 4,
            'arrival_time_s': 41752, 'transit_duration_s': 0, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.464155, 'lon': 42.108502, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 800, 'service_waiting_duration_s': 600, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:13:20', 'service_waiting_duration': '00:10:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 6,
            'location_id': 'm_57117516_57117631_57661387_57661465', 'location_ref': '114937433_114937432_115658455_115658454',
            'location_title': 'г. Кинешма, улица Рубинского, д. 20', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2169, 'transit_duration': '00:04:37',
            'arrival_time': '11:53:49', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 5, 'arrival_time_s': 42829,
            'transit_duration_s': 277, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.457222, 'lon': 42.090823, 'service_duration_s': 560,
            'shared_service_duration_s': 400, 'total_service_duration_s': 960, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:09:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:16:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 7,
            'location_id': 'm_57096387_57096550_57096802', 'location_ref': '114908909_114908908_114908910', 'location_title': 'г. Кинешма, улица Гагарина, д. 10', 'type': 'delivery',
            'multi_order': False, 'transit_distance_m': 1600, 'transit_duration': '00:03:58', 'arrival_time': '12:13:47', 'time_window': '09:00:00-22:00:00',
            'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 6, 'arrival_time_s': 44027, 'transit_duration_s': 238, 'waiting_duration_s': 0,
            'location_description': '', 'lat': 57.459207, 'lon': 42.102204, 'service_duration_s': 420, 'shared_service_duration_s': 400, 'total_service_duration_s': 820,
            'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:07:00', 'shared_service_duration': '00:06:40',
            'total_service_duration': '00:13:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 8, 'location_id': '57826317',
            'location_ref': '115855715', 'location_title': 'г. Кинешма, Красноветкинская улица, д. 1', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1888,
            'transit_duration': '00:06:43', 'arrival_time': '12:34:10', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 7,
            'arrival_time_s': 45250, 'transit_duration_s': 403, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.456796, 'lon': 42.123117, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 9, 'location_id': '57666461',
            'location_ref': '115665385', 'location_title': 'г. Кинешма, улица имени Юрия Горохова, д. 12', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2725,
            'transit_duration': '00:09:40', 'arrival_time': '12:53:50', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 8,
            'arrival_time_s': 46430, 'transit_duration_s': 580, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.449557, 'lon': 42.094829, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 10, 'location_id': 'm_57156024_57244270',
            'location_ref': '114989756_115109082', 'location_title': 'г. Кинешма, улица Гагарина, д. 2В', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1993,
            'transit_duration': '00:05:57', 'arrival_time': '13:09:47', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 9,
            'arrival_time_s': 47387, 'transit_duration_s': 357, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.46155, 'lon': 42.110262, 'service_duration_s': 280,
            'shared_service_duration_s': 400, 'total_service_duration_s': 680, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:04:40',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:20', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 11, 'location_id': 'm_57004465_57500843',
            'location_ref': '114783648_115447039', 'location_title': 'г. Кинешма, микрорайон Автоагрегат улица Щорса, д. 1Б', 'type': 'delivery', 'multi_order': False,
            'transit_distance_m': 3824, 'transit_duration': '00:08:32', 'arrival_time': '13:29:39', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False,
            'is_last_in_route': False, 'stop_number': 10, 'arrival_time_s': 48579, 'transit_duration_s': 512, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.443643,
            'lon': 42.108295, 'service_duration_s': 280, 'shared_service_duration_s': 400, 'total_service_duration_s': 680, 'service_waiting_duration_s': 0,
            'waiting_duration': '00:00:00', 'service_duration': '00:04:40', 'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:20',
            'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 12,
            'location_id': 'm_55956782_56396376_56833394_57062642_57063674_57065015_57638772_57680377_57680483_57858074_57863485_57938133_57989913',
            'location_ref': '113370314_113965427_114552462_114861805_114864577_114865138_115628264_115684606_115684607_115897671_115904943_116004757_116072629',
            'location_title': 'г. Кинешма, Красноветкинская улица, д. 15', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3731, 'transit_duration': '00:08:51',
            'arrival_time': '13:49:50', 'time_window': '11:00:00-15:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 11, 'arrival_time_s': 49790,
            'transit_duration_s': 531, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.460365, 'lon': 42.117539, 'service_duration_s': 1020,
            'shared_service_duration_s': 400, 'total_service_duration_s': 1420, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:17:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:23:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 13, 'location_id': '58064374',
            'location_ref': '116173506', 'location_title': 'г. Кинешма, улица Декабристов, д. 17А', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1878,
            'transit_duration': '00:04:29', 'arrival_time': '14:17:59', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 12,
            'arrival_time_s': 51479, 'transit_duration_s': 269, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.451785, 'lon': 42.121015, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 14,
            'location_id': 'm_55992371_56167044_56402000_56738380_56760651_56823513_56823720_57030165_57617997_57618553_57659667_57721582_57858653_57902633',
            'location_ref': '113419107_113656157_113973499_114425316_114453399_114538535_114538536_114819072_115599829_115599828_115655674_115736676_115897682_115956405',
            'location_title': 'г. Кинешма, улица Правды, д. 14/9', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 710, 'transit_duration': '00:02:49',
            'arrival_time': '14:30:48', 'time_window': '11:00:00-15:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 13, 'arrival_time_s': 52248,
            'transit_duration_s': 169, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.454768, 'lon': 42.119524, 'service_duration_s': 1080,
            'shared_service_duration_s': 400, 'total_service_duration_s': 1480, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:18:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:24:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 15, 'location_id': '57499312',
            'location_ref': '115444362', 'location_title': 'г. Кинешма, улица Щорса, д. 7', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1615,
            'transit_duration': '00:04:29', 'arrival_time': '14:59:57', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 14,
            'arrival_time_s': 53997, 'transit_duration_s': 269, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.450351, 'lon': 42.099869, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 16, 'location_id': '55969850',
            'location_ref': '113386553', 'location_title': 'г. Кинешма, улица Щорса, д. 64', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 300,
            'transit_duration': '00:02:07', 'arrival_time': '15:12:04', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 15,
            'arrival_time_s': 54724, 'transit_duration_s': 127, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.451334, 'lon': 42.099833, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 17, 'location_id': '56735644',
            'location_ref': '114420502', 'location_title': 'г. Кинешма, Шуйская улица, д. 1', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1100,
            'transit_duration': '00:03:42', 'arrival_time': '15:25:46', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 16,
            'arrival_time_s': 55546, 'transit_duration_s': 222, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.44592, 'lon': 42.099114, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 18, 'location_id': '57281146',
            'location_ref': '115156176', 'location_title': 'г. Кинешма, Карельская улица, д. 40', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1872,
            'transit_duration': '00:05:06', 'arrival_time': '15:40:52', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 17,
            'arrival_time_s': 56452, 'transit_duration_s': 306, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.45254, 'lon': 42.087921, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 19, 'location_id': 'm_57063872_57064234',
            'location_ref': '114864356_114864355', 'location_title': 'г. Кинешма, улица имени Юрия Горохова, д. 14А', 'type': 'delivery', 'multi_order': False,
            'transit_distance_m': 1187, 'transit_duration': '00:06:30', 'arrival_time': '15:57:22', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False,
            'is_last_in_route': False, 'stop_number': 18, 'arrival_time_s': 57442, 'transit_duration_s': 390, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.450593,
            'lon': 42.094155, 'service_duration_s': 280, 'shared_service_duration_s': 400, 'total_service_duration_s': 680, 'service_waiting_duration_s': 0,
            'waiting_duration': '00:00:00', 'service_duration': '00:04:40', 'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:20',
            'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 20, 'location_id': 'm_57104523_57357928',
            'location_ref': 'LO-73499932_LO-73768612', 'location_title': 'г. Кинешма, улица Менделеева, д. 3А, стр. , к. ', 'type': 'delivery', 'multi_order': False,
            'transit_distance_m': 2420, 'transit_duration': '00:08:14', 'arrival_time': '16:16:56', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False,
            'is_last_in_route': False, 'stop_number': 19, 'arrival_time_s': 58616, 'transit_duration_s': 494, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.453964,
            'lon': 42.111781, 'service_duration_s': 280, 'shared_service_duration_s': 400, 'total_service_duration_s': 680, 'service_waiting_duration_s': 0,
            'waiting_duration': '00:00:00', 'service_duration': '00:04:40', 'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:20',
            'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 21, 'location_id': '57613252',
            'location_ref': 'LO-74036257', 'location_title': 'г. Кинешма, улица Менделеева, д. 3А, стр. , к. ', 'type': 'delivery', 'multi_order': True, 'transit_distance_m': 0,
            'transit_duration': '00:00:00', 'arrival_time': '16:16:56', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 19,
            'arrival_time_s': 58616, 'transit_duration_s': 0, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.453964, 'lon': 42.111781, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 880, 'service_waiting_duration_s': 680, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:14:40', 'service_waiting_duration': '00:11:20'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 22, 'location_id': '56945529',
            'location_ref': '114702991', 'location_title': 'г. Кинешма, улица Менделеева, д. 3', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 168,
            'transit_duration': '00:01:17', 'arrival_time': '16:32:53', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 20,
            'arrival_time_s': 59573, 'transit_duration_s': 77, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.454157, 'lon': 42.11249, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 23, 'location_id': '57087768',
            'location_ref': '114897588', 'location_title': 'г. Кинешма, 2-я Шуйская улица, д. 1В', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2238,
            'transit_duration': '00:05:20', 'arrival_time': '16:48:13', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 21,
            'arrival_time_s': 60493, 'transit_duration_s': 320, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.440253, 'lon': 42.105393, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 24, 'location_id': 'm_57177487_57177738',
            'location_ref': '115019322_115019323', 'location_title': 'г. Кинешма, улица Щорса, д. 13А', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1899,
            'transit_duration': '00:05:05', 'arrival_time': '17:03:18', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 22,
            'arrival_time_s': 61398, 'transit_duration_s': 305, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.452278, 'lon': 42.095754, 'service_duration_s': 280,
            'shared_service_duration_s': 400, 'total_service_duration_s': 680, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:04:40',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:20', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 20214, 'vehicle_ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'route_number': 1, 'location_in_route': 25, 'location_id': '888891180',
            'location_ref': 'depot-91180', 'location_title': '', 'type': 'depot', 'multi_order': False, 'transit_distance_m': 104481, 'transit_duration': '01:34:01',
            'arrival_time': '18:48:39', 'time_window': '08:30:00-23:59:00', 'is_first_in_route': False, 'is_last_in_route': True, 'stop_number': 23, 'arrival_time_s': 67719,
            'transit_duration_s': 5641, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962973, 'lon': 40.947555, 'service_duration_s': 300,
            'shared_service_duration_s': 0, 'total_service_duration_s': 300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:00:00', 'total_service_duration': '00:05:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 0, 'location_id': '888891180',
            'location_ref': 'depot-91180', 'location_title': '', 'type': 'depot', 'multi_order': False, 'transit_distance_m': 0, 'transit_duration': '00:00:00',
            'arrival_time': '09:00:00', 'time_window': '08:30:00-23:59:00', 'is_first_in_route': True, 'is_last_in_route': False, 'stop_number': 0, 'arrival_time_s': 32400,
            'transit_duration_s': 0, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962973, 'lon': 40.947555, 'service_duration_s': 300,
            'shared_service_duration_s': 0, 'total_service_duration_s': 300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:00:00', 'total_service_duration': '00:05:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 1, 'location_id': '57334113',
            'location_ref': '115229641', 'location_title': 'г. Иваново, улица Смирнова, д. 100', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 7377,
            'transit_duration': '00:18:05', 'arrival_time': '09:23:05', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 1,
            'arrival_time_s': 33785, 'transit_duration_s': 1085, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.98296, 'lon': 41.014955, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 2,
            'location_id': 'm_57437223_57910175_58080132_58104742', 'location_ref': '115363386_115967057_116193712_116227214', 'location_title': 'г. Иваново, 11-й проезд, д. 7',
            'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2552, 'transit_duration': '00:06:53', 'arrival_time': '09:39:58', 'time_window': '09:00:00-14:00:00',
            'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 2, 'arrival_time_s': 34798, 'transit_duration_s': 413, 'waiting_duration_s': 0,
            'location_description': '', 'lat': 56.98926, 'lon': 41.03852, 'service_duration_s': 480, 'shared_service_duration_s': 400, 'total_service_duration_s': 880,
            'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:08:00', 'shared_service_duration': '00:06:40',
            'total_service_duration': '00:14:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 3, 'location_id': '58046392',
            'location_ref': '116148981', 'location_title': 'г. Иваново, 1-я Лагерная улица, д. 1', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 931,
            'transit_duration': '00:03:07', 'arrival_time': '09:57:45', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 3,
            'arrival_time_s': 35865, 'transit_duration_s': 187, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.983092, 'lon': 41.035293, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 4, 'location_id': '58080188',
            'location_ref': '116195559', 'location_title': 'г. Иваново, Садовая улица, д. 36', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3924,
            'transit_duration': '00:08:57', 'arrival_time': '10:16:42', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 4,
            'arrival_time_s': 37002, 'transit_duration_s': 537, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.997299, 'lon': 40.99204, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 5,
            'location_id': 'm_57434643_57576920_58079128_58114270', 'location_ref': '115360632_115547823_116193385_116240563', 'location_title': 'г. Иваново, улица Смирнова, д. 105',
            'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2297, 'transit_duration': '00:06:56', 'arrival_time': '10:33:38', 'time_window': '09:00:00-14:00:00',
            'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 5, 'arrival_time_s': 38018, 'transit_duration_s': 416, 'waiting_duration_s': 0,
            'location_description': '', 'lat': 56.98601, 'lon': 41.008083, 'service_duration_s': 480, 'shared_service_duration_s': 400, 'total_service_duration_s': 880,
            'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:08:00', 'shared_service_duration': '00:06:40',
            'total_service_duration': '00:14:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 6, 'location_id': '58041990',
            'location_ref': '116143535', 'location_title': 'г. Иваново, 1-я Меланжевая улица, д. 5А', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2195,
            'transit_duration': '00:06:06', 'arrival_time': '10:54:24', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 6,
            'arrival_time_s': 39264, 'transit_duration_s': 366, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.98724, 'lon': 41.02917, 'service_duration_s': 300,
            'shared_service_duration_s': 400, 'total_service_duration_s': 700, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 7, 'location_id': '58044662',
            'location_ref': '116146732', 'location_title': 'г. Иваново, улица Каравайковой, д. 141', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 328,
            'transit_duration': '00:01:08', 'arrival_time': '11:07:12', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 7,
            'arrival_time_s': 40032, 'transit_duration_s': 68, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.987918, 'lon': 41.028538, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 8, 'location_id': '58125498',
            'location_ref': '116255437', 'location_title': 'г. Иваново, 11-й проезд, д. 1', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 807,
            'transit_duration': '00:02:53', 'arrival_time': '11:20:05', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 8,
            'arrival_time_s': 40805, 'transit_duration_s': 173, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.989875, 'lon': 41.036228, 'service_duration_s': 300,
            'shared_service_duration_s': 400, 'total_service_duration_s': 700, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 9,
            'location_id': 'm_57060713_57294272_57501808_57614025_57742530_57803298_57864361_57864729_57866155_57884065_57958206_57964631_58038892_58039985_58040307_58045649_58047402_58068554_58111423_58125554_58130453',
            'location_ref': '114859850_115176972_115447949_115595959_115758764_115826217_115905797_115905536_115908197_115932492_116031321_116040107_116139107_116140597_116140598_116148172_116149760_116179422_116236472_116255684_116261754',
            'location_title': 'г. Иваново, улица Колотилова, д. 38', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2860, 'transit_duration': '00:06:00',
            'arrival_time': '11:37:45', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 9, 'arrival_time_s': 41865,
            'transit_duration_s': 360, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.995402, 'lon': 40.998912, 'service_duration_s': 1500,
            'shared_service_duration_s': 400, 'total_service_duration_s': 1900, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:25:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:31:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 10, 'location_id': '57529572',
            'location_ref': '115486584', 'location_title': 'г. Иваново, улица Окуловой, д. 68б', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3162,
            'transit_duration': '00:07:26', 'arrival_time': '12:16:51', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 10,
            'arrival_time_s': 44211, 'transit_duration_s': 446, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.99521, 'lon': 41.041654, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 11, 'location_id': '57712430',
            'location_ref': '115724780', 'location_title': 'г. Иваново, Пролетарская улица, д. 20', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2941,
            'transit_duration': '00:07:29', 'arrival_time': '12:34:20', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 11,
            'arrival_time_s': 45260, 'transit_duration_s': 449, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.992209, 'lon': 41.001157, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 12, 'location_id': '58065707',
            'location_ref': '116175012', 'location_title': 'г. Иваново, 10-я Санаторная улица, д. 5А', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 5077,
            'transit_duration': '00:12:17', 'arrival_time': '12:56:37', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 12,
            'arrival_time_s': 46597, 'transit_duration_s': 737, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.973213, 'lon': 41.048463, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 13, 'location_id': '57962072',
            'location_ref': '116037010', 'location_title': 'г. Иваново, улица Каравайковой, д. 90', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3962,
            'transit_duration': '00:10:14', 'arrival_time': '13:16:51', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 13,
            'arrival_time_s': 47811, 'transit_duration_s': 614, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.993998, 'lon': 41.025879, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 14, 'location_id': '58110155',
            'location_ref': '116234525', 'location_title': 'г. Иваново, пер стрелковый, д. 5', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2048,
            'transit_duration': '00:05:16', 'arrival_time': '13:32:07', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 14,
            'arrival_time_s': 48727, 'transit_duration_s': 316, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.991905, 'lon': 41.005137, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 15, 'location_id': '57560543',
            'location_ref': '115526839', 'location_title': 'г. Иваново, улица Смирнова, д. 78', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 835,
            'transit_duration': '00:03:51', 'arrival_time': '13:45:58', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 15,
            'arrival_time_s': 49558, 'transit_duration_s': 231, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.987212, 'lon': 40.999998, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 16, 'location_id': 'm_57597758_57597921',
            'location_ref': '115575031_115575028', 'location_title': 'г. Иваново, 7-я Санаторная улица, д. 3', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 4280,
            'transit_duration': '00:09:51', 'arrival_time': '14:05:49', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 16,
            'arrival_time_s': 50749, 'transit_duration_s': 591, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.977726, 'lon': 41.048813, 'service_duration_s': 280,
            'shared_service_duration_s': 400, 'total_service_duration_s': 680, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:04:40',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:20', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 17, 'location_id': '57803212',
            'location_ref': '115824285', 'location_title': 'г. Иваново, улица Каравайковой, д. 114', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2126,
            'transit_duration': '00:06:10', 'arrival_time': '14:23:19', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 17,
            'arrival_time_s': 51799, 'transit_duration_s': 370, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.990733, 'lon': 41.026687, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 18, 'location_id': 'm_57586050_57586073',
            'location_ref': '115560032_115560031', 'location_title': 'г. Иваново, 14-й проезд, д. 4', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1262,
            'transit_duration': '00:03:38', 'arrival_time': '14:36:57', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 18,
            'arrival_time_s': 52617, 'transit_duration_s': 218, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.985496, 'lon': 41.029293, 'service_duration_s': 280,
            'shared_service_duration_s': 400, 'total_service_duration_s': 680, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:04:40',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:20', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 19,
            'location_id': 'm_57622819_57622933_57623001', 'location_ref': '115607572_115607575_115607574', 'location_title': 'г. Иваново, 9-й проезд, д. 56', 'type': 'delivery',
            'multi_order': False, 'transit_distance_m': 1377, 'transit_duration': '00:04:03', 'arrival_time': '14:52:20', 'time_window': '14:00:00-22:00:00',
            'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 19, 'arrival_time_s': 53540, 'transit_duration_s': 243, 'waiting_duration_s': 0,
            'location_description': '', 'lat': 56.991719, 'lon': 41.035994, 'service_duration_s': 420, 'shared_service_duration_s': 400, 'total_service_duration_s': 820,
            'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:07:00', 'shared_service_duration': '00:06:40',
            'total_service_duration': '00:13:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22873, 'vehicle_ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'route_number': 2, 'location_in_route': 20, 'location_id': '888891180',
            'location_ref': 'depot-91180', 'location_title': '', 'type': 'depot', 'multi_order': False, 'transit_distance_m': 9531, 'transit_duration': '00:21:01',
            'arrival_time': '15:27:01', 'time_window': '08:30:00-23:59:00', 'is_first_in_route': False, 'is_last_in_route': True, 'stop_number': 20, 'arrival_time_s': 55621,
            'transit_duration_s': 1261, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962973, 'lon': 40.947555, 'service_duration_s': 300,
            'shared_service_duration_s': 0, 'total_service_duration_s': 300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:00:00', 'total_service_duration': '00:05:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 0, 'location_id': '888891180',
            'location_ref': 'depot-91180', 'location_title': '', 'type': 'depot', 'multi_order': False, 'transit_distance_m': 0, 'transit_duration': '00:00:00',
            'arrival_time': '09:00:00', 'time_window': '08:30:00-23:59:00', 'is_first_in_route': True, 'is_last_in_route': False, 'stop_number': 0, 'arrival_time_s': 32400,
            'transit_duration_s': 0, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962973, 'lon': 40.947555, 'service_duration_s': 300,
            'shared_service_duration_s': 0, 'total_service_duration_s': 300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:00:00', 'total_service_duration': '00:05:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 1, 'location_id': '56855133',
            'location_ref': '114581471', 'location_title': 'г. Кинешма, Вичугская улица, д. 184А', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 100261,
            'transit_duration': '01:28:32', 'arrival_time': '10:33:32', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 1,
            'arrival_time_s': 38012, 'transit_duration_s': 5312, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.429217, 'lon': 42.094676, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 2, 'location_id': '57409649',
            'location_ref': '115328969', 'location_title': 'г. Кинешма, улица Герцена, д. 31', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 7808,
            'transit_duration': '00:14:58', 'arrival_time': '10:58:30', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 2,
            'arrival_time_s': 39510, 'transit_duration_s': 898, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.427298, 'lon': 42.182802, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 3, 'location_id': '57640005',
            'location_ref': '115629394', 'location_title': 'г. Кинешма, Высоковольтная улица, д. 35А', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1342,
            'transit_duration': '00:03:16', 'arrival_time': '11:11:46', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 3,
            'arrival_time_s': 40306, 'transit_duration_s': 196, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.426096, 'lon': 42.166874, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 4, 'location_id': '57987359',
            'location_ref': '116069816', 'location_title': 'г. Кинешма, улица Аккуратова, д. 60', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2137,
            'transit_duration': '00:06:15', 'arrival_time': '11:28:01', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 4,
            'arrival_time_s': 41281, 'transit_duration_s': 375, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.421221, 'lon': 42.187626, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 5, 'location_id': '57229207',
            'location_ref': 'LO-73632237', 'location_title': 'г. Кинешма, улица имени Урицкого, д. 2А, стр. , к. ', 'type': 'delivery', 'multi_order': False,
            'transit_distance_m': 4430, 'transit_duration': '00:10:01', 'arrival_time': '11:48:02', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False,
            'is_last_in_route': False, 'stop_number': 5, 'arrival_time_s': 42482, 'transit_duration_s': 601, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.435516,
            'lon': 42.210308, 'service_duration_s': 200, 'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0,
            'waiting_duration': '00:00:00', 'service_duration': '00:03:20', 'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00',
            'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 6, 'location_id': '57054443',
            'location_ref': 'LO-73447161', 'location_title': 'г. Кинешма, Ключевая улица, д. 8, стр. , к. ', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1175,
            'transit_duration': '00:03:59', 'arrival_time': '12:02:01', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 6,
            'arrival_time_s': 43321, 'transit_duration_s': 239, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.434469, 'lon': 42.223477, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 7, 'location_id': '57251250',
            'location_ref': '115119178', 'location_title': 'г. Кинешма, улица Аристарха Макарова, д. 108', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1730,
            'transit_duration': '00:04:14', 'arrival_time': '12:16:15', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 7,
            'arrival_time_s': 44175, 'transit_duration_s': 254, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.435165, 'lon': 42.234417, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 8, 'location_id': '57282061',
            'location_ref': '115160661', 'location_title': 'г. Кинешма, улица имени Урицкого, д. 21', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2544,
            'transit_duration': '00:05:34', 'arrival_time': '12:31:49', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 8,
            'arrival_time_s': 45109, 'transit_duration_s': 334, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.43334, 'lon': 42.211862, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 9,
            'location_id': 'm_56273732_56436736_56565934_57617348', 'location_ref': '113798679_114017297_114189999_115600442',
            'location_title': 'г. Кинешма, улица имени Урицкого, д. 2', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 345, 'transit_duration': '00:01:18',
            'arrival_time': '12:43:07', 'time_window': '09:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 9, 'arrival_time_s': 45787,
            'transit_duration_s': 78, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.435995, 'lon': 42.21152, 'service_duration_s': 480,
            'shared_service_duration_s': 400, 'total_service_duration_s': 880, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:08:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:14:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 10,
            'location_id': 'm_57333999_57334136_57668297_57684740_57685033', 'location_ref': '115229665_115229664_115668160_115690522_115690523',
            'location_title': 'г. Кинешма, улица имени М. Горького, д. 45', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3337, 'transit_duration': '00:07:11',
            'arrival_time': '13:04:58', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 10, 'arrival_time_s': 47098,
            'transit_duration_s': 431, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.441188, 'lon': 42.164565, 'service_duration_s': 700,
            'shared_service_duration_s': 400, 'total_service_duration_s': 1100, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:11:40',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:18:20', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 11,
            'location_id': 'm_57111994_57670050_57808882', 'location_ref': '114930542_115670211_115833453', 'location_title': 'г. Кинешма, улица Верещагина, д. 14',
            'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2088, 'transit_duration': '00:04:52', 'arrival_time': '13:28:10', 'time_window': '09:00:00-22:00:00',
            'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 11, 'arrival_time_s': 48490, 'transit_duration_s': 292, 'waiting_duration_s': 0,
            'location_description': '', 'lat': 57.432221, 'lon': 42.155295, 'service_duration_s': 420, 'shared_service_duration_s': 400, 'total_service_duration_s': 820,
            'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:07:00', 'shared_service_duration': '00:06:40',
            'total_service_duration': '00:13:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 12, 'location_id': '57951673',
            'location_ref': '116022658', 'location_title': 'г. Кинешма, улица Котовского, д. 2', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 705,
            'transit_duration': '00:02:07', 'arrival_time': '13:43:57', 'time_window': '09:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 12,
            'arrival_time_s': 49437, 'transit_duration_s': 127, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.432978, 'lon': 42.162696, 'service_duration_s': 300,
            'shared_service_duration_s': 400, 'total_service_duration_s': 700, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 13,
            'location_id': 'm_55966730_56103080_56530512_56795573_56972901_57478299_57478659_57571600_57649537_57864997_58062842',
            'location_ref': '113380575_113569956_114141198_114502187_114739534_115416998_115416999_115541501_115642141_115906832_116170632',
            'location_title': 'г. Кинешма, улица имени Ленина, д. 41', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2319, 'transit_duration': '00:04:31',
            'arrival_time': '14:00:08', 'time_window': '11:00:00-15:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 13, 'arrival_time_s': 50408,
            'transit_duration_s': 271, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.444245, 'lon': 42.16002, 'service_duration_s': 900,
            'shared_service_duration_s': 400, 'total_service_duration_s': 1300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:15:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:21:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 14,
            'location_id': 'm_57342999_57343263_57343702', 'location_ref': '115242109_115242108_115242110', 'location_title': 'г. Кинешма, улица Красный Металлист, д. 8',
            'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2335, 'transit_duration': '00:05:07', 'arrival_time': '14:26:55', 'time_window': '09:00:00-22:00:00',
            'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 14, 'arrival_time_s': 52015, 'transit_duration_s': 307, 'waiting_duration_s': 0,
            'location_description': '', 'lat': 57.438494, 'lon': 42.125453, 'service_duration_s': 420, 'shared_service_duration_s': 400, 'total_service_duration_s': 820,
            'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:07:00', 'shared_service_duration': '00:06:40',
            'total_service_duration': '00:13:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 15,
            'location_id': 'm_56953219_56953406_56991900_57000789_57152465_57152908_57349696_57350071_57350138_57350145_57350258_57350268_57350303_57357797_57517270_57517412_57529211_57704227_57704238_57713739_57714173_57805043_57805120_57866459',
            'location_ref': '114713448_114713450_114766727_114778697_114986099_114986100_115251303_115251301_115251305_115251302_115251304_115251299_115251300_115261570_115469655_115469654_LO-73947985_115713562_115713561_115726649_115726648_115827935_115827937_115908549',
            'location_title': 'г. Кинешма, Авиационная улица, д. 27', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3459, 'transit_duration': '00:07:31',
            'arrival_time': '14:48:06', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 15, 'arrival_time_s': 53286,
            'transit_duration_s': 451, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.413339, 'lon': 42.10949, 'service_duration_s': 4860,
            'shared_service_duration_s': 400, 'total_service_duration_s': 5260, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '01:21:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '01:27:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 16, 'location_id': '57880162',
            'location_ref': '115926660', 'location_title': 'г. Кинешма, улица Ивана Виноградова, д. 31', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 586,
            'transit_duration': '00:02:22', 'arrival_time': '16:18:08', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 16,
            'arrival_time_s': 58688, 'transit_duration_s': 142, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.414755, 'lon': 42.111862, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 17, 'location_id': '58111830',
            'location_ref': '116237213', 'location_title': 'г. Кинешма, Комсомольская улица, д. 48', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 5481,
            'transit_duration': '00:10:12', 'arrival_time': '16:38:20', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 17,
            'arrival_time_s': 59900, 'transit_duration_s': 612, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.44313, 'lon': 42.159679, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 18, 'location_id': '57177999',
            'location_ref': '115020564', 'location_title': 'г. Кинешма, Пригородная улица, д. 4', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 4605,
            'transit_duration': '00:09:44', 'arrival_time': '16:58:04', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 18,
            'arrival_time_s': 61084, 'transit_duration_s': 584, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.429658, 'lon': 42.101522, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 19, 'location_id': '57233624',
            'location_ref': '115094605', 'location_title': 'г. Кинешма, улица Бекренёва, д. 1', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 455,
            'transit_duration': '00:02:08', 'arrival_time': '17:10:12', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 19,
            'arrival_time_s': 61812, 'transit_duration_s': 128, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.432187, 'lon': 42.099258, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 19339, 'vehicle_ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'route_number': 3, 'location_in_route': 20, 'location_id': '888891180',
            'location_ref': 'depot-91180', 'location_title': '', 'type': 'depot', 'multi_order': False, 'transit_distance_m': 100732, 'transit_duration': '01:29:42',
            'arrival_time': '18:49:54', 'time_window': '08:30:00-23:59:00', 'is_first_in_route': False, 'is_last_in_route': True, 'stop_number': 20, 'arrival_time_s': 67794,
            'transit_duration_s': 5382, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962973, 'lon': 40.947555, 'service_duration_s': 300,
            'shared_service_duration_s': 0, 'total_service_duration_s': 300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:00:00', 'total_service_duration': '00:05:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 0, 'location_id': '888891180',
            'location_ref': 'depot-91180', 'location_title': '', 'type': 'depot', 'multi_order': False, 'transit_distance_m': 0, 'transit_duration': '00:00:00',
            'arrival_time': '09:00:00', 'time_window': '08:30:00-23:59:00', 'is_first_in_route': True, 'is_last_in_route': False, 'stop_number': 0, 'arrival_time_s': 32400,
            'transit_duration_s': 0, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962973, 'lon': 40.947555, 'service_duration_s': 300,
            'shared_service_duration_s': 0, 'total_service_duration_s': 300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:00:00', 'total_service_duration': '00:05:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 1,
            'location_id': 'm_56946259_57134309_57934465', 'location_ref': '114704370_114959591_115998951', 'location_title': 'г. Иваново, Лежневская, д. 160А', 'type': 'delivery',
            'multi_order': False, 'transit_distance_m': 2479, 'transit_duration': '00:07:37', 'arrival_time': '09:12:37', 'time_window': '09:00:00-14:00:00',
            'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 1, 'arrival_time_s': 33157, 'transit_duration_s': 457, 'waiting_duration_s': 0,
            'location_description': '', 'lat': 56.96105, 'lon': 40.972879, 'service_duration_s': 420, 'shared_service_duration_s': 400, 'total_service_duration_s': 820,
            'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:07:00', 'shared_service_duration': '00:06:40',
            'total_service_duration': '00:13:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 2, 'location_id': '56913306',
            'location_ref': '114659346', 'location_title': 'г. Иваново, проспект Текстильщиков, д. 80', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 974,
            'transit_duration': '00:03:11', 'arrival_time': '09:29:28', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 2,
            'arrival_time_s': 34168, 'transit_duration_s': 191, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.959455, 'lon': 40.982452, 'service_duration_s': 300,
            'shared_service_duration_s': 400, 'total_service_duration_s': 700, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 3, 'location_id': '57863989',
            'location_ref': '115905665', 'location_title': 'г. Иваново, проспект Строителей, д. 50А', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1835,
            'transit_duration': '00:05:21', 'arrival_time': '09:46:29', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 3,
            'arrival_time_s': 35189, 'transit_duration_s': 321, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962767, 'lon': 40.997825, 'service_duration_s': 300,
            'shared_service_duration_s': 400, 'total_service_duration_s': 700, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 4, 'location_id': '58106638',
            'location_ref': '116229635', 'location_title': 'г. Иваново, проспект Строителей, д. 35', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 782,
            'transit_duration': '00:02:40', 'arrival_time': '10:00:49', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 4,
            'arrival_time_s': 36049, 'transit_duration_s': 160, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.965893, 'lon': 41.003017, 'service_duration_s': 300,
            'shared_service_duration_s': 400, 'total_service_duration_s': 700, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 5,
            'location_id': 'm_57497276_57576748_57616655_57618063_57640606_57681622_57681854_57774803_57818403_57819542_57955530_58066716_58069028',
            'location_ref': '115442562_115547930_115599832_115601175_115630144_115685632_115686536_115793228_115845651_115847466_116027499_116176243_116179627',
            'location_title': 'г. Иваново, проспект Строителей, д. 50А', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 963, 'transit_duration': '00:03:25',
            'arrival_time': '10:15:54', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 5, 'arrival_time_s': 36954,
            'transit_duration_s': 205, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962914, 'lon': 40.99656, 'service_duration_s': 1020,
            'shared_service_duration_s': 400, 'total_service_duration_s': 1420, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:17:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:23:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 6,
            'location_id': 'm_57093314_57099566_57472862_57541467_57541653_57632376_57648591_57655722_57801547_57864602_57870773',
            'location_ref': '114904442_114912192_115409201_115500867_115501069_115619838_115641293_115649770_115823437_115905323_115914274',
            'location_title': 'г. Иваново, проспект Строителей, д. 25', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 734, 'transit_duration': '00:03:45',
            'arrival_time': '10:43:19', 'time_window': '10:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 6, 'arrival_time_s': 38599,
            'transit_duration_s': 225, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.965176, 'lon': 40.99001, 'service_duration_s': 900,
            'shared_service_duration_s': 400, 'total_service_duration_s': 1300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:15:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:21:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 7, 'location_id': 'm_58031030_58065880',
            'location_ref': '116127946_116175401', 'location_title': 'г. Иваново, Московский микрорайон, д. 15', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 673,
            'transit_duration': '00:02:42', 'arrival_time': '11:07:41', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 7,
            'arrival_time_s': 40061, 'transit_duration_s': 162, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.967248, 'lon': 40.985518, 'service_duration_s': 360,
            'shared_service_duration_s': 400, 'total_service_duration_s': 760, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:06:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:12:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 8, 'location_id': '58013998',
            'location_ref': '116105695', 'location_title': 'г. Иваново, Московский микрорайон, д. 8', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 776,
            'transit_duration': '00:02:27', 'arrival_time': '11:22:48', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 8,
            'arrival_time_s': 40968, 'transit_duration_s': 147, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.96842, 'lon': 40.991842, 'service_duration_s': 300,
            'shared_service_duration_s': 400, 'total_service_duration_s': 700, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 9,
            'location_id': 'm_57075430_57239187_57268322_57329290_57390697_57435153_57450157_57455238_57529854_57549717_57598011_57628736_57632125_57635937_57673260_57685275_57714224_57725663_57836905_57845249_57849897_57870380_57888532_57898746_57899023_57899807_57902902_57905482_57930276_57930522_57934705_57967761_57986005_58003418_58013369_58018099_58019841_58021724_58024016_58024776_58024810_58028425_58029223_58030654_58036241_58038690_58077479_58080651',
            'location_ref': '114879288_115103105_115142046_115223122_115305045_115361682_115379867_115386312_115486961_115511518_115574959_115614699_115619559_115624664_115674904_115690825_115726722_115740606_115869022_115879790_115887079_115913512_115938315_115951974_115951973_115952508_115956389_115961450_115994453_115994409_115999950_116044690_116067708_116089726_116104493_116110845_116113177_116115282_116118471_116120003_116119565_116124348_116125465_116128020_116135109_116138940_116191788_116195958',
            'location_title': 'г. Иваново, Лежневская улица, д. 117', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1647, 'transit_duration': '00:04:45',
            'arrival_time': '11:39:13', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 9, 'arrival_time_s': 41953,
            'transit_duration_s': 285, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.975329, 'lon': 40.976196, 'service_duration_s': 3120,
            'shared_service_duration_s': 400, 'total_service_duration_s': 3520, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:52:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:58:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 10,
            'location_id': 'm_56983667_57282482_57342695_57342960_57714309', 'location_ref': '114754896_115161381_115241181_115241183_115724345',
            'location_title': 'г. Иваново, Лежневская улица, д. 167', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1082, 'transit_duration': '00:03:25',
            'arrival_time': '12:41:18', 'time_window': '09:00:00-13:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 10, 'arrival_time_s': 45678,
            'transit_duration_s': 205, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.968254, 'lon': 40.974136, 'service_duration_s': 540,
            'shared_service_duration_s': 400, 'total_service_duration_s': 940, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:09:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:15:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 11, 'location_id': '57397737',
            'location_ref': '115313950', 'location_title': 'г. Иваново, Лежневская улица, д. 152', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1225,
            'transit_duration': '00:04:37', 'arrival_time': '13:01:35', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 11,
            'arrival_time_s': 46895, 'transit_duration_s': 277, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.965476, 'lon': 40.970893, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 12,
            'location_id': 'm_56749771_57097323_57606905_57816830_57988837_58112603', 'location_ref': '114438259_114909815_115586323_115843383_116071099_116237559',
            'location_title': 'г. Иваново, Лежневская улица, д. 152', 'type': 'delivery', 'multi_order': True, 'transit_distance_m': 0, 'transit_duration': '00:00:00',
            'arrival_time': '13:01:35', 'time_window': '08:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 11, 'arrival_time_s': 46895,
            'transit_duration_s': 0, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.965477, 'lon': 40.970894, 'service_duration_s': 600,
            'shared_service_duration_s': 400, 'total_service_duration_s': 1200, 'service_waiting_duration_s': 600, 'waiting_duration': '00:00:00', 'service_duration': '00:10:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:20:00', 'service_waiting_duration': '00:10:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 13, 'location_id': '57550205',
            'location_ref': '115512714', 'location_title': 'г. Иваново, д. 14Ак1', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1828,
            'transit_duration': '00:06:07', 'arrival_time': '13:27:42', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 12,
            'arrival_time_s': 48462, 'transit_duration_s': 367, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.966182, 'lon': 40.990557, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 14, 'location_id': '57874957',
            'location_ref': '115919477', 'location_title': 'г. Иваново, микрорайон Московский, д. 15', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 312,
            'transit_duration': '00:01:11', 'arrival_time': '13:38:53', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 13,
            'arrival_time_s': 49133, 'transit_duration_s': 71, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.967037, 'lon': 40.98672, 'service_duration_s': 300,
            'shared_service_duration_s': 400, 'total_service_duration_s': 700, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 15, 'location_id': '57389059',
            'location_ref': '115302645', 'location_title': 'г. Иваново, Московский микрорайон, д. 5', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 400,
            'transit_duration': '00:01:09', 'arrival_time': '13:51:42', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 14,
            'arrival_time_s': 49902, 'transit_duration_s': 69, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.966786, 'lon': 40.992327, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 16, 'location_id': '58029997',
            'location_ref': '116127255', 'location_title': 'г. Иваново, Мкр московский, д. 14а корп2', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 250,
            'transit_duration': '00:01:15', 'arrival_time': '14:02:57', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 15,
            'arrival_time_s': 50577, 'transit_duration_s': 75, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.966266, 'lon': 40.989012, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 17, 'location_id': '58119145',
            'location_ref': '116246870', 'location_title': 'г. Иваново, улица Текстильщиков, д. 80, тц Аксон', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2159,
            'transit_duration': '00:09:02', 'arrival_time': '14:21:59', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 16,
            'arrival_time_s': 51719, 'transit_duration_s': 542, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.960068, 'lon': 40.981098, 'service_duration_s': 1100,
            'shared_service_duration_s': 400, 'total_service_duration_s': 1500, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:18:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:25:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 18, 'location_id': '57943876',
            'location_ref': '116012809', 'location_title': 'г. Иваново, проспект Текстильщиков, д. 109', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1373,
            'transit_duration': '00:05:28', 'arrival_time': '14:52:27', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 17,
            'arrival_time_s': 53547, 'transit_duration_s': 328, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.957089, 'lon': 40.9923, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 19,
            'location_id': 'm_57220616_57220819_57220906', 'location_ref': '115077061_115077059_115077060', 'location_title': 'г. Иваново, 2-я улица Чапаева, д. 40А',
            'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2843, 'transit_duration': '00:08:08', 'arrival_time': '15:10:35', 'time_window': '09:00:00-22:00:00',
            'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 18, 'arrival_time_s': 54635, 'transit_duration_s': 488, 'waiting_duration_s': 0,
            'location_description': '', 'lat': 56.975116, 'lon': 41.002729, 'service_duration_s': 420, 'shared_service_duration_s': 400, 'total_service_duration_s': 820,
            'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:07:00', 'shared_service_duration': '00:06:40',
            'total_service_duration': '00:13:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 20, 'location_id': '57599421',
            'location_ref': 'LO-74021240', 'location_title': 'г. Иваново, , д. 21, стр. , к. ', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2601,
            'transit_duration': '00:06:48', 'arrival_time': '15:31:03', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 19,
            'arrival_time_s': 55863, 'transit_duration_s': 408, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.970756, 'lon': 40.98859, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 21, 'location_id': '58106912',
            'location_ref': '116230523', 'location_title': 'г. Иваново, Микрорайон 30, д. 18', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1167,
            'transit_duration': '00:04:18', 'arrival_time': '15:45:21', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 20,
            'arrival_time_s': 56721, 'transit_duration_s': 258, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.968665, 'lon': 40.999414, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 22, 'location_id': '57154794',
            'location_ref': '114988621', 'location_title': 'г. Иваново, д. 2', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 731, 'transit_duration': '00:03:48',
            'arrival_time': '15:59:09', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 21, 'arrival_time_s': 57549,
            'transit_duration_s': 228, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.968317, 'lon': 40.994492, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 23, 'location_id': '57222722',
            'location_ref': '115079767', 'location_title': 'г. Иваново, Лежневская улица, д. 117', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1926,
            'transit_duration': '00:06:10', 'arrival_time': '16:15:19', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 22,
            'arrival_time_s': 58519, 'transit_duration_s': 370, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.975097, 'lon': 40.976274, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 24, 'location_id': '57524976',
            'location_ref': '115480349', 'location_title': 'г. Иваново, 2-я Полётная улица, д. 20', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 685,
            'transit_duration': '00:02:39', 'arrival_time': '16:27:58', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 23,
            'arrival_time_s': 59278, 'transit_duration_s': 159, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.976578, 'lon': 40.983568, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 25, 'location_id': '57760952',
            'location_ref': '115778855', 'location_title': 'г. Иваново, микрорайон Московский, д. 19', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1576,
            'transit_duration': '00:04:35', 'arrival_time': '16:42:33', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 24,
            'arrival_time_s': 60153, 'transit_duration_s': 275, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.97001, 'lon': 40.987458, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 26, 'location_id': '57462940',
            'location_ref': '115396641', 'location_title': 'г. Иваново, улица Станкостроителей, д. 5Б', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3520,
            'transit_duration': '00:09:25', 'arrival_time': '17:01:58', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 25,
            'arrival_time_s': 61318, 'transit_duration_s': 565, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.964808, 'lon': 40.945192, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 27, 'location_id': '57956084',
            'location_ref': '116029221', 'location_title': 'г. Иваново, улица Кирякиных, д. 9', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2260,
            'transit_duration': '00:07:28', 'arrival_time': '17:19:26', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 26,
            'arrival_time_s': 62366, 'transit_duration_s': 448, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.968454, 'lon': 40.970561, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 28, 'location_id': '57944995',
            'location_ref': '116014243', 'location_title': 'г. Иваново, 1-ая Полевая, д. 35А', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1315,
            'transit_duration': '00:05:40', 'arrival_time': '17:35:06', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 27,
            'arrival_time_s': 63306, 'transit_duration_s': 340, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.972202, 'lon': 40.978996, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 29, 'location_id': 'm_57880576_57881205',
            'location_ref': '115927559_115927561', 'location_title': 'г. Иваново, 30 Микрорайон, д. 53', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1812,
            'transit_duration': '00:05:28', 'arrival_time': '17:50:34', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 28,
            'arrival_time_s': 64234, 'transit_duration_s': 328, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.970966, 'lon': 41.000304, 'service_duration_s': 280,
            'shared_service_duration_s': 400, 'total_service_duration_s': 680, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:04:40',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:20', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 30, 'location_id': '57872496',
            'location_ref': '115916730', 'location_title': 'г. Иваново, Московский Мкн, д. 16', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1521,
            'transit_duration': '00:04:54', 'arrival_time': '18:06:48', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 29,
            'arrival_time_s': 65208, 'transit_duration_s': 294, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.968523, 'lon': 40.985706, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 31, 'location_id': '57296030',
            'location_ref': 'LO-73703236', 'location_title': 'г. Иваново, улица Мякишева, д. 10, стр. , к.', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1458,
            'transit_duration': '00:05:16', 'arrival_time': '18:22:04', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 30,
            'arrival_time_s': 66124, 'transit_duration_s': 316, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.969308, 'lon': 40.9771, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 32, 'location_id': '57703798',
            'location_ref': '115713352', 'location_title': 'г. Иваново, улица Станкостроителей, д. 3Бс5', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2747,
            'transit_duration': '00:07:56', 'arrival_time': '18:40:00', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 31,
            'arrival_time_s': 67200, 'transit_duration_s': 476, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.963789, 'lon': 40.955269, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 22876, 'vehicle_ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'route_number': 4, 'location_in_route': 33, 'location_id': '888891180',
            'location_ref': 'depot-91180', 'location_title': '', 'type': 'depot', 'multi_order': False, 'transit_distance_m': 1140, 'transit_duration': '00:04:45',
            'arrival_time': '18:54:45', 'time_window': '08:30:00-23:59:00', 'is_first_in_route': False, 'is_last_in_route': True, 'stop_number': 32, 'arrival_time_s': 68085,
            'transit_duration_s': 285, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962973, 'lon': 40.947555, 'service_duration_s': 300,
            'shared_service_duration_s': 0, 'total_service_duration_s': 300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:00:00', 'total_service_duration': '00:05:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 0, 'location_id': '888891180',
            'location_ref': 'depot-91180', 'location_title': '', 'type': 'depot', 'multi_order': False, 'transit_distance_m': 0, 'transit_duration': '00:00:00',
            'arrival_time': '09:00:00', 'time_window': '08:30:00-23:59:00', 'is_first_in_route': True, 'is_last_in_route': False, 'stop_number': 0, 'arrival_time_s': 32400,
            'transit_duration_s': 0, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962973, 'lon': 40.947555, 'service_duration_s': 300,
            'shared_service_duration_s': 0, 'total_service_duration_s': 300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:00:00', 'total_service_duration': '00:05:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 1,
            'location_id': 'm_57929697_57954519_57954714_58053492_58110059', 'location_ref': '115993815_116026115_116026427_116158702_116235117',
            'location_title': 'г. Иваново, улица Богдана Хмельницкого, д. 17', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3896, 'transit_duration': '00:10:28',
            'arrival_time': '09:15:28', 'time_window': '08:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 1, 'arrival_time_s': 33328,
            'transit_duration_s': 628, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.98799, 'lon': 40.968727, 'service_duration_s': 540,
            'shared_service_duration_s': 400, 'total_service_duration_s': 940, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:09:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:15:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 2, 'location_id': '58117405',
            'location_ref': '116244210', 'location_title': 'г. Иваново, Велижская улица, д. 72', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1761,
            'transit_duration': '00:06:23', 'arrival_time': '09:37:31', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 2,
            'arrival_time_s': 34651, 'transit_duration_s': 383, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.987442, 'lon': 40.957481, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 3, 'location_id': '57974705',
            'location_ref': '116052780', 'location_title': 'г. Иваново, улица Благова, д. 34', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 977,
            'transit_duration': '00:04:31', 'arrival_time': '09:52:02', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 3,
            'arrival_time_s': 35522, 'transit_duration_s': 271, 'waiting_duration_s': 478, 'location_description': '', 'lat': 56.982273, 'lon': 40.96067, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:07:58', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 4,
            'location_id': 'm_56843742_57509839_57844094_57902917_58033563_58033851', 'location_ref': '114566190_115459303_115879079_115956036_116131966_116131967',
            'location_title': 'г. Иваново, улица Арсения, д. 20', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2964, 'transit_duration': '00:10:50',
            'arrival_time': '10:20:50', 'time_window': '08:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 4, 'arrival_time_s': 37250,
            'transit_duration_s': 650, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.998333, 'lon': 40.986327, 'service_duration_s': 600,
            'shared_service_duration_s': 400, 'total_service_duration_s': 1000, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:10:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:16:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 5,
            'location_id': 'm_57414802_57477873_57886388_58035419_58066863_58127321', 'location_ref': '115335837_115415981_115935929_116133720_116177147_116257327',
            'location_title': 'г. Иваново, площадь Революции, д. 8А', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 812, 'transit_duration': '00:02:54',
            'arrival_time': '10:40:24', 'time_window': '10:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 5, 'arrival_time_s': 38424,
            'transit_duration_s': 174, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.995106, 'lon': 40.983356, 'service_duration_s': 600,
            'shared_service_duration_s': 400, 'total_service_duration_s': 1000, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:10:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:16:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 6,
            'location_id': 'm_57559303_57689100_57925422_58031689_58122509', 'location_ref': '115524767_115696767_115987010_116129411_116251092',
            'location_title': 'г. Иваново, Московская улица, д. 62', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1454, 'transit_duration': '00:04:24',
            'arrival_time': '11:01:28', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 6, 'arrival_time_s': 39688,
            'transit_duration_s': 264, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.986781, 'lon': 40.978754, 'service_duration_s': 540,
            'shared_service_duration_s': 400, 'total_service_duration_s': 940, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:09:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:15:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 7, 'location_id': '57492696',
            'location_ref': '115436661', 'location_title': 'г. Иваново, улица Смирнова, д. 11', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 965,
            'transit_duration': '00:03:41', 'arrival_time': '11:20:49', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 7,
            'arrival_time_s': 40849, 'transit_duration_s': 221, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.993847, 'lon': 40.983694, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 8, 'location_id': '58093466',
            'location_ref': '116213183', 'location_title': 'г. Иваново, Палехская улица, д. 6', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 490,
            'transit_duration': '00:02:38', 'arrival_time': '11:33:27', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 8,
            'arrival_time_s': 41607, 'transit_duration_s': 158, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.993856, 'lon': 40.977954, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 9, 'location_id': '58035602',
            'location_ref': '116133678', 'location_title': 'г. Иваново, улица Володарского, д. 1', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 993,
            'transit_duration': '00:04:05', 'arrival_time': '11:47:32', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 9,
            'arrival_time_s': 42452, 'transit_duration_s': 245, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.98754, 'lon': 40.977334, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 10,
            'location_id': 'm_57064510_57240806_57598087_57673276_57706756_57708926_57819295_57828572_57919540_57947968_57980040_57993860_58007905_58018039_58042173_58042767_58048893_58051807_58066866',
            'location_ref': '114864559_115104181_115575480_115674644_115716974_115718704_115847767_115858979_115979582_116018598_116059953_116078557_116096172_116110487_116143432_116144440_116151376_116155487_116177314',
            'location_title': 'г. Иваново, улица Красной Армии, д. 1', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1175, 'transit_duration': '00:04:39',
            'arrival_time': '12:02:11', 'time_window': '10:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 10, 'arrival_time_s': 43331,
            'transit_duration_s': 279, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.995036, 'lon': 40.978003, 'service_duration_s': 1380,
            'shared_service_duration_s': 300, 'total_service_duration_s': 1680, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:23:00',
            'shared_service_duration': '00:05:00', 'total_service_duration': '00:28:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 11, 'location_id': '57848870',
            'location_ref': '115885450', 'location_title': 'г. Иваново, Постышева, д. 57/3', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1785,
            'transit_duration': '00:07:34', 'arrival_time': '12:37:45', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 11,
            'arrival_time_s': 45465, 'transit_duration_s': 454, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.986241, 'lon': 40.991734, 'service_duration_s': 300,
            'shared_service_duration_s': 400, 'total_service_duration_s': 700, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 12, 'location_id': '57879104',
            'location_ref': '115924705', 'location_title': 'г. Иваново, улица Ванцетти, д. 18', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2440,
            'transit_duration': '00:08:14', 'arrival_time': '12:57:39', 'time_window': '10:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 12,
            'arrival_time_s': 46659, 'transit_duration_s': 494, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.001087, 'lon': 40.993552, 'service_duration_s': 300,
            'shared_service_duration_s': 400, 'total_service_duration_s': 700, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 13,
            'location_id': 'm_57270676_57365289_57517621_57587901_57617566_57629122_57640574_57716167_57829686_57888772_57908164_57915694_57966252_58022988_58039536_58054970_58061478_58068364_58094906_58128029',
            'location_ref': '115145919_115271129_115469428_115561459_115601002_115615601_115630992_115729397_115860446_115939056_115964750_115974624_116042606_116117706_116139402_116160915_116169333_116179357_116214383_116258316',
            'location_title': 'г. Иваново, Ташкентская улица, д. 64', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 4139, 'transit_duration': '00:12:25',
            'arrival_time': '13:21:44', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 13, 'arrival_time_s': 48104,
            'transit_duration_s': 745, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.981234, 'lon': 40.966968, 'service_duration_s': 1440,
            'shared_service_duration_s': 400, 'total_service_duration_s': 1840, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:24:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:30:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 14, 'location_id': '57795318',
            'location_ref': '115814991', 'location_title': 'г. Иваново, улица Пушкина, д. 7', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3647,
            'transit_duration': '00:10:41', 'arrival_time': '14:03:05', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 14,
            'arrival_time_s': 50585, 'transit_duration_s': 641, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.999711, 'lon': 40.98188, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 15, 'location_id': '57917846',
            'location_ref': '115977341', 'location_title': 'г. Иваново, Шереметевский проспект, д. 21', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 622,
            'transit_duration': '00:02:23', 'arrival_time': '14:15:28', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 15,
            'arrival_time_s': 51328, 'transit_duration_s': 143, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.000421, 'lon': 40.9847, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 16, 'location_id': '58114188',
            'location_ref': '116241118', 'location_title': 'г. Иваново, улица Марии Рябининой, д. 32', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1095,
            'transit_duration': '00:03:04', 'arrival_time': '14:28:32', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 16,
            'arrival_time_s': 52112, 'transit_duration_s': 184, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.999798, 'lon': 40.994815, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 17, 'location_id': '57659324',
            'location_ref': '115655317', 'location_title': 'г. Иваново, Крутицкая улица, д. 20А', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1465,
            'transit_duration': '00:04:14', 'arrival_time': '14:42:46', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 17,
            'arrival_time_s': 52966, 'transit_duration_s': 254, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.001769, 'lon': 40.981843, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 18, 'location_id': '57219846',
            'location_ref': '115076530', 'location_title': 'г. Иваново, улица Степанова, д. 17', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1327,
            'transit_duration': '00:04:27', 'arrival_time': '14:57:13', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 18,
            'arrival_time_s': 53833, 'transit_duration_s': 267, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.997779, 'lon': 40.972779, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 19, 'location_id': '57501534',
            'location_ref': '115447326', 'location_title': 'г. Иваново, улица Красной Армии, д. 1', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 707,
            'transit_duration': '00:02:04', 'arrival_time': '15:09:17', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 19,
            'arrival_time_s': 54557, 'transit_duration_s': 124, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.995053, 'lon': 40.978062, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 20, 'location_id': '57693840',
            'location_ref': '115701278', 'location_title': 'г. Иваново, Шереметевский проспект, д. 58', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2519,
            'transit_duration': '00:08:29', 'arrival_time': '15:27:46', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 20,
            'arrival_time_s': 55666, 'transit_duration_s': 509, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.005798, 'lon': 40.99239, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 21, 'location_id': '57322496',
            'location_ref': 'LO-73731483', 'location_title': 'г. Иваново, Велижская улица, д. 12, стр. , к. ', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 4649,
            'transit_duration': '00:13:34', 'arrival_time': '15:51:20', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 21,
            'arrival_time_s': 57080, 'transit_duration_s': 814, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.982538, 'lon': 40.974451, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 22, 'location_id': '58117541',
            'location_ref': '116244677', 'location_title': 'г. Иваново, Велижская улица, д. 3', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 471,
            'transit_duration': '00:01:43', 'arrival_time': '16:03:03', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 22,
            'arrival_time_s': 57783, 'transit_duration_s': 103, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.981602, 'lon': 40.976176, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 23, 'location_id': '58054174',
            'location_ref': '116158955', 'location_title': 'г. Иваново, улица Богдана Хмельницкого, д. 9', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1591,
            'transit_duration': '00:05:15', 'arrival_time': '16:18:18', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 23,
            'arrival_time_s': 58698, 'transit_duration_s': 315, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.989159, 'lon': 40.971863, 'service_duration_s': 1100,
            'shared_service_duration_s': 400, 'total_service_duration_s': 1500, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:18:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:25:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 24, 'location_id': 'm_57063916_57906774',
            'location_ref': '114863878_115963438', 'location_title': 'г. Иваново, Типографская улица, д. 6', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1146,
            'transit_duration': '00:04:26', 'arrival_time': '16:47:44', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 24,
            'arrival_time_s': 60464, 'transit_duration_s': 266, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.986324, 'lon': 40.979705, 'service_duration_s': 280,
            'shared_service_duration_s': 400, 'total_service_duration_s': 680, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:04:40',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:20', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 25, 'location_id': 'm_56823253_58098840',
            'location_ref': '114538782_116219973', 'location_title': 'г. Иваново, Шуйская улица, д. 21А', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1369,
            'transit_duration': '00:05:46', 'arrival_time': '17:04:50', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 25,
            'arrival_time_s': 61490, 'transit_duration_s': 346, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.98936, 'lon': 40.985913, 'service_duration_s': 280,
            'shared_service_duration_s': 400, 'total_service_duration_s': 680, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:04:40',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:20', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 26, 'location_id': '57801682',
            'location_ref': '115824063', 'location_title': 'г. Иваново, улица Багаева, д. 25/1', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3500,
            'transit_duration': '00:11:06', 'arrival_time': '17:27:16', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 26,
            'arrival_time_s': 62836, 'transit_duration_s': 666, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.991826, 'lon': 40.974612, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 27, 'location_id': '57645983',
            'location_ref': '115637753', 'location_title': 'г. Иваново, Велижская улица, д. 10', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2106,
            'transit_duration': '00:07:44', 'arrival_time': '17:45:00', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 27,
            'arrival_time_s': 63900, 'transit_duration_s': 464, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.982356, 'lon': 40.976301, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 28, 'location_id': '58036015',
            'location_ref': '116134974', 'location_title': 'г. Иваново, улица Колотилова, д. 10', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2195,
            'transit_duration': '00:07:46', 'arrival_time': '18:02:46', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 28,
            'arrival_time_s': 64966, 'transit_duration_s': 466, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.992204, 'lon': 40.9953, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 29, 'location_id': '58100688',
            'location_ref': '116222634', 'location_title': 'г. Иваново, улица Багаева, д. 37', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2271,
            'transit_duration': '00:08:33', 'arrival_time': '18:21:19', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 29,
            'arrival_time_s': 66079, 'transit_duration_s': 513, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.993474, 'lon': 40.971882, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 30503, 'vehicle_ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'route_number': 5, 'location_in_route': 30, 'location_id': '888891180',
            'location_ref': 'depot-91180', 'location_title': '', 'type': 'depot', 'multi_order': False, 'transit_distance_m': 4510, 'transit_duration': '00:11:14',
            'arrival_time': '18:42:33', 'time_window': '08:30:00-23:59:00', 'is_first_in_route': False, 'is_last_in_route': True, 'stop_number': 30, 'arrival_time_s': 67353,
            'transit_duration_s': 674, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962973, 'lon': 40.947555, 'service_duration_s': 300,
            'shared_service_duration_s': 0, 'total_service_duration_s': 300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:00:00', 'total_service_duration': '00:05:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 27991, 'vehicle_ref': 'Васильев Григорий-1416242795-КГТ Доставка', 'route_number': 6, 'location_in_route': 0, 'location_id': '888891180',
            'location_ref': 'depot-91180', 'location_title': '', 'type': 'depot', 'multi_order': False, 'transit_distance_m': 0, 'transit_duration': '00:00:00',
            'arrival_time': '09:00:00', 'time_window': '08:30:00-23:59:00', 'is_first_in_route': True, 'is_last_in_route': False, 'stop_number': 0, 'arrival_time_s': 32400,
            'transit_duration_s': 0, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962973, 'lon': 40.947555, 'service_duration_s': 300,
            'shared_service_duration_s': 0, 'total_service_duration_s': 300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:00:00', 'total_service_duration': '00:05:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 27991, 'vehicle_ref': 'Васильев Григорий-1416242795-КГТ Доставка', 'route_number': 6, 'location_in_route': 1, 'location_id': '57395238',
            'location_ref': '115309835', 'location_title': 'г. Иваново, Силикатный переулок, д. 17', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3537,
            'transit_duration': '00:08:45', 'arrival_time': '09:13:45', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 1,
            'arrival_time_s': 33225, 'transit_duration_s': 525, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.983053, 'lon': 40.939344, 'service_duration_s': 900,
            'shared_service_duration_s': 300, 'total_service_duration_s': 1200, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:15:00',
            'shared_service_duration': '00:05:00', 'total_service_duration': '00:20:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 27991, 'vehicle_ref': 'Васильев Григорий-1416242795-КГТ Доставка', 'route_number': 6, 'location_in_route': 2, 'location_id': 'm_57296451_57864207',
            'location_ref': '115178939_115905040', 'location_title': 'г. Иваново, 4-я Березниковская улица, д. 54/25', 'type': 'delivery', 'multi_order': False,
            'transit_distance_m': 283, 'transit_duration': '00:01:13', 'arrival_time': '09:34:58', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False,
            'is_last_in_route': False, 'stop_number': 2, 'arrival_time_s': 34498, 'transit_duration_s': 73, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.983156,
            'lon': 40.943207, 'service_duration_s': 900, 'shared_service_duration_s': 300, 'total_service_duration_s': 1200, 'service_waiting_duration_s': 0,
            'waiting_duration': '00:00:00', 'service_duration': '00:15:00', 'shared_service_duration': '00:05:00', 'total_service_duration': '00:20:00',
            'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 27991, 'vehicle_ref': 'Васильев Григорий-1416242795-КГТ Доставка', 'route_number': 6, 'location_in_route': 3, 'location_id': '57360355',
            'location_ref': '115264823', 'location_title': 'г. Иваново, улица Жарова, д. 41', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3428,
            'transit_duration': '00:09:19', 'arrival_time': '10:04:17', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 3,
            'arrival_time_s': 36257, 'transit_duration_s': 559, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.996901, 'lon': 40.967147, 'service_duration_s': 900,
            'shared_service_duration_s': 300, 'total_service_duration_s': 1200, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:15:00',
            'shared_service_duration': '00:05:00', 'total_service_duration': '00:20:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 27991, 'vehicle_ref': 'Васильев Григорий-1416242795-КГТ Доставка', 'route_number': 6, 'location_in_route': 4, 'location_id': '57910886',
            'location_ref': '115964338', 'location_title': 'г. Иваново, улица 10 Августа, д. 18/17', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1497,
            'transit_duration': '00:06:33', 'arrival_time': '10:30:50', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 4,
            'arrival_time_s': 37850, 'transit_duration_s': 393, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.997052, 'lon': 40.980301, 'service_duration_s': 900,
            'shared_service_duration_s': 300, 'total_service_duration_s': 1200, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:15:00',
            'shared_service_duration': '00:05:00', 'total_service_duration': '00:20:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 27991, 'vehicle_ref': 'Васильев Григорий-1416242795-КГТ Доставка', 'route_number': 6, 'location_in_route': 5, 'location_id': 'm_57543025_57543040',
            'location_ref': '115502122_115502123', 'location_title': 'г. Иваново, Советская улица, д. 25', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 967,
            'transit_duration': '00:03:29', 'arrival_time': '10:54:19', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 5,
            'arrival_time_s': 39259, 'transit_duration_s': 209, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.996641, 'lon': 40.984242, 'service_duration_s': 900,
            'shared_service_duration_s': 300, 'total_service_duration_s': 1200, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:15:00',
            'shared_service_duration': '00:05:00', 'total_service_duration': '00:20:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 27991, 'vehicle_ref': 'Васильев Григорий-1416242795-КГТ Доставка', 'route_number': 6, 'location_in_route': 6, 'location_id': '57569642',
            'location_ref': '115539376', 'location_title': 'г. Иваново, Революционная улица, д. 16Ак3', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 6834,
            'transit_duration': '00:16:13', 'arrival_time': '11:30:32', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 6,
            'arrival_time_s': 41432, 'transit_duration_s': 973, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.030979, 'lon': 40.922402, 'service_duration_s': 900,
            'shared_service_duration_s': 300, 'total_service_duration_s': 1200, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:15:00',
            'shared_service_duration': '00:05:00', 'total_service_duration': '00:20:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 27991, 'vehicle_ref': 'Васильев Григорий-1416242795-КГТ Доставка', 'route_number': 6, 'location_in_route': 7, 'location_id': '58061694',
            'location_ref': '116170105', 'location_title': 'г. Иваново, Набережная улица, д. 7', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 5959,
            'transit_duration': '00:14:03', 'arrival_time': '12:04:35', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 7,
            'arrival_time_s': 43475, 'transit_duration_s': 843, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.003553, 'lon': 40.978026, 'service_duration_s': 900,
            'shared_service_duration_s': 300, 'total_service_duration_s': 1200, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:15:00',
            'shared_service_duration': '00:05:00', 'total_service_duration': '00:20:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 27991, 'vehicle_ref': 'Васильев Григорий-1416242795-КГТ Доставка', 'route_number': 6, 'location_in_route': 8, 'location_id': '57928083',
            'location_ref': '115989123', 'location_title': 'г. Иваново, улица Кудряшова, д. 71к1', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 6228,
            'transit_duration': '00:16:25', 'arrival_time': '12:41:00', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 8,
            'arrival_time_s': 45660, 'transit_duration_s': 985, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.96602, 'lon': 40.985697, 'service_duration_s': 900,
            'shared_service_duration_s': 300, 'total_service_duration_s': 1200, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:15:00',
            'shared_service_duration': '00:05:00', 'total_service_duration': '00:20:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 27991, 'vehicle_ref': 'Васильев Григорий-1416242795-КГТ Доставка', 'route_number': 6, 'location_in_route': 9, 'location_id': '57550243',
            'location_ref': '115513077', 'location_title': 'г. Иваново, д. 4', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 768, 'transit_duration': '00:03:08',
            'arrival_time': '13:04:08', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 9, 'arrival_time_s': 47048,
            'transit_duration_s': 188, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.967139, 'lon': 40.994483, 'service_duration_s': 900,
            'shared_service_duration_s': 300, 'total_service_duration_s': 1200, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:15:00',
            'shared_service_duration': '00:05:00', 'total_service_duration': '00:20:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 27991, 'vehicle_ref': 'Васильев Григорий-1416242795-КГТ Доставка', 'route_number': 6, 'location_in_route': 10, 'location_id': 'm_58104461_58104653',
            'location_ref': '116227366_116227367', 'location_title': 'г. Иваново, проспект Строителей, д. 20', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 890,
            'transit_duration': '00:04:11', 'arrival_time': '13:28:19', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 10,
            'arrival_time_s': 48499, 'transit_duration_s': 251, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962409, 'lon': 40.988662, 'service_duration_s': 900,
            'shared_service_duration_s': 300, 'total_service_duration_s': 1200, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:15:00',
            'shared_service_duration': '00:05:00', 'total_service_duration': '00:20:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 27991, 'vehicle_ref': 'Васильев Григорий-1416242795-КГТ Доставка', 'route_number': 6, 'location_in_route': 11, 'location_id': '57264906',
            'location_ref': '115137326', 'location_title': 'г. Иваново, Лежневская улица, д. 140', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2049,
            'transit_duration': '00:06:32', 'arrival_time': '13:54:51', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 11,
            'arrival_time_s': 50091, 'transit_duration_s': 392, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.968189, 'lon': 40.9726, 'service_duration_s': 900,
            'shared_service_duration_s': 300, 'total_service_duration_s': 1200, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:15:00',
            'shared_service_duration': '00:05:00', 'total_service_duration': '00:20:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 27991, 'vehicle_ref': 'Васильев Григорий-1416242795-КГТ Доставка', 'route_number': 6, 'location_in_route': 12, 'location_id': '57996356',
            'location_ref': '116081921', 'location_title': 'г. Иваново, Лежневская улица, д. 140', 'type': 'delivery', 'multi_order': True, 'transit_distance_m': 0,
            'transit_duration': '00:00:00', 'arrival_time': '14:00:00', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 11,
            'arrival_time_s': 50400, 'transit_duration_s': 0, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.968189, 'lon': 40.9726, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 1191, 'service_waiting_duration_s': 891, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:19:51', 'service_waiting_duration': '00:14:51'
        }, {
            'vehicle_id': 27991, 'vehicle_ref': 'Васильев Григорий-1416242795-КГТ Доставка', 'route_number': 6, 'location_in_route': 13, 'location_id': '57929387',
            'location_ref': '115992681', 'location_title': 'г. Иваново, улица Куконковых, д. 150А', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 4363,
            'transit_duration': '00:10:29', 'arrival_time': '14:30:20', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 12,
            'arrival_time_s': 52220, 'transit_duration_s': 629, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.960471, 'lon': 41.025448, 'service_duration_s': 900,
            'shared_service_duration_s': 300, 'total_service_duration_s': 1200, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:15:00',
            'shared_service_duration': '00:05:00', 'total_service_duration': '00:20:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 27991, 'vehicle_ref': 'Васильев Григорий-1416242795-КГТ Доставка', 'route_number': 6, 'location_in_route': 14, 'location_id': '57948249',
            'location_ref': '116018632', 'location_title': 'г. Кинешма, улица Гоголя, д. 9', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 98204,
            'transit_duration': '01:23:07', 'arrival_time': '16:13:27', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 13,
            'arrival_time_s': 58407, 'transit_duration_s': 4987, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.444142, 'lon': 42.151333, 'service_duration_s': 900,
            'shared_service_duration_s': 300, 'total_service_duration_s': 1200, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:15:00',
            'shared_service_duration': '00:05:00', 'total_service_duration': '00:20:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 27991, 'vehicle_ref': 'Васильев Григорий-1416242795-КГТ Доставка', 'route_number': 6, 'location_in_route': 15, 'location_id': 'm_57296941_57426826',
            'location_ref': '115179919_115351670', 'location_title': 'г. Кинешма, улица Щорса, д. 7', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 4801,
            'transit_duration': '00:10:46', 'arrival_time': '16:44:13', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 14,
            'arrival_time_s': 60253, 'transit_duration_s': 646, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.450351, 'lon': 42.099869, 'service_duration_s': 900,
            'shared_service_duration_s': 300, 'total_service_duration_s': 1200, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:15:00',
            'shared_service_duration': '00:05:00', 'total_service_duration': '00:20:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 27991, 'vehicle_ref': 'Васильев Григорий-1416242795-КГТ Доставка', 'route_number': 6, 'location_in_route': 16, 'location_id': '58073941',
            'location_ref': '116186780', 'location_title': 'г. Иваново, улица Арсения, д. 57', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 102009,
            'transit_duration': '01:26:12', 'arrival_time': '18:30:25', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 15,
            'arrival_time_s': 66625, 'transit_duration_s': 5172, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.996387, 'lon': 40.992516, 'service_duration_s': 900,
            'shared_service_duration_s': 300, 'total_service_duration_s': 1200, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:15:00',
            'shared_service_duration': '00:05:00', 'total_service_duration': '00:20:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 27991, 'vehicle_ref': 'Васильев Григорий-1416242795-КГТ Доставка', 'route_number': 6, 'location_in_route': 17, 'location_id': '888891180',
            'location_ref': 'depot-91180', 'location_title': '', 'type': 'depot', 'multi_order': False, 'transit_distance_m': 6143, 'transit_duration': '00:15:30',
            'arrival_time': '19:05:55', 'time_window': '08:30:00-23:59:00', 'is_first_in_route': False, 'is_last_in_route': True, 'stop_number': 16, 'arrival_time_s': 68755,
            'transit_duration_s': 930, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962973, 'lon': 40.947555, 'service_duration_s': 300,
            'shared_service_duration_s': 0, 'total_service_duration_s': 300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:00:00', 'total_service_duration': '00:05:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 0, 'location_id': '888891180',
            'location_ref': 'depot-91180', 'location_title': '', 'type': 'depot', 'multi_order': False, 'transit_distance_m': 0, 'transit_duration': '00:00:00',
            'arrival_time': '09:00:00', 'time_window': '08:30:00-23:59:00', 'is_first_in_route': True, 'is_last_in_route': False, 'stop_number': 0, 'arrival_time_s': 32400,
            'transit_duration_s': 0, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962973, 'lon': 40.947555, 'service_duration_s': 300,
            'shared_service_duration_s': 0, 'total_service_duration_s': 300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:00:00', 'total_service_duration': '00:05:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 1, 'location_id': '58013636',
            'location_ref': '116104684', 'location_title': 'г. Иваново, улица Калинина, д. 7', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 6742,
            'transit_duration': '00:18:43', 'arrival_time': '09:23:43', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 1,
            'arrival_time_s': 33823, 'transit_duration_s': 1123, 'waiting_duration_s': 2177, 'location_description': '', 'lat': 57.007749, 'lon': 40.97931, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:36:17', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 2, 'location_id': '58068103',
            'location_ref': '116178162', 'location_title': 'г. Иваново, улица Громобоя, д. 27', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 929,
            'transit_duration': '00:03:22', 'arrival_time': '10:13:22', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 2,
            'arrival_time_s': 36802, 'transit_duration_s': 202, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.011715, 'lon': 40.984694, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 3,
            'location_id': 'm_57677899_57808134_57865138_57935679_58079088_58111234_58117136_58124104',
            'location_ref': '115679644_115831687_115905422_116001626_116194399_116236439_116244505_116253161', 'location_title': 'г. Иваново, улица Калинцева, д. 4',
            'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2937, 'transit_duration': '00:07:59', 'arrival_time': '10:31:21', 'time_window': '09:00:00-14:00:00',
            'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 3, 'arrival_time_s': 37881, 'transit_duration_s': 479, 'waiting_duration_s': 0,
            'location_description': '', 'lat': 57.017008, 'lon': 41.019938, 'service_duration_s': 720, 'shared_service_duration_s': 400, 'total_service_duration_s': 1120,
            'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:12:00', 'shared_service_duration': '00:06:40',
            'total_service_duration': '00:18:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 4, 'location_id': '57816775',
            'location_ref': '115843413', 'location_title': 'г. Иваново, улица Чайковского, д. 37', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 394,
            'transit_duration': '00:01:36', 'arrival_time': '10:51:37', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 4,
            'arrival_time_s': 39097, 'transit_duration_s': 96, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.015363, 'lon': 41.015101, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 5, 'location_id': '57356673',
            'location_ref': '115259820', 'location_title': 'г. Иваново, Строкинская улица, д. 10', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3257,
            'transit_duration': '00:07:27', 'arrival_time': '11:09:04', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 5,
            'arrival_time_s': 40144, 'transit_duration_s': 447, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.038316, 'lon': 41.016752, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 6, 'location_id': '57351947',
            'location_ref': '115253800', 'location_title': 'г. Иваново, улица Свободы, д. 50А', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3210,
            'transit_duration': '00:07:12', 'arrival_time': '11:26:16', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 6,
            'arrival_time_s': 41176, 'transit_duration_s': 432, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.017306, 'lon': 41.012979, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 7, 'location_id': '57173088',
            'location_ref': '115013634', 'location_title': 'г. Иваново, улица Шошина, д. 19', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2180,
            'transit_duration': '00:06:39', 'arrival_time': '11:42:55', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 7,
            'arrival_time_s': 42175, 'transit_duration_s': 399, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.008911, 'lon': 40.995606, 'service_duration_s': 300,
            'shared_service_duration_s': 400, 'total_service_duration_s': 700, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 8, 'location_id': '57845748',
            'location_ref': '115880980', 'location_title': 'г. Иваново, улица Шошина, д. 15', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 131,
            'transit_duration': '00:02:21', 'arrival_time': '11:56:56', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 8,
            'arrival_time_s': 43016, 'transit_duration_s': 141, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.009631, 'lon': 40.993917, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 9,
            'location_id': 'm_57848701_57868098_58060406_58128254', 'location_ref': '115884761_115911128_116167475_116258912',
            'location_title': 'г. Иваново, улица Степана Халтурина, д. 19А', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3967, 'transit_duration': '00:08:24',
            'arrival_time': '12:15:20', 'time_window': '10:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 9, 'arrival_time_s': 44120,
            'transit_duration_s': 504, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.030678, 'lon': 40.984314, 'service_duration_s': 480,
            'shared_service_duration_s': 400, 'total_service_duration_s': 880, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:08:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:14:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 10, 'location_id': 'm_57868605_57888675_57889205',
            'location_ref': '115911129_115938838_115938837', 'location_title': 'г. Иваново, 1-й Спортивный переулок, д. 8', 'type': 'delivery', 'multi_order': False,
            'transit_distance_m': 433, 'transit_duration': '00:01:35', 'arrival_time': '12:31:35', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False,
            'is_last_in_route': False, 'stop_number': 10, 'arrival_time_s': 45095, 'transit_duration_s': 95, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.028193,
            'lon': 40.981835, 'service_duration_s': 420, 'shared_service_duration_s': 400, 'total_service_duration_s': 820, 'service_waiting_duration_s': 0,
            'waiting_duration': '00:00:00', 'service_duration': '00:07:00', 'shared_service_duration': '00:06:40', 'total_service_duration': '00:13:40',
            'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 11, 'location_id': 'm_57400674_57508074',
            'location_ref': '115317838_115457053', 'location_title': 'г. Иваново, 4-я Деревенская улица, д. 34', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1277,
            'transit_duration': '00:04:44', 'arrival_time': '12:49:59', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 11,
            'arrival_time_s': 46199, 'transit_duration_s': 284, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.036323, 'lon': 40.984386, 'service_duration_s': 360,
            'shared_service_duration_s': 400, 'total_service_duration_s': 760, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:06:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:12:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 12, 'location_id': '57651122',
            'location_ref': '115643846', 'location_title': 'г. Иваново, 4-я Деревенская улица, д. 34', 'type': 'delivery', 'multi_order': True, 'transit_distance_m': 0,
            'transit_duration': '00:00:00', 'arrival_time': '12:49:59', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 11,
            'arrival_time_s': 46199, 'transit_duration_s': 0, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.036323, 'lon': 40.984386, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 960, 'service_waiting_duration_s': 760, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:16:00', 'service_waiting_duration': '00:12:40'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 13, 'location_id': '58068119',
            'location_ref': '116178194', 'location_title': 'г. Иваново, улица Талка, д. 10', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 5226,
            'transit_duration': '00:10:55', 'arrival_time': '13:16:54', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 12,
            'arrival_time_s': 47814, 'transit_duration_s': 655, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.021083, 'lon': 41.022465, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 14, 'location_id': '57622645',
            'location_ref': '115607588', 'location_title': 'г. Иваново, улица Полка Нормандия-Неман, д. 7', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3434,
            'transit_duration': '00:08:32', 'arrival_time': '13:35:26', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 13,
            'arrival_time_s': 48926, 'transit_duration_s': 512, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.020961, 'lon': 40.976031, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 15, 'location_id': '57038579',
            'location_ref': '114830270', 'location_title': 'г. Иваново, 4-й Завокзальный переулок, д. 25', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1384,
            'transit_duration': '00:02:38', 'arrival_time': '13:48:04', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 14,
            'arrival_time_s': 49684, 'transit_duration_s': 158, 'waiting_duration_s': 716, 'location_description': '', 'lat': 57.02171, 'lon': 40.99407, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:11:56', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 16, 'location_id': 'm_58094061_58095321',
            'location_ref': '116214112_116211791', 'location_title': 'г. Иваново, 11-я Завокзальная улица, д. 42', 'type': 'delivery', 'multi_order': False,
            'transit_distance_m': 573, 'transit_duration': '00:02:16', 'arrival_time': '14:12:16', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False,
            'is_last_in_route': False, 'stop_number': 15, 'arrival_time_s': 51136, 'transit_duration_s': 136, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.023474,
            'lon': 40.99822, 'service_duration_s': 280, 'shared_service_duration_s': 400, 'total_service_duration_s': 680, 'service_waiting_duration_s': 0,
            'waiting_duration': '00:00:00', 'service_duration': '00:04:40', 'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:20',
            'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 17, 'location_id': '58026551',
            'location_ref': '116122182', 'location_title': 'г. Иваново, улица Свободы, д. 34', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2321,
            'transit_duration': '00:06:09', 'arrival_time': '14:29:45', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 16,
            'arrival_time_s': 52185, 'transit_duration_s': 369, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.018232, 'lon': 41.020292, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 18, 'location_id': '58033236',
            'location_ref': '116129986', 'location_title': 'г. Иваново, улица Калинина, д. 12', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3393,
            'transit_duration': '00:10:22', 'arrival_time': '14:50:07', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 17,
            'arrival_time_s': 53407, 'transit_duration_s': 622, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.009229, 'lon': 40.980792, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 19, 'location_id': '58052837',
            'location_ref': '116156127', 'location_title': 'г. Иваново, 3-я Снежная улица, д. 6', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 5571,
            'transit_duration': '00:13:11', 'arrival_time': '15:13:18', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 18,
            'arrival_time_s': 54798, 'transit_duration_s': 791, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.035759, 'lon': 41.0226, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 20, 'location_id': '57503639',
            'location_ref': '115450660', 'location_title': 'г. Иваново, Плесская, д. 37', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 839,
            'transit_duration': '00:03:38', 'arrival_time': '15:26:56', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 19,
            'arrival_time_s': 55616, 'transit_duration_s': 218, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.033947, 'lon': 41.031421, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 21, 'location_id': 'm_57349662_57445111',
            'location_ref': '115250562_115373244', 'location_title': 'г. Иваново, улица Свободы, д. 45А', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 4146,
            'transit_duration': '00:10:37', 'arrival_time': '15:47:33', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 20,
            'arrival_time_s': 56853, 'transit_duration_s': 637, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.01486, 'lon': 41.007239, 'service_duration_s': 280,
            'shared_service_duration_s': 400, 'total_service_duration_s': 680, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:04:40',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:20', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 16861, 'vehicle_ref': 'Носко Денис-450347823-Lada Largus Фургон', 'route_number': 7, 'location_in_route': 22, 'location_id': '888891180',
            'location_ref': 'depot-91180', 'location_title': '', 'type': 'depot', 'multi_order': False, 'transit_distance_m': 9679, 'transit_duration': '00:24:51',
            'arrival_time': '16:23:44', 'time_window': '08:30:00-23:59:00', 'is_first_in_route': False, 'is_last_in_route': True, 'stop_number': 21, 'arrival_time_s': 59024,
            'transit_duration_s': 1491, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962973, 'lon': 40.947555, 'service_duration_s': 300,
            'shared_service_duration_s': 0, 'total_service_duration_s': 300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:00:00', 'total_service_duration': '00:05:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 0, 'location_id': '888891180',
            'location_ref': 'depot-91180', 'location_title': '', 'type': 'depot', 'multi_order': False, 'transit_distance_m': 0, 'transit_duration': '00:00:00',
            'arrival_time': '09:00:00', 'time_window': '08:30:00-23:59:00', 'is_first_in_route': True, 'is_last_in_route': False, 'stop_number': 0, 'arrival_time_s': 32400,
            'transit_duration_s': 0, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962973, 'lon': 40.947555, 'service_duration_s': 300,
            'shared_service_duration_s': 0, 'total_service_duration_s': 300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:00:00', 'total_service_duration': '00:05:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 1, 'location_id': '58119086',
            'location_ref': '116247095', 'location_title': 'г. Иваново, улица Зверева, д. 15', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 5747,
            'transit_duration': '00:15:51', 'arrival_time': '09:20:51', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 1,
            'arrival_time_s': 33651, 'transit_duration_s': 951, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.001705, 'lon': 40.965961, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 2, 'location_id': '57498016',
            'location_ref': '115442049', 'location_title': 'г. Иваново, Яблочная улица, д. 5', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 849,
            'transit_duration': '00:02:43', 'arrival_time': '09:33:34', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 2,
            'arrival_time_s': 34414, 'transit_duration_s': 163, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.998122, 'lon': 40.964012, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 3, 'location_id': '58047433',
            'location_ref': '116149951', 'location_title': 'г. Иваново, Силикатная улица, д. 14', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3206,
            'transit_duration': '00:09:07', 'arrival_time': '09:52:41', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 3,
            'arrival_time_s': 35561, 'transit_duration_s': 547, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.985378, 'lon': 40.941411, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 4,
            'location_id': 'm_56989291_56989464_56989541_56989664_56989837_56989844_56989928',
            'location_ref': '114763184_114763180_114763182_114763181_114763183_114763185_114763186', 'location_title': 'г. Иваново, улица Ломоносова, д. 3', 'type': 'delivery',
            'multi_order': False, 'transit_distance_m': 5193, 'transit_duration': '00:12:49', 'arrival_time': '10:15:30', 'time_window': '09:00:00-22:00:00',
            'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 4, 'arrival_time_s': 36930, 'transit_duration_s': 769, 'waiting_duration_s': 0,
            'location_description': '', 'lat': 57.000504, 'lon': 40.929292, 'service_duration_s': 979, 'shared_service_duration_s': 400, 'total_service_duration_s': 1379,
            'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:16:19', 'shared_service_duration': '00:06:40',
            'total_service_duration': '00:22:59', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 5, 'location_id': '58068449',
            'location_ref': '116178877', 'location_title': 'г. Иваново, улица Шевченко, д. 14/30', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 527,
            'transit_duration': '00:01:24', 'arrival_time': '10:39:53', 'time_window': '10:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 5,
            'arrival_time_s': 38393, 'transit_duration_s': 84, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.004393, 'lon': 40.931229, 'service_duration_s': 300,
            'shared_service_duration_s': 400, 'total_service_duration_s': 700, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 6, 'location_id': '57399846',
            'location_ref': '115316120', 'location_title': 'г. Иваново, улица Шевченко, д. 1', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1326,
            'transit_duration': '00:03:21', 'arrival_time': '10:54:54', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 6,
            'arrival_time_s': 39294, 'transit_duration_s': 201, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.000372, 'lon': 40.930204, 'service_duration_s': 300,
            'shared_service_duration_s': 400, 'total_service_duration_s': 700, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 7,
            'location_id': 'm_57649513_57684005_57758667', 'location_ref': '115641309_115688821_115775126', 'location_title': 'г. Иваново, Бакинский проезд, д. 55А',
            'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2327, 'transit_duration': '00:06:04', 'arrival_time': '11:12:38', 'time_window': '09:00:00-14:00:00',
            'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 7, 'arrival_time_s': 40358, 'transit_duration_s': 364, 'waiting_duration_s': 0,
            'location_description': '', 'lat': 56.989062, 'lon': 40.936119, 'service_duration_s': 420, 'shared_service_duration_s': 400, 'total_service_duration_s': 820,
            'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:07:00', 'shared_service_duration': '00:06:40',
            'total_service_duration': '00:13:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 8,
            'location_id': 'm_56903709_57293572_57550930_57640113_57653779_57670021_57715352_57839991_57840527_57883800_57886939_57906537_58019290_58070936_58075721_58090265_58100812_58117804_58119599_58120125_58129200_58130000',
            'location_ref': '114646894_115175680_115513957_115629666_115647879_115668875_115728250_115874006_115874007_115932583_115935997_115962098_116112693_116182407_116189093_116208861_116222403_116244861_116247871_116248103_116260416_116261515',
            'location_title': 'г. Иваново, улица Кузнецова, д. 8', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 4243, 'transit_duration': '00:10:03',
            'arrival_time': '11:36:21', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 8, 'arrival_time_s': 41781,
            'transit_duration_s': 603, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.998387, 'lon': 40.966734, 'service_duration_s': 1560,
            'shared_service_duration_s': 400, 'total_service_duration_s': 1960, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:26:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:32:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 9,
            'location_id': 'm_56988110_57289714_57515838_57574880_57605067_57680518_57822060_57846846_57883246_57904966_57926914_57931086_57933159_57936851_57947447_57959929_57960205_57983018_57992749_57993004_58003897_58031328_58066686_58075673_58080535_58080784_58081087_58085071_58086668_58096402_58111611_58119213_58121296',
            'location_ref': '114760598_115170080_115466797_115545801_115583633_115684221_115851057_115882695_115931243_115960359_115989621_115995599_115997989_116002790_116017472_116033767_116033766_116061180_116076898_116076899_116091701_116128301_116177073_116189130_116195421_116196672_116196673_116202133_116203910_116216352_116236642_116246446_116250197',
            'location_title': 'г. Иваново, улица Парижской Коммуны, д. 7А', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1246, 'transit_duration': '00:03:48',
            'arrival_time': '12:12:49', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 9, 'arrival_time_s': 43969,
            'transit_duration_s': 228, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.997691, 'lon': 40.951759, 'service_duration_s': 2520,
            'shared_service_duration_s': 400, 'total_service_duration_s': 2920, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:42:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:48:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 10, 'location_id': 'm_57685203_57685206',
            'location_ref': '115690597_115690595', 'location_title': 'г. Ивановская область, городской округ Иваново, Шахтинский проезд, д. 79', 'type': 'delivery',
            'multi_order': False, 'transit_distance_m': 1652, 'transit_duration': '00:03:40', 'arrival_time': '13:05:09', 'time_window': '09:00:00-22:00:00',
            'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 10, 'arrival_time_s': 47109, 'transit_duration_s': 220, 'waiting_duration_s': 0,
            'location_description': '', 'lat': 56.991306, 'lon': 40.931529, 'service_duration_s': 280, 'shared_service_duration_s': 400, 'total_service_duration_s': 680,
            'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:04:40', 'shared_service_duration': '00:06:40',
            'total_service_duration': '00:11:20', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 11, 'location_id': 'm_57668594_57834188',
            'location_ref': '115667963_115864746', 'location_title': 'г. Иваново, улица Кузнецова, д. 112', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3000,
            'transit_duration': '00:07:53', 'arrival_time': '13:24:22', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 11,
            'arrival_time_s': 48262, 'transit_duration_s': 473, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.00985, 'lon': 40.943386, 'service_duration_s': 360,
            'shared_service_duration_s': 400, 'total_service_duration_s': 760, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:06:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:12:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 12, 'location_id': '57147552',
            'location_ref': '114978975', 'location_title': 'г. Иваново, улица Поляковой, д. 8Б', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1540,
            'transit_duration': '00:06:11', 'arrival_time': '13:43:13', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 12,
            'arrival_time_s': 49393, 'transit_duration_s': 371, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.012787, 'lon': 40.949917, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 13, 'location_id': '57808493',
            'location_ref': '115832850', 'location_title': 'г. Иваново, улица Красных Зорь, д. 34А', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2755,
            'transit_duration': '00:07:32', 'arrival_time': '14:00:45', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 13,
            'arrival_time_s': 50445, 'transit_duration_s': 452, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.004025, 'lon': 40.927601, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 14, 'location_id': '57511943',
            'location_ref': '115462076', 'location_title': 'г. Иваново, Силикатная улица, д. 52', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 5913,
            'transit_duration': '00:16:51', 'arrival_time': '14:27:36', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 14,
            'arrival_time_s': 52056, 'transit_duration_s': 1011, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.985299, 'lon': 40.929112, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 15, 'location_id': '57082813',
            'location_ref': 'LO-73476421', 'location_title': 'г. Иваново, улица Красных Зорь, д. 16', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 4674,
            'transit_duration': '00:13:23', 'arrival_time': '14:50:59', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 15,
            'arrival_time_s': 53459, 'transit_duration_s': 803, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.002191, 'lon': 40.950968, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 16, 'location_id': '57953149',
            'location_ref': '116024591', 'location_title': 'г. Иваново, Неждановская улица, д. 45', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2628,
            'transit_duration': '00:07:31', 'arrival_time': '15:08:30', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 16,
            'arrival_time_s': 54510, 'transit_duration_s': 451, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.012258, 'lon': 40.929822, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 17, 'location_id': '57867752',
            'location_ref': '115910357', 'location_title': 'г. Иваново, улица Наговицыной-Икрянистовой, д. 6Д', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2601,
            'transit_duration': '00:06:56', 'arrival_time': '15:25:26', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 17,
            'arrival_time_s': 55526, 'transit_duration_s': 416, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.009685, 'lon': 40.958874, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 18, 'location_id': 'm_57918525_57918902',
            'location_ref': '115977388_115977387', 'location_title': 'г. Иваново, улица Кузнецова, д. 67Бк1', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1372,
            'transit_duration': '00:03:57', 'arrival_time': '15:39:23', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 18,
            'arrival_time_s': 56363, 'transit_duration_s': 237, 'waiting_duration_s': 0, 'location_description': '', 'lat': 57.005499, 'lon': 40.950852, 'service_duration_s': 280,
            'shared_service_duration_s': 400, 'total_service_duration_s': 680, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:04:40',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:20', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 19, 'location_id': 'm_57652176_57652339',
            'location_ref': '115645545_115645546', 'location_title': 'г. Иваново, Суздальская улица, д. 18б', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 4345,
            'transit_duration': '00:12:32', 'arrival_time': '16:03:15', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 19,
            'arrival_time_s': 57795, 'transit_duration_s': 752, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.992184, 'lon': 40.904786, 'service_duration_s': 280,
            'shared_service_duration_s': 400, 'total_service_duration_s': 680, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:04:40',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:20', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 20, 'location_id': '57455410',
            'location_ref': '115386526', 'location_title': 'г. Иваново, улица Герцена, д. 7', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 4204,
            'transit_duration': '00:11:19', 'arrival_time': '16:25:54', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 20,
            'arrival_time_s': 59154, 'transit_duration_s': 679, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.998828, 'lon': 40.954266, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 26275, 'vehicle_ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'route_number': 8, 'location_in_route': 21, 'location_id': '888891180',
            'location_ref': 'depot-91180', 'location_title': '', 'type': 'depot', 'multi_order': False, 'transit_distance_m': 4776, 'transit_duration': '00:13:09',
            'arrival_time': '16:49:03', 'time_window': '08:30:00-23:59:00', 'is_first_in_route': False, 'is_last_in_route': True, 'stop_number': 21, 'arrival_time_s': 60543,
            'transit_duration_s': 789, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962973, 'lon': 40.947555, 'service_duration_s': 300,
            'shared_service_duration_s': 0, 'total_service_duration_s': 300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:00:00', 'total_service_duration': '00:05:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 0, 'location_id': '888891180',
            'location_ref': 'depot-91180', 'location_title': '', 'type': 'depot', 'multi_order': False, 'transit_distance_m': 0, 'transit_duration': '00:00:00',
            'arrival_time': '09:00:00', 'time_window': '08:30:00-23:59:00', 'is_first_in_route': True, 'is_last_in_route': False, 'stop_number': 0, 'arrival_time_s': 32400,
            'transit_duration_s': 0, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962973, 'lon': 40.947555, 'service_duration_s': 300,
            'shared_service_duration_s': 0, 'total_service_duration_s': 300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:00:00', 'total_service_duration': '00:05:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 1, 'location_id': '56571032',
            'location_ref': '114197333', 'location_title': 'г. Иваново, проспект Строителей, д. 59', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 4750,
            'transit_duration': '00:12:22', 'arrival_time': '09:17:22', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 1,
            'arrival_time_s': 33442, 'transit_duration_s': 742, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.968626, 'lon': 41.010266, 'service_duration_s': 300,
            'shared_service_duration_s': 400, 'total_service_duration_s': 700, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 2, 'location_id': '56954971',
            'location_ref': '114715975', 'location_title': 'г. Иваново, улица Шубиных, д. 10', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1449,
            'transit_duration': '00:04:03', 'arrival_time': '09:33:05', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 2,
            'arrival_time_s': 34385, 'transit_duration_s': 243, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.961339, 'lon': 41.002532, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 3, 'location_id': 'm_56966445_56966446',
            'location_ref': '114730833_114730832', 'location_title': 'г. Иваново, Ломовская улица, д. 16', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1307,
            'transit_duration': '00:03:52', 'arrival_time': '09:46:57', 'time_window': '10:00:00-18:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 3,
            'arrival_time_s': 35217, 'transit_duration_s': 232, 'waiting_duration_s': 783, 'location_description': '', 'lat': 56.955749, 'lon': 40.998381, 'service_duration_s': 280,
            'shared_service_duration_s': 400, 'total_service_duration_s': 680, 'service_waiting_duration_s': 0, 'waiting_duration': '00:13:03', 'service_duration': '00:04:40',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:20', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 4, 'location_id': 'm_57932136_58074370',
            'location_ref': '115996732_116187335', 'location_title': 'г. Иваново, проспект Текстильщиков, д. 7Г', 'type': 'delivery', 'multi_order': False,
            'transit_distance_m': 1195, 'transit_duration': '00:02:45', 'arrival_time': '10:14:05', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False,
            'is_last_in_route': False, 'stop_number': 4, 'arrival_time_s': 36845, 'transit_duration_s': 165, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.96143,
            'lon': 41.013289, 'service_duration_s': 360, 'shared_service_duration_s': 400, 'total_service_duration_s': 760, 'service_waiting_duration_s': 0,
            'waiting_duration': '00:00:00', 'service_duration': '00:06:00', 'shared_service_duration': '00:06:40', 'total_service_duration': '00:12:40',
            'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 5, 'location_id': '56995310',
            'location_ref': '114770528', 'location_title': 'г. Иваново, проспект Текстильщиков, д. 38', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1768,
            'transit_duration': '00:03:42', 'arrival_time': '10:30:27', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 5,
            'arrival_time_s': 37827, 'transit_duration_s': 222, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.957477, 'lon': 40.998804, 'service_duration_s': 300,
            'shared_service_duration_s': 400, 'total_service_duration_s': 700, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 6,
            'location_id': 'm_56880461_58033081_58041527_58071743', 'location_ref': '114615708_116131252_116141674_116183507',
            'location_title': 'г. Иваново, проспект Текстильщиков, д. 3', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1686, 'transit_duration': '00:03:59',
            'arrival_time': '10:46:06', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 6, 'arrival_time_s': 38766,
            'transit_duration_s': 239, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.96289, 'lon': 41.01702, 'service_duration_s': 480,
            'shared_service_duration_s': 400, 'total_service_duration_s': 880, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:08:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:14:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 7,
            'location_id': 'm_57080064_57102825_57313934_57438479_57523032_57800292_57801120_57826590_57873111_57882339_57892091_57911813_57935228_57939781_58013349_58025231_58040859_58043040_58067194_58067383_58112590',
            'location_ref': '114885923_114915678_115201643_115365967_115477561_115821264_115822929_115856511_115916763_115929762_115943265_115968517_116000499_116006715_116104180_116120487_116141144_116144347_116177791_116178318_116238515',
            'location_title': 'г. Иваново, улица Куконковых, д. 141', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 745, 'transit_duration': '00:03:50',
            'arrival_time': '11:04:36', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 7, 'arrival_time_s': 39876,
            'transit_duration_s': 230, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.966468, 'lon': 41.024492, 'service_duration_s': 1500,
            'shared_service_duration_s': 300, 'total_service_duration_s': 1800, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:25:00',
            'shared_service_duration': '00:05:00', 'total_service_duration': '00:30:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 8, 'location_id': '57483287',
            'location_ref': 'LO-73898690', 'location_title': 'г. Иваново, улица Шубиных, д. 26А, стр. , к. ', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2136,
            'transit_duration': '00:08:12', 'arrival_time': '11:42:48', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 8,
            'arrival_time_s': 42168, 'transit_duration_s': 492, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.958194, 'lon': 41.010419, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 9, 'location_id': 'm_57666154_58018748',
            'location_ref': '115664897_116111370', 'location_title': 'г. Иваново, улица Куконковых, д. 90', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2573,
            'transit_duration': '00:06:07', 'arrival_time': '11:58:55', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 9,
            'arrival_time_s': 43135, 'transit_duration_s': 367, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.971825, 'lon': 41.011776, 'service_duration_s': 360,
            'shared_service_duration_s': 400, 'total_service_duration_s': 760, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:06:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:12:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 10,
            'location_id': 'm_57851861_57933479_58027760_58046004_58089592', 'location_ref': '115889144_115998801_116123574_116148045_116208441',
            'location_title': 'г. Иваново, Кохомское шоссе, д. 4А', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 3571, 'transit_duration': '00:08:09',
            'arrival_time': '12:19:44', 'time_window': '10:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 10, 'arrival_time_s': 44384,
            'transit_duration_s': 489, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.949383, 'lon': 41.051229, 'service_duration_s': 540,
            'shared_service_duration_s': 400, 'total_service_duration_s': 940, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:09:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:15:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 11, 'location_id': '57618967',
            'location_ref': '115602576', 'location_title': 'г. Иваново, Кохомское шоссе, д. 14', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 712,
            'transit_duration': '00:03:35', 'arrival_time': '12:38:59', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 11,
            'arrival_time_s': 45539, 'transit_duration_s': 215, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.95356, 'lon': 41.053448, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 12, 'location_id': '58074188',
            'location_ref': '116187439', 'location_title': 'г. Иваново, Микрорайон Тэц-3, д. 7', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 9171,
            'transit_duration': '00:14:43', 'arrival_time': '13:03:42', 'time_window': '09:00:00-14:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 12,
            'arrival_time_s': 47022, 'transit_duration_s': 883, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.954231, 'lon': 41.103781, 'service_duration_s': 300,
            'shared_service_duration_s': 400, 'total_service_duration_s': 700, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 13, 'location_id': '58024710',
            'location_ref': '116119509', 'location_title': 'г. Иваново, д. 3', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 8016, 'transit_duration': '00:15:04',
            'arrival_time': '13:30:26', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 13, 'arrival_time_s': 48626,
            'transit_duration_s': 904, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.954982, 'lon': 41.0429, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 14, 'location_id': '58112714',
            'location_ref': '116238040', 'location_title': 'г. Иваново, Кохомское шоссе, д. 17', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1403,
            'transit_duration': '00:05:04', 'arrival_time': '13:45:30', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 14,
            'arrival_time_s': 49530, 'transit_duration_s': 304, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.952661, 'lon': 41.056215, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 15, 'location_id': '57688498',
            'location_ref': '115695759', 'location_title': 'г. Иваново, Генерала Хлебникова., д. 60', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 4758,
            'transit_duration': '00:11:55', 'arrival_time': '14:07:25', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 15,
            'arrival_time_s': 50845, 'transit_duration_s': 715, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.972536, 'lon': 41.005083, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 16,
            'location_id': 'm_58075120_58095067_58113686', 'location_ref': '116187929_116215712_116240177', 'location_title': 'г. Иваново, Куконковых, д. 146', 'type': 'delivery',
            'multi_order': False, 'transit_distance_m': 2590, 'transit_duration': '00:07:23', 'arrival_time': '14:24:48', 'time_window': '09:00:00-22:00:00',
            'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 16, 'arrival_time_s': 51888, 'transit_duration_s': 443, 'waiting_duration_s': 0,
            'location_description': '', 'lat': 56.961702, 'lon': 41.024873, 'service_duration_s': 420, 'shared_service_duration_s': 400, 'total_service_duration_s': 820,
            'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:07:00', 'shared_service_duration': '00:06:40',
            'total_service_duration': '00:13:40', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 17, 'location_id': 'm_57941530_57941731',
            'location_ref': '116009410_116009409', 'location_title': 'г. Иваново, улица Шубиных, д. 19', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 1358,
            'transit_duration': '00:04:38', 'arrival_time': '14:43:06', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 17,
            'arrival_time_s': 52986, 'transit_duration_s': 278, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.963047, 'lon': 41.010599, 'service_duration_s': 280,
            'shared_service_duration_s': 400, 'total_service_duration_s': 680, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:04:40',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:11:20', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 18, 'location_id': '57996304',
            'location_ref': '116080928', 'location_title': 'г. Иваново, 2-я Лагерная улица, д. 52', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 4440,
            'transit_duration': '00:11:06', 'arrival_time': '15:05:32', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 18,
            'arrival_time_s': 54332, 'transit_duration_s': 666, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.973189, 'lon': 41.052757, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 19, 'location_id': '58098662',
            'location_ref': '116220066', 'location_title': 'г. Иваново, улица Куконковых, д. 150', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 2989,
            'transit_duration': '00:09:51', 'arrival_time': '15:25:23', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 19,
            'arrival_time_s': 55523, 'transit_duration_s': 591, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.961079, 'lon': 41.026841, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 20, 'location_id': '58036932',
            'location_ref': '116135817', 'location_title': 'г. Иваново, улица Куконковых, д. 126', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 884,
            'transit_duration': '00:03:04', 'arrival_time': '15:38:27', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 20,
            'arrival_time_s': 56307, 'transit_duration_s': 184, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.964499, 'lon': 41.021387, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 21, 'location_id': '57998934',
            'location_ref': '116082052', 'location_title': 'г. Иваново, улица Куконковых, д. 142', 'type': 'delivery', 'multi_order': False, 'transit_distance_m': 278,
            'transit_duration': '00:02:14', 'arrival_time': '15:50:41', 'time_window': '14:00:00-22:00:00', 'is_first_in_route': False, 'is_last_in_route': False, 'stop_number': 21,
            'arrival_time_s': 57041, 'transit_duration_s': 134, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962944, 'lon': 41.023867, 'service_duration_s': 200,
            'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:03:20',
            'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00', 'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 22, 'location_id': '57549982',
            'location_ref': 'LO-73970961', 'location_title': 'г. Иваново, Кавалерийская улица, д. 50, стр. , к. ', 'type': 'delivery', 'multi_order': False,
            'transit_distance_m': 467, 'transit_duration': '00:02:33', 'arrival_time': '16:03:14', 'time_window': '09:00:00-22:00:00', 'is_first_in_route': False,
            'is_last_in_route': False, 'stop_number': 22, 'arrival_time_s': 57794, 'transit_duration_s': 153, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.960127,
            'lon': 41.024774, 'service_duration_s': 200, 'shared_service_duration_s': 400, 'total_service_duration_s': 600, 'service_waiting_duration_s': 0,
            'waiting_duration': '00:00:00', 'service_duration': '00:03:20', 'shared_service_duration': '00:06:40', 'total_service_duration': '00:10:00',
            'service_waiting_duration': '00:00:00'
        }, {
            'vehicle_id': 14939, 'vehicle_ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'route_number': 9, 'location_in_route': 23, 'location_id': '888891180',
            'location_ref': 'depot-91180', 'location_title': '', 'type': 'depot', 'multi_order': False, 'transit_distance_m': 5978, 'transit_duration': '00:15:17',
            'arrival_time': '16:28:31', 'time_window': '08:30:00-23:59:00', 'is_first_in_route': False, 'is_last_in_route': True, 'stop_number': 23, 'arrival_time_s': 59311,
            'transit_duration_s': 917, 'waiting_duration_s': 0, 'location_description': '', 'lat': 56.962973, 'lon': 40.947555, 'service_duration_s': 300,
            'shared_service_duration_s': 0, 'total_service_duration_s': 300, 'service_waiting_duration_s': 0, 'waiting_duration': '00:00:00', 'service_duration': '00:05:00',
            'shared_service_duration': '00:00:00', 'total_service_duration': '00:05:00', 'service_waiting_duration': '00:00:00'
        }]
    }


def mvrp_market_request_excel():
    return {
        'Orders': [{
            'hard_window': False, 'id': '57816775', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.015363, 'point.lon': 41.015101, 'ref': '115843413',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.216648,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Чайковского, д. 37', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58071904', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.02636, 'point.lon': 40.9256, 'ref': '116183483',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.259776,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, Левобережная улица, д. 12', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57437223_57910175_58080132_58104742', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100,
            'penalty.drop': 30000, 'penalty.out_of_time.fixed': 120, 'penalty.out_of_time.minute': 4.0, 'point.lat': 56.98926, 'point.lon': 41.03852,
            'ref': '115363386_115967057_116193712_116227214', 'required_tags': 'delivery,prepaid', 'service_duration_s': 480, 'shared_service_duration_s': 400,
            'shipment_size.units': 0.001947, 'shipment_size.weight_kg': 4, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, 11-й проезд, д. 7',
            'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57666461', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.449557, 'point.lon': 42.094829, 'ref': '115665385',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.008325,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица имени Юрия Горохова, д. 12', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57868605_57888675_57889205', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 22500,
            'penalty.out_of_time.fixed': 90, 'penalty.out_of_time.minute': 3.0, 'point.lat': 57.028193, 'point.lon': 40.981835, 'ref': '115911129_115938838_115938837',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 420, 'shared_service_duration_s': 400, 'shipment_size.units': 0.155294,
            'shipment_size.weight_kg': 3, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, 1-й Спортивный переулок, д. 8', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57522985', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.034702, 'point.lon': 40.924594, 'ref': '115477923',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 1100, 'shared_service_duration_s': 400, 'shipment_size.units': 0.008464,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Дюковская улица, д. 25', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57872496', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.968523, 'point.lon': 40.985706, 'ref': '115916730',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.230945,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, Московский Мкн, д. 16', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57569642', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.030979, 'point.lon': 40.922402, 'ref': '115539376',
            'required_tags': 'delivery,prepaid,bulky_cargo', 'service_duration_s': 900, 'shared_service_duration_s': 300, 'shipment_size.units': 0.374976,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Революционная улица, д. 16Ак3', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57054443', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.434469, 'point.lon': 42.223477, 'ref': 'LO-73447161',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00264,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, Ключевая улица, д. 8, стр. , к. ', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57550205', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.966182, 'point.lon': 40.990557, 'ref': '115512714',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.010626,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, д. 14Ак1', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58052837', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.035759, 'point.lon': 41.0226, 'ref': '116156127',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.0091,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, 3-я Снежная улица, д. 6', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57996304', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.973189, 'point.lon': 41.052757, 'ref': '116080928',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.000567,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, 2-я Лагерная улица, д. 52', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57543025_57543040', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.996641, 'point.lon': 40.984242, 'ref': '115502122_115502123',
            'required_tags': 'delivery,prepaid,bulky_cargo', 'service_duration_s': 900, 'shared_service_duration_s': 300, 'shipment_size.units': 0.241458,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Советская улица, д. 25', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58119145', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.960068, 'point.lon': 40.981098, 'ref': '116246870',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 1100, 'shared_service_duration_s': 400, 'shipment_size.units': 0.005,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Текстильщиков, д. 80, тц Аксон', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57705499', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.034785, 'point.lon': 40.918126, 'ref': '115715880',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.02772,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Революционная, д. 24к4', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58117405', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.987442, 'point.lon': 40.957481, 'ref': '116244210',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.022032,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, Велижская улица, д. 72', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58047433', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.985378, 'point.lon': 40.941411, 'ref': '116149951',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.046125,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, Силикатная улица, д. 14', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57651122', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.036323, 'point.lon': 40.984386, 'ref': '115643846',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00204,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, 4-я Деревенская улица, д. 34', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57974705', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.982273, 'point.lon': 40.96067, 'ref': '116052780',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.013416,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Благова, д. 34', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58051058', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.026874, 'point.lon': 40.968009, 'ref': '116155733',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.013431,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Калашникова, д. 26', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58080188', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.997299, 'point.lon': 40.99204, 'ref': '116195559',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.062244,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Садовая улица, д. 36', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57718668', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 7500,
            'penalty.out_of_time.fixed': 30, 'penalty.out_of_time.minute': 1.0, 'point.lat': 57.032812, 'point.lon': 40.968306, 'ref': '115732755',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 300, 'shared_service_duration_s': 400, 'shipment_size.units': 0.001,
            'shipment_size.weight_kg': 1, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, 1-я Минеевская улица, д. 4', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57096387_57096550_57096802', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.459207, 'point.lon': 42.102204, 'ref': '114908909_114908908_114908910',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 420, 'shared_service_duration_s': 400, 'shipment_size.units': 0.012154,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Гагарина, д. 10', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58100688', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.993474, 'point.lon': 40.971882, 'ref': '116222634',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.000714,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица Багаева, д. 37', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57087768', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.440253, 'point.lon': 42.105393, 'ref': '114897588',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00462,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, 2-я Шуйская улица, д. 1В', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57910886', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.997052, 'point.lon': 40.980301, 'ref': '115964338',
            'required_tags': 'delivery,prepaid,bulky_cargo', 'service_duration_s': 900, 'shared_service_duration_s': 300, 'shipment_size.units': 0.58344,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица 10 Августа, д. 18/17', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58110155', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.991905, 'point.lon': 41.005137, 'ref': '116234525',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00068,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, пер стрелковый, д. 5', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58013636', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.007749, 'point.lon': 40.97931, 'ref': '116104684',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00084,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Калинина, д. 7', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57281146', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.45254, 'point.lon': 42.087921, 'ref': '115156176',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00564,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, Карельская улица, д. 40', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_56989291_56989464_56989541_56989664_56989837_56989844_56989928', 'optional_tags.0.tag': 'reg218202',
            'optional_tags.0.value': 100, 'penalty.drop': 5000, 'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.000504,
            'point.lon': 40.929292, 'ref': '114763184_114763180_114763182_114763181_114763183_114763185_114763186', 'required_tags': 'delivery,prepaid',
            'service_duration_s': 979, 'shared_service_duration_s': 400, 'shipment_size.units': 0.056171, 'shipment_size.weight_kg': 0,
            'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица Ломоносова, д. 3', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58024710', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.954982, 'point.lon': 41.0429, 'ref': '116119509',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.000448,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, д. 3', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57360355', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.996901, 'point.lon': 40.967147, 'ref': '115264823',
            'required_tags': 'delivery,prepaid,bulky_cargo', 'service_duration_s': 900, 'shared_service_duration_s': 300, 'shipment_size.units': 0.075411,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Жарова, д. 41', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57956084', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.968454, 'point.lon': 40.970561, 'ref': '116029221',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.001,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица Кирякиных, д. 9', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57601463', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.476013, 'point.lon': 42.093446, 'ref': '115579509',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00525,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, Социалистическая улица, д. 35/2', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57848870', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 7500,
            'penalty.out_of_time.fixed': 30, 'penalty.out_of_time.minute': 1.0, 'point.lat': 56.986241, 'point.lon': 40.991734, 'ref': '115885450',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 300, 'shared_service_duration_s': 400, 'shipment_size.units': 0.0343,
            'shipment_size.weight_kg': 1, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, Постышева, д. 57/3', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57622645', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.020961, 'point.lon': 40.976031, 'ref': '115607588',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00864,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица Полка Нормандия-Неман, д. 7', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57341525', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.008293, 'point.lon': 40.97631, 'ref': '115239628',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 1e-06,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, Комсомольская улица, д. 17', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58013998', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 7500,
            'penalty.out_of_time.fixed': 30, 'penalty.out_of_time.minute': 1.0, 'point.lat': 56.96842, 'point.lon': 40.991842, 'ref': '116105695',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 300, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00024,
            'shipment_size.weight_kg': 1, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, Московский микрорайон, д. 8', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57928083', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.96602, 'point.lon': 40.985697, 'ref': '115989123',
            'required_tags': 'delivery,prepaid,bulky_cargo', 'service_duration_s': 900, 'shared_service_duration_s': 300, 'shipment_size.units': 0.359898,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Кудряшова, д. 71к1', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57943876', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.957089, 'point.lon': 40.9923, 'ref': '116012809',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.032292,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, проспект Текстильщиков, д. 109', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57093314_57099566_57472862_57541467_57541653_57632376_57648591_57655722_57801547_57864602_57870773',
            'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 82500, 'penalty.out_of_time.fixed': 330,
            'penalty.out_of_time.minute': 11.0, 'point.lat': 56.965176, 'point.lon': 40.99001,
            'ref': '114904442_114912192_115409201_115500867_115501069_115619838_115641293_115649770_115823437_115905323_115914274',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 900, 'shared_service_duration_s': 400, 'shipment_size.units': 0.036088,
            'shipment_size.weight_kg': 11, 'time_window': '10:00:00-14:00:00', 'title': 'г. Иваново, проспект Строителей, д. 25', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57499312', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.450351, 'point.lon': 42.099869, 'ref': '115444362',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.18496,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Щорса, д. 7', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_58094061_58095321', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.023474, 'point.lon': 40.99822, 'ref': '116214112_116211791',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 280, 'shared_service_duration_s': 400, 'shipment_size.units': 0.0121,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, 11-я Завокзальная улица, д. 42', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57929666_58044674', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 15000,
            'penalty.out_of_time.fixed': 60, 'penalty.out_of_time.minute': 2.0, 'point.lat': 57.010857, 'point.lon': 40.974118, 'ref': '115993753_116147039',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 360, 'shared_service_duration_s': 400, 'shipment_size.units': 0.01365,
            'shipment_size.weight_kg': 2, 'time_window': '08:00:00-14:00:00', 'title': 'г. Иваново, улица Громобоя, д. 11а', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57296451_57864207', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.983156, 'point.lon': 40.943207, 'ref': '115178939_115905040',
            'required_tags': 'delivery,prepaid,bulky_cargo', 'service_duration_s': 900, 'shared_service_duration_s': 300, 'shipment_size.units': 0.180352,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, 4-я Березниковская улица, д. 54/25', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57333999_57334136_57668297_57684740_57685033', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100,
            'penalty.drop': 5000, 'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.441188, 'point.lon': 42.164565,
            'ref': '115229665_115229664_115668160_115690522_115690523', 'required_tags': 'delivery,prepaid', 'service_duration_s': 700,
            'shared_service_duration_s': 400, 'shipment_size.units': 0.2768, 'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00',
            'title': 'г. Кинешма, улица имени М. Горького, д. 45', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_56823253_58098840', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.98936, 'point.lon': 40.985913, 'ref': '114538782_116219973',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 280, 'shared_service_duration_s': 400, 'shipment_size.units': 0.164105,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Шуйская улица, д. 21А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58038177', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.015547, 'point.lon': 40.96562, 'ref': '116138130',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.044064,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Дзержинского, д. 13', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57845748', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.009631, 'point.lon': 40.993917, 'ref': '115880980',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.0072,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Шошина, д. 15', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57962072', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.993998, 'point.lon': 41.025879, 'ref': '116037010',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.000675,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Каравайковой, д. 90', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58098662', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.961079, 'point.lon': 41.026841, 'ref': '116220066',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.055328,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, улица Куконковых, д. 150', 'type': 'delivery'
        }, {
            'hard_window': False,
            'id': 'm_55992371_56167044_56402000_56738380_56760651_56823513_56823720_57030165_57617997_57618553_57659667_57721582_57858653_57902633',
            'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 105000, 'penalty.out_of_time.fixed': 420,
            'penalty.out_of_time.minute': 14.0, 'point.lat': 57.454768, 'point.lon': 42.119524,
            'ref': '113419107_113656157_113973499_114425316_114453399_114538535_114538536_114819072_115599829_115599828_115655674_115736676_115897682_115956405',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 1080, 'shared_service_duration_s': 400, 'shipment_size.units': 0.137127,
            'shipment_size.weight_kg': 14, 'time_window': '11:00:00-15:00:00', 'title': 'г. Кинешма, улица Правды, д. 14/9', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57932136_58074370', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 15000,
            'penalty.out_of_time.fixed': 60, 'penalty.out_of_time.minute': 2.0, 'point.lat': 56.96143, 'point.lon': 41.013289, 'ref': '115996732_116187335',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 360, 'shared_service_duration_s': 400, 'shipment_size.units': 0.0266,
            'shipment_size.weight_kg': 2, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, проспект Текстильщиков, д. 7Г', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58065707', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.973213, 'point.lon': 41.048463, 'ref': '116175012',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.000375,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, 10-я Санаторная улица, д. 5А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57911244', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.463133, 'point.lon': 42.086996, 'ref': '115967942',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.07224,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Куйбышева, д. 5', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58068103', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.011715, 'point.lon': 40.984694, 'ref': '116178162',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.046624,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Громобоя, д. 27', 'type': 'delivery'
        }, {
            'hard_window': False,
            'id': 'm_57080064_57102825_57313934_57438479_57523032_57800292_57801120_57826590_57873111_57882339_57892091_57911813_57935228_57939781_58013349_58025231_58040859_58043040_58067194_58067383_58112590',
            'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 157500, 'penalty.out_of_time.fixed': 630,
            'penalty.out_of_time.minute': 21.0, 'point.lat': 56.966468, 'point.lon': 41.024492,
            'ref': '114885923_114915678_115201643_115365967_115477561_115821264_115822929_115856511_115916763_115929762_115943265_115968517_116000499_116006715_116104180_116120487_116141144_116144347_116177791_116178318_116238515',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 1500, 'shared_service_duration_s': 300, 'shipment_size.units': 0.799808,
            'shipment_size.weight_kg': 21, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, улица Куконковых, д. 141', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57559303_57689100_57925422_58031689_58122509', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100,
            'penalty.drop': 37500, 'penalty.out_of_time.fixed': 150, 'penalty.out_of_time.minute': 5.0, 'point.lat': 56.986781, 'point.lon': 40.978754,
            'ref': '115524767_115696767_115987010_116129411_116251092', 'required_tags': 'delivery,prepaid', 'service_duration_s': 540,
            'shared_service_duration_s': 400, 'shipment_size.units': 0.105965, 'shipment_size.weight_kg': 5, 'time_window': '09:00:00-14:00:00',
            'title': 'г. Иваново, Московская улица, д. 62', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57998934', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.962944, 'point.lon': 41.023867, 'ref': '116082052',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.000784,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, улица Куконковых, д. 142', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57793652', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.014958, 'point.lon': 40.973031, 'ref': '115813450',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.032224,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Октябрьская улица, д. 3/70', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57038579', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.02171, 'point.lon': 40.99407, 'ref': '114830270',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.000288,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, 4-й Завокзальный переулок, д. 25', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57334113', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.98296, 'point.lon': 41.014955, 'ref': '115229641',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.036,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица Смирнова, д. 100', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57703798', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.963789, 'point.lon': 40.955269, 'ref': '115713352',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.028125,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица Станкостроителей, д. 3Бс5', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57929697_57954519_57954714_58053492_58110059', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100,
            'penalty.drop': 37500, 'penalty.out_of_time.fixed': 150, 'penalty.out_of_time.minute': 5.0, 'point.lat': 56.98799, 'point.lon': 40.968727,
            'ref': '115993815_116026115_116026427_116158702_116235117', 'required_tags': 'delivery,prepaid', 'service_duration_s': 540,
            'shared_service_duration_s': 400, 'shipment_size.units': 0.040352, 'shipment_size.weight_kg': 5, 'time_window': '08:00:00-14:00:00',
            'title': 'г. Иваново, улица Богдана Хмельницкого, д. 17', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58126493', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.046146, 'point.lon': 40.955685, 'ref': '116257240',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.04368,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, 10-я Минеевская улица, д. 30', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58054174', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.989159, 'point.lon': 40.971863, 'ref': '116158955',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 1100, 'shared_service_duration_s': 400, 'shipment_size.units': 0.010296,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Богдана Хмельницкого, д. 9', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57351947', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.017306, 'point.lon': 41.012979, 'ref': '115253800',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.0015,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Свободы, д. 50А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57098203_57130189_57251334_57426559_57426918_57436592_57437016_57459588_57463951_57464228_57473048',
            'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 82500, 'penalty.out_of_time.fixed': 330,
            'penalty.out_of_time.minute': 11.0, 'point.lat': 57.017315, 'point.lon': 40.97023,
            'ref': '114910800_114955697_115119063_115349731_115349735_115363904_115363905_115390728_115398092_115398093_115409517',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 900, 'shared_service_duration_s': 400, 'shipment_size.units': 0.040247,
            'shipment_size.weight_kg': 11, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, проспект Ленина, д. 108', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57652176_57652339', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.992184, 'point.lon': 40.904786, 'ref': '115645545_115645546',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 280, 'shared_service_duration_s': 400, 'shipment_size.units': 0.003174,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Суздальская улица, д. 18б', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57063872_57064234', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.450593, 'point.lon': 42.094155, 'ref': '114864356_114864355',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 280, 'shared_service_duration_s': 400, 'shipment_size.units': 0.01325,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица имени Юрия Горохова, д. 14А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57296941_57426826', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.450351, 'point.lon': 42.099869, 'ref': '115179919_115351670',
            'required_tags': 'delivery,prepaid,bulky_cargo', 'service_duration_s': 900, 'shared_service_duration_s': 300, 'shipment_size.units': 0.28335,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Щорса, д. 7', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_55956782_56396376_56833394_57062642_57063674_57065015_57638772_57680377_57680483_57858074_57863485_57938133_57989913',
            'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 97500, 'penalty.out_of_time.fixed': 390,
            'penalty.out_of_time.minute': 13.0, 'point.lat': 57.460365, 'point.lon': 42.117539,
            'ref': '113370314_113965427_114552462_114861805_114864577_114865138_115628264_115684606_115684607_115897671_115904943_116004757_116072629',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 1020, 'shared_service_duration_s': 400, 'shipment_size.units': 0.325714,
            'shipment_size.weight_kg': 13, 'time_window': '11:00:00-15:00:00', 'title': 'г. Кинешма, Красноветкинская улица, д. 15', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58026551', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.018232, 'point.lon': 41.020292, 'ref': '116122182',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.01386,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Свободы, д. 34', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '56945529', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.454157, 'point.lon': 42.11249, 'ref': '114702991',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.002808,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Менделеева, д. 3', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58098844', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.011092, 'point.lon': 40.977415, 'ref': '116220188',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00204,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица Громобоя, д. 15', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58046392', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.983092, 'point.lon': 41.035293, 'ref': '116148981',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00594,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, 1-я Лагерная улица, д. 1', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58068449', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 7500,
            'penalty.out_of_time.fixed': 30, 'penalty.out_of_time.minute': 1.0, 'point.lat': 57.004393, 'point.lon': 40.931229, 'ref': '116178877',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 300, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00126,
            'shipment_size.weight_kg': 1, 'time_window': '10:00:00-14:00:00', 'title': 'г. Иваново, улица Шевченко, д. 14/30', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_58104461_58104653', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.962409, 'point.lon': 40.988662, 'ref': '116227366_116227367',
            'required_tags': 'delivery,prepaid,bulky_cargo', 'service_duration_s': 900, 'shared_service_duration_s': 300, 'shipment_size.units': 0.26805,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, проспект Строителей, д. 20', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57892853', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.045269, 'point.lon': 40.947986, 'ref': '115944173',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.0048,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, 2-я Петрозаводская улица, д. 1а', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57156024_57244270', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.46155, 'point.lon': 42.110262, 'ref': '114989756_115109082',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 280, 'shared_service_duration_s': 400, 'shipment_size.units': 0.034188,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Гагарина, д. 2В', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57917846', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.000421, 'point.lon': 40.9847, 'ref': '115977341',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.026197,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Шереметевский проспект, д. 21', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57685203_57685206', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.991306, 'point.lon': 40.931529, 'ref': '115690597_115690595',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 280, 'shared_service_duration_s': 400, 'shipment_size.units': 0.004704,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Ивановская область, городской округ Иваново, Шахтинский проезд, д. 79',
            'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57808493', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.004025, 'point.lon': 40.927601, 'ref': '115832850',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.003024,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Красных Зорь, д. 34А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57296030', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.969308, 'point.lon': 40.9771, 'ref': 'LO-73703236',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.004807,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица Мякишева, д. 10, стр. , к.', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57996356', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.968189, 'point.lon': 40.9726, 'ref': '116081921',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.0015,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, Лежневская улица, д. 140', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57004465_57500843', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.443643, 'point.lon': 42.108295, 'ref': '114783648_115447039',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 280, 'shared_service_duration_s': 400, 'shipment_size.units': 0.08717,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, микрорайон Автоагрегат улица Щорса, д. 1Б', 'type': 'delivery'
        }, {
            'hard_window': False,
            'id': 'm_56953219_56953406_56991900_57000789_57152465_57152908_57349696_57350071_57350138_57350145_57350258_57350268_57350303_57357797_57517270_57517412_57529211_57704227_57704238_57713739_57714173_57805043_57805120_57866459',
            'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000, 'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5,
            'point.lat': 57.413339, 'point.lon': 42.10949,
            'ref': '114713448_114713450_114766727_114778697_114986099_114986100_115251303_115251301_115251305_115251302_115251304_115251299_115251300_115261570_115469655_115469654_LO-73947985_115713562_115713561_115726649_115726648_115827935_115827937_115908549',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 4860, 'shared_service_duration_s': 400, 'shipment_size.units': 0.303932,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, Авиационная улица, д. 27', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58106638', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 7500,
            'penalty.out_of_time.fixed': 30, 'penalty.out_of_time.minute': 1.0, 'point.lat': 56.965893, 'point.lon': 41.003017, 'ref': '116229635',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 300, 'shared_service_duration_s': 400, 'shipment_size.units': 0.014904,
            'shipment_size.weight_kg': 1, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, проспект Строителей, д. 35', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '56855133', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.429217, 'point.lon': 42.094676, 'ref': '114581471',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.048,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, Вичугская улица, д. 184А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58035602', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.98754, 'point.lon': 40.977334, 'ref': '116133678',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.015444,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Володарского, д. 1', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57063916_57906774', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.986324, 'point.lon': 40.979705, 'ref': '114863878_115963438',
            'required_tags': 'delivery,postpaid,prepaid', 'service_duration_s': 280, 'shared_service_duration_s': 400, 'shipment_size.units': 0.069612,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Типографская улица, д. 6', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57905893_57906156', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.01672, 'point.lon': 40.955379, 'ref': '115962229_115962228',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 280, 'shared_service_duration_s': 400, 'shipment_size.units': 0.13472,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Тимирязева, д. 1', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '56944075', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.464155, 'point.lon': 42.108502, 'ref': '114702030',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.0018,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Желябова, д. 5', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57586050_57586073', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.985496, 'point.lon': 41.029293, 'ref': '115560032_115560031',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 280, 'shared_service_duration_s': 400, 'shipment_size.units': 0.008277,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, 14-й проезд, д. 4', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57801682', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.991826, 'point.lon': 40.974612, 'ref': '115824063',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.139664,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица Багаева, д. 25/1', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57549982', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.960127, 'point.lon': 41.024774, 'ref': 'LO-73970961',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.0084,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, Кавалерийская улица, д. 50, стр. , к. ', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_56983667_57282482_57342695_57342960_57714309', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100,
            'penalty.drop': 37500, 'penalty.out_of_time.fixed': 150, 'penalty.out_of_time.minute': 5.0, 'point.lat': 56.968254, 'point.lon': 40.974136,
            'ref': '114754896_115161381_115241181_115241183_115724345', 'required_tags': 'delivery,prepaid', 'service_duration_s': 540,
            'shared_service_duration_s': 400, 'shipment_size.units': 0.016315, 'shipment_size.weight_kg': 5, 'time_window': '09:00:00-13:00:00',
            'title': 'г. Иваново, Лежневская улица, д. 167', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57111994_57670050_57808882', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.432221, 'point.lon': 42.155295, 'ref': '114930542_115670211_115833453',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 420, 'shared_service_duration_s': 400, 'shipment_size.units': 0.025732,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Верещагина, д. 14', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57483287', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.958194, 'point.lon': 41.010419, 'ref': 'LO-73898690',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.018772,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица Шубиных, д. 26А, стр. , к. ', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57597758_57597921', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.977726, 'point.lon': 41.048813, 'ref': '115575031_115575028',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 280, 'shared_service_duration_s': 400, 'shipment_size.units': 0.001412,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, 7-я Санаторная улица, д. 3', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57409649', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.427298, 'point.lon': 42.182802, 'ref': '115328969',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.000588,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Герцена, д. 31', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57760952', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.97001, 'point.lon': 40.987458, 'ref': '115778855',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00204,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, микрорайон Московский, д. 19', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58112714', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.952661, 'point.lon': 41.056215, 'ref': '116238040',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.000468,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, Кохомское шоссе, д. 17', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58119086', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.001705, 'point.lon': 40.965961, 'ref': '116247095',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.003315,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица Зверева, д. 15', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57929387', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.960471, 'point.lon': 41.025448, 'ref': '115992681',
            'required_tags': 'delivery,prepaid,bulky_cargo', 'service_duration_s': 900, 'shared_service_duration_s': 300, 'shipment_size.units': 0.18,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, улица Куконковых, д. 150А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '56571032', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 7500,
            'penalty.out_of_time.fixed': 30, 'penalty.out_of_time.minute': 1.0, 'point.lat': 56.968626, 'point.lon': 41.010266, 'ref': '114197333',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 300, 'shared_service_duration_s': 400, 'shipment_size.units': 0.18183,
            'shipment_size.weight_kg': 1, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, проспект Строителей, д. 59', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57682836', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.029568, 'point.lon': 40.977334, 'ref': '115687496',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00748,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Люлина, д. 6', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_58075120_58095067_58113686', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.961702, 'point.lon': 41.024873, 'ref': '116187929_116215712_116240177',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 420, 'shared_service_duration_s': 400, 'shipment_size.units': 0.010316,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, Куконковых, д. 146', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58036932', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.964499, 'point.lon': 41.021387, 'ref': '116135817',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.040755,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, улица Куконковых, д. 126', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57104523_57357928', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.453964, 'point.lon': 42.111781, 'ref': 'LO-73499932_LO-73768612',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 280, 'shared_service_duration_s': 400, 'shipment_size.units': 0.009049,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Менделеева, д. 3А, стр. , к. ', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57941530_57941731', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.963047, 'point.lon': 41.010599, 'ref': '116009410_116009409',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 280, 'shared_service_duration_s': 400, 'shipment_size.units': 0.01626,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица Шубиных, д. 19', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57399846', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 7500,
            'penalty.out_of_time.fixed': 30, 'penalty.out_of_time.minute': 1.0, 'point.lat': 57.000372, 'point.lon': 40.930204, 'ref': '115316120',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 300, 'shared_service_duration_s': 400, 'shipment_size.units': 0.001875,
            'shipment_size.weight_kg': 1, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, улица Шевченко, д. 1', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57543926', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.464155, 'point.lon': 42.108502, 'ref': 'LO-73963757',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00504,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Желябова, д. 5, стр. , к. ', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '56735644', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.44592, 'point.lon': 42.099114, 'ref': '114420502',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.12,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, Шуйская улица, д. 1', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57322496', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.982538, 'point.lon': 40.974451, 'ref': 'LO-73731483',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.049504,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, Велижская улица, д. 12, стр. , к. ', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57220616_57220819_57220906', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.975116, 'point.lon': 41.002729, 'ref': '115077061_115077059_115077060',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 420, 'shared_service_duration_s': 400, 'shipment_size.units': 0.002478,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, 2-я улица Чапаева, д. 40А', 'type': 'delivery'
        }, {
            'hard_window': False,
            'id': 'm_56903709_57293572_57550930_57640113_57653779_57670021_57715352_57839991_57840527_57883800_57886939_57906537_58019290_58070936_58075721_58090265_58100812_58117804_58119599_58120125_58129200_58130000',
            'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 165000, 'penalty.out_of_time.fixed': 660,
            'penalty.out_of_time.minute': 22.0, 'point.lat': 56.998387, 'point.lon': 40.966734,
            'ref': '114646894_115175680_115513957_115629666_115647879_115668875_115728250_115874006_115874007_115932583_115935997_115962098_116112693_116182407_116189093_116208861_116222403_116244861_116247871_116248103_116260416_116261515',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 1560, 'shared_service_duration_s': 400, 'shipment_size.units': 0.17763,
            'shipment_size.weight_kg': 22, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, улица Кузнецова, д. 8', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58093466', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.993856, 'point.lon': 40.977954, 'ref': '116213183',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00429,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Палехская улица, д. 6', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57147552', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.012787, 'point.lon': 40.949917, 'ref': '114978975',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.03654,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Поляковой, д. 8Б', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57501534', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.995053, 'point.lon': 40.978062, 'ref': '115447326',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.035235,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица Красной Армии, д. 1', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57876039', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.048927, 'point.lon': 40.929759, 'ref': '115920992',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.02706,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, 2-я Парковская улица, д. 81', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57397737', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.965476, 'point.lon': 40.970893, 'ref': '115313950',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.017,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Лежневская улица, д. 152', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57622819_57622933_57623001', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.991719, 'point.lon': 41.035994, 'ref': '115607572_115607575_115607574',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 420, 'shared_service_duration_s': 400, 'shipment_size.units': 0.032051,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, 9-й проезд, д. 56', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_56880461_58033081_58041527_58071743', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100,
            'penalty.drop': 30000, 'penalty.out_of_time.fixed': 120, 'penalty.out_of_time.minute': 4.0, 'point.lat': 56.96289, 'point.lon': 41.01702,
            'ref': '114615708_116131252_116141674_116183507', 'required_tags': 'delivery,prepaid', 'service_duration_s': 480, 'shared_service_duration_s': 400,
            'shipment_size.units': 0.023386, 'shipment_size.weight_kg': 4, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, проспект Текстильщиков, д. 3',
            'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_56273732_56436736_56565934_57617348', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100,
            'penalty.drop': 30000, 'penalty.out_of_time.fixed': 120, 'penalty.out_of_time.minute': 4.0, 'point.lat': 57.435995, 'point.lon': 42.21152,
            'ref': '113798679_114017297_114189999_115600442', 'required_tags': 'delivery,prepaid', 'service_duration_s': 480, 'shared_service_duration_s': 400,
            'shipment_size.units': 0.068484, 'shipment_size.weight_kg': 4, 'time_window': '09:00:00-18:00:00', 'title': 'г. Кинешма, улица имени Урицкого, д. 2',
            'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58114188', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.999798, 'point.lon': 40.994815, 'ref': '116241118',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.045496,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, улица Марии Рябининой, д. 32', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57434643_57576920_58079128_58114270', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100,
            'penalty.drop': 30000, 'penalty.out_of_time.fixed': 120, 'penalty.out_of_time.minute': 4.0, 'point.lat': 56.98601, 'point.lon': 41.008083,
            'ref': '115360632_115547823_116193385_116240563', 'required_tags': 'delivery,prepaid', 'service_duration_s': 480, 'shared_service_duration_s': 400,
            'shipment_size.units': 0.022318, 'shipment_size.weight_kg': 4, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, улица Смирнова, д. 105',
            'type': 'delivery'
        }, {
            'hard_window': False,
            'id': 'm_57064510_57240806_57598087_57673276_57706756_57708926_57819295_57828572_57919540_57947968_57980040_57993860_58007905_58018039_58042173_58042767_58048893_58051807_58066866',
            'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 142500, 'penalty.out_of_time.fixed': 570,
            'penalty.out_of_time.minute': 19.0, 'point.lat': 56.995036, 'point.lon': 40.978003,
            'ref': '114864559_115104181_115575480_115674644_115716974_115718704_115847767_115858979_115979582_116018598_116059953_116078557_116096172_116110487_116143432_116144440_116151376_116155487_116177314',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 1380, 'shared_service_duration_s': 300, 'shipment_size.units': 1.310765,
            'shipment_size.weight_kg': 19, 'time_window': '10:00:00-14:00:00', 'title': 'г. Иваново, улица Красной Армии, д. 1', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57529572', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.99521, 'point.lon': 41.041654, 'ref': '115486584',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.10716,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Окуловой, д. 68б', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57645983', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.982356, 'point.lon': 40.976301, 'ref': '115637753',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.0135,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Велижская улица, д. 10', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_58031030_58065880', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 15000,
            'penalty.out_of_time.fixed': 60, 'penalty.out_of_time.minute': 2.0, 'point.lat': 56.967248, 'point.lon': 40.985518, 'ref': '116127946_116175401',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 360, 'shared_service_duration_s': 400, 'shipment_size.units': 0.043419,
            'shipment_size.weight_kg': 2, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, Московский микрорайон, д. 15', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58125498', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 7500,
            'penalty.out_of_time.fixed': 30, 'penalty.out_of_time.minute': 1.0, 'point.lat': 56.989875, 'point.lon': 41.036228, 'ref': '116255437',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 300, 'shared_service_duration_s': 400, 'shipment_size.units': 0.05382,
            'shipment_size.weight_kg': 1, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, 11-й проезд, д. 1', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57948249', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.444142, 'point.lon': 42.151333, 'ref': '116018632',
            'required_tags': 'delivery,postpaid,bulky_cargo', 'service_duration_s': 900, 'shared_service_duration_s': 300, 'shipment_size.units': 0.349272,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Гоголя, д. 9', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58068119', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.021083, 'point.lon': 41.022465, 'ref': '116178194',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.003016,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица Талка, д. 10', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57550243', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.967139, 'point.lon': 40.994483, 'ref': '115513077',
            'required_tags': 'delivery,prepaid,bulky_cargo', 'service_duration_s': 900, 'shared_service_duration_s': 300, 'shipment_size.units': 0.343,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, д. 4', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57688498', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.972536, 'point.lon': 41.005083, 'ref': '115695759',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.21016,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, Генерала Хлебникова., д. 60', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57637247', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.005309, 'point.lon': 40.972663, 'ref': 'LO-74061347',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00336,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, проспект Ленина, д. 23, стр. , к. ', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58062934', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.024047, 'point.lon': 40.961463, 'ref': '116171257',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.012274,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, улица Юрия Гагарина, д. 15', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57613252', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.453964, 'point.lon': 42.111781, 'ref': 'LO-74036257',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00336,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Менделеева, д. 3А, стр. , к. ', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57219846', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.997779, 'point.lon': 40.972779, 'ref': '115076530',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.01875,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Степанова, д. 17', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57826317', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.456796, 'point.lon': 42.123117, 'ref': '115855715',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00075,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, Красноветкинская улица, д. 1', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58069332', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.023322, 'point.lon': 40.961551, 'ref': '116180391',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00081,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица Тимирязева, д. 39', 'type': 'delivery'
        }, {
            'hard_window': False,
            'id': 'm_56988110_57289714_57515838_57574880_57605067_57680518_57822060_57846846_57883246_57904966_57926914_57931086_57933159_57936851_57947447_57959929_57960205_57983018_57992749_57993004_58003897_58031328_58066686_58075673_58080535_58080784_58081087_58085071_58086668_58096402_58111611_58119213_58121296',
            'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 247500, 'penalty.out_of_time.fixed': 990,
            'penalty.out_of_time.minute': 33.0, 'point.lat': 56.997691, 'point.lon': 40.951759,
            'ref': '114760598_115170080_115466797_115545801_115583633_115684221_115851057_115882695_115931243_115960359_115989621_115995599_115997989_116002790_116017472_116033767_116033766_116061180_116076898_116076899_116091701_116128301_116177073_116189130_116195421_116196672_116196673_116202133_116203910_116216352_116236642_116246446_116250197',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 2520, 'shared_service_duration_s': 400, 'shipment_size.units': 0.360558,
            'shipment_size.weight_kg': 33, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, улица Парижской Коммуны, д. 7А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57117516_57117631_57661387_57661465', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.457222, 'point.lon': 42.090823,
            'ref': '114937433_114937432_115658455_115658454', 'required_tags': 'delivery,prepaid', 'service_duration_s': 560, 'shared_service_duration_s': 400,
            'shipment_size.units': 0.146763, 'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Рубинского, д. 20',
            'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57292901', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.010553, 'point.lon': 40.976373, 'ref': '115174324',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.0009,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Громобоя, д. 18', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58111830', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.44313, 'point.lon': 42.159679, 'ref': '116237213',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.000765,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, Комсомольская улица, д. 48', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57349662_57445111', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.01486, 'point.lon': 41.007239, 'ref': '115250562_115373244',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 280, 'shared_service_duration_s': 400, 'shipment_size.units': 0.027606,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, улица Свободы, д. 45А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57560543', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.987212, 'point.lon': 40.999998, 'ref': '115526839',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00015,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Смирнова, д. 78', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57114523', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.025733, 'point.lon': 40.95493, 'ref': '114933602',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.009,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, Зубчатая улица, д. 21', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58064374', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.451785, 'point.lon': 42.121015, 'ref': '116173506',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.032,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Декабристов, д. 17А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57177487_57177738', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.452278, 'point.lon': 42.095754, 'ref': '115019322_115019323',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 280, 'shared_service_duration_s': 400, 'shipment_size.units': 0.031988,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Щорса, д. 13А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58041990', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 7500,
            'penalty.out_of_time.fixed': 30, 'penalty.out_of_time.minute': 1.0, 'point.lat': 56.98724, 'point.lon': 41.02917, 'ref': '116143535',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 300, 'shared_service_duration_s': 400, 'shipment_size.units': 0.028392,
            'shipment_size.weight_kg': 1, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, 1-я Меланжевая улица, д. 5А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57082813', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.002191, 'point.lon': 40.950968, 'ref': 'LO-73476421',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.009,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Красных Зорь, д. 16', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_55966730_56103080_56530512_56795573_56972901_57478299_57478659_57571600_57649537_57864997_58062842',
            'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 82500, 'penalty.out_of_time.fixed': 330,
            'penalty.out_of_time.minute': 11.0, 'point.lat': 57.444245, 'point.lon': 42.16002,
            'ref': '113380575_113569956_114141198_114502187_114739534_115416998_115416999_115541501_115642141_115906832_116170632',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 900, 'shared_service_duration_s': 400, 'shipment_size.units': 0.079954,
            'shipment_size.weight_kg': 11, 'time_window': '11:00:00-15:00:00', 'title': 'г. Кинешма, улица имени Ленина, д. 41', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57879104', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 7500,
            'penalty.out_of_time.fixed': 30, 'penalty.out_of_time.minute': 1.0, 'point.lat': 57.001087, 'point.lon': 40.993552, 'ref': '115924705',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 300, 'shared_service_duration_s': 400, 'shipment_size.units': 0.0004,
            'shipment_size.weight_kg': 1, 'time_window': '10:00:00-14:00:00', 'title': 'г. Иваново, улица Ванцетти, д. 18', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58036015', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.992204, 'point.lon': 40.9953, 'ref': '116134974',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.018,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, улица Колотилова, д. 10', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57511943', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.985299, 'point.lon': 40.929112, 'ref': '115462076',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.0033,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, Силикатная улица, д. 52', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57229207', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.435516, 'point.lon': 42.210308, 'ref': 'LO-73632237',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 1e-06,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица имени Урицкого, д. 2А, стр. , к. ', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57693840', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.005798, 'point.lon': 40.99239, 'ref': '115701278',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.0012,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Шереметевский проспект, д. 58', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57803212', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.990733, 'point.lon': 41.026687, 'ref': '115824285',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.144,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, улица Каравайковой, д. 114', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57356673', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.038316, 'point.lon': 41.016752, 'ref': '115259820',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00342,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Строкинская улица, д. 10', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57918525_57918902', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.005499, 'point.lon': 40.950852, 'ref': '115977388_115977387',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 280, 'shared_service_duration_s': 400, 'shipment_size.units': 0.003015,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица Кузнецова, д. 67Бк1', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57851861_57933479_58027760_58046004_58089592', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100,
            'penalty.drop': 37500, 'penalty.out_of_time.fixed': 150, 'penalty.out_of_time.minute': 5.0, 'point.lat': 56.949383, 'point.lon': 41.051229,
            'ref': '115889144_115998801_116123574_116148045_116208441', 'required_tags': 'delivery,prepaid', 'service_duration_s': 540,
            'shared_service_duration_s': 400, 'shipment_size.units': 0.030542, 'shipment_size.weight_kg': 5, 'time_window': '10:00:00-14:00:00',
            'title': 'г. Иваново, Кохомское шоссе, д. 4А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57666154_58018748', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 15000,
            'penalty.out_of_time.fixed': 60, 'penalty.out_of_time.minute': 2.0, 'point.lat': 56.971825, 'point.lon': 41.011776, 'ref': '115664897_116111370',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 360, 'shared_service_duration_s': 400, 'shipment_size.units': 0.25872,
            'shipment_size.weight_kg': 2, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, улица Куконковых, д. 90', 'type': 'delivery'
        }, {
            'hard_window': False,
            'id': 'm_57060713_57294272_57501808_57614025_57742530_57803298_57864361_57864729_57866155_57884065_57958206_57964631_58038892_58039985_58040307_58045649_58047402_58068554_58111423_58125554_58130453',
            'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 157500, 'penalty.out_of_time.fixed': 630,
            'penalty.out_of_time.minute': 21.0, 'point.lat': 56.995402, 'point.lon': 40.998912,
            'ref': '114859850_115176972_115447949_115595959_115758764_115826217_115905797_115905536_115908197_115932492_116031321_116040107_116139107_116140597_116140598_116148172_116149760_116179422_116236472_116255684_116261754',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 1500, 'shared_service_duration_s': 400, 'shipment_size.units': 0.178583,
            'shipment_size.weight_kg': 21, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, улица Колотилова, д. 38', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57173088', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 7500,
            'penalty.out_of_time.fixed': 30, 'penalty.out_of_time.minute': 1.0, 'point.lat': 57.008911, 'point.lon': 40.995606, 'ref': '115013634',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 300, 'shared_service_duration_s': 400, 'shipment_size.units': 0.002904,
            'shipment_size.weight_kg': 1, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, улица Шошина, д. 19', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57177999', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.429658, 'point.lon': 42.101522, 'ref': '115020564',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.03528,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, Пригородная улица, д. 4', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57599421', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.970756, 'point.lon': 40.98859, 'ref': 'LO-74021240',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00336,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, , д. 21, стр. , к. ', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_56966445_56966446', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.955749, 'point.lon': 40.998381, 'ref': '114730833_114730832',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 280, 'shared_service_duration_s': 400, 'shipment_size.units': 0.03575,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Ломовская улица, д. 16', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57880576_57881205', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.970966, 'point.lon': 41.000304, 'ref': '115927559_115927561',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 280, 'shared_service_duration_s': 400, 'shipment_size.units': 0.004663,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, 30 Микрорайон, д. 53', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58029997', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.966266, 'point.lon': 40.989012, 'ref': '116127255',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.026598,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, Мкр московский, д. 14а корп2', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58044662', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.987918, 'point.lon': 41.028538, 'ref': '116146732',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.000616,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица Каравайковой, д. 141', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57264906', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.968189, 'point.lon': 40.9726, 'ref': '115137326',
            'required_tags': 'delivery,prepaid,bulky_cargo', 'service_duration_s': 900, 'shared_service_duration_s': 300, 'shipment_size.units': 0.28314,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Лежневская улица, д. 140', 'type': 'delivery'
        }, {
            'hard_window': False,
            'id': 'm_57097795_57545507_57576284_57582754_57601047_57614422_57619498_57620505_57792771_57799279_57821929_57847938_57912314_57917217_57918118_57961782_58016241_58018881_58023199_58028156_58033140_58055629_58064757_58096594_58104161_58108789_58125578',
            'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 202500, 'penalty.out_of_time.fixed': 810,
            'penalty.out_of_time.minute': 27.0, 'point.lat': 57.016096, 'point.lon': 40.972564,
            'ref': '114911272_115506024_115547762_115555801_115578892_115596486_115603948_115603969_115811534_115820458_115850108_115883243_115969592_115975829_115977598_116036496_116108585_116111736_116118253_116124358_116131228_116161013_116174284_116217275_116226336_116233725_116256056',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 1860, 'shared_service_duration_s': 400, 'shipment_size.units': 0.295133,
            'shipment_size.weight_kg': 27, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, улица Карла Маркса, д. 8', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '56913306', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 7500,
            'penalty.out_of_time.fixed': 30, 'penalty.out_of_time.minute': 1.0, 'point.lat': 56.959455, 'point.lon': 40.982452, 'ref': '114659346',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 300, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00297,
            'shipment_size.weight_kg': 1, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, проспект Текстильщиков, д. 80', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57436783', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 7500,
            'penalty.out_of_time.fixed': 30, 'penalty.out_of_time.minute': 1.0, 'point.lat': 57.017693, 'point.lon': 40.96448, 'ref': '115363275',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 300, 'shared_service_duration_s': 400, 'shipment_size.units': 0.001,
            'shipment_size.weight_kg': 1, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, улица Ермака, д. 11', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57712430', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.992209, 'point.lon': 41.001157, 'ref': '115724780',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00022,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, Пролетарская улица, д. 20', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57893925', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.045176, 'point.lon': 40.952954, 'ref': '115945554',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.032,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, 5-я Кубанская улица, д. 56', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57414802_57477873_57886388_58035419_58066863_58127321', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100,
            'penalty.drop': 45000, 'penalty.out_of_time.fixed': 180, 'penalty.out_of_time.minute': 6.0, 'point.lat': 56.995106, 'point.lon': 40.983356,
            'ref': '115335837_115415981_115935929_116133720_116177147_116257327', 'required_tags': 'delivery,prepaid', 'service_duration_s': 600,
            'shared_service_duration_s': 400, 'shipment_size.units': 0.022798, 'shipment_size.weight_kg': 6, 'time_window': '10:00:00-14:00:00',
            'title': 'г. Иваново, площадь Революции, д. 8А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57389059', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.966786, 'point.lon': 40.992327, 'ref': '115302645',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00551,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Московский микрорайон, д. 5', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57342999_57343263_57343702', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.438494, 'point.lon': 42.125453, 'ref': '115242109_115242108_115242110',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 420, 'shared_service_duration_s': 400, 'shipment_size.units': 0.088262,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Красный Металлист, д. 8', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58033236', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.009229, 'point.lon': 40.980792, 'ref': '116129986',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.000432,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, улица Калинина, д. 12', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58122757', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.034026, 'point.lon': 40.919312, 'ref': '116251477',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.06804,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, Революционная улица, д. 24к3', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57395238', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.983053, 'point.lon': 40.939344, 'ref': '115309835',
            'required_tags': 'delivery,prepaid,bulky_cargo', 'service_duration_s': 900, 'shared_service_duration_s': 300, 'shipment_size.units': 0.165888,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, Силикатный переулок, д. 17', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57919353_57919807', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.029368, 'point.lon': 40.914694, 'ref': '115979427_115979428',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 280, 'shared_service_duration_s': 400, 'shipment_size.units': 0.009654,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Авдотьинская улица, д. 30', 'type': 'delivery'
        }, {
            'hard_window': False,
            'id': 'm_57270676_57365289_57517621_57587901_57617566_57629122_57640574_57716167_57829686_57888772_57908164_57915694_57966252_58022988_58039536_58054970_58061478_58068364_58094906_58128029',
            'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 150000, 'penalty.out_of_time.fixed': 600,
            'penalty.out_of_time.minute': 20.0, 'point.lat': 56.981234, 'point.lon': 40.966968,
            'ref': '115145919_115271129_115469428_115561459_115601002_115615601_115630992_115729397_115860446_115939056_115964750_115974624_116042606_116117706_116139402_116160915_116169333_116179357_116214383_116258316',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 1440, 'shared_service_duration_s': 400, 'shipment_size.units': 0.283808,
            'shipment_size.weight_kg': 20, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, Ташкентская улица, д. 64', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57503639', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.033947, 'point.lon': 41.031421, 'ref': '115450660',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.006479,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, Плесская, д. 37', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_56843742_57509839_57844094_57902917_58033563_58033851', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100,
            'penalty.drop': 45000, 'penalty.out_of_time.fixed': 180, 'penalty.out_of_time.minute': 6.0, 'point.lat': 56.998333, 'point.lon': 40.986327,
            'ref': '114566190_115459303_115879079_115956036_116131966_116131967', 'required_tags': 'delivery,prepaid', 'service_duration_s': 600,
            'shared_service_duration_s': 400, 'shipment_size.units': 0.021056, 'shipment_size.weight_kg': 6, 'time_window': '08:00:00-14:00:00',
            'title': 'г. Иваново, улица Арсения, д. 20', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57154794', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.968317, 'point.lon': 40.994492, 'ref': '114988621',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.058097,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, д. 2', 'type': 'delivery'
        }, {
            'hard_window': False,
            'id': 'm_57075430_57239187_57268322_57329290_57390697_57435153_57450157_57455238_57529854_57549717_57598011_57628736_57632125_57635937_57673260_57685275_57714224_57725663_57836905_57845249_57849897_57870380_57888532_57898746_57899023_57899807_57902902_57905482_57930276_57930522_57934705_57967761_57986005_58003418_58013369_58018099_58019841_58021724_58024016_58024776_58024810_58028425_58029223_58030654_58036241_58038690_58077479_58080651',
            'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 360000, 'penalty.out_of_time.fixed': 1440,
            'penalty.out_of_time.minute': 48.0, 'point.lat': 56.975329, 'point.lon': 40.976196,
            'ref': '114879288_115103105_115142046_115223122_115305045_115361682_115379867_115386312_115486961_115511518_115574959_115614699_115619559_115624664_115674904_115690825_115726722_115740606_115869022_115879790_115887079_115913512_115938315_115951974_115951973_115952508_115956389_115961450_115994453_115994409_115999950_116044690_116067708_116089726_116104493_116110845_116113177_116115282_116118471_116120003_116119565_116124348_116125465_116128020_116135109_116138940_116191788_116195958',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 3120, 'shared_service_duration_s': 400, 'shipment_size.units': 0.692357,
            'shipment_size.weight_kg': 48, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, Лежневская улица, д. 117', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57795318', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.999711, 'point.lon': 40.98188, 'ref': '115814991',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.162,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Пушкина, д. 7', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_56749771_57097323_57606905_57816830_57988837_58112603', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100,
            'penalty.drop': 45000, 'penalty.out_of_time.fixed': 180, 'penalty.out_of_time.minute': 6.0, 'point.lat': 56.965477, 'point.lon': 40.970894,
            'ref': '114438259_114909815_115586323_115843383_116071099_116237559', 'required_tags': 'delivery,prepaid', 'service_duration_s': 600,
            'shared_service_duration_s': 400, 'shipment_size.units': 0.009255, 'shipment_size.weight_kg': 6, 'time_window': '08:00:00-14:00:00',
            'title': 'г. Иваново, Лежневская улица, д. 152', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57222722', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.975097, 'point.lon': 40.976274, 'ref': '115079767',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.005022,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Лежневская улица, д. 117', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57462940', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.964808, 'point.lon': 40.945192, 'ref': '115396641',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00144,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Станкостроителей, д. 5Б', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58061694', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.003553, 'point.lon': 40.978026, 'ref': '116170105',
            'required_tags': 'delivery,prepaid,bulky_cargo', 'service_duration_s': 900, 'shared_service_duration_s': 300, 'shipment_size.units': 0.925806,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, Набережная улица, д. 7', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57880162', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.414755, 'point.lon': 42.111862, 'ref': '115926660',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.023064,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Ивана Виноградова, д. 31', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57709515_58030981_58036050_58048449', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100,
            'penalty.drop': 30000, 'penalty.out_of_time.fixed': 120, 'penalty.out_of_time.minute': 4.0, 'point.lat': 57.032194, 'point.lon': 40.92525,
            'ref': '115720546_116128417_116130542_116151607', 'required_tags': 'delivery,prepaid', 'service_duration_s': 480, 'shared_service_duration_s': 400,
            'shipment_size.units': 0.026946, 'shipment_size.weight_kg': 4, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, Дюковская улица, д. 38А',
            'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57874957', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 7500,
            'penalty.out_of_time.fixed': 30, 'penalty.out_of_time.minute': 1.0, 'point.lat': 56.967037, 'point.lon': 40.98672, 'ref': '115919477',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 300, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00117,
            'shipment_size.weight_kg': 1, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, микрорайон Московский, д. 15', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57863989', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 7500,
            'penalty.out_of_time.fixed': 30, 'penalty.out_of_time.minute': 1.0, 'point.lat': 56.962767, 'point.lon': 40.997825, 'ref': '115905665',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 300, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00016,
            'shipment_size.weight_kg': 1, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, проспект Строителей, д. 50А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57524976', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.976578, 'point.lon': 40.983568, 'ref': '115480349',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.005112,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, 2-я Полётная улица, д. 20', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57944995', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.972202, 'point.lon': 40.978996, 'ref': '116014243',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00098,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, 1-ая Полевая, д. 35А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57282061', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.43334, 'point.lon': 42.211862, 'ref': '115160661',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.08424,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица имени Урицкого, д. 21', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '56954971', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.961339, 'point.lon': 41.002532, 'ref': '114715975',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.033306,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица Шубиных, д. 10', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57281334_57305922_57449350_58044398_58074749_58127145', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100,
            'penalty.drop': 45000, 'penalty.out_of_time.fixed': 180, 'penalty.out_of_time.minute': 6.0, 'point.lat': 57.033654, 'point.lon': 40.918585,
            'ref': '115159212_115191737_115378774_116146073_116187477_116256762', 'required_tags': 'delivery,prepaid', 'service_duration_s': 600,
            'shared_service_duration_s': 400, 'shipment_size.units': 0.043007, 'shipment_size.weight_kg': 6, 'time_window': '09:00:00-14:00:00',
            'title': 'г. Иваново, Революционная улица, д. 24, к. 1', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57677899_57808134_57865138_57935679_58079088_58111234_58117136_58124104', 'optional_tags.0.tag': 'reg218204',
            'optional_tags.0.value': 100, 'penalty.drop': 60000, 'penalty.out_of_time.fixed': 240, 'penalty.out_of_time.minute': 8.0, 'point.lat': 57.017008,
            'point.lon': 41.019938, 'ref': '115679644_115831687_115905422_116001626_116194399_116236439_116244505_116253161', 'required_tags': 'delivery,prepaid',
            'service_duration_s': 720, 'shared_service_duration_s': 400, 'shipment_size.units': 0.110098, 'shipment_size.weight_kg': 8,
            'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, улица Калинцева, д. 4', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '56995310', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 7500,
            'penalty.out_of_time.fixed': 30, 'penalty.out_of_time.minute': 1.0, 'point.lat': 56.957477, 'point.lon': 40.998804, 'ref': '114770528',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 300, 'shared_service_duration_s': 400, 'shipment_size.units': 0.002261,
            'shipment_size.weight_kg': 1, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, проспект Текстильщиков, д. 38', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57492696', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.993847, 'point.lon': 40.983694, 'ref': '115436661',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00693,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, улица Смирнова, д. 11', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57953149', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.012258, 'point.lon': 40.929822, 'ref': '116024591',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.01196,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, Неждановская улица, д. 45', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57659324', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.001769, 'point.lon': 40.981843, 'ref': '115655317',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00264,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Крутицкая улица, д. 20А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57649513_57684005_57758667', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 22500,
            'penalty.out_of_time.fixed': 90, 'penalty.out_of_time.minute': 3.0, 'point.lat': 56.989062, 'point.lon': 40.936119, 'ref': '115641309_115688821_115775126',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 420, 'shared_service_duration_s': 400, 'shipment_size.units': 0.067128,
            'shipment_size.weight_kg': 3, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, Бакинский проезд, д. 55А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57668594_57834188', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 15000,
            'penalty.out_of_time.fixed': 60, 'penalty.out_of_time.minute': 2.0, 'point.lat': 57.00985, 'point.lon': 40.943386, 'ref': '115667963_115864746',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 360, 'shared_service_duration_s': 400, 'shipment_size.units': 0.172832,
            'shipment_size.weight_kg': 2, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, улица Кузнецова, д. 112', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57951673', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 7500,
            'penalty.out_of_time.fixed': 30, 'penalty.out_of_time.minute': 1.0, 'point.lat': 57.432978, 'point.lon': 42.162696, 'ref': '116022658',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 300, 'shared_service_duration_s': 400, 'shipment_size.units': 0.00035,
            'shipment_size.weight_kg': 1, 'time_window': '09:00:00-18:00:00', 'title': 'г. Кинешма, улица Котовского, д. 2', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57987359', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.421221, 'point.lon': 42.187626, 'ref': '116069816',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.18704,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Аккуратова, д. 60', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57848701_57868098_58060406_58128254', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100,
            'penalty.drop': 30000, 'penalty.out_of_time.fixed': 120, 'penalty.out_of_time.minute': 4.0, 'point.lat': 57.030678, 'point.lon': 40.984314,
            'ref': '115884761_115911128_116167475_116258912', 'required_tags': 'delivery,prepaid', 'service_duration_s': 480, 'shared_service_duration_s': 400,
            'shipment_size.units': 0.001882, 'shipment_size.weight_kg': 4, 'time_window': '10:00:00-14:00:00', 'title': 'г. Иваново, улица Степана Халтурина, д. 19А',
            'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57439953_57941164', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 15000,
            'penalty.out_of_time.fixed': 60, 'penalty.out_of_time.minute': 2.0, 'point.lat': 57.006756, 'point.lon': 40.979595, 'ref': '115367829_116008801',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 360, 'shared_service_duration_s': 400, 'shipment_size.units': 0.017,
            'shipment_size.weight_kg': 2, 'time_window': '10:00:00-14:00:00', 'title': 'г. Иваново, улица Калинина, д. 5', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57400674_57508074', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 15000,
            'penalty.out_of_time.fixed': 60, 'penalty.out_of_time.minute': 2.0, 'point.lat': 57.036323, 'point.lon': 40.984386, 'ref': '115317838_115457053',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 360, 'shared_service_duration_s': 400, 'shipment_size.units': 0.016215,
            'shipment_size.weight_kg': 2, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, 4-я Деревенская улица, д. 34', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57867752', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.009685, 'point.lon': 40.958874, 'ref': '115910357',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.0156,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица Наговицыной-Икрянистовой, д. 6Д', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57328527', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.456046, 'point.lon': 42.127861, 'ref': '115221354',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.02254,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Ивановская область, городской округ Кинешма, Лесозаводская улица, д. 23 в',
            'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57618967', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.95356, 'point.lon': 41.053448, 'ref': '115602576',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.1488,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, Кохомское шоссе, д. 14', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57251250', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.435165, 'point.lon': 42.234417, 'ref': '115119178',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.01275,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Аристарха Макарова, д. 108', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57640005', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.426096, 'point.lon': 42.166874, 'ref': '115629394',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.008,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, Высоковольтная улица, д. 35А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58073941', 'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.996387, 'point.lon': 40.992516, 'ref': '116186780',
            'required_tags': 'delivery,postpaid,bulky_cargo', 'service_duration_s': 900, 'shared_service_duration_s': 300, 'shipment_size.units': 0.317376,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, улица Арсения, д. 57', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57455410', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.998828, 'point.lon': 40.954266, 'ref': '115386526',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.0715,
            'shipment_size.weight_kg': 0, 'time_window': '14:00:00-22:00:00', 'title': 'г. Иваново, улица Герцена, д. 7', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '56976267', 'optional_tags.0.tag': 'reg218203', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.019613, 'point.lon': 40.967327, 'ref': 'LO-73362883',
            'required_tags': 'delivery,postpaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.0084,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, улица Академика Мальцева, д. 4, стр. , к.', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57233624', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.432187, 'point.lon': 42.099258, 'ref': '115094605',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.0176,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Бекренёва, д. 1', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_56946259_57134309_57934465', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 22500,
            'penalty.out_of_time.fixed': 90, 'penalty.out_of_time.minute': 3.0, 'point.lat': 56.96105, 'point.lon': 40.972879, 'ref': '114704370_114959591_115998951',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 420, 'shared_service_duration_s': 400, 'shipment_size.units': 0.062035,
            'shipment_size.weight_kg': 3, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, Лежневская, д. 160А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': 'm_57497276_57576748_57616655_57618063_57640606_57681622_57681854_57774803_57818403_57819542_57955530_58066716_58069028',
            'optional_tags.0.tag': 'reg218205', 'optional_tags.0.value': 100, 'penalty.drop': 97500, 'penalty.out_of_time.fixed': 390,
            'penalty.out_of_time.minute': 13.0, 'point.lat': 56.962914, 'point.lon': 40.99656,
            'ref': '115442562_115547930_115599832_115601175_115630144_115685632_115686536_115793228_115845651_115847466_116027499_116176243_116179627',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 1020, 'shared_service_duration_s': 400, 'shipment_size.units': 0.033164,
            'shipment_size.weight_kg': 13, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, проспект Строителей, д. 50А', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58106912', 'optional_tags.0.tag': 'reg-1', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.968665, 'point.lon': 40.999414, 'ref': '116230523',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.01512,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, Микрорайон 30, д. 18', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58117541', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.981602, 'point.lon': 40.976176, 'ref': '116244677',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.000882,
            'shipment_size.weight_kg': 0, 'time_window': '10:00:00-18:00:00', 'title': 'г. Иваново, Велижская улица, д. 3', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '55969850', 'optional_tags.0.tag': 'reg10689', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 57.451334, 'point.lon': 42.099833, 'ref': '113386553',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.004872,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Кинешма, улица Щорса, д. 64', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '58074188', 'optional_tags.0.tag': 'reg218204', 'optional_tags.0.value': 100, 'penalty.drop': 7500,
            'penalty.out_of_time.fixed': 30, 'penalty.out_of_time.minute': 1.0, 'point.lat': 56.954231, 'point.lon': 41.103781, 'ref': '116187439',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 300, 'shared_service_duration_s': 400, 'shipment_size.units': 0.002088,
            'shipment_size.weight_kg': 1, 'time_window': '09:00:00-14:00:00', 'title': 'г. Иваново, Микрорайон Тэц-3, д. 7', 'type': 'delivery'
        }, {
            'hard_window': False, 'id': '57498016', 'optional_tags.0.tag': 'reg218202', 'optional_tags.0.value': 100, 'penalty.drop': 5000,
            'penalty.out_of_time.fixed': 20, 'penalty.out_of_time.minute': 0.5, 'point.lat': 56.998122, 'point.lon': 40.964012, 'ref': '115442049',
            'required_tags': 'delivery,prepaid', 'service_duration_s': 200, 'shared_service_duration_s': 400, 'shipment_size.units': 0.002128,
            'shipment_size.weight_kg': 0, 'time_window': '09:00:00-22:00:00', 'title': 'г. Иваново, Яблочная улица, д. 5', 'type': 'delivery'
        }, {
            'hard_window': True, 'id': '536672', 'optional_tags.0.tag': 'reg5', 'optional_tags.0.value': 100, 'penalty.drop': 5000, 'penalty.out_of_time.fixed': 20,
            'penalty.out_of_time.minute': 0.5, 'point.lat': 56.995035, 'point.lon': 40.978003, 'ref': 'dropships=TMM13469705', 'required_tags': 'dropship',
            'service_duration_s': 600, 'shared_service_duration_s': 400, 'shipment_size.units': 0.012236, 'shipment_size.weight_kg': 0,
            'time_window': '14:00:00-20:00:00', 'title': 'г. Иваново, улица Красной Армии, д. 1', 'type': 'pickup'
        }, {
            'hard_window': True, 'id': '536705', 'optional_tags.0.tag': 'reg5', 'optional_tags.0.value': 100, 'penalty.drop': 5000, 'penalty.out_of_time.fixed': 20,
            'penalty.out_of_time.minute': 0.5, 'point.lat': 56.975329, 'point.lon': 40.976196, 'ref': 'dropships=TMM13469701', 'required_tags': 'dropship',
            'service_duration_s': 600, 'shared_service_duration_s': 400, 'shipment_size.units': 0.006, 'shipment_size.weight_kg': 0,
            'time_window': '14:00:00-20:00:00', 'title': 'г. Иваново, Лежневская улица, д. 117', 'type': 'pickup'
        }, {
            'hard_window': True, 'id': '536965', 'optional_tags.0.tag': 'reg5', 'optional_tags.0.value': 100, 'penalty.drop': 5000, 'penalty.out_of_time.fixed': 20,
            'penalty.out_of_time.minute': 0.5, 'point.lat': 56.981234, 'point.lon': 40.966968, 'ref': 'dropships=TMM13469708', 'required_tags': 'dropship',
            'service_duration_s': 600, 'shared_service_duration_s': 400, 'shipment_size.units': 0.0168, 'shipment_size.weight_kg': 0,
            'time_window': '14:00:00-20:00:00', 'title': 'г. Иваново, Ташкентская улица, д. 64', 'type': 'pickup'
        }, {
            'hard_window': True, 'id': '537224', 'optional_tags.0.tag': 'reg5', 'optional_tags.0.value': 100, 'penalty.drop': 5000, 'penalty.out_of_time.fixed': 20,
            'penalty.out_of_time.minute': 0.5, 'point.lat': 56.995401, 'point.lon': 40.998911, 'ref': 'dropships=TMM13469706', 'required_tags': 'dropship',
            'service_duration_s': 600, 'shared_service_duration_s': 400, 'shipment_size.units': 0.066658, 'shipment_size.weight_kg': 0,
            'time_window': '14:00:00-20:00:00', 'title': 'г. Иваново, улица Колотилова, д. 38', 'type': 'pickup'
        }], 'Vehicles': [{
            'capacity.limits.units_perc': 80, 'capacity.units': 6.0,
            'cost': 'max(2800 + (distance_km > 110)*500 + (distance_km > 200)*1000 + (distance_km > 300)*1000 + (distance_km > 400)*1200 + (distance_km > 500)*1200 + (distance_km > 600)*1200, stops*(140 + (distance_km > 110) * 0 + (distance_km > 200) * 15 + (distance_km > 300) * 0 + (distance_km > 400) * 10 + (distance_km > 500) * 0 + (distance_km > 600) * 0) + total_weight_kg*5)',
            'excluded_tags': '', 'id': 31050, 'max_runs': 1, 'ref': 'Бахарев Алексей-1348654287-Ford Transit Яндекс', 'return_to_depot': True,
            'routing_mode': 'driving', 'service_duration_multiplier': 1, 'shared_service_duration_multiplier': 1, 'shifts.0.balanced_group_id': 'Car',
            'shifts.0.hard_window': False, 'shifts.0.minimal_unique_stops': 1, 'shifts.0.penalty.out_of_time.fixed': 0,
            'shifts.0.penalty.out_of_time.minute': 1, 'shifts.0.penalty.stop_lack.per_stop': 1000, 'shifts.0.penalty.unique_stop_lack.per_stop': 1000,
            'shifts.0.time_window': '09:00:00-19:00:00', 'tags': 'carOnly,regular_cargo,delivery,pvz,postpaid,lavka,prepaid,medium_size_cargo,locker',
            'travel_time_multiplier': 1, 'visit_depot_at_start': True
        }, {
            'capacity.limits.units_perc': 80, 'capacity.units': 1.6,
            'cost': 'max(2800 + (distance_km > 110)*500 + (distance_km > 200)*1000 + (distance_km > 300)*1000 + (distance_km > 400)*1200 + (distance_km > 500)*1200 + (distance_km > 600)*1200, stops*(140 + (distance_km > 110) * 0 + (distance_km > 200) * 15 + (distance_km > 300) * 0 + (distance_km > 400) * 10 + (distance_km > 500) * 0 + (distance_km > 600) * 0) + total_weight_kg*5)',
            'excluded_tags': '', 'id': 20214, 'max_runs': 1, 'ref': 'Назаров Алексей-7729634-Subaru Forester Кроссовер', 'return_to_depot': True,
            'routing_mode': 'driving', 'service_duration_multiplier': 1, 'shared_service_duration_multiplier': 1, 'shifts.0.balanced_group_id': 'Car',
            'shifts.0.hard_window': False, 'shifts.0.minimal_unique_stops': 1, 'shifts.0.penalty.out_of_time.fixed': 0,
            'shifts.0.penalty.out_of_time.minute': 1, 'shifts.0.penalty.stop_lack.per_stop': 1000, 'shifts.0.penalty.unique_stop_lack.per_stop': 1000,
            'shifts.0.time_window': '09:00:00-19:00:00', 'tags': 'carOnly,regular_cargo,delivery,postpaid,client,prepaid',
            'travel_time_multiplier': 1, 'visit_depot_at_start': True
        }, {
            'capacity.limits.units_perc': 80, 'capacity.units': 6.0,
            'cost': 'max(2800 + (distance_km > 110)*500 + (distance_km > 200)*1000 + (distance_km > 300)*1000 + (distance_km > 400)*1200 + (distance_km > 500)*1200 + (distance_km > 600)*1200, stops*(140 + (distance_km > 110) * 0 + (distance_km > 200) * 15 + (distance_km > 300) * 0 + (distance_km > 400) * 10 + (distance_km > 500) * 0 + (distance_km > 600) * 0) + total_weight_kg*5)',
            'excluded_tags': '', 'id': 22873, 'max_runs': 1, 'ref': 'Павлов Александр-1517617758-Ford Transit Яндекс', 'return_to_depot': True,
            'routing_mode': 'driving', 'service_duration_multiplier': 1, 'shared_service_duration_multiplier': 1, 'shifts.0.balanced_group_id': 'Car',
            'shifts.0.hard_window': False, 'shifts.0.minimal_unique_stops': 1, 'shifts.0.penalty.out_of_time.fixed': 0,
            'shifts.0.penalty.out_of_time.minute': 1, 'shifts.0.penalty.stop_lack.per_stop': 1000, 'shifts.0.penalty.unique_stop_lack.per_stop': 1000,
            'shifts.0.time_window': '09:00:00-19:00:00', 'tags': 'carOnly,regular_cargo,delivery,pvz,postpaid,lavka,prepaid,medium_size_cargo,locker',
            'travel_time_multiplier': 1, 'visit_depot_at_start': True
        }, {
            'capacity.limits.units_perc': 80, 'capacity.units': 6.0,
            'cost': 'max(2800 + (distance_km > 110)*500 + (distance_km > 200)*1000 + (distance_km > 300)*1000 + (distance_km > 400)*1200 + (distance_km > 500)*1200 + (distance_km > 600)*1200, stops*(140 + (distance_km > 110) * 0 + (distance_km > 200) * 15 + (distance_km > 300) * 0 + (distance_km > 400) * 10 + (distance_km > 500) * 0 + (distance_km > 600) * 0) + total_weight_kg*5)',
            'excluded_tags': '', 'id': 19339, 'max_runs': 1, 'ref': 'Панов Дмитрий-1469434542-Ford Transit Яндекс', 'return_to_depot': True,
            'routing_mode': 'driving', 'service_duration_multiplier': 1, 'shared_service_duration_multiplier': 1, 'shifts.0.balanced_group_id': 'Car',
            'shifts.0.hard_window': False, 'shifts.0.minimal_unique_stops': 1, 'shifts.0.penalty.out_of_time.fixed': 0,
            'shifts.0.penalty.out_of_time.minute': 1, 'shifts.0.penalty.stop_lack.per_stop': 1000, 'shifts.0.penalty.unique_stop_lack.per_stop': 1000,
            'shifts.0.time_window': '09:00:00-19:00:00', 'tags': 'carOnly,regular_cargo,delivery,pvz,postpaid,lavka,prepaid,medium_size_cargo,locker',
            'travel_time_multiplier': 1, 'visit_depot_at_start': True
        }, {
            'capacity.limits.units_perc': 80, 'capacity.units': 6.0,
            'cost': 'max(2800 + (distance_km > 110)*500 + (distance_km > 200)*1000 + (distance_km > 300)*1000 + (distance_km > 400)*1200 + (distance_km > 500)*1200 + (distance_km > 600)*1200, stops*(140 + (distance_km > 110) * 0 + (distance_km > 200) * 15 + (distance_km > 300) * 0 + (distance_km > 400) * 10 + (distance_km > 500) * 0 + (distance_km > 600) * 0) + total_weight_kg*5)',
            'excluded_tags': '', 'id': 22876, 'max_runs': 1, 'ref': 'Сироткин Максим-1517481586-Ford Transit Яндекс', 'return_to_depot': True,
            'routing_mode': 'driving', 'service_duration_multiplier': 1, 'shared_service_duration_multiplier': 1, 'shifts.0.balanced_group_id': 'Car',
            'shifts.0.hard_window': False, 'shifts.0.minimal_unique_stops': 1, 'shifts.0.penalty.out_of_time.fixed': 0,
            'shifts.0.penalty.out_of_time.minute': 1, 'shifts.0.penalty.stop_lack.per_stop': 1000, 'shifts.0.penalty.unique_stop_lack.per_stop': 1000,
            'shifts.0.time_window': '09:00:00-19:00:00', 'tags': 'carOnly,regular_cargo,delivery,pvz,postpaid,lavka,prepaid,medium_size_cargo,locker',
            'travel_time_multiplier': 1, 'visit_depot_at_start': True
        }, {
            'capacity.limits.units_perc': 80, 'capacity.units': 6.0,
            'cost': 'max(2800 + (distance_km > 110)*500 + (distance_km > 200)*1000 + (distance_km > 300)*1000 + (distance_km > 400)*1200 + (distance_km > 500)*1200 + (distance_km > 600)*1200, stops*(140 + (distance_km > 110) * 0 + (distance_km > 200) * 15 + (distance_km > 300) * 0 + (distance_km > 400) * 10 + (distance_km > 500) * 0 + (distance_km > 600) * 0) + total_weight_kg*5)',
            'excluded_tags': '', 'id': 30503, 'max_runs': 1, 'ref': 'Асафьев Евгений-1639714453-Ford Transit Яндекс', 'return_to_depot': True,
            'routing_mode': 'driving', 'service_duration_multiplier': 1, 'shared_service_duration_multiplier': 1, 'shifts.0.balanced_group_id': 'Car',
            'shifts.0.hard_window': False, 'shifts.0.minimal_unique_stops': 1, 'shifts.0.penalty.out_of_time.fixed': 0,
            'shifts.0.penalty.out_of_time.minute': 1, 'shifts.0.penalty.stop_lack.per_stop': 1000, 'shifts.0.penalty.unique_stop_lack.per_stop': 1000,
            'shifts.0.time_window': '09:00:00-19:00:00', 'tags': 'carOnly,regular_cargo,delivery,pvz,postpaid,lavka,prepaid,medium_size_cargo,locker',
            'travel_time_multiplier': 1, 'visit_depot_at_start': True
        }, {
            'capacity.limits.units_perc': 80, 'capacity.units': 9.0,
            'cost': 'max(5100 + (distance_km > 150)*1100 + (distance_km > 250)*1100 + (distance_km > 350)*1100 + (distance_km > 450)*1100 + (distance_km > 550)*1100 + (distance_km > 650)*1100,stops*(460 + Min(120, Ceil(Max(0, distance_km - 150)/100)*20) + total_weight_kg*10000))',
            'excluded_tags': '', 'id': 27991, 'max_runs': 1, 'ref': 'Васильев Григорий-1416242795-КГТ Доставка', 'return_to_depot': True,
            'routing_mode': 'driving', 'service_duration_multiplier': 1, 'shared_service_duration_multiplier': 1, 'shifts.0.balanced_group_id': 'Car',
            'shifts.0.hard_window': False, 'shifts.0.minimal_unique_stops': 1, 'shifts.0.penalty.out_of_time.fixed': 0,
            'shifts.0.penalty.out_of_time.minute': 1, 'shifts.0.penalty.stop_lack.per_stop': 1000, 'shifts.0.penalty.unique_stop_lack.per_stop': 1000,
            'shifts.0.time_window': '09:00:00-19:00:00', 'tags': 'carOnly,delivery,postpaid,client,prepaid,bulky_cargo', 'travel_time_multiplier': 1,
            'visit_depot_at_start': True
        }, {
            'capacity.limits.units_perc': 80, 'capacity.units': 2.5,
            'cost': 'max(2800 + (distance_km > 110)*500 + (distance_km > 200)*1000 + (distance_km > 300)*1000 + (distance_km > 400)*1200 + (distance_km > 500)*1200 + (distance_km > 600)*1200, stops*(140 + (distance_km > 110) * 0 + (distance_km > 200) * 15 + (distance_km > 300) * 0 + (distance_km > 400) * 10 + (distance_km > 500) * 0 + (distance_km > 600) * 0) + total_weight_kg*5)',
            'excluded_tags': '', 'id': 16861, 'max_runs': 1, 'ref': 'Носко Денис-450347823-Lada Largus Фургон', 'return_to_depot': True,
            'routing_mode': 'driving', 'service_duration_multiplier': 1, 'shared_service_duration_multiplier': 1, 'shifts.0.balanced_group_id': 'Car',
            'shifts.0.hard_window': False, 'shifts.0.minimal_unique_stops': 1, 'shifts.0.penalty.out_of_time.fixed': 0,
            'shifts.0.penalty.out_of_time.minute': 1, 'shifts.0.penalty.stop_lack.per_stop': 1000, 'shifts.0.penalty.unique_stop_lack.per_stop': 1000,
            'shifts.0.time_window': '09:00:00-19:00:00', 'tags': 'carOnly,regular_cargo,delivery,postpaid,client,prepaid,medium_size_cargo',
            'travel_time_multiplier': 1, 'visit_depot_at_start': True
        }, {
            'capacity.limits.units_perc': 80, 'capacity.units': 2.5,
            'cost': 'max(2800 + (distance_km > 110)*500 + (distance_km > 200)*1000 + (distance_km > 300)*1000 + (distance_km > 400)*1200 + (distance_km > 500)*1200 + (distance_km > 600)*1200, stops*(140 + (distance_km > 110) * 0 + (distance_km > 200) * 15 + (distance_km > 300) * 0 + (distance_km > 400) * 10 + (distance_km > 500) * 0 + (distance_km > 600) * 0) + total_weight_kg*5)',
            'excluded_tags': '', 'id': 26275, 'max_runs': 1, 'ref': 'Дадынин Сергей-1556363909-Lada Largus Фургон', 'return_to_depot': True,
            'routing_mode': 'driving', 'service_duration_multiplier': 1, 'shared_service_duration_multiplier': 1, 'shifts.0.balanced_group_id': 'Car',
            'shifts.0.hard_window': False, 'shifts.0.minimal_unique_stops': 1, 'shifts.0.penalty.out_of_time.fixed': 0,
            'shifts.0.penalty.out_of_time.minute': 1, 'shifts.0.penalty.stop_lack.per_stop': 1000, 'shifts.0.penalty.unique_stop_lack.per_stop': 1000,
            'shifts.0.time_window': '09:00:00-19:00:00', 'tags': 'carOnly,regular_cargo,delivery,postpaid,client,prepaid,medium_size_cargo',
            'travel_time_multiplier': 1, 'visit_depot_at_start': True
        }, {
            'capacity.limits.units_perc': 80, 'capacity.units': 6.0,
            'cost': 'max(2800 + (distance_km > 110)*500 + (distance_km > 200)*1000 + (distance_km > 300)*1000 + (distance_km > 400)*1200 + (distance_km > 500)*1200 + (distance_km > 600)*1200, stops*(140 + (distance_km > 110) * 0 + (distance_km > 200) * 15 + (distance_km > 300) * 0 + (distance_km > 400) * 10 + (distance_km > 500) * 0 + (distance_km > 600) * 0) + total_weight_kg*5)',
            'excluded_tags': '', 'id': 14939, 'max_runs': 1, 'ref': 'Рындин Сергей-1282932237-Ford Transit Яндекс', 'return_to_depot': True,
            'routing_mode': 'driving', 'service_duration_multiplier': 1, 'shared_service_duration_multiplier': 1, 'shifts.0.balanced_group_id': 'Car',
            'shifts.0.hard_window': False, 'shifts.0.minimal_unique_stops': 1, 'shifts.0.penalty.out_of_time.fixed': 0,
            'shifts.0.penalty.out_of_time.minute': 1, 'shifts.0.penalty.stop_lack.per_stop': 1000, 'shifts.0.penalty.unique_stop_lack.per_stop': 1000,
            'shifts.0.time_window': '09:00:00-19:00:00', 'tags': 'carOnly,regular_cargo,delivery,pvz,postpaid,lavka,prepaid,medium_size_cargo,locker',
            'travel_time_multiplier': 1, 'visit_depot_at_start': True
        }], 'Depot': [{
            'finish_service_duration_s': 300, 'flexible_start_time': False, 'hard_window': False, 'id': '888891180',
            'point.lat': 56.962973, 'point.lon': 40.947555, 'ref': 'depot-91180', 'service_duration_s': 300,
            'time_window': '08:30:00-23:59:00'
        }], 'Options': [{
            'absolute_time': False, 'avoid_tolls': True, 'balanced_groups.0.id': 'Car',
            'balanced_groups.0.penalty.hour': 50, 'balanced_groups.0.penalty.stop': 10, 'balanced_groups.1.id': 'Foot',
            'balanced_groups.1.penalty.hour': 10, 'balanced_groups.1.penalty.stop': 25, 'date': '2022-06-20',
            'global_proximity_factor': 1, 'merge_multiorders': False, 'minimize': 'cost',
            'minimize_lateness_risk': False, 'penalize_late_service': True, 'post_optimization': True,
            'quality': 'normal', 'restart_on_drop': False, 'routing_mode': 'driving', 'time_zone': 3,
            'weighted_drop_penalty': False, 'matrix_router': 'main'
        }], 'Location_Groups': []
    }

