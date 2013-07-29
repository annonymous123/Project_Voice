/*
 * Outgoing Call Context here sets the following channel variable
 * totalItem;item0,item1....,count
 */

package org.raxa.audioplayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.fastagi.BaseAgiScript;
import org.hibernate.Query;
import org.hibernate.Session;
import org.raxa.alertmessage.ContentFormat;
import org.raxa.database.HibernateUtil;
import org.raxa.database.Patient;
import org.raxa.alertmessage.MessageInterface;
import com.fasterxml.jackson.databind.ObjectMapper;


public class CallHandler extends BaseAgiScript implements MessageInterface
{
	private AgiRequest request;
	private AgiChannel channel;
	private Logger logger = Logger.getLogger(this.getClass());
	
	/**
	 * checks whether the call is incoming or outgoing.Handles the call accordingly
	 */
    public void service(AgiRequest request, AgiChannel channel) throws AgiException{
    	try{
    		answer();
	    	this.request=request;
	    	this.channel=channel;
	    	if(request.getContext().equals("incoming-call"))
	        	handleIncomingCall();
	        if(request.getContext().equals("outgoing-call"))
	        	provideMedicalInfo();
	        
	        return;
    	}
    	catch(AgiException ex){
    		
    		logger.error("IMPORTANT:SOME ERROR WHILE HANDLING THE CALL");
    		logger.info("Hanging the call");
    		hangup();
    	}
    	catch(Exception ex1){
    		
    		logger.error("IMPORTANT:SOME ERROR WHILE HANDLING THE CALL");
    		logger.info("Hanging  the call");
    		hangup();
    	}
    	
    }
    
    /**
     * Use in Incoming-Call Context
     * 
     * Handles patient incoming call.
     * 
     * INCOMPLETE
     * 
     * @throws AgiException
     * 
     */
    private void handleIncomingCall() throws AgiException{ 
    	
    	String welcome=null;String preferLanguageFileLocation=null;String patientIdNotFound=null;String preferLanguageNotIdentified=null;String mainmenu=null;
    	
    	String pnumber=channel.getName();
    	
    	Properties prop = new Properties();
		
    	//Get files to be played
    	try{
			logger.info("Trying to fetch the sound folder location");
			prop.load(this.getClass().getClassLoader().getResourceAsStream("soundFolder.properties"));
			welcome=prop.getProperty("welcome");
			preferLanguageFileLocation=prop.getProperty("getpreferLanguage");
			preferLanguageNotIdentified=prop.getProperty("languageNotFound");
			patientIdNotFound=prop.getProperty("patientIdNotFound");
		}
		catch(Exception ex){
			ex.printStackTrace();
			logger.info("welcome sound file not found");
		}
    	//Streaming the welcome file
		channel.streamFile(welcome);
    	
		String preferLanguage=incomingGetPreferLanguage(preferLanguageFileLocation);
		if(preferLanguage==null){
			channel.streamFile(preferLanguageNotIdentified);
			channel.hangup();
			return;
		}
		
		
		
		String pid=getPatientIdFromNumber(pnumber);
		if(pid==null){
			channel.streamFile(patientIdNotFound);
			channel.hangup();
			logger.error("Couldnot Get Patient ID");
			return;
		}
		
		
    	
    	
    	
    	
    	
    	
    	
    	
    	char option='0';
    	//Play main menu
    	
    	//choose what to do 
  
    	//Perform the below  if the patient choose something in main menu which needs to access the database
    	
    	//get Number of the caller
    	 //Doubt
    	
    	String patientName=null;
    	//get patient registered for the pnumber
    	List<Patient> nameAndId=getNameAndId(pnumber);
    	
    	//check if the patient exist
    	if(nameAndId!=null || nameAndId.size()<1){
    		//play that You are not registered.Please register yourself.
    		return;
    	}
    	//Iterate through the list of nameAndId and makes a string like 1 Atul Agrawal 2 Apurv Tiwari 3 John Stoecker
    	for(int i=0;i<nameAndId.size();i++){
    		patientName=patientName+" "+(i+1)+" "+nameAndId.get(i).getPatientName();
    	}
    	//get patient pressed DTMF key
    	//Play and get which patient is it
    	int whichPatient=getPatientName(patientName);
    	
    	String patientUUID=null;
    	
    	//check if pressed DTMF is valid or not
    	if(whichPatient<nameAndId.size())
    		 patientUUID = nameAndId.get(whichPatient).getPatientId();
    	
    	
    	
    	
   }
    
