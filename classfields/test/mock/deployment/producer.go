package deployment

import (
	"fmt"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/context"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
)

type State struct {
	id    int64
	value model.State
}

type ProducerMock struct {
	c chan State
}

func NewProducerMock() *ProducerMock {

	return &ProducerMock{
		c: make(chan State, 25),
	}
}

func (m *ProducerMock) Notify(ctx *context.Context) error {

	m.c <- State{
		value: ctx.Deployment.State,
		id:    ctx.Deployment.ID,
	}
	return nil
}

func (m *ProducerMock) Assert(t *testing.T, ctx *context.Context, expected model.State) {
	test.Wait(t, func() error {
		select {
		case result := <-m.c:
			assert.Equal(t, expected.String(), result.value.String())
			assert.Equal(t, ctx.Deployment.ID, result.id)
			return nil
		default:
			return fmt.Errorf("producer is empty, but expected %s", expected.String())
		}
	})
}

func (m *ProducerMock) AssertEmpty(t *testing.T) {
	assert.Equal(t, 0, len(m.c))
	for i := 0; i < len(m.c); i++ {
		state := <-m.c
		logrus.Infof("TEST fail event id: %d; state: %s", state.id, state.value.String())
	}
}

func (m *ProducerMock) Reset() {
	m.c = make(chan State, 25)
}
