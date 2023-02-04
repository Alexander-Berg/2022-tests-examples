package session

import (
	"github.com/YandexClassifieds/goLB/pb/kikimr/persqueue"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestIsInitRetryable(t *testing.T) {
	assert.True(t, IsInitRetryable(&persqueue.Error{
		Code: persqueue.EErrorCode_INITIALIZING,
		Description: "clusters list or local cluster is empty",
	}))
	assert.True(t, IsInitRetryable(&persqueue.Error{
		Code: persqueue.EErrorCode_ERROR,
		Description: "status is not ok: partition 252 is not ready",
	}))
	assert.True(t, IsInitRetryable(&persqueue.Error{
		Code: persqueue.EErrorCode_ERROR,
		Description: "status is not ok: tablet is not ready",
	}))
	assert.False(t, IsInitRetryable(&persqueue.Error{
		Code:        persqueue.EErrorCode_BAD_REQUEST,
		Description: "any desc",
	}))
}
