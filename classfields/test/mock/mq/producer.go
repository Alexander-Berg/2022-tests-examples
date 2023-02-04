package mq

import (
	"github.com/YandexClassifieds/shiva/pkg/mq"
	"github.com/stretchr/testify/require"
	"testing"
	"time"
)

type ProducerMock struct {
	Msg chan *mq.Message
}

func NewProducerMock() ProducerMock {
	return ProducerMock{
		Msg: make(chan *mq.Message, 10),
	}
}

func (p ProducerMock) Push(msg *mq.Message) error {
	p.Msg <- msg
	return nil
}

func (p ProducerMock) Get(t *testing.T) *mq.Message {
	timer := time.NewTimer(3 * time.Second)
	defer timer.Stop()
	select {
	case msg := <-p.Msg:
		return msg
	case <-timer.C:
		require.FailNow(t, "timeout")
	}
	return nil
}
