package tech.powerscheduler.worker.persistence

import tech.powerscheduler.common.enums.JobStatusEnum
import tech.powerscheduler.worker.util.ClasspathUtil.readTextFrom
import java.time.LocalDateTime

/**
 * 任务进度持仓库
 *
 * @author grayrat
 * @since 2025/5/25
 */
object TaskProgressRepository {

    private val insertSql = readTextFrom("sql/task_progress/insert.sql")
    private val deleteByIdSql = readTextFrom("sql/task_progress/delete_by_id.sql")
    private val listByTaskIdSql = readTextFrom("sql/task_progress/list_by_job_instance_id.sql")
    private val listDistinctTaskIdSql = readTextFrom("sql/task_progress/list_distinct_task_id.sql")

    fun save(entity: TaskProgressEntity) {
        DataSourceManager.getConnection().use { conn ->
            conn.prepareStatement(insertSql).use { stmt ->
                stmt.setObject(1, null)
                stmt.setObject(2, entity.jobInstanceId)
                stmt.setObject(3, entity.taskId)
                stmt.setString(4, entity.status?.name)
                stmt.setObject(5, entity.startAt)
                stmt.setObject(6, entity.endAt)
                stmt.setString(7, entity.result)
                stmt.setString(8, entity.subTaskListBody)
                stmt.setString(9, entity.subTaskName)
                stmt.executeUpdate()
            }
        }
    }

    fun listDistinctJobInstanceIds(): Set<Long> {
        return DataSourceManager.getConnection().use { conn ->
            return@use conn.prepareStatement(listDistinctTaskIdSql).use { stmt ->
                return@use stmt.executeQuery().use { rs ->
                    return@use generateSequence {
                        rs.takeIf { it.next() }?.getLong("task_id")
                    }.toSet()
                }
            }
        }
    }

    fun listByTaskId(taskId: Long): List<TaskProgressEntity> {
        return DataSourceManager.getConnection().use { conn ->
            conn.prepareStatement(listByTaskIdSql).use { stmt ->
                stmt.setLong(1, taskId)
                stmt.executeQuery().use { rs ->
                    generateSequence {
                        if (rs.next()) {
                            TaskProgressEntity().apply {
                                this.id = rs.getLong("id")
                                this.jobInstanceId = rs.getLong("job_instance_id")
                                this.taskId = rs.getLong("task_id")
                                this.status = rs.getString("status")?.let { JobStatusEnum.valueOf(it) }
                                this.startAt = rs.getObject("start_at", LocalDateTime::class.java)
                                this.endAt = rs.getObject("end_at", LocalDateTime::class.java)
                                this.result = rs.getString("message")
                                this.subTaskListBody = rs.getString("sub_task_list_body")
                                this.subTaskName = rs.getString("sub_task_name")
                            }
                        } else {
                            null
                        }
                    }.toList()
                }
            }
        }
    }

    fun deleteByIds(ids: Iterable<Long>) {
        DataSourceManager.getConnection().use { conn ->
            conn.prepareStatement(deleteByIdSql).use { stmt ->
                for (id in ids) {
                    stmt.setLong(1, id)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        }
    }

}