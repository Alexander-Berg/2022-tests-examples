package mq

import (
	"context"
	"fmt"
	"sort"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/pkg/mq/conf"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

const (
	topic = "mq_test"
)

func TestPushAndRead(t *testing.T) {
	// double run for check previous ack
	testPushAndRead(t)
	testPushAndRead(t)
}

func testPushAndRead(t *testing.T) {
	test.InitTestEnv()
	p := NewProducer(conf.NewProducerConf(topic), test.NewLogger(t))
	handler := NewTestHandler(t, false, false)
	c := Consume(conf.NewConsumerConf(topic, t.Name()), test.NewLogger(t), nil, handler.handle)
	msg := newMessage(t, p)
	assertEqualMessage(t, msg, handler.Get())
	c.Close()
	<-c.Closed
}

func assertEqualMessage(t *testing.T, msg1 *Message, msg2 *Message) {
	assert.Equal(t, msg1.Name, msg2.Name)
	assert.Equal(t, string(msg1.Payload), string(msg2.Payload))
	assert.Equal(t, msg1.Headers["test_uuid"], msg2.Headers["test_uuid"])
}

func TestReadOldMessage(t *testing.T) {
	test.InitTestEnv()
	p := NewProducer(conf.NewProducerConf(topic), test.NewLogger(t))
	msg := newMessage(t, p)
	time.Sleep(250 * time.Millisecond)
	handler := NewTestHandler(t, false, false)
	c := Consume(conf.NewConsumerConf(topic, t.Name()), test.NewLogger(t), nil, handler.handle)
	assertEqualMessage(t, msg, handler.Get())

	c.Close()
	<-c.Closed
}

func TestEndlessRetry(t *testing.T) {
	test.InitTestEnv()
	p := NewProducer(conf.NewProducerConf(topic), test.NewLogger(t))
	handler := NewTestHandler(t, true, true)
	c := Consume(conf.NewConsumerConf(topic, t.Name()), test.NewLogger(t), nil, handler.handle)
	msg1 := newMessage(t, p)
	msg2 := newMessage(t, p)
	require.Eventually(t, func() bool {
		return handler.attempts > 15
	}, 10*time.Second, 250*time.Millisecond, "handler.attempts is not greater 15")
	handler.failAll = false
	assertEqualMessage(t, msg1, handler.Get())
	assertEqualMessage(t, msg2, handler.Get())
	c.Close()
	<-c.Closed
}

func newMessage(t *testing.T, p *Producer) *Message {
	msg1 := NewMessage(t.Name(), []byte(t.Name()),
		map[string]string{"test_uuid": uuid.New().String()})
	require.NoError(t, p.Push(msg1))
	return msg1
}

func TestRetry(t *testing.T) {
	test.InitTestEnv()
	p := NewProducer(conf.NewProducerConf(topic), test.NewLogger(t))
	errH := NewTestHandler(t, true, false)
	c := Consume(conf.NewConsumerConf(topic, t.Name()), test.NewLogger(t), nil, errH.handle)
	msg1 := newMessage(t, p)
	assertEqualMessage(t, msg1, errH.Get())
	c.Close()
	<-c.Closed
}

func TestConsumeWrongTopic(t *testing.T) {
	test.InitTestEnv()
	handler := NewTestHandler(t, true, false)
	c := Consume(conf.NewConsumerConf(topic+"no", t.Name()), test.NewLogger(t), nil, handler.handle)

	c.Close()
	<-c.Closed
}

func TestReconnect(t *testing.T) {
	test.InitTestEnv()
	newCxt, cancel := context.WithCancel(context.Background())
	log := test.NewLogger(t)
	p := NewProducer(conf.NewProducerConf(topic), log)
	handler := NewTestHandler(t, false, false)
	c := newConsumer(conf.NewConsumerConf(topic, t.Name()), log, nil, handler.handle)
	c.ctx = newCxt
	go c.FirstRun()
	msg1 := newMessage(t, p)
	assertEqualMessage(t, msg1, handler.Get())

	c.ctx = context.Background()
	cancel()
	msg2 := newMessage(t, p)
	assertEqualMessage(t, msg2, handler.Get())

	c.Close()
	<-c.Closed
}

func TestDistributionPush(t *testing.T) {

	test.InitTestEnv()
	handler := NewTestHandler(t, false, false)
	c := Consume(conf.NewConsumerConf(topic, t.Name()), test.NewLogger(t), nil, handler.handle)
	producerConf := conf.NewProducerConf(topic)
	p1 := NewProducer(producerConf, test.NewLogger(t))
	p2 := NewProducer(producerConf, test.NewLogger(t))
	p3 := NewProducer(producerConf, test.NewLogger(t))

	require.NoError(t, p1.Push(NewMessage(t.Name(), []byte("a"), map[string]string{})))
	require.NoError(t, p2.Push(NewMessage(t.Name(), []byte("b"), map[string]string{})))
	require.NoError(t, p3.Push(NewMessage(t.Name(), []byte("c"), map[string]string{})))

	results := []string{}
	for i := 0; i < 3; i++ {
		msg := handler.Get()
		results = append(results, string(msg.Payload))
	}
	sort.Strings(results)

	assert.Equal(t, "a", results[0])
	assert.Equal(t, "b", results[1])
	assert.Equal(t, "c", results[2])

	c.Close()
	<-c.Closed
}

type TestHandler struct {
	t         *testing.T
	attempts  int
	failFirst bool
	failAll   bool
	result    chan *Message
}

func NewTestHandler(t *testing.T, failFirst, failAll bool) *TestHandler {

	return &TestHandler{
		t:         t,
		attempts:  0,
		failFirst: failFirst,
		failAll:   failAll,
		result:    make(chan *Message, 10),
	}
}

func (h *TestHandler) Get() *Message {
	select {
	case <-time.NewTimer(5 * time.Second).C:
		require.FailNow(h.t, "timeout")
		return nil
	case result := <-h.result:
		return result
	}
}

func (h *TestHandler) handle(msg *Message) error {
	h.attempts++
	t := h.t
	if t.Name() != msg.Name {
		return nil
	}
	if h.attempts == 1 && h.failFirst {
		return fmt.Errorf("first test error")
	}
	if h.failAll {
		return fmt.Errorf("test error")
	}
	h.result <- msg
	return nil
}

func TestNotification(t *testing.T) {

	test.InitTestEnv()
	p := NewProducer(conf.NewProducerConf(topic), test.NewLogger(t))

	handler1 := NewTestHandler(t, false, false)
	c1 := Consume(conf.NewConsumerConf(topic, t.Name()+"1"), test.NewLogger(t), nil, handler1.handle)
	handler2 := NewTestHandler(t, false, false)
	c2 := Consume(conf.NewConsumerConf(topic, t.Name()+"2"), test.NewLogger(t), nil, handler2.handle)
	handler3 := NewTestHandler(t, false, false)
	c3 := Consume(conf.NewConsumerConf(topic, t.Name()+"3"), test.NewLogger(t), nil, handler3.handle)

	msg1 := NewMessage(t.Name(), []byte(t.Name()), map[string]string{})
	require.NoError(t, p.Push(msg1))

	assertEqualMessage(t, msg1, handler1.Get())
	assertEqualMessage(t, msg1, handler2.Get())
	assertEqualMessage(t, msg1, handler3.Get())

	c1.Close()
	c2.Close()
	c3.Close()
	<-c1.Closed
	<-c2.Closed
	<-c3.Closed
}

func TestInitialisedWait(t *testing.T) {
	test.InitTestEnv()
	p := NewProducer(conf.NewProducerConf(topic), test.NewLogger(t))
	handler := NewTestHandler(t, false, false)

	initialised := make(chan struct{})
	c := Consume(conf.NewConsumerConf(topic, t.Name()), test.NewLogger(t), initialised, handler.handle)
	newMessage(t, p)
	require.Never(t, func() bool {
		<-handler.result
		return true
	}, time.Second*3, time.Second*3/2)
	c.Close()
	<-c.Closed
}
