# Tiny money transfer server

## Technologies
* Java 8
* Jetty web-server 9.4.19
* Jersey RESTful Web Services 2.29
* H2 database 1.4.199 (embedded mode)
* JUnit 5.5.0-M1
* Mockito 2.15.0

## Description
RESTfull demo server simulates bank client and it's account and provides API for basic operations:
* Add new client
* View client's info
* Open account for client
* View account's info
* Debit account
* Withdraw from account
* Transfer certain amount of money from one account to other client account

All operations are done in parallel allowing multiple requests to the server at the same time.

### Restrictions

* for the sake of simplicity one client can have only one account
* all supported currecies for account: EUR, USD, RUB
* all client/server communications are done using JSON object format
* data stored in in-memory database and lost after server restart

### Transfer objects properties
#### Client data
* id - unique client id
* name - client name
* href - relative API path to client's info

#### Account data
* id - unique id of the account
* clientId - link to client id (owner of the account)
* currency - currency of the account
* amount - balance of the account
* href - relative API path to account's info

#### Debit/Withdraw data
* amountDiff - amount of money to debit (positive integer number) or withdraw (negative integer number) from account.

#### Transfer data
* srcAccountId - source account id to transfer money from
* dstAccountId - destination account id to transfer money to
* amount - amount to transfer

#### Error data
* code - unique code of the error
* msg - message of the error

### List of errors

|Code|Message|
|---|---|
|1  | Client not found |
|2 | Account not found |
|3|Internal SQL error|
|4|Client cannot be created|
|5|Client already exists|
|6|Client name must be provided|
|7|Currency must be provided to create an account|
|8|Client already has an account|
|9|Account cannot be created|
|10|Provided currency for account not supported|
|11|Client is not the owner of this account|
|12|Amount must be provided to update an account|
|13|Account cannot be updated|
|14|Insufficient amount to withdraw from account|
|15|Source account must be provided|
|16|Destination account must be provided|
|17|Amount to transfer must be provided|
|18|Source account not found|
|19|Destination account not found|
|20|Transfer must be done between different accounts|
|21|Currencies in accounts do not match|
|22|Amount to transfer must be a positive number|
|23|Error occurred while updating destination account|
|24|Error occurred while updating source account|
|25|Unexpected server error|

## Installation
Not required

## Run
```
mvn package
java -jar target/tinymoneytransfer-1.0.jar
```

Server starts on localhost at port ``8080``. Context root is ``/api``

## Endpoints
/clients - client and it's account opeartions

/transfers - transfer between 2 accounts

## Operations paths
### /clients
* POST - create new client using Client transport data
### /clients/{clientId}
* GET - retrieve client info
### /clients/{clientId}/account
* POST - create new client account
### /clients/{clientId}/account/{accountId}
* GET - retrieve client account info
* PUT - debit/withdraw from account using Debit/Withdraw transport data
### /transfers
* PUT - transfer money between accounts using Transfer transport data

## Usage (sample commands using curl)

#### Create client
```
curl -d '{"name": "Bob"}' -H "Content-Type: application/json" -H "Accept: application/json" -X POST http://localhost:8080/api/clients
```
Respond:
````
{"id":1,"name":"Bob","href":"/clients/1"}
````
#### Client info
````
 curl -q -H "Content-Type: application/json" -H "Accept: application/json" -X GET http://localhost:8080/api/clients/1
````
Respond:
````
{"id":1,"name":"Bob","href":"/clients/1"}
````
#### Open an account for client
````
curl -d '{"currency":"EUR"}' -H "Content-Type: application/json" -H "Accept: application/json" -X POST http://localhost:8080/api/clients/1/account
````
Respond:
````
{"id":1,"clientId":1,"currency":"EUR","amount":0,"href":"/clients/1/account/1"}
````
#### Debit account
````
curl -d '{"amountDiff":"100"}' -H "Content-Type: application/json" -H "Accept: application/json" -X PUT http://localhost:8080/api/clients/1/account/1
````
Respond
````
{"id":1,"clientId":1,"currency":"EUR","amount":100,"href":"/clients/1/account/1"}
````
#### Transfer between accounts
````
curl -d '{"srcAccountId":"1", "dstAccountId":"2", "amount":"50"}' -H "Content-Type: application/json" -H "Accept: application/json" -X POST http://localhost:8080/api/transfers
````
Respond:
no content
