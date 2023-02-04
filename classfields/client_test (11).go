package lb_int_nginx_test

import (
	"testing"

	"github.com/YandexClassifieds/drills-helper/app/lb/lb_int_nginx"
	"github.com/YandexClassifieds/drills-helper/test"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/require"
)

func TestClient_Open(t *testing.T) {
	conductorMock := test.PrepareConductorMock()
	sshMock := test.PrepareSSHMock()

	client := lb_int_nginx.New(conductorMock, sshMock, logrus.New())
	err := client.Open("sas")
	require.NoError(t, err)

	conductorMock.Mock.AssertNumberOfCalls(t, "GroupToHosts", 1)
	conductorMock.Mock.AssertNumberOfCalls(t, "FilterHostsByDC", 1)
	sshMock.Mock.AssertNumberOfCalls(t, "ConcurrentRun", 1)
}

func TestClient_Close(t *testing.T) {
	conductorMock := test.PrepareConductorMock()
	sshMock := test.PrepareSSHMock()

	client := lb_int_nginx.New(conductorMock, sshMock, logrus.New())
	err := client.Close("sas")
	require.NoError(t, err)

	conductorMock.Mock.AssertNumberOfCalls(t, "GroupToHosts", 1)
	conductorMock.Mock.AssertNumberOfCalls(t, "FilterHostsByDC", 1)
	sshMock.Mock.AssertNumberOfCalls(t, "ConcurrentRun", 1)
}
