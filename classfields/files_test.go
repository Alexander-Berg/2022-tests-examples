package bot

import (
	"fmt"

	"github.com/YandexClassifieds/shiva/common/config"
	"github.com/YandexClassifieds/shiva/pkg/arc/diff/file"
)

const (
	newService = "new-service"
	oldService = "old-service"
	commonYml  = `
COMMON_INT_1: 1
COMMON_STRING_1: common 1
`
	common2ml = `
COMMON_INT_2: 1
COMMON_STRING_1: common 1
`
	prodYml = `
PROD_INT_1: 1
PROD_STRING_1: prod 1
`
	testYml = `
TEST_INT_1: 1
TEST_STRING_1: test 1
`
	sec1Yml = `
MY_SECRET_1: '${sec-1:ver-1:MY_SECRET_1}'
`
	sec2Yml = `
MY_SECRET_2: '${sec-2:ver-2:MY_SECRET_2}'
`
	template1Yml = `
SHIVA_URL: '${url:shiva:ci}'
`
	template2Yml = `
SHIVA_TVM: '${tvm-id:shiva}'
`
	template3Yml = `
SHIVA_HOST: '${host:shiva-tg:qwerty}'
`
	template4Yml = `
SHIVA_TVM: '${tvm-id:old-service}'
`
	templateValidYml = `
SHIVA_HOST: '${host:shiva-tg:api}'
`
	commonYmlPath        = "conf/service/common.yml"
	common2YmlPath       = "conf/service/common2.yml"
	prodYmlPath          = "conf/service/prod.yml"
	testYmlPath          = "conf/service/test.yml"
	sec1YmlPath          = "conf/service/sec1.yml"
	sec2YmlPath          = "conf/service/sec2.yml"
	template1YmlPath     = "conf/service/template1.yml"
	template2YmlPath     = "conf/service/template2.yml"
	template3YmlPath     = "conf/service/template3.yml"
	template4YmlPath     = "conf/service/template4.yml"
	templateValidYmlPath = "conf/service/template_valid.yml"
	oldServiceMap        = `
name: old-service
description: old-service test service
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
`

	dependsTestMap = `
name: depends-test-service
description: old-personal-service test service with single user owner
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_comp_auto'
  - 'https://staff.yandex-team.ru/ibiryulin'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
`
	dependsMap = `
name: depends-service
description: new-service test service
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_interface_interfaces2'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
depends_on:
  - service: depends-test-service
    interface_name: api
    expected_rps: 100
    failure_reaction:
      missing: fatal
      timeout: severe
      unexpected_result: severe
      errors: fatal
`
	oldServiceMap2 = `
name: old-service2
description: old-service2 test service
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
`
	invalidMap = `
name: invalid-service
description: invalid test service with empty owner
owner:
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
`
	shivaTgMap = `
name: shiva-tg
description: description
owner:
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
provides:
  - name: api
    protocol: grpc
    port: 80
`

	oldServiceMap3 = `
name: old-service3
description: old-service3 test service with group and user owners
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
  - 'https://staff.yandex-team.ru/dshtan'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
`

	oldServiceMapWrongOwners = `
name: old-service-wrong-owners
description: old-service-wrong-owners test service
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_veserv_infa_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
`
	oldPersonalServiceMap = `
name: old-personal-service
description: old-personal-service test service with single user owner
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_comp_auto'
  - 'https://staff.yandex-team.ru/ibiryulin'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
`
	oldServiceMapMySQL = `
name: old-mysql
description: old-mysql test mysql cluster
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: old
    protocol: tcp
    port: 3306
    description: old database
`
	oldManifest = `
name: old-service
general:
  datacenters:
    sas:
      count: 5
    vla:
      count: 5
test:
  config:
    files:
      - conf/service/common.yml
      - conf/service/test.yml
      - conf/service/sec1.yml
prod:
  config:
    files:
      - conf/service/common.yml
      - conf/service/prod.yml
      - conf/service/sec1.yml
`
	oldManifest2 = `
name: old-service2
general:
  datacenters:
    sas:
      count: 5
    vla:
      count: 5
  config:
    files:
      - conf/service/common2.yml
      - conf/service/sec1.yml
`
)

