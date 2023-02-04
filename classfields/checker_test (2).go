package checker

import (
	"github.com/hashicorp/consul/api"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestServiceIsAlive(t *testing.T) {
	var err error
	client := newConsulClient(t)

	err = client.Agent().ServiceDeregister("test-service")
	require.NoError(t, err)
	err = client.Agent().ServiceRegister(&api.AgentServiceRegistration{
		Name: "test-service",
		Check: &api.AgentServiceCheck{
			Name:   "test-check",
			Status: "passing",
			TTL:    "5s",
		},
	})
	require.NoError(t, err)

	c := &Checker{consul: client}
	assert.True(t, c.isHealthy("test-service"), "test service should be registered and alive")
	assert.False(t, c.isHealthy("asjdhsakjdhlkhasldkf"), "non-existent service should not be healthy")
}

func newConsulClient(tb testing.TB) *api.Client {
	viper.AutomaticEnv()
	client, err := api.NewClient(&api.Config{Address: viper.GetString("CONSUL_ADDR")})
	require.NoError(tb, err)
	return client
}
