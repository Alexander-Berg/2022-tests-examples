package validator

import (
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler"
	"github.com/YandexClassifieds/shiva/common/config"
	"github.com/YandexClassifieds/shiva/common/user_error"
	smProto "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pkg/conductor"
	"github.com/YandexClassifieds/shiva/pkg/feature_flags"
	"github.com/YandexClassifieds/shiva/pkg/mdb"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/mock/mq"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/go-yaml/yaml"
	nomadAPI "github.com/hashicorp/nomad/api"
	"github.com/stretchr/testify/assert"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

func TestSuccessValidate(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service := runUpService(t)
	errors, _ := service.Validate(sMap(), []*smProto.ServiceMap{})
	test.AssertUserErrors(t, user_error.NewUserErrors(), errors)
}

func TestValidateName(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service := runUpService(t)
	tests := map[string]*user_error.UserError{
		"":                                  ErrEmptyName,
		"servicenameservicenameservicename": ErrLongName,
		"serviceName":                       ErrCaseName,
		"SERVICE-NAME":                      ErrCaseName,
		"Service-name":                      ErrCaseName,
		"service_name":                      ErrNotAllowSymbolName,
		"service name":                      ErrNotAllowSymbolName,
		"service*name":                      ErrNotAllowSymbolName,
		"service//name":                     ErrNotAllowSymbolName,
		"service\name":                      ErrNotAllowSymbolName,
		"service(name)":                     ErrNotAllowSymbolName,
		"service[name]":                     ErrNotAllowSymbolName,
		"имясервиса":                        ErrNotAllowSymbolName,
	}

	for v, expectedErr := range tests {
		t.Run(v, func(t *testing.T) {
			sMap := sMap()
			sMap.Name = v
			errs, _ := service.Validate(sMap, nil)
			test.AssertUserErrors(t, user_error.NewUserErrors(expectedErr), errs)
		})
	}
}

func TestValidateOwners_ErrOneUserOwner(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	serviceMap := sMap()
	serviceMap.Owners = []string{"https://staff.yandex-team.ru/spooner"}
	errs, _ := runUpService(t).Validate(serviceMap, nil)
	test.AssertUserErrors(t, user_error.NewUserErrors(ErrOneUserOwner), errs)
}

func TestSuccessValidate_DeprecatedOwnerField(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	serviceMap := sMap()
	serviceMap.Owner = "https://staff.yandex-team.ru/spooner"
	errs, warns := runUpService(t).Validate(serviceMap, nil)
	test.AssertUserErrors(t, user_error.NewUserErrors(ErrDeprecatedOwnerField), errs)
	test.AssertUserErrors(t, user_error.NewUserErrors(), warns)
}

func TestValidateOwner_ErrEmptyOwners(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	serviceMap := sMap()
	serviceMap.Owners = []string{}
	errs, _ := runUpService(t).Validate(serviceMap, nil)
	test.AssertUserErrors(t, user_error.NewUserErrors(ErrEmptyOwners), errs)
}

func TestValidateOwners_ErrNotStaffLink(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service := runUpService(t)
	tests := []string{
		"http://staff.yandex-team.ru/departments/yandex_mnt_fin_vertical/",
		"owner",
		"departments/yandex_mnt_fin_vertical/",
		"yandex_mnt_fin_vertical",
		"https://staff.yandex-team.ru",
	}

	for _, owner := range tests {
		t.Run(owner, func(t *testing.T) {
			sMap := sMap()
			sMap.Owners = []string{owner}
			errs, _ := service.Validate(sMap, nil)
			test.AssertUserErrors(t, user_error.NewUserErrors(ErrNotStaffLink(owner)), errs)
		})
	}
}

func TestValidateOwners_Existance(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service := runUpService(t)

	type testCase struct {
		Owners         []string
		ExpectedErrors []*user_error.UserError
	}
	tests := map[string]testCase{
		"validUsers": {
			Owners: []string{
				"https://staff.yandex-team.ru/danevge",
				"https://staff.yandex-team.ru/spooner",
			},
			ExpectedErrors: nil,
		},
		"validUsersWithSlashes": {
			Owners: []string{
				"https://staff.yandex-team.ru/danevge/",
				"https://staff.yandex-team.ru/spooner/",
			},
			ExpectedErrors: nil,
		},
		"validGroup": {
			Owners: []string{
				"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt",
			},
			ExpectedErrors: nil,
		},
		"validGroupWithSlash": {
			Owners: []string{
				"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/",
			},
			ExpectedErrors: nil,
		},
		"duplicateOwners": {
			Owners: []string{
				"https://staff.yandex-team.ru/spooner",
				"https://staff.yandex-team.ru/spooner/",
			},
			ExpectedErrors: []*user_error.UserError{ErrDuplicateOwners, ErrOneUserOwner},
		},
		"empty": {
			Owners: []string{
				"https://staff.yandex-team.ru/",
				"https://staff.yandex-team.ru/departments/",
			},
			ExpectedErrors: []*user_error.UserError{ErrEmptyOwnerUrl, ErrEmptyOwnerUrl, ErrOneUserOwner},
		},
		"invalidUsers": {
			Owners: []string{
				"https://staff.yandex-team.ru/notvaliduserlogin",
				"https://staff.yandex-team.ru/notvaliduserlogin2",
			},
			ExpectedErrors: []*user_error.UserError{
				ErrNotValidStaffUser("notvaliduserlogin"),
				ErrNotValidStaffUser("notvaliduserlogin2"),
				ErrOneUserOwner,
			},
		},
		"invalidGroup": {
			Owners: []string{
				"https://staff.yandex-team.ru/departments/yandex_not_exists_department_1337/",
			},
			ExpectedErrors: []*user_error.UserError{
				ErrNotValidStaffGroup("yandex_not_exists_department_1337"),
				ErrOneUserOwner,
			},
		},
		"validWithInvalid": {
			Owners: []string{
				"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt",
				"https://staff.yandex-team.ru/departments/yandex_not_exists_department_1337/",
			},
			ExpectedErrors: []*user_error.UserError{
				ErrNotValidStaffGroup("yandex_not_exists_department_1337"),
			},
		},
	}

	for name, c := range tests {
		t.Run(name, func(t *testing.T) {
			sMap := sMap()
			sMap.Owners = c.Owners
			errs, _ := service.Validate(sMap, nil)
			test.AssertUserErrors(t, user_error.NewUserErrors(c.ExpectedErrors...), errs)
		})
	}
}

func TestValidateDescription(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service := runUpService(t)
	tests := map[string]*user_error.UserError{
		"":                        ErrEmptyDescription,
		"123":                     nil,
		"No":                      nil,
		"yandex_mnt_fin_vertical": nil,
		"long string long string long string long string long string long string long string long string": nil,
	}

	for v, expectedErr := range tests {
		t.Run(v, func(t *testing.T) {
			sMap := sMap()
			sMap.Description = v
			errs, _ := service.Validate(sMap, nil)
			test.AssertUserErrors(t, user_error.NewUserErrors(expectedErr), errs)
		})
	}
}

func TestValidateSrc(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service := runUpService(t)
	tests := map[string]*user_error.UserError{
		"":                           ErrEmptySrc,
		"123":                        ErrNotAllowedVCSLink,
		"services":                   ErrNotAllowedVCSLink,
		"YandexClassifieds/services": ErrNotAllowedVCSLink,
		"http://github.com/YandexClassifieds/services": ErrNotAllowedVCSLink,
	}

	for v, expectedErr := range tests {
		t.Run(v, func(t *testing.T) {
			sMap := sMap()
			sMap.Src = v
			errs, _ := service.Validate(sMap, nil)
			test.AssertUserErrors(t, user_error.NewUserErrors(expectedErr), errs)
		})
	}
}

func TestNotValidateSrc(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service := runUpService(t)

	tests := map[string]struct {
		Type smProto.ServiceType
		Map  *smProto.ServiceMap
	}{
		"External": {
			smProto.ServiceType_external,
			sMap(),
		},
		"MySQL": {
			smProto.ServiceType_mysql,
			sMapMySQL(),
		},
		"MDB MySQL": {
			smProto.ServiceType_mdb_mysql,
			sMapMDBMySQL(),
		},
		"MDB PostgreSQL": {
			smProto.ServiceType_mdb_postgresql,
			sMapMDBPostgreSQL(),
		},
		"Kafka": {
			smProto.ServiceType_kafka,
			sMapKafka(),
		},
	}

	for name, st := range tests {
		t.Run(name, func(t *testing.T) {
			sMap := st.Map
			sMap.Type = st.Type
			sMap.Src = ""

			service.mdb = getMdbMock(sMap, nil)

			errs, _ := service.Validate(sMap, nil)
			test.AssertUserErrors(t, user_error.NewUserErrors(), errs)
		})
	}
}

func TestValidateProvides(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service := runUpService(t)
	type testCase struct {
		err *user_error.UserError
		p   *smProto.ServiceProvides
	}

	tests := map[string]testCase{
		"ErrEmptyProvideName": {
			err: ErrEmptyProvideName,
			p: &smProto.ServiceProvides{
				Name:        "",
				Description: "http api",
				Protocol:    smProto.ServiceProvides_http,
				Port:        82,
			},
		},
		"ErrLongProvideName": {
			err: ErrLongProvideName,
			p: &smProto.ServiceProvides{
				Name:        "aaaaaaaaaaaaaaaaaaaaaaaaaaaa",
				Description: "http api",
				Protocol:    smProto.ServiceProvides_http,
				Port:        82,
			},
		},
		"ErrCaseProvideName": {
			err: ErrCaseProvideName,
			p: &smProto.ServiceProvides{
				Name:        "API",
				Description: "http api",
				Protocol:    smProto.ServiceProvides_http,
				Port:        82,
			},
		},
		"ErrNotAllowSymbolProvideName": {
			err: ErrNotAllowSymbolProvideName,
			p: &smProto.ServiceProvides{
				Name:        "http_api",
				Description: "http api",
				Protocol:    smProto.ServiceProvides_http,
				Port:        82,
			},
		},
		"ErrReusedMetricPort": {
			err: ErrReusedMetricPort,
			p: &smProto.ServiceProvides{
				Name:        "http-api",
				Description: "http api",
				Protocol:    smProto.ServiceProvides_http,
				Port:        scheduler.MonitoringPort,
			},
		},
		"ErrUnknownProvideProtocol": {
			err: ErrUnknownProvideProtocol,
			p: &smProto.ServiceProvides{
				Name:        "http-api",
				Description: "http api",
				Protocol:    smProto.ServiceProvides_unknown,
				Port:        82,
			},
		},
		"ErrEmptyProvideDescription": {
			err: ErrEmptyProvideDescription,
			p: &smProto.ServiceProvides{
				Name:        "http-api",
				Description: "",
				Protocol:    smProto.ServiceProvides_http,
				Port:        82,
			},
		},
		"ErrDoubleProvidesName": {
			err: ErrDoubleProvidesName,
			p: &smProto.ServiceProvides{
				Name:        "http",
				Description: "http api",
				Protocol:    smProto.ServiceProvides_http,
				Port:        82,
			},
		},
		"ErrDoubleProvidesPort": {
			err: ErrDoubleProvidesPort,
			p: &smProto.ServiceProvides{
				Name:        "http-api",
				Description: "http api",
				Protocol:    smProto.ServiceProvides_http,
				Port:        80,
			},
		},
		"ErrRestrictedProvidesName": {
			err: ErrRestrictedProvideName("monitoring"),
			p: &smProto.ServiceProvides{
				Name:        "monitoring",
				Description: "http api",
				Protocol:    smProto.ServiceProvides_http,
				Port:        82,
			},
		},
		"ErrEmptyProvidePort_http": {
			err: ErrEmptyProvidePort,
			p: &smProto.ServiceProvides{
				Name:        "http-api",
				Description: "http api",
				Protocol:    smProto.ServiceProvides_http,
			},
		},
		"ErrEmptyProvidePort_grpc": {
			err: ErrEmptyProvidePort,
			p: &smProto.ServiceProvides{
				Name:        "http-api",
				Description: "http api",
				Protocol:    smProto.ServiceProvides_grpc,
			},
		},
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			sMap := sMap()
			sMap.Provides = append(sMap.Provides, tc.p)
			errs, _ := service.Validate(sMap, nil)
			test.AssertUserErrors(t, user_error.NewUserErrors(tc.err), errs)
		})
	}
}

