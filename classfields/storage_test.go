package checks

import (
	"testing"

	pbCheckStatuses "github.com/YandexClassifieds/cms/pb/cms/domains/check_statuses"
	pbChecks "github.com/YandexClassifieds/cms/pb/cms/domains/checks"
	"github.com/YandexClassifieds/cms/test"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/stretchr/testify/require"
	"gorm.io/gorm"
)

var (
	checkOk = &Check{
		Type:        pbChecks.Check_CONSUL,
		Status:      pbCheckStatuses.Status_OK,
		Description: "consul ok",
	}
)

func TestStorage_getIDByHostID(t *testing.T) {
	test.InitTestEnv()
	log := logrus.New()
	db := test.NewSeparatedGorm(t)

	hostStorage := prepareOld(t, db, log)
	storage := NewStorage(db, log)

	hostID, err := hostStorage.GetIDByName("test1")
	require.NoError(t, err)

	checkOk.HostID = hostID
	require.NoError(t, storage.base.Save(checkOk))

	check := &Check{}
	require.NoError(t, storage.base.Get(&check, "host_id = ? and type = ?", hostID, checkOk.Type))

	t.Run("get exists", func(t *testing.T) {
		checkID, err := storage.getIDByHostID(1, checkOk.Type)
		require.NoError(t, err)
		require.Equal(t, check.ID, checkID)
	})

	t.Run("get not exists", func(t *testing.T) {
		_, err := storage.getIDByHostID(999, pbChecks.Check_ANSIBLE_PULL)
		require.ErrorIs(t, err, gorm.ErrRecordNotFound)
	})
}

func TestStorage_updateCheckByID(t *testing.T) {
	test.InitTestEnv()
	log := logrus.New()
	db := test.NewSeparatedGorm(t)

	hostStorage := prepareOld(t, db, log)
	storage := NewStorage(db, log)

	hostID, err := hostStorage.GetIDByName("test1")
	require.NoError(t, err)

	checkOk.HostID = hostID
	require.NoError(t, storage.base.Save(checkOk))

	check := &Check{}
	require.NoError(t, storage.base.Get(&check, "host_id = ? and type = ?", hostID, checkOk.Type))

	t.Run("update exists", func(t *testing.T) {
		require.NoError(t, storage.updateByID(check.ID, pbCheckStatuses.Status_WARN, "warning"))

		require.NoError(t, storage.base.Get(&check, "host_id = ? and type = ?", hostID, checkOk.Type))
		require.Equal(t, check.Status, pbCheckStatuses.Status_WARN)
		require.Equal(t, check.Description, "warning")
	})
}
