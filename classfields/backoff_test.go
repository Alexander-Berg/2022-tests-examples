package retry

// Copied from https://github.com/yandex-cloud/go-sdk/tree/master/pkg/retry

import (
	"math"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

const (
	defaultLinearBackoffTimeout   = 50 * time.Millisecond
	defaultLinearBackoffJitter    = 0.1
	defaultExponentialBackoffBase = 50 * time.Millisecond
)

func TestBackoffLinearWithJitter(t *testing.T) {
	dto := float64(defaultLinearBackoffTimeout)
	toMin := time.Duration(dto * (1 - defaultLinearBackoffJitter))
	toMax := time.Duration(dto * (1 + defaultLinearBackoffJitter))

	backoff := BackoffLinearWithJitter(defaultLinearBackoffTimeout, defaultLinearBackoffJitter)

	for attempt := 0; attempt < 1000; attempt++ {
		to := backoff(attempt)
		res := to <= toMax && to >= toMin
		require.True(t, res)
	}
}

func TestBackoffExponentialWithJitter(t *testing.T) {
	maxBackoffTo := 30 * time.Second
	backoff := BackoffExponentialWithJitter(defaultExponentialBackoffBase, maxBackoffTo)

	for attempt := 0; attempt <= 10; attempt++ {
		to := backoff(attempt)
		t.Logf("Attempt: %v, Backoff: %s", attempt, to)
		maxTo := time.Duration(math.Pow(3, float64(attempt)) * float64(defaultExponentialBackoffBase))
		require.True(t, to <= maxTo, "to: %v,maxTo: %v", to, maxTo)
	}

	for attempt := 20; attempt < 1000; attempt++ {
		to := backoff(attempt)
		require.True(t, to <= maxBackoffTo, "to: %v, maxBackoffTo: %v", to, maxBackoffTo)
	}
}
