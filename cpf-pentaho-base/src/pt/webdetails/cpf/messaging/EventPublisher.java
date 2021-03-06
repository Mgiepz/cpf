/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cpf.messaging;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import pt.webdetails.cpf.CpfProperties;
import pt.webdetails.cpf.JsonPluginCall;
import pt.webdetails.cpf.PentahoInterPluginCall;
import pt.webdetails.cpf.Result;
import pt.webdetails.cpf.plugin.CorePlugin;


/**
 * Inefficient but thread-safe..
 */
public class EventPublisher implements IEventPublisher {
  
  protected static final long TIMEOUT = CpfProperties.getInstance().getLongProperty("messaging.publishTimeout", 11); 
  private static final boolean LOG_PUBLISH = false;//CpfProperties.getInstance().getBooleanProperty("messaging.logPublish", false);
  private static Log logger = LogFactory.getLog(EventPublisher.class);
  private static ThreadPoolExecutor executor =
      new ThreadPoolExecutor(0, CpfProperties.getInstance().getIntProperty("messaging.maxThreads", 1), 
          CpfProperties.getInstance().getLongProperty("messaging.publishTimeout", 11), TimeUnit.SECONDS, 
          new ArrayBlockingQueue<Runnable>(CpfProperties.getInstance().getIntProperty("messaging.queueSize", 3), true) ,
          new ThreadPoolExecutor.DiscardOldestPolicy()); 
      
//  static {
//    executor.allowCoreThreadTimeOut(true);
//  }
  
  private static Boolean cdvExists;
  
  public synchronized static boolean canPush(){
    if(cdvExists == null){
      cdvExists = new PentahoInterPluginCall(CorePlugin.CDV, "whatever").pluginExists();
    }
    return cdvExists;
  }

  public EventPublisher() {}
  
  
  public static EventPublisher getPublisher(){
    return new EventPublisher();
  }
  
  public void publish(final PluginEvent event){
    
    if(!canPush()){
      logger.warn("publishToCDV: plugin not available, ignoring request");
      return;
    }
    
    Runnable toRun = LOG_PUBLISH ? getPublishAndLogTask(event) :  getPublishTask(event);
    executor.execute(toRun);
  }
  
  private Runnable getPublishAndLogTask(final PluginEvent event){
    Runnable publishAndLog = new Runnable(){
      @Override
      public void run() {
        
      FutureTask<Result> toRun = getPublishTask(event);
        
        try {
          executor.execute(toRun);
          Result result = toRun.get(TIMEOUT, TimeUnit.SECONDS);
          String msg = "[" + event.getPlugin() + "] pushed event " + result;
          switch(result.getStatus()){
            case OK:
              logger.info(msg);
              break;
            case ERROR:
              logger.error(msg);
              break;
          }
        } catch (Exception e){
          toRun.cancel(true);
          logger.error("push failed: timeout reached: " + TIMEOUT + " seconds");
        }

      }
    };
    return publishAndLog;
  }
  
  private FutureTask<Result> getPublishTask(final PluginEvent event){
    return new FutureTask<Result>(new Callable<Result>(){

      @Override
      public Result call() throws Exception {
        JsonPluginCall call = new JsonPluginCall(CorePlugin.CDV, "warnings");
        return new Result(call.call(event.toJSON()));
      }
      
    });
  }
  
}
