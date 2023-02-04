package juggler

import (
	"github.com/spf13/viper"
	"testing"
	"time"
)

func TestAddDowntime(t *testing.T) {
	s := NewService()
	viper.AutomaticEnv()
	host := "kasev-01-dev.sas.yp-c.yandex.net"
	jugglerToken := viper.GetString("JUGGLER_OAUTH_TOKEN")
	dtDuration := 1 * time.Minute

	err := s.AddDowntime(host, jugglerToken, dtDuration)
	if err != nil {
		t.Error(err)
	}

}
