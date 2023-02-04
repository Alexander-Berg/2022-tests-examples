package parser

import (
	"testing"

	proto "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestNotYml(t *testing.T) {
	s := NewService()
	_, err := s.Parse([]byte("aaa bbb ccc"), "")
	assert.NotNil(t, err)
	assert.Contains(t, err.Error(), "yaml: unmarshal errors")
}

func TestParseService(t *testing.T) {
	s := NewService()
	sMap, err := s.Parse([]byte(`name: service
description: some description
owners:
  - https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra
design_document: https://docs.yandex-team.ru/classifieds-infra/deploy/manifest/
src: https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo
provides:
  - name: my-api
    protocol: grpc
    port: 1337
    description: echo
depends_on:
  - service: zookeeper
    interface_name: kv
    expected_rps:   10
    failure_reaction:
        missing:           fatal
        timeout:           severe
        unexpected_result: graceful
        errors:            fatal
sox: true
pci_dss: true
chat_duty: https://t.me/Chat
startrek: VOID
language: go`), "maps/service.yml")

	assert.Nil(t, err)
	assert.Equal(t, sMap.Name, "service")
	assert.Equal(t, sMap.Type, proto.ServiceType_service)

	assert.Equal(t, len(sMap.Provides), 1)
	assert.Equal(t, sMap.Provides[0].Name, "my-api")
	assert.Equal(t, sMap.Provides[0].Protocol, proto.ServiceProvides_grpc)
	assert.Equal(t, sMap.Provides[0].Port, uint32(1337))
	assert.Equal(t, sMap.Provides[0].Description, "echo")

	assert.Equal(t, len(sMap.DependsOn), 1)
	assert.Equal(t, sMap.DependsOn[0].GetPath(), "zookeeper")
	assert.Equal(t, sMap.DependsOn[0].InterfaceName, "kv")
	assert.Equal(t, sMap.DependsOn[0].ExpectedRps, uint32(10))
	assert.Equal(t, sMap.DependsOn[0].FailureReaction.Missing, proto.FailureReaction_Fatal)
	assert.Equal(t, sMap.DependsOn[0].FailureReaction.Timeout, proto.FailureReaction_Severe)
	assert.Equal(t, sMap.DependsOn[0].FailureReaction.UnexpectedResult, proto.FailureReaction_Graceful)
	assert.Equal(t, sMap.DependsOn[0].FailureReaction.Errors, proto.FailureReaction_Fatal)

	assert.True(t, sMap.Sox, "sox")
	assert.True(t, sMap.PciDss, "pci-dss")
	assert.Equal(t, sMap.Startrek, "VOID")
	assert.Equal(t, sMap.ChatDuty, "https://t.me/Chat")
	assert.Equal(t, sMap.Language, proto.ServiceLanguage_GO)
}

func TestParseMDBMySQL(t *testing.T) {
	s := NewService()
	sMap, err := s.Parse([]byte(`name: vertis-h2p
type: mdb_mysql
mdb_mysql:
 prod_id: mdb00000000000000000
 test_id: mdb11111111111111111
provides:
 - name: subscribe
   protocol: tcp
   port: 3306
   description: database subscribe`), "maps/mysql/vertis-h2p.yml")

	assert.Nil(t, err)
	assert.Equal(t, sMap.Name, "vertis-h2p")
	assert.Equal(t, sMap.Type, proto.ServiceType_mdb_mysql)
	assert.Equal(t, sMap.MdbMysql.ProdId, "mdb00000000000000000")
	assert.Equal(t, sMap.MdbMysql.TestId, "mdb11111111111111111")
	assert.Equal(t, len(sMap.Provides), 1)
	assert.Equal(t, sMap.Provides[0].Name, "subscribe")
	assert.Equal(t, sMap.Provides[0].Protocol, proto.ServiceProvides_tcp)
	assert.Equal(t, sMap.Provides[0].Port, uint32(3306))
	assert.Equal(t, sMap.Provides[0].Description, "database subscribe")
}

func TestParseMDBPostgreSQL(t *testing.T) {
	s := NewService()
	sMap, err := s.Parse([]byte(`name: vertis-test
type: mdb_postgresql
mdb_cluster:
 prod_id: mdb00000000000000000
 test_id: mdb11111111111111111
provides:
 - name: subscribe
   protocol: tcp
   port: 6432
   description: database subscribe`), "maps/postgresql/vertis-test.yml")

	t.Log(sMap)

	assert.Nil(t, err)
	assert.Equal(t, sMap.Name, "vertis-test")
	assert.Equal(t, sMap.Type, proto.ServiceType_mdb_postgresql)
	assert.Equal(t, sMap.MdbCluster.ProdId, "mdb00000000000000000")
	assert.Equal(t, sMap.MdbCluster.TestId, "mdb11111111111111111")
	assert.Equal(t, len(sMap.Provides), 1)
	assert.Equal(t, sMap.Provides[0].Name, "subscribe")
	assert.Equal(t, sMap.Provides[0].Protocol, proto.ServiceProvides_tcp)
	assert.Equal(t, sMap.Provides[0].Port, uint32(6432))
	assert.Equal(t, sMap.Provides[0].Description, "database subscribe")
}

