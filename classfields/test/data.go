package test

import (
	"fmt"
	"strconv"
	"strings"
	"sync/atomic"

	"github.com/YandexClassifieds/envoy-api/consul"
	"github.com/YandexClassifieds/envoy-api/pb/shiva/service_map"
	"github.com/YandexClassifieds/envoy-api/pb/shiva/types/deployment"
	"github.com/YandexClassifieds/envoy-api/pb/shiva/types/dtype"
	"github.com/YandexClassifieds/envoy-api/pb/shiva/types/info"
	"github.com/YandexClassifieds/envoy-api/pb/shiva/types/layer"
	"github.com/YandexClassifieds/envoy-api/pb/shiva/types/state"
	"github.com/YandexClassifieds/envoy-api/pb/shiva/types/traffic"
	"github.com/YandexClassifieds/envoy-api/utils"
)

const (
	noShiva              = "noshiva"
	sasDC                = "sas"
	mytDC                = "myt"
	vlaDC                = "vla"
	api1                 = "api1"
	api2                 = "api2"
	LegacyClusterPostfix = "-test-int_slb_vertis_yandex_net"
)

var (
	ip        *int64
	nameIndex *int64
)

func init() {
	ipInt := int64(0)
	ip = &ipInt

	nameIndexInt := int64(0)
	nameIndex = &nameIndexInt
}

type Data struct {
	Shiva  []*info.DeploymentInfo
	Consul []*consul.ServiceInfo
}

func Empty() *Data {
	return &Data{
		Shiva:  []*info.DeploymentInfo{},
		Consul: []*consul.ServiceInfo{},
	}
}

func Old() *Data {
	return &Data{
		Shiva:  []*info.DeploymentInfo{},
		Consul: []*consul.ServiceInfo{consulInfo("old")},
	}
}

func Olds() *Data {
	return &Data{
		Shiva: []*info.DeploymentInfo{},
		Consul: []*consul.ServiceInfo{
			consulInfo(name("old")),
			consulInfo(name("old")),
			consulInfo(name("old")),
		},
	}
}

func OldsCommonDomain() *Data {
	si1 := consulInfo(name("old"))
	si2 := consulInfo(name("old"))
	common := name("old")
	commonSi := consulInfo(common)
	commonDomain := oldTagDomain(common, api1, layer.Layer_TEST)
	si1.Tags.Add(commonDomain)
	si2.Tags.Add(commonDomain)
	commonSi.Nodes = []consul.ServiceNode{}
	commonSi.Nodes = append(commonSi.Nodes, si1.Nodes...)
	commonSi.Nodes = append(commonSi.Nodes, si2.Nodes...)
	return &Data{
		Shiva:  []*info.DeploymentInfo{},
		Consul: []*consul.ServiceInfo{si1, si2, commonSi},
	}
}

func EmptyProvides() *Data {
	return &Data{
		Shiva: []*info.DeploymentInfo{
			shivaInfo(name("new"), "", traffic.Traffic_UNKNOWN),
		},
		Consul: []*consul.ServiceInfo{},
	}
}

func Simple() *Data {
	d := &Data{
		Shiva: []*info.DeploymentInfo{
			shivaInfo(name("new"), "", traffic.Traffic_UNKNOWN, api1),
		},
		Consul: []*consul.ServiceInfo{},
	}
	prepareConsul(d)
	return d
}

func Simples() *Data {
	d := &Data{
		Shiva: []*info.DeploymentInfo{
			shivaInfo(name("new"), "", traffic.Traffic_UNKNOWN, api1),
			shivaInfo(name("new"), "", traffic.Traffic_UNKNOWN, api1),
			shivaInfo(name("new"), "", traffic.Traffic_UNKNOWN, api1),
		},
		Consul: []*consul.ServiceInfo{},
	}
	prepareConsul(d)
	return d
}

func prepareConsul(d *Data) {
	for _, si := range d.Shiva {
		for _, p := range si.Map.Provides {
			d.Consul = append(d.Consul, shivaConsulInfo(si, p))
		}
	}
}

func SimpleGrpc() *Data {
	si := shivaInfo(name("new"), "", traffic.Traffic_UNKNOWN, api1)
	si.Map.Provides[0].Protocol = service_map.ServiceProvides_grpc
	d := &Data{
		Shiva: []*info.DeploymentInfo{
			si,
		},
		Consul: []*consul.ServiceInfo{},
	}
	prepareConsul(d)
	return d
}

func SimpleTcp() *Data {
	si := shivaInfo(name("new"), "", traffic.Traffic_UNKNOWN, api1)
	si.Map.Provides[0].Protocol = service_map.ServiceProvides_tcp
	d := &Data{
		Shiva: []*info.DeploymentInfo{
			si,
		},
		Consul: []*consul.ServiceInfo{},
	}
	prepareConsul(d)
	return d
}

func OnlyBranch() *Data {
	d := &Data{
		Shiva: []*info.DeploymentInfo{
			shivaInfo(name("new"), "br", traffic.Traffic_UNKNOWN, api1),
		},
		Consul: []*consul.ServiceInfo{},
	}
	prepareConsul(d)
	return d
}

