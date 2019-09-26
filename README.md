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
#### Client object
* id - unique client id
* name - client name
* href - relative API path to client's info

#### Account
* id - unique id of the account
* clientId - link to client id (owner of the account)
* currency - currency of the account
* amount - balance of the account
* href - relative API path to account's info

#### Debit/Withdraw data
* amount - amount of money to debit (positive integer number) or withdraw (negative integer number) from account.

#### Transfer data
* srcAccountId
* dstAccountId
* amount
