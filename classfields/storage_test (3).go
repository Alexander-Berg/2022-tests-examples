package hosts

import (
	"testing"

	"github.com/YandexClassifieds/cms/cmd/server/clusters"
	pbDC "github.com/YandexClassifieds/cms/pb/cms/domains/datacenters"
	pbHostTypes "github.com/YandexClassifieds/cms/pb/cms/domains/host_types"
	"github.com/YandexClassifieds/cms/test"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/stretchr/testify/require"
	"gorm.io/gorm"
)

func TestStorage_Save(t *testing.T) {
	test.InitTestEnv()

	st, cluster := prepare(t)
	require.NoError(t, st.Save("test", cluster))

	var host *Host
	require.NoError(t, st.base.Get(&host, "name = ?", "test"))
	require.Equal(t, "test", host.Name)
	require.Equal(t, cluster, host.ClusterID)
}

func TestStorage_GetByName(t *testing.T) {
	test.InitTestEnv()

	st, cluster := prepare(t)
	require.NoError(t, st.Save("test", cluster))
	require.NoError(t, st.Save("test2", cluster))

	host, err := st.GetByName("test")
	require.NoError(t, err)
	require.Equal(t, "test", host.Name)
	require.Equal(t, cluster, host.ClusterID)
}

func TestStorage_GetIDByName(t *testing.T) {
	test.InitTestEnv()

	st, cluster := prepare(t)
	require.NoError(t, st.Save("test", cluster))

	var host *Host
	require.NoError(t, st.base.Get(&host, "name = ?", "test"))

	id, err := st.GetIDByName("test")
	require.NoError(t, err)
	require.Equal(t, id, host.ID)
}

func TestStorage_GetNotRemovedByType(t *testing.T) {
	test.InitTestEnv()

	st, cluster := prepare(t)
	require.NoError(t, st.Save("test", cluster))
	require.NoError(t, st.Save("test2", cluster))
	require.NoError(t, st.Save("test3", cluster))

	require.NoError(t, st.SetRemovedByName("test2", true))

	hosts, err := st.GetNotRemovedByType(pbHostTypes.HostType_BAREMETAL)
	require.NoError(t, err)
	require.Equal(t, 2, len(hosts))
}

func TestStorage_GetNotRemoved(t *testing.T) {
	test.InitTestEnv()

	st, cluster := prepare(t)
	require.NoError(t, st.Save("test", cluster))
	require.NoError(t, st.Save("test2", cluster))
	require.NoError(t, st.Save("test3", cluster))

	require.NoError(t, st.SetRemovedByName("test2", true))

	hosts, err := st.GetNotRemoved()
	require.NoError(t, err)
	require.Equal(t, 2, len(hosts))
}

func TestStorage_SetRemovedByName(t *testing.T) {
	test.InitTestEnv()

	st, cluster := prepare(t)
	require.NoError(t, st.Save("test", cluster))

	require.NoError(t, st.SetRemovedByName("test", true))
	host, err := st.GetByName("test")
	require.NoError(t, err)
	require.Equal(t, true, host.Removed)

	require.NoError(t, st.SetRemovedByName("test", false))
	host, err = st.GetByName("test")
	require.NoError(t, err)
	require.Equal(t, false, host.Removed)
}

func TestStorage_SetAutorepairEnableByName(t *testing.T) {
	test.InitTestEnv()

	hostname := "docker-111-sas.prod.vertis.yandex.net"
	st, cluster := prepare(t)

	require.ErrorIs(t, st.SetAutorepairEnableByName(hostname, false), gorm.ErrRecordNotFound)

	require.NoError(t, st.Save(hostname, cluster))

	require.NoError(t, st.SetAutorepairEnableByName(hostname, false))
	host, err := st.GetByName(hostname)
	require.NoError(t, err)
	require.Equal(t, false, host.AutorepairEnable)

	require.NoError(t, st.SetAutorepairEnableByName(hostname, true))
	host, err = st.GetByName(hostname)
	require.NoError(t, err)
	require.Equal(t, true, host.AutorepairEnable)
}

func TestStorage_IsRemovedByName(t *testing.T) {
	test.InitTestEnv()

	st, cluster := prepare(t)
	require.NoError(t, st.Save("test", cluster))

	removed, err := st.IsRemovedByName("test")
	require.NoError(t, err)
	require.Equal(t, false, removed)

	require.NoError(t, st.SetRemovedByName("test", true))
	removed, err = st.IsRemovedByName("test")
	require.NoError(t, err)
	require.Equal(t, true, removed)
}

func TestStorage_UpsertByName(t *testing.T) {
	test.InitTestEnv()

	st, cluster := prepare(t)
	require.NoError(t, st.Save("test", cluster))

	require.NoError(t, st.UpsertByName("test2", cluster))
	host, err := st.GetByName("test2")
	require.NoError(t, err)
	require.Equal(t, cluster, host.ClusterID)
	require.Equal(t, false, host.Removed)

	require.NoError(t, st.SetRemovedByName("test", true))
	require.NoError(t, st.UpsertByName("test", cluster))
	host, err = st.GetByName("test")
	require.NoError(t, err)
	require.Equal(t, cluster, host.ClusterID)
	require.Equal(t, false, host.Removed)
}

func TestStorage_GetDCbyName(t *testing.T) {
	tests := map[string]struct {
		Name string
		DC   pbDC.DC
	}{
		"sas": {
			Name: "docker-01-sas.test.vertis.yandex.net",
			DC:   pbDC.DC_SAS,
		},
		"vla": {
			Name: "docker-01-vla.test.vertis.yandex.net",
			DC:   pbDC.DC_VLA,
		},
		"sas-cloud": {
			Name: "docker-cloud-01-sas.test.vertis.yandex.net",
			DC:   pbDC.DC_SAS,
		},
		"vla-cloud": {
			Name: "docker-cloud-01-vla.test.vertis.yandex.net",
			DC:   pbDC.DC_VLA,
		},
	}

	test.InitTestEnv()

	st, _ := prepare(t)

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			dc := st.GetDCByName(tc.Name)
			require.Equal(t, tc.DC, dc)
		})
	}
}

func prepare(t *testing.T) (*Storage, uint) {
	db := test.NewSeparatedGorm(t)
	log := logrus.New()

	c := clusters.NewStorage(db, log)
	require.NoError(t, c.Save(pbHostTypes.HostType_BAREMETAL, pbDC.DC_SAS, true, 1))
	clusterList, err := c.ListByHostType(pbHostTypes.HostType_BAREMETAL)
	require.NoError(t, err)

	return NewStorage(db, log), clusterList[0].ID
}
