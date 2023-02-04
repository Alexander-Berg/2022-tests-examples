package copier

import (
	"context"
	"errors"
	"sync"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/require"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/mediator/pkg/commands"
	"a.yandex-team.ru/billing/hot/mediator/pkg/core"
	"a.yandex-team.ru/billing/library/go/billingo/mock"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/configops"
	configopstype "a.yandex-team.ru/billing/library/go/billingo/pkg/configops/type"
	configvars "a.yandex-team.ru/billing/library/go/billingo/pkg/configops/vars"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/extracontext"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/logbroker"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/kikimr/public/sdk/go/persqueue"
)

type testSuite struct {
	btesting.BaseSuite
	copier *copier
	batch  persqueue.MessageBatch

	ctx *commands.MediatorContext
}

// In general, we create 5 messages and send them to copier with different configs.
// Configs are set up in SetupTest funcs.
func (s *testSuite) SetupTest() {
	registry := s.SolomonRegistry()
	ctx := extracontext.NewWithParent(context.Background())
	cmd := commands.StorageCommand{Registry: registry}
	cmd.Config = &core.Config{}

	require.NoError(s.T(), cmd.Init(ctx))

	ctx, _ = cmd.CreateContext(ctx)
	s.ctx = commands.CreateContextFrom(ctx)
	s.copier = New(&Proxy{Registry: registry})

	mData := [][]byte{
		[]byte(`{"event": {"contract_id":1}}`),
		[]byte(`{"event": {"contract_id":2}}`),
		[]byte(`{"event": {"contract_id":3}}`),
		[]byte(`{"event": {"contract_id":4}}`),
		[]byte(`{"event": {"contract_id":5}}`),
	}

	var messages []persqueue.ReadMessage
	for _, mData := range mData {
		messages = append(messages, persqueue.ReadMessage{Data: mData})
	}

	s.batch = persqueue.MessageBatch{Messages: messages, Topic: btesting.RandS(16)}
}

type proxySuite struct {
	testSuite
}

func (s *proxySuite) SetupTest() {
	s.testSuite.SetupTest()

	// setup Proxy
	cntrl := gomock.NewController(s.T())
	fromMock := mock.NewMockConsumerProtocol(cntrl)

	var toMocks []logbroker.ProducerProtocol
	for i := 0; i < 5; i++ {
		toMocks = append(toMocks, mock.NewMockProducerProtocol(cntrl))
	}

	proxy := NewProxy(fromMock, toMocks)
	proxy.Registry = s.SolomonRegistry()
	s.copier.proxy = proxy
}

func (s *proxySuite) TestSuccess() {
	proxy := s.copier.proxy.(*Proxy)

	wg := sync.WaitGroup{}
	wg.Add(len(proxy.To) - 1)
	mu := sync.Mutex{}
	mu.Lock()

	for i, toMock := range proxy.To {
		i := i

		toMock.(*mock.MockProducerProtocol).EXPECT().WriteBatch(gomock.Any(), gomock.Any()).Do(
			func(_, d any) {
				if i+1 == len(proxy.To) {
					wg.Wait()
					mu.Unlock()
				} else {
					wg.Done()
					mu.Lock()   //nolint:SA2001
					mu.Unlock() //nolint:SA2001
				}
			},
		).Return(nil).Times(1)
	}

	err := s.copier.proxy.(*Proxy).handle(s.ctx, s.batch.Messages[0], s.batch)
	require.NoError(s.T(), err)
}

func (s *proxySuite) TestOneProducerClosed() {
	producerError := errors.New("producer is closed")
	tos := s.copier.proxy.(*Proxy).To

	wg := sync.WaitGroup{}
	wg.Add(len(tos) - 1)
	mu := sync.Mutex{}
	mu.Lock()

	for i, to := range tos {
		i := i

		call := to.(*mock.MockProducerProtocol).EXPECT().WriteBatch(gomock.Any(), gomock.Any()).Do(
			func(_, _ any) {
				if i == 0 {
					wg.Wait()
					wg.Add(1)
					mu.Unlock()
				} else {
					wg.Done()
					mu.Lock()   //nolint:SA2001
					mu.Unlock() //nolint:SA2001
				}
			},
		).MaxTimes(2)

		if i == 0 {
			call.Return(producerError)
		} else {
			call.Return(nil)
		}
	}

	err := s.copier.proxy.(*Proxy).handle(s.ctx, s.batch.Messages[0], s.batch)
	require.NoError(s.T(), err)
}

func (s *proxySuite) TestAllProducersClosed() {
	producerError := errors.New("producer is closed")

	for _, mockTo := range s.copier.proxy.(*Proxy).To {
		mockTo.(*mock.MockProducerProtocol).EXPECT().WriteBatch(gomock.Any(), gomock.Any()).Return(producerError).AnyTimes()
	}

	err := s.copier.proxy.(*Proxy).handle(s.ctx, s.batch.Messages[0], s.batch)
	require.Error(s.T(), err)
}