func TestValidateProvidesMDB(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service := runUpService(t)
	type testCase struct {
		err        *user_error.UserError
		p          *smProto.ServiceProvides
		Type       smProto.ServiceType
		mdbCluster *smProto.MDBCluster
	}

	tests := map[string]testCase{
		"ValidProvidesKafka": {
			err: nil,
			p: &smProto.ServiceProvides{
				Name:        "kafka",
				Description: "kafka",
				Protocol:    smProto.ServiceProvides_kafka,
			},
			Type: smProto.ServiceType_kafka,
			mdbCluster: &smProto.MDBCluster{
				ProdId: "mdb4u82hc7bsaq7cq1tc",
				TestId: "mdb9adu18n492urt7kuk",
			},
		},
		"ValidDoubleProvidesPortMySQL": {
			err: nil,
			p: &smProto.ServiceProvides{
				Name:        "database",
				Description: "database",
				Protocol:    smProto.ServiceProvides_tcp,
				Port:        80,
			},
			Type: smProto.ServiceType_mysql,
		},
		"ValidDoubleProvidesPortMDBMySQL": {
			err: nil,
			p: &smProto.ServiceProvides{
				Name:        "database",
				Description: "database",
				Protocol:    smProto.ServiceProvides_tcp,
				Port:        80,
			},
			Type: smProto.ServiceType_mdb_mysql,
			mdbCluster: &smProto.MDBCluster{
				ProdId: "mdbmef09944k5geatbkb",
				TestId: "mdb50ifhjj66soaqgk69",
			},
		},
		"ValidDoubleProvidesPortMDBPostgreSQL": {
			err: nil,
			p: &smProto.ServiceProvides{
				Name:        "database",
				Description: "database",
				Protocol:    smProto.ServiceProvides_tcp,
				Port:        80,
			},
			Type: smProto.ServiceType_mdb_postgresql,
			mdbCluster: &smProto.MDBCluster{
				ProdId: "mdbmef09944k5geatbkb",
				TestId: "mdb50ifhjj66soaqgk69",
			},
		},
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			sMap := sMap()
			sMap.Name = "vertis-h2p"
			switch tc.Type {
			case smProto.ServiceType_mdb_mysql, smProto.ServiceType_mysql:
				sMap.Path = "maps/mysql/vertis-h2p.yml"
			case smProto.ServiceType_mdb_postgresql:
				sMap.Path = "maps/postgresql/vertis-test.yml"
			case smProto.ServiceType_kafka:
				sMap.Path = "maps/kafka/shared-01.yml"
			}

			sMap.Provides = append(sMap.Provides, tc.p)
			sMap.Type = tc.Type
			sMap.MdbCluster = tc.mdbCluster

			service.mdb = getMdbMock(sMap, nil)

			errs, _ := service.Validate(sMap, nil)
			test.AssertUserErrors(t, user_error.NewUserErrors(tc.err), errs)
		})
	}
}

