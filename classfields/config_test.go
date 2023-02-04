package register

import (
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"io/ioutil"
	"testing"
)

func TestReadServices(t *testing.T) {
	err := ioutil.WriteFile("/tmp/test_services.json", []byte(testPayload), 0644)
	require.NoError(t, err)
	lst, err := ReadServices("/tmp/test_services.json")
	require.NoError(t, err)

	assert.Len(t, lst, 3)
	assert.Equal(t, "logs-generator-monitoring", lst[0].Id)
}

var (
	testPayload = `
{"services":[
{
 "id":"logs-generator-monitoring","name":"logs-generator-monitoring","port":81,
 "tags":["metrics_logs-generator","service=logs-generator","version=yd8","canary=false"],
 "check":{"id":"logs-generator-monitoring","name":"logs-generator-monitoring","http":"http://localhost:81/ping","tcp":"","grpc":"","grpc_use_tls":false,"interval":"1s","timeout":"2s"}
},
{"id":"logs-agent-svc","name":"logs-agent","port":8502,"tags":["metrics_logs_agent"]},
{"id":"logs-agent-infra","name":"logs-agent","port":8503,"tags":["metrics_logs_agent"]}]}
`
)
