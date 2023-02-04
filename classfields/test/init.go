package test

import (
	"github.com/YandexClassifieds/envoy-api/common/logger"
	"github.com/YandexClassifieds/envoy-api/consul"
	"github.com/YandexClassifieds/envoy-api/utils"
	"github.com/spf13/viper"
	"os"
	"testing"
	"time"
)

func init() {
	viper.SetDefault("envoy-api.environment", "testing")
	viper.SetDefault("consul_address", "localhost:18500")
	viper.SetDefault("consul_fetch_workers", 20)
	viper.SetDefault("dc", "dc1")
	viper.AutomaticEnv()
}

func Init(t testing.TB) {
	_ = os.Remove("/tmp/consul_balancer_names_cache")
	_ = os.Remove("/tmp/consul_balancer_settings")
	_ = os.Remove("/tmp/consul_default_balancer_settings")
	_ = os.Remove("/tmp/consul_service_info_cache")
	_ = os.Remove("/tmp/shiva_statuses_info_cache_2")
	utils.EnvoyApiDatacenter = viper.GetString("dc")
	logger.Init("/dev/null", "")
}

func ConsulSvc(t testing.TB) *consul.Service {
	t.Helper()
	svc, err := consul.NewService(consul.Config{
		Address:     viper.GetString("consul_address"),
		Datacenters: []string{"dc1"},
		CachePath:   "/tmp",
	}, logger.Logger)
	if err != nil {
		t.Fatal(err)
	}
	svc.Init()
	time.Sleep(time.Second / 5)
	return svc
}
