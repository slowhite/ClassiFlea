# Blockchain Payment Design

FlapForm Project rely on blockchain to do payment. This is the design doc of the whole process.


## Process
User pick the service and pay for it. He needs to sign a transaction (sender: His address, receiver: FlapForm) and upload the provement of this transaction. 

FlapForm address is a smart contract. It will maintain the balance of user. After receive transaction from user, it will update the deposit balance. Receiver can withdraw tokens from this contract.


## RoadMap
1. Prepare account (User, Receiver, Smart Contract)
2. Deploy Smart Contract
3. DEMO.
    1. Use Python: Create new accounts. Run scripts manually to commit transactions. Maybe need to deploy a Python Server.
    2. Use JS: Use Metamask Provider. Should better fit into our front-end. 






