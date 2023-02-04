# encoding: utf-8

from cppzoom import ZYasmConf


def test_load_yasm_conf():
    rawconf = ("" +
               "{ " +
               "\"conflist\": {" +
               "    \"addrsmeasure\": {" +
               "        \"patterns\": {}," +
               "        \"periods\": {}," +
               "        \"signals\": {}" +
               "     }," +
               "\"distributor\": {" +
               "    \"patterns\": {}," +
               "    \"periods\": {" +
               "        \"distributor_free_space-space-used-perc\": \"aver\"," +
               "        \"distributor_metrics-deposit\": \"summ\"," +
               "        \"distributor_metrics-requests\": \"summ\"" +
               "    }," +
               "    \"signals\": {" +
               "        \"distributor_free_space-space-used-perc\": [false, [\"aver\", true]]," +
               "        \"distributor_metrics-deposit\": [false, [\"summ\", true]]," +
               "        \"distributor_metrics-requests\": [false, [\"summ\", true]]" +
               "    }" +
               "}," +
               "\"common\": {" +
               "    \"patterns\": {" +
               "        \"iostat-max_load_(100|\\\\d\\\\d)\": [false, [\"summ\",true]]," +
               "        \"iostat-max_load_avg\": [false, [\"trnsp\", true]], " +
               "        \"iostat-max_load_max\": [false, [\"max\",true]]" +
               "    }," +
               "    \"periods\": {" +
               "        \"instances-discarded_instances\": \"summ\"," +
               "        \"instances-discarded_tags\": \"summ\"," +
               "        \"instances-ready_data_aggregated_size\": \"summ\"," +
               "        \"instances-ready_data_realtime_size\": \"summ\"," +
               "        \"instances-tags\": \"summ\"" +
               "    }," +
               "    \"signals\": {" +
               "        \"instances-discarded_instances\": [false, [\"summ\", true]]," +
               "        \"instances-discarded_tags\": [false, [\"summ\", true]]," +
               "        \"instances-ready_data_aggregated_size\": [false, [\"summ\", true]]," +
               "        \"instances-ready_data_realtime_size\": [false, [\"summ\", true]]," +
               "        \"instances-tags\": [false, [\"summ\", true]]" +
               "    }" +
               "  }" +
               " }" +
               "}")

    assert ZYasmConf(rawconf)
