package scheduler

import (
	"github.com/YandexClassifieds/shiva/pkg/secrets"
	"testing"

	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/config"
	proto "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	manifest "github.com/YandexClassifieds/shiva/pkg/manifest/model"
	"github.com/stretchr/testify/assert"
)

const (
	commonDomainTag = "domain-svc-api.vrts-slb.test.vertis.yandex.net"
	branchDomainTag = "domain-svc-branch-api.vrts-slb.test.vertis.yandex.net"
	canaryDomainTag = "domain-svc-canary-api.vrts-slb.test.vertis.yandex.net"

	metricsTag       = "metrics_svc"
	metricsBranchTag = "metrics_svc-branch"
	metricsCanaryTag = "metrics_svc-canary"
	batchMetricsTag  = "batch_metrics"

	serviceTag            = "service=svc"
	branchTag             = "branch=branch"
	branchCanaryTag       = "branch=canary"
	canaryTrueTag         = "canary=true"
	canaryFalseTag        = "canary=false"
	providesTag           = "provides=api"
	providesMonitoringTag = "provides=monitoring"
)

func TestMakeServiceDefinition_WithoutBranch(t *testing.T) {
	s := &proto.ServiceMap{Name: "svc"}
	m := &manifest.Manifest{Name: "svc"}
	sctx := MakeContext("1.0", "", m, s, 0, common.Test, nil, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	sp := &proto.ServiceProvides{
		Name: "api",
		Port: 42,
	}
	sd := MakeServiceDefinition(common.Test, sctx, sp)

	assert.Equal(t, "svc-api", sd.SanitizedName)
	assert.Equal(t, 42, sd.Port)
	assert.Contains(t, sd.Tags, commonDomainTag)
	assert.NotContains(t, sd.Tags, branchDomainTag)
	assert.NotContains(t, sd.Tags, canaryDomainTag)

	assert.NotContains(t, sd.Tags, metricsTag)
	assert.NotContains(t, sd.Tags, metricsBranchTag)
	assert.NotContains(t, sd.Tags, metricsCanaryTag)

	assert.Contains(t, sd.Tags, serviceTag)
	assert.NotContains(t, sd.Tags, branchTag)
	assert.NotContains(t, sd.Tags, branchCanaryTag)
	assert.Contains(t, sd.Tags, providesTag)
	assert.NotContains(t, sd.Tags, providesMonitoringTag)
	assert.NotContains(t, sd.Tags, canaryTrueTag)
	assert.Contains(t, sd.Tags, canaryFalseTag)
}

func TestMakeServiceDefinition_Branch(t *testing.T) {
	s := &proto.ServiceMap{Name: "svc"}
	m := &manifest.Manifest{Name: "svc"}
	sctx := MakeContext("1.0", "branch", m, s, 0, common.Test, nil, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	sp := &proto.ServiceProvides{
		Name: "api",
		Port: 42,
	}
	sd := MakeServiceDefinition(common.Test, sctx, sp)

	assert.Equal(t, "svc-branch-api", sd.SanitizedName)
	assert.Equal(t, 42, sd.Port)
	assert.Contains(t, sd.Tags, branchDomainTag)
	assert.NotContains(t, sd.Tags, commonDomainTag)
	assert.NotContains(t, sd.Tags, canaryDomainTag)

	assert.NotContains(t, sd.Tags, metricsTag)
	assert.NotContains(t, sd.Tags, metricsBranchTag)
	assert.NotContains(t, sd.Tags, metricsCanaryTag)

	assert.Contains(t, sd.Tags, serviceTag)
	assert.Contains(t, sd.Tags, branchTag)
	assert.NotContains(t, sd.Tags, branchCanaryTag)
	assert.Contains(t, sd.Tags, providesTag)
	assert.NotContains(t, sd.Tags, providesMonitoringTag)
	assert.NotContains(t, sd.Tags, canaryTrueTag)
	assert.Contains(t, sd.Tags, canaryFalseTag)
}

func TestMakeServiceDefinition_Canary(t *testing.T) {
	s := &proto.ServiceMap{Name: "svc"}
	m := &manifest.Manifest{Name: "svc"}
	sctx := MakeContext("1.0", config.Canary, m, s, 0, common.Test, nil, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	sp := &proto.ServiceProvides{
		Name: "api",
		Port: 42,
	}
	sd := MakeServiceDefinition(common.Test, sctx, sp)

	assert.Equal(t, "svc-canary-api", sd.SanitizedName)
	assert.Equal(t, 42, sd.Port)
	assert.Contains(t, sd.Tags, canaryDomainTag)
	assert.NotContains(t, sd.Tags, branchDomainTag)

	assert.NotContains(t, sd.Tags, metricsTag)
	assert.NotContains(t, sd.Tags, metricsBranchTag)
	assert.NotContains(t, sd.Tags, metricsCanaryTag)

	assert.Contains(t, sd.Tags, serviceTag)
	assert.NotContains(t, sd.Tags, branchTag)
	assert.Contains(t, sd.Tags, branchCanaryTag)
	assert.Contains(t, sd.Tags, providesTag)
	assert.NotContains(t, sd.Tags, providesMonitoringTag)
	assert.Contains(t, sd.Tags, canaryTrueTag)
	assert.NotContains(t, sd.Tags, canaryFalseTag)
}

func TestMakeMonitoringServiceDefinition_WithoutBranch(t *testing.T) {
	s := &proto.ServiceMap{Name: "svc"}
	m := &manifest.Manifest{Name: "svc"}
	sctx := MakeContext("1.0", "", m, s, 0, common.Test, nil, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})

	sd := MakeMonitoringServiceDefinition(sctx)

	assert.Equal(t, "svc-monitoring", sd.SanitizedName)
	assert.Equal(t, MonitoringPort, sd.Port)

	assert.Contains(t, sd.Tags, metricsTag)
	assert.NotContains(t, sd.Tags, metricsBranchTag)
	assert.NotContains(t, sd.Tags, metricsCanaryTag)

	assert.Contains(t, sd.Tags, serviceTag)
	assert.NotContains(t, sd.Tags, branchTag)
	assert.NotContains(t, sd.Tags, branchCanaryTag)
	assert.NotContains(t, sd.Tags, providesTag)
	assert.Contains(t, sd.Tags, providesMonitoringTag)
	assert.NotContains(t, sd.Tags, canaryTrueTag)
	assert.Contains(t, sd.Tags, canaryFalseTag)
}

func TestMakeMonitoringServiceDefinition_Branch(t *testing.T) {
	s := &proto.ServiceMap{Name: "svc"}
	m := &manifest.Manifest{Name: "svc"}
	sctx := MakeContext("1.0", "branch", m, s, 0, common.Test, nil, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})

	sd := MakeMonitoringServiceDefinition(sctx)

	assert.Equal(t, "svc-branch-monitoring", sd.SanitizedName)
	assert.Equal(t, MonitoringPort, sd.Port)

	assert.NotContains(t, sd.Tags, metricsTag)
	assert.Contains(t, sd.Tags, metricsBranchTag)
	assert.NotContains(t, sd.Tags, metricsCanaryTag)

	assert.Contains(t, sd.Tags, serviceTag)
	assert.Contains(t, sd.Tags, branchTag)
	assert.NotContains(t, sd.Tags, branchCanaryTag)
	assert.NotContains(t, sd.Tags, providesTag)
	assert.Contains(t, sd.Tags, providesMonitoringTag)
	assert.NotContains(t, sd.Tags, canaryTrueTag)
	assert.Contains(t, sd.Tags, canaryFalseTag)
}

func TestMakeMonitoringServiceDefinition_Batch(t *testing.T) {
	s := &proto.ServiceMap{Name: "svc", Type: proto.ServiceType_batch}
	m := &manifest.Manifest{Name: "svc"}
	sctx := MakeContext("1.0", "", m, s, 0, common.Test, nil, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})

	sd := MakeMonitoringServiceDefinition(sctx)

	assert.Equal(t, "svc-monitoring", sd.SanitizedName)
	assert.Equal(t, MonitoringPort, sd.Port)

	assert.Contains(t, sd.Tags, batchMetricsTag)
	assert.NotContains(t, sd.Tags, metricsBranchTag)
	assert.NotContains(t, sd.Tags, metricsCanaryTag)

	assert.Contains(t, sd.Tags, serviceTag)
	assert.NotContains(t, sd.Tags, branchTag)
	assert.NotContains(t, sd.Tags, branchCanaryTag)
	assert.NotContains(t, sd.Tags, providesTag)
	assert.Contains(t, sd.Tags, providesMonitoringTag)
	assert.NotContains(t, sd.Tags, canaryTrueTag)
	assert.Contains(t, sd.Tags, canaryFalseTag)
}

