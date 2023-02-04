package register

import (
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/hashicorp/consul/api"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"testing"
	"time"
)

func TestRegister_Enable(t *testing.T) {
	cli, err := api.NewClient(&api.Config{
		Address: "http://localhost:8501",
	})
	require.NoError(t, err)
	lst := []ConsulService{
		{Id: "svc-one-id", Name: "svc-one"},
	}
	r := NewRegister(cli, logrus.New("INFO"), lst)
	r.Enable()
	time.Sleep(time.Second)
	svc, _, err := cli.Agent().Service("svc-one-id", nil)
	require.NoError(t, err)
	assert.Equal(t, "svc-one-id", svc.ID)
}
