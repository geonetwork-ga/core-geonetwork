package org.fao.geonet.services.metadata;

import java.io.IOException;

import jeeves.server.ServiceConfig;
import jeeves.server.context.ServiceContext;
import jeeves.constants.Jeeves;

import org.fao.geonet.services.Utils;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.services.NotInReadOnlyModeService;
import org.jdom.Element;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.HttpResponse;

import java.io.BufferedReader;  
import java.io.DataOutputStream;  
import java.io.InputStreamReader;  
import java.net.HttpURLConnection;  
import java.net.URL;  


/**
 * For editing : update leaves information. Access is restricted.
 */
public class CreateHCPBucket extends NotInReadOnlyModeService {
	private ServiceConfig config;
  private String HCPLocation;
  private String HCPnfsLocation;
  private String HCPsmbLocation;
  private Element elResp;

 	public void init(String appPath, ServiceConfig params) throws Exception
  {
 		config = params;
    elResp = new Element(Jeeves.Elem.RESPONSE);
    
  }
  
 	public Element serviceSpecificExec(Element params, ServiceContext context) throws Exception {   
 		String id = Utils.getIdentifierFromParameters(params, context);

    // If no id found, Utils.getIdentifierFromParameters returns null
    if (id == null)
      throw new IllegalArgumentException("Metadata not found");

    //***** NEEDS to be sent through to here
    String parent = "groundwater";
    //***********

    
    
     sendPostRequest(parent, id);
    // ---- insert sendPostRequest here...
    
    
    
    elResp.addContent(new Element(Geonet.Elem.ID).setText(id));
    elResp.addContent(new Element("path").setText(HCPsmbLocation + "\\" +parent+"\\"+id ));
    elResp.addContent(new Element("webPath").setText(HCPnfsLocation + "/browser/content_input.action?fileName=/"+parent+"/"+id+"/"));
    
    
    return elResp;
  }

 
 // HTTP Post request  
 private void sendPostRequest(String parent, String name) throws Exception {  
  String url = "http://localhost:8082/HCPservlet";
  URL obj = new URL(url);
  HttpURLConnection con = (HttpURLConnection) obj.openConnection();

  // Setting basic post request  
  con.setRequestMethod("POST");
  con.setRequestProperty("User-Agent", "Mozilla/5.0");
  con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
  con.setRequestProperty("Content-Type","application/json");

  String postJsonData = "{\"name\":\""+name+"\",\"parent\":\""+parent+"\"}";

  // Send post request
  con.setDoOutput(true);
  DataOutputStream wr = new DataOutputStream(con.getOutputStream());
  wr.writeBytes(postJsonData);
  wr.flush();
  wr.close();

  int responseCode = con.getResponseCode();

  System.out.println("\nSending 'POST' request to URL : " + url);
  System.out.println("Post Data : " + postJsonData);
  System.out.println("Response Code : " + responseCode);

  if(responseCode == 200){   
    BufferedReader in = new BufferedReader( new InputStreamReader(con.getInputStream()) );
    String output;
    StringBuffer response = new StringBuffer();

    while ((output = in.readLine()) != null) {
      response.append(output);
    }
    //printing result from response
    System.out.println(response.toString());
    
    in.close();
  }else{
    System.out.println("*** createHCPBucket.java - Error from HCP servlet");
  }


 }

 }