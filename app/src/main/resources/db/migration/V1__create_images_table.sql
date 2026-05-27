CREATE TABLE images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(127) NOT NULL,
    size_bytes BIGINT NOT NULL,
    title VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_images_created_at ON images (created_at DESC);
