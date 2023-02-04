package oebsgate

import (
	"encoding/json"
	"fmt"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func (s *OEBSGateTestSuite) TestLBMessage() {
	p, _, err := s.createRandomPayoutWithRequestAmount("124.666")
	require.NoError(s.T(), err)

	dt, _ := time.Parse(time.RFC3339, "2021-03-19T18:27:34Z")
	data, err := FormatLBMessage(p, &dt)
	require.NoError(s.T(), err)

	jsonMsg := fmt.Sprintf(`{"message_id":%d,"message_dt":"2021-03-19 18:27:34","message_system":"billingpay",`+
		`"billing_contract_id":%d,"billing_person_id":-1,"transaction_type":"PAYMENT",`+
		`"transaction_dt":"2021-03-19","service_id":%d,"amount":%s,"paysys_type_cc":"payout",`+
		`"partner_currency":"RUB","billing_client_id":%d,"namespace":"taxi"}`,
		p.ID, p.ContractID, p.ServiceID, p.AmountOEBS.String(), p.ClientID)
	assert.Equal(s.T(), jsonMsg, string(data))

	var message NewPayoutLB
	err = json.Unmarshal(data, &message)
	require.NoError(s.T(), err)

	assert.Equal(s.T(), "2021-03-19 18:27:34", message.MessageDT)
	assert.Equal(s.T(), "2021-03-19", message.TransactionDT)
	assert.Equal(s.T(), json.Number("124.67"), message.Amount)
	assert.Equal(s.T(), "124.666", p.Amount.String())
	assert.Equal(s.T(), "124.67", p.AmountOEBS.String())
	assert.Equal(s.T(), p.ClientID, message.ClientID)
	assert.Equal(s.T(), p.Namespace, message.Namespace)
}
