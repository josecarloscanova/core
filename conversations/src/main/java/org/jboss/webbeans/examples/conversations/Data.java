package org.jboss.webbeans.examples.conversations;

import javax.context.ConversationScoped;
import javax.annotation.Named;
import javax.annotation.PreDestroy;

import org.jboss.webbeans.log.LogProvider;
import org.jboss.webbeans.log.Logging;

import java.io.Serializable;

@ConversationScoped
@Named
public class Data implements Serializable
{
	private static LogProvider log = Logging.getLogProvider(Data.class);
	private String data;

	public Data()
	{
	}
	
	public String getData()
	{
		return data;
	}
	
	public void setData(String data)
	{
		this.data = data;
	}
	
   public void longop() {
      try 
      {
         Thread.sleep(5000);
      } 
      catch (InterruptedException e) 
      {
         // non-issue
      }
   }
   
   @PreDestroy
   public void bye() 
   {
   		log.info("Data " + data + " destroyed");
   }
   
}