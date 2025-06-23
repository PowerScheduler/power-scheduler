@file:Suppress("unused")

package tech.powerscheduler.server.domain.workflow

import tech.powerscheduler.common.enums.WorkflowStatusEnum

class Attrs {
    var line: Line? = null

    class Line {
        var stroke: String? = null
        var targetMarker: TargetMarker? = null

        class TargetMarker {
            var name: String? = null
            var width: Int? = null
            var height: Int? = null
        }
    }
}

class Source {
    var cell: String? = null
    var port: String? = null
}

class Target {
    var cell: String? = null
    var port: String? = null
}

class Position {
    var x: Int? = null
    var y: Int? = null
}

class Size {
    var width: Int? = null
    var height: Int? = null
}

class Ports {
    var groups: Groups? = null
    var items: List<Item?>? = null

    class Groups {
        var top: GroupAttr? = null
        var right: GroupAttr? = null
        var bottom: GroupAttr? = null
        var left: GroupAttr? = null

        class GroupAttr {
            var position: String? = null
            var attrs: Attrs? = null

            class Attrs {
                var circle: Circle? = null

                class Circle {
                    var r: Int? = null
                    var magnet: Boolean? = null
                    var stroke: String? = null
                    var strokeWidth: Int? = null
                    var fill: String? = null
                    var style: Style? = null

                    class Style {
                        var visibility: String? = null
                    }
                }
            }
        }
    }

    class Item {
        var group: String? = null
        var id: String? = null
    }
}

open class BaseDataBody {
    var name: String? = null
    var description: String? = null
    var processor: String? = null
    var executeParams: String? = null
    var priority: Int? = null
    var maxAttemptCnt: Int? = null
    var attemptInterval: Int? = null
    var taskMaxAttemptCnt: Int? = null
    var taskAttemptInterval: Int? = null
    var jobType: String? = null
    var scriptType: String? = null
    var executeMode: String? = null
}

open class BaseGraphDataItem<T : BaseDataBody> {
    var shape: String? = null
    var attrs: Attrs? = null
    var id: String? = null
    var zIndex: Int? = null
    var source: Source? = null
    var target: Target? = null
    var position: Position? = null
    var size: Size? = null
    var view: String? = null
    var ports: Ports? = null
    var data: T? = null
}

// 工作流DAG定义
class WorkflowGraphDataBody : BaseDataBody() {
    var workflowNodeCode: String? = null
}

class WorkflowGraphDataItem : BaseGraphDataItem<WorkflowGraphDataBody>()

class WorkflowGraphData : ArrayList<WorkflowGraphDataItem>()

// 工作流实例DAG定义
class WorkflowInstanceGraphDataBody : BaseDataBody() {
    var workflowNodeCode: String? = null
    var workflowInstanceCode: String? = null
    var workflowNodeInstanceCode: String? = null
    var status: WorkflowStatusEnum? = null

    companion object {
        fun from(workflowGraphDataBody: WorkflowGraphDataBody): WorkflowInstanceGraphDataBody {
            return WorkflowInstanceGraphDataBody().apply {
                this.name = workflowGraphDataBody.name
                this.description = workflowGraphDataBody.description
                this.processor = workflowGraphDataBody.processor
                this.executeParams = workflowGraphDataBody.executeParams
                this.priority = workflowGraphDataBody.priority
                this.maxAttemptCnt = workflowGraphDataBody.maxAttemptCnt
                this.attemptInterval = workflowGraphDataBody.attemptInterval
                this.taskMaxAttemptCnt = workflowGraphDataBody.taskMaxAttemptCnt
                this.taskAttemptInterval = workflowGraphDataBody.taskAttemptInterval
                this.jobType = workflowGraphDataBody.jobType
                this.scriptType = workflowGraphDataBody.scriptType
                this.executeMode = workflowGraphDataBody.executeMode
                this.workflowNodeCode = workflowGraphDataBody.workflowNodeCode
                this.status = WorkflowStatusEnum.WAITING
            }
        }
    }
}

class WorkflowInstanceGraphDataItem : BaseGraphDataItem<WorkflowInstanceGraphDataBody>() {
    companion object {
        fun from(workflowGraphDataItem: WorkflowGraphDataItem): WorkflowInstanceGraphDataItem {
            return WorkflowInstanceGraphDataItem().apply {
                this.shape = if (workflowGraphDataItem.shape == "workflow-node") {
                    "workflow-node-instance"
                } else {
                    workflowGraphDataItem.shape
                }
                this.attrs = workflowGraphDataItem.attrs
                this.id = workflowGraphDataItem.id
                this.zIndex = workflowGraphDataItem.zIndex
                this.source = workflowGraphDataItem.source
                this.target = workflowGraphDataItem.target
                this.position = workflowGraphDataItem.position
                this.size = workflowGraphDataItem.size
                this.view = workflowGraphDataItem.view
                this.ports = workflowGraphDataItem.ports
                this.data = workflowGraphDataItem.data?.let { WorkflowInstanceGraphDataBody.from(it) }
            }
        }
    }
}

class WorkflowInstanceGraphData() : ArrayList<WorkflowInstanceGraphDataItem>() {
    companion object {
        fun from(workflowGraphData: WorkflowGraphData): WorkflowInstanceGraphData {
            val workflowInstanceGraphData = WorkflowInstanceGraphData()
            val workflowInstanceGraphDataItems = workflowGraphData.map {
                WorkflowInstanceGraphDataItem.from(it)
            }
            workflowInstanceGraphData.addAll(workflowInstanceGraphDataItems)
            return workflowInstanceGraphData
        }
    }
}