func OldAddress() *Data {
	si := shivaInfo(name("new"), "", traffic.Traffic_UNKNOWN, api1)
	si.Map.Provides[0].OldAddressTest = oldDomain("old-addr", api2, layer.Layer_TEST)
	d := &Data{
		Shiva: []*info.DeploymentInfo{
			si,
		},
		Consul: []*consul.ServiceInfo{},
	}
	prepareConsul(d)
	return d
}

func WithOld() *Data {
	name := name("new-old")
	deploymentInfo := shivaInfo(name, "", traffic.Traffic_UNKNOWN, api1)
	serviceInfo := consulInfo(name + "-collision")
	shivaDomain := domain(deploymentInfo.Map.Name, deploymentInfo.Map.Provides[0].Name, layer.Layer_TEST)
	serviceInfo.Tags["domain-"+shivaDomain] = struct{}{}
	serviceInfo.HttpDomains.Add(shivaDomain)
	d := &Data{
		Shiva: []*info.DeploymentInfo{
			deploymentInfo,
		},
		Consul: []*consul.ServiceInfo{serviceInfo},
	}
	prepareConsul(d)
	return d
}

func WithOldAddressAndOld() *Data {
	si := shivaInfo(name("new"), "", traffic.Traffic_UNKNOWN, api1)
	ci := consulInfo(name("old"))
	for d := range ci.HttpDomains {
		si.Map.Provides[0].OldAddressTest = d
		break
	}
	d := &Data{
		Shiva:  []*info.DeploymentInfo{si},
		Consul: []*consul.ServiceInfo{ci},
	}
	prepareConsul(d)
	return d
}

func BranchWithoutTraffic() *Data {
	name := name("new")
	d := &Data{
		Shiva: []*info.DeploymentInfo{
			shivaInfo(name, "", traffic.Traffic_UNKNOWN, api1),
			shivaInfo(name, "br", traffic.Traffic_UNKNOWN, api1),
		},
		Consul: []*consul.ServiceInfo{},
	}
	prepareConsul(d)
	return d
}

func BranchOnePercent() *Data {
	name := name("new")
	d := &Data{
		Shiva: []*info.DeploymentInfo{
			shivaInfo(name, "", traffic.Traffic_UNKNOWN, api1),
			shivaInfo(name, "br", traffic.Traffic_ONE_PERCENT, api1),
		},
		Consul: []*consul.ServiceInfo{},
	}
	prepareConsul(d)
	return d
}

func BranchWithTraffic() *Data {
	name := name("new")
	d := &Data{
		Shiva: []*info.DeploymentInfo{
			shivaInfo(name, "", traffic.Traffic_UNKNOWN, api1),
			shivaInfo(name, "br", traffic.Traffic_REAL, api1),
		},
		Consul: []*consul.ServiceInfo{},
	}
	prepareConsul(d)
	return d
}

func Branches() *Data {
	name := name("new")
	d := &Data{
		Shiva: []*info.DeploymentInfo{
			shivaInfo(name, "", traffic.Traffic_UNKNOWN, api1),
			shivaInfo(name, "b1", traffic.Traffic_UNKNOWN, api1),
			shivaInfo(name, "b2", traffic.Traffic_ONE_PERCENT, api1),
			shivaInfo(name, "b3", traffic.Traffic_REAL, api1),
		},
		Consul: []*consul.ServiceInfo{},
	}
	prepareConsul(d)
	return d
}

func CanaryOnePercent() *Data {
	name := name("new")
	canary := shivaInfo(name, "", traffic.Traffic_ONE_PERCENT, api1)
	canary.Deployment.State = state.DeploymentState_CANARY_ONE_PERCENT
	d := &Data{
		Shiva: []*info.DeploymentInfo{
			shivaInfo(name, "", traffic.Traffic_UNKNOWN, api1),
			canary,
		},
		Consul: []*consul.ServiceInfo{},
	}
	prepareConsul(d)
	return d
}

func Canary() *Data {
	name := name("new")
	canary := shivaInfo(name, "", traffic.Traffic_REAL, api1)
	canary.Deployment.State = state.DeploymentState_CANARY
	d := &Data{
		Shiva: []*info.DeploymentInfo{
			shivaInfo(name, "", traffic.Traffic_UNKNOWN, api1),
			canary,
		},
		Consul: []*consul.ServiceInfo{},
	}
	prepareConsul(d)
	return d
}

func BranchesProvides() *Data {
	name := name("new")
	d := &Data{
		Shiva: []*info.DeploymentInfo{
			shivaInfo(name, "", traffic.Traffic_UNKNOWN, api1, api2),
			shivaInfo(name, "b1", traffic.Traffic_UNKNOWN, api1, api2),
			shivaInfo(name, "b2", traffic.Traffic_ONE_PERCENT, api1, api2),
			shivaInfo(name, "b3", traffic.Traffic_REAL, api1, api2),
		},
		Consul: []*consul.ServiceInfo{},
	}
	prepareConsul(d)
	return d
}