func smNew() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.MapBasePath + "/new-service.yml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: new-service
description: new-service test service
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
`),
	}
}

func smYaml() *file.File {
	return &file.File{
		Name:   "new-service.yaml",
		Path:   config.MapBasePath + "/new-service.yaml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: new-service
description: new-service test service
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
`),
	}
}

func smDeleted(name string) *file.File {
	return &file.File{
		Name:   name,
		Path:   fmt.Sprintf("%s/%s", config.MapBasePath, name),
		Type:   file.ServiceMap,
		Action: file.Delete,
	}
}

func smUpdateToLegacyOwner() *file.File {
	return &file.File{
		Name:   "old-service.yml",
		Path:   config.MapBasePath + "/old-service.yml",
		Type:   file.ServiceMap,
		Action: file.Update,
		Data: []byte(`
name: old-service
description: UPDATE FIELD!
owner: 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
`),
	}
}

func errorAndWarning() *file.File {
	return &file.File{
		Name:   "new-mdb.yml",
		Path:   config.MapBasePath + "/mysql/new-mdb.yml",
		Type:   file.ServiceMap,
		Action: file.Update,
		Data: []byte(`
name: new-mdb
description: UPDATE FIELD!
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
startrek: NOVOID
type: mdb_mysql
mdb_mysql:
  test_id: mdb000000000
  prod_id: mdb111111111
provides:
  - name: db
    protocol: tcp
    port: 3306
    description: main db
`),
	}
}

func smNewWithBadSTQueue() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.MapBasePath + "/new-service.yml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: new-service
description: new-service test service
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
startrek: NOVOID
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
`),
	}
}

func smUpdate() *file.File {
	return &file.File{
		Name:   "old-service.yml",
		Path:   config.MapBasePath + "/old-service.yml",
		Type:   file.ServiceMap,
		Action: file.Update,
		Data: []byte(`
name: old-service
description: UPDATE FIELD!
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
`),
	}
}

func smOwnerChanged() *file.File {
	return &file.File{
		Name:   "old-personal-service.yml",
		Path:   config.MapBasePath + "/old-personal-service.yml",
		Type:   file.ServiceMap,
		Action: file.Update,
		Data: []byte(`
name: old-personal-service
description: old-personal-service test service with single user owner
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_comp_auto'
  - 'https://staff.yandex-team.ru/swapster'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
`),
	}
}

func smOwnerChangedToTeamWithoutLeader() *file.File {
	return &file.File{
		Name:   "old-personal-service.yml",
		Path:   config.MapBasePath + "/old-personal-service.yml",
		Type:   file.ServiceMap,
		Action: file.Update,
		Data: []byte(`
name: old-personal-service
description: old-personal-service test service with single user owner
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_without_leader'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
`),
	}
}

func smNewAddDependency() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.MapBasePath + "/new-service.yml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: new-service
description: new-service test service
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
depends_on:
  - service: old-service2
    interface_name: api
    expected_rps: 100
    failure_reaction:
      missing: fatal
      timeout: severe
      unexpected_result: severe
      errors: fatal
`),
	}
}

func smNewMapWithDependencyToAnotherService() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.MapBasePath + "/new-service.yml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: new-service
description: new-service test service
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_interface_interfaces2'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
depends_on:
  - service: old-service2
    interface_name: api
    expected_rps: 100
    failure_reaction:
      missing: fatal
      timeout: severe
      unexpected_result: severe
      errors: fatal
`),
	}
}

func smNewAddMySQLDependency() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.MapBasePath + "/new-service.yml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: new-service
description: new-service test service
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
depends_on:
  - service: mysql/old-mysql
    interface_name: old
    expected_rps: 100
    failure_reaction:
      missing: fatal
      timeout: severe
      unexpected_result: severe
      errors: fatal
`),
	}
}

