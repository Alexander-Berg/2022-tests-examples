import AutoRuProtoModels

extension Mocker {
    @discardableResult
    func mock_sharkCreditProductList(products: [SharkMocker.Product], appProduct: [SharkMocker.Product]? = nil) -> Self {
        SharkMocker(server: server)
            .mockProductList(products: products, appProduct: appProduct)

        return self
    }

    @discardableResult
    func mock_sharkCreditApplicationActiveEmpty() -> Self {
        SharkMocker(server: server)
            .mockNoApplication()

        return self
    }

    @discardableResult
    func mock_sharkCreditProductCalculator() -> Self {
        SharkMocker(server: server)
            .mockCalculator()

        return self
    }
}
