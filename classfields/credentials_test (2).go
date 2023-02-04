package srvticket

import (
	"context"
	"errors"
	"github.com/YandexClassifieds/shiva/test/mock/tvm"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestAuthAccess_GetRequestMetadata(t *testing.T) {
	mockTvm := &tvm.TvmClient{}
	creds := newAuthAccess(mockTvm, 1, 2)
	mockTvm.On("ServiceTicket", 1, 2).Return("testing-ticket", nil)
	authData, err := creds.GetRequestMetadata(context.Background())
	if !assert.NoError(t, err) {
		return
	}
	assert.Equal(t, "testing-ticket", authData["x-ya-service-ticket"])

}

func TestAuthAccess_GetRequestMetadata_Err(t *testing.T) {
	mockTvm := &tvm.TvmClient{}
	creds := newAuthAccess(mockTvm, 1, 2)
	mockTvm.On("ServiceTicket", 1, 2).Return("", errors.New("dummy err"))
	_, err := creds.GetRequestMetadata(context.Background())
	assert.Error(t, err)
}
