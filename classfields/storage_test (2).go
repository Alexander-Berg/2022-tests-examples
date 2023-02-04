package clusters

import (
	"testing"

	pbDC "github.com/YandexClassifieds/cms/pb/cms/domains/datacenters"
	pbHostTypes "github.com/YandexClassifieds/cms/pb/cms/domains/host_types"
	"github.com/YandexClassifieds/cms/test"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/stretchr/testify/require"
	"gorm.io/gorm"
)

var (
	hostType   = pbHostTypes.HostType_BAREMETAL
	dc1        = pbDC.DC_SAS
	dc2        = pbDC.DC_VLA
	autorepair = true
)

func TestStorage_SaveCluster(t *testing.T) {
	test.InitTestEnv()
	log := logrus.New()
	db := test.NewSeparatedGorm(t)

	storage := NewStorage(db, log)
	require.NoError(t, storage.Save(hostType, dc1, autorepair, 1))

	var cluster *Cluster
	require.NoError(t, storage.base.Get(&cluster, "host_type = ?", hostType))
	require.Equal(t, hostType, cluster.HostType)
	require.Equal(t, dc1, cluster.DC)
	require.Equal(t, autorepair, cluster.AutorepairEnable)
	require.False(t, cluster.Removed)
}

func TestStorage_GetClustersByHostType(t *testing.T) {
	test.InitTestEnv()
	log := logrus.New()
	db := test.NewSeparatedGorm(t)

	storage := NewStorage(db, log)
	err := storage.Save(hostType, dc1, autorepair, 1)
	require.NoError(t, err)

	cls, err := storage.ListByHostType(hostType)
	require.Len(t, cls, 1)
	require.Equal(t, hostType, cls[0].HostType)
	require.Equal(t, dc1, cls[0].DC)
	require.Equal(t, autorepair, cls[0].AutorepairEnable)
	require.Equal(t, false, cls[0].Removed)
}

func TestStorage_GetClustersMapByHostType(t *testing.T) {
	log := logrus.New()
	db := test.NewSeparatedGorm(t)

	storage := NewStorage(db, log)
	err := storage.Save(hostType, dc1, autorepair, 1)
	require.NoError(t, err)
	err = storage.Save(hostType, dc2, autorepair, 1)
	require.NoError(t, err)

	m, err := storage.GetMapByHostType(hostType)
	require.Len(t, m, 2)
	require.NotEmpty(t, m[dc1])
	require.NotEmpty(t, m[dc2])
}

func TestStorage_Disable(t *testing.T) {
	log := logrus.New()
	db := test.NewSeparatedGorm(t)

	storage := NewStorage(db, log)

	require.NoError(t, storage.Save(hostType, dc1, autorepair, 1))
	require.NoError(t, storage.Disable(hostType, dc1))

	l, err := storage.ListByHostType(hostType)
	require.NoError(t, err)
	require.Equal(t, 1, len(l))
	require.Equal(t, false, l[0].AutorepairEnable)
}

func TestStorage_Enable(t *testing.T) {
	log := logrus.New()
	db := test.NewSeparatedGorm(t)

	storage := NewStorage(db, log)

	require.NoError(t, storage.Save(hostType, dc1, false, 1))
	require.NoError(t, storage.Enable(hostType, dc1))

	l, err := storage.ListByHostType(hostType)
	require.NoError(t, err)
	require.Equal(t, 1, len(l))
	require.Equal(t, true, l[0].AutorepairEnable)
}

func TestStorage_SetAutorepairEnableByTypeAndDC(t *testing.T) {
	log := logrus.New()
	db := test.NewSeparatedGorm(t)

	storage := NewStorage(db, log)

	require.ErrorIs(t, storage.SetAutorepairEnableByTypeAndDC(hostType, dc1, true), gorm.ErrRecordNotFound)
	require.NoError(t, storage.Save(hostType, dc1, false, 1))

	require.NoError(t, storage.SetAutorepairEnableByTypeAndDC(hostType, dc1, true))
	l, err := storage.ListByHostType(hostType)
	require.NoError(t, err)
	require.Equal(t, 1, len(l))
	require.Equal(t, true, l[0].AutorepairEnable)

	require.NoError(t, storage.SetAutorepairEnableByTypeAndDC(hostType, dc1, false))
	l, err = storage.ListByHostType(hostType)
	require.NoError(t, err)
	require.Equal(t, 1, len(l))
	require.Equal(t, false, l[0].AutorepairEnable)
}
