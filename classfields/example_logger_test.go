package logrus_test

import (
	"fmt"
	"net/http"

	vConf "github.com/YandexClassifieds/go-common/conf/viper"
	vlog "github.com/YandexClassifieds/go-common/log"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

func Example_clean_logger() {
	// Init
	log := logrus.NewLogger()
	// Usage
	log.WithField("key", "value").Infof("message")
}

func Example_logger_with_sentry_and_level() {
	// Init
	conf := vConf.NewConf("")
	log := logrus.NewLogger(logrus.WithLevel("debug"), logrus.WithSentryFromConf(conf))
	// Usage
	err := fmt.Errorf("test error")
	log.WithError(err).Error("alert to sentry")
}

func Example_logger_with_metrics() {
	// Init
	log := logrus.NewLogger(logrus.WithMetrics())
	http.Handle("/metrics", promhttp.Handler())
	if err := http.ListenAndServe(":2112", nil); err != nil {
		log.WithError(err).WithField(vlog.Context, "init logger with metrics").WithField(vlog.Reason, "http fail")
	}
	// Usage
	log.WithField(vlog.Context, "service_name").WithField(vlog.Reason, "warn_label").Warnf("message")
}

func Example_logger_with_sentry_and_metrics_and_level() {
	// Init
	conf := vConf.NewConf("")
	log := logrus.NewLogrusLogger(logrus.WithMetrics(), logrus.WithSentryFromConf(conf), logrus.WithLevel("debug"))
	http.Handle("/metrics", promhttp.Handler())
	if err := http.ListenAndServe(":2112", nil); err != nil {
		log.WithError(err).WithField(vlog.Context, "init logger with metrics").WithField(vlog.Reason, "http fail")
	}
	// Usage
	localLog := log.WithField(vlog.Context, "service_name")
	localLog.WithField(vlog.Reason, "warn_label").Warnf("message")
	localLog.WithError(fmt.Errorf("test_error")).Error("alert to sentry")
}
