package election

import (
	"context"
	"errors"
	"fmt"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/common/health"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

var namespace = test.RandString(10)

func TestWatch(t *testing.T) {
	test.InitTestEnv()
	h := health.NewChecker(time.Second / 10)
	mc := &mockCheck{healthy: false}
	h.Add(mc)
	h.Start()
	log := test.NewLogger(t)
	election := NewElection(log, newTestConf(t), h)
	require.NoError(t, election.Start())
	defer election.Stop()

	// init signal
	state := election.State()
	assertState(t, false, state)
	assertNoState(t, state)

	// get leader
	mc.healthy = true
	assertState(t, true, state)
	assertNoState(t, state)

	// miss leader
	mc.healthy = false
	assertState(t, false, state)
	assertNoState(t, state)
}

func assertState(t *testing.T, want bool, actualC <-chan bool) {
	select {
	case actual, ok := <-actualC:
		require.Equal(t, true, ok)
		require.Equal(t, want, actual)
	case <-time.NewTimer(3 * time.Second).C:
		require.FailNow(t, "timeout")
	}
}

func assertNoState(t *testing.T, actualC <-chan bool) {
	select {
	case actual, ok := <-actualC:
		require.Equal(t, true, ok)
		require.FailNow(t, fmt.Sprintf("actual state: %v", actual))
	case <-time.NewTimer(3 * time.Second).C:
	}
}

func TestLeader(t *testing.T) {
	test.InitTestEnv()
	h := health.NewChecker(time.Second / 10)
	h.Start()
	assert.Eventually(t, func() bool {
		return h.Healthy()
	}, 1*time.Second, 10*time.Millisecond)

	locker1 := NewElection(test.NewLogger(t), newTestConf(t), h)
	locker2 := NewElection(test.NewLogger(t), newTestConf(t), h)
	w1 := locker1.State()
	w2 := locker2.State()
	assertState(t, false, w1)
	assertState(t, false, w2)
	require.NoError(t, locker1.Start())
	assertState(t, true, w1)

	require.NoError(t, locker2.Start())
	defer locker2.Stop()
	assertNoState(t, w2)
	assert.False(t, locker2.IsLeader())

	locker1.Stop()
	assertState(t, true, w2)
}

func TestIsLeader(t *testing.T) {
	test.InitTestEnv()
	h := health.NewChecker(time.Second / 10)
	e := NewElection(test.NewLogger(t), newTestConf(t), h)
	assert.False(t, e.IsLeader())
	e.setState(true)
	assert.True(t, e.IsLeader())
}

type mockCheck struct {
	healthy bool
}

func (m *mockCheck) Ping(ctx context.Context) error {
	if !m.healthy {
		return errors.New("health err")
	}
	return nil
}

func newTestConf(t *testing.T) Conf {
	conf := NewConf(namespace)
	conf.ServiceName = "shiva-ci/" + t.Name()
	return conf
}
