"""Public, anonymous retailer catalog adapters. No account or checkout operations."""
import argparse, concurrent.futures, datetime, http.cookiejar, json, re, unicodedata, urllib.parse, urllib.request
from pathlib import Path
MERC_CATEGORIES=[112,115,117,118,120,121,126,77,29,27,38,31,56,53,72,104,59,78,122,34,60,142,140,37]
DIA_TERMS=['arroz','spaghetti','pechuga pollo','huevos','patatas','cebolla','tomate','zanahoria','pimiento','calabacín','brócoli','espinacas','garbanzos','lentejas','atún','salmón','tofu','queso rallado','yogur natural','limón','ajo','aceite oliva','pan','salsa soja','leche','avena','champiñones','kimchi','udon','comino','alubias cocidas','lomo de cerdo','obleas arroz']
def request(url,method='GET',data=None,opener=None):
 req=urllib.request.Request(url,data=data,method=method,headers={'Accept':'application/json','Content-Type':'application/json','User-Agent':'InhouseCook/2.0'})
 return (opener or urllib.request.build_opener()).open(req,timeout=18)
def quantity(name,unit=None,size=None):
 if size and unit:
  if unit.lower() in ('kg','l'):return float(size)*1000, 'ml' if unit.lower()=='l' else 'g'
  return float(size), 'ud' if unit.lower() in ('ud','uds','unidades') else unit.lower()
 m=list(re.finditer(r'(\d+(?:[.,]\d+)?)\s*(kg|ml|g|l|litros?|ud\.?|unidades?)\b',name.lower()))
 if m:
  m=m[-1];v=float(m[1].replace(',','.'));u=m[2];prefix=name.lower()[:m.start()];multi=re.search(r'(\d+)\s*[x×]\s*$',prefix);v*=int(multi[1]) if multi else 1;return (v*1000,'g') if u=='kg' else ((v*1000,'ml') if u.startswith('l') else (v, 'ud' if u.startswith('u') else u))
 return 1,'ud'
def mercadona(postal):
 with request('https://tienda.mercadona.es/api/postal-codes/actions/change-pc/','PUT',json.dumps({'new_postal_code':postal}).encode()) as r: wh=r.headers.get('x-customer-wh')
 if not wh:raise ValueError('Mercadona no ha confirmado el centro de distribución para ese código postal')
 out={}
 def load(i):
  try:
   with request(f'https://tienda.mercadona.es/api/categories/{i}/?wh={urllib.parse.quote(wh)}') as r:return json.load(r)
  except Exception:return {}
 def walk(x):
  if isinstance(x,dict):
   for p in x.get('products',[]):
    pi=p['price_instructions'];q,u=quantity(p['display_name'],pi.get('size_format'),pi.get('drained_weight') or pi.get('unit_size'))
    price=pi.get('unit_price')
    if not price:continue
    out[p['id']]={'id':p['id'],'name':p['display_name'],'price':float(price),'quantity':q,'unit':u,'pack':p.get('packaging') or 'Unidad','image':p.get('thumbnail',''),'url':p.get('share_url',''),'store':'mercadona','drained':bool(pi.get('drained_weight')),'approximateWeight':bool(pi.get('approx_size'))}
   for k,v in x.items():
    if k!='products' and isinstance(v,(dict,list)):walk(v)
  elif isinstance(x,list):
   for v in x:walk(v)
 with concurrent.futures.ThreadPoolExecutor(max_workers=4) as ex:
  for x in ex.map(load,MERC_CATEGORIES):walk(x)
 return {'store':'mercadona','postal':postal,'warehouse':wh,'products':list(out.values()),'updatedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'locationVerified':True}
def dia(postal):
 cookies=http.cookiejar.CookieJar();opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies))
 with request(f'https://www.dia.es/api/v1/common-aggregator/save-shipping-address?new_postal_code={postal}&skip_dry_run=true','PUT',b'{}',opener) as r:r.read()
 out={}
 for term in DIA_TERMS:
  try:
   with request('https://www.dia.es/api/v1/search-back/search/reduced?q='+urllib.parse.quote(term),opener=opener) as r:data=json.load(r)
   if str(data.get('cart',{}).get('postal_code'))!=postal:raise ValueError('DIA no ha confirmado el código postal')
   for p in data.get('search_items',[]):
    if p.get('units_in_stock',1)==0:continue
    pr=p.get('prices',{});price=pr.get('price')
    if price is None:continue
    q,u=quantity(p['display_name']);out[p['object_id']]={'id':p['object_id'],'name':p['display_name'],'price':float(price),'quantity':q,'unit':u,'pack':f'{q:g} {u}','image':'https://www.dia.es'+p['image'],'url':'https://www.dia.es'+p['url'],'store':'dia'}
  except urllib.error.HTTPError:continue
 return {'store':'dia','postal':postal,'products':list(out.values()),'updatedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'locationVerified':True}
if __name__=='__main__':
 ap=argparse.ArgumentParser();ap.add_argument('--postal',default='28001');ap.add_argument('--out',default='app/src/main/assets/catalog-seed.json');args=ap.parse_args()
 results=[]
 for provider in [mercadona,dia]:
  r=provider(args.postal);print(r['store'],len(r['products']),r.get('warehouse',''));results.append(r)
 Path(args.out).write_text(json.dumps(results,ensure_ascii=False,separators=(',',':')))
