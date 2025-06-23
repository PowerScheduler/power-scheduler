package tech.powerscheduler.server.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import tech.powerscheduler.server.application.actor.AppGuardian
import tech.powerscheduler.server.application.actor.ServerAddressHolder
import tech.powerscheduler.server.application.utils.JSON

@Profile("!IT")
@Configuration
@EnableConfigurationProperties(PowerSchedulerServerProperties::class)
class AkkaConfiguration() {

    val log = LoggerFactory.getLogger(AkkaConfiguration::class.java)

    @Bean(destroyMethod = "shutdown")
    fun appGuardian(
        properties: PowerSchedulerServerProperties,
        applicationContext: ApplicationContext,
    ): AppGuardian {
        log.info("bootstrap config: {}", JSON.writeValueAsString(properties))
        val akkaProperties = properties.akka!!
        return AppGuardian(
            clusterMode = properties.clusterMode,
            endpoints = akkaProperties.endpoints.orEmpty(),
            akkaHost = akkaProperties.host!!,
            akkaManagementHttpPort = akkaProperties.managementHttpPort!!,
            akkaRemotePort = akkaProperties.remotePort!!,
            applicationContext = applicationContext
        )
    }

    @Bean
    fun server(properties: PowerSchedulerServerProperties): ServerAddressHolder {
        val address = properties.akka!!.let {
            "${it.host}:${it.remotePort}"
        }
        return ServerAddressHolder(address)
    }

}