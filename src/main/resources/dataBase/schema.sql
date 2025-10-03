DROP TABLE IF EXISTS StoredQueries;
DROP TABLE IF EXISTS Titanic;
DROP TABLE IF EXISTS QueryExecutionJob;

CREATE TABLE StoredQueries
(
    id INT PRIMARY KEY AUTO_INCREMENT,
    query   VARCHAR NOT NULL,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE QueryExecutionJob
(
    id INT PRIMARY KEY AUTO_INCREMENT,
    sourceQueryId   VARCHAR NOT NULL,
    status VARCHAR NOT NULL,
    result VARCHAR,
    errorMessage VARCHAR
);

CREATE TABLE Titanic AS SELECT * FROM CSVREAD('src/main/resources/static/titanic.csv');