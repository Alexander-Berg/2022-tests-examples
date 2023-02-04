package logbroker

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/stretchr/testify/suite"
	"gopkg.in/yaml.v2"

	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

type testSuite struct {
	btesting.BaseSuite
}

func consumerToMap(consumer *LogbrokerConsumer) map[string]any {
	return map[string]any{
		"endpoint":  consumer.Endpoint,
		"namespace": consumer.Namespace,
		"handler": map[string]any{
			"client": consumer.Handler.Client,
		},
	}
}

func (s *testSuite) TestNewConsumer() {
	config := LogbrokerStorageConfig{}
	configYml := `
consumers:
  - consumer:
      consumer: consumer1
      endpoint: endpoint1
      handler:
        client: Processor
        errorProducer:
          endpoint: endpoint1
          topic: topic1
          tvmId: 1
      topic: /payments/testing/manual
      tvmId: 1
    endpoint: endpoint1
    namespace: namespace1
  - consumer:
      consumer: consumer2
      endpoint: endpoint2
      handler:
        client: Processor
        errorProducer:
          endpoint: endpoint2
          topic: topic2
          tvmId: 2
      topic: topic2
      tvmId: 2
    endpoint: endpoint2
    namespace: namespace2`

	require.NoError(s.T(), yaml.Unmarshal([]byte(configYml), &config))
	storage, err := NewStorage(context.Background(), config, s.TVM())
	require.NoError(s.T(), err)

	assert.Len(s.T(), storage.Consumers, 2)
	assert.Equal(
		s.T(),
		map[string]any{
			"consumer1": consumerToMap(storage.Consumers[0]),
			"consumer2": consumerToMap(storage.Consumers[1]),
		},
		map[string]any{
			"consumer1": map[string]any{
				"endpoint":  "endpoint1",
				"namespace": "namespace1",
				"handler": map[string]any{
					"client": "Processor",
				},
			},
			"consumer2": map[string]any{
				"endpoint":  "endpoint2",
				"namespace": "namespace2",
				"handler": map[string]any{
					"client": "Processor",
				},
			},
		},
	)
}

func TestConsumer(t *testing.T) {
	suite.Run(t, &testSuite{})
}