func TestValidateDepends(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service := runUpService(t)
	type testCase struct {
		err   *user_error.UserError
		warn  *user_error.UserError
		d     *smProto.ServiceDependency
		flags []*feature_flags.FeatureFlag
	}

	tests := map[string]testCase{
		"ErrMapsNotFound": {
			err: ErrMapsNotFound("maps/service-name-x.yml"),
			d: &smProto.ServiceDependency{
				Service:       "service-name-x",
				InterfaceName: "main",
			},
		},
		"FindNewMaps": {
			d: &smProto.ServiceDependency{
				Service:       "service-name-3",
				InterfaceName: "main",
			},
		},
		"ErrProvidesNotFound": {
			err: ErrProvidesNotFound("service-name", "service-name-3", "no"),
			d: &smProto.ServiceDependency{
				Service:       "service-name-3",
				InterfaceName: "no",
			},
		},
		"WarnDeprecatedServiceNameFieldFeatureFlag": {
			warn: ErrProvidesDeprecatedServiceNameField,
			d: &smProto.ServiceDependency{
				ServiceName:   "service-name-3",
				InterfaceName: "main",
			},
			flags: []*feature_flags.FeatureFlag{
				{
					Flag:  feature_flags.DeprecatedServiceMapDependencyServiceNameField.String(),
					Value: true,
				},
			},
		},
		"DeprecatedServiceNameFieldNoFeatureFlag": {
			d: &smProto.ServiceDependency{
				ServiceName:   "service-name-3",
				InterfaceName: "main",
			},
		},
		"ErrProvidesUsedServiceServiceName": {
			err: ErrProvidesUsedServiceServiceName,
			d: &smProto.ServiceDependency{
				ServiceName:   "service-name-3",
				Service:       "service-name-3",
				InterfaceName: "main",
			},
		},
		"ValidServiceDependency": {
			d: &smProto.ServiceDependency{
				Service:       "service-name-3",
				InterfaceName: "main",
			},
		},
		"ErrMySQLDependencyNotFound": {
			err: ErrMapsNotFound("maps/mysql/mysql-cluster.yml"),
			d: &smProto.ServiceDependency{
				Service:       "mysql/mysql-cluster",
				InterfaceName: "main",
			},
		},
		"ValidDependsWithDifferentInt": {
			d: &smProto.ServiceDependency{
				Service:       "service-name-2",
				InterfaceName: "main2",
			},
		},
		"ErrDuplicateDepends": {
			err: ErrDuplicateDepends,
			d: &smProto.ServiceDependency{
				Service:       "service-name-2",
				InterfaceName: "main",
			},
		},
	}

	for name, d := range tests {
		t.Run(name, func(t *testing.T) {
			flags, err := service.flags.GetAllActual()
			assert.NoError(t, err)
			err = service.flags.ClearFlags(flags)
			assert.NoError(t, err)
			err = service.flags.SetFlags(d.flags)
			assert.NoError(t, err)

			sMap := sMap()
			sMap.DependsOn = append(sMap.DependsOn, d.d)
			errs, warns := service.Validate(sMap, []*smProto.ServiceMap{sMap3()})
			test.AssertUserErrors(t, user_error.NewUserErrors(d.warn), warns)
			test.AssertUserErrors(t, user_error.NewUserErrors(d.err), errs)
		})
	}
}

