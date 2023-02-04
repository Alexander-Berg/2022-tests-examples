package updater

import (
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/h2p/cmd/h2p-idm/models"
	"github.com/YandexClassifieds/h2p/common/idm"
	sm "github.com/YandexClassifieds/h2p/pb/shiva/service_map"
	"github.com/YandexClassifieds/h2p/test/mocks"
	"github.com/stretchr/testify/mock"
	"testing"
	"time"
)

var (
	oldMysqlEntries = models.Entries{
		"/mdb00000/old/ro/false": {
			Name:     "old",
			Cluster:  "mdb00000",
			Database: "old",
			Mode:     "ro",
			Sox:      false,
			Type:     models.Mysql,
			Old:      true,
		},
		"/mdb00000/old/rw/false": {
			Name:     "old",
			Cluster:  "mdb00000",
			Database: "old",
			Mode:     "rw",
			Sox:      false,
			Type:     models.Mysql,
			Old:      true,
		},
		"/mdb00000/current/ro/false": {
			Name:     "current",
			Cluster:  "mdb00000",
			Database: "current",
			Mode:     "ro",
			Sox:      false,
			Type:     models.Mysql,
			Old:      true,
		},
		"/mdb00000/current/rw/false": {
			Name:     "current",
			Cluster:  "mdb00000",
			Database: "current",
			Mode:     "rw",
			Sox:      false,
			Type:     models.Mysql,
			Old:      true,
		},
	}
	newMysqlMaps = []*sm.ServiceMap{
		{
			Name: "test",
			MdbCluster: &sm.MDBCluster{
				TestId: "mdb00000",
				ProdId: "mdb11111",
			},
			Provides: []*sm.ServiceProvides{
				{
					Name: "current",
				},
			},
			Type: sm.ServiceType_mdb_mysql,
		},
		{
			Name: "new-test",
			MdbCluster: &sm.MDBCluster{
				TestId: "mdb22222",
				ProdId: "mdb33333",
			},
			Provides: []*sm.ServiceProvides{
				{
					Name: "new",
				},
			},
			Type: sm.ServiceType_mdb_mysql,
		},
	}
)

func TestService_updateMySQL(t *testing.T) {
	db := &mocks.IMngr{}
	db.On("GetEntries", models.Mysql).Return(oldMysqlEntries, nil)
	db.On("AddEntry", mock.Anything).Return(nil)
	db.On("DeleteEntry", mock.Anything).Return(nil)

	sMap := &mocks.IServiceMap{}
	sMap.On("GetServices", []sm.ServiceType{sm.ServiceType_mdb_mysql}).Return(newMysqlMaps, nil)

	idmService := &mocks.IIDMService{}
	idmService.On("AddNodeRecursive", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything)
	idmService.On("DeleteNode", mock.Anything)

	staff := &mocks.IStaff{}

	s := New(
		db,
		sMap,
		idmService,
		staff,
		1*time.Minute,
		1*time.Second,
		"test",
		logrus.New("info"),
	)

	s.updateMySQL()

	db.AssertNumberOfCalls(t, "GetEntries", 1)
	db.AssertNumberOfCalls(t, "AddEntry", 2)
	db.AssertCalled(t, "AddEntry", &models.Entry{
		Name: "new-test", Cluster: "mdb22222", Database: "new", Mode: "ro", Type: models.Mysql, New: true,
	})
	db.AssertCalled(t, "AddEntry", &models.Entry{
		Name: "new-test", Cluster: "mdb22222", Database: "new", Mode: "rw", Type: models.Mysql, New: true,
	})
	db.AssertNumberOfCalls(t, "DeleteEntry", 2)
	db.AssertCalled(t, "DeleteEntry", &models.Entry{
		Name: "old", Cluster: "mdb00000", Database: "old", Mode: "ro", Type: models.Mysql, Old: true,
	})
	db.AssertCalled(t, "DeleteEntry", &models.Entry{
		Name: "old", Cluster: "mdb00000", Database: "old", Mode: "rw", Type: models.Mysql, Old: true,
	})

	var trueNil *[]idm.RoleField
	idmService.AssertNumberOfCalls(t, "AddNodeRecursive", 2)
	idmService.AssertCalled(t, "AddNodeRecursive",
		"/service/mysql/instance/", []string{"mdb22222", "database", "new", "mode", "ro"},
		"mysql/mdb22222/new/ro", trueNil, true)
	idmService.AssertCalled(t, "AddNodeRecursive",
		"/service/mysql/instance/", []string{"mdb22222", "database", "new", "mode", "rw"},
		"mysql/mdb22222/new/rw", trueNil, true)
	idmService.AssertNumberOfCalls(t, "DeleteNode", 2)
	idmService.AssertCalled(t, "DeleteNode", "service/mysql/instance/mdb00000/database/old/mode/ro/")
	idmService.AssertCalled(t, "DeleteNode", "service/mysql/instance/mdb00000/database/old/mode/rw/")
}
