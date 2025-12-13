package tech.powerscheduler.server.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import tech.powerscheduler.server.application.schedule.system.ClusterMode

/**
 * @author grayrat
 * @since 2025/5/26
 */
@ConfigurationProperties(prefix = "power-scheduler-server")
class PowerSchedulerServerProperties(
    /**
     * 集群模式: SINGLE-单节点集群, CLUSTER-多节点集群
     */
    var clusterMode: ClusterMode = ClusterMode.SINGLETON,
    /**
     * akka相关配置
     */
    var akka: AkkaProperties? = null,
) {

    /**
     * Akka相关配置
     */
    class AkkaProperties {
        /**
         * 暴露给akka集群的host，必须保证集群中其他节点可以访问该地址（单机模式下可以省略）
         */
        var host: String? = null

        /**
         * 集群节点内部通信使用的tcp端口
         */
        var remotePort: Int? = null

        /**
         * 集群管理使用的http接口（使用场景：服务发现）
         */
        var managementHttpPort: Int? = null

        /**
         * 集群节点配置, 格式：ip1:port1, ip2:port2...
         * ⚠️注意：本地进行多节点集群测试的时候，配置同个ip加不同端口是无法启动的（例如: 127.0.0.1:8551, 127.0.0.1:8552）
         * 本地测试建议创建多个回环端口（例如: 127.0.0.1:8551, 127.0.0.2:8552）
         */
        var endpoints: List<String>? = emptyList()
    }

}