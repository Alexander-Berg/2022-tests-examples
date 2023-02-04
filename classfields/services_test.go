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
	oldServicesEntries = models.Entries{
		"/service/old/false": {
			Name:     "service",
			Provides: "old",
			Sox:      false,
			Type:     models.Service,
			Old:      true,
		},
		"/service/current/false": {
			Name:     "service",
			Provides: "current",
			Sox:      false,
			Type:     models.Service,
			Old:      true,
		},
		"/service/monitoring/false": {
			Name:     "service",
			Provides: "monitoring",
			Sox:      false,
			Type:     models.Service,
			Old:      true,
		},
	}
	newServicesMaps = []*sm.ServiceMap{
		{
			Name: "service",
			Provides: []*sm.ServiceProvides{
				{
					Name: "current",
				},
			},
			Type: sm.ServiceType_service,
		},
		{
			Name: "new-service",
			Provides: []*sm.ServiceProvides{
				{
					Name: "new",
				},
			},
			Type: sm.ServiceType_service,
		},
		{
			Name: "new-conductor",
			Provides: []*sm.ServiceProvides{
				{
					Name: "new",
				},
			},
			Type: sm.ServiceType_conductor,
		},
		{
			Name: "new-jenkins",
			Provides: []*sm.ServiceProvides{
				{
					Name: "new",
				},
			},
			Type: sm.ServiceType_jenkins,
		},
	}
)

func TestService_updateServices(t *testing.T) {
	db := &mocks.IMngr{}
	db.On("GetEntries", models.Service).Return(oldServicesEntries, nil)
	db.On("AddEntry", mock.Anything).Return(nil)
	db.On("DeleteEntry", mock.Anything).Return(nil)

	sMap := &mocks.IServiceMap{}
	sMap.On("GetServices", []sm.ServiceType{sm.ServiceType_service, sm.ServiceType_conductor, sm.ServiceType_jenkins}).Return(newServicesMaps, nil)

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

	s.updateServices()

	db.AssertNumberOfCalls(t, "GetEntries", 1)
	db.AssertNumberOfCalls(t, "AddEntry", 4)
	db.AssertCalled(t, "AddEntry", &models.Entry{
		Name: "new-service", Provides: "new", Type: models.Service, New: true,
	})
	db.AssertCalled(t, "AddEntry", &models.Entry{
		Name: "new-service", Provides: "monitoring", Type: models.Service, New: true,
	})
	db.AssertCalled(t, "AddEntry", &models.Entry{
		Name: "new-jenkins", Provides: "new", Type: models.Service, New: true,
	})
	db.AssertCalled(t, "AddEntry", &models.Entry{
		Name: "new-conductor", Provides: "new", Type: models.Service, New: true,
	})
	db.AssertNumberOfCalls(t, "DeleteEntry", 1)
	db.AssertCalled(t, "DeleteEntry", &models.Entry{
		Name: "service", Provides: "old", Type: models.Service, Old: true,
	})

	var trueNil *[]idm.RoleField
	idmService.AssertNumberOfCalls(t, "AddNodeRecursive", 4)
	idmService.AssertCalled(t, "AddNodeRecursive",
		"/service/", []string{"new-service", "provides", "new"},
		"service/new-service/new", trueNil, true)
	idmService.AssertCalled(t, "AddNodeRecursive",
		"/service/", []string{"new-service", "provides", "monitoring"},
		"service/new-service/monitoring", trueNil, true)
	idmService.AssertCalled(t, "AddNodeRecursive",
		"/service/", []string{"new-conductor", "provides", "new"},
		"service/new-conductor/new", trueNil, true)
	idmService.AssertCalled(t, "AddNodeRecursive",
		"/service/", []string{"new-jenkins", "provides", "new"},
		"service/new-jenkins/new", trueNil, true)
	idmService.AssertNumberOfCalls(t, "DeleteNode", 1)
	idmService.AssertCalled(t, "DeleteNode", "service/service/provides/old/")
}
