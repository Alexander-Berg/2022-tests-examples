package handlers

import (
	"fmt"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/h2p/common/idm"
	"github.com/YandexClassifieds/h2p/common/serviceDiscovery"
	"github.com/YandexClassifieds/h2p/test/mocks"
	"github.com/YandexClassifieds/ssh"
	"github.com/hashicorp/consul/api"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"testing"
)

func createSshContextMock(verdict interface{}) ssh.Context {
	ctx := &mocks.SSHContext{}
	ctx.On("SetValue", mock.Anything, mock.Anything).Return()
	ctx.On("User").Return("test")
	ctx.On("Value", "fingerprint").Return("[fingerprint]")
	ctx.On("Value", mock.Anything).Return(verdict)

	return ctx
}

func createSDMock() serviceDiscovery.IServiceDiscovery {
	sdMock := &mocks.IServiceDiscovery{}
	sdMock.On("Datacenters").Return([]string{"sas"}, nil)

	sdMock.On("ConsulService", "mdb-ro-role", mock.Anything, mock.Anything).Return([]*api.CatalogService{{
		ServicePort: 3306,
	}}, nil, nil)
	sdMock.On("ConsulService", "mdb-rw-role", mock.Anything, mock.Anything).Return([]*api.CatalogService{{
		ServicePort: 3306,
	}}, nil, nil)
	sdMock.On("ConsulService", "mdb-ro-role-rw", mock.Anything, mock.Anything).Return([]*api.CatalogService{{
		ServicePort: 3306,
	}}, nil, nil)
	sdMock.On("ConsulService", "mdb-rw-test", mock.Anything, mock.Anything).Return([]*api.CatalogService{{
		ServicePort: 3306,
	}}, nil, nil)

	sdMock.On("ConsulService", "pg-ro-role", mock.Anything, mock.Anything).Return([]*api.CatalogService{{
		ServicePort: 6432,
	}}, nil, nil)
	sdMock.On("ConsulService", "pg-rw-role", mock.Anything, mock.Anything).Return([]*api.CatalogService{{
		ServicePort: 6432,
	}}, nil, nil)
	sdMock.On("ConsulService", "pg-ro-role-rw", mock.Anything, mock.Anything).Return([]*api.CatalogService{{
		ServicePort: 6432,
	}}, nil, nil)
	sdMock.On("ConsulService", "pg-rw-test", mock.Anything, mock.Anything).Return([]*api.CatalogService{{
		ServicePort: 6432,
	}}, nil, nil)

	sdMock.On("Find", "not-valid-service", mock.Anything).Return(nil, fmt.Errorf("service not found"))
	sdMock.On("Find", "service-without-role", mock.Anything).Return(&serviceDiscovery.Service{
		Name:     "service-without",
		Provides: "role",
	}, nil)
	sdMock.On("Find", "service-with-role", mock.Anything).Return(&serviceDiscovery.Service{
		Name:     "service-with",
		Provides: "role",
	}, nil)
	sdMock.On("Find", "service-in-test.query.consul", mock.Anything).Return(&serviceDiscovery.Service{
		Name:     "service-in",
		Provides: "test",
	}, nil)

	sdMock.On("GetMDBInfo", "db.mdb-ro-role-rw.query.consul").Return(&serviceDiscovery.MDBInfo{
		Cluster: "role-rw",
		Db:      "db",
		Mode:    "ro",
	})
	sdMock.On("GetMDBInfo", "db.mdb-ro-role.query.consul").Return(&serviceDiscovery.MDBInfo{
		Cluster: "role",
		Db:      "db",
		Mode:    "ro",
	})
	sdMock.On("GetMDBInfo", "db.mdb-rw-role.query.consul").Return(&serviceDiscovery.MDBInfo{
		Cluster: "role",
		Db:      "db",
		Mode:    "rw",
	})
	sdMock.On("GetMDBInfo", "db.mdb-rw-test.query.consul").Return(&serviceDiscovery.MDBInfo{
		Cluster: "test",
		Db:      "db",
		Mode:    "rw",
	})
	sdMock.On("GetMDBInfo", "db.pg-ro-role.query.consul").Return(&serviceDiscovery.MDBInfo{
		Cluster: "role",
		Db:      "db",
		Mode:    "ro",
	})
	sdMock.On("GetMDBInfo", "db.pg-rw-role.query.consul").Return(&serviceDiscovery.MDBInfo{
		Cluster: "role",
		Db:      "db",
		Mode:    "rw",
	})
	sdMock.On("GetMDBInfo", "db.pg-rw-test.query.consul").Return(&serviceDiscovery.MDBInfo{
		Cluster: "test",
		Db:      "db",
		Mode:    "rw",
	})
	sdMock.On("GetMDBInfo", "db.pg-ro-role-rw.query.consul").Return(&serviceDiscovery.MDBInfo{
		Cluster: "role-rw",
		Db:      "db",
		Mode:    "ro",
	})

	return sdMock
}

