package election

import (
	"context"
	"fmt"
	"github.com/YandexClassifieds/go-common/conf"
	"github.com/YandexClassifieds/go-common/conf/viper"
	"math/rand"
	"os"
	"testing"
	"time"

	"github.com/YandexClassifieds/go-common/log"
	"github.com/YandexClassifieds/go-common/monitoring/health"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

var (
	r           = rand.New(rand.NewSource(time.Now().UnixNano()))
	serviceName = RandString(10)
)

func TestTwoElection(t *testing.T) {
	el := newElection(t, serviceName, nil)
	assertStateSign(t, el, true)
	require.True(t, el.IsLeader())
	el2 := newElection(t, serviceName, nil)
	require.False(t, el2.IsLeader())
	require.True(t, el.IsLeader())

	el.Stop()
	assertStateSign(t, el2, true)
	require.True(t, el2.IsLeader())

	el2.Stop()
}

func assertStateSign(t *testing.T, el *Election, want bool) {
	select {
	case state := <-el.State():
		assert.Equal(t, want, state)
	case <-time.NewTimer(10 * time.Second).C:
		require.FailNow(t, "timeout")
	}
}

func assertNoSign(t *testing.T, el *Election) {
	select {
	case state := <-el.State():
		require.FailNow(t, fmt.Sprintf("signal: %v", state))
	case <-time.NewTimer(time.Second).C:
	}
}

func TestOtherElection(t *testing.T) {
	el := newElection(t, serviceName, nil)
	assertStateSign(t, el, true)
	require.True(t, el.IsLeader())

	el2 := newElection(t, serviceName+"2", nil)
	assertStateSign(t, el2, true)
	require.True(t, el2.IsLeader())

	el.Stop()
	el2.Stop()
}

func TestWatch(t *testing.T) {
	ping := &mockPing{healthy: false}
	el := newElection(t, serviceName, ping)
	defer el.Stop()

	// init state without signal
	require.False(t, el.IsLeader())
	assertNoSign(t, el)

	// get leader
	ping.healthy = true
	assertStateSign(t, el, true)
	assertNoSign(t, el)

	// miss leader
	ping.healthy = false
	assertStateSign(t, el, false)
	assertNoSign(t, el)
}

type mockPing struct {
	healthy bool
}

func (m *mockPing) Name() string {
	return "mock ping"
}

func (m *mockPing) Ping(ctx context.Context) error {
	if m.healthy {
		return nil
	} else {
		return fmt.Errorf("mock error")
	}

}

func newElection(t *testing.T, name string, ping *mockPing) *Election {
	require.NoError(t, os.Setenv("_DEPLOY_SERVICE_NAME", "common_ci/"+name))
	require.NoError(t, os.Setenv("_DEPLOY_ALLOC_ID", RandString(5)))
	var params []Option
	confS := viper.NewTestConf()
	params = append(params, WithNamespace("go_common/"+name+"/"+t.Name()))
	if os.Getenv("CI") != "" {
		params = append(params, WithToken(conf.Str("CONSUL_API_TOKEN")))
		params = append(params, WithAddress(conf.Str("CONSUL_ADDRESS")))
		params = append(params, WithConf(confS))
	} else {
		params = append(params, WithAddress("localhost:8500"))
	}

	if ping != nil {
		check := WithHealthCheck(health.NewChecker(log.SilenceLogger, 100*time.Millisecond, []health.Pingable{ping}))
		params = append(params, check)
	}
	el, err := NewElection(params...)
	require.NoError(t, err)
	return el
}

var letterRunes = []rune("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")

func RandString(n int) string {
	b := make([]rune, n)
	for i := range b {
		b[i] = letterRunes[r.Intn(len(letterRunes))]
	}
	return string(b)
}
