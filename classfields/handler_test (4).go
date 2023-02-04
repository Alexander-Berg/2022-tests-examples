package admin

import (
	"context"
	"testing"

	"github.com/YandexClassifieds/shiva/pb/shiva/api/admin"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/flags"
	"github.com/YandexClassifieds/shiva/pkg/auth/blackbox"
	"github.com/YandexClassifieds/shiva/pkg/auth/oauth/grpc"
	"github.com/YandexClassifieds/shiva/pkg/feature_flags"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock/mq"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestFlags(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)
	featureFlagsService := feature_flags.NewService(db, mq.NewProducerMock(), log)
	sMap := service_map.NewService(db, log, service_change.NewNotificationMock())
	adminServer := NewAdminHandler(featureFlagsService, nil, sMap, nil, nil, log)

	ctx := grpc.UserInfoContext(context.Background(), &blackbox.UserInfo{Login: "not admin", UID: 0})

	_, err := adminServer.SetFlags(ctx, &flags.Flags{
		List: []*flags.FeatureFlag{{
			Name:   feature_flags.TestSasOff.String(),
			Reason: "учения",
		}},
	})
	require.Error(t, err)

	_, err = adminServer.GetFlags(ctx, &admin.GetFlagsRequest{})
	require.Error(t, err)

	_, err = adminServer.ClearFlags(ctx, &flags.Flags{
		List: []*flags.FeatureFlag{{
			Name: feature_flags.TestSasOff.String(),
		}},
	})
	require.Error(t, err)

	ctx = grpc.UserInfoContext(context.Background(), &blackbox.UserInfo{Login: "ibiryulin", UID: 0})

	_, err = adminServer.SetFlags(ctx, &flags.Flags{
		List: []*flags.FeatureFlag{{
			Name:   feature_flags.TestSasOff.String(),
			Reason: "учения",
		}},
	})
	require.NoError(t, err)

	fs, err := adminServer.GetFlags(ctx, &admin.GetFlagsRequest{})
	require.NoError(t, err)
	assert.Len(t, fs.List, 1)
	assert.Equal(t, feature_flags.TestSasOff.String(), fs.List[0].Name)
	assert.Equal(t, "учения", fs.List[0].Reason)

	_, err = adminServer.ClearFlags(ctx, &flags.Flags{
		List: []*flags.FeatureFlag{{
			Name: feature_flags.TestSasOff.String(),
		}},
	})
	require.NoError(t, err)
}

func TestAdmin_TurnOnOff(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)
	mP := mq.NewProducerMock()
	featureFlagsService := feature_flags.NewService(db, mP, log)
	sMap := service_map.NewService(db, log, service_change.NewNotificationMock())
	adminServer := NewAdminHandler(featureFlagsService, nil, sMap, nil, nil, log)

	assert.Len(t, mP.Msg, 0)
	ctx := grpc.UserInfoContext(context.Background(), &blackbox.UserInfo{Login: "ibiryulin", UID: 0})
	_, err := adminServer.SetFlags(ctx, &flags.Flags{
		List: []*flags.FeatureFlag{{
			Name:   feature_flags.TestSasOff.String(),
			Reason: "учения",
		}},
	})
	require.NoError(t, err)
	assert.Len(t, mP.Msg, 1)

	_, err = adminServer.ClearFlags(ctx, &flags.Flags{
		List: []*flags.FeatureFlag{{
			Name: feature_flags.TestSasOff.String(),
		}},
	})
	require.NoError(t, err)
	assert.Len(t, mP.Msg, 2)
}
