package conductor

import (
	"testing"

	"github.com/YandexClassifieds/drills-helper/test"
	"github.com/stretchr/testify/require"
)

func TestClient_GroupToHosts(t *testing.T) {
	test.InitTestEnv()

	cli := New()

	hosts, err := cli.GroupToHosts("vertis_for_test")
	require.NoError(t, err)
	require.ElementsMatch(t, hosts, []string{"vertis-for-test-01-vla.test.vertis.yandex.net"})
}

func TestClient_GroupsToHosts(t *testing.T) {
	test.InitTestEnv()

	cli := New()

	hosts, err := cli.GroupsToHosts([]string{"vertis_for_test"})
	require.NoError(t, err)
	require.ElementsMatch(t, hosts, []string{"vertis-for-test-01-vla.test.vertis.yandex.net"})
}

func TestClient_FilterHostsByDC_Prod(t *testing.T) {
	test.InitTestEnv()

	hosts := []string{"host1-vla.prod.vertis.yandex.net", "host2-vla.prod.vertis.yandex.net",
		"host1-sas.prod.vertis.yandex.net", "host2-sas.prod.vertis.yandex.net"}
	expected := []string{"host1-sas.prod.vertis.yandex.net", "host2-sas.prod.vertis.yandex.net"}

	cli := New()
	require.Equal(t, expected, cli.FilterHostsByDC(hosts, "sas"))
}

func TestClient_FilterHostsByDC_Test(t *testing.T) {
	test.InitTestEnv()

	hosts := []string{"host1-vla.test.vertis.yandex.net", "host2-vla.test.vertis.yandex.net",
		"host1-sas.test.vertis.yandex.net", "host2-sas.test.vertis.yandex.net"}
	expected := []string{"host1-sas.test.vertis.yandex.net", "host2-sas.test.vertis.yandex.net"}

	cli := New()
	require.Equal(t, expected, cli.FilterHostsByDC(hosts, "sas"))
}
