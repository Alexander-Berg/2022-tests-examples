package interactions

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/mediator/pkg/core"
	"a.yandex-team.ru/billing/hot/mediator/pkg/interactions/processor"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

type testSuite struct {
	btesting.BaseSuite
	clients *Clients
}

func (s *testSuite) SetupTest() {
	config := &core.Config{Debug: true}
	clients, err := NewClients(config, s.TVM(), s.Registry())
	require.NoError(s.T(), err)
	s.clients = clients
}

func (s *testSuite) TestGetProcessorClientErr() {
	_, err := s.clients.GetProcessorClient(btesting.RandS(16))
	require.Error(s.T(), err)
}

func (s *testSuite) TestGetProcessorClient() {
	params := []struct {
		Name   string
		Client processor.ProcessorClient
	}{
		{Name: "ProcessorMockSuccess", Client: s.clients.ProcessorMockSuccess},
		{Name: "processor", Client: s.clients.Processor},
		{Name: "Processor", Client: s.clients.Processor},
	}

	for _, param := range params {
		client, err := s.clients.GetProcessorClient(param.Name)
		require.NoError(s.T(), err)
		assert.Equal(s.T(), client, param.Client)
	}
}

func TestInteractions(t *testing.T) {
	suite.Run(t, &testSuite{})
}