func TestParseKafka(t *testing.T) {
	s := NewService()
	sMap, err := s.Parse([]byte(`name: shared-01
description: kafka shared-01 cluster
type: kafka
mdb_cluster:
  prod_id: mdb00000000000000000
  test_id: mdb11111111111111111
provides:
  - name: rent-diff-events
    protocol: kafka
    description: rent-diff-events kafka topic
    options:
      partitions: 12
      retention.ms: 30000
  - name: glue
    protocol: kafka
    description: glue kafka topic
    options:
      partitions: 3
      retention.ms: 43200000`), "maps/kafka/shared-01.yml")

	t.Log(sMap)

	assert.Nil(t, err)
	assert.Equal(t, sMap.Name, "shared-01")
	assert.Equal(t, sMap.Description, "kafka shared-01 cluster")
	assert.Equal(t, sMap.Type, proto.ServiceType_kafka)
	assert.Equal(t, sMap.MdbCluster.ProdId, "mdb00000000000000000")
	assert.Equal(t, sMap.MdbCluster.TestId, "mdb11111111111111111")
	assert.Equal(t, len(sMap.Provides), 2)

	assert.Equal(t, sMap.Provides[0].Name, "rent-diff-events")
	assert.Equal(t, sMap.Provides[0].Description, "rent-diff-events kafka topic")
	assert.Equal(t, sMap.Provides[0].Protocol, proto.ServiceProvides_kafka)
	assert.Equal(t, &proto.KafkaOptions{
		Partitions:  sMap.Provides[0].GetKafkaOptions().Partitions,
		RetentionMs: sMap.Provides[0].GetKafkaOptions().RetentionMs,
	}, &proto.KafkaOptions{
		Partitions:  12,
		RetentionMs: 30000,
	})

	assert.Equal(t, sMap.Provides[1].Name, "glue")
	assert.Equal(t, sMap.Provides[1].Description, "glue kafka topic")
	assert.Equal(t, sMap.Provides[1].Protocol, proto.ServiceProvides_kafka)
	assert.Equal(t, &proto.KafkaOptions{
		Partitions:  sMap.Provides[1].GetKafkaOptions().Partitions,
		RetentionMs: sMap.Provides[1].GetKafkaOptions().RetentionMs,
	}, &proto.KafkaOptions{
		Partitions:  3,
		RetentionMs: 43200000,
	})
}

func TestParseLanguage(t *testing.T) {
	s := NewService()
	sMap, err := s.Parse([]byte(`name: service
description: some description
owners:
 - https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra
design_document: https://docs.yandex-team.ru/classifieds-infra/deploy/manifest/
src: https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo
language: scala
chat_duty: https://t.me/Chat
startrek: VOID`), "maps/service.yml")

	assert.NoError(t, err)

	require.Equal(t, sMap.Language, proto.ServiceLanguage_SCALA)
}

func TestParseLanguageMixedCase(t *testing.T) {
	s := NewService()
	sMap, err := s.Parse([]byte(`name: service
description: some description
owners:
 - https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra
design_document: https://docs.yandex-team.ru/classifieds-infra/deploy/manifest/
src: https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo
language: NodeJs
chat_duty: https://t.me/Chat
startrek: VOID`), "maps/service.yml")

	assert.NoError(t, err)

	require.Equal(t, sMap.Language, proto.ServiceLanguage_NODEJS)
}

func TestParseLanguageUnknown(t *testing.T) {
	s := NewService()
	sMap, err := s.Parse([]byte(`name: service
description: some description
owners:
 - https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra
design_document: https://docs.yandex-team.ru/classifieds-infra/deploy/manifest/
src: https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo
language: Rust
chat_duty: https://t.me/Chat
startrek: VOID`), "maps/service.yml")

	assert.ErrorIs(t, err, proto.ErrLanguageUnknown)

	require.Equal(t, sMap.Language, proto.ServiceLanguage_UNKNOWN)
}

func TestParseLanguageEmpty(t *testing.T) {
	s := NewService()
	sMap, err := s.Parse([]byte(`name: service
description: some description
owners:
 - https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra
design_document: https://docs.yandex-team.ru/classifieds-infra/deploy/manifest/
src: https://github.com/YandexClassifieds/admin-utils/tree/master/grpc-echo
chat_duty: https://t.me/Chat
startrek: VOID`), "maps/service.yml")

	assert.NoError(t, err)

	require.Equal(t, sMap.Language, proto.ServiceLanguage_UNKNOWN)
}
