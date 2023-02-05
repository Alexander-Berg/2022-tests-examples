package com.edadeal.android.helpers

import com.edadeal.android.data.endpoints.EndpointsRepository
import com.edadeal.android.dto.Experiment
import com.edadeal.android.model.Experiments
import com.edadeal.android.model.ExperimentsUrlToSourceMapper
import com.edadeal.android.model.api.endpoints.Endpoints
import com.edadeal.android.model.api.endpoints.VitalEndpointsDelegate
import com.edadeal.android.model.auth.passport.PassportContext
import com.edadeal.android.util.DefaultUrls
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever

object ExperimentsFactory {

    fun getExperiments(
        id: String,
        source: Map<String, Map<String, Set<String>>>
    ): Experiments {
        val experiment = getExperiment(id, source)
        val endpointsRepository: EndpointsRepository = mock()
        val passportContext = PassportContext.PRODUCTION
        val endpoints = Endpoints.create(DefaultUrls.Edadeal, passportContext)
        whenever(endpointsRepository.endpoints).thenReturn(endpoints)
        val vitalEndpointsDelegate = VitalEndpointsDelegate(mock(), mock(), passportContext, mock(), mock())
        val experimentsUrlToSourceMapper = ExperimentsUrlToSourceMapper(
            mock(), endpointsRepository, vitalEndpointsDelegate
        )
        return Experiments(mock(), experimentsUrlToSourceMapper, listOf(experiment))
    }

    private fun getExperiment(
        id: String,
        source: Map<String, Map<String, Set<String>>>,
        releasedWithCalibrator: Boolean = false
    ): Experiment {
        return Experiment(
            ExperimentID = id,
            CONTEXT = Experiment.Context(EDADEAL = Experiment.ContextType(source = source)),
            ReleasedWithCalibrator = releasedWithCalibrator
        )
    }
}
