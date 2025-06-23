package tech.powerscheduler.server.infrastructure.persistence.repository

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import tech.powerscheduler.common.enums.JobSourceTypeEnum
import tech.powerscheduler.common.enums.JobStatusEnum
import tech.powerscheduler.common.enums.TaskTypeEnum
import tech.powerscheduler.server.domain.common.Page
import tech.powerscheduler.server.domain.common.PageQuery
import tech.powerscheduler.server.domain.job.JobInstanceId
import tech.powerscheduler.server.domain.job.SourceId
import tech.powerscheduler.server.domain.task.Task
import tech.powerscheduler.server.domain.task.TaskId
import tech.powerscheduler.server.domain.task.TaskRepository
import tech.powerscheduler.server.infrastructure.persistence.model.JobInstanceEntity
import tech.powerscheduler.server.infrastructure.persistence.model.TaskEntity
import tech.powerscheduler.server.infrastructure.persistence.repository.impl.TaskRepositoryJpaRepository
import tech.powerscheduler.server.infrastructure.utils.toDomainModel
import tech.powerscheduler.server.infrastructure.utils.toDomainPage
import tech.powerscheduler.server.infrastructure.utils.toEntity

/**
 * @author grayrat
 * @since 2025/6/7
 */
@Repository
class TaskRepositoryRepositoryImpl(
    private val taskRepositoryJpaRepository: TaskRepositoryJpaRepository,
) : TaskRepository {

    override fun lockById(taskId: TaskId): Task? {
        val entity = taskRepositoryJpaRepository.findByIdForUpdate(taskId.value)
        return entity?.toDomainModel()
    }

    override fun findById(taskId: TaskId): Task? {
        val entity = taskRepositoryJpaRepository.findByIdOrNull(taskId.value)
        return entity?.toDomainModel()
    }

    override fun findAllByJobInstanceId(jobInstanceId: JobInstanceId): List<Task> {
        val entities = taskRepositoryJpaRepository.findAllByJobInstanceId(jobInstanceId.value)
        return entities.map { it.toDomainModel() }
    }

    override fun findAllByJobInstanceIdAndBatchAndTaskType(
        jobInstanceId: JobInstanceId,
        batch: Int,
        taskTypes: Collection<TaskTypeEnum>,
        pageQuery: PageQuery
    ): Page<Task> {
        val pageable = PageRequest.of(
            pageQuery.pageNo - 1,
            pageQuery.pageSize,
            Sort.by(JobInstanceEntity::scheduleAt.name).ascending()
        )
        val entities = taskRepositoryJpaRepository.findAllByJobInstanceIdAndBatchAndTaskTypeIn(
            jobInstanceId = jobInstanceId.value,
            batch = batch,
            taskTypes = taskTypes,
            pageable = pageable
        )
        return entities.map { it.toDomainModel() }.toDomainPage()
    }

    override fun findAllByJobInstanceIdAndBatchAndTaskType(
        jobInstanceId: JobInstanceId,
        batch: Int
    ): List<Task> {
        val entities = taskRepositoryJpaRepository.findAllByJobInstanceIdAndBatch(
            jobInstanceId = jobInstanceId.value,
            batch = batch,
        )
        return entities.map { it.toDomainModel() }
    }

    override fun listDispatchable(
        sourceIds: Iterable<SourceId>,
        sourceType: JobSourceTypeEnum,
        pageQuery: PageQuery
    ): Page<Task> {
        val pageable = PageRequest.of(
            pageQuery.pageNo - 1,
            pageQuery.pageSize,
            Sort.by(JobInstanceEntity::scheduleAt.name).ascending()
        )
        val page = taskRepositoryJpaRepository.listDispatchable(
            sourceIds = sourceIds.map { it.value },
            sourceType = sourceType,
            jobStatuses = listOf(JobStatusEnum.WAITING_DISPATCH),
            pageRequest = pageable,
        )
        return page.map { it.toDomainModel() }.toDomainPage()
    }

    override fun findAllUncompletedByWorkerAddress(workerAddress: String): List<Task> {
        val specification = Specification<TaskEntity> { root, _, criteriaBuilder ->
            val workerAddressEqual = criteriaBuilder.equal(
                root.get<String>(TaskEntity::workerAddress.name), workerAddress
            )
            val taskStatusIn = root.get<String>(TaskEntity::taskStatus.name)
                .`in`(JobStatusEnum.UNCOMPLETED_STATUSES)
            criteriaBuilder.and(workerAddressEqual, taskStatusIn)
        }
        val list = taskRepositoryJpaRepository.findAll(specification)
        return list.map { it.toDomainModel() }
    }

    override fun save(task: Task): TaskId {
        val entity = task.toEntity()
        taskRepositoryJpaRepository.save(entity)
        return TaskId(entity.id!!)
    }

    override fun saveAll(taskList: Iterable<Task>): List<TaskId> {
        val entities = taskList.map { it.toEntity() }
        taskRepositoryJpaRepository.saveAll(entities)
        return entities.map { TaskId(it.id!!) }
    }

    override fun deleteByJobInstanceId(jobInstanceIds: Iterable<JobInstanceId>) {
        taskRepositoryJpaRepository.deleteByJobInstanceIdIn(jobInstanceIds.map { it.value })
    }

}