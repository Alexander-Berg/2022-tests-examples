package providers

import (
	"encoding/json"
	"errors"
	"fmt"
	"sync"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"golang.org/x/sync/errgroup"

	"a.yandex-team.ru/billing/hot/accrualer/internal/config"
	"a.yandex-team.ru/billing/hot/accrualer/internal/entities"
	"a.yandex-team.ru/billing/hot/accrualer/internal/mocks"
)

const (
	defaultNamespace = "default"
)

func getProviders(configs ProviderConfigs) *Providers {
	providers := Providers{
		Lock:    sync.Mutex{},
		Writers: Writers{},
	}
	for conf := range configs {
		producer := &mocks.MockLogBrokerProducer{}
		providers.Writers[conf] = producer
	}
	return &providers
}

func (s *ProvidersTestSuite) TestSimpleOk() {
	notifier := config.ProviderConfig{Topic: "errors"}
	base := config.ProviderConfig{Topic: defaultNamespace}

	var empty any
	configs := ProviderConfigs{notifier: empty, base: empty}
	messages := NewProviderMessages(configs, 10)

	require.NoError(s.T(), messages.SaveMessage(base, "message", notifier, "TestUnknownProviderError", []byte("message"), nil))

	providers := getProviders(configs)
	require.NoError(s.T(), messages.FlushMessages(*s.ctx, providers))

	assert.Len(s.T(), providers.Writers[notifier].(*mocks.MockLogBrokerProducer).Messages, 0)
	require.Len(s.T(), providers.Writers[base].(*mocks.MockLogBrokerProducer).Messages, 1)
	message := providers.Writers[base].(*mocks.MockLogBrokerProducer).Messages[0]
	assert.Equal(s.T(), []byte("\"message\""), message)
}

func (s *ProvidersTestSuite) TestUnknownProviderError() {
	notifier := config.ProviderConfig{Topic: "errors"}
	base := config.ProviderConfig{Topic: defaultNamespace}

	var empty any
	configs := ProviderConfigs{notifier: empty, base: empty}
	messages := NewProviderMessages(configs, 10)

	require.NoError(s.T(), messages.SaveMessage(config.ProviderConfig{Topic: "other"}, "message", notifier, "TestUnknownProviderError", []byte("message"), nil))

	failed := messages[notifier].storage[0]
	var message entities.NotifierMessage
	require.NoError(s.T(), json.Unmarshal(failed.Data, &message))
	assert.Equal(s.T(), "message", message.Msg)
	assert.Equal(s.T(), "TestUnknownProviderError", message.Source)
	assert.Equal(s.T(), ErrorNoNamespaceProvider.Error(), message.Err)
}

func (s *ProvidersTestSuite) TestSendError() {
	sendErr := errors.New("send error")

	notifier := config.ProviderConfig{Topic: "errors"}
	base := config.ProviderConfig{Topic: defaultNamespace}

	var empty any
	configs := ProviderConfigs{notifier: empty, base: empty}
	messages := NewProviderMessages(configs, 10)
	require.NoError(s.T(), messages.SaveMessage(base, "message", notifier, "TestUnknownProviderError", []byte("message"), nil))

	providers := getProviders(configs)
	providers.Writers[base] = &mocks.MockLogBrokerProducer{Err: sendErr}
	require.Error(s.T(), messages.FlushMessages(*s.ctx, providers))
}

func (s *ProvidersTestSuite) TestMultipleMessages() {
	goroutines := 4
	messagePerGoroutine := 16
	topics := 8

	notifier := config.ProviderConfig{Topic: "errors"}

	var empty any
	configs := ProviderConfigs{notifier: empty}
	for i := 0; i < topics; i++ {
		topic := fmt.Sprintf("topic-%v", i)
		configs[config.ProviderConfig{Topic: topic}] = empty
	}

	messages := NewProviderMessages(configs, goroutines*messagePerGoroutine)

	g, _ := errgroup.WithContext(*s.ctx)
	for i := 0; i < goroutines; i++ {
		i := i
		g.Go(func() error {
			for j := 0; j < messagePerGoroutine; j++ {
				message := fmt.Sprintf("message-%v-%v", i, j)
				topic := fmt.Sprintf("topic-%v", (i+j)%topics)
				messages.StoreMessage(config.ProviderConfig{Topic: topic}, []byte(message))
			}
			return nil
		})
	}
	require.NoError(s.T(), g.Wait())

	providers := getProviders(configs)
	require.NoError(s.T(), messages.FlushMessages(*s.ctx, providers))

	assert.Len(s.T(), providers.Writers[notifier].(*mocks.MockLogBrokerProducer).Messages, 0)

	for i := 0; i < topics; i++ {
		topic := fmt.Sprintf("topic-%v", i)
		conf := config.ProviderConfig{Topic: topic}
		distinct := map[string]any{}
		for _, message := range providers.Writers[conf].(*mocks.MockLogBrokerProducer).Messages {
			if _, ok := distinct[string(message)]; ok {
				assert.Failf(s.T(), "message duplicates", "topic %s, message %s", topic, string(message))
			}
			distinct[string(message)] = empty
		}
		assert.Lenf(s.T(), distinct, goroutines*messagePerGoroutine/topics, "fail topic %s", topic)
	}
}
