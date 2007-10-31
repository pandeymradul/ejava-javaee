create table ESALES_ACCT (
    USER_ID varchar(20) not null,
    FIRST_NAME varchar(20) not null,
    primary key (USER_ID)
);

create table ESALES_ACCT_ADDRESS_LINK (
    USER_ID varchar(20) not null,
    ADDRESS_ID integer not null,
    unique (ADDRESS_ID)
);

create table ESALES_ADDRESS (
    ID integer not null identity,
    NAME varchar(10) not null,
    CITY varchar(20) not null,
    primary key (ID)
);

alter table ESALES_ACCT_ADDRESS_LINK
    add constraint ESALES_ACCT_LINK
    foreign key (USER_ID)
    references ESALES_ACCT;

alter table ESALES_ACCT_ADDRESS_LINK
    add constraint ESALES_ADDR_LINK
    foreign key (ADDRESS_ID)
    references ESALES_ADDRESS;