func shivaInfo(name, branch string, tr traffic.Traffic, ps ...string) *info.DeploymentInfo {

	result := &info.DeploymentInfo{
		Deployment: &deployment.Deployment{
			ServiceName: name,
			Version:     "0.1.1",
			Branch:      branch,
			Type:        dtype.DeploymentType_RUN,
			Layer:       layer.Layer_TEST,
			State:       state.DeploymentState_SUCCESS,
			Traffic:     tr,
		},
		Map: &service_map.ServiceMap{
			Name:     name,
			Type:     service_map.ServiceType_service,
			Provides: []*service_map.ServiceProvides{},
		},
		BalancerInfo: balancerInfo(tr),
	}
	for _, p := range ps {
		result.Map.Provides = append(result.Map.Provides, provides(p))
	}
	return result
}

func provides(name string) *service_map.ServiceProvides {
	return &service_map.ServiceProvides{
		Name:     name,
		Protocol: service_map.ServiceProvides_http,
		Port:     80,
	}
}

func balancerInfo(tr traffic.Traffic) *info.BalancerInfo {
	return &info.BalancerInfo{
		// not used
		Domains: []string{},
		// not used
		BranchDomains: []string{},
		// not used (see nginx)
		BranchUIDSuffix: 0,
		Traffic:         tr,
	}
}

func shivaConsulInfo(d *info.DeploymentInfo, p *service_map.ServiceProvides) *consul.ServiceInfo {
	branch := d.Deployment.Branch
	if d.Deployment.State == state.DeploymentState_CANARY || d.Deployment.State == state.DeploymentState_CANARY_ONE_PERCENT {
		branch = "canary"
	}
	httpDomains := utils.NewStringSet()
	tags := utils.StringSet{
		"service=" + d.Map.Name:           {},
		"version=" + d.Deployment.Version: {},
		"provides=" + p.Name:              {},
	}
	var name string
	if branch != "" {
		tags["branch="+branch] = struct{}{}
		name = d.Map.Name + "-" + branch
	} else {
		name = d.Map.Name
	}
	tags[tagDomain(name, p.Name, layer.Layer_TEST)] = struct{}{}
	tags["canary="+strconv.FormatBool(d.Deployment.State == state.DeploymentState_SUCCESS)] = struct{}{}
	httpDomains.Add(domain(name, p.Name, layer.Layer_TEST))
	if p.OldAddressTest != "" {
		tags[p.OldAddressTest] = struct{}{}
		httpDomains.Add(p.OldAddressTest)
	}
	return &consul.ServiceInfo{
		Name:        name + "-" + p.Name,
		HttpDomains: httpDomains,
		Tags:        tags,
		Nodes:       serviceNodes(80, sasDC, vlaDC),
	}
}

func consulInfo(name string) *consul.ServiceInfo {
	httpDomains := utils.NewStringSet()
	httpDomains.Add(oldDomain(name, api1, layer.Layer_TEST))
	return &consul.ServiceInfo{
		Name:        name + "-" + api1,
		HttpDomains: httpDomains,
		Nodes:       serviceNodes(80, sasDC, vlaDC),
		Tags:        utils.StringSet{noShiva: {}, oldTagDomain(name, api1, layer.Layer_TEST): {}},
	}
}

func serviceNodes(port int, dcs ...string) []consul.ServiceNode {
	result := make([]consul.ServiceNode, 0)
	for _, dc := range dcs {
		result = append(result, serviceNode(port, dc))
		result = append(result, serviceNode(port, dc))
	}
	return result
}

func serviceNode(port int, dc string) consul.ServiceNode {
	return consul.ServiceNode{
		DataCenter:  dc,
		ServiceIP:   serviceIP(),
		ServicePort: port,
	}
}

func oldDomain(service, api string, layer layer.Layer) string {
	layerStr := strings.ToLower(layer.String())
	return fmt.Sprintf("%s-%s-%s-int.slb.vertis.yandex.net", service, api, layerStr)
}

func oldTagDomain(service, api string, layer layer.Layer) string {
	layerStr := strings.ToLower(layer.String())
	return fmt.Sprintf("domain-%s-%s-%s-int.slb.vertis.yandex.net", service, api, layerStr)
}

func domain(service, api string, layer layer.Layer) string {
	layerStr := strings.ToLower(layer.String())
	return fmt.Sprintf("%s-%s.vrts-slb.%s.vertis.yandex.net", service, api, layerStr)
}

func tagDomain(service, api string, layer layer.Layer) string {
	layerStr := strings.ToLower(layer.String())
	return fmt.Sprintf("domain-%s-%s.vrts-slb.%s.vertis.yandex.net", service, api, layerStr)
}

func serviceIP() string {
	newIP := atomic.AddInt64(ip, 1)
	return "::1" + strconv.FormatInt(newIP, 10)
}

func name(name string) string {

	index := atomic.AddInt64(nameIndex, 1)
	indexStr := strconv.FormatInt(index, 10)
	return name + indexStr
}
