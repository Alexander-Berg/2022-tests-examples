package logrus_test

import (
	"testing"

	"github.com/YandexClassifieds/go-common/log"
	"github.com/YandexClassifieds/go-common/log/logrus"
	dto "github.com/prometheus/client_model/go"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestMetricHook_Fire(t *testing.T) {
	logger := logrus.NewLogger(logrus.WithMetrics())
	testFunc1(logger)

	metricCount := getMetricCount(t, "testCon", "testFunc2", "testReason")

	assert.Equal(t, 2, metricCount)
}

func TestMetricHook_FireWrongType(t *testing.T) {
	logger := logrus.NewLogger(logrus.WithMetrics())
	logger.WithFields(log.Fields{
		"key": "val",
		"reason": struct {
			x int
		}{},
	}).Warn()

	metricCount := getMetricCount(t, "main", "TestMetricHook_Fire2", "")
	assert.Equal(t, 0, metricCount)
}

func TestMetricHook_FireEmptyFields(t *testing.T) {
	logger := logrus.NewLogger(logrus.WithMetrics())
	logger.Warn()

	metricCount := getMetricCount(t, "main", "TestMetricHook_FireEmptyFields", "")
	assert.Equal(t, 1, metricCount)
}

func TestMetricHook_NotFire(t *testing.T) {
	var fields = log.Fields{
		log.Context: "testCon1",
		log.Reason:  "testReason1",
	}
	logger := logrus.NewLogger(logrus.WithMetrics())

	logger.WithFields(fields).Error("Test")
	logger.WithFields(fields).Info("Test1")

	metricCount := getMetricCount(t, "testCon1", "TestMetricHook_NotFire", "testReason1")

	assert.Equal(t, 0, metricCount)
}

func getMetricCount(t *testing.T, lvs ...string) int {
	c, err := logrus.WarnCounter.GetMetricWithLabelValues(lvs...)
	require.NoError(t, err)

	met := &dto.Metric{}
	err = c.Write(met)
	require.NoError(t, err)

	return int(*met.Counter.Value)
}

func testFunc1(log log.Logger) {
	testFunc2(log)
}

func testFunc2(logger log.Logger) {
	var fields = log.Fields{
		log.Context: "testCon",
		log.Reason:  "testReason",
	}

	logger.WithFields(fields).Warn("Test")
	logger.WithFields(fields).Warnf("Test1")
}
