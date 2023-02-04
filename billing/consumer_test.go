package consumer

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/stretchr/testify/suite"
	"golang.org/x/sync/errgroup"
	"gopkg.in/yaml.v2"

	"a.yandex-team.ru/billing/hot/mediator/pkg/commands"
	"a.yandex-team.ru/billing/hot/mediator/pkg/commands/consumer/mocks"
	"a.yandex-team.ru/billing/hot/mediator/pkg/core"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

type TestSuite struct {
	cmd ConsumerCommand
	btesting.BaseSuite
}

func (s *TestSuite) SetupTest() {
	ctx := context.Background()
	config := &core.Config{}

	manifestYml := `
consumers:
  - consumer:
      consumer: /payments/testing/consumer
      endpoint: lbkx.logbroker.yandex.net
      handler:
        client: Processor
        errorProducer:
          endpoint: lbkx.logbroker.yandex.net
          topic: /payments/testing/manual-write
          tvmId: 2001059
      topic: /payments/testing/manual
      tvmId: 2001059
    endpoint: revenue
    namespace: taxi-draft
  - consumer:
      consumer: /payments/testing/consumer
      endpoint: lbkx.logbroker.yandex.net
      handler:
        client: Processor
        errorProducer:
          endpoint: lbkx.logbroker.yandex.net
          topic: /payments/testing/manual-write
          tvmId: 2001059
      topic: /payments/testing/manual
      tvmId: 2001059
    endpoint: revenue
    namespace: taxi-draft`

	require.NoError(s.T(), yaml.Unmarshal([]byte(manifestYml), &config.Logbroker))

	s.cmd = NewConsumerCommand(config)
	if err := s.cmd.Init(ctx); err != nil {
		s.T().Fatal(err)
	}
}

func (s *TestSuite) TestCreateContext() {
	ctx := context.Background()

	extraContext, err := s.cmd.CreateContext(ctx)
	if err != nil {
		s.T().Fatal(err)
	}

	var mediatorContext = commands.CreateContextFrom(extraContext)
	assert.Equal(s.T(), mediatorContext.Logbroker, s.cmd.Logbroker)
}

func (s *TestSuite) TestRun() {
	_, ctx := errgroup.WithContext(s.cmd.Context())

	runner := mocks.NewMockConsumerRunner(s.Ctrl())

	runner.EXPECT().RunWebServer(ctx).Return(func() error { return nil }).Times(1)
	runner.EXPECT().RunConsumer(ctx, s.cmd.Logbroker.Consumers[0]).Return(func() error { return nil }).Times(1)
	runner.EXPECT().RunConsumer(ctx, s.cmd.Logbroker.Consumers[1]).Return(func() error { return nil }).Times(1)

	s.cmd.Runner = runner

	err := s.cmd.Run()
	if err != nil {
		s.T().Fatal(err)
	}
}

func TestConsumerTestSuite(t *testing.T) {
	suite.Run(t, new(TestSuite))
}
