package updater

import (
	"testing"
	"time"

	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/h2p/common/idm"
	sm "github.com/YandexClassifieds/h2p/pb/shiva/service_map"
	"github.com/YandexClassifieds/h2p/test/mocks"
	"github.com/stretchr/testify/mock"
)

var (
	oldOwners = map[string][]idm.Owner{
		"service":        {{Id: "login", Type: idm.USER}, {Id: "test", Type: idm.GROUP}},
		"mysql/mdb00000": {{Id: "login", Type: idm.USER}},
	}
	ownerRoles = map[string][]idm.Owner{
		"/service/service/provides/owner/":                 {},
		"/service/mysql/instance/mdb00000/database/owner/": {{Id: "login", Type: idm.USER}},
	}
	actualMaps = []*sm.ServiceMap{
		{
			Type: sm.ServiceType_service,
			Path: "maps/service.yml",
			Owners: []string{
				"https://staff.yandex-team.ru/login",
				"https://staff.yandex-team.ru/departments/test2/",
			},
		},
		{
			Type: sm.ServiceType_mdb_mysql,
			MdbCluster: &sm.MDBCluster{
				TestId: "mdb00000",
				ProdId: "mdb11111",
			},
			Owners: []string{
				"https://staff.yandex-team.ru/login",
			},
		},
		{
			Type: sm.ServiceType_mdb_postgresql,
			MdbCluster: &sm.MDBCluster{
				TestId: "mdb00000",
				ProdId: "mdb11111",
			},
			Owners: []string{
				"https://staff.yandex-team.ru/login2",
			},
		},
	}
)

func TestService_updateOwners(t *testing.T) {
	db := &mocks.IMngr{}
	db.On("GetOwners").Return(oldOwners, nil)
	db.On("AddOwner", mock.Anything, mock.Anything).Return(nil)
	db.On("DeleteOwner", mock.Anything, mock.Anything).Return(nil)
	db.On("GetOwnerRolesMap").Return(ownerRoles, nil)

	sMap := &mocks.IServiceMap{}
	sMap.On("GetServices", mock.Anything).Return(actualMaps, nil)

	idmService := &mocks.IIDMService{}
	idmService.On("GetNodeDescription", mock.Anything).Return(nil, nil)
	idmService.On("AddRoleOwner", mock.Anything, mock.Anything).Return(nil)
	idmService.On("DeleteRoleOwner", mock.Anything, mock.Anything).Return(nil)

	staff := &mocks.IStaff{}
	staff.On("CheckLogin", "login").Return(nil)
	staff.On("CheckLogin", "login2").Return(nil)
	staff.On("GetGroupId", "test2").Return("test2", nil)

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

	s.updateOwners()

	db.AssertNumberOfCalls(t, "GetOwners", 2)
	db.AssertNumberOfCalls(t, "GetOwnerRolesMap", 1)
	db.AssertNumberOfCalls(t, "AddOwner", 2)
	db.AssertCalled(t, "AddOwner", "service", idm.Owner{
		Id: "test2", Type: idm.GROUP,
	})
	db.AssertCalled(t, "AddOwner", "postgresql/mdb00000", idm.Owner{
		Id: "login2", Type: idm.USER,
	})
	db.AssertNumberOfCalls(t, "DeleteOwner", 1)
	db.AssertCalled(t, "DeleteOwner", "service", idm.Owner{
		Id: "test", Type: idm.GROUP,
	})

	idmService.AssertNumberOfCalls(t, "GetNodeDescription", 2)
	idmService.AssertCalled(t, "GetNodeDescription", "/service/service/provides/owner")
	idmService.AssertCalled(t, "GetNodeDescription", "/service/postgresql/instance/mdb00000/database/owner")
	idmService.AssertNumberOfCalls(t, "AddRoleOwner", 3)
	idmService.AssertCalled(t, "AddRoleOwner", "service", idm.Owner{Id: "test2", Type: idm.GROUP})
	idmService.AssertCalled(t, "AddRoleOwner", "postgresql/mdb00000", idm.Owner{Id: "login2", Type: idm.USER})
	idmService.AssertCalled(t, "AddRoleOwner", "service", idm.Owner{Id: "login", Type: idm.USER})
	idmService.AssertNumberOfCalls(t, "DeleteRoleOwner", 1)
	idmService.AssertCalled(t, "DeleteRoleOwner", "service", idm.Owner{Id: "test", Type: idm.GROUP})
}
