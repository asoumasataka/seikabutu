update ca
set newAmount = obj.newAmount
,oldAmount = obj.oldAmount
from CUSTOMER_ACCOUNT ca
inner join (
	select
	sum(newAmount + #amount + #commission) as newAmount
	,newAmount as oldAmount
	,customerId as customerId
	from CUSTOMER_ACCOUNT
	where customerId = #customerId
	group by customerId,newAmount,customerId
)obj
on obj.customerId = ca.customerId
where ca.customerId = #customerId