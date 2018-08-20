package com.cas.graph.mining.Graph;

import org.apache.log4j.*;

public class Test {
    private static Logger logger = Logger.getLogger(Test.class);
    
    public static void main(String[] args) throws Exception {
    	PropertyConfigurator.configure( "./config/log4j.properties" );
    	 Logger logger  =  Logger.getLogger(Test.class );
    	// 记录debug级别的信息  
        logger.debug("i am xujingjing.");  
        // 记录info级别的信息  
        logger.info("This is info message.");  
        // 记录error级别的信息  
        logger.error("This is error message."); 
    }
}