


1. 普通預金DBの確認
```
$ docker exec -it savings-db   psql -U savings -d savings -c "SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY 1;"  
 table_name 
------------
 accounts
(1 row)


What's next:
    Try Docker Debug for seamless, persistent debugging tools in any container or image → docker debug savings-db
    Learn more at https://docs.docker.com/go/debug-cli/
```
```
$ docker exec -it savings-db   psql -U savings -d savings -c "SELECT id, owner, balance, created_at
      FROM accounts
      ORDER BY id DESC
      LIMIT 10;"
                  id                  | owner | balance |          created_at
--------------------------------------+-------+---------+-------------------------------
 03ab7f01-80ef-45c0-92f7-c3cfe140d817 | demo  |    0.00 | 2025-09-11 12:44:09.035989+00
(1 row)


What's next:
    Try Docker Debug for seamless, persistent debugging tools in any container or image → docker debug savings-db
    Learn more at https://docs.docker.com/go/debug-cli/
```



2. 定期預金DB確認
```

$ docker exec -it timedeposit-db \
  psql -U timedeposit -d timedeposit \
  -c "SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY 1;"  
  table_name   
---------------
 time_deposits
(1 row)


What's next:
    Try Docker Debug for seamless, persistent debugging tools in any container or image → docker debug timedeposit-db
    Learn more at https://docs.docker.com/go/debug-cli/

```
```
$ docker exec -it timedeposit-db   psql -U timedeposit -d timedeposit   -c "SELECT id, owner, principal, status, start_at
      FROM time_deposits
      ORDER BY start_at DESC
      LIMIT 10;"

What's next:
    Try Docker Debug for seamless, persistent debugging tools in any container or image → docker debug timedeposit-db
    Learn more at https://docs.docker.com/go/debug-cli/

$


                  id                  | owner  | principal | status |           start_at         

--------------------------------------+--------+-----------+--------+-------------------------------
 382c85f4-df78-4f11-b539-9fe834ac3f6e | demo   |  10000.00 | OPEN   | 2025-09-06 10:22:22.43919+00
 a8cf0665-5512-4e10-a1f8-5a0e2f1756e5 | demo   |  10000.00 | OPEN   | 2025-09-06 10:22:14.613697+:
```





