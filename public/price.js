/* Currency conversion uses dated reference rates, never a guessed exchange rate. */
(function(root){
 function monthlyCny(label, snapshot){
  if(!label)return null;
  const value=label.trim().replace(/,/g,'').toUpperCase();
  const match=value.match(/^(?:(USD|CAD|EUR|CNY|RMB|GBP|HKD|AUD|SGD|JPY|[$€¥￥£])\s*)?(\d+(?:\.\d+)?)\s*(USD|CAD|EUR|CNY|RMB|GBP|HKD|AUD|SGD|JPY)?\s*[/／]\s*(\d+)?\s*(MONTHS?|YEARS?|季度|半年|个月|月|季|年)$/);
  if(!match)return null;
  const currency=({ '$':'USD','€':'EUR','¥':'CNY','￥':'CNY','£':'GBP',RMB:'CNY' })[match[1]||match[3]]||match[1]||match[3];
  if(!currency)return null;
  const period=match[5],months=Number(match[4]||1)*(/^(年|YEARS?)$/.test(period)?12:period==='半年'?6:/季/.test(period)?3:1);
  const rate=currency==='CNY'?1:Number(snapshot?.cnyPerUnit?.[currency]);
  if(!Number.isFinite(rate)||rate<=0||!months)return null;
  return {amount:Number(match[2])*rate/months,currency,months,date:snapshot?.date||null};
 }
 root.StockPrice={monthlyCny};
})(globalThis);
