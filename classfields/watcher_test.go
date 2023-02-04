package consul

import (
	"github.com/hashicorp/consul/api"
	"github.com/sirupsen/logrus"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"testing"
	"time"
)

func TestWatcher(t *testing.T) {
	testInit()
	cli, _ := api.NewClient(&api.Config{
		Address: viper.GetString("consul_address"),
	})
	err := cli.Agent().ServiceRegister(&api.AgentServiceRegistration{
		Name: "watcher-svc",
		Tags: []string{"domain-watcher-svc"},
		Port: 42,
	})
	require.NoError(t, err)

	dc := viper.GetString("dc")
	w := newWatcher(cli, logrus.StandardLogger())
	key := watchKey{
		service: "watcher-svc",
		tag:     "domain-watcher-svc",
		dc:      dc,
	}
	w.AddWatch(key)
	assert.Contains(t, w.watches, key)
	w.AddWatch(watchKey{service: "non-existent-svc"})

	entries := w.Get(key)
	require.NotEmpty(t, entries)
	assert.Equal(t, 42, entries[0].Service.Port)

	err = cli.Agent().ServiceRegister(&api.AgentServiceRegistration{
		Name: "watcher-svc",
		Tags: []string{"domain-watcher-svc"},
		Port: 43,
	})
	require.NoError(t, err)

	<-time.After(time.Second / 5)
	entries2 := w.Get(key)
	require.NotEmpty(t, entries2)
	assert.Equal(t, 43, entries2[0].Service.Port)

	assert.Len(t, w.Keys(), 2)
	w.RemoveWatch(key)
	assert.Len(t, w.Keys(), 1)
}
