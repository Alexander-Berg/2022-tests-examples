package mock

import (
	"context"
	error1 "github.com/YandexClassifieds/shiva/pb/shiva/types/error"
	"github.com/YandexClassifieds/shiva/pb/ss/access"
	"google.golang.org/grpc"
	"testing"
)

type ssMockData struct {
	request  *access.CheckTokenRequest
	response *access.CheckTokenResponse
}

type AccessClientMock struct {
	data []ssMockData
	t    *testing.T
}

func NewAccessClientMock(t *testing.T) *AccessClientMock {
	return &AccessClientMock{
		t:    t,
		data: []ssMockData{},
	}
}

func (a *AccessClientMock) NewToken(_ context.Context, _ *access.NewTokenRequest, _ ...grpc.CallOption) (*access.NewTokenResponse, error) {
	panic("implement me")
}

func (a *AccessClientMock) RemoveToken(_ context.Context, _ *access.RemoveTokenRequest, _ ...grpc.CallOption) (*access.RemoveTokenResponse, error) {
	panic("implement me")
}

func (a *AccessClientMock) CheckToken(_ context.Context, in *access.CheckTokenRequest, _ ...grpc.CallOption) (*access.CheckTokenResponse, error) {

	for _, d := range a.data {
		r := d.request
		if in.ServiceName == r.ServiceName && in.SecretId == r.SecretId && in.VersionId == in.VersionId {
			return d.response, nil
		}
	}
	return &access.CheckTokenResponse{
		IsDelegated: true,
		MissingKeys: []string{},
	}, nil
}

func (a *AccessClientMock) GetDelegationToken(ctx context.Context, in *access.GetDelegationTokenRequest, opts ...grpc.CallOption) (*access.GetDelegationTokenResponse, error) {
	return &access.GetDelegationTokenResponse{}, nil
}

func (a *AccessClientMock) Add(name, secID, verID string, err *error1.UserError, isDelegated bool, MissingKeys ...string) {
	a.data = append(a.data, ssMockData{
		request: &access.CheckTokenRequest{
			ServiceName: name,
			SecretId:    secID,
			VersionId:   verID,
		},
		response: &access.CheckTokenResponse{
			IsDelegated: isDelegated,
			Error:       err,
			MissingKeys: MissingKeys,
		},
	})
}
