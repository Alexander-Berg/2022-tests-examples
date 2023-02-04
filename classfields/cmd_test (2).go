package vmagent

import (
	"github.com/stretchr/testify/require"
	"testing"
)

func Test_getHostInDC(t *testing.T) {
	host, err := getHostInDC(
		[]string{
			"host-01-sas.test.vertis.yandex.net",
			"host-01-vla.test.vertis.yandex.net",
			"host-01-man.test.vertis.yandex.net",
		},
		"vla",
	)

	require.NoError(t, err)
	require.Equal(t, "host-01-vla.test.vertis.yandex.net", host)

	host, err = getHostInDC(
		[]string{
			"host-01-sas.test.vertis.yandex.net",
			"host-01-vla.test.vertis.yandex.net",
			"host-01-man.test.vertis.yandex.net",
		},
		"myt",
	)

	require.EqualError(t, err, "can't find vmagent host in myt")
}
