const char* rerouted_back_test_txt = R"*8e74fd7f6e64*(
1493938706	guidance	set_route	timestamp	1493938706.845	route_id	2_0	properties	 	vehicle_type	0	vehicle_options	{}
1493938730	guidance	route_mismatch	timestamp	1493938730.0	route_id	2_0	length_left	130.0
1493938745	guidance	route_lost	timestamp	1493938745.183	length_left	130.8694152832	time_left	18.84671465526	route_id	2_0
1493938745	guidance	not_on_route	timestamp	1493938745.483	route_id	2_0
1493938747	guidance	reroute	timestamp	1493938747.439	parent_route_id	2_0	route_id	6_0	properties	 	vehicle_type	0	vehicle_options	{}
1493938747	guidance	on_route	timestamp	1493938747.509	route_id	6_0	length_left	419.4724731445	time_left	57.33311247052	manoeuvre_distance	44.1724395752
1493938853	guidance	rerouted_back	timestamp	1493938853.409	route_id	2_0
1493938853	guidance	reroute	timestamp	1493938853.709	parent_route_id	6_0	route_id	2_0	properties	
)*8e74fd7f6e64*";