func createIDMMock() idm.IIDMService {
	idmMock := &mocks.IIDMService{}

	idmMock.On("CheckRole", mock.Anything, "/service-without/role/").Return(fmt.Errorf("role not found"))
	idmMock.On("CheckRole", mock.Anything, "/service-with/role/").Return(nil)
	idmMock.On("CheckRole", mock.Anything, "/service-in/test/").Return(fmt.Errorf("role not found"))

	idmMock.On("CheckRole", mock.Anything, "/mysql/role/db/ro/").Return(nil)
	idmMock.On("CheckRole", mock.Anything, "/mysql/role/db/rw/").Return(nil)
	idmMock.On("CheckRole", mock.Anything, "/mysql/role-rw/db/ro/").Return(fmt.Errorf("role not found"))
	idmMock.On("CheckRole", mock.Anything, "/mysql/role-rw/db/rw/").Return(nil)
	idmMock.On("CheckRole", mock.Anything, "/mysql/test/db/rw/").Return(fmt.Errorf("role not found"))

	idmMock.On("CheckRole", mock.Anything, "/postgresql/role/db/ro/").Return(nil)
	idmMock.On("CheckRole", mock.Anything, "/postgresql/role/db/rw/").Return(nil)
	idmMock.On("CheckRole", mock.Anything, "/postgresql/role-rw/db/ro/").Return(fmt.Errorf("role not found"))
	idmMock.On("CheckRole", mock.Anything, "/postgresql/role-rw/db/rw/").Return(nil)
	idmMock.On("CheckRole", mock.Anything, "/postgresql/test/db/rw/").Return(fmt.Errorf("role not found"))

	return idmMock
}

