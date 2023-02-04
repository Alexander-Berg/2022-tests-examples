package app

import (
	"fmt"
	"github.com/YandexClassifieds/h2p/cmd/cli/errors"
	"github.com/YandexClassifieds/h2p/common/serviceDiscovery"
	"github.com/YandexClassifieds/h2p/test"
	"github.com/YandexClassifieds/h2p/test/mocks"
	"github.com/hashicorp/consul/api"
	"github.com/jarcoal/httpmock"
	"github.com/sirupsen/logrus"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"golang.org/x/crypto/ssh"
	"golang.org/x/crypto/ssh/agent"
	"os"
	"os/user"
	"strings"
	"testing"
)

func TestDetectMode(t *testing.T) {
	a := App{}

	tests := map[string]struct {
		Service string
		Extra   []string

		ExpectedKind   Kind
		ExpectedAction Action
	}{
		"serviceListen": {
			Service:        "service",
			Extra:          []string{},
			ExpectedKind:   Service,
			ExpectedAction: Listen,
		},
		"serviceQuery": {
			Service:        "service",
			Extra:          []string{"/metrics"},
			ExpectedKind:   Service,
			ExpectedAction: Query,
		},
		"mysql": {
			Service:        "mysql-mdb1234@acl",
			Extra:          []string{},
			ExpectedKind:   Mysql,
			ExpectedAction: Listen,
		},
		"mysqlQuery": {
			Service:        "mysql-mdb1234@acl",
			Extra:          []string{"select 1"},
			ExpectedKind:   Mysql,
			ExpectedAction: Query,
		},
		"postgresql": {
			Service:        "pg-mdb1234@acl",
			Extra:          []string{},
			ExpectedKind:   Postgresql,
			ExpectedAction: Listen,
		},
		"postgresqlQuery": {
			Service:        "pg-mdb1234@acl",
			Extra:          []string{"select 1"},
			ExpectedKind:   Postgresql,
			ExpectedAction: Query,
		},
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			a.s.service = tc.Service
			a.s.extra = tc.Extra
			err := a.detectMode()

			assert.NoError(t, err)
			assert.Equal(t, tc.ExpectedKind, a.s.kind)
			assert.Equal(t, tc.ExpectedAction, a.s.action)
		})
	}
}

func TestParseServiceTag(t *testing.T) {
	a := App{}

	tests := map[string]struct {
		Kind            Kind
		Action          Action
		Service         string
		ExpectedService string
		ExpectedTag     string
		Write           bool
	}{
		"serviceListen": {
			Kind:            Service,
			Action:          Listen,
			Service:         "service",
			ExpectedService: "service",
			ExpectedTag:     "",
		},
		"serviceListenWithTag": {
			Kind:            Service,
			Action:          Listen,
			Service:         "service@tag",
			ExpectedService: "service",
			ExpectedTag:     "tag",
		},
		"serviceQuery": {
			Kind:            Service,
			Action:          Query,
			Service:         "service",
			ExpectedService: "service",
			ExpectedTag:     "",
		},
		"serviceQueryWithTag": {
			Kind:            Service,
			Action:          Query,
			Service:         "service@tag",
			ExpectedService: "service",
			ExpectedTag:     "tag",
		},
		"mysqlListenMDBRO": {
			Kind:            Mysql,
			Action:          Listen,
			Service:         "mysql-mdb00000000000000000@acl",
			ExpectedService: "mdb-ro-mdb00000000000000000",
			ExpectedTag:     "acl",
		},
		"mysqlListenMDBRW": {
			Kind:            Mysql,
			Action:          Query,
			Service:         "mysql-mdb00000000000000000@acl",
			ExpectedService: "mdb-rw-mdb00000000000000000",
			ExpectedTag:     "acl",
			Write:           true,
		},
		"postgresqlListenMDBRO": {
			Kind:            Postgresql,
			Action:          Listen,
			Service:         "pg-mdb00000000000000000@acl",
			ExpectedService: "pg-ro-mdb00000000000000000",
			ExpectedTag:     "acl",
		},
		"postgresqlListenMDBRW": {
			Kind:            Postgresql,
			Action:          Query,
			Service:         "pg-mdb00000000000000000@acl",
			ExpectedService: "pg-rw-mdb00000000000000000",
			ExpectedTag:     "acl",
			Write:           true,
		},
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			a.s.service = tc.Service
			a.s.kind = tc.Kind
			a.s.action = tc.Action
			a.s.writeMode = tc.Write
			a.s.tag = ""

			a.parseServiceTag()
			assert.Equal(t, tc.ExpectedTag, a.s.tag)
			assert.Equal(t, tc.ExpectedService, a.s.service)
		})
	}
}

