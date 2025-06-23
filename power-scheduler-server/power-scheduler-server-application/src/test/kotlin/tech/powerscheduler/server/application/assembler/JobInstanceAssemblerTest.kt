package tech.powerscheduler.server.application.assembler

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import tech.powerscheduler.common.enums.*
import tech.powerscheduler.server.application.dto.request.JobInstanceQueryRequestDTO
import tech.powerscheduler.server.domain.appgroup.AppGroup
import tech.powerscheduler.server.domain.appgroup.AppGroupId
import tech.powerscheduler.server.domain.job.JobId
import tech.powerscheduler.server.domain.job.JobInstance
import tech.powerscheduler.server.domain.job.JobInstanceId
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class JobInstanceAssemblerTest : FunSpec({

    val jobInstanceAssembler = JobInstanceAssembler()

    context("test ${JobInstanceAssembler::toDomainQuery.name}") {
        test("should assemble correct query") {
            val jobInstanceQueryRequestDTO = JobInstanceQueryRequestDTO().also {
                it.appCode = "appCode"
                it.jobName = "jobName"
            }
            val result = shouldNotThrowAny {
                jobInstanceAssembler.toDomainQuery(jobInstanceQueryRequestDTO)
            }
            result.shouldNotBeNull()
            result.appCode shouldBe jobInstanceQueryRequestDTO.appCode
            result.jobName shouldBe jobInstanceQueryRequestDTO.jobName
        }
    }

    context("test ${JobInstanceAssembler::toJobInstanceQueryResponseDTO.name}") {
        test("should assemble correct DTO") {
            val appGroup = AppGroup().also {
                it.id = AppGroupId(1L)
                it.code = "appCode"
                it.name = "appName"
            }
            val jobInstance = JobInstance().also {
                it.appGroup = appGroup
                it.id = JobInstanceId(1L)
                it.sourceId = JobId(2L)
                it.jobName = "jobName"
                it.jobType = JobTypeEnum.JAVA
                it.processor = "processor"
                it.jobStatus = JobStatusEnum.entries.random()
                it.scheduleAt = LocalDateTime.now().minusMinutes(10)
                it.startAt = LocalDateTime.now().minusMinutes(8)
                it.endAt = LocalDateTime.now().minusMinutes(5)
                it.executeParams = "executeParams"
                it.executeMode = ExecuteModeEnum.entries.random()
                it.scheduleType = ScheduleTypeEnum.FIX_RATE
                it.message = "message"
                it.dataTime = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
                it.scriptType = ScriptTypeEnum.PYTHON
                it.scriptCode = "scriptCode"
                it.attemptCnt = (0..100).random()
                it.priority = (0..100).random()
            }
            val result = shouldNotThrowAny {
                jobInstanceAssembler.toJobInstanceQueryResponseDTO(jobInstance)
            }
            result.id shouldBe jobInstance.id!!.value
            result.jobId shouldBe jobInstance.sourceId?.value
            result.appCode shouldBe appGroup.code
            result.jobName shouldBe jobInstance.jobName
            result.jobType?.code shouldBe jobInstance.jobType?.name
            result.processor shouldBe jobInstance.processor
            result.jobStatus?.code shouldBe jobInstance.jobStatus?.name
            result.scheduleAt shouldBe jobInstance.scheduleAt
            result.startAt shouldBe jobInstance.startAt
            result.endAt shouldBe jobInstance.endAt
            result.executeParams shouldBe jobInstance.executeParams
            result.executeMode?.code shouldBe jobInstance.executeMode?.name
            result.scheduleType?.code shouldBe jobInstance.scheduleType?.name
            result.message shouldBe jobInstance.message
            result.dataTime shouldBe jobInstance.dataTime
            result.scriptType?.code shouldBe jobInstance.scriptType?.name
            result.scriptCode shouldBe jobInstance.scriptCode
            result.attemptCnt shouldBe jobInstance.attemptCnt
            result.priority shouldBe jobInstance.priority
            result.createdBy shouldBe jobInstance.createdBy
            result.createdAt shouldBe jobInstance.createdAt
            result.updatedBy shouldBe jobInstance.updatedBy
            result.updatedAt shouldBe jobInstance.updatedAt
        }
    }

    context("test ${JobInstanceAssembler::toJobInstanceQueryDetailDTO.name}") {
        test("should assemble correct DTO") {
            val appGroup = AppGroup().also {
                it.id = AppGroupId(1L)
                it.code = "appCode"
                it.name = "appName"
            }
            val jobInstance = JobInstance().also {
                it.appGroup = appGroup
                it.id = JobInstanceId(1L)
                it.sourceId = JobId(2L)
                it.jobName = "jobName"
                it.jobType = JobTypeEnum.JAVA
                it.processor = "processor"
                it.jobStatus = JobStatusEnum.entries.random()
                it.scheduleAt = LocalDateTime.now().minusMinutes(30)
                it.startAt = LocalDateTime.now().minusMinutes(20)
                it.endAt = LocalDateTime.now().minusMinutes(10)
                it.executeParams = "executeParams"
                it.executeMode = ExecuteModeEnum.entries.random()
                it.scheduleType = ScheduleTypeEnum.FIX_RATE
                it.message = "message"
                it.dataTime = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
                it.scriptType = ScriptTypeEnum.PYTHON
                it.scriptCode = "scriptCode"
                it.attemptCnt = (0..100).random()
                it.priority = (0..100).random()
            }
            val result = shouldNotThrowAny {
                jobInstanceAssembler.toJobInstanceQueryDetailDTO(jobInstance)
            }
            result.id shouldBe jobInstance.id!!.value
            result.jobId shouldBe jobInstance.sourceId?.value
            result.appCode shouldBe appGroup.code
            result.jobName shouldBe jobInstance.jobName
            result.jobType?.code shouldBe jobInstance.jobType?.name
            result.processor shouldBe jobInstance.processor
            result.jobStatus?.code shouldBe jobInstance.jobStatus?.name
            result.scheduleAt shouldBe jobInstance.scheduleAt
            result.startAt shouldBe jobInstance.startAt
            result.endAt shouldBe jobInstance.endAt
            result.executeParams shouldBe jobInstance.executeParams
            result.executeMode?.code shouldBe jobInstance.executeMode?.name
            result.scheduleType?.code shouldBe jobInstance.scheduleType?.name
            result.message shouldBe jobInstance.message
            result.dataTime shouldBe jobInstance.dataTime
            result.scriptType?.code shouldBe jobInstance.scriptType?.name
            result.scriptCode shouldBe jobInstance.scriptCode
            result.attemptCnt shouldBe jobInstance.attemptCnt
            result.priority shouldBe jobInstance.priority
        }
    }

})
