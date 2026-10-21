const {test}=require('node:test');const assert=require('node:assert/strict');const {resolve}=require('../affiliate.js');
test('all five merchants replace third party IDs and preserve product IDs',()=>{
 for(const [vendorId,sourceUrl,key,expected] of [[1,'https://bwh91.com/pid=87','aff','85031'],[2,'https://dmitea.com/pid=100','aff','29171'],[7,'https://app.vmiss.com/aff.php?aff=5896&pid=44','aff','7151'],[8,'https://gomami.io/aff.php?aff=661&pid=26','aff','974'],[9,'https://vps.hosting/?cmd=cart&action=add&affid=1706&id=144','affid','2594']]){
  const result=resolve({vendorId,sourceUrl,purchaseUrl:'https://vpszdm.com/short'});const u=new URL(result.url);assert.equal(u.searchParams.get(key),expected);assert.ok(u.searchParams.get(vendorId===9?'id':'pid'));assert.ok(result.sponsored);
  assert.equal(resolve({vendorId,purchaseUrl:result.url}).url,result.url);
 }
});
test('region product wins; select region and term at checkout',()=>{const r=resolve({vendorId:1,sourceUrl:'https://bwh91.com/pid=87'},'https://bandwagonhost.com/cart.php?a=add&pid=44&billingcycle=annually');assert.equal(new URL(r.url).searchParams.get('pid'),'44');assert.ok(r.regionSelection)});
test('untrusted and wrong merchant URLs cannot supply product IDs',()=>{for(const sourceUrl of ['https://bandwagonhost.com.evil.test/?pid=9','javascript:alert(1)','https://app.vmiss.com/?pid=9','https://user@bandwagonhost.com/?pid=9'])assert.equal(resolve({vendorId:1,sourceUrl}).url,'https://bandwagonhost.com/aff.php?aff=85031')});
test('unconfigured merchants remain unchanged',()=>{assert.deepEqual(resolve({vendorId:3,purchaseUrl:'https://example.com/item'}),{url:'https://example.com/item',sponsored:false})});

test('known Japan short link resolves without another affiliate hop',()=>assert.equal(new URL(resolve({vendorId:1,purchaseUrl:'https://bwh91.com/jp'}).url).searchParams.get('pid'),'162'));
test('lottery entries are not product purchase links',()=>assert.equal(resolve({vendorId:2,name:'🎉抽奖专用',purchaseUrl:'https://lottery.dmitea.com/lottery-guide?lottery_id=2'}).sponsored,false));
