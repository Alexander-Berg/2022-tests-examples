package xds

import (
	"strings"
	"testing"

	"github.com/YandexClassifieds/envoy-api/common/logger"
	"github.com/YandexClassifieds/envoy-api/consul"
	"github.com/YandexClassifieds/envoy-api/info"
	"github.com/YandexClassifieds/envoy-api/test"
	"github.com/YandexClassifieds/envoy-api/test/mock"
	"github.com/YandexClassifieds/envoy-api/utils"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/assert"
)

func TestGetEndPoints_Old(t *testing.T) {
	data := test.Old()
	testGetEndPoints(t, data, map[string]utils.StringSet{
		data.Consul[0].Name + test.LegacyClusterPostfix: addresses(data.Consul[0]),
	})
}

func TestGetEndPoints_Olds(t *testing.T) {
	data := test.Olds()
	testGetEndPoints(t, data, map[string]utils.StringSet{
		data.Consul[0].Name + test.LegacyClusterPostfix: addresses(data.Consul[0]),
		data.Consul[1].Name + test.LegacyClusterPostfix: addresses(data.Consul[1]),
		data.Consul[2].Name + test.LegacyClusterPostfix: addresses(data.Consul[2]),
	})
}

func TestGetEndPoints_OldsCommonDomain(t *testing.T) {
	data := test.OldsCommonDomain()
	ser1 := data.Consul[0]
	ser2 := data.Consul[1]
	common := data.Consul[2]
	adr1 := addresses(ser1)
	adr2 := addresses(ser2)
	adrCommon := addresses(common)
	assert.Len(t, adrCommon, len(adr1)+len(adr2))
	testGetEndPoints(t, data, map[string]utils.StringSet{
		ser1.Name + test.LegacyClusterPostfix:   adr1,
		ser2.Name + test.LegacyClusterPostfix:   adr2,
		common.Name + test.LegacyClusterPostfix: adrCommon,
	})
}

func TestGetEndPoints_OldAddress(t *testing.T) {
	data := test.OldAddress()
	oldAddr := data.Shiva[0].Map.Provides[0].OldAddressTest
	name := join(data.Shiva[0].Map.Name, data.Shiva[0].Map.Provides[0].Name)
	odlName := strings.ReplaceAll(oldAddr, ".", "_")
	testGetEndPoints(t, data, map[string]utils.StringSet{
		name:    addresses(data.Consul[0]),
		odlName: addresses(data.Consul[0]),
	})
}

func TestGetEndPoints_Simple(t *testing.T) {
	data := test.Simple()
	name := join(data.Shiva[0].Map.Name, data.Shiva[0].Map.Provides[0].Name)
	testGetEndPoints(t, data, map[string]utils.StringSet{
		name: addresses(data.Consul[0]),
	})
}

func TestGetEndPoints_Simples(t *testing.T) {
	data := test.Simples()
	name1 := join(data.Shiva[0].Map.Name, data.Shiva[0].Map.Provides[0].Name)
	name2 := join(data.Shiva[1].Map.Name, data.Shiva[1].Map.Provides[0].Name)
	name3 := join(data.Shiva[2].Map.Name, data.Shiva[2].Map.Provides[0].Name)
	testGetEndPoints(t, data, map[string]utils.StringSet{
		name1: addresses(data.Consul[0]),
		name2: addresses(data.Consul[1]),
		name3: addresses(data.Consul[2]),
	})
}

func TestGetEndPoints_WithOld(t *testing.T) {
	data := test.WithOld()
	name := join(data.Shiva[0].Map.Name, data.Shiva[0].Map.Provides[0].Name)
	testGetEndPoints(t, data, map[string]utils.StringSet{
		name: joinAddresses(addresses(data.Consul[0]), addresses(data.Consul[1])),
	})
}

func TestGetEndPoints_WithOldAddressAndOld(t *testing.T) {
	data := test.WithOldAddressAndOld()
	name := join(data.Shiva[0].Map.Name, data.Shiva[0].Map.Provides[0].Name)
	oldName := data.Consul[0].Name + test.LegacyClusterPostfix
	testGetEndPoints(t, data, map[string]utils.StringSet{
		name:    addresses(data.Consul[1]),
		oldName: joinAddresses(addresses(data.Consul[0]), addresses(data.Consul[1])),
	})
}

