package payout

import (
	"fmt"
	"math/rand"
	"testing"
	"time"

	"github.com/shopspring/decimal"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/payout/internal/context"
	"a.yandex-team.ru/billing/hot/payout/internal/core"
	bt "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

func getRandomContractServiceClient() (int64, int64, int64) {
	// service_id в БД объявлен как integer (4 байта), то есть max=2**31-1
	service := rand.Intn(2_000_000_00)
	return bt.RandN64(), int64(service), bt.RandN64()
}

func (s *PayoutTestSuite) truncatePayouts() error {
	db := s.ctx.Storage.Backend
	_, err := db.QueryRaw(s.ctx, "truncate table payout.t_payout")
	return err
}

func (s *PayoutTestSuite) TestRequestMetricsBase() {
	data, err := GetSelfMetrics(s.ctx)
	require.NoError(s.T(), err)

	for _, status := range Statuses {
		require.True(s.T(), data.ContainMetric(core.SolomonLabels{"sensor": "last", "status": status}))
		require.True(s.T(), data.ContainMetric(core.SolomonLabels{"sensor": "total", "status": status}))
		require.True(s.T(), data.ContainMetric(core.SolomonLabels{"sensor": "retardation", "status": status}))
		require.True(s.T(), data.ContainMetric(core.SolomonLabels{"sensor": "amount", "status": status, "type": "sum"}))
		require.True(s.T(), data.ContainMetric(core.SolomonLabels{"sensor": "amount", "status": status, "type": "last"}))
		require.True(s.T(), data.ContainMetric(core.SolomonLabels{"sensor": "partition_count"}))
	}
}

func createPayoutWithParams(T *testing.T, ctx *context.PayoutContext, serviceID, contractID, clientID int64,
	amount decimal.Decimal, namespace string, dryRun bool, approveRequired bool, status string) *Payout {
	np := NewPayout{
		ExternalID:      bt.RandS(30),
		ServiceID:       serviceID,
		ContractID:      contractID,
		Amount:          amount,
		Currency:        "RUB",
		ClientID:        clientID,
		Namespace:       namespace,
		ApproveRequired: approveRequired,
	}
	db := ctx.Storage.Backend
	tx, _ := db.BeginTx(ctx)

	p, err := CreateTx(ctx, tx, &np)
	require.NoError(T, err)
	update := core.UpdateDesc{
		{Name: NamespaceCol, Value: namespace},
		{Name: DryRunCol, Value: dryRun},
		{Name: StatusCol, Value: status},
	}
	err = UpdateTxX(ctx, p.ID, update, tx)
	require.NoError(T, err)
	_ = tx.Commit()
	resP, err := Get(ctx, p.ID)
	require.NoError(T, err)

	return resP
}

// requireEqualMetric находит в данных метрику по меткам и сравнивает ожидаемое значение с настоящим
// падает, если такой метрики нет
func requireEqualMetric(T *testing.T, data *core.SolomonData, expected core.Metric) {
	for _, metric := range data.Metrics {
		if metric.SameMetric(expected.Labels) {
			require.Equal(T, expected.Value, metric.Value, expected)
			return
		}
	}
	require.Fail(T, "no such metric", expected)
}

// TestRequestMetricsValues проверяет наличие и значения метрик, отправляемых в соломон
func (s *PayoutTestSuite) TestRequestMetricsValues() {
	// создаем выплаты с разными параметрами
	// ServiceID, ContractID и ClientID в этом тесте неважны, у всех одинаковые
	contractID, serviceID, clientID := getRandomContractServiceClient()

	_ = createPayoutWithParams(s.T(), s.ctx, serviceID, contractID, clientID,
		decimal.RequireFromString("100.00"), "monitoring1", false, false, StatusNew)

	_ = createPayoutWithParams(s.T(), s.ctx, serviceID, contractID, clientID,
		decimal.RequireFromString("200.00"), "monitoring1", false, false, StatusNew)

	_ = createPayoutWithParams(s.T(), s.ctx, serviceID, contractID, clientID,
		decimal.RequireFromString("300.00"), "monitoring1", false, false, StatusPending)

	_ = createPayoutWithParams(s.T(), s.ctx, serviceID, contractID, clientID,
		decimal.RequireFromString("400.00"), "monitoring1", false, false, StatusDone)

	_ = createPayoutWithParams(s.T(), s.ctx, serviceID, contractID, clientID,
		decimal.RequireFromString("500.00"), "monitoring1", true, false, StatusNew)

	_ = createPayoutWithParams(s.T(), s.ctx, serviceID, contractID, clientID,
		decimal.RequireFromString("600.00"), "monitoring1", true, false, StatusRejected)

	_ = createPayoutWithParams(s.T(), s.ctx, serviceID, contractID, clientID,
		decimal.RequireFromString("700.00"), "monitoring2", false, false, StatusConfirmed)

	_ = createPayoutWithParams(s.T(), s.ctx, serviceID, contractID, clientID,
		decimal.RequireFromString("800.00"), "monitoring2", true, false, StatusDone)

	_ = createPayoutWithParams(s.T(), s.ctx, serviceID, contractID, clientID,
		decimal.RequireFromString("900.00"), "monitoring2", false, false, StatusDone)

	_ = createPayoutWithParams(s.T(), s.ctx, serviceID, contractID, clientID,
		decimal.RequireFromString("900.00"), "monitoring2", false, false, StatusDone)

	_ = createPayoutWithParams(s.T(), s.ctx, serviceID, contractID, clientID,
		decimal.RequireFromString("1000.00"), "monitoring_approve", false, true, StatusDone)

	data, err := GetSelfMetrics(s.ctx)
	require.NoError(s.T(), err)

	expectedMetrics := []core.Metric{
		{Labels: core.SolomonLabels{"dry_run": "false", "namespace": "monitoring1", "sensor": "sliced_amount", "status": "new", "type": "sum"}, Value: float64(300)},
		{Labels: core.SolomonLabels{"dry_run": "false", "namespace": "monitoring1", "sensor": "sliced_total", "status": "new"}, Value: int64(2)},

		{Labels: core.SolomonLabels{"dry_run": "false", "namespace": "monitoring1", "sensor": "sliced_amount", "status": "pending", "type": "sum"}, Value: float64(300)},
		{Labels: core.SolomonLabels{"dry_run": "false", "namespace": "monitoring1", "sensor": "sliced_total", "status": "pending"}, Value: int64(1)},

		{Labels: core.SolomonLabels{"dry_run": "false", "namespace": "monitoring1", "sensor": "sliced_amount", "status": "done", "type": "sum"}, Value: float64(400)},
		{Labels: core.SolomonLabels{"dry_run": "false", "namespace": "monitoring1", "sensor": "sliced_total", "status": "done"}, Value: int64(1)},

		{Labels: core.SolomonLabels{"dry_run": "true", "namespace": "monitoring1", "sensor": "sliced_amount", "status": "new", "type": "sum"}, Value: float64(500)},
		{Labels: core.SolomonLabels{"dry_run": "true", "namespace": "monitoring1", "sensor": "sliced_total", "status": "new"}, Value: int64(1)},

		{Labels: core.SolomonLabels{"dry_run": "true", "namespace": "monitoring1", "sensor": "sliced_amount", "status": "rejected", "type": "sum"}, Value: float64(600)},
		{Labels: core.SolomonLabels{"dry_run": "true", "namespace": "monitoring1", "sensor": "sliced_total", "status": "rejected"}, Value: int64(1)},

		{Labels: core.SolomonLabels{"dry_run": "false", "namespace": "monitoring2", "sensor": "sliced_amount", "status": "confirmed", "type": "sum"}, Value: float64(700)},
		{Labels: core.SolomonLabels{"dry_run": "false", "namespace": "monitoring2", "sensor": "sliced_total", "status": "confirmed"}, Value: int64(1)},

		{Labels: core.SolomonLabels{"dry_run": "true", "namespace": "monitoring2", "sensor": "sliced_amount", "status": "done", "type": "sum"}, Value: float64(800)},
		{Labels: core.SolomonLabels{"dry_run": "true", "namespace": "monitoring2", "sensor": "sliced_total", "status": "done"}, Value: int64(1)},

		{Labels: core.SolomonLabels{"dry_run": "false", "namespace": "monitoring2", "sensor": "sliced_amount", "status": "done", "type": "sum"}, Value: float64(1800)},
		{Labels: core.SolomonLabels{"dry_run": "false", "namespace": "monitoring2", "sensor": "sliced_total", "status": "done"}, Value: int64(2)},

		{Labels: core.SolomonLabels{"approve_req": "true", "namespace": "monitoring_approve", "sensor": "amount", "type": "sum"}, Value: float64(1000)},
		{Labels: core.SolomonLabels{"approve_req": "true", "namespace": "monitoring_approve", "sensor": "total"}, Value: int64(1)},

		{Labels: core.SolomonLabels{"approve_req": "true", "namespace": "monitoring_approve", "sensor": "amount", "type": "last"}, Value: float64(1000)},
		{Labels: core.SolomonLabels{"approve_req": "true", "namespace": "monitoring_approve", "sensor": "last"}, Value: int64(1)},
	}

	for _, m := range expectedMetrics {
		requireEqualMetric(s.T(), data, m)
	}
	doubles := findDoubles(data)
	require.Equal(s.T(), 0, len(doubles), doubles)
}

func findDoubles(data *core.SolomonData) (ret []core.Metric) {
	metricsMap := map[[16]byte]core.Metric{}
	for _, metric := range data.Metrics {
		hash := solomonLabelsToHash(metric.Labels)
		if metric2, exists := metricsMap[hash]; exists {
			ret = append(ret, metric, metric2)
		} else {
			metricsMap[hash] = metric
		}
	}
	return
}

// TestTotalCountAmount проверяет, что общие метрики собираются только по dry_run=false
func (s *PayoutTestSuite) TestTotalCountAmountRetardation() {
	// чистим базу от созданных выплат, чтобы не сайдэффектили пред. тесты
	require.NoError(s.T(), s.truncatePayouts())
	tx, err := s.ctx.Storage.Backend.BeginTx(s.ctx)
	require.NoError(s.T(), err)

	err = s.pDB.MvPayoutRefresh(s.ctx, tx)
	require.NoError(s.T(), err)
	err = tx.Commit()
	require.NoError(s.T(), err)
	// достаем текущие метрики
	data, err := GetSelfMetrics(s.ctx)
	require.NoError(s.T(), err)

	i, err := core.FindMetric(data, core.SolomonLabels{"sensor": "total", "status": "new"})
	require.NoError(s.T(), err)
	beforeTotalCount := i.(int64)
	i, err = core.FindMetric(data, core.SolomonLabels{"sensor": "amount", "status": "new", "type": "sum"})
	require.NoError(s.T(), err)
	beforeTotalAmount := i.(float64)

	// создаем выплаты с разными параметрами
	// ServiceID, ContractID и ClientID в этом тесте неважны, у всех одинаковые
	contractID, serviceID, clientID := getRandomContractServiceClient()

	// Создаем 5 выплат с dry_run false
	for i := 0; i < 5; i++ {
		p := createPayoutWithParams(s.T(), s.ctx, serviceID, contractID, clientID,
			decimal.RequireFromString("100.00"), "monitoring_total_dry_run", false, false, StatusNew)
		update := core.UpdateDesc{
			{Name: CreateDTCol, Value: p.CreateDt.AddDate(0, 0, -10)},
		}
		err = UpdateX(s.ctx, p.ID, update)
		require.NoError(s.T(), err)
	}

	// создаем 8 выплат с dry_run=true
	for i := 0; i < 8; i++ {
		p := createPayoutWithParams(s.T(), s.ctx, serviceID, contractID, clientID,
			decimal.RequireFromString("100.00"), "monitoring1", true, false, StatusNew)
		update := core.UpdateDesc{
			{Name: CreateDTCol, Value: p.CreateDt.AddDate(0, 0, -20)},
		}
		err = UpdateX(s.ctx, p.ID, update)
		require.NoError(s.T(), err)

	}

	tx, err = s.ctx.Storage.Backend.BeginTx(s.ctx)
	require.NoError(s.T(), err)

	err = s.pDB.MvPayoutRefresh(s.ctx, tx)
	require.NoError(s.T(), err)
	err = tx.Commit()
	require.NoError(s.T(), err)
	data, err = GetSelfMetrics(s.ctx)
	require.NoError(s.T(), err)

	i, err = core.FindMetric(data, core.SolomonLabels{"sensor": "total", "status": "new"})
	require.NoError(s.T(), err)
	afterTotalCount := i.(int64)
	i, err = core.FindMetric(data, core.SolomonLabels{"sensor": "amount", "status": "new", "type": "sum"})
	require.NoError(s.T(), err)
	afterTotalAmount := i.(float64)
	i, err = core.FindMetric(data, core.SolomonLabels{"sensor": "retardation", "status": "new"})
	require.NoError(s.T(), err)
	afterTotalRetardation := i.(float64)

	// проверяем, что учли только 5 не dry_run выплат
	require.Equal(s.T(), beforeTotalCount+5, afterTotalCount)
	require.Equal(s.T(), beforeTotalAmount+500.0, afterTotalAmount)
	// проверяем, что retardation 10 дней, а не 20, как если бы учитывали dry_run выплаты
	require.Equal(s.T(), 10*24*60, int(afterTotalRetardation))

}

// TestRetardationOldPayouts проверяет корректность метрик при использовании mv_payout
func (s *PayoutTestSuite) TestRetardationOldPayouts() {
	// чистим базу от созданных выплат, чтобы не сайдэффектили пред. тесты
	require.NoError(s.T(), s.truncatePayouts())

	// ServiceID, ContractID и ClientID в этом тесте неважны, у всех одинаковые
	contractID, serviceID, clientID := getRandomContractServiceClient()

	p := map[string]*Payout{}       // payout by status
	deltaTime := map[string]int64{} // retardation time by status

	var err error
	tNow := time.Now()
	rnd := rand.New(rand.NewSource(tNow.Unix()))
	for _, status := range Statuses {
		pCurrent := createPayoutWithParams(s.T(), s.ctx, serviceID, contractID, clientID,
			decimal.RequireFromString("100.00"), "monitoring_total_dry_run", false, false, status)
		deltaTime[status] = int64(rnd.Intn(86400*10) + 86400*181) // полгода +- 10
		update := core.UpdateDesc{
			{Name: CreateDTCol, Value: time.Unix(tNow.Unix()-deltaTime[status], 0)},
		}
		err = UpdateX(s.ctx, pCurrent.ID, update)
		require.NoError(s.T(), err)
		p[status] = pCurrent
	}

	tx, err := s.ctx.Storage.Backend.BeginTx(s.ctx)
	require.NoError(s.T(), err)

	err = s.pDB.MvPayoutRefresh(s.ctx, tx)
	require.NoError(s.T(), err)
	err = tx.Commit()
	require.NoError(s.T(), err)
	data, err := GetSelfMetrics(s.ctx)
	require.NoError(s.T(), err)

	for _, status := range Statuses {
		i, err := core.FindMetric(data, core.SolomonLabels{"sensor": "retardation", "status": status})
		require.NoError(s.T(), err)
		totalRetardation := i.(float64)
		require.Equal(s.T(), deltaTime[status]/60, int64(totalRetardation), "mismatch for status %s", status)
	}
}

// TestPayoutMaxAge проверяет корректность учета настроек по макс. возрасту выплат для попадания в мониторинг
func (s *PayoutTestSuite) TestPayoutMaxAge() {
	contractID, serviceID, clientID := getRandomContractServiceClient()
	ns := "monitoring_max_age"
	nonDryRun, nonApprove := false, false

	p1 := createPayoutWithParams(s.T(), s.ctx, serviceID, contractID, clientID,
		decimal.RequireFromString("111.11"), ns, nonDryRun, nonApprove, StatusNew)
	p2 := createPayoutWithParams(s.T(), s.ctx, serviceID, contractID, clientID,
		decimal.RequireFromString("222.00"), ns, nonDryRun, nonApprove, StatusNew)
	p3 := createPayoutWithParams(s.T(), s.ctx, serviceID, contractID, clientID,
		decimal.RequireFromString("333.00"), ns, nonDryRun, nonApprove, StatusNew)

	// вторую выплату выносим за границу учета в прошлое
	require.NoError(s.T(), UpdateX(s.ctx, p2.ID, core.UpdateDesc{
		{Name: CreateDTCol, Value: time.Now().AddDate(0, 0, -(s.ctx.Config.Monitorings.PayoutMaxAge + 1))},
	}))

	// третью выплату выносим за границу учета в будущее
	require.NoError(s.T(), UpdateX(s.ctx, p3.ID, core.UpdateDesc{
		{Name: CreateDTCol, Value: time.Now().AddDate(0, 0, 1)},
	}))

	data, err := GetSelfMetrics(s.ctx)
	require.NoError(s.T(), err)

	// убеждаемся, что находится только p1, остальные отбрасываются
	amt, err := core.FindMetric(data, core.SolomonLabels{"dry_run": "false", "sensor": "sliced_amount",
		"namespace": ns, "status": "new", "type": "sum"})
	require.NoError(s.T(), err)
	require.Equal(s.T(), p1.Amount.String(), fmt.Sprintf("%.2f", amt.(float64)))

	cnt, err := core.FindMetric(data, core.SolomonLabels{"dry_run": "false", "sensor": "sliced_total",
		"namespace": ns, "status": "new"})
	require.NoError(s.T(), err)
	require.Equal(s.T(), int64(1), cnt.(int64))
}
