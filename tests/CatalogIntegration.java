import com.inhouse.cook.CatalogClient;
import org.json.*;
/** Runs the SAME catalog code used in the APK against live public retailer APIs. */
public class CatalogIntegration {
 public static void main(String[] args)throws Exception{
  for(String store:new String[]{"mercadona","dia"})for(String postal:new String[]{"28001","08001"}){
   JSONObject c=new CatalogClient(s->{}).load(store,postal);
   if(!c.getBoolean("locationVerified")||!postal.equals(c.getString("postal"))||c.getJSONArray("products").length()<50)throw new AssertionError("Invalid location/catalog");
   for(Object obj:c.getJSONArray("products")){JSONObject p=(JSONObject)obj;if(p.getDouble("price")<0||p.getDouble("quantity")<=0)throw new AssertionError("Invalid product price/quantity");}
   System.out.println("PASS "+store+" "+postal+": "+c.getJSONArray("products").length()+" real products; warehouse "+c.optString("warehouse","session postcode confirmed"));
  }
 }
}