func TestProxy(t *testing.T) {
	suite.Run(t, &proxySuite{})
}

type matchedProxySuite struct {
	testSuite
	conditions     []configops.Logic
	variables      map[string]configvars.Value
	emptyCondition configops.Logic
}

func (s *matchedProxySuite) SetupTest() {
	s.testSuite.SetupTest()

	// Setup conditions, i := 1 to 5
	// eq:
	//   - $value
	//   - i
	var values []any

	for i := 1; i <= 5; i++ {
		values = append(values, "$value", i)
		eq, err := configops.NewEq(values)
		require.NoError(s.T(), err)
		s.conditions = append(s.conditions, eq)
		values = nil
	}

	// Setup variables:
	// vars:
	//     value:
	//          path: ["event", "contract_id"]
	//          type: int
	//          required: true

	variable := configvars.Value{
		Path:     []string{"event", "contract_id"},
		VType:    configopstype.ValueTypeInt,
		Required: true,
	}

	s.variables = map[string]configvars.Value{
		"value": variable,
	}

	s.emptyCondition = nil
}

type matchedProxySimpleSuite struct {
	matchedProxySuite
}

func (s *matchedProxySimpleSuite) SetupTest() {
	s.matchedProxySuite.SetupTest()

	cntrl := gomock.NewController(s.T())
	// setup Proxy
	fromMock := mock.NewMockConsumerProtocol(cntrl)

	matchers := make([]MatchedTopic, 0)
	toMocks := make([]logbroker.ProducerProtocol, 0)
	for i := 0; i < 5; i++ {
		toMocks = append(toMocks, mock.NewMockProducerProtocol(cntrl))
	}

	// Create one matching pattern:
	// match:
	//     condition:
	// 			eq:
	//   		   - $value
	//   		   - 1
	condition := s.conditions[0]

	matcher := NewMatchedTopic(toMocks, configops.BaseLogic{Logic: condition}, s.variables)
	matchers = append(matchers, matcher)

	proxy := NewMatchedProxy(fromMock, matchers)
	proxy.Registry = s.SolomonRegistry()
	s.testSuite.copier.proxy = proxy
}

func (s *matchedProxySimpleSuite) TestSimpleMatching() {
	for _, matchedTopic := range s.copier.proxy.(*MatchedProxy).To {
		for _, toMock := range matchedTopic.Producers {
			toMock.(*mock.MockProducerProtocol).EXPECT().WriteBatch(gomock.Any(), gomock.Any()).Return(nil).MaxTimes(1)
		}
	}

	err := s.copier.proxy.(*MatchedProxy).handle(s.ctx, s.batch.Messages[0], s.batch)
	require.NoError(s.T(), err)
}

func (s *matchedProxySimpleSuite) TestSimpleMatchingOneProducerClosed() {
	producerError := errors.New("producer is closed")
	for _, matchedTopic := range s.copier.proxy.(*MatchedProxy).To {
		matchedTopic.Producers[0].(*mock.MockProducerProtocol).EXPECT().WriteBatch(gomock.Any(), gomock.Any()).Return(producerError).MaxTimes(1)
		for i := 1; i < len(matchedTopic.Producers); i++ {
			toMock := matchedTopic.Producers[i]
			toMock.(*mock.MockProducerProtocol).EXPECT().WriteBatch(gomock.Any(), gomock.Any()).Return(nil).MaxTimes(1)
		}
	}

	err := s.copier.proxy.(*MatchedProxy).handle(s.ctx, s.batch.Messages[0], s.batch)
	require.NoError(s.T(), err)
}

func (s *matchedProxySimpleSuite) TestSimpleMatchingAllProducersClosed() {
	producerError := errors.New("producer is closed")
	for _, matchedTopic := range s.copier.proxy.(*MatchedProxy).To {
		for _, producer := range matchedTopic.Producers {
			producer.(*mock.MockProducerProtocol).EXPECT().WriteBatch(gomock.Any(), gomock.Any()).Return(producerError).MaxTimes(1)
		}
	}

	err := s.copier.proxy.(*MatchedProxy).handle(s.ctx, s.batch.Messages[0], s.batch)
	require.Error(s.T(), err)
}

func TestMatchedProxySimple(t *testing.T) {
	suite.Run(t, &matchedProxySimpleSuite{})
}

type matchedProxyComplexSuite struct {
	matchedProxySuite
}

func (s *matchedProxyComplexSuite) SetupTest() {
	s.matchedProxySuite.SetupTest()

	// setup Proxy
	cntrl := gomock.NewController(s.T())
	fromMock := mock.NewMockConsumerProtocol(cntrl)

	matchers := make([]MatchedTopic, 0)

	// Create all five matching patterns:
	// match:
	//     condition:
	// 			eq:
	//   		   - $value
	//   		   - 1
	//     condition:
	// 			eq:
	//   		   - $value
	//   		   - 2
	// etc ...

	for i := 0; i < 5; i++ {
		toMocks := make([]logbroker.ProducerProtocol, 0)

		for i := 0; i < 1; i++ {
			toMocks = append(toMocks, mock.NewMockProducerProtocol(cntrl))
		}

		condition := s.conditions[i]
		matcher := NewMatchedTopic(toMocks, configops.BaseLogic{Logic: condition}, s.variables)
		matchers = append(matchers, matcher)
	}

	proxy := NewMatchedProxy(fromMock, matchers)
	proxy.Registry = s.Registry()
	s.testSuite.copier.proxy = proxy
}

