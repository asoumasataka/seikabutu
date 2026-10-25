select
bank_name.bankName
,bank_name.bankChildName
,bank.accountKouzaId
,company_name.companyName
,bank.customerId
,transaction_history.bankId_send
,transaction_history.accountKouzaId_send
,transaction_history.bankId_receive
,transaction_history.accountKouzaId_receive
,transaction_history.amount
,transaction_history.commission
,transaction_history.transactionDay
from BANK bank
inner join BANK_NAME bank_name
on bank.bankId = bank_name.bankId
and bank.bankChildId = bank_name.bankChildId
inner join COMPANY_NAME company_name
on bank.companyId = company_name.companyId
-- send
inner join TRANSACTION_HISTORY transaction_history
on bank.accountKouzaId = transaction_history.accountKouzaId_send
