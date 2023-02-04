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
	"google.golang.org/protobuf/types/known/wrapperspb"
)

const CreateIdentityTag = "CreateIdentity"

type CreateIdentityAmmo struct {
	Tag              string           `json:"tag"`
	ExternalIdentity ExternalIdentity `json:"external_identity"`
	ParentID         *CompositeID     `json:"parent_id,omitempty"`
	Data             IdentityData     `json:"data"`
}

func ShootCreateIdentityAmmo(client api.IdentityServiceClient, aggr core.Aggregator, ammo *CreateIdentityAmmo) {
	code := codes.Unknown
	req := &api.CreateIdentityRequest{
		ExternalIdentity: &api.ExternalIdentity{
			ExternalId: ammo.ExternalIdentity.ExternalID,
			TypeId:     ammo.ExternalIdentity.TypeID,
		},
		Data: &api.ModifiableIdentityData{},
	}
	if ammo.ParentID != nil {
		req.ParentId = &api.IdentityCompositeId{}
		if ammo.ParentID.ID != "" {
			req.ParentId.IdentityIdOneof = &api.IdentityCompositeId_Id{
				Id: ammo.ParentID.ID,
			}
		} else {
			req.ParentId.IdentityIdOneof = &api.IdentityCompositeId_ExternalIdentity{
				ExternalIdentity: &api.ExternalIdentity{
					ExternalId: ammo.ParentID.ExternalIdentity.ExternalID,
					TypeId:     ammo.ParentID.ExternalIdentity.TypeID,
				},
			}
		}
	}
	if ammo.Data.Name != "" {
		req.Data.Name = &wrapperspb.StringValue{
			Value: ammo.Data.Name,
		}
	}
	if ammo.Data.LastName != "" {
		req.Data.Lastname = &wrapperspb.StringValue{
			Value: ammo.Data.LastName,
		}
	}
	if ammo.Data.Slug != "" {
		req.Data.Slug = &wrapperspb.StringValue{
			Value: ammo.Data.Slug,
		}
	}
	if ammo.Data.Phone != "" {
		req.Data.Phone = &wrapperspb.StringValue{
			Value: ammo.Data.Phone,
		}
	}
	if ammo.Data.Email != "" {
		req.Data.Email = &wrapperspb.StringValue{
			Value: ammo.Data.Email,
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

	_, err := client.CreateIdentity(ctx, req)
	if err == nil {
		code = codes.OK
	} else {
		if e, ok := status.FromError(err); ok {
			code = e.Code()
		}
	}
}
