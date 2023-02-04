package favorite

import (
	"fmt"
	"testing"

	spb "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/require"
)

var (
	login       = "shiva-test"
	serviceName = "shiva"
	serviceType = spb.ServiceType_service
	serviceMap  = `
name: shiva
description: Deployment system
type: service
`
	serviceName2 = "shiva-batch"
	serviceType2 = spb.ServiceType_batch
	serviceMap2  = `
name: shiva-batch
description: Deployment periodic task
type: batch
`
)

func TestAddFavorite(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)

	staffSvc := staff.NewService(db, staffapi.NewApi(staffapi.NewConf(), log), log)
	user, err := staffSvc.CreateOrUpdate(&staffapi.Person{Login: login})
	require.NoError(t, err)

	favoriteSvc := NewService(db, log)
	err = favoriteSvc.AddFavorite(user, serviceName, serviceType)
	require.NoError(t, err)
}

func TestDuplicate(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)

	staffSvc := staff.NewService(db, staffapi.NewApi(staffapi.NewConf(), log), log)
	user, err := staffSvc.CreateOrUpdate(&staffapi.Person{Login: login})
	require.NoError(t, err)

	favoriteSvc := NewService(db, test.NewLogger(t))
	err = favoriteSvc.AddFavorite(user, serviceName, serviceType)
	require.NoError(t, err)
	err = favoriteSvc.AddFavorite(user, serviceName, serviceType)
	require.NoError(t, err)
}

func TestRemoveEmptyFavorite(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	service := NewService(db, test.NewLogger(t))
	err := service.RemoveFavorite(&staff.User{Login: login}, serviceName, serviceType)
	require.NoError(t, err)
}

func TestListFavorite(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)

	staffSvc := staff.NewService(db, staffapi.NewApi(staffapi.NewConf(), log), log)
	user, err := staffSvc.CreateOrUpdate(&staffapi.Person{Login: login})
	require.NoError(t, err)

	favoriteSvc := NewService(db, test.NewLogger(t))
	mapService := service_map.NewService(db, test.NewLogger(t), service_change.NewNotificationMock())

	err = mapService.ReadAndSave([]byte(serviceMap), 42, "maps/shiva.yml")
	require.NoError(t, err)
	err = mapService.ReadAndSave([]byte(serviceMap2), 42, "maps/shiva-batch.yml")
	require.NoError(t, err)

	err = favoriteSvc.AddFavorite(user, serviceName, serviceType)
	require.NoError(t, err)

	f, err := favoriteSvc.GetFavorite(user)
	require.NoError(t, err)
	require.True(t, contains(f, []string{serviceName}, []spb.ServiceType{serviceType}))

	err = favoriteSvc.AddFavorite(user, serviceName2, serviceType2)
	require.NoError(t, err)

	f, err = favoriteSvc.GetFavorite(user)
	require.NoError(t, err)
	require.True(t, contains(f, []string{serviceName, serviceName2}, []spb.ServiceType{serviceType, serviceType2}))

	err = favoriteSvc.RemoveFavorite(user, serviceName, serviceType)
	require.NoError(t, err)

	f, err = favoriteSvc.GetFavorite(user)
	require.NoError(t, err)
	require.True(t, contains(f, []string{serviceName2}, []spb.ServiceType{serviceType2}))

	err = favoriteSvc.AddFavorite(user, serviceName, serviceType)
	require.NoError(t, err)

	f, err = favoriteSvc.GetFavorite(user)
	require.NoError(t, err)
	require.True(t, contains(f, []string{serviceName, serviceName2}, []spb.ServiceType{serviceType, serviceType2}))
}

// function expects unique pairs (names[i], types[i])
func contains(f []*Favorite, names []string, types []spb.ServiceType) bool {
	if len(names) != len(types) {
		panic(fmt.Sprintf("unexpected length mismatch: %v vs %v", len(names), len(types)))
	}
	if len(f) != len(names) {
		return false
	}
	// dummy search
	for i := 0; i < len(names); i++ {
		found := false
		for j := 0; j < len(f); j++ {
			if f[j].Name == names[i] && f[j].Type == types[i] {
				found = true
				break
			}
		}
		if !found {
			return false
		}
	}
	return true
}