    /**
     *  Use in Incoming-Call Context
     * 
     * @param patientName
     * @return
     */
    int getPatientName(String patientName){
    	
    	return 0;
    }
    
    /**
     *  Use in Incoming-Call Context
     * 
     */
    
    private String getPatientIdFromNumber(String pnumber){
    	
    	return null;
    }
    
    /**
     *  Use in Incoming-Call Context
     * 
     * @param preferLanguageFileLocation
     * @return
     * @throws AgiException
     */
    
    private String incomingGetPreferLanguage(String preferLanguageFileLocation) throws AgiException{
    	String preferLanguage=null;int max_try=2;
    	Properties prop=new Properties();
    	for(int i=0;i<max_try;i++){
			char preferLanguagecode=channel.getOption(preferLanguageFileLocation,"12345678");
			
			try{
				prop.load(this.getClass().getClassLoader().getResourceAsStream("languageMap.properties"));
				preferLanguage=prop.getProperty(Character.toString(preferLanguagecode),"0");
				if(preferLanguage!=null){
					return preferLanguage;
				}
				
			}
			catch(Exception ex){
				logger.fatal("Unable to set language to speak to patient with phonenumber.Trying Again");
				
			}
		}
		return null;
    }
    /**
     *  Use in Incoming-Call Context
     * 
     * @param pnumber
     * @return
     */
    List<Patient> getNameAndId(String pnumber){
    	List<Patient> nameAndId=new ArrayList<Patient>();
    	try{
	    	Session session = HibernateUtil.getSessionFactory().openSession();
	    	session.beginTransaction();
			String hql="from patient where (pnumber=:pnumber or snumber=:pnumber)";
	    	Query query=session.createQuery(hql);
	    	query.setString("pnumber", pnumber);
	    	List patientList=query.list();
	    	if(patientList==null || patientList.size()<1)
	    		return null;
	    	for(int i=0;i<patientList.size();i++){
	    		Patient p=(Patient)patientList.get(i);
	    		nameAndId.add(p);
	    	}
	    	return nameAndId;
    	}
    	catch(Exception ex){
    		logger.error("unable to retrieve data for patient with phone number:"+pnumber);
    		ex.printStackTrace();
    		return null;
    	}
    }
    /**
     *  Use in Incoming-Call Context.
     * 
     * takes message and ttsNotation i.e (en,hi) and plays it
     * @param message
     * @param ttsNotation
     * @throws AgiException
     */
    private void playUsingTTS(String message,String ttsNotation) throws AgiException{
    	String contentToPlay="googletts.agi,\""+message+"\""+","+ttsNotation;
    	//eg.exec("AGI","google"hello Atul today you have to take",hi);
    	exec("AGI",contentToPlay);
    	
    }
    
    /**
     *  Use in Incoming-Call Context
     * 
     *Copied from org.raxa.module.ami.Outgoing.getTTSNotation Method. Just to make the AGI completely independent. 
     *Return ttsNotation for preferLanguage else return default Notation
     */
    
    public String getTTSNotation(String preferLanguage){
    	String defaultLanguage=null;
    	Properties prop = new Properties();
		try{
			logger.info("Trying to fetch the notation for the prefer language:"+preferLanguage);
			prop.load(this.getClass().getClassLoader().getResourceAsStream("tts.properties"));
			defaultLanguage=prop.getProperty("default");
			return(prop.getProperty(preferLanguage.toLowerCase()));
		}
		catch(IOException ex) {
    		ex.printStackTrace();
    		logger.error("Unable to set prefer language:"+preferLanguage+" playing in defaultLanguage:"+defaultLanguage);
    		return defaultLanguage;
        }
    }
    
    /**
     * used in Outgoing-Call Context.
     * 
     * It loops between AGI and asterisk dial plan outgoing-call context.
     * asterisk dial plan contains google TTS.
     * If asked to play using TTS it returns and play it via google TTS.
     * If asked to play using audio folder it loops back using while.
     * 
     * It does use recursion.
     * 
     * Warning:Unless you are familiar with asterisk dial plan don't mess with it.
     * @throws AgiException
     */
   
