package hostsStatus

import (
	"sort"
	"testing"

	"github.com/YandexClassifieds/cms/cmd/server/clusters"
	"github.com/YandexClassifieds/cms/cmd/server/hosts"
	pbDC "github.com/YandexClassifieds/cms/pb/cms/domains/datacenters"
	pbHostStatuses "github.com/YandexClassifieds/cms/pb/cms/domains/host_statuses"
	pbHostTypes "github.com/YandexClassifieds/cms/pb/cms/domains/host_types"
	"github.com/YandexClassifieds/cms/test"
	"github.com/YandexClassifieds/go-common/log"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/stretchr/testify/require"
	"gorm.io/gorm"
)

var (
	statusNew = &HostsStatus{
		Status: pbHostStatuses.Status_NEW,
	}
	statusReady = &HostsStatus{
		Status: pbHostStatuses.Status_READY,
	}

	hostType = pbHostTypes.HostType_BAREMETAL
	dc       = pbDC.DC_SAS
)

func TestStorage_Save(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)
	log := logrus.New()
	prepare(t, db, log)

	storage := NewStorage(db, log)

	saveStatuses := []*HostsStatus{statusNew, statusReady}
	for _, status := range saveStatuses {
		require.NoError(t, storage.Save(status))
	}

	var statuses []*HostsStatus
	require.NoError(t, db.Find(&statuses).Error)
	require.Equal(t, len(saveStatuses), len(statuses))

	// sort because of different time.Time fields in Gorm model
	sort.Slice(statuses, func(i, j int) bool {
		return statuses[i].HostID < statuses[j].HostID
	})
	sort.Slice(saveStatuses, func(i, j int) bool {
		return saveStatuses[i].HostID < saveStatuses[j].HostID
	})

	for i := 0; i < len(statuses); i++ {
		require.Equal(t, saveStatuses[i].HostID, statuses[i].HostID)
		require.Equal(t, saveStatuses[i].Status, statuses[i].Status)
	}
}

func TestStorage_GetByHostID(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)
	log := logrus.New()
	prepare(t, db, log)

	storage := NewStorage(db, log)

	require.NoError(t, storage.Save(statusNew))
	require.NoError(t, storage.Save(statusReady))

	t.Run("get exists", func(t *testing.T) {
		status, err := storage.GetByHostID(statusNew.HostID)
		require.NoError(t, err)
		require.Equal(t, statusNew.HostID, status.HostID)
		require.Equal(t, pbHostStatuses.Status_NEW, status.Status)
	})

	t.Run("get not exists", func(t *testing.T) {
		_, err := storage.GetByHostID(10000)
		require.ErrorIs(t, err, gorm.ErrRecordNotFound)
	})
}

func TestStorage_UpdateByHostID(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)
	log := logrus.New()
	prepare(t, db, log)

	storage := NewStorage(db, log)

	require.NoError(t, storage.Save(statusNew))
	require.NoError(t, storage.Save(statusReady))

	t.Run("update exists", func(t *testing.T) {
		require.NoError(t, storage.UpdateByHostID(statusReady.HostID, pbHostStatuses.Status_FOUND_PROBLEM))

		status, err := storage.GetByHostID(statusReady.HostID)
		require.NoError(t, err)
		require.Equal(t, statusReady.HostID, status.HostID)
		require.Equal(t, pbHostStatuses.Status_FOUND_PROBLEM, status.Status)
	})

	t.Run("update not exists", func(t *testing.T) {
		require.NoError(t, storage.UpdateByHostID(10000, pbHostStatuses.Status_FOUND_PROBLEM))

		_, err := storage.GetByHostID(10000)
		require.ErrorIs(t, err, gorm.ErrRecordNotFound)
	})
}

func TestStorage_GetNotReadyCount(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)
	log := logrus.New()
	prepare(t, db, log)

	storage := NewStorage(db, log)

	require.NoError(t, storage.Save(statusNew))
	require.NoError(t, storage.Save(statusReady))

	count, err := storage.GetNotReadyCount(hostType, dc)
	require.NoError(t, err)
	require.Equal(t, uint64(1), count)
}

func TestStorage_RemoveByHostID(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)
	log := logrus.New()
	prepare(t, db, log)

	storage := NewStorage(db, log)
	require.NoError(t, storage.Save(statusNew))
	require.NoError(t, storage.Save(statusReady))

	require.NoError(t, storage.RemoveByHostID(statusReady.HostID))
	_, err := storage.GetByHostID(statusReady.HostID)
	require.ErrorIs(t, err, gorm.ErrRecordNotFound)
}

func TestStorage_GetByHostTypeAndDC(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)
	log := logrus.New()
	prepare(t, db, log)

	storage := NewStorage(db, log)
	require.NoError(t, storage.Save(statusNew))
	require.NoError(t, storage.Save(statusReady))
	require.NoError(t, storage.RemoveByHostID(statusReady.HostID))

	r, err := storage.GetByHostTypeAndDC(hostType, dc)
	require.NoError(t, err)
	require.Len(t, r, 1)
	require.NotEmpty(t, r[0].HostID)
	require.NotEmpty(t, r[0].Host)
	require.NotEmpty(t, r[0].Status)
}

func prepare(t *testing.T, db *gorm.DB, log log.Logger) {
	t.Helper()

	clusterStorage := clusters.NewStorage(db, log)
	require.NoError(t, clusterStorage.Save(hostType, dc, true, 1))
	clusters, err := clusterStorage.GetMapByHostType(hostType)
	require.NoError(t, err)
	hostsStorage := hosts.NewStorage(db, log)
	require.NoError(t, hostsStorage.Save("test1", clusters[dc]))
	id, err := hostsStorage.GetIDByName("test1")
	require.NoError(t, err)
	statusNew.HostID = id
	require.NoError(t, hostsStorage.Save("test2", clusters[dc]))
	id, err = hostsStorage.GetIDByName("test2")
	require.NoError(t, err)
	statusReady.HostID = id
}