func smNewAddInvalidMySQLOwners() *file.File {
	return &file.File{
		Name:   "new-mdb.yml",
		Path:   config.MapBasePath + "/mysql/new-mdb.yml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: new-mdb
description: new mysql cluster
owners:
  - ''
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
type: mdb_mysql
mdb_cluster:
  test_id: mdb000000000
  prod_id: mdb111111111
provides:
  - name: database
    protocol: tcp
    port: 3306
    description: database
`),
	}
}

func smNewAddInvalidMySQLMDBCluster() *file.File {
	return &file.File{
		Name:   "new-mdb.yml",
		Path:   config.MapBasePath + "/mysql/new-mdb.yml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: new-mdb
description: new mysql cluster
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
type: mdb_mysql
mdb_cluster:
  test_id: mdb888888888
  prod_id: mdb999999999
provides:
  - name: database
    protocol: tcp
    port: 3306
    description: database
`),
	}
}

func smNewAddInvalidMySQLDependency() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.MapBasePath + "/new-service.yml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: new-service
description: new-service test service
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
depends_on:
  - service: mysql/old-mysql-invalid
    interface_name: old
    expected_rps: 100
    failure_reaction:
      missing: fatal
      timeout: severe
      unexpected_result: severe
      errors: fatal
`),
	}
}

func smNewAddMySQLDependencyInvalidInterface() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.MapBasePath + "/new-service.yml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: new-service
description: new-service test service
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_interface_interfaces2'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
depends_on:
  - service: mysql/old-mysql
    interface_name: invalid
    expected_rps: 100
    failure_reaction:
      missing: fatal
      timeout: severe
      unexpected_result: severe
      errors: fatal
`),
	}
}

func smNewAddDependencyOnInvalidService() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.MapBasePath + "/new-service.yml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: new-service
description: new-service test service
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
depends_on:
  - service: invalid-service
    interface_name: api
    expected_rps: 100
    failure_reaction:
      missing: fatal
      timeout: severe
      unexpected_result: severe
      errors: fatal
`),
	}
}

func smNewAddDependencyOnMultipleOwners() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.MapBasePath + "/new-service.yml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: new-service
description: new-service test service
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_interface_interfaces2'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
depends_on:
  - service: old-service3
    interface_name: api
    expected_rps: 100
    failure_reaction:
      missing: fatal
      timeout: severe
      unexpected_result: severe
      errors: fatal
`),
	}
}

func smNewMySQL() *file.File {
	return &file.File{
		Name:   "new-mdb.yml",
		Path:   config.MapBasePath + "/mysql/new-mdb.yml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: new-mdb
description: new-mdb test mysql
type: mdb_mysql
mdb_cluster:
  test_id: mdb000000000
  prod_id: mdb111111111
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: subscribe
    protocol: tcp
    port: 3306
    description: database subscribe
  - name: vos2_shard1
    protocol: tcp
    port: 3306
    description: database vos2_shard1
`),
	}
}

func smNewPostgreSQL() *file.File {
	return &file.File{
		Name:   "new-mdb.yml",
		Path:   config.MapBasePath + "/postgresql/new-mdb.yml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: new-mdb
description: new-mdb test postgresql
type: mdb_postgresql
mdb_cluster:
  test_id: mdb000000000
  prod_id: mdb111111111
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: subscribe
    protocol: tcp
    port: 6432
    description: database subscribe
  - name: vos2_shard1
    protocol: tcp
    port: 6432
    description: database vos2_shard1
`),
	}
}

