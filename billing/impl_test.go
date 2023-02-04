package impl

import (
	"context"
	"net/http"
	"testing"

	"github.com/stretchr/testify/require"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/mediator/pkg/core"
	bmocks "a.yandex-team.ru/billing/library/go/billingo/mock"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

type testSuite struct {
	btesting.BaseSuite
	client          *bmocks.MockClientProtocol
	processorClient core.Processor
}

func (s *testSuite) SetupTest() {
	s.client = bmocks.NewMockClientProtocol(s.Ctrl())
	processorClient, err := NewClientWithClient(s.client)
	require.NoError(s.T(), err)

	s.processorClient = processorClient.(core.Processor)
}

func (s *testSuite) TestProcessorRequest() {
	ctx := context.Background()
	request := &core.ProcessorRequest{}
	response := &core.ProcessorResponse{Request: request}

	s.client.
		EXPECT().
		MakeRequest(ctx, interactions.Request{
			APIMethod: "/process",
			Method:    http.MethodPost,
			Headers: map[string]string{
				"Content-Type": "application/json",
			},
			Body: request,
			Name: "process",
		}, response).
		Return(nil).
		Times(1)

	s.processorClient.Process(ctx, request)
}

func TestProcessorClient(t *testing.T) {
	suite.Run(t, &testSuite{})
}
