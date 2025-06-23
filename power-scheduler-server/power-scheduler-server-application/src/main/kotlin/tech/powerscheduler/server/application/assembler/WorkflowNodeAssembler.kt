package tech.powerscheduler.server.application.assembler

import org.springframework.stereotype.Component
import tech.powerscheduler.server.application.dto.request.WorkflowNodeDTO
import tech.powerscheduler.server.domain.workflow.Workflow
import tech.powerscheduler.server.domain.workflow.WorkflowNode

/**
 * @author grayrat
 * @since 2025/6/23
 */
@Component
class WorkflowNodeAssembler {

    fun toDomainModel4AddRequest(
        workflow: Workflow,
        nodes: List<WorkflowNodeDTO>,
    ): List<WorkflowNode> {
        val nodeCode2workflowNode = nodes.associate {
            Pair(
                it.workflowNodeCode,
                toDomainModel4Add(
                    workflow = workflow,
                    currentNode = it,
                )
            )
        }
        nodes.forEach {
            val workflowNode = nodeCode2workflowNode[it.workflowNodeCode]!!
            val children = it.workflowNodeChildCodes.mapNotNull { nodeCode -> nodeCode2workflowNode[nodeCode] }
            workflowNode.children = children.toSet()
        }
        return nodeCode2workflowNode.values.toList()
    }

    fun toDomainModel4EditRequest(
        workflow: Workflow,
        nodes: List<WorkflowNodeDTO>,
        existNodes: List<WorkflowNode>,
    ): List<WorkflowNode> {
        val nodeCode2existNode = existNodes.associateBy { it.code!! }
        val nodeCode2workflowNode = nodes.associate {
            Pair(
                it.workflowNodeCode,
                if (nodeCode2existNode.containsKey(it.workflowNodeCode)) {
                    toDomainModel4Edit(
                        workflow = workflow,
                        currentNode = it,
                        existNode = nodeCode2existNode[it.workflowNodeCode]!!,
                    )
                } else {
                    toDomainModel4Add(
                        workflow = workflow,
                        currentNode = it,
                    )
                }
            )
        }

        nodes.forEach {
            val workflowNode = nodeCode2workflowNode[it.workflowNodeCode]!!
            val children = it.workflowNodeChildCodes.mapNotNull { childNodeCode -> nodeCode2workflowNode[childNodeCode] }
            workflowNode.children = children.toSet()
        }
        return nodeCode2workflowNode.values.toList()
    }

    fun toDomainModel4Add(
        workflow: Workflow,
        currentNode: WorkflowNodeDTO,
    ): WorkflowNode {
        return WorkflowNode().apply {
            this.workflow = workflow
            this.code = currentNode.workflowNodeCode
            this.name = currentNode.name
            this.description = currentNode.description
            this.jobType = currentNode.jobType
            this.processor = currentNode.processor
            this.executeMode = currentNode.executeMode
            this.executeParams = currentNode.executeParams
            this.scriptType = currentNode.scriptType
            this.scriptCode = currentNode.scriptCode
            this.maxAttemptCnt = currentNode.maxAttemptCnt
            this.attemptInterval = currentNode.attemptInterval
            this.taskMaxAttemptCnt = currentNode.taskMaxAttemptCnt
            this.taskAttemptInterval = currentNode.taskAttemptInterval
            this.priority = currentNode.priority
        }
    }

    fun toDomainModel4Edit(
        workflow: Workflow,
        currentNode: WorkflowNodeDTO,
        existNode: WorkflowNode,
    ): WorkflowNode {
        return WorkflowNode().apply {
            this.workflow = workflow
            this.id = existNode.id
            this.code = currentNode.workflowNodeCode
            this.name = currentNode.name
            this.description = currentNode.description
            this.jobType = currentNode.jobType
            this.processor = currentNode.processor
            this.executeMode = currentNode.executeMode
            this.executeParams = currentNode.executeParams
            this.scriptType = currentNode.scriptType
            this.scriptCode = currentNode.scriptCode
            this.maxAttemptCnt = currentNode.maxAttemptCnt
            this.attemptInterval = currentNode.attemptInterval
            this.taskMaxAttemptCnt = currentNode.taskMaxAttemptCnt
            this.taskAttemptInterval = currentNode.taskAttemptInterval
            this.priority = currentNode.priority
        }
    }

}