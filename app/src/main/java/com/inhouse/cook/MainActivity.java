package com.inhouse.cook;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
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

public class MainActivity extends Activity {
    private WebView web;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newFixedThreadPool(3);
    private ValueCallback<Uri[]> chooser;
    private String exportId, exportText, locationId;
    private static final int PICK_FILE=51, SAVE_FILE=52, LOCATION_PERMISSION=53;


    @Override public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        getWindow().setStatusBarColor(0xfff5f5f0);getWindow().setNavigationBarColor(0xfff5f5f0);
        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        web=new WebView(this);web.setBackgroundColor(0xfff5f5f0);web.setFitsSystemWindows(true);
        if(android.os.Build.VERSION.SDK_INT>=30){web.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets edges=insets.getInsets(android.view.WindowInsets.Type.systemBars());v.setPadding(edges.left,edges.top,edges.right,edges.bottom);return insets;});}
        WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);settings.setAllowContentAccess(true);settings.setAllowFileAccessFromFileURLs(false);settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setMediaPlaybackRequiresUserGesture(true);
        web.addJavascriptInterface(new Bridge(),"Native");
        web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){Uri uri=request.getUrl();if("file".equals(uri.getScheme())&&uri.toString().startsWith("file:///android_asset/"))return false;if("https".equals(uri.getScheme()))openBrowser(uri.toString());return true;}});
        web.setWebChromeClient(new WebChromeClient(){@Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> callback,FileChooserParams params){if(chooser!=null)chooser.onReceiveValue(null);chooser=callback;Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json");startActivityForResult(intent,PICK_FILE);return true;}});
        setContentView(web);web.loadUrl("file:///android_asset/index.html");
    }
    private void success(String id,Object data){respond(id,true,data,null);}
    private void fail(String id,String message){respond(id,false,null,message);}
    private void respond(String id,boolean ok,Object data,String error){try{JSONObject result=new JSONObject().put("ok",ok);if(ok)result.put("data",data==null?JSONObject.NULL:data);else result.put("error",error);String js="window.NativeResult&&window.NativeResult("+JSONObject.quote(id)+","+result+")";main.post(()->{if(web!=null)web.evaluateJavascript(js,null);});}catch(Exception ignored){}}
    private void progress(String id,String message){main.post(()->{if(web!=null)web.evaluateJavascript("window.NativeProgress&&window.NativeProgress("+JSONObject.quote(id)+","+JSONObject.quote(message)+")",null);});}
    private String message(Exception e){String m=e.getMessage();if(m!=null&&m.contains("HTTP"))return "El supermercado no ha respondido. Comprueba la cobertura del código postal y vuelve a intentarlo.";return m==null?"No se pudo completar la consulta.":m;}
    public class Bridge {
        @JavascriptInterface public void request(String id,String action,String payload){
            final JSONObject data;try{data=new JSONObject(payload);}catch(Exception e){fail(id,"Datos inválidos");return;}
            if(action.equals("location")){main.post(()->locate(id));return;}
            if(action.equals("open")){main.post(()->{try{openBrowser(data.getString("url"));success(id,true);}catch(Exception e){fail(id,message(e));}});return;}
            if(action.equals("share")){main.post(()->{try{Intent intent=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,data.getString("text"));startActivity(Intent.createChooser(intent,"Compartir compra"));success(id,true);}catch(Exception e){fail(id,message(e));}});return;}
            if(action.equals("awake")){main.post(()->{if(data.optBoolean("enabled"))getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);success(id,true);});return;}
            if(action.equals("export")){main.post(()->{if(exportId!=null){fail(id,"Ya hay una exportación abierta.");return;}exportId=id;exportText=data.optString("text");Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE,"inhouse-cook.json");startActivityForResult(intent,SAVE_FILE);});return;}
            worker.execute(()->{try{switch(action){case "catalog":String postal=data.getString("postal"),store=data.getString("store");if(!postal.matches("\\d{5}")||Integer.parseInt(postal.substring(0,2))<1||Integer.parseInt(postal.substring(0,2))>52)throw new Exception("Código postal no válido.");if(store.equals("mercadona"))success(id,new CatalogClient(text->progress(id,text)).load(store,postal));else if(store.equals("dia"))success(id,new CatalogClient(text->progress(id,text)).load(store,postal));else throw new Exception("Supermercado no disponible.");break;case "recipe":success(id,readRecipe(data.getString("url")));break;default:throw new Exception("Acción no disponible.");}}catch(Exception e){fail(id,message(e));}});
        }
    }
    private void openBrowser(String url){Uri uri=Uri.parse(url);if(!"https".equals(uri.getScheme())||uri.getHost()==null)throw new IllegalArgumentException("El enlace debe usar HTTPS.");startActivity(new Intent(Intent.ACTION_VIEW,uri));}
    private JSONObject findRecipe(Object value){if(value instanceof JSONObject){JSONObject obj=(JSONObject)value;Object type=obj.opt("@type");if("Recipe".equals(type)||(type instanceof JSONArray&&type.toString().contains("\"Recipe\"")))return obj;java.util.Iterator<String> keys=obj.keys();while(keys.hasNext()){JSONObject r=findRecipe(obj.opt(keys.next()));if(r!=null)return r;}}else if(value instanceof JSONArray){JSONArray list=(JSONArray)value;for(int i=0;i<list.length();i++){JSONObject r=findRecipe(list.opt(i));if(r!=null)return r;}}return null;}
    private String clean(String s){return android.text.Html.fromHtml(s,android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim();}
    private void instructions(Object value,JSONArray steps){if(value instanceof String){for(String line:((String)value).split("[\\r\\n]+"))if(!clean(line).isEmpty())steps.put(clean(line));}else if(value instanceof JSONObject){JSONObject obj=(JSONObject)value;if(obj.has("text"))steps.put(clean(obj.optString("text")));else instructions(obj.opt("itemListElement"),steps);}else if(value instanceof JSONArray){JSONArray arr=(JSONArray)value;for(int i=0;i<arr.length();i++)instructions(arr.opt(i),steps);}}
    private JSONObject readRecipe(String url)throws Exception{
        URI uri=new URI(url);if(!"https".equals(uri.getScheme())||uri.getHost()==null)throw new Exception("Pega un enlace HTTPS válido.");String host=uri.getHost().toLowerCase(Locale.ROOT);if(host.equals("localhost")||host.matches("[0-9.]+")||host.endsWith(".local"))throw new Exception("Usa el enlace público de un blog.");
        String html=new CatalogClient(text->{}).page(url);Matcher scripts=Pattern.compile("<script\\b[^>]*type\\s*=\\s*[\"']application/ld\\+json[\"'][^>]*>([\\s\\S]*?)</script>",Pattern.CASE_INSENSITIVE).matcher(html);JSONObject r=null;
        while(scripts.find()){try{Object data=new org.json.JSONTokener(scripts.group(1).trim()).nextValue();r=findRecipe(data);if(r!=null)break;}catch(Exception ignored){}}
        if(r==null)throw new Exception("Este enlace no publica datos de receta. Pega sus ingredientes y pasos para guardarla.");JSONArray ingredients=r.optJSONArray("recipeIngredient"),steps=new JSONArray();instructions(r.opt("recipeInstructions"),steps);if(ingredients==null||ingredients.length()==0||steps.length()==0)throw new Exception("El blog no publica todos los ingredientes y pasos. Puedes completarlos manualmente.");JSONArray cleaned=new JSONArray();for(int i=0;i<ingredients.length();i++)cleaned.put(clean(ingredients.getString(i)));
        String duration=r.optString("totalTime",r.optString("cookTime","PT25M"));Matcher h=Pattern.compile("(\\d+)H").matcher(duration),m=Pattern.compile("(\\d+)M").matcher(duration);int minutes=(h.find()?Integer.parseInt(h.group(1))*60:0)+(m.find()?Integer.parseInt(m.group(1)):0);Matcher portions=Pattern.compile("\\d+").matcher(r.optString("recipeYield","2"));int servings=portions.find()?Integer.parseInt(portions.group()):2;
        Object author=r.opt("author");if(author instanceof JSONArray)author=((JSONArray)author).opt(0);String authorName=author instanceof JSONObject?((JSONObject)author).optString("name"):author instanceof String?(String)author:"";
        Object image=r.opt("image");if(image instanceof JSONArray)image=((JSONArray)image).opt(0);String imageUrl=image instanceof JSONObject?((JSONObject)image).optString("url"):image instanceof String?(String)image:"";if(!imageUrl.startsWith("https://"))imageUrl="";
        return new JSONObject().put("author",clean(authorName)).put("image",imageUrl).put("name",clean(r.optString("name","Mi receta"))).put("ingredients",cleaned).put("steps",steps).put("time",Math.max(1,minutes)).put("servings",Math.max(1,Math.min(20,servings)));
    }
    private void locate(String id){
        if(locationId!=null){fail(id,"Ya se está buscando tu ubicación.");return;}locationId=id;
        if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},LOCATION_PERMISSION);return;}findLocation();
    }
    private void findLocation(){String id=locationId;if(id==null)return;try{
        LocationManager manager=(LocationManager)getSystemService(LOCATION_SERVICE);Location last=null;for(String provider:manager.getProviders(true)){try{Location candidate=manager.getLastKnownLocation(provider);if(candidate!=null&&(last==null||candidate.getTime()>last.getTime()))last=candidate;}catch(SecurityException ignored){}}
        if(last!=null&&System.currentTimeMillis()-last.getTime()<3600000){reverse(id,last);return;}
        if(!manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){locationId=null;fail(id,"Activa la ubicación o escribe tu código postal.");return;}
        LocationListener listener=new LocationListener(){boolean complete=false;@Override public void onLocationChanged(Location location){if(complete)return;complete=true;manager.removeUpdates(this);reverse(id,location);}@Override public void onStatusChanged(String p,int s,Bundle b){}@Override public void onProviderEnabled(String p){}@Override public void onProviderDisabled(String p){} };
        manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,listener,Looper.getMainLooper());main.postDelayed(()->{if(id.equals(locationId)){manager.removeUpdates(listener);locationId=null;fail(id,"No se pudo obtener tu ubicación. Escribe el código postal.");}},18000);
    }catch(Exception e){locationId=null;fail(id,"No se pudo obtener tu ubicación. Escribe el código postal.");}}
    private void reverse(String id,Location location){locationId=null;worker.execute(()->{try{List<Address> list=new Geocoder(this,new Locale("es","ES")).getFromLocation(location.getLatitude(),location.getLongitude(),1);if(list==null||list.isEmpty()||list.get(0).getPostalCode()==null)throw new Exception("No se pudo obtener el código postal. Escríbelo manualmente.");Address a=list.get(0);success(id,new JSONObject().put("postal",a.getPostalCode()).put("city",a.getLocality()==null?"":a.getLocality()));}catch(Exception e){fail(id,message(e));}});}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);if(request==LOCATION_PERMISSION){if(grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)findLocation();else{String id=locationId;locationId=null;if(id!=null)fail(id,"Sin permiso de ubicación. Escribe tu código postal.");}}}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==PICK_FILE&&chooser!=null){chooser.onReceiveValue(result==RESULT_OK&&data!=null?new Uri[]{data.getData()}:null);chooser=null;}if(request==SAVE_FILE&&exportId!=null){String id=exportId,text=exportText;exportId=null;exportText=null;if(result==RESULT_OK&&data!=null)worker.execute(()->{try(OutputStream out=getContentResolver().openOutputStream(data.getData())){out.write(text.getBytes(StandardCharsets.UTF_8));success(id,true);}catch(Exception e){fail(id,message(e));}});else fail(id,"Exportación cancelada.");}}
    @Override public void onBackPressed(){web.evaluateJavascript("window.App?window.App.back():false",result->{if(!"true".equals(result))finish();});}
    @Override protected void onDestroy(){if(chooser!=null)chooser.onReceiveValue(null);worker.shutdownNow();if(web!=null){web.removeJavascriptInterface("Native");web.destroy();web=null;}super.onDestroy();}
}