func createSDMock() *mocks.IServiceDiscovery {
	consulServiceCases := []struct {
		Service      string
		Tag          string
		QueryOptions *api.QueryOptions

		CatalogServices []*api.CatalogService
		Error           error
	}{
		{
			Service:      "service",
			QueryOptions: &api.QueryOptions{},
			CatalogServices: []*api.CatalogService{
				{
					ServiceAddress: "::1",
					ServicePort:    3306,
				},
			},
		},
		{
			Service:      "serviceTag",
			Tag:          "tag",
			QueryOptions: &api.QueryOptions{},
			CatalogServices: []*api.CatalogService{
				{
					ServiceAddress: "::2",
					ServicePort:    3306,
				},
			},
		},
		{
			Service: "serviceDC",
			QueryOptions: &api.QueryOptions{
				Datacenter: "sas",
			},
			CatalogServices: []*api.CatalogService{
				{
					ServiceAddress: "::3",
					ServicePort:    3306,
				},
			},
		},
		{
			Service: "serviceDC",
			QueryOptions: &api.QueryOptions{
				Datacenter: "myt",
			},
			CatalogServices: []*api.CatalogService{
				{
					ServiceAddress: "::4",
					ServicePort:    3306,
				},
			},
		},
		{
			Service: "serviceDCTag",
			Tag:     "tag",
			QueryOptions: &api.QueryOptions{
				Datacenter: "sas",
			},
			CatalogServices: []*api.CatalogService{
				{
					ServiceAddress: "::4",
					ServicePort:    3306,
				},
			},
		},
		{
			Service:         "unknown",
			QueryOptions:    &api.QueryOptions{},
			CatalogServices: []*api.CatalogService{},
		},
		{
			Service:      "mysql-ro-mdb1234",
			Tag:          "acl",
			QueryOptions: &api.QueryOptions{},
			CatalogServices: []*api.CatalogService{
				{
					ServiceAddress: "sas-1",
					ServicePort:    3306,
				},
			},
		},
		{
			Service: "mysql-ro-mdb1234",
			Tag:     "acl",
			QueryOptions: &api.QueryOptions{
				Datacenter: "sas",
			},
			CatalogServices: []*api.CatalogService{
				{
					ServiceAddress: "sas-1",
					ServicePort:    3306,
				},
			},
		},
		{
			Service:      "mdb-rw-mdb1234",
			Tag:          "acl",
			QueryOptions: &api.QueryOptions{},
			CatalogServices: []*api.CatalogService{
				{
					ServiceAddress: "sas-1",
					ServicePort:    3306,
				},
			},
		},
		{
			Service: "pg-ro-mdb1234",
			Tag:     "acl",
			QueryOptions: &api.QueryOptions{
				Datacenter: "sas",
			},
			CatalogServices: []*api.CatalogService{
				{
					ServiceAddress: "sas-1",
					ServicePort:    6432,
				},
			},
		},
		{
			Service:      "pg-rw-mdb1234",
			Tag:          "acl",
			QueryOptions: &api.QueryOptions{},
			CatalogServices: []*api.CatalogService{
				{
					ServiceAddress: "sas-1",
					ServicePort:    6432,
				},
			},
		},
		{
			Service: "one-dc-service",
			QueryOptions: &api.QueryOptions{
				Datacenter: "sas",
			},
			CatalogServices: []*api.CatalogService{},
		},
		{
			Service: "one-dc-service",
			QueryOptions: &api.QueryOptions{
				Datacenter: "myt",
			},
			CatalogServices: []*api.CatalogService{
				{
					ServiceAddress: "myt-1",
					ServicePort:    1234,
				},
			},
		},
		{
			Service:      "service-run",
			QueryOptions: &api.QueryOptions{},
			CatalogServices: []*api.CatalogService{
				{},
			},
		},
		{
			Service:      "service-run-wo-role",
			QueryOptions: &api.QueryOptions{},
			CatalogServices: []*api.CatalogService{
				{},
			},
		},
		{
			Service:      "mdb-ro-run",
			Tag:          "test",
			QueryOptions: &api.QueryOptions{},
			CatalogServices: []*api.CatalogService{
				{},
			},
		},
		{
			Service:      "mdb-ro-run",
			Tag:          "wo-role",
			QueryOptions: &api.QueryOptions{},
			CatalogServices: []*api.CatalogService{
				{},
			},
		},
		{
			Service:      "pg-ro-run",
			Tag:          "test",
			QueryOptions: &api.QueryOptions{},
			CatalogServices: []*api.CatalogService{
				{},
			},
		},
		{
			Service:      "pg-ro-run",
			Tag:          "wo-role",
			QueryOptions: &api.QueryOptions{},
			CatalogServices: []*api.CatalogService{
				{},
			},
		},
	}

	findCases := []struct {
		Service string

		Return *serviceDiscovery.Service
	}{
		{
			Service: "service-run.query.consul",

			Return: &serviceDiscovery.Service{
				Name:     "service",
				Provides: "run",
			},
		},
		{
			Service: "service-run-wo-role.query.consul",

			Return: &serviceDiscovery.Service{
				Name:     "service-run",
				Provides: "wo-role",
			},
		},
	}

	sdMock := &mocks.IServiceDiscovery{}
	for _, Case := range consulServiceCases {
		sdMock.On("ConsulService", Case.Service, Case.Tag, Case.QueryOptions).
			Return(Case.CatalogServices, nil, Case.Error)
	}

	for _, Case := range findCases {
		sdMock.On("Find", Case.Service, mock.Anything).
			Return(Case.Return, nil)
	}

	sdMock.On("GetMDBInfo", "wo-role.mdb-ro-run.query.consul").Return(&serviceDiscovery.MDBInfo{
		Cluster: "run",
		Db:      "wo-role",
		Mode:    "ro",
	})
	sdMock.On("GetMDBInfo", "test.mdb-ro-run.query.consul").Return(&serviceDiscovery.MDBInfo{
		Cluster: "run",
		Db:      "test",
		Mode:    "ro",
	})
	sdMock.On("GetMDBInfo", "wo-role.pg-ro-run.query.consul").Return(&serviceDiscovery.MDBInfo{
		Cluster: "run",
		Db:      "wo-role",
		Mode:    "ro",
	})
	sdMock.On("GetMDBInfo", "test.pg-ro-run.query.consul").Return(&serviceDiscovery.MDBInfo{
		Cluster: "run",
		Db:      "test",
		Mode:    "ro",
	})

	return sdMock
}

