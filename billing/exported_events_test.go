package impl

import (
	"context"
	"errors"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/accounts/mock/settingsmock"
	"a.yandex-team.ru/billing/hot/accounts/mock/storagemock"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
)

func TestGetExportedEvents(t *testing.T) {
	ctrl := gomock.NewController(t)

	type given struct {
		attrs       entities.LocationAttributes
		externalIDs []string
	}

	type expected struct {
		events []entities.LbExportEntry
		error  string
	}

	type deps struct {
		settings *settingsmock.MockSettings
		shards   *storagemock.MockShardStorage
		template nopRenderer
	}

	testcases := []struct {
		name     string
		prepare  func(deps *deps)
		given    given
		expected expected
	}{
		{
			name: "account attrs validation failed",
			prepare: func(deps *deps) {
				as := settingsmock.NewMockAccountSettings(ctrl)

				as.EXPECT().Validate(&entities.LocationAttributes{
					Namespace: "foo",
					Type:      "cashless",
				}).Return(errors.New("account validation"))

				deps.settings.EXPECT().Account().Return(as)
			},
			given: given{
				attrs: entities.LocationAttributes{
					Namespace: "foo",
					Type:      "cashless",
				},
			},
			expected: expected{
				error: "failed to validate attrs: account validation",
			},
		},
		{
			name: "shard key retrieving by attrs failed",
			prepare: func(deps *deps) {
				as := settingsmock.NewMockAccountSettings(ctrl)

				as.EXPECT().Validate(&entities.LocationAttributes{
					Namespace: "bar",
					Type:      "payout",
				}).Return(nil)
				as.EXPECT().ShardKey(&entities.LocationAttributes{
					Namespace: "bar",
					Type:      "payout",
				}).Return("", errors.New("database problem"))

				deps.settings.EXPECT().Account().Return(as).Times(2)
			},
			given: given{
				attrs: entities.LocationAttributes{
					Namespace: "bar",
					Type:      "payout",
				},
			},
			expected: expected{
				error: "failed to get shard key: database problem",
			},
		},
		{
			name: "shard retrieving by shard key failed",
			prepare: func(deps *deps) {
				as := settingsmock.NewMockAccountSettings(ctrl)

				as.EXPECT().Validate(&entities.LocationAttributes{
					Namespace: "foobar",
					Type:      "acts",
				}).Return(nil)
				as.EXPECT().ShardKey(&entities.LocationAttributes{
					Namespace: "foobar",
					Type:      "acts",
				}).Return("skey", nil)

				deps.settings.EXPECT().Account().Return(as).Times(2)
				deps.shards.EXPECT().GetShard("skey").Return(nil, errors.New("database error"))
			},
			given: given{
				attrs: entities.LocationAttributes{
					Namespace: "foobar",
					Type:      "acts",
				},
			},
			expected: expected{
				error: "failed to get shard: database error",
			},
		},
		{
			name: "export batches retrieving failed",
			prepare: func(deps *deps) {
				var (
					as  = settingsmock.NewMockAccountSettings(ctrl)
					srd = storagemock.NewMockShard(ctrl)
					str = storagemock.NewMockLbExporterStorage(ctrl)
				)

				as.EXPECT().Validate(&entities.LocationAttributes{
					Namespace: "name",
					Type:      "payment",
				}).Return(nil)
				as.EXPECT().ShardKey(&entities.LocationAttributes{
					Namespace: "name",
					Type:      "payment",
				}).Return("shard_key", nil)

				srd.EXPECT().GetLbExporterStorage().Return(str)

				str.EXPECT().
					GetExportEntriesExternalIDs(gomock.Any(), as, []string{"1", "2"}).
					Return(nil, errors.New("db problem"))

				deps.settings.EXPECT().Account().Return(as).Times(3)
				deps.shards.EXPECT().GetShard("shard_key").Return(srd, nil)
			},
			given: given{
				attrs: entities.LocationAttributes{
					Namespace: "name",
					Type:      "payment",
				},
				externalIDs: []string{"1", "2"},
			},
			expected: expected{
				error: "failed to get export batches: db problem",
			},
		},
		{
			name: "export batches retrieving succeed",
			prepare: func(deps *deps) {
				var (
					as  = settingsmock.NewMockAccountSettings(ctrl)
					srd = storagemock.NewMockShard(ctrl)
					str = storagemock.NewMockLbExporterStorage(ctrl)
				)

				as.EXPECT().Validate(&entities.LocationAttributes{
					Namespace: "space",
					Type:      "cashless",
				}).Return(nil)
				as.EXPECT().ShardKey(&entities.LocationAttributes{
					Namespace: "space",
					Type:      "cashless",
				}).Return("shard_key", nil)

				srd.EXPECT().GetLbExporterStorage().Return(str)

				str.EXPECT().
					GetExportEntriesExternalIDs(gomock.Any(), as, []string{"1"}).
					Return(
						[]entities.LbExportEntry{
							{
								ID: 1,
							},
							{
								ID: 2,
							},
						},
						nil,
					)

				deps.settings.EXPECT().Account().Return(as).Times(3)
				deps.shards.EXPECT().GetShard("shard_key").Return(srd, nil)
			},
			given: given{
				attrs: entities.LocationAttributes{
					Namespace: "space",
					Type:      "cashless",
				},
				externalIDs: []string{"1"},
			},
			expected: expected{
				events: []entities.LbExportEntry{
					{
						ID: 1,
					},
					{
						ID: 2,
					},
				},
			},
		},
	}

	for _, c := range testcases {
		t.Run(c.name, func(t *testing.T) {
			d := deps{
				settings: settingsmock.NewMockSettings(ctrl),
				shards:   storagemock.NewMockShardStorage(ctrl),
				template: nopRenderer{},
			}

			c.prepare(&d)

			action := NewActions(d.settings, d.shards, d.template)

			events, err := action.GetExportedEvents(
				context.Background(),
				c.given.attrs,
				c.given.externalIDs,
			)

			if c.expected.error != "" {
				require.EqualError(t, err, c.expected.error)

				return
			}

			require.NoError(t, err)
			require.Equal(t, events, c.expected.events)
		})
	}
}

type nopRenderer struct{}

func (r nopRenderer) Render(
	_ context.Context,
	_ any,
	_ string,
	_ ...string,
) (string, error) {
	return "", nil
}
