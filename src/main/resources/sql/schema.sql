--DROP TABLE IF EXISTS example.NPA;

CREATE TABLE example.NPA(
  CODE CHAR(3) NOT NULL,
  STATE CHAR(2) NOT NULL,
  PRIMARY KEY (CODE, STATE)
);