func smNewKafkaTopic() *file.File {
	return &file.File{
		Name:   "shared-01.yml",
		Path:   config.MapBasePath + "/kafka/shared-01.yml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: shared-01
description: kafka shared-01 cluster
type: kafka
mdb_cluster:
  test_id: mdb000000001
  prod_id: mdb111111110
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: rent-diff-events-new
    protocol: kafka
    description: rent-diff-events-new kafka topic
    options:
      partitions: 5
      retention.ms: 30000
`),
	}
}

func smUpdateKafkaTopic() *file.File {
	return &file.File{
		Name:   "shared-01.yml",
		Path:   config.MapBasePath + "/kafka/shared-01.yml",
		Type:   file.ServiceMap,
		Action: file.Update,
		Data: []byte(`
name: shared-01
description: kafka shared-01 cluster
type: kafka
mdb_cluster:
  test_id: mdb000000001
  prod_id: mdb111111110
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: rent-diff-events
    protocol: kafka
    description: rent-diff-events kafka topic
    options:
      partitions: 999
      retention.ms: 99999
  - name: glue
    protocol: kafka
    description: glue kafka topic
    options:
      partitions: 999
      retention.ms: 99999
`),
	}
}

func smAddDependencyToMineService() *file.File {
	return &file.File{
		Name:   "old-service.yml",
		Path:   config.MapBasePath + "/old-service.yml",
		Type:   file.ServiceMap,
		Action: file.Update,
		Data: []byte(`
name: old-service
description: new-service test service
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
depends_on:
  - service: old-service2
    interface_name: api
    expected_rps: 100
    failure_reaction:
      missing: fatal
      timeout: severe
      unexpected_result: severe
      errors: fatal
`),
	}
}

func smAddDependencyToAnotherService() *file.File {
	return &file.File{
		Name:   "old-personal-service.yml",
		Path:   config.MapBasePath + "/old-personal-service.yml",
		Type:   file.ServiceMap,
		Action: file.Update,
		Data: []byte(`
name: old-personal-service
description: old-personal-service test service with single user owner
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_comp_auto'
  - 'https://staff.yandex-team.ru/spooner'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
depends_on:
  - service: old-service2
    interface_name: api
    expected_rps: 100
    failure_reaction:
      missing: fatal
      timeout: severe
      unexpected_result: severe
      errors: fatal
`),
	}
}

func smOldMySQLType() *file.File {
	return &file.File{
		Name:   "old-mysql.yml",
		Path:   config.MapBasePath + "/mysql/old-mysql.yml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: old-mysql
description: old-mysql
type: mysql
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: subscribe
    protocol: tcp
    port: 3306
    description: database subscribe
  - name: vos2_shard1
    protocol: tcp
    port: 3306
    description: database vos2_shard1
`),
	}
}

