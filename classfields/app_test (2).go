package app

import (
	"fmt"
	"github.com/YandexClassifieds/h2p/cmd/cli/errors"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/assert"
	"net"
	"testing"
)

func TestParseArgs(t *testing.T) {
	tests := map[string]struct {
		Args            []string
		ExpectedService string
		ExpectedPort    int
		ExpectedExtra   []string
	}{
		"service": {
			Args:            []string{"service"},
			ExpectedService: "service",
			ExpectedPort:    0,
			ExpectedExtra:   []string{},
		},
		"serviceWithPort": {
			Args:            []string{"service", "2020"},
			ExpectedService: "service",
			ExpectedPort:    2020,
			ExpectedExtra:   []string{},
		},
		"serviceWithPortWithQuery": {
			Args:            []string{"service", "2020", "/metrics"},
			ExpectedService: "service",
			ExpectedPort:    2020,
			ExpectedExtra:   []string{"/metrics"},
		},
		"mysql": {
			Args:            []string{"mysql-mdb1234@acl"},
			ExpectedService: "mysql-mdb1234@acl",
			ExpectedPort:    0,
			ExpectedExtra:   []string{},
		},
		"mysqlWithPort": {
			Args:            []string{"mysql-mdb1234@acl", "2020"},
			ExpectedService: "mysql-mdb1234@acl",
			ExpectedPort:    2020,
			ExpectedExtra:   []string{},
		},
		"mysqlWithPortWithQuery": {
			Args:            []string{"mysql-mdb1234@acl", "2020", "select 1"},
			ExpectedService: "mysql-mdb1234@acl",
			ExpectedPort:    2020,
			ExpectedExtra:   []string{"select 1"},
		},
		"postgresql": {
			Args:            []string{"pg-mdb1234@acl"},
			ExpectedService: "pg-mdb1234@acl",
			ExpectedPort:    0,
			ExpectedExtra:   []string{},
		},
		"postgresqlWithPort": {
			Args:            []string{"pg-mdb1234@acl", "2020"},
			ExpectedService: "pg-mdb1234@acl",
			ExpectedPort:    2020,
			ExpectedExtra:   []string{},
		},
		"postgresqlWithPortWithQuery": {
			Args:            []string{"pg-mdb1234@acl", "2020", "select 1"},
			ExpectedService: "pg-mdb1234@acl",
			ExpectedPort:    2020,
			ExpectedExtra:   []string{"select 1"},
		},
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			a := App{}
			err := a.parseArgs(nil, tc.Args)
			assert.NoError(t, err)
			assert.Equal(t, tc.ExpectedPort, a.s.localPort)
			assert.Equal(t, tc.ExpectedService, a.s.service)
			assert.ElementsMatch(t, tc.ExpectedExtra, a.s.extra)
		})
	}
}

func TestCheckVersion(t *testing.T) {
	ln, err := net.Listen("tcp6", "[::1]:0")
	if err != nil {
		t.Error(err)
	}

	go func() {
		defer ln.Close()

		for {
			server, _ := ln.Accept()
			server.Write([]byte("SSH-2.0-1.9"))
			server.Close()
		}
	}()

	host, port, err := net.SplitHostPort(ln.Addr().String())
	if err != nil {
		t.Error(err)
	}

	viper.Set("h2p_host", fmt.Sprintf("[%s]", host))
	viper.Set("h2p_port", port)

	a := App{}

	err = a.checkVersion("1.8")
	assert.EqualError(t, err, errors.LowVersionError.Error())

	err = a.checkVersion("1.9")
	assert.NoError(t, err)

	err = a.checkVersion("1.10")
	assert.NoError(t, err)

	viper.Set("h2p_host", nil)
	viper.Set("h2p_port", nil)
}
