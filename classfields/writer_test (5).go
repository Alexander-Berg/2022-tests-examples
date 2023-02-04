package broker

import (
	"context"
	vlogrus "github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/sender-proxy/pb/sender_proxy/event"
	"github.com/YandexClassifieds/sender-proxy/proto/broker"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc"
	"testing"
	"time"
)

func TestNewWriter(t *testing.T) {
	cfg := Config{
		QueueSize:    10,
		QueueWorkers: 2,
	}
	w, err := NewWriter(cfg, vlogrus.New())
	require.NoError(t, err)
	m := newMockBroker()
	w.cli = m

	w.Add(&event.EmailEvent{
		EventId: "one",
	})
	w.Add(&event.EmailEvent{
		EventId: "two",
	})

	closeC := make(chan struct{})
	go func() {
		w.Stop()
		close(closeC)
	}()
	select {
	case <-time.After(time.Second * 2):
		t.Fatalf("broker stop fail")
	case <-closeC:
	}

	assert.Len(t, m.Events, 2)
}

func TestWriter_NewRequest(t *testing.T) {
	w := &SimpleWriter{
		cfg: Config{SchemaVersion: "0.42"},
	}
	// just for test so we know it has proper type
	req, err := w.newRequest(&event.EmailEvent{
		EventId: "some-id",
		Account: "acct",
	})
	require.NoError(t, err)
	assert.Equal(t, "some-id", req.GetData().GetId().GetValue())
	assert.Equal(t, "sender_proxy.event.EmailEvent", req.GetHeader().GetMessageType())
	assert.Equal(t, "0.42", req.GetHeader().GetSchemaVersion())
}

type mockBroker struct {
	Events []*broker.WriteEventRequest
}

func newMockBroker() *mockBroker {
	return &mockBroker{
		Events: make([]*broker.WriteEventRequest, 0),
	}
}

func (m *mockBroker) WriteSession(ctx context.Context, opts ...grpc.CallOption) (broker.BrokerService_WriteSessionClient, error) {
	panic("implement me")
}

func (m *mockBroker) WriteEvent(ctx context.Context, in *broker.WriteEventRequest, opts ...grpc.CallOption) (*broker.WriteEventResponse, error) {
	m.Events = append(m.Events, in)
	resp := &broker.WriteEventResponse{
		Result: &broker.WriteEventResponse_Ack_{Ack: &broker.WriteEventResponse_Ack{}},
	}
	return resp, nil
}
