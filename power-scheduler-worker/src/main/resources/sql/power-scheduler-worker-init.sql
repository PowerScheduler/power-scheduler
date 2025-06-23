DROP TABLE IF EXISTS task_progress;

CREATE TABLE task_progress
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id             BIGINT,
    job_instance_id    BIGINT      NOT NULL,
    task_id            BIGINT      NOT NULL,
    status             VARCHAR(50) NOT NULL,
    start_at           DATETIME,
    end_at             DATETIME,
    message            TEXT,
    sub_task_list_body TEXT,
    sub_task_name      TEXT
);
