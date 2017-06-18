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