func TestMakeMonitoringServiceDefinition_Canary(t *testing.T) {
	s := &proto.ServiceMap{Name: "svc"}
	m := &manifest.Manifest{Name: "svc"}
	sctx := MakeContext("1.0", config.Canary, m, s, 0, common.Test, nil, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})

	sd := MakeMonitoringServiceDefinition(sctx)

	assert.Equal(t, "svc-canary-monitoring", sd.SanitizedName)
	assert.Equal(t, MonitoringPort, sd.Port)

	assert.NotContains(t, sd.Tags, metricsTag)
	assert.NotContains(t, sd.Tags, metricsBranchTag)
	assert.Contains(t, sd.Tags, metricsCanaryTag)

	assert.Contains(t, sd.Tags, serviceTag)
	assert.NotContains(t, sd.Tags, branchTag)
	assert.Contains(t, sd.Tags, branchCanaryTag)
	assert.NotContains(t, sd.Tags, providesTag)
	assert.Contains(t, sd.Tags, providesMonitoringTag)
	assert.Contains(t, sd.Tags, canaryTrueTag)
	assert.NotContains(t, sd.Tags, canaryFalseTag)
}

func TestSanitizeServiceName(t *testing.T) {
	name := "X_Y_ZW!$#123"
	assert.Equal(t, "XYZW123", SanitizeServiceName(name))
	assert.Equal(t, "secret-service-delegationtokens", SanitizeServiceName("secret-service-delegation_tokens"))
}
