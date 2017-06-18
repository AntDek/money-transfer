Money transfer
==============

### Run Tests
- `sbt test`

### Run Server
- `sbt stage`
- `sh run.sh`

### Request Example
```sh
# account balance
curl -X GET http://localhost:8080/v1/balance/1

# transfer balance
curl -X POST \
  http://localhost:8080/v1/balance/transfer \
  -d '{
	"fromAccount": 100,
	"toAccount": 1,
	"amount": 1500
}'
```

### Resources
- https://en.wikipedia.org/wiki/Two-phase_commit_protocol
- http://blog.jupo.org/2016/01/28/distributed-transactions-in-actor-systems/
- https://www.slideshare.net/ktoso/distributed-consensus-aka-what-do-we-eat-for-lunch