func TestHandler_PortForward(t *testing.T) {
	tests := map[string]struct {
		Service          string
		Port             uint32
		Verdict          interface{}
		SkipValidateRole bool

		Choice ssh.Choice
		Reason string
	}{
		"not valid service": {
			Service: "not-valid-service",
			Verdict: nil,

			Choice: ssh.NO,
			Reason: "service not found",
		},
		"service without role": {
			Service: "service-without-role",
			Verdict: nil,

			Choice: ssh.NO,
			Reason: "internal error: role not found. Contact vertis duty",
		},
		"service with role": {
			Service: "service-with-role",
			Verdict: nil,

			Choice: ssh.TCP,
		},
		"mysql with RO role": {
			Service: "db.mdb-ro-role.query.consul",
			Port:    3306,

			Choice: ssh.MYSQL,
		},
		"mysql with RW role": {
			Service: "db.mdb-rw-role.query.consul",
			Port:    3306,

			Choice: ssh.MYSQL,
		},
		"mysql with RW role but request RO": {
			Service: "db.mdb-ro-role-rw.query.consul",
			Port:    3306,

			Choice: ssh.MYSQL,
		},
		"postgresql with RO role": {
			Service: "db.pg-ro-role.query.consul",
			Port:    6432,

			Choice: ssh.POSTGRESQL,
		},
		"postgresql with RW role": {
			Service: "db.pg-rw-role.query.consul",
			Port:    6432,

			Choice: ssh.POSTGRESQL,
		},
		"postgresql with RW role but request RO": {
			Service: "db.pg-ro-role-rw.query.consul",
			Port:    6432,

			Choice: ssh.POSTGRESQL,
		},
		"service without role in testing": {
			Service:          "service-in-test.query.consul",
			SkipValidateRole: true,

			Choice: ssh.TCP,
		},
		"mysql without role in testing": {
			Service:          "db.mdb-rw-test.query.consul",
			Port:             3306,
			SkipValidateRole: true,

			Choice: ssh.MYSQL,
		},
		"postgresql without role in testing": {
			Service:          "db.pg-rw-test.query.consul",
			Port:             6432,
			SkipValidateRole: true,

			Choice: ssh.POSTGRESQL,
		},
	}

	sdMock := createSDMock()
	idmMock := createIDMMock()
	staffMock := &mocks.IStaff{}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			h := New(sdMock, idmMock, staffMock, tc.SkipValidateRole, logrus.New("info"))
			ctx := createSshContextMock(tc.Verdict)
			verdict := h.PortForward(ctx, tc.Service, tc.Port)

			assert.Equal(t, tc.Choice, verdict.Choice)
			assert.Equal(t, tc.Reason, verdict.Reason)
		})
	}
}

func TestHandler_detectType(t *testing.T) {
	tests := map[string]struct {
		Service        string
		ExpectedChoice ssh.Choice
	}{
		"service": {
			Service:        "srv-test.query.consul",
			ExpectedChoice: ssh.TCP,
		},
		"service-tag": {
			Service:        "tag.srv-test.query.consul",
			ExpectedChoice: ssh.TCP,
		},
		"ip": {
			Service:        "::1",
			ExpectedChoice: ssh.TCP,
		},
		"mysql": {
			Service:        "db.mdb-ro-mdb1111111.query.consul",
			ExpectedChoice: ssh.MYSQL,
		},
		"postgresql": {
			Service:        "db.pg-ro-mdb1111111.query.consul",
			ExpectedChoice: ssh.POSTGRESQL,
		},
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			h := Handler{}
			choice := h.detectType(tc.Service)

			assert.Equal(t, tc.ExpectedChoice, choice)
		})
	}
}

func TestHandler_getMDBInfo(t *testing.T) {
	tests := map[string]struct {
		Host            string
		Prefix          string
		ExpectedDb      string
		ExpectedCluster string
		ExpectedMode    string
	}{
		"mysql-ro": {
			Host:            "database.mdb-ro-cluster.query.consul",
			Prefix:          "mdb",
			ExpectedDb:      "database",
			ExpectedCluster: "cluster",
			ExpectedMode:    "ro",
		},
		"mysql-rw": {
			Host:            "database.mdb-rw-cluster.query.consul",
			Prefix:          "mdb",
			ExpectedDb:      "database",
			ExpectedCluster: "cluster",
			ExpectedMode:    "rw",
		},
		"postgresql-ro": {
			Host:            "database.pg-ro-cluster.query.consul",
			Prefix:          "pg",
			ExpectedDb:      "database",
			ExpectedCluster: "cluster",
			ExpectedMode:    "ro",
		},
		"postgresql-rw": {
			Host:            "database.pg-rw-cluster.query.consul",
			Prefix:          "pg",
			ExpectedDb:      "database",
			ExpectedCluster: "cluster",
			ExpectedMode:    "rw",
		},
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			h := Handler{}
			db, cluster, mode := h.getMDBInfo(tc.Host, tc.Prefix)

			assert.Equal(t, tc.ExpectedDb, db)
			assert.Equal(t, tc.ExpectedCluster, cluster)
			assert.Equal(t, tc.ExpectedMode, mode)
		})
	}
}