func TestValidateTrackerQueue(t *testing.T) {
	test.RunUp(t)
	service := runUpService(t)
	t.Run("exists", func(t *testing.T) {
		svcMap := sMap()
		svcMap.Startrek = "VOID"
		errs, _ := service.Validate(svcMap, nil)
		assert.Len(t, errs.Get(), 0)
	})
	t.Run("not_found", func(t *testing.T) {
		svcMap := sMap()
		svcMap.Startrek = "VOID34535345345"
		errs, _ := service.Validate(svcMap, nil)
		assert.Len(t, errs.Get(), 1)
		assert.Equal(t, user_error.NewUserErrors(errTrackerQueueNotFound(svcMap.Name, svcMap.Startrek)), errs)
	})
	t.Run("forbidden", func(t *testing.T) {
		svcMap := sMap()
		svcMap.Startrek = "TEST"
		errs, _ := service.Validate(svcMap, nil)
		assert.Len(t, errs.Get(), 1)
		assert.Equal(t, user_error.NewUserErrors(errTrackerQueueAccessDenied(svcMap.Name, svcMap.Startrek)), errs)
	})
}

func TestValidateType(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service := runUpService(t)

	type testCase struct {
		Name          string
		Type          smProto.ServiceType
		IsExternal    bool
		Path          string
		MDBCluster    *smProto.MDBCluster
		MDBMySQL      *smProto.MDBMySQL
		Provides      []*smProto.ServiceProvides
		ExpectedWarn  []*user_error.UserError
		ExpectedError []*user_error.UserError
		MdbError      error
	}

	tests := map[string]testCase{
		"ValidService": {
			Name: "service",
			Path: "maps/service.yml",
		},
		"InvalidPathService": {
			Name:          "service",
			Path:          "maps/services/service.yml",
			ExpectedError: []*user_error.UserError{ErrServicePathInvalid},
		},
		"ValidExternal": {
			Name: "youtube",
			Type: smProto.ServiceType_external,
			Path: "maps/youtube.yml",
		},
		"ValidMySQL": {
			Name:         "main",
			Type:         smProto.ServiceType_mysql,
			Path:         "maps/mysql/main.yml",
			ExpectedWarn: []*user_error.UserError{ErrMySQLTypeDeprecated},
		},
		"InvalidMDBMySQL": {
			Name:          "some-invalid-database",
			Type:          smProto.ServiceType_mdb_mysql,
			ExpectedError: []*user_error.UserError{ErrMDBAdditionalInfoNotExists},
			Path:          "maps/mysql/some-invalid-database.yml",
		},
		"ValidMDBMySQL": {
			Name: "vertis-h2p",
			Type: smProto.ServiceType_mdb_mysql,
			MDBCluster: &smProto.MDBCluster{
				ProdId: "mdbmef09944k5geatbkb",
				TestId: "mdb50ifhjj66soaqgk69",
			},
			Path: "maps/mysql/vertis-h2p.yml",
		},
		"InvalidMDBCluster_empty": {
			Name: "vertis-h2p",
			Type: smProto.ServiceType_mdb_mysql,
			MDBCluster: &smProto.MDBCluster{
				TestId: "",
			},
			ExpectedError: []*user_error.UserError{ErrMDBClusterIsEmpty("test")},
			Path:          "maps/mysql/vertis-h2p.yml",
		},
		"InvalidMDBCluster_invalid_id": {
			Name: "vertis-h2p",
			Type: smProto.ServiceType_mdb_mysql,
			MDBCluster: &smProto.MDBCluster{
				TestId: "mdb00000000000000000",
				ProdId: "mdb11111111111111111",
			},
			ExpectedError: []*user_error.UserError{ErrMDBClusterNotExists("mdb00000000000000000"), ErrMDBClusterNotExists("mdb11111111111111111")},
			Path:          "maps/mysql/vertis-h2p.yml",
			MdbError:      status.Errorf(codes.PermissionDenied, "error"),
		},
		"ValidMDBPostgresql": {
			Name: "vertis-test",
			Type: smProto.ServiceType_mdb_postgresql,
			MDBCluster: &smProto.MDBCluster{
				ProdId: "mdbvgaesr3ntkf2k80rq",
				TestId: "mdbfu6meb1bt71p6eetr",
			},
			Path: "maps/postgresql/vertis-test.yml",
		},
		"InvalidMDBClusterPostgreSQL_empty": {
			Name: "vertis-test",
			Type: smProto.ServiceType_mdb_postgresql,
			MDBCluster: &smProto.MDBCluster{
				TestId: "",
			},
			ExpectedError: []*user_error.UserError{ErrMDBClusterIsEmpty("test")},
			Path:          "maps/postgresql/vertis-test.yml",
		},
		"InvalidMDBClusterPostgreSQL_invalid_id": {
			Name: "vertis-test",
			Type: smProto.ServiceType_mdb_postgresql,
			MDBCluster: &smProto.MDBCluster{
				TestId: "mdb00000000000000000",
				ProdId: "mdb11111111111111111",
			},
			ExpectedError: []*user_error.UserError{ErrMDBClusterNotExists("mdb00000000000000000"), ErrMDBClusterNotExists("mdb11111111111111111")},
			Path:          "maps/postgresql/vertis-test.yml",
			MdbError:      status.Errorf(codes.PermissionDenied, "error"),
		},
		"ValidClusterKafka": {
			Name: "kafka",
			Type: smProto.ServiceType_kafka,
			MDBCluster: &smProto.MDBCluster{
				ProdId: "mdb4u82hc7bsaq7cq1tc",
				TestId: "mdb9adu18n492urt7kuk",
			},
			Path: "maps/kafka/shared-01.yml",
		},
		"InvalidClusterKafka_empty": {
			Name: "kafka",
			Type: smProto.ServiceType_kafka,
			MDBCluster: &smProto.MDBCluster{
				TestId: "",
			},
			ExpectedError: []*user_error.UserError{ErrMDBClusterIsEmpty("test")},
			Path:          "maps/kafka/shared-01.yml",
		},
		"InvalidClusterKafka_invalid_id": {
			Name: "kafka",
			Type: smProto.ServiceType_kafka,
			MDBCluster: &smProto.MDBCluster{
				TestId: "mdb00000000000000000",
				ProdId: "mdb11111111111111111",
			},
			ExpectedError: []*user_error.UserError{ErrMDBClusterNotExists("mdb00000000000000000"), ErrMDBClusterNotExists("mdb11111111111111111")},
			Path:          "maps/kafka/shared-01.yml",
			MdbError:      status.Errorf(codes.PermissionDenied, "error"),
		},
		"InvalidMDBClusterField": {
			Name: "service",
			Type: smProto.ServiceType_external,
			MDBCluster: &smProto.MDBCluster{
				TestId: "",
				ProdId: "",
			},
			ExpectedError: []*user_error.UserError{ErrMDBClusterIsNotAllowed},
			Path:          "maps/service.yml",
		},
		"DeprecatedMDBMySQLField": {
			Name: "vertis-h2p",
			Type: smProto.ServiceType_mdb_mysql,
			MDBMySQL: &smProto.MDBMySQL{
				ProdId: "mdbmef09944k5geatbkb",
				TestId: "mdb50ifhjj66soaqgk69",
			},
			MDBCluster: &smProto.MDBCluster{
				ProdId: "mdbmef09944k5geatbkb",
				TestId: "mdb50ifhjj66soaqgk69",
			},
			Path:         "maps/mysql/vertis-h2p.yml",
			ExpectedWarn: []*user_error.UserError{ErrDeprecatedMDBMySQLField},
		},
		"DeprecatedExternalField": {
			Name:         "external",
			IsExternal:   true,
			ExpectedWarn: []*user_error.UserError{ErrDeprecatedExternalField},
			Path:         "maps/external.yml",
		},
		"ValidConductor": {
			// https://c.yandex-team.ru/packages/59613
			Name: "shiva-for-test",
			Type: smProto.ServiceType_conductor,
			Path: "maps/shiva-for-test.yml",
		},
		"InvalidConductor": {
			Name:          "some-test-service",
			Type:          smProto.ServiceType_conductor,
			ExpectedError: []*user_error.UserError{conductor.ErrConductorServiceNotExists},
			Path:          "maps/some-test-service.yml",
		},
		"DeletedConductor": {
			Name:          "yandex-verba2-main",
			Type:          smProto.ServiceType_conductor,
			ExpectedError: []*user_error.UserError{conductor.ErrConductorServiceWasDeleted},
			Path:          "maps/yandex-verba2-main.yml",
		},
		"ValidJenkins": {
			Name: "zookeeper-ui",
			Type: smProto.ServiceType_jenkins,
			Path: "maps/zookeeper-ui.yml",
		},
		"InvalidJenkins": {
			Name:          "some-test-service",
			Type:          smProto.ServiceType_jenkins,
			ExpectedError: []*user_error.UserError{ErrJenkinsServiceNotExists},
			Path:          "maps/some-test-service.yml",
		},
		"InvalidServicePath": {
			Name:          "zookeeper-ui",
			Type:          smProto.ServiceType_service,
			Path:          "maps/mysql/zookeeper-ui.yml",
			ExpectedError: []*user_error.UserError{ErrServicePathInvalid},
		},
		"InvalidMySQLPath": {
			Name:          "main",
			Type:          smProto.ServiceType_mysql,
			Path:          "maps/main.yml",
			ExpectedError: []*user_error.UserError{ErrMySQLPathInvalid},
			ExpectedWarn:  []*user_error.UserError{ErrMySQLTypeDeprecated},
		},
		"InvalidMySQLPathWithSubdirectory": {
			Name:          "main",
			Type:          smProto.ServiceType_mysql,
			Path:          "maps/mysql/subdirectory/main.yml",
			ExpectedError: []*user_error.UserError{ErrMySQLPathInvalid},
			ExpectedWarn:  []*user_error.UserError{ErrMySQLTypeDeprecated},
		},
		"InvalidMDBMySQLPath": {
			Name: "vertis-h2p",
			Type: smProto.ServiceType_mdb_mysql,
			MDBCluster: &smProto.MDBCluster{
				ProdId: "mdbmef09944k5geatbkb",
				TestId: "mdb50ifhjj66soaqgk69",
			},
			Path:          "maps/vertis-h2p.yml",
			ExpectedError: []*user_error.UserError{ErrMySQLPathInvalid},
		},
		"InvalidMDBPostgreSQLPath": {
			Name: "vertis-test",
			Type: smProto.ServiceType_mdb_postgresql,
			MDBCluster: &smProto.MDBCluster{
				ProdId: "mdbvgaesr3ntkf2k80rq",
				TestId: "mdbfu6meb1bt71p6eetr",
			},
			Path:          "maps/vertis-test.yml",
			ExpectedError: []*user_error.UserError{ErrPostgreSQLPathInvalid},
		},
		"InvalidMDBPostgreSQLPath_mysql": {
			Name: "vertis-test",
			Type: smProto.ServiceType_mdb_postgresql,
			MDBCluster: &smProto.MDBCluster{
				ProdId: "mdbvgaesr3ntkf2k80rq",
				TestId: "mdbfu6meb1bt71p6eetr",
			},
			Path:          "maps/mysql/vertis-test.yml",
			ExpectedError: []*user_error.UserError{ErrPostgreSQLPathInvalid},
		},
		"InvalidKafkaPath": {
			Name: "vertis-test",
			Type: smProto.ServiceType_kafka,
			MDBCluster: &smProto.MDBCluster{
				ProdId: "mdbvgaesr3ntkf2k80rq",
				TestId: "mdbfu6meb1bt71p6eetr",
			},
			Path:          "maps/vertis-test.yml",
			ExpectedError: []*user_error.UserError{ErrKafkaPathInvalid},
		},
		"ValidKafkaPath": {
			Name: "vertis-test",
			Type: smProto.ServiceType_kafka,
			MDBCluster: &smProto.MDBCluster{
				ProdId: "mdbvgaesr3ntkf2k80rq",
				TestId: "mdbfu6meb1bt71p6eetr",
			},
			Path: "maps/kafka/shared-01.yml",
		},
		"InvalidKafkaTopic": {
			Name: "vertis-test",
			Type: smProto.ServiceType_kafka,
			MDBCluster: &smProto.MDBCluster{
				ProdId: "mdbvgaesr3ntkf2k80rq",
				TestId: "mdbfu6meb1bt71p6eetr",
			},
			Provides: []*smProto.ServiceProvides{
				{
					Name:        "rent-diff-events",
					Description: "rent-diff-events kafka topic",
					Options:     &smProto.ServiceProvides_KafkaOptions{KafkaOptions: &smProto.KafkaOptions{Partitions: 12, RetentionMs: 30000}},
					Protocol:    smProto.ServiceProvides_kafka,
				},
				{
					Name:        "rent-diff-events",
					Description: "rent-diff-events kafka topic",
					Options:     &smProto.ServiceProvides_KafkaOptions{KafkaOptions: &smProto.KafkaOptions{Partitions: 12, RetentionMs: 30000}},
					Protocol:    smProto.ServiceProvides_kafka,
				},
			},
			Path:          "maps/kafka/shared-01.yml",
			ExpectedError: []*user_error.UserError{ErrKafkaTopicInvalid},
		},
		"ValidKafkaTopic": {
			Name: "vertis-test",
			Type: smProto.ServiceType_kafka,
			MDBCluster: &smProto.MDBCluster{
				ProdId: "mdbvgaesr3ntkf2k80rq",
				TestId: "mdbfu6meb1bt71p6eetr",
			},
			Provides: []*smProto.ServiceProvides{
				{
					Name:        "rent-diff-events",
					Description: "rent-diff-events kafka topic",
					Options:     &smProto.ServiceProvides_KafkaOptions{KafkaOptions: &smProto.KafkaOptions{Partitions: 12, RetentionMs: 30000}},
					Protocol:    smProto.ServiceProvides_kafka,
				},
				{
					Name:        "glue",
					Description: "glue kafka topic",
					Options:     &smProto.ServiceProvides_KafkaOptions{KafkaOptions: &smProto.KafkaOptions{Partitions: 3, RetentionMs: 43200000}},
					Protocol:    smProto.ServiceProvides_kafka,
				},
			},
			Path: "maps/kafka/shared-01.yml",
		},
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			sm := sMap()
			sm.Name = tc.Name
			sm.IsExternal = tc.IsExternal
			sm.Type = tc.Type
			sm.MdbCluster = tc.MDBCluster
			sm.MdbMysql = tc.MDBMySQL
			sm.Path = tc.Path
			sm.Provides = tc.Provides

			service.mdb = getMdbMock(sm, tc.MdbError)

			errs, warns := service.Validate(sm, []*smProto.ServiceMap{sMap3()})
			test.AssertUserErrors(t, user_error.NewUserErrors(tc.ExpectedError...), errs)
			test.AssertUserErrors(t, user_error.NewUserErrors(tc.ExpectedWarn...), warns)
		})
	}
}

