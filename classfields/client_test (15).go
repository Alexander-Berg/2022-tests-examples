package teamcity

import (
	"bytes"
	"net/http"
	"testing"

	_switch "github.com/YandexClassifieds/drills-helper/app/switch"
	"github.com/YandexClassifieds/drills-helper/test"
	"github.com/YandexClassifieds/drills-helper/test/mocks/conductor"
	"github.com/YandexClassifieds/drills-helper/test/mocks/ssh"
	"github.com/jarcoal/httpmock"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

const switchOutput = `level=info msg="current master: host-01-sas.prod.vertis.yandex.net"
level=info msg="new master: test-host"
`

func TestClient_findMaster(t *testing.T) {
	test.InitTestEnv()

	client := &Client{
		cli: &http.Client{},
	}

	httpmock.Activate()
	t.Cleanup(httpmock.DeactivateAndReset)

	// master
	httpmock.RegisterResponder("GET", "http://host1:8111/login.html",
		httpmock.NewStringResponder(200, ""))
	// not active master
	httpmock.RegisterResponder("GET", "http://host2:8111/login.html",
		httpmock.ConnectionFailure)
	// some host, that alive, but respond non 200
	httpmock.RegisterResponder("GET", "http://host3:8111/login.html",
		httpmock.NewStringResponder(503, "invalid"))

	master, err := client.findMaster([]string{"host3", "host2", "host1"})
	require.NoError(t, err)
	require.Equal(t, "host1", master)
}

func TestClient_findMasterNoAlive(t *testing.T) {
	test.InitTestEnv()

	client := &Client{
		cli: &http.Client{},
	}

	httpmock.Activate()
	t.Cleanup(httpmock.DeactivateAndReset)

	// invalid master
	httpmock.RegisterResponder("GET", "http://host1:8111/login.html",
		httpmock.NewStringResponder(503, "invalid"))
	// not active master
	httpmock.RegisterResponder("GET", "http://host2:8111/login.html",
		httpmock.ConnectionFailure)

	_, err := client.findMaster([]string{"host1", "host2"})
	require.EqualError(t, err, "can't find master by healthcheck")
}

func TestClient_Switch(t *testing.T) {
	test.InitTestEnv()

	conductorMock := &conductor.IClient{}
	conductorMock.On("GroupToHosts", mock.Anything).
		Return([]string{"host-01-sas.prod.vertis.yandex.net"}, nil)

	sshMock := &ssh.IClient{}
	sshMock.On("Connect", mock.Anything).Return(nil, nil)
	sshMock.On("RunWithConfirm", mock.Anything, mock.Anything, mock.Anything, mock.Anything).
		Return("[2021-03-06 20:55:03] Some string\n[2021-03-06 20:55:04] Master switched to test-host", nil)

	var out bytes.Buffer

	client := &Client{
		conductor: conductorMock,
		ssh:       sshMock,
		cli:       &http.Client{},
		log:       logrus.StandardLogger(),
	}
	client.log.SetFormatter(&logrus.TextFormatter{
		DisableTimestamp: true,
	})
	client.log.SetOutput(&out)

	httpmock.Activate()
	t.Cleanup(httpmock.DeactivateAndReset)
	httpmock.RegisterResponder("GET", "http://host-01-sas.prod.vertis.yandex.net:8111/login.html",
		httpmock.NewStringResponder(200, ""))

	err := client.Switch("sas")
	require.NoError(t, err)

	require.Equal(t, switchOutput, out.String())
}

func TestClient_SwitchAlreadyInAnotherDC(t *testing.T) {
	test.InitTestEnv()

	conductorMock := &conductor.IClient{}
	conductorMock.On("GroupToHosts", mock.Anything).
		Return([]string{"host-01-sas.prod.vertis.yandex.net"}, nil)

	client := &Client{
		cli:       &http.Client{},
		conductor: conductorMock,
		log:       logrus.StandardLogger(),
	}

	httpmock.Activate()
	t.Cleanup(httpmock.DeactivateAndReset)
	httpmock.RegisterResponder("GET", "http://host-01-sas.prod.vertis.yandex.net:8111/login.html",
		httpmock.NewStringResponder(200, ""))

	err := client.Switch("vla")
	require.ErrorIs(t, err, _switch.ErrMasterInAnotherDC)
}
