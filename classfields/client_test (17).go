package ssh

import (
	"testing"
	"time"

	"github.com/YandexClassifieds/drills-helper/test"
	"github.com/stretchr/testify/require"
	"golang.org/x/crypto/ssh"
)

const (
	confirmScriptOutput = `Hello, I am test script!
Confirm master switch? [y/N]
Some string
Master switched to test-host
`
	lsOutput = "app\nbin\nconfig\ndefaults\ndev\ndocker-mods\netc\nhome\ninit\nkeygen.sh\nlib\nlibexec\nmedia\nmnt\nopt\nproc\nroot\nrun\nsbin\nsrv\nsys\ntmp\nusr\nvar\n"
)

func TestClient_Connect(t *testing.T) {
	test.InitTestEnv()

	client := &Client{}
	client.config = &ssh.ClientConfig{
		User: "test",
		Auth: []ssh.AuthMethod{
			ssh.Password("test"),
		},
		HostKeyCallback: ssh.InsecureIgnoreHostKey(),
	}
	client.port = "2222"

	s, err := client.Connect("localhost")
	require.NoError(t, err)

	out, err := s.CombinedOutput("id")
	require.NoError(t, err)

	require.Equal(t, "uid=911(test) gid=911(test) groups=911(test),1000(users)\n", string(out))
}

func TestClient_Run(t *testing.T) {
	test.InitTestEnv()

	client := &Client{}
	client.config = &ssh.ClientConfig{
		User: "test",
		Auth: []ssh.AuthMethod{
			ssh.Password("test"),
		},
		HostKeyCallback: ssh.InsecureIgnoreHostKey(),
	}
	client.port = "2222"

	s, err := client.Connect("localhost")
	require.NoError(t, err)

	out, err := client.Run(s, "ls /")
	require.NoError(t, err)
	require.Equal(t, lsOutput, out)
}

func TestClient_RunError(t *testing.T) {
	test.InitTestEnv()

	client := &Client{}
	client.config = &ssh.ClientConfig{
		User: "test",
		Auth: []ssh.AuthMethod{
			ssh.Password("test"),
		},
		HostKeyCallback: ssh.InsecureIgnoreHostKey(),
	}
	client.port = "2222"

	s, err := client.Connect("localhost")
	require.NoError(t, err)

	out, err := client.Run(s, "ls / | grep invalid")
	require.EqualError(t, err, "Process exited with status 1")
	require.Equal(t, "", out)
}

func TestClient_RunWithConfirm(t *testing.T) {
	test.InitTestEnv()

	client := &Client{}
	client.config = &ssh.ClientConfig{
		User: "test",
		Auth: []ssh.AuthMethod{
			ssh.Password("test"),
		},
		HostKeyCallback: ssh.InsecureIgnoreHostKey(),
	}
	client.port = "2222"

	s, err := client.Connect("localhost")
	require.NoError(t, err)

	out, err := client.RunWithConfirm(s, "/usr/bin/confirm-script.sh", 5*time.Second, "[y/N]")
	require.NoError(t, err)
	require.Equal(t, confirmScriptOutput, out)
}

func TestClient_ConcurrentRun(t *testing.T) {
	test.InitTestEnv()

	client := &Client{}
	client.config = &ssh.ClientConfig{
		User: "test",
		Auth: []ssh.AuthMethod{
			ssh.Password("test"),
		},
		HostKeyCallback: ssh.InsecureIgnoreHostKey(),
	}
	client.port = "2222"

	err := client.ConcurrentRun([]string{"localhost"}, "ls / && echo 'abc'")
	require.NoError(t, err)
}

func TestClient_ConcurrentRunError(t *testing.T) {
	test.InitTestEnv()

	client := &Client{}
	client.config = &ssh.ClientConfig{
		User: "test",
		Auth: []ssh.AuthMethod{
			ssh.Password("test"),
		},
		HostKeyCallback: ssh.InsecureIgnoreHostKey(),
	}
	client.port = "2222"

	err := client.ConcurrentRun([]string{"localhost", "wronghostname"}, "cat /none_exist.txt")
	require.Error(t, err)
}
