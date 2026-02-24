-- users
CREATE TABLE users (
    id                   BIGSERIAL PRIMARY KEY,
    username             VARCHAR(50)  UNIQUE NOT NULL,
    email                VARCHAR(255) UNIQUE NOT NULL,
    name                 VARCHAR(100) NOT NULL,
    age                  INT CHECK (age >= 18 AND age <= 150),
    profile_description  TEXT,
    relationship_status  VARCHAR(50),
    profile_picture1     VARCHAR(500),
    profile_picture2     VARCHAR(500),
    profile_picture3     VARCHAR(500),
    profile_picture4     VARCHAR(500),
    is_fraud             BOOLEAN DEFAULT FALSE,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- profile_visits
CREATE TABLE profile_visits (
    id          BIGSERIAL PRIMARY KEY,
    visitor_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    visited_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    visited_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- user_likes
CREATE TABLE user_likes (
    id        BIGSERIAL PRIMARY KEY,
    liker_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    liked_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    liked_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(liker_id, liked_id)
);

-- fraud_records — written when a user is flagged, keeps detection context
CREATE TABLE fraud_records (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    visit_count          BIGINT NOT NULL,
    like_count           BIGINT NOT NULL,
    time_window_minutes  INT NOT NULL,
    detected_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- events
CREATE TABLE events (
    id            BIGSERIAL PRIMARY KEY,
    event_time    TIMESTAMP NOT NULL,
    location      VARCHAR(255) NOT NULL,
    description   TEXT,
    organizer_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- indexes
CREATE INDEX idx_visits_visited_id  ON profile_visits(visited_id);
CREATE INDEX idx_visits_visited_at  ON profile_visits(visited_at);
CREATE INDEX idx_visits_visitor_id  ON profile_visits(visitor_id);
CREATE INDEX idx_likes_liked_id     ON user_likes(liked_id);
CREATE INDEX idx_likes_liker_id     ON user_likes(liker_id);
CREATE INDEX idx_users_is_fraud     ON users(is_fraud) WHERE is_fraud = FALSE;
CREATE INDEX idx_fraud_records_user ON fraud_records(user_id);

-- =====================================================
-- Query: get all profile visitors of a user, most recent first
-- =====================================================
SELECT
    u.id, u.username, u.name, u.age, pv.visited_at AS visit_time
FROM users u
INNER JOIN profile_visits pv ON u.id = pv.visitor_id
WHERE pv.visited_id = ?
ORDER BY pv.visited_at DESC;

-- =====================================================
-- Query: fraud candidates (visit + like 100+ in 10 mins)
-- =====================================================
SELECT
    pv.visitor_id,
    COUNT(DISTINCT pv.visited_id)  AS visited_count,
    COUNT(DISTINCT ul.liked_id)    AS liked_count,
    MIN(pv.visited_at)             AS first_visit,
    MAX(pv.visited_at)             AS last_visit
FROM profile_visits pv
LEFT JOIN user_likes ul
    ON pv.visitor_id = ul.liker_id
    AND ul.liked_at >= pv.visited_at
    AND ul.liked_at <= pv.visited_at + INTERVAL '10 minutes'
WHERE pv.visited_at >= NOW() - INTERVAL '10 minutes'
GROUP BY pv.visitor_id
HAVING COUNT(DISTINCT pv.visited_id) >= 100
   AND COUNT(DISTINCT ul.liked_id)   >= 100;