func smOldMDBMySQLField() *file.File {
	return &file.File{
		Name:   "new-mdb.yml",
		Path:   config.MapBasePath + "/mysql/new-mdb.yml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: new-mdb
description: new-mdb with deprecated field
type: mdb_mysql
mdb_mysql:
  test_id: mdb000000000
  prod_id: mdb111111111
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: subscribe
    protocol: tcp
    port: 3306
    description: database subscribe
  - name: vos2_shard1
    protocol: tcp
    port: 3306
    description: database vos2_shard1
`),
	}
}

func mNewWithEnv() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.ManifestBasePath + "/new-service.yml",
		Type:   file.DeploymentManifest,
		Action: file.New,
		Data: []byte(`

name: new-service
general:
  datacenters:
    sas:
      count: 1
    vla:
      count: 1
prod:
  datacenters:
    sas:
      count: 2
    vla:
      count: 2
  env:
    - SOME_EXTRA_VAR: 'here'
`),
	}
}

func mYaml() *file.File {
	return &file.File{
		Name:   "new-service.yaml",
		Path:   config.ManifestBasePath + "/new-service.yaml",
		Type:   file.DeploymentManifest,
		Action: file.New,
		Data: []byte(`

name: new-service
general:
  datacenters:
    sas:
      count: 1
    vla:
      count: 1
`),
	}
}

func mNewWithConfig() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.ManifestBasePath + "/new-service.yml",
		Type:   file.DeploymentManifest,
		Action: file.New,
		Data: []byte(`

name: new-service
general:
  datacenters:
    sas:
      count: 1
    vla:
      count: 1
prod:
  config:
    params:
      p1: p1
      p2: p2
      COMMON_STRING_1: param
    files:
      - conf/service/common.yml
`),
	}
}

func mExtraField() *file.File {
	return &file.File{
		Name:   "old-service.yml",
		Path:   config.ManifestBasePath + "/old-service.yml",
		Type:   file.DeploymentManifest,
		Action: file.New,
		Data: []byte(`

name: old-service
general:
  datacenters:
    sas:
      count: 1
    vla:
      count: 1
prod:
  config:
    params:
      p1: p1
      p2: p2
    filess:
      - conf/service/common.yml
`),
	}
}

func mNewWithConfAndConfig() *file.File {
	return &file.File{
		Name:   "old-service.yml",
		Path:   config.ManifestBasePath + "/old-service.yml",
		Type:   file.DeploymentManifest,
		Action: file.New,
		Data: []byte(`
name: old-service
general:
  datacenters:
    sas:
      count: 1
    vla:
      count: 1
  conf:
    - conf/service/common.yml
prod:
  config:
    files:
      - conf/service/common.yml
`),
	}
}

func mNewWithEnvAndConfig() *file.File {
	return &file.File{
		Name:   "old-service.yml",
		Path:   config.ManifestBasePath + "/old-service.yml",
		Type:   file.DeploymentManifest,
		Action: file.New,
		Data: []byte(`

name: old-service
general:
  datacenters:
    sas:
      count: 1
    vla:
      count: 1
  env:
    - JAEGER_AGENT_HOST: 'jaeger-agent.service.consul'
    - JAEGER_AGENT_PORT: 5775
    - JAEGER_SAMPLER_TYPE: 'const'
    - JAEGER_SAMPLER_PARAM: 1
    - LISTEN_ADDRESS: ':1337'
    - METRICS_LISTEN_ADDRESS: ':81'
prod:
  config:
    params:
      PARAM: PARAM
    files:
`),
	}
}

func mNewWithConf() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.ManifestBasePath + "/new-service.yml",
		Type:   file.DeploymentManifest,
		Action: file.New,
		Data: []byte(`

name: new-service
general:
  datacenters:
    sas:
      count: 2
    vla:
      count: 2
prod:
  conf:
    - conf/service/common.yml
    - conf/service/prod.yml
`),
	}
}

func mUpdate() *file.File {
	return &file.File{
		Name:   "old-service.yml",
		Path:   config.ManifestBasePath + "/old-service.yml",
		Type:   file.DeploymentManifest,
		Action: file.New,
		Data: []byte(`
name: old-service
general:
  datacenters:
    sas:
      count: 1
    vla:
      count: 1
test:
  config:
    files:
      - conf/service/common.yml
      - conf/service/test.yml
prod:
  config:
    files:
      - conf/service/common.yml
      - conf/service/prod.yml
`),
	}
}

func mDeleted(name string) *file.File {
	return &file.File{
		Type:   file.DeploymentManifest,
		Action: file.Delete,
		Path:   fmt.Sprintf("%s/%s", config.ManifestBasePath, "/delete-me.yml"),
		Name:   name,
	}
}

func inclNew() *file.File {
	return &file.File{
		Name:   "new.yml",
		Path:   config.IncludeBasePath + "/service/new.yml",
		Type:   file.Include,
		Action: file.New,
		Data: []byte(`
NEW_INT: 1
NEW_STRING: new 1
`),
	}
}

func inclYaml() *file.File {
	return &file.File{
		Name:   "new.yaml",
		Path:   config.IncludeBasePath + "/service/new.yaml",
		Type:   file.Include,
		Action: file.New,
		Data: []byte(`
NEW_INT: 1
`),
	}
}

func inclNewCollision() *file.File {
	return &file.File{
		Name:   "new.yml",
		Path:   config.IncludeBasePath + "/service/new.yml",
		Type:   file.Include,
		Action: file.New,
		Data: []byte(`
COMMON_INT_1: 1
NEW_INT: 1
NEW_STRING: new 1
`),
	}
}

func inclNewParseFail() *file.File {
	return &file.File{
		Name:   "fail.yml",
		Path:   config.IncludeBasePath + "/service/fail.yml",
		Type:   file.Include,
		Action: file.New,
		Data: []byte(`
fail-1: 1
`),
	}
}

func inclUpdateProdCollision() *file.File {
	return &file.File{
		Name:   "prod.yml",
		Path:   config.IncludeBasePath + "/service/prod.yml",
		Type:   file.Include,
		Action: file.Update,
		Data: []byte(`
COMMON_INT_1: 1
NEW_INT: 1
NEW_STRING: new 1
`),
	}
}

func inclUpdateCommon() *file.File {
	return &file.File{
		Name:   "common.yml",
		Path:   config.IncludeBasePath + "/service/common.yml",
		Type:   file.Include,
		Action: file.Update,
		Data: []byte(`
COMMON_INT_1: 2
COMMON_STRING_1: common 1
COMMON_NEW_STRING: common new 1
`),
	}
}

func mNewWitNewFile() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.ManifestBasePath + "/new-service.yml",
		Type:   file.DeploymentManifest,
		Action: file.New,
		Data: []byte(`

name: new-service
general:
  datacenters:
    sas:
      count: 2
    vla:
      count: 2
test:
  config:
    files:
      - conf/service/common.yml
      - conf/service/test.yml
      - conf/service/new.yml
prod:
  config:
    files:
      - conf/service/common.yml
      - conf/service/prod.yml
      - conf/service/new.yml
`),
	}
}

func mNewWithGeobase() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.ManifestBasePath + "/new-service.yml",
		Type:   file.DeploymentManifest,
		Action: file.New,
		Data: []byte(`

name: new-service
general:
  geobase_version: 6
  datacenters:
    sas:
      count: 2
    vla:
      count: 2
`),
	}
}

func mNewWithBadGeobaseVersion() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.ManifestBasePath + "/new-service.yml",
		Type:   file.DeploymentManifest,
		Action: file.New,
		Data: []byte(`

name: new-service
general:
  datacenters:
    sas:
      count: 2
    vla:
      count: 2
test:
  geobase_version: 42
prod:
  geobase_version: 69
`),
	}
}

func mUpdateWithSec2Config() *file.File {
	return &file.File{
		Name:   "old-service.yml",
		Path:   config.ManifestBasePath + "/old-service.yml",
		Type:   file.DeploymentManifest,
		Action: file.New,
		Data: []byte(`

name: old-service
general:
  datacenters:
    sas:
      count: 2
    vla:
      count: 2
test:
  config:
    files:
      - conf/service/common.yml
      - conf/service/test.yml
prod:
  config:
    files:
      - conf/service/common.yml
      - conf/service/prod.yml
      - conf/service/sec2.yml
`),
	}
}

func mUpdateWithTemplate1Config() *file.File {
	return &file.File{
		Name:   "old-service.yml",
		Path:   config.ManifestBasePath + "/old-service.yml",
		Type:   file.DeploymentManifest,
		Action: file.New,
		Data: []byte(`
name: old-service
general:
  datacenters:
    sas:
      count: 2
    vla:
      count: 2
test:
  config:
    files:
      - conf/service/common.yml
      - conf/service/test.yml
prod:
  config:
    files:
      - conf/service/common.yml
      - conf/service/prod.yml
      - conf/service/template1.yml
`),
	}
}

func mUpdateWithTemplate2Config() *file.File {
	return &file.File{
		Name:   "old-service.yml",
		Path:   config.ManifestBasePath + "/old-service.yml",
		Type:   file.DeploymentManifest,
		Action: file.New,
		Data: []byte(`
name: old-service
general:
  datacenters:
    sas:
      count: 2
    vla:
      count: 2
test:
  config:
    files:
      - conf/service/common.yml
      - conf/service/test.yml
prod:
  config:
    files:
      - conf/service/common.yml
      - conf/service/prod.yml
      - conf/service/template2.yml
`),
	}
}

func mUpdateWithTemplate3Config() *file.File {
	return &file.File{
		Name:   "old-service.yml",
		Path:   config.ManifestBasePath + "/old-service.yml",
		Type:   file.DeploymentManifest,
		Action: file.New,
		Data: []byte(`
name: old-service
general:
  datacenters:
    sas:
      count: 2
    vla:
      count: 2
test:
  config:
    files:
      - conf/service/common.yml
      - conf/service/test.yml
prod:
  config:
    files:
      - conf/service/common.yml
      - conf/service/prod.yml
      - conf/service/template4.yml
`),
	}
}

func mUpdateWithProviderTemplateConfig() *file.File {
	return &file.File{
		Name:   "old-service.yml",
		Path:   config.ManifestBasePath + "/old-service.yml",
		Type:   file.DeploymentManifest,
		Action: file.New,
		Data: []byte(`
name: old-service
general:
  datacenters:
    sas:
      count: 2
    vla:
      count: 2
test:
  config:
    files:
      - conf/service/common.yml
      - conf/service/test.yml
prod:
  config:
    files:
      - conf/service/common.yml
      - conf/service/prod.yml
      - conf/service/template3.yml
`),
	}
}

func mNewWithSec2Config() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.ManifestBasePath + "/new-service.yml",
		Type:   file.DeploymentManifest,
		Action: file.New,
		Data: []byte(`

name: new-service
general:
  datacenters:
    sas:
      count: 2
    vla:
      count: 2
test:
  config:
    files:
      - conf/service/common.yml
      - conf/service/test.yml
prod:
  config:
    files:
      - conf/service/common.yml
      - conf/service/prod.yml
      - conf/service/sec2.yml
`),
	}
}

func m2UpdateAddCommonCollision() *file.File {
	return &file.File{
		Name:   "old-service2.yml",
		Path:   config.ManifestBasePath + "/old-service2.yml",
		Type:   file.DeploymentManifest,
		Action: file.Update,
		Data: []byte(`

name: old-service2
general:
  datacenters:
    sas:
      count: 2
    vla:
      count: 2
test:
  config:
    files:
      - conf/service/common.yml
prod:
  config:
    files:
      - conf/service/common.yml
      - conf/service/common2.yml
`),
	}
}

func incWrongFormat() *file.File {
	return &file.File{
		Name:   "incorrect.yaml",
		Path:   config.IncludeBasePath + "/service/incorrect.yaml",
		Type:   file.Include,
		Action: file.New,
		Data: []byte(`
TEST: test
ANOTHER_TEST=test2
`),
	}
}

func incNotDefinedFormat() *file.File {
	return &file.File{
		Name:   "/yaml",
		Path:   config.IncludeBasePath + "/yaml",
		Type:   file.Include,
		Action: file.New,
		Data: []byte(`
TEST: test
ANOTHER_TEST: test2
`),
	}
}

func mWrongFormat() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.ManifestBasePath + "/new-service.yml",
		Type:   file.DeploymentManifest,
		Action: file.Update,
		Data: []byte(`

name=new-service
general:
  datacenters:
    sas:
      count: 2
    vla:
      count: 2
prod:
  conf:
    - conf/service/common.yml
`),
	}
}

func smWrongFormat() *file.File {
	return &file.File{
		Name:   "new-service.yaml",
		Path:   config.MapBasePath + "/new-service.yaml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: new-service
description: new-service test service
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
    name: api
    protocol: grpc
    port: 1337
    description: echo api
`),
	}
}

