package conductor

import (
	"github.com/spf13/viper"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestCheckNodeExistsInConductor(t *testing.T) {
	s := NewService()
	viper.AutomaticEnv()

	nodeName := "spooner-01-dev.sas.yp-c.yandex.net"
	conductorToken := viper.GetString("CONDUCTOR_OAUTH_TOKEN")

	exists, err := s.CheckNodeExistsInConductor(nodeName, conductorToken)
	if err != nil {
		t.Error(err)
	}
	assert.True(t, exists, "that should be true")
}

func TestCheckNodeNotExistsInConductor(t *testing.T) {
	s := NewService()
	viper.AutomaticEnv()
	nodeName := "spooner-0111-dev.sas.yp-c.yandex.net"
	conductorToken := viper.GetString("CONDUCTOR_OAUTH_TOKEN")

	exists, err := s.CheckNodeExistsInConductor(nodeName, conductorToken)
	if err != nil {
		t.Error(err)
	}
	assert.False(t, exists, "that should be false")
}

func TestAddNode(t *testing.T) {
	s := NewService()
	viper.AutomaticEnv()
	nodeName := "spooner-0111-dev.sas.yp-c.yandex.net"
	conductorToken := viper.GetString("CONDUCTOR_OAUTH_TOKEN")
	conductorTag := "autotest"

	err := s.AddNode(conductorToken, nodeName, "sas", "vertis_parking", conductorTag)
	if err != nil {
		t.Error(err)
	}
	t.Cleanup(func() {
		err := s.DeleteNode(nodeName, conductorToken)
		if err != nil {
			t.Error(err)
		}
	})
}

func TestIsTagExistOnHost(t *testing.T) {
	s := NewService()
	viper.AutomaticEnv()
	nodeName := "spooner-01-dev.sas.yp-c.yandex.net"
	conductorToken := viper.GetString("CONDUCTOR_OAUTH_TOKEN")
	conductorTag := "autoenv=dev"

	exists, err := s.IsTagExistOnHost(conductorToken, nodeName, conductorTag)
	if err != nil {
		t.Error(err)
	}
	assert.True(t, exists, "that should be true")

}
