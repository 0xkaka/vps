/* Purchase links only. Source URLs remain untouched for inventory monitoring. */
(function(root){
 const merchants={
  1:{base:'https://bandwagonhost.com/aff.php',id:'85031',hosts:['bandwagonhost.com','bwh91.com']},
  2:{base:'https://www.dmit.io/aff.php',id:'29171',hosts:['www.dmit.io','dmit.io','dmitea.com']},
  7:{base:'https://app.vmiss.com/aff.php',id:'7151',hosts:['app.vmiss.com']},
  8:{base:'https://gomami.io/aff.php',id:'974',hosts:['gomami.io']},
  9:{base:'https://vps.hosting/',id:'2594',hosts:['vps.hosting']}
 };
 function resolve(product,regionUrl){
  const m=merchants[product.vendorId];
  if(!m||/抽奖|Racknerd等更多/i.test(product.name||''))return {url:regionUrl||product.purchaseUrl,sponsored:false};
  const target=new URL(m.base);target.searchParams.set(product.vendorId===9?'affid':'aff',m.id);
  for(const candidate of [regionUrl,product.sourceUrl,product.purchaseUrl]){
   let u;try{u=new URL(candidate)}catch{continue}
   if(!['http:','https:'].includes(u.protocol)||!m.hosts.includes(u.hostname)||u.username||u.password)continue;
   const id=product.vendorId===9?u.searchParams.get('id'):(u.searchParams.get('pid')||(product.vendorId===1&&u.hostname==='bwh91.com'&&u.pathname==='/jp'?'162':null)||u.pathname.match(/^\/pid=(\d+)\/?$/)?.[1]);
   if(!id||!/^\d+$/.test(id))continue;
   if(product.vendorId===9){target.searchParams.set('cmd','cart');target.searchParams.set('action','add');target.searchParams.set('id',id)}
   else target.searchParams.set('pid',id);
   break;
  }
  return {url:target.href,sponsored:true,regionSelection:!!regionUrl};
 }
 const api={resolve};if(typeof module==='object'&&module.exports)module.exports=api;else root.StockAffiliate=api;
})(typeof globalThis==='object'?globalThis:this);
