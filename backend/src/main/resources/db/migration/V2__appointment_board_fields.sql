ALTER TABLE appointment_record
  ADD COLUMN global_sequence BIGINT NOT NULL DEFAULT 0;

ALTER TABLE appointment_record
  ADD COLUMN display_sequence BIGINT NOT NULL DEFAULT 0;

ALTER TABLE appointment_record
  ADD COLUMN company_name VARCHAR(120) NOT NULL DEFAULT '集团公司';

ALTER TABLE appointment_record
  ADD COLUMN department_name VARCHAR(120) NOT NULL DEFAULT '党群人力部';

ALTER TABLE appointment_record
  ADD COLUMN political_status VARCHAR(80) NULL;

ALTER TABLE appointment_record
  ADD COLUMN marital_status VARCHAR(40) NULL;

ALTER TABLE appointment_record
  ADD COLUMN remark TEXT NULL;

ALTER TABLE appointment_record
  ADD COLUMN full_time_education_degree VARCHAR(120) NULL;

ALTER TABLE appointment_record
  ADD COLUMN full_time_school VARCHAR(160) NULL;

ALTER TABLE appointment_record
  ADD COLUMN full_time_major VARCHAR(160) NULL;

ALTER TABLE appointment_record
  ADD COLUMN part_time_education VARCHAR(160) NULL;

ALTER TABLE appointment_record
  ADD COLUMN part_time_degree VARCHAR(120) NULL;

ALTER TABLE appointment_record
  ADD COLUMN part_time_school VARCHAR(160) NULL;

ALTER TABLE appointment_record
  ADD COLUMN part_time_major VARCHAR(160) NULL;

UPDATE appointment_record
SET current_position = position_name
WHERE (current_position IS NULL OR current_position = '')
  AND position_name IS NOT NULL
  AND position_name <> '';

UPDATE appointment_record
SET full_time_school = graduation_school
WHERE (full_time_school IS NULL OR full_time_school = '')
  AND graduation_school IS NOT NULL
  AND graduation_school <> '';

UPDATE appointment_record
SET global_sequence = id
WHERE global_sequence = 0;

UPDATE appointment_record
SET display_sequence = id
WHERE display_sequence = 0;

CREATE INDEX idx_appointment_record_company_sequence ON appointment_record(company_name, global_sequence);
CREATE INDEX idx_appointment_record_display_sequence ON appointment_record(display_sequence);
