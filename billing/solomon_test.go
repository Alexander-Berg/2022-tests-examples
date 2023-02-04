package core

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func TestMetricEquality(t *testing.T) {
	metric := Metric{
		Labels: SolomonLabels{"a": "a", "b": "b"},
		Value:  nil,
	}

	require.True(t, metric.SameMetric(SolomonLabels{"a": "a", "b": "b"}))
	require.False(t, metric.SameMetric(SolomonLabels{"a": "a"}))
	require.False(t, metric.SameMetric(SolomonLabels{"a": "a", "c": "c"}))
}

func TestSolomonContainMetric(t *testing.T) {
	ts := time.Now()
	data := NewSolomonData(ts)

	labels := SolomonLabels{"a": "a", "b": "b"}
	data.AddMetric(0, labels)
	require.True(t, data.ContainMetric(labels))
	require.False(t, data.ContainMetric(SolomonLabels{"a": "a"}))

	AddRetardation(data, "test", ts, time.Now())
	require.True(t, data.ContainMetric(SolomonLabels{"sensor": "retardation", "status": "test"}))

	AddRetardation(data, "test_zero", ts, time.Time{})
	require.True(t, data.ContainMetric(SolomonLabels{"sensor": "retardation", "status": "test_zero"}))
	require.EqualValues(t, 0, data.Metrics[2].Value)
}
