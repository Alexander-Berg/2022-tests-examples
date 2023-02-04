package integration

import (
	"context"
	"os"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions/accounts"
	"a.yandex-team.ru/library/go/core/metrics/solomon"
)

func TestApi(t *testing.T) {
	registry := solomon.NewRegistry(nil)

	client, err := accounts.New(accounts.Config{Transport: interactions.Config{
		BaseURL: os.Getenv("ACCOUNTS_BASE_URL"),
		Name:    "accounts",
	}}, nil, registry)
	require.NoError(t, err)

	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()

	clientID, contractID, currency, product, region := "7151963", "12345", "RUB", "anything", "RUS"

	resp, err := client.ReadBatch(ctx, &entities.BatchReadRequest{
		Balances: []entities.DtRequestAttributes{
			{
				Dt: time.Unix(1718486455, 0),
				Loc: entities.LocationAttributes{
					Namespace: "taxi",
					Type:      "commissions_with_vat",
					Attributes: map[string]*string{
						"client_id":        &clientID,
						"contract_id":      &contractID,
						"currency":         &currency,
						"product":          &product,
						"detailed_product": &product,
						"region":           &region,
					},
				},
			},
		},
	})
	require.NoError(t, err)
	assert.Equal(t, &entities.BatchReadResponse{}, resp)
}