func TestService_Validate_Batch(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service := runUpService(t)

	testCases := []struct {
		name          string
		svcMap        *smProto.ServiceMap
		ExpectedError *user_error.UserError
		ExpectedWarn  *user_error.UserError
	}{
		{
			name:   "ok",
			svcMap: sMapBatch(),
		},
		{
			name:   "deny_provides",
			svcMap: sMapBatchProvides(),
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			sm := sMapBatch()
			errs, warns := service.Validate(sm, nil)
			test.AssertUserErrors(t, user_error.NewUserErrors(tc.ExpectedError), errs)
			test.AssertUserErrors(t, user_error.NewUserErrors(tc.ExpectedWarn), warns)
		})
	}
}

func sMap() *smProto.ServiceMap {
	return &smProto.ServiceMap{
		Name:           "service-name",
		Description:    "My description",
		Owners:         []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/"},
		DesignDocument: "",
		Src:            "https://github.com/YandexClassifieds/services",
		Type:           smProto.ServiceType_service,
		Provides: []*smProto.ServiceProvides{
			{
				Name:        "http",
				Description: "http api",
				Protocol:    smProto.ServiceProvides_http,
				Port:        80,
			},
		},
		DependsOn: []*smProto.ServiceDependency{
			{
				Service:       "service-name-2",
				InterfaceName: "main",
			},
		},
		Sox:  false,
		Path: "maps/service-name.yml",
	}
}

