package handlers

import (
	"context"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/accounts/mock/actionsmock"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	coreerrors "a.yandex-team.ru/billing/hot/accounts/pkg/core/errors"
)

func TestGetStateResponse(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	attr1 := "v1"
	attr2 := "v2"
	mockActions.EXPECT().GetState(
		gomock.Eq(ctx),
		gomock.Eq(entities.LocationAttributes{
			Namespace: "s",
			Type:      "t",
			Attributes: map[string]*string{
				"a1": &attr1,
				"a2": &attr2,
			},
		})).Return(
		&entities.State{
			Loc:   entities.Location{},
			State: []byte(`[{"some": "weird"}, "data"]`),
		},
		nil)

	req := httptest.NewRequest("GET", "http://localhost:9000?namespace=s&type=t&a1=v1&a2=v2", strings.NewReader(""))
	resp := httptest.NewRecorder()
	err := GetState(ctx, resp, req, mockActions)
	if err != nil {
		t.Fatal(err)
	}

	assert.Equal(t, resp.Code, 200)
	assert.Equal(t, resp.Body.String(), `{"status":"ok","data":[{"some":"weird"},"data"]}`)
}

func TestGetStateAttrs(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	attr1 := ""
	attr2 := "v2"
	mockActions.EXPECT().GetState(
		gomock.Eq(ctx),
		gomock.Eq(entities.LocationAttributes{
			Namespace: "s",
			Type:      "t",
			Attributes: map[string]*string{
				"a1": &attr1,
				"a2": &attr2,
				"a3": nil,
			},
		})).Return(&entities.State{State: []byte("42")}, nil)

	// nolint: lll
	req := httptest.NewRequest("GET", "http://localhost:9000?namespace=s&type=t&a1=&a2=v2&a3__empty=1", strings.NewReader(""))
	resp := httptest.NewRecorder()
	err := GetState(ctx, resp, req, mockActions)
	if err != nil {
		t.Fatal(err)
	}

	assert.Equal(t, 200, resp.Code)
	assert.Equal(t, `{"status":"ok","data":42}`, resp.Body.String())
}

func TestGetStateNoType(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	req := httptest.NewRequest("GET",
		"http://localhost:9000?namespace=abc&a1=a1&a2=a2", strings.NewReader(""))
	resp := httptest.NewRecorder()
	err := GetState(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, resp.Code, 400)
	// nolint: lll
	assert.Equal(t, `{"status":"error","data":{"code":"INVALID_REQUEST","description":"no required location attribute argument with key 'type'"}}`, resp.Body.String())
}

func TestGetStateActionError(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	mockActions.EXPECT().GetState(gomock.Any(), gomock.Any()).
		Return(nil, coreerrors.NewCodedError(418, "fgoihgrwjofgejohf", "q"))

	reqBody := strings.NewReader("")
	req := httptest.NewRequest("GET", "http://localhost:9000?namespace=abc&type=t", reqBody)
	resp := httptest.NewRecorder()
	err := GetState(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, 418, resp.Code)
	assert.Equal(t, `{"status":"error","data":{"code":"fgoihgrwjofgejohf","description":"q"}}`, resp.Body.String())
}

func TestGetStateInvalidJsonState(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	mockActions.EXPECT().GetState(gomock.Any(), gomock.Any()).Return(&entities.State{State: []byte("{")}, nil)

	req := httptest.NewRequest("GET",
		"http://localhost:9000?namespace=abc&type=t", strings.NewReader(""))
	resp := httptest.NewRecorder()
	err := GetState(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, resp.Code, 500)
	// nolint: lll
	assert.Equal(t, `{"status":"error","data":{"code":"LOGICAL_ERROR","description":"invalid json state: unexpected EOF"}}`, resp.Body.String())
}