func createIdmMock() *mocks.IIDMService {
	idmMock := &mocks.IIDMService{}
	idmMock.On("SetToken", mock.Anything)

	idmMock.On("CheckRole", mock.Anything, "/service/run/").Return(nil)
	idmMock.On("CheckRole", mock.Anything, "/service-run/wo-role/").Return(fmt.Errorf("role not found"))
	idmMock.On("CheckRole", mock.Anything, "/mysql/run/test/ro/").Return(nil)
	idmMock.On("CheckRole", mock.Anything, "/mysql/run/wo-role/ro/").Return(fmt.Errorf("role not found"))
	idmMock.On("CheckRole", mock.Anything, "/postgresql/run/test/ro/").Return(nil)
	idmMock.On("CheckRole", mock.Anything, "/postgresql/run/wo-role/ro/").Return(fmt.Errorf("role not found"))

	return idmMock
}

func createSSHAgentMock() *mocks.SSHAgent {
	sshMock := &mocks.SSHAgent{}
	sshMock.On("List").Return([]*agent.Key{{}}, nil)
	sshMock.On("Sign", mock.Anything, mock.Anything).Return(&ssh.Signature{}, nil)

	return sshMock
}

func TestParseRemoteAddr(t *testing.T) {

	tests := map[string]struct {
		Service    string
		Tag        string
		Datacenter string
		Kind       Kind

		ExpectedError      error
		ExpectedRemoteHost string
		ExpectedRemotePort int
	}{
		"Service": {
			Service:            "service",
			ExpectedError:      nil,
			ExpectedRemoteHost: "service.query.consul",
			ExpectedRemotePort: 3306,
		},
		"ServiceWithTag": {
			Service:            "serviceTag",
			Tag:                "tag",
			ExpectedError:      nil,
			ExpectedRemoteHost: "tag.serviceTag.query.consul",
			ExpectedRemotePort: 3306,
		},
		"ServiceWithDCSas": {
			Service:            "serviceDC",
			Datacenter:         "sas",
			ExpectedError:      nil,
			ExpectedRemoteHost: "serviceDC.query.sas.consul",
			ExpectedRemotePort: 3306,
		},
		"ServiceWithDCSasWithTag": {
			Service:            "serviceDCTag",
			Tag:                "tag",
			Datacenter:         "sas",
			ExpectedError:      nil,
			ExpectedRemoteHost: "tag.serviceDCTag.query.sas.consul",
			ExpectedRemotePort: 3306,
		},
		"Unknown": {
			Service:            "unknown",
			ExpectedError:      errors.ServiceNotFoundInConsulError,
			ExpectedRemoteHost: "",
		},
		"MySQLDC": {
			Service:            "mysql-ro-mdb1234",
			Tag:                "acl",
			Datacenter:         "sas",
			ExpectedError:      nil,
			ExpectedRemoteHost: "acl.mysql-ro-mdb1234.query.sas.consul",
			ExpectedRemotePort: 3306,
			Kind:               Mysql,
		},
		"MySQL": {
			Service:            "mdb-rw-mdb1234",
			Tag:                "acl",
			ExpectedError:      nil,
			ExpectedRemoteHost: "acl.mdb-rw-mdb1234.query.consul",
			ExpectedRemotePort: 3306,
			Kind:               Mysql,
		},
		"PostgreSQLDC": {
			Service:            "pg-ro-mdb1234",
			Tag:                "acl",
			Datacenter:         "sas",
			ExpectedError:      nil,
			ExpectedRemoteHost: "acl.pg-ro-mdb1234.query.sas.consul",
			ExpectedRemotePort: 6432,
			Kind:               Postgresql,
		},
		"PostgreSQL": {
			Service:            "pg-rw-mdb1234",
			Tag:                "acl",
			ExpectedError:      nil,
			ExpectedRemoteHost: "acl.pg-rw-mdb1234.query.consul",
			ExpectedRemotePort: 6432,
			Kind:               Postgresql,
		},
	}

	sdMock := createSDMock()
	sdMock.On("Datacenters").Return(nil, fmt.Errorf("can't get datacenters"))

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			a := &App{}
			a.s.service = tc.Service
			a.s.tag = tc.Tag
			a.s.dc = tc.Datacenter
			a.sd = sdMock
			a.s.kind = tc.Kind

			err := a.parseRemoteAddr()
			assert.Equal(t, tc.ExpectedError, err)
			assert.Equal(t, tc.ExpectedRemoteHost, a.s.remoteHost)
			assert.Equal(t, tc.ExpectedRemotePort, a.s.remotePort)
		})
	}
}