func sMap2() *smProto.ServiceMap {
	return &smProto.ServiceMap{
		Name:           "service-name-2",
		Description:    "My description 2",
		Owners:         []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/"},
		DesignDocument: "",
		Src:            "https://a.yandex-team.ru/arc/trunk/arcadia/classifieds",
		Type:           smProto.ServiceType_service,
		Provides: []*smProto.ServiceProvides{
			{
				Name:        "main",
				Description: "http api",
				Protocol:    smProto.ServiceProvides_http,
				Port:        80,
			},
			{
				Name:        "main2",
				Description: "http api",
				Protocol:    smProto.ServiceProvides_http,
				Port:        80,
			},
		},
		Sox:  false,
		Path: "maps/service-name-2.yml",
	}
}

func sMap3() *smProto.ServiceMap {
	return &smProto.ServiceMap{
		Name:           "service-name-3",
		Description:    "My description 3",
		Owners:         []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/"},
		DesignDocument: "",
		Src:            "https://github.com/YandexClassifieds/services",
		Type:           smProto.ServiceType_service,
		Provides: []*smProto.ServiceProvides{
			{
				Name:        "main",
				Description: "http api",
				Protocol:    smProto.ServiceProvides_http,
				Port:        80,
			},
		},
		Sox:  false,
		Path: "maps/service-name-3.yml",
	}
}

