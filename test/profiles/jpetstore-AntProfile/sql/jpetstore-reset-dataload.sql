USE jpetstore;

-- empty tables
DELETE FROM account;
DELETE FROM inventory;
DELETE FROM lineitem;
DELETE FROM orders;
DELETE FROM orderstatus;
DELETE FROM profile;
DELETE FROM sequence;
DELETE FROM signon;

-- insert data
INSERT INTO sequence VALUES('ordernum', 1000);
INSERT INTO sequence VALUES('linenum', 1000);

INSERT INTO signon VALUES('j2ee','j2ee');
INSERT INTO signon VALUES('ACID','ACID');

INSERT INTO account VALUES('j2ee','yourname@yourdomain.com','ABC', 'XYX', 'OK', '901 San Antonio Road', 'MS UCUP02-206', 'Palo Alto', 'CA', '94303', 'USA',  '555-555-5555');
INSERT INTO account VALUES('ACID','acid@yourdomain.com','ABC', 'XYX', 'OK', '901 San Antonio Road', 'MS UCUP02-206', 'Palo Alto', 'CA', '94303', 'USA',  '555-555-5555');

INSERT INTO profile VALUES('j2ee','english','DOGS',1,1);
INSERT INTO profile VALUES('ACID','english','CATS',1,1);

INSERT INTO inventory (itemid, qty ) VALUES ('EST-1',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-2',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-3',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-4',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-5',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-6',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-7',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-8',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-9',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-10',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-11',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-12',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-13',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-14',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-15',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-16',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-17',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-18',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-19',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-20',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-21',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-22',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-23',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-24',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-25',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-26',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-27',999999);
INSERT INTO inventory (itemid, qty ) VALUES ('EST-28',999999);

