package com.inhouse.cook;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/** Anonymous, session-scoped public catalog queries; no account or checkout actions. */
public final class CatalogClient {
    public interface Progress {void report(String text);}
    private final Progress progress;
    public CatalogClient(Progress progress){this.progress=progress;}
    public JSONObject load(String store,String postal)throws Exception{
        if(!postal.matches("\\d{5}")||Integer.parseInt(postal.substring(0,2))<1||Integer.parseInt(postal.substring(0,2))>52)throw new Exception("Código postal no válido.");
        if(store.equals("mercadona"))return mercadona(postal);
        if(store.equals("dia"))return dia(postal);
        throw new Exception("Supermercado no disponible.");
    }
    public String page(String url)throws Exception{return request(url,"GET",null,null).body;}
    private static final int[] CATEGORIES={112,115,117,118,120,121,126,77,29,27,38,31,56,53,72,104,59,78,122,34,60,142,140,37};
    private static final String[] TERMS={"arroz","spaghetti","pechuga pollo","huevos","patatas","cebolla","tomate","zanahoria","pimiento","calabacín","brócoli","espinacas","garbanzos","lentejas","atún","salmón","tofu","queso rallado","yogur natural","limón","ajo","aceite oliva","pan","salsa soja","leche","avena","champiñones","kimchi","udon","comino","alubias cocidas","lomo de cerdo","obleas arroz"};
    private static class Response {String body;Map<String,List<String>> headers;String header(String key){for(Map.Entry<String,List<String>> e:headers.entrySet())if(e.getKey()!=null&&e.getKey().equalsIgnoreCase(key))return e.getValue().get(0);return null;}}
    private Response request(String url,String method,String body,CookieManager cookies)throws Exception{
        URL target=new URL(url);if(!"https".equals(target.getProtocol()))throw new Exception("Solo se admiten enlaces HTTPS.");
        HttpURLConnection conn=(HttpURLConnection)target.openConnection();conn.setConnectTimeout(16000);conn.setReadTimeout(20000);conn.setRequestMethod(method);conn.setRequestProperty("Accept","application/json,text/html;q=0.9");conn.setRequestProperty("User-Agent","InhouseCook/2.0 (Android)");
        if(cookies!=null)for(Map.Entry<String,List<String>> h:cookies.get(target.toURI(),new LinkedHashMap<>()).entrySet())conn.setRequestProperty(h.getKey(),String.join("; ",h.getValue()));
        if(body!=null){conn.setDoOutput(true);conn.setRequestProperty("Content-Type","application/json");byte[] bytes=body.getBytes(StandardCharsets.UTF_8);conn.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=conn.getOutputStream()){out.write(bytes);}}
        try{int status=conn.getResponseCode();if(cookies!=null)cookies.put(target.toURI(),conn.getHeaderFields());if(status<200||status>=300)throw new Exception("HTTP "+status);
            ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=conn.getInputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>6000000)throw new Exception("La página es demasiado grande.");out.write(buffer,0,n);}}
            Response response=new Response();response.body=new String(out.toByteArray(),StandardCharsets.UTF_8);response.headers=conn.getHeaderFields();return response;
        }finally{conn.disconnect();}
    }
    private Object[] quantity(String name,String unit,double size){
        if(size>0&&unit!=null&&!unit.isEmpty()){unit=unit.toLowerCase(Locale.ROOT);if(unit.equals("kg"))return new Object[]{size*1000,"g"};if(unit.equals("l"))return new Object[]{size*1000,"ml"};return new Object[]{size,unit.startsWith("u")?"ud":unit};}
        Matcher m=Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(kg|ml|g|l|litros?|ud\\.?|unidades?)\\b").matcher(name.toLowerCase(Locale.ROOT));double q=1;String u="ud";while(m.find()){q=Double.parseDouble(m.group(1).replace(',','.'));Matcher multi=Pattern.compile("(\\d+)\\s*[x×]\\s*$").matcher(name.substring(0,m.start()));if(multi.find())q*=Integer.parseInt(multi.group(1));u=m.group(2);if(u.equals("kg")){q*=1000;u="g";}else if(u.startsWith("l")){q*=1000;u="ml";}else if(u.startsWith("u"))u="ud";}return new Object[]{q,u};
    }
    private String timestamp(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss\'Z\'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
    private JSONObject metadata(String store,String postal,Map<String,JSONObject> products)throws Exception{if(products.size()<10)throw new Exception("No hay catálogo disponible para ese código postal.");return new JSONObject().put("store",store).put("postal",postal).put("products",new JSONArray(products.values())).put("updatedAt",timestamp()).put("locationVerified",true);}
    private JSONObject mercadona(String postal)throws Exception{
        progress.report("Localizando tu Mercadona…");Response address=request("https://tienda.mercadona.es/api/postal-codes/actions/change-pc/","PUT",new JSONObject().put("new_postal_code",postal).toString(),null);String warehouse=address.header("x-customer-wh");if(warehouse==null||warehouse.isEmpty())throw new Exception("Mercadona no ha confirmado tu zona.");String actual=address.header("x-customer-pc");if(actual!=null&&!postal.equals(actual))throw new Exception("Mercadona no ha confirmado el código postal.");
        Map<String,JSONObject> products=new LinkedHashMap<>();ExecutorService pool=Executors.newFixedThreadPool(4);List<Future<JSONObject>> tasks=new ArrayList<>();
        try{for(int category:CATEGORIES)tasks.add(pool.submit(()->{try{return new JSONObject(request("https://tienda.mercadona.es/api/categories/"+category+"/?wh="+URLEncoder.encode(warehouse,"UTF-8"),"GET",null,null).body);}catch(Exception e){return new JSONObject();}}));int count=0;for(Future<JSONObject> task:tasks){walkMercadona(task.get(),products);progress.report("Consultando productos de Mercadona · "+(++count)+"/"+CATEGORIES.length);}}finally{pool.shutdown();}
        return metadata("mercadona",postal,products).put("warehouse",warehouse);
    }
    private void walkMercadona(Object value,Map<String,JSONObject> result)throws Exception{
        if(value instanceof JSONObject){JSONObject obj=(JSONObject)value;JSONArray list=obj.optJSONArray("products");if(list!=null)for(int i=0;i<list.length();i++){JSONObject p=list.getJSONObject(i),price=p.getJSONObject("price_instructions");double euros=price.optDouble("unit_price",-1);if(euros<0)continue;Object[] q=quantity(p.getString("display_name"),price.optString("size_format"),price.optDouble("drained_weight",0)>0?price.optDouble("drained_weight"):price.optDouble("unit_size",0));String id=p.getString("id");result.put(id,new JSONObject().put("id",id).put("name",p.getString("display_name")).put("price",euros).put("quantity",q[0]).put("unit",q[1]).put("pack",p.optString("packaging","Unidad")).put("image",p.optString("thumbnail")).put("url",p.optString("share_url")).put("store","mercadona").put("drained",price.optDouble("drained_weight",0)>0).put("approximateWeight",price.optBoolean("approx_size")));}java.util.Iterator<String> keys=obj.keys();while(keys.hasNext()){String key=keys.next();if(!key.equals("products"))walkMercadona(obj.get(key),result);}}
        else if(value instanceof JSONArray){JSONArray arr=(JSONArray)value;for(int i=0;i<arr.length();i++)walkMercadona(arr.get(i),result);}
    }
    private JSONObject dia(String postal)throws Exception{
        progress.report("Localizando tu tienda DIA…");CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);request("https://www.dia.es/api/v1/common-aggregator/save-shipping-address?new_postal_code="+postal+"&skip_dry_run=true","PUT","{}",cookies);
        Map<String,JSONObject> products=new LinkedHashMap<>();boolean confirmed=false;int count=0;
        for(String term:TERMS){progress.report("Consultando productos de DIA · "+(++count)+"/"+TERMS.length);JSONObject data;try{data=new JSONObject(request("https://www.dia.es/api/v1/search-back/search/reduced?q="+URLEncoder.encode(term,"UTF-8"),"GET",null,cookies).body);}catch(Exception e){continue;}
            if(!postal.equals(data.optJSONObject("cart")==null?"":data.getJSONObject("cart").optString("postal_code")))throw new Exception("DIA no ha confirmado tu código postal.");confirmed=true;JSONArray list=data.optJSONArray("search_items");if(list==null)continue;
            for(int i=0;i<list.length();i++){JSONObject p=list.getJSONObject(i),price=p.optJSONObject("prices");if(p.optInt("units_in_stock",1)==0||price==null||!price.has("price"))continue;double euros=price.optDouble("price",-1);if(euros<0)continue;String name=p.getString("display_name"),pid=p.getString("object_id");Object[] q=quantity(name,null,0);String image=p.optString("image"),url=p.optString("url");resultProduct(products,pid,name,euros,q,image.startsWith("https:")?image:"https://www.dia.es"+image,url.startsWith("https:")?url:"https://www.dia.es"+url);}
        }
        if(!confirmed)throw new Exception("DIA no ha confirmado tu zona.");return metadata("dia",postal,products);
    }
    private void resultProduct(Map<String,JSONObject> out,String id,String name,double price,Object[] q,String image,String url)throws Exception{out.put(id,new JSONObject().put("id",id).put("name",name).put("price",price).put("quantity",q[0]).put("unit",q[1]).put("image",image).put("url",url).put("store","dia"));}
}