func (s *matchedProxyComplexSuite) TestComplexMatching() {
	// each producers runs exactly 1 time, means all messages distributes to all producers.
	for _, matchedTopic := range s.copier.proxy.(*MatchedProxy).To {
		for _, toMock := range matchedTopic.Producers {
			toMock.(*mock.MockProducerProtocol).EXPECT().WriteBatch(gomock.Any(), gomock.Any()).Return(nil).Times(1)
		}
	}
	err := s.copier.proxy.(*MatchedProxy).handle(s.ctx, s.batch.Messages[0], s.batch)
	require.NoError(s.T(), err)
}

func (s *matchedProxyComplexSuite) TestComplexOneProducerClosed() {
	producerError := errors.New("producer is closed")

	// first producer is closed .
	s.copier.proxy.(*MatchedProxy).To[0].Producers[0].(*mock.MockProducerProtocol).EXPECT().WriteBatch(gomock.Any(), gomock.Any()).Return(producerError).Times(1)

	for j := 1; j < len(s.copier.proxy.(*MatchedProxy).To); j++ {
		matchedTopic := s.copier.proxy.(*MatchedProxy).To[j]
		for _, toMock := range matchedTopic.Producers {
			toMock.(*mock.MockProducerProtocol).EXPECT().WriteBatch(gomock.Any(), gomock.Any()).Return(nil).MaxTimes(1)
		}
	}

	err := s.copier.proxy.(*MatchedProxy).handle(s.ctx, s.batch.Messages[0], s.batch)
	require.Error(s.T(), err)
}

func TestMatchedProxyComplex(t *testing.T) {
	suite.Run(t, &matchedProxyComplexSuite{})
}

type matchedProxyGlobalSuite struct {
	matchedProxySuite
}

func (s *matchedProxyGlobalSuite) SetupTest() {
	s.matchedProxySuite.SetupTest()

	// setup Proxy
	cntrl := gomock.NewController(s.T())
	fromMock := mock.NewMockConsumerProtocol(cntrl)

	var matchers []MatchedTopic
	var toMocks []logbroker.ProducerProtocol
	for i := 0; i < 1; i++ {
		toMocks = append(toMocks, mock.NewMockProducerProtocol(cntrl))
	}

	// match:
	//     to:
	//        toMocks
	//     condition:
	// 			eq:
	//   		   - $value
	//   		   - 1
	matchers = append(
		matchers,
		NewMatchedTopic(
			toMocks,
			configops.BaseLogic{Logic: s.conditions[0]},
			s.variables,
		),
	)

	// match without condition
	// match:
	//      to:
	//        toMocks
	toMocks = make([]logbroker.ProducerProtocol, 0)
	for i := 0; i < 4; i++ {
		toMocks = append(toMocks, mock.NewMockProducerProtocol(cntrl))
	}

	matchers = append(
		matchers,
		NewMatchedTopic(
			toMocks,
			configops.BaseLogic{Logic: s.emptyCondition},
			s.variables,
		),
	)

	proxy := NewMatchedProxy(fromMock, matchers)
	proxy.Registry = s.Registry()
	s.testSuite.copier.proxy = proxy
}

func (s *matchedProxyGlobalSuite) TestEmptyMatch() {

	matchedTopic := s.copier.proxy.(*MatchedProxy).To[0] // with condition must run 1 time
	matchedTopic.Producers[0].(*mock.MockProducerProtocol).EXPECT().WriteBatch(gomock.Any(), gomock.Any()).Return(nil).Times(1)

	matchedTopic = s.copier.proxy.(*MatchedProxy).To[1] // without condition each producer gets 1 of 4 messages.

	wg := sync.WaitGroup{}
	wg.Add(len(matchedTopic.Producers) - 1)
	mu := sync.Mutex{}
	mu.Lock()

	for i, producer := range matchedTopic.Producers {
		i := i
		producer.(*mock.MockProducerProtocol).EXPECT().WriteBatch(gomock.Any(), gomock.Any()).Do(func(_, _ any) {
			if i+1 == len(matchedTopic.Producers) {
				wg.Wait()
				mu.Unlock()
			} else {
				wg.Done()
				mu.Lock()   //nolint:SA2001
				mu.Unlock() //nolint:SA2001
			}
		}).Return(nil).Times(1)
	}

	err := s.copier.proxy.(*MatchedProxy).handle(s.ctx, s.batch.Messages[0], s.batch)
	require.NoError(s.T(), err)
}

func TestEmptyCondition(t *testing.T) {
	suite.Run(t, &matchedProxyGlobalSuite{})
}