func TestGetEndPoints_Empty(t *testing.T) {
	data := test.Empty()
	testGetEndPoints(t, data, map[string]utils.StringSet{})
}

func TestGetEndPoints_EmptyProvides(t *testing.T) {
	data := test.EmptyProvides()
	testGetEndPoints(t, data, map[string]utils.StringSet{})
}

func TestGetEndPoints_BranchWithoutTraffic(t *testing.T) {
	data := test.BranchWithoutTraffic()
	mainName := join(data.Shiva[0].Map.Name, data.Shiva[0].Map.Provides[0].Name)
	branchName := join(data.Shiva[1].Map.Name, data.Shiva[1].Deployment.Branch, data.Shiva[1].Map.Provides[0].Name)
	testGetEndPoints(t, data, map[string]utils.StringSet{
		mainName:   addresses(data.Consul[0]),
		branchName: addresses(data.Consul[1]),
	})
}

func TestGetEndPoints_BranchOnePercent(t *testing.T) {
	data := test.BranchOnePercent()
	mainName := join(data.Shiva[0].Map.Name, data.Shiva[0].Map.Provides[0].Name)
	branchName := join(data.Shiva[1].Map.Name, data.Shiva[1].Deployment.Branch, data.Shiva[1].Map.Provides[0].Name)
	testGetEndPoints(t, data, map[string]utils.StringSet{
		mainName:   addresses(data.Consul[0]),
		branchName: addresses(data.Consul[1]),
	})
}

func TestGetEndPoints_CanaryOnePercent(t *testing.T) {
	data := test.CanaryOnePercent()
	mainName := join(data.Shiva[0].Map.Name, data.Shiva[0].Map.Provides[0].Name)
	branchName := join(data.Shiva[1].Map.Name, "canary", data.Shiva[1].Map.Provides[0].Name)
	testGetEndPoints(t, data, map[string]utils.StringSet{
		mainName:   addresses(data.Consul[0]),
		branchName: addresses(data.Consul[1]),
	})
}

func TestGetEndPoints_BranchWithTraffic(t *testing.T) {
	data := test.BranchWithTraffic()
	mainName := join(data.Shiva[0].Map.Name, data.Shiva[0].Map.Provides[0].Name)
	branch := data.Shiva[1]
	branchName := join(branch.Map.Name, branch.Deployment.Branch, branch.Map.Provides[0].Name)
	testGetEndPoints(t, data, map[string]utils.StringSet{
		mainName:   joinAddresses(addresses(data.Consul[0]), addresses(data.Consul[1])),
		branchName: addresses(data.Consul[1]),
	})
}

func TestGetEndPoints_Canary(t *testing.T) {
	data := test.Canary()
	mainName := join(data.Shiva[0].Map.Name, data.Shiva[0].Map.Provides[0].Name)
	branch := data.Shiva[1]
	branchName := join(branch.Map.Name, "canary", branch.Map.Provides[0].Name)
	testGetEndPoints(t, data, map[string]utils.StringSet{
		mainName:   joinAddresses(addresses(data.Consul[0]), addresses(data.Consul[1])),
		branchName: addresses(data.Consul[1]),
	})
}

func TestGetEndPoints_Branches(t *testing.T) {
	data := test.Branches()

	main := data.Shiva[0]
	bOffTraffic := data.Shiva[1]
	bOnePercent := data.Shiva[2]
	bTraffic := data.Shiva[3]

	mainName := join(main.Map.Name, main.Map.Provides[0].Name)
	bOffTrafficName := join(bOffTraffic.Map.Name, bOffTraffic.Deployment.Branch, bOnePercent.Map.Provides[0].Name)
	bOnePercentName := join(bOnePercent.Map.Name, bOnePercent.Deployment.Branch, bOnePercent.Map.Provides[0].Name)
	bTrafficName := join(bTraffic.Map.Name, bTraffic.Deployment.Branch, bTraffic.Map.Provides[0].Name)

	testGetEndPoints(t, data, map[string]utils.StringSet{
		mainName:        joinAddresses(addresses(data.Consul[0]), addresses(data.Consul[3])),
		bOffTrafficName: addresses(data.Consul[1]),
		bOnePercentName: addresses(data.Consul[2]),
		bTrafficName:    addresses(data.Consul[3]),
	})
}

