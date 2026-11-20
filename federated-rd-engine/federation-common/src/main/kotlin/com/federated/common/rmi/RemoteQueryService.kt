package com.federated.common.rmi

import java.rmi.Remote
import java.rmi.RemoteException

interface RemoteQueryService : Remote {
    @Throws(RemoteException::class)
    fun executeSubQuery(request: FederatedQueryRequest): FederatedQueryResponse

    @Throws(RemoteException::class)
    fun getHealthStatus(): NodeHealth
}
