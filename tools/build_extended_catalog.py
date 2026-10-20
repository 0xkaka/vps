"""Build a reviewable one-time SQL import from saved public pages and catalog JSON.
Usage: python build_extended_catalog.py SOURCE_DIRECTORY CATALOG_JSON OUTPUT_SQL
Requires beautifulsoup4. Does not access credentials or execute SQL.
"""
import sys, json, hashlib, re
from pathlib import Path
from collections import defaultdict
from bs4 import BeautifulSoup


def key(a, b):
    return hashlib.sha256((a + '\n' + b + '\nfalse').encode()).hexdigest()


def value(x):
    if x is None: return 'NULL'
    if isinstance(x, bool): return str(int(x))
    if isinstance(x, int): return str(x)
    return "'" + str(x).replace('\\', '\\\\').replace("'", "''") + "'"


def text(node):
    return node.get_text(' ', strip=True) if node else ''


def href(node):
    a = node.select_one('a[href]')
    return a.get('href', '') if a else ''


def build(source_dir, catalog, output):
    existing = catalog['products']
    candidates = {}
    ignored = 0
    # Prefer each merchant's own matrix. Both matrices contain both merchants.
    for host in ['matrix.bwh91.com', 'matrix.dmitea.com']:
        soup = BeautifulSoup((source_dir / (host + '.html')).read_text(), 'html.parser')
        for row in soup.select('#vpsTable tbody tr'):
            cells = row.select('td')
            if len(cells) != 25: raise ValueError('Matrix column count changed')
            t = [text(c) for c in cells]
            if t[0] not in ['搬瓦工', 'DMIT'] or not t[2] or not t[3]:
                ignored += 1
                continue
            identity = key('matrix:' + t[0] + '\n' + t[2], t[3] + '\n' + href(cells[14]))
            preferred = ('bwh91' in host) == (t[0] == '搬瓦工')
            if identity in candidates and not preferred: continue
            vendor = 1 if t[0] == '搬瓦工' else 2
            notes = ['参考价格：USD ' + t[5] + '/年（以结算页为准）'] if t[5] else []
            for i, label in [(19, '超额后'), (20, '更换机房'), (21, '配置升级'), (22, '类型'), (23, '评价'), (24, '备注')]:
                if t[i] and t[i] != '/': notes.append(label + '：' + t[i])
            if t[4] not in ['有货', '无货']: notes.append('购买限制：' + t[4])
            candidates[identity] = dict(name=f'{t[0]} {t[2]} {t[3]}' + (' [PID ' + href(cells[14]).split('pid=')[-1] + ']' if 'pid=' in href(cells[14]) else ''), vendor_id=vendor,
                parent_id=1 if vendor == 1 else 10, group_flag=False,
                specification=f'{t[7].removesuffix(".0")}C / {t[8]} / {t[9]} / {t[10]} @ {t[11]}',
                route_tags=f'IPv4：{t[12]}；IPv6：{t[13]}', description='；'.join(notes),
                purchase_url=href(cells[14]), source_url='https://' + host + '/',
                source_page='https://' + host + '/', source_key=identity,
                speed_url=href(cells[17]), looking_glass_url=href(cells[18]), review_url=href(cells[16]),
                stock_status={'有货':'IN_STOCK','无货':'OUT_OF_STOCK'}.get(t[4], 'UNKNOWN'),
                interval_seconds=120, last_checked_at=None, monitor_enabled=True, enabled=True)
    matrix_count = len(candidates)
    by_url = defaultdict(list)
    for p in candidates.values():
        if p['purchase_url']: by_url[p['purchase_url']].append(p)
    old_by_url = defaultdict(list)
    for p in existing:
        if not p['groupFlag'] and p['vendorId'] in [1, 2] and '/pid=' in (p['purchaseUrl'] or ''):
            old_by_url[p['purchaseUrl']].append(p)
    merged = {}
    for url, items in by_url.items():
        if len(items) == 1 and len(old_by_url[url]) == 1:
            p = items[0]; old = old_by_url[url][0]; p['id'] = old['id']; merged[old['id']] = p
    # Stock-only sources are authoritative for unambiguous model matches.
    stock_rows = 0
    for host in ['stock.bwh91.com', 'stock.dmitea.com']:
        soup = BeautifulSoup((source_dir / (host + '.html')).read_text(), 'html.parser')
        for row in soup.select('table tbody tr'):
            name = text(row.select_one('.product-name')); buy = row.select_one('.purchase-btn')
            if not name or buy is None: raise ValueError('Stock markup changed')
            stock_rows += 1
            url = buy['href']; identity = key('stock:' + name, url)
            matches = by_url[url]
            # Shared purchase links are not sufficient to merge different models.
            if len(matches) > 1:
                terms = set(re.findall(r'[a-z]+', name.lower()))
                narrowed = [p for p in matches if set(re.findall(r'[a-z]+', p['name'].lower())) & terms - {'dmit','pro','special','kvm','bwh'}]
                matches = narrowed if len(narrowed) == 1 else []
            if len(matches) == 1:
                p = matches[0]
            else:
                old = old_by_url[url]
                if len(old) == 1 and old[0]['id'] in merged:
                    p = merged[old[0]['id']]
                elif len(old) == 1:
                    o=old[0]; p=dict(id=o['id'], name=o['name'], description=o.get('description') or '')
                    merged[o['id']]=p
                else:
                    vendor = 1 if 'bwh91' in host else 2
                    p=dict(name=name, vendor_id=vendor, parent_id=1 if vendor==1 else 10,
                        group_flag=False, specification='',route_tags='',description='独立库存页未提供完整配置，请以购买页为准。',
                        purchase_url=url,source_url='https://'+host+'/',speed_url='',looking_glass_url='',review_url='',
                        interval_seconds=120,monitor_enabled=True,enabled=True)
                    candidates[identity]=p
            badge=row.select_one('.status-badge'); classes=badge.get('class',[])
            p.update(source_page='https://'+host+'/',source_key=identity,
                stock_status='IN_STOCK' if 'status-in-stock' in classes else 'OUT_OF_STOCK' if 'status-out-of-stock' in classes else 'UNKNOWN',
                last_checked_at=text(row.select('.time-info')[-1]),interval_seconds=120)
            # Preserve caveats even when the stock page reports in stock.
            if '邀请码' in name and '邀请码' not in p.get('description',''):
                p['description']=p.get('description','')+'；购买限制：需要邀请码'
    sql=['SET NAMES utf8mb4;', 'START TRANSACTION;']
    emitted=set(); added=0; updated=0; next_id=max(1000,max(p['id'] for p in existing)+1)
    all_items=list(candidates.values())+list(merged.values())
    for p in all_items:
        if id(p) in emitted: continue
        emitted.add(id(p))
        if 'id' in p:
            # Retain the existing display name, grouping, and inventory until a live sync.
            fields={k:v for k,v in p.items() if k not in ['id','name','vendor_id','parent_id','group_flag','stock_status','last_checked_at','monitor_enabled','enabled']}
            sql.append('UPDATE vps_product SET '+','.join('`'+k+'`='+value(v) for k,v in fields.items())+' WHERE id='+str(p['id'])+';')
            updated+=1
        else:
            p['id']=next_id;next_id+=1;p['sort_order']=p['id'];p['stock_status']='UNKNOWN';p['last_checked_at']=None
            sql.append('INSERT INTO vps_product ('+','.join('`'+k+'`' for k in p)+') VALUES ('+','.join(value(v) for v in p.values())+');')
            added+=1
    sql.append('COMMIT;')
    output.write_text('\n'.join(sql)+'\n')
    print(json.dumps(dict(unique_matrix_models=matrix_count,stock_rows=stock_rows,ignored_headers=ignored,existing_enriched=updated,new_products=added,total_records=len(existing)+added),ensure_ascii=False))

if __name__=='__main__':
    build(Path(sys.argv[1]),json.loads(Path(sys.argv[2]).read_text()),Path(sys.argv[3]))
