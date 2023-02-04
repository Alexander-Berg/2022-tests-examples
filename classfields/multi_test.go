package consumer

import (
	"github.com/YandexClassifieds/logs/cmd/golf/domain"
	"github.com/YandexClassifieds/logs/cmd/golf/test"
	"github.com/stretchr/testify/mock"
	"go.uber.org/goleak"
	"testing"
)

func TestMultiConsumer_Shutdown(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))
	test.InitEnv()

	ms := newMockStorage()
	c := NewMultiConsumer(nil, nil, test.NewTestLogger())
	c.writer = ms
	go c.commitProcessor()

	close(ms.keyC)
	c.Shutdown()
}

func TestMultiConsumer_CommitProcess(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))
	test.InitEnv()

	ms := newMockStorage()
	c := NewMultiConsumer(nil, nil, test.NewTestLogger())
	c.writer = ms

	sc1 := new(mockConsumer)
	sc2 := new(mockConsumer)
	sc1.On("Commit", domain.BatchKey{Datacenter: "dc1", Cookie: 1})
	sc2.On("Commit", domain.BatchKey{Datacenter: "dc2", Cookie: 1})
	sc1.On("Stop")
	sc2.On("Stop")
	defer mock.AssertExpectationsForObjects(t, sc1, sc2)

	c.consumers = map[string]consumer{
		"dc1": sc1,
		"dc2": sc2,
	}

	go c.commitProcessor()
	ms.keyC <- domain.BatchKey{Datacenter: "dc1", Cookie: 1}
	ms.keyC <- domain.BatchKey{Datacenter: "dc2", Cookie: 1}
	close(ms.keyC)

	c.Shutdown()
}

type mockStorage struct {
	keyC chan domain.BatchKey
}

func newMockStorage() *mockStorage {
	return &mockStorage{keyC: make(chan domain.BatchKey)}
}

func (m *mockStorage) Add(_ []domain.Row, key domain.BatchKey) error {
	m.keyC <- key
	return nil
}

func (m *mockStorage) CommitC() <-chan domain.BatchKey {
	return m.keyC
}

type mockConsumer struct {
	mock.Mock
}

func (m *mockConsumer) Init() {
}

func (m *mockConsumer) Stop() {
	m.Called()
}

func (m *mockConsumer) Commit(key domain.BatchKey) {
	m.Called(key)
}