func sMapBatch() *smProto.ServiceMap {
	return &smProto.ServiceMap{
		Name:        "some-batch-svc",
		Description: "desc",
		Owners:      []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/"},
		Src:         "https://github.com/YandexClassifieds/services",
		Type:        smProto.ServiceType_batch,
	}
}

func sMapBatchProvides() *smProto.ServiceMap {
	return &smProto.ServiceMap{
		Name:        "some-batch-svc",
		Description: "desc",
		Owners:      []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/"},
		Src:         "https://github.com/YandexClassifieds/services",
		Type:        smProto.ServiceType_batch,
		Provides: []*smProto.ServiceProvides{
			{
				Name:        "main",
				Description: "tcp",
				Protocol:    smProto.ServiceProvides_tcp,
				Port:        3306,
			},
		},
	}
}

func sMapMySQL() *smProto.ServiceMap {
	return &smProto.ServiceMap{
		Name:           "main",
		Description:    "Desc",
		Owners:         []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/"},
		DesignDocument: "",
		Src:            "https://github.com/YandexClassifieds/services",
		Type:           smProto.ServiceType_mysql,
		Provides: []*smProto.ServiceProvides{
			{
				Name:        "main",
				Description: "tcp",
				Protocol:    smProto.ServiceProvides_tcp,
				Port:        3306,
			},
		},
		Sox:  false,
		Path: "maps/mysql/main.yml",
	}
}

