DROP TABLE IF EXISTS QueryExecutionJob;
DROP TABLE IF EXISTS StoredQueries;
DROP TABLE IF EXISTS authorities;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS Titanic;

CREATE TABLE users
(
    id_users INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50)  NOT NULL,
    password VARCHAR(100) NOT NULL,
    enabled  BOOLEAN      NOT NULL,
    CONSTRAINT uq_username UNIQUE (username)
);

CREATE TABLE authorities
(
    id_authorities INT PRIMARY KEY AUTO_INCREMENT,
    authority      VARCHAR(50) NOT NULL,
    username       VARCHAR(50) NOT NULL,
    user_id        INT,
    CONSTRAINT authorities_users FOREIGN KEY (user_id) REFERENCES users (id_users)
);
CREATE UNIQUE INDEX ix_auth_username ON authorities (username, authority);

CREATE TABLE StoredQueries
(
    id        INT PRIMARY KEY AUTO_INCREMENT,
    query     VARCHAR NOT NULL,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id   INT,
    CONSTRAINT storedQueries_user FOREIGN KEY (user_id) REFERENCES users (id_users)
);

CREATE TABLE QueryExecutionJob
(
    id            INT PRIMARY KEY AUTO_INCREMENT,
    sourceQueryId VARCHAR NOT NULL,
    status        VARCHAR NOT NULL,
    result        VARCHAR,
    errorMessage  VARCHAR,
    createdAt     Date,
    user_id       INT,
    CONSTRAINT jobs_user FOREIGN KEY (user_id) REFERENCES users (id_users)
);

CREATE TABLE Titanic AS
SELECT *
FROM CSVREAD('src/main/resources/static/titanic.csv');