func TestParseRemoteAddrOneDC(t *testing.T) {
	sdMock := createSDMock()
	sdMock.On("Datacenters").Return([]string{"sas", "myt"}, nil)

	a := &App{}
	a.s.service = "one-dc-service"
	a.sd = sdMock

	err := a.parseRemoteAddr()
	assert.NoError(t, err)
	assert.Equal(t, "one-dc-service.query.consul", a.s.remoteHost)
	assert.Equal(t, 1234, a.s.remotePort)
}

func TestGetUsername(t *testing.T) {
	currentUser, err := user.Current()
	assert.NoError(t, err)

	a := App{
		log: logrus.New(),
	}
	a.Config()
	err = a.getUsername()
	assert.NoError(t, err)
	assert.Equal(t, currentUser.Username, a.s.user)

	err = os.Setenv("H2P_USER", "overridden-user")
	assert.NoError(t, err)
	a.Config()

	a = App{}
	err = a.getUsername()
	assert.NoError(t, err)
	assert.Equal(t, "overridden-user", a.s.user)
}

func TestApp_run(t *testing.T) {
	cases := map[string]struct {
		Service      string
		Tag          string
		UserChoice   string
		TestMode     bool
		SkipValidate bool
		Extra        []string

		Error error
	}{
		"valid service in prod": {
			Service:    "service-run",
			UserChoice: "y",
		},
		"service in prod canceled": {
			Service:    "service-run",
			UserChoice: "n",

			Error: errors.ProdConfirmError,
		},
		"service in prod skip validate": {
			Service:      "service-run",
			UserChoice:   "n",
			SkipValidate: true,
		},
		"service in test canceled": {
			Service:    "service-run",
			UserChoice: "n",
			TestMode:   true,
		},
		"service without role": {
			Service:    "service-run-wo-role",
			UserChoice: "y",

			Error: fmt.Errorf("role not found"),
		},
		"service without role skip validate": {
			Service:      "service-run-wo-role",
			UserChoice:   "y",
			SkipValidate: true,
		},
		"curl in prod": {
			Service:    "service-run",
			UserChoice: "n",
			Extra:      []string{"/test", "-m 1"},
		},
		"curl without role": {
			Service:    "service-run-wo-role",
			UserChoice: "y",

			Error: fmt.Errorf("role not found"),
		},
		"curl without role skip validate": {
			Service:      "service-run-wo-role",
			UserChoice:   "y",
			SkipValidate: true,
		},
		"mysql query in prod": {
			Service:    "mysql-run@test",
			UserChoice: "n",
			Extra:      []string{"SELECT 1"},
		},
		"mysql in prod canceled": {
			Service:    "mysql-run@test",
			UserChoice: "n",

			Error: errors.ProdConfirmError,
		},
		"mysql in prod": {
			Service:    "mysql-run@test",
			UserChoice: "y",
		},
		"mysql in prod without role": {
			Service:    "mysql-run@wo-role",
			UserChoice: "y",

			Error: fmt.Errorf("role not found"),
		},
		"mysql query in prod without role": {
			Service:    "mysql-run@wo-role",
			UserChoice: "y",
			Extra:      []string{"SELECT 1"},

			Error: fmt.Errorf("role not found"),
		},
		"mysql in prod without role skip validate": {
			Service:      "mysql-run@wo-role",
			UserChoice:   "y",
			SkipValidate: true,
		},
		"mysql query in prod without role skip validate": {
			Service:      "mysql-run@wo-role",
			UserChoice:   "y",
			Extra:        []string{"SELECT 1"},
			SkipValidate: true,
		},
		"postgresql query in prod": {
			Service:    "pg-run@test",
			UserChoice: "n",
			Extra:      []string{"SELECT 1"},
		},
		"postgresql in prod canceled": {
			Service:    "pg-run@test",
			UserChoice: "n",

			Error: errors.ProdConfirmError,
		},
		"postgresql in prod": {
			Service:    "pg-run@test",
			UserChoice: "y",
		},
		"postgresql in prod without role": {
			Service:    "pg-run@wo-role",
			UserChoice: "y",

			Error: fmt.Errorf("role not found"),
		},
		"postgresql query in prod without role": {
			Service:    "pg-run@wo-role",
			UserChoice: "y",
			Extra:      []string{"SELECT 1"},

			Error: fmt.Errorf("role not found"),
		},
		"postgresql in prod without role skip validate": {
			Service:      "pg-run@wo-role",
			UserChoice:   "y",
			SkipValidate: true,
		},
		"postgresql query in prod without role skip validate": {
			Service:      "pg-run@wo-role",
			UserChoice:   "y",
			Extra:        []string{"SELECT 1"},
			SkipValidate: true,
		},
	}

	httpmock.Activate()
	defer httpmock.DeactivateAndReset()
	viper.Set("oauth_token_url", "http://127.0.0.1/token")

	httpmock.RegisterResponder("POST", "http://127.0.0.1/token",
		httpmock.NewStringResponder(200, "{\"access_token\":\"token\",\"token_type\":\"bearer\"}"))

	sdMock := createSDMock()
	sdMock.On("Datacenters").Return([]string{""}, nil)

	a := App{
		log:      logrus.New(),
		sd:       sdMock,
		idm:      createIdmMock(),
		sshAgent: createSSHAgentMock(),
	}

	test.InitConfig(t)

	for name, tc := range cases {
		t.Run(name, func(t *testing.T) {
			a.s.service = tc.Service
			a.reader = strings.NewReader(fmt.Sprintf("%s\n", tc.UserChoice))
			a.s.skipValidate = tc.SkipValidate
			a.s.testMode = tc.TestMode
			a.s.extra = tc.Extra

			err := a.run(nil, nil)
			assert.Equal(t, tc.Error, err)
		})
	}
}