    private void provideMedicalInfo() throws AgiException{
    	
    	while(true){
	    	int readItemSoFar=Integer.parseInt(channel.getVariable("count"));
	    	
	    	if(readItemSoFar==0){
	    		int msgId=Integer.parseInt(request.getParameter("msgId"));
	            
	            try{
	            	List content=getMessageContent(msgId);
	            	int totalItem=content.size();
	            	channel.setVariable("totalItem",String.valueOf(totalItem));
	            	for(int i=0;i<totalItem;i++){
	            		String item="item"+i;
	            		channel.setVariable(item,(String)content.get(i));
	            	}
	            }
	            catch(Exception ex){
	            	logger.error("IMPORTANT:ERROR OCCURED WHILE IN CALL.CHECK THE ISSUE");
	            	channel.hangup();
	            	return;
	            }
	    	}
	    	
	    	
	    	if(readItemSoFar>=Integer.parseInt(channel.getVariable("totalItem"))){
	    		channel.hangup();
	    		int par1=Integer.parseInt(request.getParameter("msgId"));
	    		String aid=request.getParameter("aid");
	    		String serviceInfo=channel.getName();//Doubt
	    		CallSuccess obj=new CallSuccess(par1,aid,true,serviceInfo);
	    		obj.updateAlert();
	    		return;
	    	}
	    	
	    	updateCount(readItemSoFar);
	    	
	    	String itemNumber="item"+readItemSoFar;
	    	String itemContent=channel.getVariable(itemNumber);
	    	ContentFormat message=parseString(itemContent);
	    	String preferLanguage=(request.getParameter("language")).toLowerCase();
	    	String ttsNotation=request.getParameter("ttsNotation");
	    	
	    	if(message==null || (message.getContent())==null){
	    		provideMedicalInfo();
	    		return;
	    	}
	    	
	    	//Caution:Ensure that the below if statement return.
	    	
	    	if(message.getMode()==TTS_MODE){
	    		logger.info("Playing "+message.getContent()+" in TTS");
	    		channel.setVariable("message", message.getContent().toLowerCase());
	    		channel.setVariable("language",ttsNotation);
	    		return;
	    	}
	    	//The part below does not depend on dialplan.So you can mess with it..
	    	
	    	else if(message.getMode()==AUDIO_MODE){
		    		Properties prop = new Properties();
		    		try{
		    			logger.info("Searching for "+preferLanguage+".properties file");
			    		prop.load(this.getClass().getClassLoader().getResourceAsStream(preferLanguage+".properties"));
			    		String fileLocation=prop.getProperty(message.getField())+"/"+message.getField();    //if want to put un fromatted location then remove "/"+message.getField() 
			    		logger.info("Playing "+message.getField()+" in from audio Folder with file location "+fileLocation);
			    		channel.streamFile(fileLocation);
			    	}
		    		catch (IOException ex) {
		        		ex.printStackTrace();
		        		logger.error("Some error while playing AudioFile returning back");
		        		
		            }
		    }
    	}
    }
    
    /**
     * used in Outgoing-Call Context.
     * 
     * update how many ivrMsg:itemNumber of msgId has been played.
     * @param count
     * @throws AgiException
     */
    private void updateCount(int count) throws AgiException{
		++count;
		channel.setVariable("count",String.valueOf(count));
	}
    
    /**
     * used in Outgoing-Call Context.
     * 
     * @param itemContent
     * @return
     */
	private ContentFormat parseString(String itemContent){
		try{
			logger.info("Parsing the content of msgId(Json String)");
    		ObjectMapper mapper = new ObjectMapper();
    		return (mapper.readValue(itemContent, ContentFormat.class));
		}
		catch(Exception ex){
			return null;
		}
	}
   
	
	/**
	 * used in Outgoing-Call Context.
	 * 
     * Get Message Content form IvrMsg
     * @param msgId
     * @return
     * @throws Exception
     */
	private List getMessageContent(int msgId) throws Exception{
		logger.info("Getting content for medicine Reminder haveing msgId"+msgId);
		String hql="select content from IvrMsg where ivrId=:msgId order by itemNumber";
		Session session = HibernateUtil.getSessionFactory().openSession();
    	session.beginTransaction();
    	Query query=session.createQuery(hql);
    	query.setInteger("msgId", msgId);
    	List content=query.list();
    	session.getTransaction().commit();
    	session.close();
    	logger.info("Successfully retreived msg content from database with msgId"+msgId);
    	return content;
	}
    
}
    
 