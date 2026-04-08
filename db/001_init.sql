CREATE TABLE IF NOT EXISTS tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(140) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    due_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);

ALTER TABLE tasks
    DROP CONSTRAINT IF EXISTS status_check;

ALTER TABLE tasks
    ADD CONSTRAINT status_check
    CHECK (status IN ('OPEN','IN_PROGRESS','DONE','BLOCKED'));

ALTER TABLE tasks
    DROP CONSTRAINT IF EXISTS priority_check;

ALTER TABLE tasks
    ADD CONSTRAINT priority_check
    CHECK (priority IN ('LOW','MEDIUM','HIGH'));