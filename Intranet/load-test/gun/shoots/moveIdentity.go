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

const MoveIdentityTag = "MoveIdentity"

type MoveIdentityAmmo struct {
	Tag      string      `json:"tag"`
	Identity CompositeID `json:"identity"`
	ToGroup  CompositeID `json:"to_group"`
}

func ShootMoveIdentityAmmo(client api.IdentityServiceClient, aggr core.Aggregator, ammo *MoveIdentityAmmo) {
	code := codes.Unknown
	req := &api.MoveIdentityRequest{
		Identity: &api.IdentityCompositeId{},
		ToGroup:  &api.IdentityCompositeId{},
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
	if ammo.ToGroup.ID != "" {
		req.ToGroup.IdentityIdOneof = &api.IdentityCompositeId_Id{
			Id: ammo.ToGroup.ID,
		}
	} else {
		req.ToGroup.IdentityIdOneof = &api.IdentityCompositeId_ExternalIdentity{
			ExternalIdentity: &api.ExternalIdentity{
				ExternalId: ammo.ToGroup.ExternalIdentity.ExternalID,
				TypeId:     ammo.ToGroup.ExternalIdentity.TypeID,
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
	_, err := client.MoveIdentity(ctx, req)
	if err == nil {
		code = codes.OK
	} else {
		if e, ok := status.FromError(err); ok {
			code = e.Code()
		}
	}
}
