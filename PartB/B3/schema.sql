CREATE DATABASE IF NOT EXISTS pass_test;
USE pass_test;
CREATE TABLE IF NOT EXISTS bus_pass (
    pass_id VARCHAR(30) PRIMARY KEY,
    reg_no VARCHAR(30) NOT NULL,
    name VARCHAR(80) NOT NULL,
    route_no INT NOT NULL,
    amount DOUBLE NOT NULL
);
