package shoots

import (
	api "a.yandex-team.ru/intranet/ims/imscore/backend/service/proto"
	"context"
	"github.com/gofrs/uuid"
	"github.com/yandex/pandora/core"
	"github.com/yandex/pandora/core/aggregator/netsample"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/metadata"
	"google.golang.org/grpc/status"
)

const ListIdentityGroupsTag = "ListIdentityGroups"

type ListIdentityGroupsAmmo struct {
	Tag      string      `json:"tag"`
	Identity CompositeID `json:"identity"`
}

func ShootListIdentityGroupsAmmo(
	client api.IdentityGroupServiceClient, aggr core.Aggregator, ammo *ListIdentityGroupsAmmo) {
	code := codes.Unknown
	req := &api.ListIdentityGroupsRequest{
		Identity: &api.IdentityCompositeId{},
	}

	if ammo.Identity.ID != "" {
		req.Identity.IdentityIdOneof = &api.IdentityCompositeId_Id{
			Id: ammo.Identity.ID,
		}
	} else {
		req.Identity.IdentityIdOneof = &api.IdentityCompositeId_ExternalIdentity{
			ExternalIdentity: &api.ExternalIdentity{
				ExternalId: ammo.Identity.ExternalIdentity.ExternalID,
				TypeId:     ammo.Identity.ExternalIdentity.TypeID,
			},
		}
	}

	sample := netsample.Acquire(ammo.Tag)
	defer func() {
		sample.SetProtoCode(int(code))
		aggr.Report(sample)
	}()

	requestID, _ := uuid.NewV4()
	ctx := metadata.AppendToOutgoingContext(context.Background(),
		"X-Request-Id", requestID.String(),
	)
	_, err := client.ListIdentityGroups(ctx, req)
	if err == nil {
		code = codes.OK
	} else {
		if e, ok := status.FromError(err); ok {
			code = e.Code()
		}
	}
}