func smNewAddDependencyFromWrongOwnersService() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.MapBasePath + "/new-service.yml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: new-service
description: new-service test service
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
depends_on:
  - service: old-service-wrong-owners
    interface_name: api
    expected_rps: 100
    failure_reaction:
      missing: fatal
      timeout: severe
      unexpected_result: severe
      errors: fatal
`),
	}
}

func smNewWithLanguage() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.MapBasePath + "/new-service.yml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: new-service
description: new-service test service
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
language: Java
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
`),
	}
}

func smNewWithLanguageUnknown() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.MapBasePath + "/new-service.yml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: new-service
description: new-service test service
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
language: Jara
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
`),
	}
}

func smNewWithLanguageEmpty() *file.File {
	return &file.File{
		Name:   "new-service.yml",
		Path:   config.MapBasePath + "/new-service.yml",
		Type:   file.ServiceMap,
		Action: file.New,
		Data: []byte(`
name: new-service
description: new-service test service
owners:
  - 'https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt'
design_doc: 'https://docs.yandex-team.ru/classifieds-infra/'
src: 'https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo'
provides:
  - name: api
    protocol: grpc
    port: 1337
    description: echo api
`),
	}
}

func smNewWithValidTemplate() *file.File {
	return &file.File{
		Name:   "old-service.yml",
		Path:   config.ManifestBasePath + "/old-service.yml",
		Type:   file.DeploymentManifest,
		Action: file.New,
		Data: []byte(`
name: old-service
general:
  datacenters:
    sas:
      count: 2
    vla:
      count: 2
test:
  config:
    files:
      - conf/service/common.yml
      - conf/service/test.yml
prod:
  config:
    files:
      - conf/service/common.yml
      - conf/service/prod.yml
      - conf/service/template_valid.yml
`),
	}
}
