package main

import (
	"github.com/YandexClassifieds/sub-zero/golp"
	dockerTypes "github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/network"
	"github.com/stretchr/testify/assert"
	"testing"
)

type networkMode struct {
	NetworkMode string `json:",omitempty"`
}

func TestCreatePostToGolp(t *testing.T) {

	hostname := "docker-01-sas.test.vertis.yandex.net"

	containers := []dockerTypes.Container{
		//Container with net=host
		{
			ID:    "container1",
			Names: []string{"container1nameA"},
			HostConfig: networkMode{
				NetworkMode: "host",
			},
		},
		// Container with net=bridge and valid ipv6 addr
		{
			ID:    "container2",
			Names: []string{"/container2nameA-d44be319-7014-bd8c-afa8-c689a34848d1"},
			HostConfig: networkMode{
				NetworkMode: "bridge",
			},
			NetworkSettings: &dockerTypes.SummaryNetworkSettings{
				Networks: map[string]*network.EndpointSettings{
					"bridge": {GlobalIPv6Address: "2a02:6b8:c02:576::1459:c778:e479"},
				},
			},
		},
		// Container with net=bridge and with ipv4 and ipv6
		{
			ID:    "container3",
			Names: []string{"container3nameA"},
			HostConfig: networkMode{
				NetworkMode: "bridge",
			},
			NetworkSettings: &dockerTypes.SummaryNetworkSettings{
				Networks: map[string]*network.EndpointSettings{
					"docker0": {IPAddress: "172.0.0.5"},
					"bridge":  {GlobalIPv6Address: "2a02:6b8:c02:55b::1459:da69:595e"},
				},
			},
		},
		// Container with net=vertis and valid ipv6 addr
		{
			ID:    "container4",
			Names: []string{"/container4nameA"},
			HostConfig: networkMode{
				NetworkMode: "vertis",
			},
			NetworkSettings: &dockerTypes.SummaryNetworkSettings{
				Networks: map[string]*network.EndpointSettings{
					"vertis": {GlobalIPv6Address: "2a02:6b8:c23:1828:0:1459:f:e"},
				},
			},
		},
	}

	containersData := golp.CreatePostToGolp(containers, hostname)

	assert.Equal(t, len(containersData), 3)

	assert.Equal(t, "container2nameA", containersData[0].ServiceName)
	assert.Equal(t, "2a02:6b8:c02:576::1459:c778:e479", containersData[0].ContainerIp)

	assert.Equal(t, "container3nameA", containersData[1].ServiceName)
	assert.Equal(t, "2a02:6b8:c02:55b::1459:da69:595e", containersData[1].ContainerIp)

	assert.Equal(t, "container4nameA", containersData[2].ServiceName)
	assert.Equal(t, "2a02:6b8:c23:1828:0:1459:f:e", containersData[2].ContainerIp)

}
