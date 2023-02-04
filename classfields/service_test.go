package main

import (
	"github.com/YandexClassifieds/auto-scaling/pkg/config"
	prometheus_metrics "github.com/YandexClassifieds/auto-scaling/pkg/prometheus-metrics"
	yandex_monitoring "github.com/YandexClassifieds/auto-scaling/pkg/yandex-monitoring"
	vlog "github.com/YandexClassifieds/go-common/i/log"
	vlogrus "github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/spf13/viper"
	"testing"
)

func TestService(t *testing.T) {
	viper.AutomaticEnv()
	var logger vlog.Logger
	logger = vlogrus.New()
	logger.Info("Start transfer of metrics...")

	env, jsonConfig, err := config.NewConfig()
	if err != nil {
		t.Error(err)
	}
	prometheusConfig := prometheus_metrics.NewPrometheusConfig(env, logger)
	yandexMonitoringConfig, err := yandex_monitoring.NewYandexMonitoringConfig(env, logger)
	if err != nil {
		t.Errorf("Error while getting config: %s", err)
	}
	iamToken, err := yandexMonitoringConfig.GetIAMToken()
	if err != nil {
		t.Errorf("Error during getting IAMToken: %s", err)
	}
	ymPostConfig := yandexMonitoringConfig.NewYMPostConfig(iamToken)
	_, err = yandexMonitoringConfig.GetInstanceGroupsInfo(iamToken)
	if err != nil {
		t.Errorf("Error while getting information about IG: %s", err)
	}
	for _, metric := range jsonConfig.Metrics {
		_, err := prometheusConfig.GetPrometheusMetrics(metric.PrometheusMetricName)
		if err != nil {
			t.Errorf("Error while getting prometheus metric: %s", err)
		}
	}
	err = yandexMonitoringConfig.PushMetric(ymPostConfig, "test_metric", 100.0, "ru-central1-c", 10)
	if err != nil {
		t.Error(err)
	}
}
