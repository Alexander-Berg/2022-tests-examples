from mock import call


def test_logbroker(logbroker_auth_pqlib_mocks, auth_token):
    lb = logbroker_auth_pqlib_mocks
    # create an instance
    lb_a = lb.Logbroker.get_instance("a")
    lb.auth.OAuthTokenCredentialsProvider.assert_called_once_with(auth_token)
    # get the same instance
    assert lb.Logbroker.get_instance("a") == lb_a
    lb.auth.OAuthTokenCredentialsProvider.assert_called_once()
    # initialize and check mock calls
    lb_a.init()
    lb_a.api.start.assert_called_once()
    assert lb_a.api.start.return_value.mock_calls[0] == call.result(timeout=1)
    # create a producer
    producer = lb_a.get_producer("test_topic")
    assert producer.logbroker == lb_a
    assert producer.topic == "test_topic"
    # get the same producer
    assert lb_a.get_producer("test_topic") == producer
    # create a consumer
    consumer = lb_a.get_consumer("test_consumer", "test_topic")
    assert consumer.logbroker == lb_a
    assert consumer.name == "test_consumer"
    assert consumer.topic == "test_topic"
    # get the same consumer
    assert lb_a.get_consumer("test_consumer", "test_topic") == consumer
    # clean resources
    lb_a.clean_resources()
    lb_a.api.stop.assert_called_once()
