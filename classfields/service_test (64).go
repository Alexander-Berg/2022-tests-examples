package migrator

import (
	"embed"
	_ "embed"
	"sort"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/common/storage/model"
	"github.com/YandexClassifieds/shiva/pkg/i/kv"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock/election"
	iMock "github.com/YandexClassifieds/shiva/test/mock/mockery/mocks/i"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"gorm.io/gorm"
)

//go:embed migrations
var testDir embed.FS

func TestMigrateUp(t *testing.T) {
	test.InitTestEnv()

	testCases := []struct {
		name         string
		electionMock *election.Election
	}{
		{
			name:         "migration up",
			electionMock: election.NewElectionStub(),
		},

		{
			name: "migration up after leader change",
			electionMock: election.NewElectionStub().
				SetIsLeaderCalls(false, true).
				SetSateChan(false, false, true),
		},
	}

	type user struct {
		model.Model
		DollarSalary int `gorm:"index:test_idx;check:test_check, dollar_salary <= 1000"`
	}

	type userBefore struct {
		model.Model
		Salary int
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			db := test.NewSeparatedGorm(t)

			require.NoError(t, db.Table("users").AutoMigrate(&userBefore{}))
			require.NoError(t, db.Table("users").Create(&userBefore{Salary: 10000}).Error)
			require.NoError(t, db.Table("users").Create(&userBefore{}).Error)

			service := makeService(t, db)
			defer service.Stop()

			err := service.storage.Save(
				&Migration{
					MigrationVersion: 1,
					AppVersion:       "tc20210715.130436",
					State:            Reverted,
					Up:               []byte("wrong sql"),
				})
			require.NoError(t, err)
			service.AddModel(&user{}, false)

			require.NoError(t, service.MigrateColumnsAndTables(false, &user{}))
			require.False(t, service.HasIndex(&user{}, "test_idx"))

			require.False(t, service.HasConstraint(&user{}, "test_check"))

			service.Election = tc.electionMock

			err = service.Migrate()
			require.NoError(t, err)
			require.True(t, service.HasIndex(&user{}, "test_idx"))
			require.True(t, service.HasConstraint(&user{}, "test_check"))

			migrations := []*Migration{}
			require.NoError(t, db.Find(&migrations).Error)
			require.Len(t, migrations, 3)

			for _, m := range migrations {
				require.Equal(t, Applied, m.State)
			}
		})
	}

}

func TestMigrateDown(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)

	ms := []*Migration{
		{
			MigrationVersion: 2,
			AppVersion:       "tc20210715.130435",
			State:            Applied,
		},
		{
			MigrationVersion: 3,
			AppVersion:       "tc20210715.130437",
			State:            Applied,
		},
	}

	service := makeService(t, db)
	defer service.Stop()
	err := service.storage.Save(ms...)
	require.NoError(t, err)

	require.NoError(t, service.Migrate())

	migrations := []*Migration{}
	require.NoError(t, db.Find(&migrations).Error)

	sort.Slice(migrations, func(i, j int) bool {
		return migrations[i].MigrationVersion < migrations[j].MigrationVersion
	})

	require.Equal(t, Applied, migrations[0].State)
	require.Equal(t, Reverted, migrations[1].State)
}

func TestMigrationDrop(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)

	ms := []*Migration{
		{
			MigrationVersion: 2,
			AppVersion:       "tc20210715.130435",
			State:            Applied,
		},
		{
			MigrationVersion: 3,
			AppVersion:       "tc20210715.130436",
			State:            Applied,
		},
	}

	service := makeService(t, db)
	defer service.Stop()
	err := service.storage.Save(ms...)
	require.NoError(t, err)

	require.NoError(t, db.Exec("update schema_migrations set updated_at = ? where migration_version = 2", time.Now().Add(-time.Hour*24*31)).Error)

	require.NoError(t, service.Migrate())

	migrations := []*Migration{}
	require.NoError(t, db.Find(&migrations).Error)

	require.Len(t, migrations, 1)
	require.Equal(t, int64(3), migrations[0].MigrationVersion)
}

func TestLastInstanceMigrations(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)

	service := makeService(t, db)
	defer service.Stop()

	kvMock := &iMock.KV{}
	kvMock.On("All").
		Return(map[string]kv.Value{
			test.RandString(10): &MetaInfo{Version: service.conf.appVersion},
			test.RandString(10): &MetaInfo{Version: "some-ver"}}, nil)
	service.kv = kvMock

	go service.lastInstanceMigration(nil, nil)

	ch := service.Election.State()

	require.Never(t, func() bool {
		_, ok := <-ch
		return !ok
	}, time.Second*3, time.Second/2)

	kvMock = &iMock.KV{}
	kvMock.On("All").
		Return(map[string]kv.Value{
			test.RandString(10): &MetaInfo{Version: service.conf.appVersion},
			test.RandString(10): &MetaInfo{Version: service.conf.appVersion}}, nil)
	service.kv = kvMock

	require.Eventually(t, func() bool {
		_, ok := <-ch
		return !ok
	}, time.Second*3, time.Second/2)
}

func TestWaitMigrationEnd(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)

	service := makeService(t, db)
	defer service.Stop()
	e := election.NewElectionStub()
	e.DefaultLeader = false
	service.Election = e

	require.Never(t, func() bool {
		kvMock := &iMock.KV{}
		kvMock.On("All").
			Return(map[string]kv.Value{
				test.RandString(10): &MetaInfo{Version: "previous version"},
			}, nil)
		service.kv = kvMock
		service.waitMigrationEnd()
		return true
	}, time.Second*3, time.Second*3/2)

	kvMock := &iMock.KV{}
	kvMock.On("All").
		Return(map[string]kv.Value{
			test.RandString(10): &MetaInfo{Version: "previous version"},
			test.RandString(10): &MetaInfo{Version: service.conf.appVersion},
		}, nil)
	service.kv = kvMock

	require.Eventually(t, func() bool {
		service.waitMigrationEnd()
		return true
	}, time.Second*3, time.Second*3/2)
	time.Sleep(10 * time.Second)
}

func TestMetaInfo(t *testing.T) {
	m := &MetaInfo{Version: "version1"}
	var value kv.Value
	value = m
	bytes, err := m.Marshal()
	require.NoError(t, err)
	m2 := value.New()
	err = m2.Unmarshal(bytes)
	require.NoError(t, err)
	assert.Equal(t, m.Version, m2.(*MetaInfo).Version)
}

func TestSkipMigrationByVersion(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)

	service := makeService(t, db)
	defer service.Stop()
	service.conf.appVersion = "hot-fix-test"
	require.NoError(t, service.Migrate())

	migrations := []*Migration{}
	require.NoError(t, db.Find(&migrations).Error)

	require.Len(t, migrations, 3)
	for _, m := range migrations {
		require.Equal(t, New, m.State)
	}
}

func makeService(t *testing.T, db *gorm.DB) *Service {
	e := election.NewElectionStub()
	kvMock := &iMock.KV{}
	kvMock.On("All").Return(nil, nil)
	kvMock.On("Save", mock.Anything, mock.Anything).Return(nil)
	kvMock.On("Save", mock.Anything, mock.Anything).Return(nil)

	service := NewService(db, test.NewLogger(t), &testDir, e, kvMock)
	service.conf.isTest = false
	service.conf.appVersion = "tc20210715.130436"

	return service
}
