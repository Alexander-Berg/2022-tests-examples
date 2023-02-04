package ru.auto.ara.core

import androidx.fragment.app.Fragment
import ru.auto.ara.ui.adapter.main.ILoanCabinetFragmentFactory
import ru.auto.core_ui.util.withParcelableArgs
import ru.auto.data.model.stat.SearchId
import ru.auto.feature.loans.cabinet.di.ILoanCabinetProvider
import ru.auto.feature.loans.cabinet.ui.LoanCabinetFragment

class TestLoanCabinetFragmentFactory : ILoanCabinetFragmentFactory {

    override fun create(searchId: SearchId): Fragment = LoanCabinetFragment()
        .withParcelableArgs(
            ILoanCabinetProvider.Args(
                isInlineScreen = true,
                showCollapsingHeader = false,
                isDev = true,
                searchId = searchId,
            )
        )

}