func TestGetEndPoints_BranchesProvides(t *testing.T) {
	data := test.BranchesProvides()

	main := data.Shiva[0]
	bOffTraffic := data.Shiva[1]
	bOnePercent := data.Shiva[2]
	bTraffic := data.Shiva[3]

	mainName1 := join(main.Map.Name, main.Map.Provides[0].Name)
	bOffTrafficName1 := join(bOffTraffic.Map.Name, bOffTraffic.Deployment.Branch, bOnePercent.Map.Provides[0].Name)
	bOnePercentName1 := join(bOnePercent.Map.Name, bOnePercent.Deployment.Branch, bOnePercent.Map.Provides[0].Name)
	bTrafficName1 := join(bTraffic.Map.Name, bTraffic.Deployment.Branch, bTraffic.Map.Provides[0].Name)

	mainName2 := join(main.Map.Name, main.Map.Provides[1].Name)
	bOffTrafficName2 := join(bOffTraffic.Map.Name, bOffTraffic.Deployment.Branch, bOnePercent.Map.Provides[1].Name)
	bOnePercentName2 := join(bOnePercent.Map.Name, bOnePercent.Deployment.Branch, bOnePercent.Map.Provides[1].Name)
	bTrafficName2 := join(bTraffic.Map.Name, bTraffic.Deployment.Branch, bTraffic.Map.Provides[1].Name)

	testGetEndPoints(t, data, map[string]utils.StringSet{
		mainName1:        joinAddresses(addresses(data.Consul[0]), addresses(data.Consul[6])),
		mainName2:        joinAddresses(addresses(data.Consul[1]), addresses(data.Consul[7])),
		bOffTrafficName1: addresses(data.Consul[2]),
		bOffTrafficName2: addresses(data.Consul[3]),
		bOnePercentName1: addresses(data.Consul[4]),
		bOnePercentName2: addresses(data.Consul[5]),
		bTrafficName1:    addresses(data.Consul[6]),
		bTrafficName2:    addresses(data.Consul[7]),
	})
}

func joinAddresses(sets ...utils.StringSet) utils.StringSet {
	result := utils.NewStringSet()
	for _, addresses := range sets {
		for adr := range addresses {
			result[adr] = struct{}{}
		}
	}
	return result
}

func addresses(ci *consul.ServiceInfo) utils.StringSet {
	result := utils.NewStringSet()
	for _, node := range ci.Nodes {
		result.Add(node.ServiceIP)
	}
	return result
}

func testGetEndPoints(t *testing.T, data *test.Data, expected map[string]utils.StringSet) {
	viper.SetDefault("dc", "sas")
	test.Init(t)
	shivaMock := mock.NewShivaMock(data.Shiva)
	consulMock := mock.NewConsulMock(data.Consul, nil)
	log := logger.Logger.WithField("test", "test")

	infoSvc := info.RunService(info.NewConf(), shivaMock, consulMock, log)
	s := (&snapshotBuilder{
		Datacenter:        "sas",
		Defaults:          defaultSettings,
		ClusterData:       infoSvc.Info().Clusters,
		svc:               mock.NewConsulMock(nil, nil),
		log:               log,
	}).Build()
	result := parseSnapshot(t, s)

	actual := make(map[string]utils.StringSet)
	for resourceName := range expected {
		if la, ok := result.Endpoints[resourceName]; ok {
			epSet := utils.NewStringSet()
			for _, es := range la.Endpoints {
				for _, e := range es.LbEndpoints {
					epSet.Add(e.GetEndpoint().GetAddress().GetSocketAddress().GetAddress())
				}
			}
			actual[resourceName] = epSet
		}
	}
	assert.Equal(t, expected, actual)
}
