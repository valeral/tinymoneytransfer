# Tiny money transfer server

RESTFull demo server simulates bank client and it's account and provides API for basic operations:
* Add new client
* View client's info
* Open account for client
* View account's info
* Debit account
* Withdraw from account
* Transfer certain amount of money from one account to other client account

All operations are done in parallel allowing multiple requests to server at the same time.

### Restrictions

* for the sake of simplicity one client can have only one account
* all supported currecies for account: EUR, USD, RUB
* all client/server communications are done using JSON object format

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
* amount - amount of money to debit (positive integer number) or withdraw (negative integer number) from account.

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
`mvn package`
`java -jar target/tinymoneytransfer-1.0.jar`

Server starts on localhost at port 8080
