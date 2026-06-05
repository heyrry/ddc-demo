DROP TABLE IF EXISTS ddc_event_listen;
DROP TABLE IF EXISTS ddc_event;

CREATE TABLE ddc_event (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  gmt_create    TIMESTAMP     NOT NULL,
  gmt_modified  TIMESTAMP     NOT NULL,
  gmt_event     TIMESTAMP     NOT NULL,
  domain        VARCHAR(64)   NOT NULL,
  entity_id     VARCHAR(64)   NOT NULL,
  event         VARCHAR(64)   NOT NULL,
  state         VARCHAR(16)   NOT NULL,
  notify_type   VARCHAR(16)   DEFAULT NULL,
  gmt_notify    TIMESTAMP     DEFAULT NULL,
  notify_id     VARCHAR(64)   DEFAULT NULL,
  notify_result VARCHAR(512)  DEFAULT NULL,
  event_context TEXT          DEFAULT NULL,
  retry_times   INT           NOT NULL DEFAULT 0,
  version       INT           NOT NULL DEFAULT 1,
  local_send    BOOLEAN       DEFAULT NULL,
  remote_send   BOOLEAN       DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE ddc_event_listen (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  gmt_create    TIMESTAMP     NOT NULL,
  gmt_modified  TIMESTAMP     NOT NULL,
  event_id      BIGINT        NOT NULL,
  domain        VARCHAR(64)   NOT NULL,
  event         VARCHAR(64)   NOT NULL,
  event_content TEXT          DEFAULT NULL,
  msg_id        VARCHAR(64)   DEFAULT NULL,
  listen_names  VARCHAR(512)  DEFAULT NULL,
  listen_result BIGINT        NOT NULL DEFAULT 0,
  error_info    VARCHAR(2000) DEFAULT NULL,
  retry_times   INT           NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE (event_id)
);