func sMapMDBMySQL() *smProto.ServiceMap {
	return &smProto.ServiceMap{
		Name:           "vertis-h2p",
		Description:    "Desc",
		Owners:         []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/"},
		DesignDocument: "",
		Src:            "https://github.com/YandexClassifieds/services",
		Type:           smProto.ServiceType_mdb_mysql,
		MdbCluster: &smProto.MDBCluster{
			ProdId: "mdbmef09944k5geatbkb",
			TestId: "mdb50ifhjj66soaqgk69",
		},
		Provides: []*smProto.ServiceProvides{
			{
				Name:        "h2p-idm",
				Description: "tcp",
				Protocol:    smProto.ServiceProvides_tcp,
				Port:        3306,
			},
		},
		Sox:  false,
		Path: "maps/mysql/vertis-h2p.yml",
	}
}

func sMapMDBPostgreSQL() *smProto.ServiceMap {
	return &smProto.ServiceMap{
		Name:           "vertis-test",
		Description:    "Desc",
		Owners:         []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/"},
		DesignDocument: "",
		Src:            "https://github.com/YandexClassifieds/services",
		Type:           smProto.ServiceType_mdb_postgresql,
		MdbCluster: &smProto.MDBCluster{
			ProdId: "mdbvgaesr3ntkf2k80rq",
			TestId: "mdbfu6meb1bt71p6eetr",
		},
		Provides: []*smProto.ServiceProvides{
			{
				Name:        "test",
				Description: "tcp",
				Protocol:    smProto.ServiceProvides_tcp,
				Port:        6432,
			},
		},
		Sox:  false,
		Path: "maps/postgresql/vertis-test.yml",
	}
}

func sMapKafka() *smProto.ServiceMap {
	return &smProto.ServiceMap{
		Name:           "shared-01",
		Description:    "Desc",
		Owners:         []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/"},
		DesignDocument: "",
		Src:            "https://github.com/YandexClassifieds/services",
		Type:           smProto.ServiceType_kafka,
		MdbCluster: &smProto.MDBCluster{
			ProdId: "mdb4u82hc7bsaq7cq1tc",
			TestId: "mdb9adu18n492urt7kuk",
		},
		Provides: []*smProto.ServiceProvides{
			{
				Name:        "rent-diff-events",
				Description: "rent-diff-events kafka topic",
				Options:     &smProto.ServiceProvides_KafkaOptions{KafkaOptions: &smProto.KafkaOptions{Partitions: 12, RetentionMs: 30000}},
				Protocol:    smProto.ServiceProvides_kafka,
			},
			{
				Name:        "glue",
				Description: "glue kafka topic",
				Options:     &smProto.ServiceProvides_KafkaOptions{KafkaOptions: &smProto.KafkaOptions{Partitions: 3, RetentionMs: 43200000}},
				Protocol:    smProto.ServiceProvides_kafka,
			},
		},
		Path: "maps/kafka/shared-01.yml",
	}
}

func runUpService(t *testing.T) *Service {
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	service := service_map.NewService(db, log, service_change.NewNotificationMock())
	sMap := sMap2()
	bytes, err := yaml.Marshal(sMap)
	test.Check(t, err)
	err = service.ReadAndSave(bytes, 10, "maps/service-name-2.yml")
	test.Check(t, err)

	nomadConf := nomadAPI.DefaultConfig()
	nomadConf.Address = config.Str("NOMAD_ENDPOINT")

	nomadClient, err := nomadAPI.NewClient(nomadConf)
	if err != nil {
		log.WithError(err).Fatalf("Can't create new client")
	}

	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	featureFlagsService := feature_flags.NewService(db, mq.NewProducerMock(), log)
	return NewService(service, nil, staffService, nomadClient, *conductor.NewApi(log), featureFlagsService, log)
}

func getMdbMock(sMap *smProto.ServiceMap, err error) *mock.MdbService {
	mdbSvcMock := &mock.MdbService{}
	if sMap.MdbCluster != nil && (sMap.Type == smProto.ServiceType_mdb_mysql || sMap.Type == smProto.ServiceType_mdb_postgresql || sMap.Type == smProto.ServiceType_kafka) {
		if err == nil {
			mdbSvcMock.On("GetCluster", sMap.Type, sMap.MdbCluster.TestId).
				Return(&mdb.Cluster{Name: sMap.Name + "-test", Environment: mdb.Prestable}, nil).Once()
			mdbSvcMock.On("GetCluster", sMap.Type, sMap.MdbCluster.ProdId).
				Return(&mdb.Cluster{Name: sMap.Name + "-prod", Environment: mdb.Production}, nil).Once()
		} else {
			mdbSvcMock.On("GetCluster", sMap.Type, sMap.MdbCluster.TestId).
				Return(nil, err).Once()
			mdbSvcMock.On("GetCluster", sMap.Type, sMap.MdbCluster.ProdId).
				Return(nil, err).Once()
		}
	}

	return mdbSvcMock
}
