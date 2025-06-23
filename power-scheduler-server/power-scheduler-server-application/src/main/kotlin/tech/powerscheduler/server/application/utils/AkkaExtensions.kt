package tech.powerscheduler.server.application.utils

import akka.actor.Address
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.cluster.typed.Cluster
import akka.japi.function.Function
import kotlin.jvm.optionals.getOrNull

fun <T> AbstractBehavior<T>.registerSelfAsService(serviceKey: ServiceKey<T>) {
    val receptionist = this.context.system.receptionist()
    receptionist.tell(Receptionist.register(serviceKey, this.context.self.narrow()))
}

fun <T, S> AbstractBehavior<T>.subscribeService(serviceKey: ServiceKey<S>, adapterMapper: (Set<ActorRef<S>>) -> T) {
    val adapter = Function<Receptionist.Listing, T> { listing ->
        val serviceInstances = listing.getServiceInstances(serviceKey)
        adapterMapper(serviceInstances)
    }
    val subscriptionAdapter = context.messageAdapter(Receptionist.Listing::class.java, adapter)
    val receptionist = this.context.system.receptionist()
    receptionist.tell(Receptionist.subscribe(serviceKey, subscriptionAdapter))
}

fun ActorSystem<*>.hostPort(): String {
    val address = Cluster.get(this).selfMember().address()
    return getHostPort(address)
}

fun ActorRef<*>.hostPort(): String {
    val address = this.path().address()
    return getHostPort(address)
}

private fun getHostPort(address: Address): String {
    val host = address.host.getOrNull()
    val port = address.port.getOrNull()
    if (host.isNullOrBlank() || port == null) {
        return ""
    }
    return "$host:$port"
}